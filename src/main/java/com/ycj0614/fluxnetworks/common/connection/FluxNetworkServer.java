package com.ycj0614.fluxnetworks.common.connection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.api.network.AccessLevel;
import com.ycj0614.fluxnetworks.api.network.FluxLogicType;
import com.ycj0614.fluxnetworks.api.network.NetworkMember;
import com.ycj0614.fluxnetworks.api.network.SecurityType;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.tiles.IFluxPlug;
import com.ycj0614.fluxnetworks.api.tiles.IFluxPoint;
import com.ycj0614.fluxnetworks.common.capabilities.SuperAdminData;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.event.FluxConnectionEvent;

/**
 * This class represents a Flux Network on logical server
 */
public class FluxNetworkServer extends FluxNetworkBase {

    private final Map<FluxLogicType, List<? extends IFluxConnector>> connections = new EnumMap<>(FluxLogicType.class);

    private final Queue<IFluxConnector> toAdd = new LinkedList<>();
    private final Queue<IFluxConnector> toRemove = new LinkedList<>();

    public boolean sortConnections = true;

    private final List<PriorityGroup<IFluxPlug>> sortedPlugs = new ArrayList<>();
    private final List<PriorityGroup<IFluxPoint>> sortedPoints = new ArrayList<>();

    private final TransferIterator<IFluxPlug> plugTransferIterator = new TransferIterator<>(false);
    private final TransferIterator<IFluxPoint> pointTransferIterator = new TransferIterator<>(true);

    public long bufferLimiter = 0;

    public FluxNetworkServer() {
        super();
    }

    public FluxNetworkServer(int id, String name, SecurityType security, int color, UUID owner, String password) {
        super(id, name, security, color, owner, password);
    }

    private void handleConnectionQueue() {
        IFluxConnector device;
        while ((device = toAdd.poll()) != null) {
            for (FluxLogicType type : FluxLogicType.getValidTypes(device)) {
                sortConnections |= FluxUtils.addWithCheck(getConnections(type), device);
            }
            MinecraftForge.EVENT_BUS.post(new FluxConnectionEvent.Connected(device, this));
        }
        while ((device = toRemove.poll()) != null) {
            for (FluxLogicType type : FluxLogicType.getValidTypes(device)) {
                sortConnections |= getConnections(type).remove(device);
            }
        }
        if (sortConnections) {
            sortConnections();
            sortConnections = false;
        }
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends IFluxConnector> List<T> getConnections(FluxLogicType type) {
        return (List<T>) connections.computeIfAbsent(type, m -> new ArrayList<>());
    }

    @Override
    public void onEndServerTick() {
        network_stats.getValue()
            .startProfiling();

        handleConnectionQueue();

        bufferLimiter = 0;

        List<IFluxConnector> devices = getConnections(FluxLogicType.ANY);
        for (IFluxConnector f : devices) {
            f.getTransferHandler()
                .onCycleStart();
        }

        if (!sortedPoints.isEmpty() && !sortedPlugs.isEmpty()) {
            plugTransferIterator.reset(sortedPlugs);
            pointTransferIterator.reset(sortedPoints);
            CYCLE: while (pointTransferIterator.hasNext()) {
                while (plugTransferIterator.hasNext()) {
                    IFluxPlug plug = plugTransferIterator.next();
                    IFluxPoint point = pointTransferIterator.next();
                    if (plug.getConnectionType() == point.getConnectionType()) {
                        break CYCLE; // Storage always have the lowest priority, the cycle can be broken here.
                    }
                    // we don't need to simulate this action
                    long operate = plug.getTransferHandler()
                        .removeFromBuffer(
                            point.getTransferHandler()
                                .getRequest());
                    if (operate > 0) {
                        point.getTransferHandler()
                            .addToBuffer(operate);
                        continue CYCLE;
                    } else {
                        // although the plug still need transfer (buffer > 0)
                        // but it reached max transfer limit, so we use next plug
                        plugTransferIterator.incrementFlux();
                    }
                }
                break; // all plugs have been used
            }
        }
        for (IFluxConnector f : devices) {
            f.getTransferHandler()
                .onCycleEnd();
            bufferLimiter += f.getTransferHandler()
                .getRequest();
        }

        network_stats.getValue()
            .stopProfiling();
    }

    @Override
    public AccessLevel getMemberPermission(EntityPlayer player) {
        if (FluxConfig.enableSuperAdmin && SuperAdminData.isPlayerSuperAdmin(player)) {
            return AccessLevel.SUPER_ADMIN;
        }
        // 원작의 Collectors.toMap(...).getOrDefault(...)와 같은 결과. 단, 같은 UUID가 여러 번 있으면 예외 대신 첫 번째 멤버를 쓴다
        UUID uuid = player.getGameProfile()
            .getId();
        for (NetworkMember member : network_players.getValue()) {
            if (Objects.equals(member.getPlayerUUID(), uuid)) {
                return member.getAccessPermission();
            }
        }
        return network_security.getValue()
            .isEncrypted() ? AccessLevel.NONE : AccessLevel.USER;
    }

    @Override
    public void onRemoved() {
        getConnections(FluxLogicType.ANY)
            .forEach(flux -> MinecraftForge.EVENT_BUS.post(new FluxConnectionEvent.Disconnected(flux, this)));
        connections.clear();
        toAdd.clear();
        toRemove.clear();
        sortedPoints.clear();
        sortedPlugs.clear();
    }

    @Override
    public void queueConnectionAddition(IFluxConnector flux) {
        toAdd.add(flux);
        toRemove.remove(flux);
        addToLite(flux);
    }

    @Override
    public void queueConnectionRemoval(IFluxConnector flux, boolean chunkUnload) {
        toRemove.add(flux);
        toAdd.remove(flux);
        if (chunkUnload) {
            changeChunkLoaded(flux, false);
        } else {
            removeFromLite(flux);
        }
    }

    private void addToLite(IFluxConnector flux) {
        Optional<IFluxConnector> c = all_connectors.getValue()
            .stream()
            .filter(
                f -> f.getCoords()
                    .equals(flux.getCoords()))
            .findFirst();
        if (c.isPresent()) {
            changeChunkLoaded(flux, true);
        } else {
            FluxLiteConnector lite = new FluxLiteConnector(flux);
            all_connectors.getValue()
                .add(lite);
        }
    }

    private void removeFromLite(IFluxConnector flux) {
        all_connectors.getValue()
            .removeIf(
                f -> f.getCoords()
                    .equals(flux.getCoords()));
    }

    private void changeChunkLoaded(IFluxConnector flux, boolean chunkLoaded) {
        Optional<IFluxConnector> c = all_connectors.getValue()
            .stream()
            .filter(
                f -> f.getCoords()
                    .equals(flux.getCoords()))
            .findFirst();
        c.ifPresent(fluxConnector -> fluxConnector.setChunkLoaded(chunkLoaded));
    }

    @Override
    public void addNewMember(String name) {
        NetworkMember a = NetworkMember.createMemberByUsername(name);
        if (network_players.getValue()
            .stream()
            .noneMatch(
                f -> f.getPlayerUUID()
                    .equals(a.getPlayerUUID()))) {
            network_players.getValue()
                .add(a);
        }
    }

    @Override
    public void removeMember(UUID uuid) {
        network_players.getValue()
            .removeIf(
                p -> p.getPlayerUUID()
                    .equals(uuid)
                    && !p.getAccessPermission()
                        .canDelete());
    }

    @Override
    public Optional<NetworkMember> getValidMember(UUID player) {
        return network_players.getValue()
            .stream()
            .filter(
                f -> f.getPlayerUUID()
                    .equals(player))
            .findFirst();
    }

    public void markLiteSettingChanged(IFluxConnector flux) {

    }

    private void sortConnections() {
        sortedPlugs.clear();
        sortedPoints.clear();
        List<IFluxPlug> plugs = getConnections(FluxLogicType.PLUG);
        List<IFluxPoint> points = getConnections(FluxLogicType.POINT);
        plugs.forEach(
            p -> PriorityGroup.getOrCreateGroup(p.getLogicPriority(), sortedPlugs)
                .getConnectors()
                .add(p));
        points.forEach(
            p -> PriorityGroup.getOrCreateGroup(p.getLogicPriority(), sortedPoints)
                .getConnectors()
                .add(p));
        sortedPlugs.sort(Comparator.comparing(p -> -p.getPriority()));
        sortedPoints.sort(Comparator.comparing(p -> -p.getPriority()));
    }
}
