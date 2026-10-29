package com.ycj0614.fluxnetworks.common.network;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.network.AccessLevel;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Server → Client: 열린 GUI의 접근 권한. GUI 반영은 ClientProxy.setGuiAccessPermission이 맡는다
 * (이 클래스는 데디케이티드 서버에서도 로드되므로 클라이언트 클래스를 참조하지 않는다)
 */
public class PacketGUIPermission implements IMessageHandler<PacketGUIPermission.GUIPermissionMessage, IMessage> {

    @Override
    public IMessage onMessage(GUIPermissionMessage message, MessageContext ctx) {
        final AccessLevel level = message.accessPermission;
        if (level == null) {
            return null;
        }
        PacketHandler.handlePacket(() -> {
            if (PacketHandler.getPlayer(ctx) != null) {
                FluxNetworks.proxy.setGuiAccessPermission(level);
            }
        }, ctx);
        return null;
    }

    public static class GUIPermissionMessage implements IMessage {

        /** 잘못된 ordinal을 받으면 null (핸들러가 무시) */
        public AccessLevel accessPermission;

        public GUIPermissionMessage() {}

        public GUIPermissionMessage(AccessLevel permission) {
            this.accessPermission = permission;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            int index = buf.readInt();
            AccessLevel[] values = AccessLevel.values();
            accessPermission = index >= 0 && index < values.length ? values[index] : null;
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(accessPermission.ordinal());
        }
    }
}
