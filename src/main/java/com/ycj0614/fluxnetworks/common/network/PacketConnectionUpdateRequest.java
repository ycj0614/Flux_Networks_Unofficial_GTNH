package com.ycj0614.fluxnetworks.common.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import com.ycj0614.fluxnetworks.api.network.FluxLogicType;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.utils.Coord4D;
import com.ycj0614.fluxnetworks.common.connection.FluxLiteConnector;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Client → Server: 로드된 연결 장치의 최신 정보 요청. 응답은 PacketConnectionUpdate (sendTo)
 */
public class PacketConnectionUpdateRequest
    implements IMessageHandler<PacketConnectionUpdateRequest.ConnectionRequestMessage, IMessage> {

    /** 한 번에 요청할 수 있는 좌표 수. 이보다 많으면 비정상 패킷으로 보고 무시한다 */
    public static final int MAX_COORDS = 1024;

    @Override
    public IMessage onMessage(ConnectionRequestMessage message, MessageContext ctx) {
        if (message.malformed || message.coords.isEmpty()) {
            return null;
        }
        final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        final int networkID = message.networkID;
        final List<Coord4D> coords = message.coords;
        PacketHandler.handlePacket(() -> {
            if (player == null) {
                return;
            }
            IFluxNetwork network = FluxNetworkCache.instance.getNetwork(networkID);
            if (!network.isInvalid()) {
                List<NBTTagCompound> tags = new ArrayList<>();
                List<IFluxConnector> onlineConnectors = network.getConnections(FluxLogicType.ANY);
                coords.forEach(
                    c -> onlineConnectors.stream()
                        .filter(
                            f -> f.getCoords()
                                .equals(c))
                        .findFirst()
                        .ifPresent(f -> tags.add(FluxLiteConnector.writeCustomNBT(f, new NBTTagCompound()))));
                PacketHandler.network
                    .sendTo(new PacketConnectionUpdate.NetworkConnectionMessage(networkID, tags), player);
            }
        }, ctx);
        return null;
    }

    public static class ConnectionRequestMessage implements IMessage {

        public int networkID;
        public List<Coord4D> coords = new ArrayList<>();
        /** 좌표 수가 비정상인 패킷 (핸들러가 무시) */
        private boolean malformed = false;

        public ConnectionRequestMessage() {}

        public ConnectionRequestMessage(int networkID, List<Coord4D> coords) {
            this.networkID = networkID;
            this.coords = coords;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            networkID = buf.readInt();
            int size = buf.readInt();
            // Coord4D 하나는 int 4개 = 16바이트
            if (size < 0 || size > MAX_COORDS || (long) size * 16 > buf.readableBytes()) {
                malformed = true;
                return;
            }
            for (int i = 0; i < size; i++) {
                coords.add(new Coord4D(buf));
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(networkID);
            buf.writeInt(coords.size());
            coords.forEach(c -> c.write(buf));
        }
    }
}
