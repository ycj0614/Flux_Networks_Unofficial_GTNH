package com.ycj0614.fluxnetworks.api.utils;

import net.minecraft.nbt.NBTTagCompound;

import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPlug;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPoint;

/// TODO remove common references
public class FluxConfigurationType {

    public static FluxConfigurationType NETWORK = new FluxConfigurationType(
        0,
        "network",
        FluxConfigurationType::copyNetwork,
        FluxConfigurationType::pasteNetwork);
    public static FluxConfigurationType PRIORITY = new FluxConfigurationType(
        2,
        "priority",
        FluxConfigurationType::copyPriority,
        FluxConfigurationType::pastePriority);
    public static FluxConfigurationType PRIORITY_SETTING = new FluxConfigurationType(
        3,
        "p_setting",
        FluxConfigurationType::copyPrioritySetting,
        FluxConfigurationType::pastePrioritySetting);
    public static FluxConfigurationType TRANSFER = new FluxConfigurationType(
        4,
        "transfer",
        FluxConfigurationType::copyTransfer,
        FluxConfigurationType::pasteTransfer);
    public static FluxConfigurationType TRANSFER_SETTING = new FluxConfigurationType(
        5,
        "t_setting",
        FluxConfigurationType::copyTransferSetting,
        FluxConfigurationType::pasteTransferSetting);

    public static FluxConfigurationType[] VALUES = new FluxConfigurationType[] { NETWORK, PRIORITY, PRIORITY_SETTING,
        TRANSFER, TRANSFER_SETTING };

    public int ordinal;
    public String key;
    public ICopyMethod copy;
    public IPasteMethod paste;

    public FluxConfigurationType(int ordinal, String key, ICopyMethod copy, IPasteMethod paste) {
        this.ordinal = ordinal;
        this.key = key;
        this.copy = copy;
        this.paste = paste;
    }

    public String getNBTName() {
        return key;
    }

    //// COPY / PASTE ALL (원작 FluxUtils.copyConfiguration / pasteConfiguration) \\\\

    /**
     * 모든 설정 종류를 타일에서 config로 복사하고 config를 반환한다.
     */
    public static NBTTagCompound copyAll(TileFluxCore tile, NBTTagCompound config) {
        if (config == null) {
            config = new NBTTagCompound();
        }
        for (FluxConfigurationType type : VALUES) {
            type.copy.copyFromTile(config, type.getNBTName(), tile);
        }
        // EU 설정 (원작에 없음). 키는 장치 NBT 키와 같다
        if (tile instanceof TileFluxPlug) {
            config.setInteger(TileFluxPlug.NBT_MAX_INPUT_TIER, ((TileFluxPlug) tile).getMaxInputTier());
        }
        if (tile instanceof TileFluxPoint) {
            TileFluxPoint point = (TileFluxPoint) tile;
            config.setInteger(TileFluxPoint.NBT_OUTPUT_TIER, point.getOutputTier());
            config.setLong(TileFluxPoint.NBT_OUTPUT_AMPERAGE, point.getOutputAmperage());
        }
        return config;
    }

    /**
     * config에 키가 있는 설정만 타일에 붙여넣는다.
     */
    public static void pasteAll(TileFluxCore tile, NBTTagCompound config) {
        if (config == null) {
            return;
        }
        for (FluxConfigurationType type : VALUES) {
            if (config.hasKey(type.getNBTName())) {
                type.paste.pasteToTile(config, type.getNBTName(), tile);
            }
        }
        // EU 설정 (원작에 없음). 보정과 동기화를 위해 반드시 setter를 쓴다
        if (tile instanceof TileFluxPlug && config.hasKey(TileFluxPlug.NBT_MAX_INPUT_TIER)) {
            ((TileFluxPlug) tile).setMaxInputTier(config.getInteger(TileFluxPlug.NBT_MAX_INPUT_TIER));
        }
        if (tile instanceof TileFluxPoint) {
            TileFluxPoint point = (TileFluxPoint) tile;
            if (config.hasKey(TileFluxPoint.NBT_OUTPUT_TIER)) {
                point.setOutputTier(config.getInteger(TileFluxPoint.NBT_OUTPUT_TIER));
            }
            if (config.hasKey(TileFluxPoint.NBT_OUTPUT_AMPERAGE)) {
                point.setOutputAmperage(config.getLong(TileFluxPoint.NBT_OUTPUT_AMPERAGE));
            }
        }
    }

    //// NETWORK \\\\

    public static void copyNetwork(NBTTagCompound nbt, String key, TileFluxCore tile) {
        if (!tile.getNetwork()
            .isInvalid() && tile.getNetworkID() != -1) {
            nbt.setInteger(key, tile.getNetworkID());
        }
    }

    public static void pasteNetwork(NBTTagCompound nbt, String key, TileFluxCore tile) {
        int storedID = nbt.getInteger(key);
        if (storedID != -1) {
            IFluxNetwork newNetwork = FluxNetworkCache.instance.getNetwork(storedID);
            tile.getNetwork()
                .queueConnectionRemoval(tile, false);
            newNetwork.queueConnectionAddition(tile);
        }
    }

    //// PRIORITY \\\\

    public static void copyPriority(NBTTagCompound nbt, String key, TileFluxCore tile) {
        nbt.setInteger(key, tile.priority);
    }

    public static void pastePriority(NBTTagCompound nbt, String key, TileFluxCore tile) {
        tile.priority = nbt.getInteger(key);
    }

    //// PRIORITY SETTING \\\\

    public static void copyPrioritySetting(NBTTagCompound nbt, String key, TileFluxCore tile) {
        nbt.setBoolean(key, tile.surgeMode);
    }

    public static void pastePrioritySetting(NBTTagCompound nbt, String key, TileFluxCore tile) {
        tile.surgeMode = nbt.getBoolean(key);
    }

    //// TRANSFER LIMIT \\\\

    public static void copyTransfer(NBTTagCompound nbt, String key, TileFluxCore tile) {
        nbt.setLong(key, tile.limit);
    }

    public static void pasteTransfer(NBTTagCompound nbt, String key, TileFluxCore tile) {
        tile.limit = Math.max(0L, nbt.getLong(key)); // 음수 한도는 0 (PacketByteBuf ID 3과 같음)
    }

    //// TRANSFER SETTING \\\\

    public static void copyTransferSetting(NBTTagCompound nbt, String key, TileFluxCore tile) {
        nbt.setBoolean(key, tile.disableLimit);
    }

    public static void pasteTransferSetting(NBTTagCompound nbt, String key, TileFluxCore tile) {
        tile.disableLimit = nbt.getBoolean(key);
    }

    public interface ICopyMethod<T> {

        void copyFromTile(NBTTagCompound tag, String key, TileFluxCore tile);
    }

    public interface IPasteMethod<T> {

        void pasteToTile(NBTTagCompound tag, String key, TileFluxCore tile);
    }
}
