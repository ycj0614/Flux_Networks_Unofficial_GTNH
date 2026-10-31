package com.ycj0614.fluxnetworks.common.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;

/**
 * 원작 FluxChunkManager. 차원마다 티켓 하나로 장치 청크를 강제 로딩한다.
 * 강제 로딩 중인 청크 목록은 FluxNetworkData.loadedChunks에 저장되고, 월드 로드 때 callback에서 다시 건다.
 * 한 청크에는 로더 하나만 허용한다 (이미 있으면 forceChunk가 false).
 */
public class FluxChunkManager {

    public static final Map<Integer, ForgeChunkManager.Ticket> worldTickets = new HashMap<>();

    public static void clear() {
        worldTickets.clear();
    }

    /** x, z는 블록 좌표 */
    public static ChunkCoordIntPair getChunk(int x, int z) {
        return new ChunkCoordIntPair(x >> 4, z >> 4);
    }

    public static boolean forceChunk(World world, ChunkCoordIntPair chunk) {
        int dim = world.provider.dimensionId;
        List<ChunkCoordIntPair> chunks = FluxNetworkData.get().loadedChunks
            .computeIfAbsent(dim, l -> new ArrayList<>());
        if (chunks.contains(chunk)) {
            return false;
        }
        ForgeChunkManager.Ticket ticket = worldTickets.get(dim);
        if (ticket == null) {
            ticket = ForgeChunkManager.requestTicket(FluxNetworks.instance, world, ForgeChunkManager.Type.NORMAL);
            // 원작과 다름: 티켓 한도를 넘으면 null이 오므로 실패로 처리한다
            if (ticket == null) {
                return false;
            }
            worldTickets.put(dim, ticket);
        }
        ForgeChunkManager.forceChunk(ticket, chunk);
        chunks.add(chunk);
        return true;
    }

    public static void releaseChunk(World world, ChunkCoordIntPair chunk) {
        int dim = world.provider.dimensionId;
        ForgeChunkManager.Ticket ticket = worldTickets.get(dim);
        if (ticket == null) {
            return;
        }
        List<ChunkCoordIntPair> chunks = FluxNetworkData.get().loadedChunks.get(dim);
        int count = 0;
        if (chunks != null) {
            for (ChunkCoordIntPair c : chunks) {
                if (c.equals(chunk)) {
                    count++;
                }
            }
        }
        if (count > 0) {
            chunks.remove(chunk);
            if (count <= 1) {
                ForgeChunkManager.unforceChunk(ticket, chunk);
            }
        }
        if (ticket.getChunkList()
            .isEmpty()) {
            worldTickets.remove(dim);
            ForgeChunkManager.releaseTicket(ticket);
        }
    }

    /**
     * 장치 청크의 강제 로딩을 걸거나 풀고 tile.chunkLoading을 맞춘다.
     * FluxConfig.enableChunkLoading과 스토리지 여부 확인은 호출하는 쪽에서 한다.
     *
     * @return 거는 경우 성공 여부 (그 청크에 이미 로더가 있으면 false). 푸는 경우 항상 true
     */
    public static boolean setChunkLoading(TileFluxCore tile, boolean load) {
        ChunkCoordIntPair chunk = getChunk(tile.xCoord, tile.zCoord);
        if (load) {
            tile.chunkLoading = forceChunk(tile.getWorldObj(), chunk);
            return tile.chunkLoading;
        }
        releaseChunk(tile.getWorldObj(), chunk);
        tile.chunkLoading = false;
        return true;
    }

    /** ForgeChunkManager.LoadingCallback (월드 로드 때 저장된 티켓을 돌려받는다) */
    public static void callback(List<ForgeChunkManager.Ticket> tickets, World world) {
        if (tickets.isEmpty()) {
            return;
        }
        int dim = world.provider.dimensionId;
        ForgeChunkManager.Ticket ticket = tickets.get(0); // 차원마다 티켓은 하나
        // 원작과 다름: putIfAbsent 대신 put. 차원이 다시 로드되면 이전 월드의 낡은 티켓을 덮어쓴다
        worldTickets.put(dim, ticket);
        if (FluxConfig.enableChunkLoading) {
            List<ChunkCoordIntPair> chunks = FluxNetworkData.get().loadedChunks.get(dim);
            if (chunks != null) {
                for (ChunkCoordIntPair pos : chunks) {
                    ForgeChunkManager.forceChunk(ticket, pos);
                }
            }
        }
    }

    /** 원작과 다름: 차원이 언로드되면 그 월드의 티켓을 맵에서 지운다 (CommonProxy.onWorldUnload) */
    public static void onWorldUnload(World world) {
        ForgeChunkManager.Ticket ticket = worldTickets.get(world.provider.dimensionId);
        if (ticket != null && ticket.world == world) {
            worldTickets.remove(world.provider.dimensionId);
        }
    }
}
