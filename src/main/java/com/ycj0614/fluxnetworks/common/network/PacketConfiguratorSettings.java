package com.ycj0614.fluxnetworks.common.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import com.ycj0614.fluxnetworks.api.utils.FluxConfigurationType;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.core.GTEnergy;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.item.ItemConfigurator;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPlug;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPoint;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Client → Server (ID 17): 구성기 GUI에서 고친 이름과 설정 태그를 손에 든 구성기에 쓴다.
 */
public class PacketConfiguratorSettings
    implements IMessageHandler<PacketConfiguratorSettings.ConfiguratorSettingsMessage, IMessage> {

    @Override
    public IMessage onMessage(ConfiguratorSettingsMessage message, MessageContext ctx) {
        final String customName = message.customName;
        final NBTTagCompound tag = message.tag;
        final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null || customName == null) {
            return null;
        }
        PacketHandler.handlePacket(() -> {
            ItemStack stack = player.getHeldItem();
            // 보안: 서버에서 손에 든 아이템이 구성기인지 다시 확인한다
            if (stack == null || !(stack.getItem() instanceof ItemConfigurator)) {
                return;
            }
            if (tag != null && !tag.hasNoTags()) {
                stack.setTagInfo(FluxUtils.CONFIGS_TAG, sanitize(tag, ItemConfigurator.getConfigs(stack)));
            }
            // 보안: 이름은 최대 24자
            String name = TileFluxCore.limitCustomName(customName);
            if (name.isEmpty()) {
                clearCustomName(stack);
            } else {
                stack.setStackDisplayName(name);
            }
            // 열린 GUI(ContainerCore)에는 슬롯이 없어 손에 든 아이템이 자동 동기화되지 않으므로 직접 보낸다
            player.inventoryContainer.detectAndSendChanges();
        }, ctx);
        return null;
    }

    /**
     * 보안: 클라이언트가 보낸 태그에서 알려진 키·타입만 골라 범위를 보정한다.
     * 네트워크 ID는 클라이언트 값을 버리고 기존 구성기 값을 유지한다 (네트워크 지정은 비밀번호를 확인하는 ID 18로만).
     */
    private static NBTTagCompound sanitize(NBTTagCompound in, NBTTagCompound old) {
        NBTTagCompound out = new NBTTagCompound();
        String networkKey = FluxConfigurationType.NETWORK.getNBTName();
        if (old != null && old.hasKey(networkKey, Constants.NBT.TAG_INT)) {
            out.setInteger(networkKey, old.getInteger(networkKey));
        }
        String key = FluxConfigurationType.PRIORITY.getNBTName();
        if (in.hasKey(key, Constants.NBT.TAG_INT)) {
            out.setInteger(key, in.getInteger(key));
        }
        key = FluxConfigurationType.PRIORITY_SETTING.getNBTName();
        if (in.hasKey(key, Constants.NBT.TAG_BYTE)) {
            out.setBoolean(key, in.getBoolean(key));
        }
        key = FluxConfigurationType.TRANSFER.getNBTName();
        if (in.hasKey(key, Constants.NBT.TAG_LONG)) {
            out.setLong(key, Math.max(0L, in.getLong(key)));
        }
        key = FluxConfigurationType.TRANSFER_SETTING.getNBTName();
        if (in.hasKey(key, Constants.NBT.TAG_BYTE)) {
            out.setBoolean(key, in.getBoolean(key));
        }
        key = FluxConfigurationType.CHUNK_LOADING.getNBTName();
        if (in.hasKey(key, Constants.NBT.TAG_BYTE)) {
            out.setBoolean(key, in.getBoolean(key));
        }
        key = TileFluxPlug.NBT_MAX_INPUT_TIER;
        if (in.hasKey(key, Constants.NBT.TAG_INT)) {
            out.setInteger(key, GTEnergy.clampTier(in.getInteger(key)));
        }
        key = TileFluxPoint.NBT_OUTPUT_TIER;
        if (in.hasKey(key, Constants.NBT.TAG_INT)) {
            out.setInteger(key, GTEnergy.clampTier(in.getInteger(key)));
        }
        key = TileFluxPoint.NBT_OUTPUT_AMPERAGE;
        if (in.hasKey(key, Constants.NBT.TAG_LONG)) {
            out.setLong(key, Math.max(1L, in.getLong(key)));
        }
        return out;
    }

    /** 1.7.10 ItemStack에는 이름 지우기 메서드 이름이 난독화되어 있어 직접 지운다 */
    private static void clearCustomName(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound()
            .hasKey("display", Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound display = stack.getTagCompound()
                .getCompoundTag("display");
            display.removeTag("Name");
            if (display.hasNoTags()) {
                stack.getTagCompound()
                    .removeTag("display");
            }
        }
    }

    public static class ConfiguratorSettingsMessage implements IMessage {

        public String customName;
        public NBTTagCompound tag;

        public ConfiguratorSettingsMessage() {}

        public ConfiguratorSettingsMessage(String customName, NBTTagCompound tag) {
            this.customName = customName;
            this.tag = tag;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            customName = ByteBufUtils.readUTF8String(buf);
            tag = ByteBufUtils.readTag(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, customName);
            ByteBufUtils.writeTag(buf, tag);
        }
    }
}
