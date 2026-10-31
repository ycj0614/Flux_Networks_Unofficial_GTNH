package com.ycj0614.fluxnetworks.common.connection.transfer;

import javax.annotation.Nonnull;

import net.minecraftforge.common.util.ForgeDirection;

import com.ycj0614.fluxnetworks.common.tileentity.TileFluxController;

/**
 * 컨트롤러 전송 핸들러. 3단계에서는 요청량 0인 껍데기다 (네트워크에서 에너지를 받지 않는다).
 * <p>
 * TODO 4단계: 원작 onCycleStart — 활성 상태이고 ENABLE_WIRELESS가 켜져 있을 때만 동작, timer == 0이면 updatePlayers,
 * (timer & 3) == 2이면 demand = chargeAllItems(getLogicLimit(), 시뮬레이션). 아니면 demand = 0, clearPlayers.
 * TODO 4단계: 원작 onCycleEnd — super 다음 timer = ++timer & 0x1f. reset — super 다음 clearPlayers.
 * TODO 4단계: 원작 sendToConsumers — (timer & 3) == 0이고 무선이 켜져 있을 때 chargeAllItems(energy, 실제)로 플레이어 아이템 충전.
 * TODO 4단계: 원작 updatePlayers — 네트워크 멤버 중 접속 중이고 다른 네트워크가 충전하지 않는 플레이어(정적 CHARGING_PLAYERS)마다
 * NETWORK_WIRELESS 설정의 오른손·왼손·핫바(든 아이템 제외)·갑옷·바우블 슬롯 핸들러(WirelessHandler)를 만든다. 충전 대상은 GT 전기 아이템.
 */
public class FluxControllerHandler extends BasicPointHandler<TileFluxController> {

    public FluxControllerHandler(TileFluxController fluxController) {
        super(fluxController);
    }

    @Override
    public void onCycleStart() {
        demand = 0;
    }

    @Override
    protected long sendToConsumers(long energy) {
        return 0;
    }

    /** 컨트롤러는 인접 블록과 에너지를 주고받지 않는다 */
    @Override
    public void updateTransfers(@Nonnull ForgeDirection... faces) {}
}
