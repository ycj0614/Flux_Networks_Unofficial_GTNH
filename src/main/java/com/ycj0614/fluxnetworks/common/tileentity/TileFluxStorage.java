package com.ycj0614.fluxnetworks.common.tileentity;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.api.network.ConnectionType;
import com.ycj0614.fluxnetworks.api.network.ITransferHandler;
import com.ycj0614.fluxnetworks.api.tiles.IFluxStorage;
import com.ycj0614.fluxnetworks.common.connection.transfer.FluxStorageHandler;
import com.ycj0614.fluxnetworks.common.core.GTEnergy;

/**
 * 플럭스 스토리지 (기본·Herculean·Gargantuan). 네트워크 버퍼 전용이며 GT 기계·케이블과 직접 연결하지 않는다
 * (IEnergyConnected를 구현하지 않음).
 * <p>
 * 원작의 OpenComputers getFluxInfo, TheOneProbe IBigPower는 GTNH 범위 밖이라 옮기지 않았다.
 * TODO 배치 R: 원작 TileFluxStorageRenderer(에너지 게이지)와 ItemFluxStorageRenderer
 */
public class TileFluxStorage extends TileFluxCore implements IFluxStorage {

    /** 네트워크 전체에서 가장 낮은 우선순위가 되도록 빼는 값 */
    public static final int PRI_DIFF = 1000000;
    /** 스토리지 우선순위 상한 */
    public static final int PRI_UPPER = -10000;

    private final FluxStorageHandler handler = new FluxStorageHandler(this);

    private boolean needSyncEnergy = false;

    public TileFluxStorage() {
        customName = "Basic Storage";
        limit = FluxConfig.basicTransfer;
    }

    public static class Herculean extends TileFluxStorage {

        public Herculean() {
            customName = "Herculean Storage";
            limit = FluxConfig.herculeanTransfer;
        }

        @Override
        public long getMaxTransferLimit() {
            return FluxConfig.herculeanCapacity;
        }
    }

    public static class Gargantuan extends TileFluxStorage {

        public Gargantuan() {
            customName = "Gargantuan Storage";
            limit = FluxConfig.gargantuanTransfer;
        }

        @Override
        public long getMaxTransferLimit() {
            return FluxConfig.gargantuanCapacity;
        }
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.STORAGE;
    }

    @Override
    public ITransferHandler getTransferHandler() {
        return handler;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (worldObj != null && !worldObj.isRemote) {
            sendPacketIfNeeded();
        }
    }

    /** 에너지가 바뀌었으면 4틱마다 클라이언트에 동기화한다 (서버) */
    private void sendPacketIfNeeded() {
        if (needSyncEnergy && (worldObj.getTotalWorldTime() & 3) == 0) {
            sendPackets();
            needSyncEnergy = false;
        }
    }

    public void markServerEnergyChanged() {
        needSyncEnergy = true;
    }

    @Override
    public int getLogicPriority() {
        return surgeMode ? PRI_UPPER : Math.min(priority - PRI_DIFF, PRI_UPPER);
    }

    /** 저장 용량 (EU) */
    @Override
    public long getMaxTransferLimit() {
        return FluxConfig.basicCapacity;
    }

    // ---- IFluxPlug / IFluxPoint의 EU 설정 ----
    // 원작과 다름: 스토리지는 GT와 직접 주고받지 않으므로 아래 값은 쓰이지 않는다. 인터페이스를 채우는 고정값이다

    @Override
    public int getMaxInputTier() {
        return GTEnergy.MAX_TIER;
    }

    @Override
    public int getOutputTier() {
        return GTEnergy.MAX_TIER;
    }

    @Override
    public long getOutputAmperage() {
        return 1L;
    }
}
