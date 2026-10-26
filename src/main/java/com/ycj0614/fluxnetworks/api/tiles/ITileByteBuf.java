package com.ycj0614.fluxnetworks.api.tiles;

import io.netty.buffer.ByteBuf;

public interface ITileByteBuf {

    void writePacket(ByteBuf buf, int id);

    void readPacket(ByteBuf buf, int id);
}
