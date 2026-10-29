package com.ycj0614.fluxnetworks.common.tileentity;

import java.util.HashSet;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.NetworkFolder;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConfigurable;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.tiles.ITileByteBuf;
import com.ycj0614.fluxnetworks.api.utils.Coord4D;
import com.ycj0614.fluxnetworks.api.utils.FluxConfigurationType;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkInvalid;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkServer;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.data.FluxNetworkData;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public abstract class TileFluxCore extends TileEntity implements IFluxConnector, IFluxConfigurable, ITileByteBuf {

    // TILE_DROP NBT 키 (원작 ItemFluxConnector의 상수와 같은 값). 2단계의 아이템 클래스가 이 상수를 참조한다.
    public static final String NBT_CUSTOM_NAME = "customName";
    public static final String NBT_PRIORITY = "priority";
    public static final String NBT_SURGE_MODE = "surgeMode";
    public static final String NBT_LIMIT = "limit";
    public static final String NBT_DISABLE_LIMIT = "disableLimit";

    /** 장치 이름 최대 길이 (원작 GUI 입력칸 setMaxStringLength(24)) */
    public static final int MAX_CUSTOM_NAME_LENGTH = 24;

    /** 인접 타일을 다시 확인하는 주기 (틱). GT 케이블도 20틱마다 연결을 다시 확인한다. */
    public static final int TRANSFER_UPDATE_INTERVAL = 20;

    /** 네트워크에 연결되지 않았을 때의 색 */
    public static final int DISCONNECTED_COLOR = 0xb2b2b2;

    public HashSet<EntityPlayer> playerUsing = new HashSet<>();

    public String customName = "";
    public int networkID = -1;
    public UUID playerUUID = FluxUtils.UUID_DEFAULT;
    public int color = -1;
    public int folderID = -1;

    public int priority = 0;
    public long limit = FluxConfig.defaultLimit;

    public boolean surgeMode = false;
    public boolean disableLimit = false;

    public boolean connected = false;
    public byte[] connections = new byte[] { 0, 0, 0, 0, 0, 0 };

    public boolean chunkLoading = false;

    protected IFluxNetwork network = FluxNetworkInvalid.instance;

    protected boolean load = false;

    /** 주기적 updateTransfers용 카운터 (서버 전용, 저장하지 않음) */
    protected int transferUpdateTimer = 0;

    public Coord4D coord4D;

    /** null이면 빈 문자열, 24자를 넘으면 앞 24자만 남긴다 */
    public static String limitCustomName(String name) {
        if (name == null) {
            return "";
        }
        return name.length() > MAX_CUSTOM_NAME_LENGTH ? name.substring(0, MAX_CUSTOM_NAME_LENGTH) : name;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (worldObj != null && !worldObj.isRemote && load) {
            FluxUtils.removeConnection(this, false);
            // TODO 3단계: 원작의 FluxChunkManager.releaseChunk(world, chunkPos) (chunkLoading이면 이 타일 청크의 강제 로딩 해제)
            getTransferHandler().reset();
            load = false;
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (worldObj != null && !worldObj.isRemote && load) {
            FluxUtils.removeConnection(this, true);
            getTransferHandler().reset();
            load = false;
        }
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        if (!playerUsing.isEmpty()) {
            sendPackets();
        }
        if (!load) {
            // 1.7.10에는 onLoad가 없으므로 첫 서버 틱에서 연결한다
            if (!FluxUtils.addConnection(this)) {
                networkID = -1;
                connected = false;
                color = DISCONNECTED_COLOR;
            }
            updateTransfers(ForgeDirection.VALID_DIRECTIONS);
            sendPackets();
            load = true;
            transferUpdateTimer = 0;
        } else if (++transferUpdateTimer >= TRANSFER_UPDATE_INTERVAL) {
            // 인접 청크가 로드되지 않아 끊었던 연결을 다시 잡는다 (FluxPlugHandler.updateSidedTransfers의 청크 가드)
            transferUpdateTimer = 0;
            updateTransfers(ForgeDirection.VALID_DIRECTIONS);
        }
    }

    @Override
    public void connect(IFluxNetwork network) {
        this.network = network;
        this.networkID = network.getNetworkID();
        this.color = network.getSetting(NetworkSettings.NETWORK_COLOR);
        connected = true;
        getTransferHandler().reset();
        sendPackets();
    }

    @Override
    public void disconnect(IFluxNetwork network) {
        if (network.getNetworkID() == getNetworkID()) {
            this.network = FluxNetworkInvalid.instance;
            this.networkID = -1;
            this.color = DISCONNECTED_COLOR;
            connected = false;
            getTransferHandler().reset();
            sendPackets();
        }
    }

    @Override
    public IFluxNetwork getNetwork() {
        return network;
    }

    @Override
    public int getNetworkID() {
        return networkID;
    }

    @Override
    public Packet getDescriptionPacket() {
        return new S35PacketUpdateTileEntity(
            xCoord,
            yCoord,
            zCoord,
            0,
            writeCustomNBT(new NBTTagCompound(), NBTType.TILE_UPDATE));
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        // TODO 확인: S35PacketUpdateTileEntity의 NBT getter 이름 (1.7.10 MCP: func_148857_g, 1.8+의 getNbtCompound)
        NBTTagCompound tag = pkt.func_148857_g();
        if (tag == null) {
            return;
        }
        readCustomNBT(tag, NBTType.TILE_UPDATE);
        if (worldObj != null) {
            worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
        }
    }

    public void sendPackets() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        writeCustomNBT(compound, NBTType.ALL_SAVE);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        readCustomNBT(compound, NBTType.ALL_SAVE);
    }

    /** 타일이 다루는 NBT 종류인지 (ALL_SAVE, TILE_UPDATE, TILE_DROP) */
    protected static boolean isTileNBT(NBTType type) {
        return type == NBTType.ALL_SAVE || type == NBTType.TILE_UPDATE || type == NBTType.TILE_DROP;
    }

    @Override
    public NBTTagCompound writeCustomNBT(NBTTagCompound tag, NBTType type) {
        if (type == NBTType.ALL_SAVE || type == NBTType.TILE_UPDATE) {
            tag.setInteger("0", priority);
            tag.setLong("1", limit);
            tag.setBoolean("2", disableLimit);
            tag.setBoolean("3", surgeMode);
            tag.setInteger("4", networkID);
            UUID uuid = playerUUID != null ? playerUUID : FluxUtils.UUID_DEFAULT;
            tag.setLong("5Most", uuid.getMostSignificantBits());
            tag.setLong("5Least", uuid.getLeastSignificantBits());
            tag.setString("6", customName);
            tag.setInteger("7", color);
            tag.setBoolean("8", connected);
            tag.setInteger("9", folderID);
            for (int i = 0; i < connections.length; i++) {
                tag.setByte('c' + String.valueOf(i), connections[i]);
            }
            tag.setBoolean("l", chunkLoading);
        }
        if (type == NBTType.TILE_DROP) {
            tag.setInteger(NBT_PRIORITY, priority);
            tag.setLong(NBT_LIMIT, limit);
            tag.setBoolean(NBT_DISABLE_LIMIT, disableLimit);
            tag.setBoolean(NBT_SURGE_MODE, surgeMode);
            tag.setInteger(FluxNetworkData.NETWORK_ID, networkID);
            tag.setString(NBT_CUSTOM_NAME, customName);
            tag.setInteger(NetworkFolder.FOLDER_ID, folderID);
        }
        getTransferHandler().writeCustomNBT(tag, type);
        return tag;
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag, NBTType type) {
        if (type == NBTType.ALL_SAVE || type == NBTType.TILE_UPDATE) {
            priority = tag.getInteger("0");
            limit = tag.getLong("1");
            disableLimit = tag.getBoolean("2");
            surgeMode = tag.getBoolean("3");
            networkID = tag.getInteger("4");
            if (tag.hasKey("5Most") && tag.hasKey("5Least")) {
                playerUUID = new UUID(tag.getLong("5Most"), tag.getLong("5Least"));
            }
            customName = tag.getString("6");
            color = tag.getInteger("7");
            connected = tag.getBoolean("8");
            folderID = tag.getInteger("9");
            for (int i = 0; i < connections.length; i++) {
                connections[i] = tag.getByte('c' + String.valueOf(i));
            }
            chunkLoading = tag.getBoolean("l");
        }
        if (type == NBTType.TILE_DROP) {
            priority = tag.getInteger(NBT_PRIORITY);
            long l;
            limit = (l = tag.getLong(NBT_LIMIT)) > 0 ? l : limit;
            disableLimit = tag.getBoolean(NBT_DISABLE_LIMIT);
            surgeMode = tag.getBoolean(NBT_SURGE_MODE);
            int i;
            networkID = (i = tag.getInteger(FluxNetworkData.NETWORK_ID)) > 0 ? i : networkID;
            String name;
            customName = (name = tag.getString(NBT_CUSTOM_NAME)).isEmpty() ? customName : name;
            folderID = tag.getInteger(NetworkFolder.FOLDER_ID);
        }
        getTransferHandler().readCustomNBT(tag, type);
    }

    @Override
    public boolean canAccess(EntityPlayer player) {
        if (!network.isInvalid()) {
            UUID id = player.getGameProfile()
                .getId();
            if (playerUUID != null && playerUUID.equals(id)) {
                return true;
            }
            return network.getMemberPermission(player)
                .canAccess();
        }
        return true;
    }

    /*
     * ITileByteBuf ID 표 (PacketByteBuf, 배치 L의 GUI가 참조)
     * ID | 값 | 형식 | 대상
     * 1 | 이름 | UTF8 | 모든 장치. 서버는 최대 24자로 자른다
     * 2 | 우선순위 | int | 모든 장치. 연결 정렬 다시 함
     * 3 | 한도 | long | 모든 장치. 음수면 0. 포인트는 출력 한도로 쓰지 않음 (출력 = V×A)
     * 4 | 서지 모드 | boolean | 모든 장치. 연결 정렬 다시 함
     * 5 | 한도 해제 | boolean | 모든 장치. 포인트는 쓰지 않음
     * 6 | 티어 | int | 플러그 maxInputTier, 포인트 outputTier (TileFluxPlug / TileFluxPoint)
     * 7 | 출력 암페어 | long | 포인트만 (TileFluxPoint). 1 미만은 1
     * 대상 장치가 다루지 않는 ID는 아무것도 쓰거나 읽지 않는다.
     */
    @Override
    public void writePacket(ByteBuf buf, int id) {
        switch (id) {
            case 1:
                ByteBufUtils.writeUTF8String(buf, customName);
                break;
            case 2:
                buf.writeInt(priority);
                break;
            case 3:
                buf.writeLong(limit);
                break;
            case 4:
                buf.writeBoolean(surgeMode);
                break;
            case 5:
                buf.writeBoolean(disableLimit);
                break;
        }
    }

    /** ID 표는 writePacket 위 주석 참조 */
    @Override
    public void readPacket(ByteBuf buf, int id) {
        switch (id) {
            case 1:
                // 보안: 장치 이름은 최대 24자로 자른다
                customName = limitCustomName(ByteBufUtils.readUTF8String(buf));
                markLiteSettingChanged();
                break;
            case 2:
                priority = buf.readInt();
                sortNetworkConnections();
                break;
            case 3:
                // 음수 한도는 0
                limit = Math.max(0L, buf.readLong());
                markLiteSettingChanged();
                break;
            case 4:
                surgeMode = buf.readBoolean();
                sortNetworkConnections();
                break;
            case 5:
                disableLimit = buf.readBoolean();
                markLiteSettingChanged();
                break;
        }
    }

    protected void sortNetworkConnections() {
        if (network instanceof FluxNetworkServer) {
            FluxNetworkServer fluxNetworkServer = (FluxNetworkServer) network;
            fluxNetworkServer.sortConnections = true;
            markLiteSettingChanged();
        }
    }

    protected void markLiteSettingChanged() {
        if (network instanceof FluxNetworkServer) {
            FluxNetworkServer fluxNetworkServer = (FluxNetworkServer) network;
            fluxNetworkServer.markLiteSettingChanged(this);
        }
    }

    /**
     * 설정 setter 공통 후처리: 저장 표시, 네트워크에 가벼운 설정 변경 알림, 클라이언트 동기화.
     */
    protected void notifySettingChanged() {
        markDirty();
        markLiteSettingChanged();
        sendPackets();
    }

    @Override
    public NBTTagCompound copyConfiguration(NBTTagCompound config) {
        return FluxConfigurationType.copyAll(this, config);
    }

    @Override
    public void pasteConfiguration(NBTTagCompound config) {
        FluxConfigurationType.pasteAll(this, config);
    }

    @Override
    public final World getFluxWorld() {
        return worldObj;
    }

    @Override
    public UUID getConnectionOwner() {
        return playerUUID;
    }

    /**
     * 인접 타일과의 에너지 연결을 다시 확인한다. 서버에서만 동작한다.
     */
    public void updateTransfers(ForgeDirection... facings) {
        if (worldObj == null || worldObj.isRemote || facings == null) {
            return;
        }
        getTransferHandler().updateTransfers(facings);
    }

    @Override
    public int getLogicPriority() {
        return surgeMode ? Integer.MAX_VALUE : priority;
    }

    @Override
    public void open(EntityPlayer player) {
        if (worldObj != null && !worldObj.isRemote) {
            playerUsing.add(player);
            sendPackets();
        }
    }

    @Override
    public void close(EntityPlayer player) {
        if (worldObj != null && !worldObj.isRemote) {
            playerUsing.remove(player);
        }
    }

    @Override
    public long getMaxTransferLimit() {
        return Long.MAX_VALUE;
    }

    @Override
    public final long getTransferBuffer() {
        return getTransferHandler().getBuffer();
    }

    @Override
    public final long getTransferChange() {
        return getTransferHandler().getChange();
    }

    @Override
    public String getCustomName() {
        return customName;
    }

    @Override
    public boolean isChunkLoaded() {
        return !isInvalid();
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public boolean isForcedLoading() {
        return chunkLoading;
    }

    @Override
    public long getLogicLimit() {
        return disableLimit ? Long.MAX_VALUE : limit;
    }

    @Override
    public final int getRawPriority() {
        return priority;
    }

    @Override
    public long getRawLimit() {
        return limit;
    }

    @Override
    public int getFolderID() {
        return folderID;
    }

    /**
     * worldObj가 아직 없으면(타일이 월드에 붙기 전) 캐시하지 않고 차원 0으로 임시 좌표를 만들어 반환한다.
     * 캐시된 좌표가 현재 위치·차원과 다르면 다시 만든다.
     */
    @Override
    public Coord4D getCoords() {
        if (worldObj == null) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("x", xCoord);
            tag.setInteger("y", yCoord);
            tag.setInteger("z", zCoord);
            tag.setInteger("dimension", 0);
            return new Coord4D(tag);
        }
        if (coord4D == null || coord4D.getX() != xCoord
            || coord4D.getY() != yCoord
            || coord4D.getZ() != zCoord
            || coord4D.getDimension() != worldObj.provider.dimensionId) {
            coord4D = new Coord4D(this);
        }
        return coord4D;
    }

    @Override
    public boolean getDisableLimit() {
        return disableLimit;
    }

    @Override
    public boolean getSurgeMode() {
        return surgeMode;
    }
}
