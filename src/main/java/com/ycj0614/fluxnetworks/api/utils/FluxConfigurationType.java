package com.ycj0614.fluxnetworks.api.utils;

import net.minecraft.nbt.NBTTagCompound;

import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;

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
        // TODO 2단계: 구성기로 EU 설정 복사 (플러그 maxInputTier, 포인트 outputTier/outputAmperage)
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
        // TODO 2단계: 구성기로 EU 설정 복사 (플러그 maxInputTier, 포인트 outputTier/outputAmperage)
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
        tile.limit = nbt.getLong(key);
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
