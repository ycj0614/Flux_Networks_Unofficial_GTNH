package com.ycj0614.fluxnetworks.api.tiles;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.api.network.ConnectionType;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.ITransferHandler;
import com.ycj0614.fluxnetworks.api.utils.Coord4D;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;

/**
 * Extended by IFluxPoint and IFluxPlug
 */
public interface IFluxConnector extends INetworkConnector {

    NBTTagCompound writeCustomNBT(NBTTagCompound tag, NBTType type);

    void readCustomNBT(NBTTagCompound tag, NBTType type);

    int getLogicPriority();

    int getRawPriority(); // ignore surge

    UUID getConnectionOwner();

    ConnectionType getConnectionType();

    boolean canAccess(EntityPlayer player);

    long getLogicLimit();

    long getRawLimit(); // ignore disable limit

    /**
     * If this device is storage, this method returns the max energy storage of it,
     * or Long.MAX_VALUE otherwise
     *
     * @return max transfer limit
     */
    long getMaxTransferLimit();

    boolean isActive();

    boolean isChunkLoaded();

    boolean isForcedLoading();

    void connect(IFluxNetwork network);

    void disconnect(IFluxNetwork network);

    ITransferHandler getTransferHandler();

    World getFluxWorld();

    Coord4D getCoords();

    int getFolderID();

    String getCustomName();

    boolean getDisableLimit();

    boolean getSurgeMode();

    /**
     * Transfer handler is unavailable on client, this method is mainly used for gui display on client
     * If this device is storage, this method returns the energy stored of it
     *
     * @return internal buffer or energy stored
     */
    long getTransferBuffer();

    long getTransferChange();

    default void setChunkLoaded(boolean chunkLoaded) {}

    /**
     * GUI 연결 목록에 그릴 아이콘. 원작과 다름: 원작은 언로드된 장치의 표시용 ItemStack을 NBT로 저장했지만,
     * 여기서는 연결 종류로만 정한다 (버퍼는 getTransferBuffer()의 마지막 값으로 표시)
     */
    default ItemStack getDisplayStack() {
        return FluxUtils.getDisplayStack(getConnectionType());
    }
}
