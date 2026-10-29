package com.ycj0614.fluxnetworks.common.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.ycj0614.fluxnetworks.common.capabilities.SuperAdminData;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Client → Server: SuperAdmin 켜기/끄기 요청. 바뀌었으면 PacketSuperAdmin (sendTo)
 */
public class PacketActivateSuperAdmin
    implements IMessageHandler<PacketActivateSuperAdmin.ActivateSuperAdminMessage, IMessage> {

    @Override
    public IMessage onMessage(ActivateSuperAdminMessage message, MessageContext ctx) {
        final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        PacketHandler.handlePacket(() -> {
            if (player == null) {
                return;
            }
            SuperAdminData superAdmin = SuperAdminData.get(player);
            if (superAdmin != null) {
                if (superAdmin.getPermission() || SuperAdminData.canActivateSuperAdmin(player)) {
                    superAdmin.changePermission();
                    PacketHandler.network
                        .sendTo(new PacketSuperAdmin.SuperAdminMessage(superAdmin.getPermission()), player);
                }
            }
        }, ctx);
        return null;
    }

    public static class ActivateSuperAdminMessage implements IMessage {

        public ActivateSuperAdminMessage() {}

        @Override
        public void fromBytes(ByteBuf buf) {}

        @Override
        public void toBytes(ByteBuf buf) {}
    }
}
