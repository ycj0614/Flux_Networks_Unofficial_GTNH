package com.ycj0614.fluxnetworks.common.network;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import com.ycj0614.fluxnetworks.api.network.AccessLevel;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Client → Server: GUI 접근 권한 요청. 응답은 PacketGUIPermission (sendTo)
 * <p>
 * 원작과 다른 점: 원작은 클라이언트가 보낸 UUID로 플레이어를 찾아 그 권한을 돌려준다. 이 백포트는 다른 플레이어의 권한을
 * 조회하지 못하도록 항상 <b>패킷을 보낸 플레이어 자신</b>의 권한을 계산한다. 메시지 형식(networkID + UUID)은 원작과 같게 두고 UUID
 * 값은 무시한다.
 */
public class PacketPermissionRequest
    implements IMessageHandler<PacketPermissionRequest.PermissionRequestMessage, IMessage> {

    @Override
    public IMessage onMessage(PermissionRequestMessage message, MessageContext ctx) {
        if (message.malformed) {
            return null;
        }
        final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        final int networkID = message.networkID;
        // message.uuid는 사용하지 않는다 (위 설명 참조)
        PacketHandler.handlePacket(() -> {
            if (player == null) {
                return;
            }
            IFluxNetwork network = FluxNetworkCache.instance.getNetwork(networkID);
            AccessLevel level = network.isInvalid() ? AccessLevel.NONE : network.getMemberPermission(player);
            PacketHandler.network.sendTo(new PacketGUIPermission.GUIPermissionMessage(level), player);
        }, ctx);
        return null;
    }

    public static class PermissionRequestMessage implements IMessage {

        public int networkID;
        public UUID uuid;
        /** 길이가 모자란 패킷 (핸들러가 무시) */
        private boolean malformed = false;

        public PermissionRequestMessage() {}

        public PermissionRequestMessage(int networkID, UUID uuid) {
            this.networkID = networkID;
            this.uuid = uuid;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            // int 1개 + long 2개
            if (buf.readableBytes() < 20) {
                malformed = true;
                return;
            }
            networkID = buf.readInt();
            uuid = new UUID(buf.readLong(), buf.readLong());
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(networkID);
            // 서버가 UUID를 쓰지 않으므로 null이면 0으로 채운다 (형식은 원작과 같음)
            buf.writeLong(uuid == null ? 0L : uuid.getMostSignificantBits());
            buf.writeLong(uuid == null ? 0L : uuid.getLeastSignificantBits());
        }
    }
}
