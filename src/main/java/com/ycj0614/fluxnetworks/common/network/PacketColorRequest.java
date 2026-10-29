package com.ycj0614.fluxnetworks.common.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;

import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Client → Server (ID 7): 네트워크 ID 목록의 색·이름 요청. 응답은 PacketColorCache (sendTo)
 */
public class PacketColorRequest implements IMessageHandler<PacketColorRequest.ColorRequestMessage, IMessage> {

    @Override
    public IMessage onMessage(ColorRequestMessage message, MessageContext ctx) {
        final List<Integer> requests = message.requests;
        if (requests == null || requests.isEmpty()) {
            return null;
        }
        final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null) {
            return null;
        }
        PacketHandler.handlePacket(() -> {
            Map<Integer, PacketColorCache.Entry> cache = new HashMap<>();
            for (int id : requests) {
                cache.put(id, PacketColorCache.Entry.of(FluxNetworkCache.instance.getNetwork(id)));
            }
            PacketHandler.network.sendTo(new PacketColorCache.ColorCacheMessage(cache), player);
        }, ctx);
        return null;
    }

    public static class ColorRequestMessage implements IMessage {

        /** fromBytes에서 개수가 범위를 벗어나면 null */
        public List<Integer> requests;

        public ColorRequestMessage() {}

        public ColorRequestMessage(List<Integer> requests) {
            this.requests = requests;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            int size = buf.readInt();
            // 보안: 1024개를 넘는 요청은 무시한다
            if (size < 0 || size > PacketColorCache.MAX_ENTRIES) {
                requests = null;
                return;
            }
            requests = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                requests.add(buf.readInt());
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(requests.size());
            for (int id : requests) {
                buf.writeInt(id);
            }
        }
    }
}
