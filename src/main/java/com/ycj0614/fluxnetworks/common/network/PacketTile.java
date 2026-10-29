package com.ycj0614.fluxnetworks.common.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 장치 GUI의 장치 대상 명령 (클라이언트 → 서버, ID 5). 네트워크 연결, 청크 로딩.
 */
public class PacketTile implements IMessageHandler<PacketTile.TileMessage, IMessage> {

    /** 장치 대상 패킷을 받는 최대 거리의 제곱 (8블록, 블록 중심 기준) */
    public static final double MAX_DISTANCE_SQ = 64.0D;

    @Override
    public IMessage onMessage(TileMessage message, MessageContext ctx) {
        final PacketTileType type = message.handler;
        final NBTTagCompound tag = message.tag;
        if (type == null || tag == null) {
            return null; // 패킷 규칙 4
        }
        final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null) {
            return null;
        }
        final int x = message.x;
        final int y = message.y;
        final int z = message.z;
        final int dimension = message.dimension;
        PacketHandler.handlePacket(() -> {
            // 보안: 플레이어의 현재 차원이 아닌 장치는 무시 (원작의 다른 차원 월드 조회는 삭제)
            if (player.worldObj == null || player.worldObj.provider.dimensionId != dimension) {
                return;
            }
            TileFluxCore tile = getAccessibleTile(player, x, y, z);
            if (tile == null) {
                return;
            }
            IMessage returned = type.handler.handlePacket(tile, player, tag);
            if (returned != null) {
                PacketHandler.network.sendTo(returned, player);
            }
        }, ctx);
        return null;
    }

    /**
     * 서버 방향 장치 패킷(PacketTile, PacketByteBuf)의 공통 검사. 서버 메인 스레드에서만 호출한다.
     *
     * @return 플레이어의 현재 월드에 있고, 8블록 이내이며, 플레이어가 접근할 수 있는 TileFluxCore. 아니면 null
     */
    static TileFluxCore getAccessibleTile(EntityPlayerMP player, int x, int y, int z) {
        if (player.worldObj == null) {
            return null;
        }
        // 보안: 플레이어와 블록 중심 거리의 제곱이 64 이하(8블록)일 때만 처리
        double dx = x + 0.5D - player.posX;
        double dy = y + 0.5D - player.posY;
        double dz = z + 0.5D - player.posZ;
        if (dx * dx + dy * dy + dz * dz > MAX_DISTANCE_SQ) {
            return null;
        }
        // 보안: 플레이어의 현재 월드에서만 타일을 찾는다
        TileEntity te = player.worldObj.getTileEntity(x, y, z);
        if (!(te instanceof TileFluxCore) || te.isInvalid()) {
            return null;
        }
        TileFluxCore tile = (TileFluxCore) te;
        // 보안: 장치에 접근할 수 있는 플레이어(tile.canAccess)만
        if (!tile.canAccess(player)) {
            return null;
        }
        return tile;
    }

    public static class TileMessage implements IMessage {

        public PacketTileType handler;
        public NBTTagCompound tag;
        public int x;
        public int y;
        public int z;
        /** 형식 유지를 위해 남긴다. 서버는 플레이어의 현재 차원과 다르면 패킷을 무시한다 */
        public int dimension;

        public TileMessage() {}

        public TileMessage(PacketTileType handler, NBTTagCompound tag, int x, int y, int z, int dimension) {
            this.handler = handler;
            this.tag = tag;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            x = buf.readInt();
            y = buf.readInt();
            z = buf.readInt();
            dimension = buf.readInt();
            handler = PacketTileType.fromOrdinal(buf.readInt());
            tag = ByteBufUtils.readTag(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
            buf.writeInt(dimension);
            buf.writeInt(handler.ordinal());
            ByteBufUtils.writeTag(buf, tag);
        }
    }
}
