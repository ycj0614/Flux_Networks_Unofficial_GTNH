package com.ycj0614.fluxnetworks.common.network;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Server → Client: 이 클라이언트 플레이어의 SuperAdmin 상태. GUI 반영은 ClientProxy.onSuperAdminChanged가 맡는다
 */
public class PacketSuperAdmin implements IMessageHandler<PacketSuperAdmin.SuperAdminMessage, IMessage> {

    @Override
    public IMessage onMessage(SuperAdminMessage message, MessageContext ctx) {
        final boolean superAdmin = message.superAdmin;
        PacketHandler.handlePacket(() -> {
            FluxNetworkCache.instance.superAdminClient = superAdmin;
            if (PacketHandler.getPlayer(ctx) != null) {
                FluxNetworks.proxy.onSuperAdminChanged();
            }
        }, ctx);
        return null;
    }

    public static class SuperAdminMessage implements IMessage {

        public boolean superAdmin;

        public SuperAdminMessage() {}

        public SuperAdminMessage(boolean superAdmin) {
            this.superAdmin = superAdmin;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            superAdmin = buf.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeBoolean(superAdmin);
        }
    }
}
