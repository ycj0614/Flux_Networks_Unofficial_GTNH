package com.ycj0614.fluxnetworks.common.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.network.AccessLevel;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.NetworkMember;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.capabilities.SuperAdminData;
import com.ycj0614.fluxnetworks.common.connection.FluxLiteConnector;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkServer;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.network.PacketNetworkUpdate;

/**
 * Save network data to local. Only on server side
 */
public class FluxNetworkData extends WorldSavedData {

    private static final String NETWORK_DATA = FluxNetworks.MODID + "data";

    private static FluxNetworkData data;

    public static final String NETWORKS = "networks";
    public static final String LOADED_CHUNKS = "loadedChunks";
    public static final String UNIQUE_ID = "uniqueID";

    public static final String NETWORK_ID = "networkID";
    public static final String NETWORK_NAME = "networkName";
    public static final String NETWORK_COLOR = "networkColor";
    public static final String NETWORK_PASSWORD = "networkPassword";
    public static final String SECURITY_TYPE = "networkSecurity";
    public static final String OWNER_UUID = "ownerUUID";
    public static final String WIRELESS_MODE = "wirelessMode";

    public static final String PLAYER_LIST = "playerList";
    public static final String NETWORK_FOLDERS = "folders";
    public static final String UNLOADED_CONNECTIONS = "unloaded";

    public Map<Integer, IFluxNetwork> networks = new HashMap<>();
    // Forced Chunks. 3단계(청크 로딩)에서 사용하며, 저장 형식은 지금부터 유지한다
    public Map<Integer, List<ChunkCoordIntPair>> loadedChunks = new HashMap<>();

    public int uniqueID = 1;

    // MapStorage.loadData()가 리플렉션으로 (String) 생성자를 호출하므로 반드시 public 유지
    public FluxNetworkData(String name) {
        super(name);
    }

    public FluxNetworkData() {
        this(NETWORK_DATA);
    }

    public static void clear() {
        if (data != null) {
            data = null;
            FluxNetworks.logger.info("FluxNetworkData has been unloaded");
        }
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    public static FluxNetworkData get() {
        if (data == null) {
            loadData();
        }
        return data;
    }

    private static void loadData() {
        World world = DimensionManager.getWorld(0);
        if (world == null) {
            FluxNetworks.logger.error(
                "Tried to load FluxNetworkData but the overworld is not available (server not started or called from client side)");
            throw new IllegalStateException("FluxNetworkData can only be loaded on the server side");
        }

        // 1.7.10: 모든 차원이 공유하는 저장소
        MapStorage mapStorage = world.mapStorage;
        FluxNetworkData savedData = (FluxNetworkData) mapStorage.loadData(FluxNetworkData.class, NETWORK_DATA);

        if (savedData == null) {
            savedData = new FluxNetworkData(NETWORK_DATA);
            mapStorage.setData(NETWORK_DATA, savedData);
            FluxNetworks.logger.info("No FluxNetworkData found");
        }
        data = savedData;
        FluxNetworks.logger.info("FluxNetworkData has been successfully loaded");
    }

    /** 새 네트워크를 추가하고 모든 클라이언트에 알린다 */
    public void addNetwork(IFluxNetwork network) {
        addNetwork(network, true);
    }

    /**
     * @param sendPacket false면 패킷을 보내지 않는다. readFromNBT(월드 로드 중)에서 쓴다. 로드 시점에는 접속자가 없고,
     *                   플레이어가 접속하면 CommonProxy.onPlayerJoined가 전체 네트워크를 보낸다
     */
    public void addNetwork(IFluxNetwork network, boolean sendPacket) {
        networks.putIfAbsent(network.getNetworkID(), network);
        if (sendPacket) {
            PacketHandler.network.sendToAll(
                new PacketNetworkUpdate.NetworkUpdateMessage(
                    Collections.singletonList(network),
                    NBTType.NETWORK_GENERAL));
        }
    }

    public void removeNetwork(IFluxNetwork network) {
        PacketHandler.network.sendToAll(
            new PacketNetworkUpdate.NetworkUpdateMessage(Collections.singletonList(network), NBTType.NETWORK_CLEAR));
        network.onRemoved();
        networks.remove(network.getNetworkID());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        uniqueID = nbt.getInteger(UNIQUE_ID);
        if (nbt.hasKey(NETWORKS)) {
            NBTTagList list = nbt.getTagList(NETWORKS, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                FluxNetworkServer network = new FluxNetworkServer();
                network.readNetworkNBT(tag, NBTType.ALL_SAVE);
                addNetwork(network, false);
            }
        }
        readChunks(nbt);
        data = this;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        compound.setInteger(UNIQUE_ID, uniqueID);

        NBTTagList list = new NBTTagList();
        // 원작은 FluxNetworkCache.instance.getAllNetworks()를 거쳤지만, 그 경로가 다시 FluxNetworkData.get()을
        // 호출하므로 저장 중 재귀 로딩을 피하려고 자기 맵을 직접 순회한다
        for (IFluxNetwork network : networks.values()) {
            NBTTagCompound tag = new NBTTagCompound();
            network.writeNetworkNBT(tag, NBTType.ALL_SAVE);
            list.appendTag(tag);
        }
        compound.setTag(NETWORKS, list);

        NBTTagCompound tag = new NBTTagCompound();
        loadedChunks.forEach((dim, pos) -> writeChunks(dim, pos, tag));
        compound.setTag(LOADED_CHUNKS, tag);
    }

    public static void readPlayers(IFluxNetwork network, @Nonnull NBTTagCompound nbt) {
        if (!nbt.hasKey(PLAYER_LIST)) {
            return;
        }
        List<NetworkMember> a = new ArrayList<>();
        NBTTagList list = nbt.getTagList(PLAYER_LIST, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound c = list.getCompoundTagAt(i);
            a.add(new NetworkMember(c));
        }
        network.setSetting(NetworkSettings.NETWORK_PLAYERS, a);
    }

    public static NBTTagCompound writePlayers(IFluxNetwork network, @Nonnull NBTTagCompound nbt) {
        List<NetworkMember> a = network.getSetting(NetworkSettings.NETWORK_PLAYERS);
        if (!a.isEmpty()) {
            NBTTagList list = new NBTTagList();
            a.forEach(s -> list.appendTag(s.writeNetworkNBT(new NBTTagCompound())));
            nbt.setTag(PLAYER_LIST, list);
        }
        return nbt;
    }

    public static void writeAllPlayers(IFluxNetwork network, @Nonnull NBTTagCompound nbt) {
        List<NetworkMember> a = network.getSetting(NetworkSettings.NETWORK_PLAYERS);
        NBTTagList list = new NBTTagList();
        if (!a.isEmpty()) {
            a.forEach(s -> list.appendTag(s.writeNetworkNBT(new NBTTagCompound())));
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null) {
            // 1.7.10: playerEntityList는 raw List
            List<?> players = server.getConfigurationManager().playerEntityList;
            for (Object o : players) {
                EntityPlayerMP p = (EntityPlayerMP) o;
                UUID uuid = p.getUniqueID();
                if (a.stream()
                    .noneMatch(
                        s -> s.getPlayerUUID()
                            .equals(uuid))) {
                    list.appendTag(
                        NetworkMember.createNetworkMember(p, getPermission(p))
                            .writeNetworkNBT(new NBTTagCompound()));
                }
            }
        }
        nbt.setTag(PLAYER_LIST, list);
    }

    private static AccessLevel getPermission(EntityPlayer player) {
        return SuperAdminData.isPlayerSuperAdmin(player) ? AccessLevel.SUPER_ADMIN : AccessLevel.NONE;
    }

    public static void readConnections(IFluxNetwork network, @Nonnull NBTTagCompound nbt) {
        if (!nbt.hasKey(UNLOADED_CONNECTIONS)) {
            return;
        }
        List<IFluxConnector> a = new ArrayList<>();
        NBTTagList list = nbt.getTagList(UNLOADED_CONNECTIONS, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            a.add(new FluxLiteConnector(list.getCompoundTagAt(i)));
        }
        network.getSetting(NetworkSettings.ALL_CONNECTORS)
            .addAll(a);
    }

    public static NBTTagCompound writeConnections(IFluxNetwork network, @Nonnull NBTTagCompound nbt) {
        List<IFluxConnector> a = network.getSetting(NetworkSettings.ALL_CONNECTORS);
        if (!a.isEmpty()) {
            NBTTagList list = new NBTTagList();
            a.forEach(s -> {
                if (!s.isChunkLoaded()) {
                    list.appendTag(s.writeCustomNBT(new NBTTagCompound(), NBTType.DEFAULT));
                }
            });
            nbt.setTag(UNLOADED_CONNECTIONS, list);
        }
        return nbt;
    }

    public static void readAllConnections(IFluxNetwork network, @Nonnull NBTTagCompound nbt) {
        if (!nbt.hasKey(UNLOADED_CONNECTIONS)) {
            return;
        }
        List<IFluxConnector> a = new ArrayList<>();
        NBTTagList list = nbt.getTagList(UNLOADED_CONNECTIONS, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            a.add(new FluxLiteConnector(list.getCompoundTagAt(i)));
        }
        network.setSetting(NetworkSettings.ALL_CONNECTORS, a);
    }

    public static NBTTagCompound writeAllConnections(IFluxNetwork network, @Nonnull NBTTagCompound nbt) {
        List<IFluxConnector> a = network.getSetting(NetworkSettings.ALL_CONNECTORS);
        if (!a.isEmpty()) {
            NBTTagList list = new NBTTagList();
            a.forEach(s -> list.appendTag(s.writeCustomNBT(new NBTTagCompound(), NBTType.DEFAULT)));
            nbt.setTag(UNLOADED_CONNECTIONS, list);
        }
        return nbt;
    }

    private void readChunks(NBTTagCompound nbt) {
        if (!nbt.hasKey(LOADED_CHUNKS)) {
            return;
        }
        NBTTagCompound tags = nbt.getCompoundTag(LOADED_CHUNKS);
        // TODO 확인: func_150296_c() = getKeySet() (1.7.10 미매핑 이름, 반환형 raw Set의 원소는 String)
        for (Object o : tags.func_150296_c()) {
            String key = (String) o;
            NBTTagList list = tags.getTagList(key, Constants.NBT.TAG_COMPOUND);
            List<ChunkCoordIntPair> pos = loadedChunks.computeIfAbsent(Integer.valueOf(key), l -> new ArrayList<>());
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                pos.add(new ChunkCoordIntPair(tag.getInteger("x"), tag.getInteger("z")));
            }
        }
    }

    private NBTTagCompound writeChunks(int dim, List<ChunkCoordIntPair> pos, NBTTagCompound nbt) {
        if (!pos.isEmpty()) {
            NBTTagList list = new NBTTagList();
            pos.forEach(p -> {
                NBTTagCompound t = new NBTTagCompound();
                t.setInteger("x", p.chunkXPos);
                t.setInteger("z", p.chunkZPos);
                list.appendTag(t);
            });
            nbt.setTag(String.valueOf(dim), list);
        }
        return nbt;
    }
}
