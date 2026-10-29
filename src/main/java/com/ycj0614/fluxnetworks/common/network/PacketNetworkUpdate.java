package com.ycj0614.fluxnetworks.common.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

/**
 * Server → Client: 네트워크 정보 갱신 (NBTType에 따라 일부 또는 삭제)
 */
public class PacketNetworkUpdate implements IMessageHandler<PacketNetworkUpdate.NetworkUpdateMessage, IMessage> {

    @Override
    public IMessage onMessage(NetworkUpdateMessage message, MessageContext ctx) {
        final NBTType type = message.type;
        if (ctx.side == Side.CLIENT && type != null) {
            final Map<Integer, NBTTagCompound> updated = message.updatedNetworks;
            PacketHandler.handlePacket(() -> FluxNetworkCache.instance.updateClientFromPacket(updated, type), ctx);
            FluxNetworks.logger
                .info("[H-debug] {} -> client cache {}", type, FluxNetworkCache.instance.networks.keySet());
        }
        return null;
    }

    public static class NetworkUpdateMessage implements IMessage {

        public Map<Integer, NBTTagCompound> updatedNetworks = new HashMap<>();
        /** 잘못된 ordinal을 받으면 null (핸들러가 무시) */
        public NBTType type;

        public NetworkUpdateMessage() {}

        public NetworkUpdateMessage(List<IFluxNetwork> toSend, NBTType type) {
            this.type = type;
            toSend.forEach(n -> {
                NBTTagCompound tag = new NBTTagCompound();
                n.writeNetworkNBT(tag, type);
                if (!tag.hasNoTags()) {
                    updatedNetworks.put(n.getNetworkID(), tag);
                }
            });
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            int typeIndex = buf.readInt();
            NBTType[] types = NBTType.values();
            if (typeIndex < 0 || typeIndex >= types.length) {
                type = null;
                return;
            }
            type = types[typeIndex];
            int size = buf.readInt();
            for (int i = 0; i < size; i++) {
                updatedNetworks.put(buf.readInt(), ByteBufUtils.readTag(buf));
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(type.ordinal());
            buf.writeInt(updatedNetworks.size());
            updatedNetworks.forEach((i, n) -> {
                buf.writeInt(i);
                ByteBufUtils.writeTag(buf, n);
            });
        }
    }
}
