package com.ycj0614.fluxnetworks.common.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

/**
 * Server → Client: 네트워크 연결 장치 정보 갱신
 */
public class PacketConnectionUpdate
    implements IMessageHandler<PacketConnectionUpdate.NetworkConnectionMessage, IMessage> {

    @Override
    public IMessage onMessage(NetworkConnectionMessage message, MessageContext ctx) {
        if (ctx.side == Side.CLIENT) {
            final int networkID = message.networkID;
            final List<NBTTagCompound> tags = message.tags;
            PacketHandler.handlePacket(() -> FluxNetworkCache.instance.updateClientConnections(networkID, tags), ctx);
        }
        return null;
    }

    public static class NetworkConnectionMessage implements IMessage {

        public int networkID;
        public List<NBTTagCompound> tags = new ArrayList<>();

        public NetworkConnectionMessage() {}

        public NetworkConnectionMessage(int networkID, List<NBTTagCompound> tags) {
            this.networkID = networkID;
            this.tags = tags;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            networkID = buf.readInt();
            int size = buf.readInt();
            for (int i = 0; i < size; i++) {
                tags.add(ByteBufUtils.readTag(buf));
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(networkID);
            buf.writeInt(tags.size());
            tags.forEach(t -> ByteBufUtils.writeTag(buf, t));
        }
    }
}
