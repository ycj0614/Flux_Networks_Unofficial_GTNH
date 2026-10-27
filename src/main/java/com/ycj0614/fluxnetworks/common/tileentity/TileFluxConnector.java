package com.ycj0614.fluxnetworks.common.tileentity;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.ycj0614.fluxnetworks.common.handler.TileEntityHandler;

import gregtech.api.interfaces.tileentity.IColoredTileEntity;
import gregtech.api.interfaces.tileentity.IEnergyConnected;

/**
 * GT 에너지 네트워크와 직접 연결되는 플럭스 장치의 공통 부모.
 * 원작의 에너지 API 래퍼 층(TileForgeEnergy ~ TileGTEnergy)을 대신한다.
 */
public abstract class TileFluxConnector extends TileFluxCore implements IEnergyConnected {

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
            // TODO 확인: World.blockExists(int, int, int) (1.7.10 MCP 이름, 1.8+의 isBlockLoaded)
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
}
