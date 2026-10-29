package com.ycj0614.fluxnetworks.common.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import com.ycj0614.fluxnetworks.common.handler.PacketHandler;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * GUI 조작 패킷 (클라이언트 → 서버, ID 1). 네트워크 생성·편집·삭제·멤버 권한·무선 설정.
 */
public class PacketGeneral implements IMessageHandler<PacketGeneral.GeneralMessage, IMessage> {

    @Override
    public IMessage onMessage(GeneralMessage message, MessageContext ctx) {
        final PacketGeneralType type = message.handler;
        final NBTTagCompound tag = message.nbtTag;
        // 패킷 규칙 4: 범위를 벗어난 ordinal(fromBytes에서 null)이나 읽지 못한 태그는 무시
        if (type == null || tag == null) {
            return null;
        }
        final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null) {
            return null;
        }
        PacketHandler.handlePacket(() -> {
            IMessage returned = type.handler.handlePacket(player, tag);
            if (returned != null) {
                PacketHandler.network.sendTo(returned, player);
            }
        }, ctx);
        return null;
    }

    public static class GeneralMessage implements IMessage {

        public PacketGeneralType handler;
        public NBTTagCompound nbtTag;

        public GeneralMessage() {}

        public GeneralMessage(PacketGeneralType handler, NBTTagCompound nbtTag) {
            this.handler = handler;
            this.nbtTag = nbtTag;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            handler = PacketGeneralType.fromOrdinal(buf.readInt());
            nbtTag = ByteBufUtils.readTag(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(handler.ordinal());
            ByteBufUtils.writeTag(buf, nbtTag);
        }
    }
}
