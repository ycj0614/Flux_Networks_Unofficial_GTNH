package com.ycj0614.fluxnetworks.common.tileentity;

import com.ycj0614.fluxnetworks.api.network.ConnectionType;
import com.ycj0614.fluxnetworks.api.network.ITransferHandler;
import com.ycj0614.fluxnetworks.api.tiles.IFluxController;
import com.ycj0614.fluxnetworks.common.connection.transfer.FluxControllerHandler;
import com.ycj0614.fluxnetworks.common.core.GTEnergy;

/**
 * 플럭스 컨트롤러. 네트워크당 1개 (FluxUtils.addConnection, PacketTileHandler, 구성기 붙여넣기에서 검사).
 * GT 기계·케이블과 직접 연결하지 않는다 (IEnergyConnected를 구현하지 않음).
 * <p>
 * 원작의 OpenComputers getPeripheralName("flux_controller")은 GTNH 범위 밖이라 옮기지 않았다.
 * 무선 충전은 4단계 (FluxControllerHandler 참고).
 */
public class TileFluxController extends TileFluxCore implements IFluxController {

    private final FluxControllerHandler handler = new FluxControllerHandler(this);

    public TileFluxController() {
        customName = "Flux Controller";
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.CONTROLLER;
    }

    @Override
    public ITransferHandler getTransferHandler() {
        return handler;
    }

    // ---- IFluxPoint의 EU 설정 ----
    // 원작과 다름: 컨트롤러는 GT와 직접 주고받지 않으므로 아래 값은 쓰이지 않는다. 인터페이스를 채우는 고정값이다

    @Override
    public int getOutputTier() {
        return GTEnergy.MAX_TIER;
    }

    @Override
    public long getOutputAmperage() {
        return 1L;
    }
}
