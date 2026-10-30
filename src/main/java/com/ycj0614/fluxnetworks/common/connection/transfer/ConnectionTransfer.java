package com.ycj0614.fluxnetworks.common.connection.transfer;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.interfaces.tileentity.IEnergyConnected;

/**
 * 한 방향의 인접 GT 장치(IEnergyConnected)와의 연결.
 * inbound/outbound는 GUI 통계용이다.
 */
public class ConnectionTransfer {

    private final TileEntity tile;
    private final ForgeDirection side;

    // TODO 5단계: 인접 장치 아이콘 표시가 필요하면 (WAILA 등) 표시용 ItemStack 추가.
    // 원작: 생성자에서 FluxUtils.getBlockItem으로 만든 displayStack. 원작 GUI에서는 쓰지 않았다

    /** 상대 타일에서 나와 플러그로 들어온 EU (이번 틱, 상대 타일 기준 "나감") */
    public long outbound;
    /** 포인트가 상대 타일에 넣은 EU (이번 틱, 상대 타일 기준 "들어옴") */
    public long inbound;

    /**
     * @param tile 상대 타일
     * @param dir  우리 블록 기준, 상대 타일이 있는 방향
     */
    public ConnectionTransfer(@Nonnull TileEntity tile, @Nonnull ForgeDirection dir) {
        this.tile = tile;
        this.side = dir.getOpposite(); // the tile is on our north side, we charge it from its south side
    }

    /**
     * 상대 타일에 전압 voltage로 최대 amperage 암페어를 넣는다.
     * 받는 쪽(GT BaseMetaTileEntity)이 inputEnergyFrom 검사와 과전압 폭발을 스스로 처리한다.
     *
     * @return 실제로 사용된 암페어 (0 ~ amperage)
     */
    public long sendToTile(long voltage, long amperage) {
        if (voltage <= 0 || amperage <= 0) {
            return 0;
        }
        if (tile.isInvalid() || !(tile instanceof IEnergyConnected)) {
            return 0;
        }
        long used = ((IEnergyConnected) tile).injectEnergyUnits(side, voltage, amperage);
        if (used <= 0) {
            return 0;
        }
        if (used > amperage) {
            used = amperage;
        }
        inbound += used * voltage;
        return used;
    }

    public void onEnergyReceived(long amount) {
        outbound += amount;
    }

    public void onCycleStart() {
        outbound = 0;
        inbound = 0;
    }

    public TileEntity getTile() {
        return tile;
    }
}
