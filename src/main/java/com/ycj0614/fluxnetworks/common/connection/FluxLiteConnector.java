package com.ycj0614.fluxnetworks.common.connection;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.api.network.ConnectionType;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.ITransferHandler;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.tiles.IFluxPlug;
import com.ycj0614.fluxnetworks.api.tiles.IFluxPoint;
import com.ycj0614.fluxnetworks.api.utils.Coord4D;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPlug;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPoint;

/**
 * 언로드된 청크에 있는 커넥터 정보의 스냅샷
 */
public class FluxLiteConnector implements IFluxConnector {

    public int networkID;
    public int priority;
    public UUID playerUUID;
    public ConnectionType connectionType;
    public long limit;
    public Coord4D coord4D;
    public int folderID;
    public String customName;
    public boolean surgeMode;
    public boolean disableLimit;
    public boolean isChunkLoaded;
    public boolean chunkLoading;
    public long buffer;
    public long change;
    // 원작에 없음: GT EU 설정 (연결 탭 표시·편집용). 플러그는 최대 입력 티어, 포인트는 출력 티어와 출력 암페어. 그 밖에는 0
    public int tier;
    public long amperage;
    // 원작과 다름: 원작은 표시용 ItemStack(tile.getDisplayStack())도 저장했다.
    // 여기서는 IFluxConnector.getDisplayStack()이 연결 종류로 아이콘을 정하므로 저장하지 않는다

    public FluxLiteConnector(IFluxConnector tile) {
        this.networkID = tile.getNetworkID();
        this.priority = tile.getRawPriority();
        this.playerUUID = tile.getConnectionOwner();
        this.connectionType = tile.getConnectionType();
        this.limit = tile.getRawLimit();
        this.coord4D = tile.getCoords();
        this.folderID = tile.getFolderID();
        this.customName = tile.getCustomName();
        this.surgeMode = tile.getSurgeMode();
        this.disableLimit = tile.getDisableLimit();
        this.isChunkLoaded = tile.isChunkLoaded();
        this.buffer = tile.getTransferHandler()
            .getBuffer();
        this.change = tile.getTransferHandler()
            .getChange();
        this.chunkLoading = tile.isForcedLoading();
        this.tier = getEUTier(tile);
        this.amperage = getEUAmperage(tile);
    }

    public FluxLiteConnector(NBTTagCompound tag) {
        readCustomNBT(tag, NBTType.ALL_SAVE);
    }

    public static NBTTagCompound writeCustomNBT(IFluxConnector tile, NBTTagCompound tag) {
        tile.getCoords()
            .write(tag);
        tag.setInteger(
            "type",
            tile.getConnectionType()
                .ordinal());
        tag.setInteger("n_id", tile.getNetworkID());
        tag.setInteger("priority", tile.getRawPriority());
        tag.setInteger("folder_id", tile.getFolderID());
        tag.setLong("limit", tile.getRawLimit());
        tag.setString("name", tile.getCustomName());
        tag.setBoolean("dLimit", tile.getDisableLimit());
        tag.setBoolean("surge", tile.getSurgeMode());
        tag.setBoolean("isChunkLoaded", tile.isChunkLoaded());
        tag.setLong("buffer", tile.getTransferBuffer());
        tag.setLong("change", tile.getTransferChange());
        tag.setBoolean("forcedChunk", tile.isForcedLoading());
        writeEUSettings(tag, tile.getConnectionType(), getEUTier(tile), getEUAmperage(tile));
        return tag;
    }

    /** 플러그는 최대 입력 티어, 포인트는 출력 티어. 그 밖에는 0 (스토리지는 IFluxPlug·IFluxPoint를 둘 다 상속하므로 연결 종류로 가린다) */
    public static int getEUTier(IFluxConnector tile) {
        if (tile instanceof FluxLiteConnector) {
            return ((FluxLiteConnector) tile).tier;
        }
        if (tile.getConnectionType()
            .isPlug() && tile instanceof IFluxPlug) {
            return ((IFluxPlug) tile).getMaxInputTier();
        }
        if (tile.getConnectionType()
            .isPoint() && tile instanceof IFluxPoint) {
            return ((IFluxPoint) tile).getOutputTier();
        }
        return 0;
    }

    /** 포인트의 출력 암페어. 그 밖에는 0 */
    public static long getEUAmperage(IFluxConnector tile) {
        if (tile instanceof FluxLiteConnector) {
            return ((FluxLiteConnector) tile).amperage;
        }
        if (tile.getConnectionType()
            .isPoint() && tile instanceof IFluxPoint) {
            return ((IFluxPoint) tile).getOutputAmperage();
        }
        return 0;
    }

    /** 키는 장치 NBT 키와 같다 (플러그 "mt", 포인트 "ot"·"oa") */
    private static void writeEUSettings(NBTTagCompound tag, ConnectionType type, int tier, long amperage) {
        if (type.isPlug()) {
            tag.setInteger(TileFluxPlug.NBT_MAX_INPUT_TIER, tier);
        } else if (type.isPoint()) {
            tag.setInteger(TileFluxPoint.NBT_OUTPUT_TIER, tier);
            tag.setLong(TileFluxPoint.NBT_OUTPUT_AMPERAGE, amperage);
        }
    }

    @Override
    public NBTTagCompound writeCustomNBT(NBTTagCompound tag, NBTType type) {
        coord4D.write(tag);
        tag.setInteger("type", connectionType.ordinal());
        tag.setInteger("n_id", networkID);
        tag.setInteger("priority", priority);
        tag.setInteger("folder_id", folderID);
        tag.setLong("limit", limit);
        tag.setString("name", customName);
        tag.setBoolean("dLimit", disableLimit);
        tag.setBoolean("surge", surgeMode);
        tag.setBoolean("isChunkLoaded", isChunkLoaded);
        tag.setLong("buffer", buffer);
        tag.setLong("change", change);
        tag.setBoolean("forcedChunk", chunkLoading);
        writeEUSettings(tag, connectionType, tier, amperage);
        return tag;
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag, NBTType type) {
        coord4D = new Coord4D(tag);
        connectionType = ConnectionType.values()[tag.getInteger("type")];
        networkID = tag.getInteger("n_id");
        priority = tag.getInteger("priority");
        folderID = tag.getInteger("folder_id");
        limit = tag.getLong("limit");
        customName = tag.getString("name");
        disableLimit = tag.getBoolean("dLimit");
        surgeMode = tag.getBoolean("surge");
        isChunkLoaded = tag.getBoolean("isChunkLoaded");
        buffer = tag.getLong("buffer");
        change = tag.getLong("change");
        chunkLoading = tag.getBoolean("forcedChunk");
        tier = tag.hasKey(TileFluxPlug.NBT_MAX_INPUT_TIER) ? tag.getInteger(TileFluxPlug.NBT_MAX_INPUT_TIER)
            : tag.getInteger(TileFluxPoint.NBT_OUTPUT_TIER);
        amperage = tag.getLong(TileFluxPoint.NBT_OUTPUT_AMPERAGE);
    }

    @Override
    public int getNetworkID() {
        return networkID;
    }

    @Override
    public int getLogicPriority() {
        return priority;
    }

    @Override
    public int getRawPriority() {
        return priority;
    }

    @Override
    public IFluxNetwork getNetwork() {
        return FluxNetworkInvalid.instance;
    }

    @Override
    public void open(EntityPlayer player) {}

    @Override
    public void close(EntityPlayer player) {}

    @Override
    public UUID getConnectionOwner() {
        return playerUUID;
    }

    @Override
    public ConnectionType getConnectionType() {
        return connectionType;
    }

    @Override
    public boolean canAccess(EntityPlayer player) {
        return false;
    }

    @Override
    public boolean isChunkLoaded() {
        return isChunkLoaded;
    }

    @Override
    public boolean isForcedLoading() {
        return chunkLoading;
    }

    @Override
    public void connect(IFluxNetwork network) {}

    @Override
    public void disconnect(IFluxNetwork network) {}

    @Override
    public ITransferHandler getTransferHandler() {
        return null;
    }

    @Override
    public World getFluxWorld() {
        return null;
    }

    @Override
    public long getLogicLimit() {
        return limit;
    }

    @Override
    public long getRawLimit() {
        return limit;
    }

    @Override
    public long getMaxTransferLimit() {
        return 0;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public Coord4D getCoords() {
        return coord4D;
    }

    @Override
    public int getFolderID() {
        return folderID;
    }

    @Override
    public String getCustomName() {
        return customName;
    }

    @Override
    public boolean getDisableLimit() {
        return disableLimit;
    }

    @Override
    public boolean getSurgeMode() {
        return surgeMode;
    }

    @Override
    public long getTransferBuffer() {
        return buffer;
    }

    @Override
    public long getTransferChange() {
        return change;
    }

    @Override
    public void setChunkLoaded(boolean chunkLoaded) {
        isChunkLoaded = chunkLoaded;
    }
}
