package com.ycj0614.fluxnetworks.api.network;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import com.google.common.base.Charsets;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.FMLCommonHandler;

public class NetworkMember {

    private UUID playerUUID;
    private String cachedName;
    private AccessLevel accessPermission;

    NetworkMember() {}

    public NetworkMember(NBTTagCompound nbt) {
        readNetworkNBT(nbt);
    }

    public static NetworkMember createNetworkMember(EntityPlayer player, AccessLevel permissionLevel) {
        NetworkMember t = new NetworkMember();
        GameProfile profile = player.getGameProfile();

        t.playerUUID = profile.getId();
        t.cachedName = profile.getName();
        t.accessPermission = permissionLevel;

        return t;
    }

    public static NetworkMember createMemberByUsername(String username) {
        NetworkMember t = new NetworkMember();
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        boolean isOffline = !server.isServerInOnlineMode();

        if (!isOffline) {
            // func_152358_ax() = getPlayerProfileCache(), func_152655_a(String) = getGameProfileForUsername(String)
            GameProfile profile = server.func_152358_ax()
                .func_152655_a(username);
            if (profile != null) {
                t.playerUUID = profile.getId();
            } else {
                isOffline = true;
            }
        }
        if (isOffline) {
            t.playerUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(Charsets.UTF_8));
        }
        t.cachedName = username;
        t.accessPermission = AccessLevel.USER;
        return t;
    }

    public String getCachedName() {
        return cachedName;
    }

    public AccessLevel getAccessPermission() {
        return accessPermission;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setAccessPermission(AccessLevel accessPermission) {
        this.accessPermission = accessPermission;
    }

    public void readNetworkNBT(NBTTagCompound nbt) {
        playerUUID = new UUID(nbt.getLong("playerUUIDMost"), nbt.getLong("playerUUIDLeast"));
        cachedName = nbt.getString("cachedName");
        accessPermission = AccessLevel.values()[nbt.getByte("playerAccess")];
    }

    public NBTTagCompound writeNetworkNBT(NBTTagCompound nbt) {
        nbt.setLong("playerUUIDMost", playerUUID.getMostSignificantBits());
        nbt.setLong("playerUUIDLeast", playerUUID.getLeastSignificantBits());
        nbt.setString("cachedName", cachedName);
        nbt.setByte("playerAccess", (byte) accessPermission.ordinal());
        return nbt;
    }
}
