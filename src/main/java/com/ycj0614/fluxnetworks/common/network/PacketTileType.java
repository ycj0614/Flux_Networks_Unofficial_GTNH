package com.ycj0614.fluxnetworks.common.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;

import cpw.mods.fml.common.network.simpleimpl.IMessage;

public enum PacketTileType {

    SET_NETWORK(PacketTileHandler::handleSetNetworkPacket),
    CHUNK_LOADING(PacketTileHandler::handleChunkLoadPacket);

    private static final PacketTileType[] VALUES = values();

    public IPacketTileHandler handler;

    PacketTileType(IPacketTileHandler handler) {
        this.handler = handler;
    }

    /** 범위를 벗어나면 null (패킷 규칙 4) */
    public static PacketTileType fromOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null;
    }

    public interface IPacketTileHandler {

        /** 서버 메인 스레드에서 보안 검사를 통과한 타일로 호출된다 */
        IMessage handlePacket(TileFluxCore tile, EntityPlayer player, NBTTagCompound nbtTag);
    }
}
