package com.ycj0614.fluxnetworks.common.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.tiles.ITileByteBuf;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 장치 GUI의 개별 설정 변경 (ID 3 서버 방향, ID 4 클라이언트 방향). 값은 ITileByteBuf.writePacket/readPacket이 쓰고 읽는다.
 * ID 표는 TileFluxCore.writePacket 위 주석 참조.
 */
public class PacketByteBuf implements IMessageHandler<PacketByteBuf.ByteBufMessage, IMessage> {

    @Override
    public IMessage onMessage(ByteBufMessage message, final MessageContext ctx) {
        final ByteBuf data = message.buf;
        if (data == null) {
            return null;
        }
        final int x = message.x;
        final int y = message.y;
        final int z = message.z;
        final int id = message.id;
        final boolean server = ctx.side.isServer();
        final EntityPlayerMP serverPlayer = server ? ctx.getServerHandler().playerEntity : null;
        if (server && serverPlayer == null) {
            data.release();
            return null;
        }

        boolean scheduled = false;
        try {
            PacketHandler.handlePacket(() -> {
                try {
                    ITileByteBuf tile = server ? findServerTarget(serverPlayer, x, y, z)
                        : findClientTarget(ctx, x, y, z);
                    if (tile != null) {
                        readSafely(tile, data, id, x, y, z);
                    }
                } finally {
                    // fromBytes의 retain()과 짝. 타일이 없거나 보안 검사에 실패해도 반드시 해제
                    data.release();
                }
            }, ctx);
            scheduled = true;
        } finally {
            if (!scheduled) {
                data.release();
            }
        }
        return null;
    }

    private static ITileByteBuf findServerTarget(EntityPlayerMP player, int x, int y, int z) {
        // 보안: 같은 차원(플레이어의 현재 월드)·8블록 이내·canAccess 검사 (PacketTile.getAccessibleTile)
        // 보안: canAccess를 확인할 수 있는 TileFluxCore(ITileByteBuf)만 대상. 그 밖의 타일은 무시
        return PacketTile.getAccessibleTile(player, x, y, z);
    }

    private static ITileByteBuf findClientTarget(MessageContext ctx, int x, int y, int z) {
        EntityPlayer player = FluxNetworks.proxy.getPlayer(ctx);
        if (player == null || player.worldObj == null) {
            return null;
        }
        TileEntity te = player.worldObj.getTileEntity(x, y, z);
        return te instanceof ITileByteBuf ? (ITileByteBuf) te : null;
    }

    private static void readSafely(ITileByteBuf tile, ByteBuf data, int id, int x, int y, int z) {
        try {
            tile.readPacket(data, id);
        } catch (RuntimeException e) {
            // 보안: 내용이 모자라거나 잘못된 버퍼가 메인 스레드 작업에서 예외로 번지지 않게 한다
            FluxNetworks.logger
                .warn("Ignored malformed PacketByteBuf (id " + id + ") at " + x + ", " + y + ", " + z + ": " + e);
        }
    }

    public static class ByteBufMessage implements IMessage {

        /** 보내는 쪽에서만 쓴다 (toBytes에서 writePacket 호출) */
        public ITileByteBuf tile;
        public int x;
        public int y;
        public int z;
        public int id;
        /** 받는 쪽에서만 쓴다. fromBytes에서 retain(), 처리 작업의 finally에서 release() */
        public ByteBuf buf;

        public ByteBufMessage() {}

        public ByteBufMessage(ITileByteBuf tile, int x, int y, int z, int id) {
            this.tile = tile;
            this.x = x;
            this.y = y;
            this.z = z;
            this.id = id;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            x = buf.readInt();
            y = buf.readInt();
            z = buf.readInt();
            id = buf.readInt();
            this.buf = buf.retain();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
            buf.writeInt(id);
            tile.writePacket(buf, id);
        }
    }
}
