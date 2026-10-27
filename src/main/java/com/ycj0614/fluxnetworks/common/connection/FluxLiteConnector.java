package com.ycj0614.fluxnetworks.common.connection;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.api.network.ConnectionType;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.ITransferHandler;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.utils.Coord4D;
import com.ycj0614.fluxnetworks.api.utils.NBTType;

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
    // TODO 2단계: 표시용 ItemStack 저장 (1.7.10은 ItemStack.loadItemStackFromNBT)
    // (원작: ItemStack stack 필드에 tile.getDisplayStack()을 담고 같은 태그에 writeToNBT / new ItemStack(tag)로 저장·복원,
    // getDisplayStack()으로 GUI에 아이콘 표시)

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
        // TODO 2단계: 표시용 ItemStack 저장 (원작: this.stack = tile.getDisplayStack())
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
        // TODO 2단계: 표시용 ItemStack 저장 (원작: tile.getDisplayStack().writeToNBT(tag))
        return tag;
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
        // TODO 2단계: 표시용 ItemStack 저장 (원작: stack.writeToNBT(tag))
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
        // TODO 2단계: 표시용 ItemStack 저장 (원작: stack = new ItemStack(tag) → 1.7.10은 ItemStack.loadItemStackFromNBT(tag))
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
