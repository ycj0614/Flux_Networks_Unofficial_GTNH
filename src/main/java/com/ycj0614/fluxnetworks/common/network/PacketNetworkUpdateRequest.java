package com.ycj0614.fluxnetworks.common.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;

import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Client → Server: 네트워크 정보 요청. 응답은 PacketNetworkUpdate (sendTo)
 */
public class PacketNetworkUpdateRequest
    implements IMessageHandler<PacketNetworkUpdateRequest.UpdateRequestMessage, IMessage> {

    @Override
    public IMessage onMessage(UpdateRequestMessage message, MessageContext ctx) {
        if (message.malformed || message.type == null) {
            return null;
        }
        final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        final List<Integer> ids = message.networks;
        final NBTType type = message.type;
        PacketHandler.handlePacket(() -> {
            if (player == null) {
                return;
            }
            List<IFluxNetwork> networks = new ArrayList<>();
            for (Integer i : ids) {
                IFluxNetwork network = FluxNetworkCache.instance.getNetwork(i);
                if (!network.isInvalid()) {
                    networks.add(network);
                }
            }
            if (!networks.isEmpty()) {
                PacketHandler.network.sendTo(new PacketNetworkUpdate.NetworkUpdateMessage(networks, type), player);
            }
        }, ctx);
        return null;
    }

    public static class UpdateRequestMessage implements IMessage {

        public List<Integer> networks = new ArrayList<>();
        /** 잘못된 ordinal을 받으면 null (핸들러가 무시) */
        public NBTType type;
        /** 길이가 맞지 않는 패킷 (핸들러가 무시) */
        private boolean malformed = false;

        public UpdateRequestMessage() {}

        public UpdateRequestMessage(int networkID, NBTType type) {
            this.networks.add(networkID);
            this.type = type;
        }

        public UpdateRequestMessage(List<IFluxNetwork> networks, NBTType type) {
            networks.forEach(n -> this.networks.add(n.getNetworkID()));
            this.type = type;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            int size = buf.readInt();
            // 네트워크 ID(int) size개 + NBTType(int) 1개가 남아 있어야 한다
            if (size < 0 || (long) size * 4 + 4 > buf.readableBytes()) {
                malformed = true;
                return;
            }
            for (int i = 0; i < size; i++) {
                networks.add(buf.readInt());
            }
            int typeIndex = buf.readInt();
            NBTType[] types = NBTType.values();
            type = typeIndex >= 0 && typeIndex < types.length ? types[typeIndex] : null;
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(networks.size());
            networks.forEach(buf::writeInt);
            buf.writeInt(type.ordinal());
        }
    }
}
