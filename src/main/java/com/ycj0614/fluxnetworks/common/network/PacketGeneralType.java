package com.ycj0614.fluxnetworks.common.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.simpleimpl.IMessage;

public enum PacketGeneralType {

    CREATE_NETWORK(PacketGeneralHandler::handleCreateNetworkPacket),
    EDIT_NETWORK(PacketGeneralHandler::handleNetworkEditPacket),
    DELETE_NETWORK(PacketGeneralHandler::handleDeleteNetworkPacket),
    /*
     * ADD_MEMBER(PacketGeneralHandler::handleAddMemberPacket),
     * REMOVE_MEMBER(PacketGeneralHandler::handleRemoveMemberPacket),
     */
    CHANGE_PERMISSION(PacketGeneralHandler::handleChangePermissionPacket),
    /** 4단계(무선) 기능. 패킷만 옮기고 GUI에서는 숨긴다 */
    CHANGE_WIRELESS(PacketGeneralHandler::handleChangeWirelessPacket);

    private static final PacketGeneralType[] VALUES = values();

    public IPacketGeneralHandler handler;

    PacketGeneralType(IPacketGeneralHandler handler) {
        this.handler = handler;
    }

    /** 범위를 벗어나면 null (패킷 규칙 4) */
    public static PacketGeneralType fromOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null;
    }

    public interface IPacketGeneralHandler {

        /** 서버 메인 스레드에서 호출된다. 반환값은 PacketGeneral이 요청자에게 sendTo로 보낸다 */
        IMessage handlePacket(EntityPlayer player, NBTTagCompound nbtTag);
    }
}
