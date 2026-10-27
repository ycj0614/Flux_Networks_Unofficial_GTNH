package com.ycj0614.fluxnetworks.common.connection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.api.network.AccessLevel;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.NetworkMember;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.network.SecurityType;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.utils.Coord4D;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.data.FluxNetworkData;

/**
 * Cached all flux networks
 */
public class FluxNetworkCache {

    public static FluxNetworkCache instance = new FluxNetworkCache();

    /** Client Cache **/
    public Map<Integer, IFluxNetwork> networks = new HashMap<>();
    public boolean superAdminClient = false;

    public void clearNetworks() {
        FluxNetworkData.clear();
    }

    public void clearClientCache() {
        networks.clear();
        superAdminClient = false;
    }

    public boolean hasSpaceLeft(EntityPlayer player) {
        if (FluxConfig.maximumPerPlayer == -1) {
            return true;
        }
        UUID uuid = player.getGameProfile()
            .getId();
        List<IFluxNetwork> created = getAllNetworks().stream()
            .filter(
                s -> s.getSetting(NetworkSettings.NETWORK_OWNER)
                    .equals(uuid))
            .collect(Collectors.toList());
        return created.size() < FluxConfig.maximumPerPlayer;
    }

    public IFluxNetwork createdNetwork(EntityPlayer player, String name, int color, SecurityType securityType,
        String password) {
        UUID uuid = player.getGameProfile()
            .getId();

        NetworkMember owner = NetworkMember.createNetworkMember(player, AccessLevel.OWNER);
        FluxNetworkServer network = new FluxNetworkServer(getUniqueID(), name, securityType, color, uuid, password);
        network.getSetting(NetworkSettings.NETWORK_PLAYERS)
            .add(owner);

        FluxNetworkData.get()
            .addNetwork(network);
        return network;
    }

    private int getUniqueID() {
        return FluxNetworkData.get().uniqueID++;
    }

    // 2단계 패킷에서 사용
    /** Client Only **/
    public void updateClientFromPacket(Map<Integer, NBTTagCompound> serverSideNetworks, NBTType type) {
        serverSideNetworks.forEach((i, n) -> {
            IFluxNetwork network = networks.get(i);
            if (type == NBTType.NETWORK_CLEAR) {
                if (network != null) {
                    networks.remove(i);
                    return;
                }
            }
            if (network == null) {
                network = new FluxLiteNetwork();
                network.readNetworkNBT(n, type);
                networks.put(i, network);
            } else {
                network.readNetworkNBT(n, type);
            }
        });
    }

    // 2단계 패킷에서 사용
    public void updateClientConnections(int networkID, List<NBTTagCompound> tags) {
        IFluxNetwork network = networks.get(networkID);
        if (network != null) {
            List<IFluxConnector> connectors = network.getSetting(NetworkSettings.ALL_CONNECTORS);
            tags.forEach(t -> {
                Coord4D coord4D = new Coord4D(t);
                connectors.stream()
                    .filter(
                        f -> f.getCoords()
                            .equals(coord4D))
                    .findFirst()
                    .ifPresent(f -> f.readCustomNBT(t, NBTType.DEFAULT));
            });
        }
    }

    /** Server Only **/
    public IFluxNetwork getNetwork(int id) {
        return FluxNetworkData.get().networks.getOrDefault(id, FluxNetworkInvalid.instance);
    }

    /** Server Only **/
    public Collection<IFluxNetwork> getAllNetworks() {
        return FluxNetworkData.get().networks.values();
    }

    /** Client Only **/
    public IFluxNetwork getClientNetwork(int id) {
        return networks.getOrDefault(id, FluxNetworkInvalid.instance);
    }

    /** Client Only **/
    public List<IFluxNetwork> getAllClientNetworks() {
        return new ArrayList<>(networks.values());
    }
}
