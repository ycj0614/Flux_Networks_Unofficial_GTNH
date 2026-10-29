package com.ycj0614.fluxnetworks.common.network;

import java.util.HashMap;
import java.util.Map;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Server → Client (ID 8): 네트워크 ID → (색, 이름) 캐시. 아이템 툴팁 등에서 쓴다.
 * 원작은 net.minecraft.util.Tuple을 썼으나 여기서는 {@link Entry}를 쓴다.
 */
public class PacketColorCache implements IMessageHandler<PacketColorCache.ColorCacheMessage, IMessage> {

    /** 한 패킷에 담는 최대 항목 수. 넘으면 패킷을 무시한다 */
    public static final int MAX_ENTRIES = 1024;

    @Override
    public IMessage onMessage(ColorCacheMessage message, MessageContext ctx) {
        final Map<Integer, Entry> cache = message.cache;
        if (cache == null) {
            return null;
        }
        PacketHandler.handlePacket(() -> FluxNetworks.proxy.receiveColorCache(cache), ctx);
        return null;
    }

    /** 네트워크 하나의 색(알파 포함)과 이름 */
    public static class Entry {

        public final int color;
        public final String name;

        public Entry(int color, String name) {
            this.color = color;
            this.name = name;
        }

        /** 원작 PacketColorRequest와 같은 값: 색 | 0xff000000, 잘못된 네트워크면 이름 "NONE" */
        public static Entry of(IFluxNetwork network) {
            return new Entry(
                network.getSetting(NetworkSettings.NETWORK_COLOR) | 0xff000000,
                network.isInvalid() ? "NONE" : network.getSetting(NetworkSettings.NETWORK_NAME));
        }
    }

    public static class ColorCacheMessage implements IMessage {

        /** fromBytes에서 항목 수가 범위를 벗어나면 null */
        public Map<Integer, Entry> cache;

        public ColorCacheMessage() {}

        public ColorCacheMessage(Map<Integer, Entry> cache) {
            this.cache = cache;
        }

        /** 네트워크 하나만 담은 메시지 (네트워크 편집 시 전체 전송용) */
        public static ColorCacheMessage of(IFluxNetwork network) {
            Map<Integer, Entry> cache = new HashMap<>();
            cache.put(network.getNetworkID(), Entry.of(network));
            return new ColorCacheMessage(cache);
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            int size = buf.readInt();
            if (size < 0 || size > MAX_ENTRIES) {
                cache = null;
                return;
            }
            cache = new HashMap<>();
            for (int i = 0; i < size; i++) {
                int id = buf.readInt();
                int color = buf.readInt();
                String name = ByteBufUtils.readUTF8String(buf);
                cache.put(id, new Entry(color, name));
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(cache.size());
            for (Map.Entry<Integer, Entry> e : cache.entrySet()) {
                buf.writeInt(e.getKey());
                buf.writeInt(e.getValue().color);
                ByteBufUtils.writeUTF8String(buf, e.getValue().name);
            }
        }
    }
}
