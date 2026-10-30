package com.ycj0614.fluxnetworks.common.tileentity;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.ycj0614.fluxnetworks.common.handler.TileEntityHandler;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.tileentity.IColoredTileEntity;
import gregtech.api.interfaces.tileentity.IEnergyConnected;
import gregtech.api.metatileentity.BaseMetaPipeEntity;

/**
 * GT 에너지 네트워크와 직접 연결되는 플럭스 장치의 공통 부모.
 * 원작의 에너지 API 래퍼 층(TileForgeEnergy ~ TileGTEnergy)을 대신한다.
 */
public abstract class TileFluxConnector extends TileFluxCore implements IEnergyConnected {

    /**
     * GT 케이블은 이웃이 바뀐 뒤 최대 20틱 늦게 연결을 다시 확인한다 (MTECable.onPostTick의 aTick % 20).
     * 그래서 첫 틱에 한 번, 이 틱 수만큼 지난 뒤 한 번 더 GT 기계에 전력 경로 갱신을 알린다.
     */
    private static final int GT_RENOTIFY_DELAY = 25;

    private boolean gtNotified = false;
    private int gtRenotifyTimer = 0;

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        if (!gtNotified) {
            gtNotified = true;
            gtRenotifyTimer = GT_RENOTIFY_DELAY;
            notifyGTCables();
        } else if (gtRenotifyTimer > 0 && --gtRenotifyTimer == 0) {
            notifyGTCables();
        }
    }

    @Override
    public void invalidate() {
        // 블록이 제거될 때: 이 장치를 소비자로 기억하던 GT 전력 경로를 다시 만들게 한다.
        // causeCableUpdate는 이번 틱 끝에 처리되므로 super.invalidate() 전후 어느 쪽이든 동작한다.
        if (worldObj != null && !worldObj.isRemote) {
            notifyGTCables();
        }
        super.invalidate();
    }

    @Override
    public void updateTransfers(ForgeDirection... facings) {
        super.updateTransfers(facings);
        if (worldObj == null || worldObj.isRemote || facings == null) {
            return;
        }
        boolean sendUpdate = false;
        for (ForgeDirection dir : facings) {
            if (dir == null || dir == ForgeDirection.UNKNOWN) {
                continue;
            }
            int index = dir.ordinal();
            if (index >= connections.length) {
                continue;
            }
            int tx = xCoord + dir.offsetX;
            int ty = yCoord + dir.offsetY;
            int tz = zCoord + dir.offsetZ;
            // 인접 청크가 로드되지 않았으면 getTileEntity가 청크를 불러오므로 확인하지 않고 표시 상태를 유지한다.
            // 청크가 로드되면 주기적 updateTransfers로 다시 확인된다.
            if (!worldObj.blockExists(tx, ty, tz)) {
                continue;
            }
            TileEntity tile = worldObj.getTileEntity(tx, ty, tz);
            boolean b = connections[index] != 0;
            boolean c = TileEntityHandler.canRenderConnection(tile);
            if (b != c) {
                connections[index] = (byte) (c ? 1 : 0);
                sendUpdate = true;
            }
        }
        if (sendUpdate) {
            sendPackets();
        }
    }

    // ---- IEnergyConnected (GT5U) ----
    // side는 0~5 또는 ForgeDirection.UNKNOWN(특정 면 없음). 기본 구현은 어떤 side에서도 예외를 던지지 않는다.

    @Override
    public long injectEnergyUnits(ForgeDirection side, long aVoltage, long aAmperage) {
        return 0;
    }

    @Override
    public boolean inputEnergyFrom(ForgeDirection side) {
        return false;
    }

    @Override
    public boolean outputsEnergyTo(ForgeDirection side) {
        return false;
    }

    // ---- IColoredTileEntity: 항상 무색 (모든 색 케이블과 연결) ----

    @Override
    public byte getColorization() {
        return (byte) IColoredTileEntity.UNCOLOURED;
    }

    @Override
    public byte setColorization(byte aColor) {
        return (byte) IColoredTileEntity.UNCOLOURED;
    }

    // 인접한 GT 케이블을 따라 GT 기계들의 전력 경로를 다시 만들게 한다
    protected void notifyGTCables() {
        if (worldObj == null || worldObj.isRemote) return;
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            int x = xCoord + dir.offsetX, y = yCoord + dir.offsetY, z = zCoord + dir.offsetZ;
            if (worldObj.blockExists(x, y, z) && worldObj.getTileEntity(x, y, z) instanceof BaseMetaPipeEntity) {
                GregTechAPI.causeCableUpdate(worldObj, x, y, z);
            }
        }
    }

}
