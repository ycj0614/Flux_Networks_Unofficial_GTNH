package com.ycj0614.fluxnetworks.common.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.utils.FluxConfigurationType;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.item.ItemConfigurator;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Client → Server (ID 18): 손에 든 구성기의 네트워크를 정한다. 접근 권한이 없으면 비밀번호를 확인한다.
 * 응답은 PacketFeedback (SUCCESS / PASSWORD_REQUIRE / REJECT)
 */
public class PacketSetConfiguratorNetwork
    implements IMessageHandler<PacketSetConfiguratorNetwork.SetConfiguratorNetworkMessage, IMessage> {

    @Override
    public IMessage onMessage(SetConfiguratorNetworkMessage message, MessageContext ctx) {
        final int id = message.id;
        // 보안: 비밀번호는 최대 16자로 자른다
        final String password = PacketGeneralHandler
            .truncate(message.password, PacketGeneralHandler.MAX_PASSWORD_LENGTH);
        final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null) {
            return null;
        }
        PacketHandler.handlePacket(() -> {
            ItemStack stack = player.getHeldItem();
            // 보안: 서버에서 손에 든 아이템이 구성기인지 다시 확인한다 (원작은 확인 전에 권한부터 보고 SUCCESS를 보냈다)
            if (stack == null || !(stack.getItem() instanceof ItemConfigurator)) {
                return;
            }
            IFluxNetwork network = FluxNetworkCache.instance.getNetwork(id);
            if (network.isInvalid()) {
                return;
            }
            if (!network.getMemberPermission(player)
                .canAccess()) {
                if (password.isEmpty()) {
                    PacketHandler.network
                        .sendTo(new PacketFeedback.FeedbackMessage(EnumFeedbackInfo.PASSWORD_REQUIRE), player);
                    return;
                }
                if (!password.equals(network.getSetting(NetworkSettings.NETWORK_PASSWORD))) {
                    PacketHandler.network.sendTo(new PacketFeedback.FeedbackMessage(EnumFeedbackInfo.REJECT), player);
                    return;
                }
            }
            NBTTagCompound configs = ItemConfigurator.getConfigs(stack);
            if (configs == null) {
                configs = new NBTTagCompound();
                stack.setTagInfo(FluxUtils.CONFIGS_TAG, configs);
            }
            configs.setInteger(FluxConfigurationType.NETWORK.getNBTName(), id);
            // 열린 GUI(ContainerCore)에는 슬롯이 없어 손에 든 아이템이 자동 동기화되지 않으므로 직접 보낸다
            player.inventoryContainer.detectAndSendChanges();
            PacketHandler.network.sendTo(new PacketFeedback.FeedbackMessage(EnumFeedbackInfo.SUCCESS), player);
        }, ctx);
        return null;
    }

    public static class SetConfiguratorNetworkMessage implements IMessage {

        public int id;
        public String password;

        public SetConfiguratorNetworkMessage() {}

        public SetConfiguratorNetworkMessage(int id, String password) {
            this.id = id;
            this.password = password;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            id = buf.readInt();
            password = ByteBufUtils.readUTF8String(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(id);
            ByteBufUtils.writeUTF8String(buf, password);
        }
    }
}
