package com.ycj0614.fluxnetworks.common.connection.transfer;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.api.tiles.IFluxPoint;
import com.ycj0614.fluxnetworks.api.utils.Coord4D;
import com.ycj0614.fluxnetworks.common.core.GTEnergy;

/**
 * 포인트: 설정된 전압 V, 최대 암페어 A로 인접 GT 장치에 EU를 내보낸다.
 * <p>
 * GT에는 시뮬레이션이 없어 수요를 미리 알 수 없으므로, 요청량은 "버퍼 용량 − 현재 버퍼"로 정한다.
 * 버퍼 용량 = V × A × FluxConfig.pointBufferTicks. 소비자가 받지 않으면 버퍼가 차고 요청량이 0이 되어
 * 플러그도 수신을 멈춘다 (역압).
 */
public class FluxPointHandler extends BasicPointHandler<IFluxPoint> {

    private final Map<ForgeDirection, ConnectionTransfer> transfers = new EnumMap<>(ForgeDirection.class);

    public FluxPointHandler(IFluxPoint fluxPoint) {
        super(fluxPoint);
    }

    @Override
    public void onCycleStart() {
        for (ConnectionTransfer transfer : transfers.values()) {
            if (transfer != null) {
                transfer.onCycleStart();
            }
        }
        if (!device.isActive()) {
            demand = 0;
            return;
        }
        long voltage = GTEnergy.voltage(device.getOutputTier());
        long amperage = device.getOutputAmperage();
        demand = saturatedMultiply(saturatedMultiply(voltage, amperage), FluxConfig.pointBufferTicks);
    }

    @Override
    protected long sendToConsumers(long energy) {
        if (!device.isActive() || energy <= 0) {
            return 0;
        }
        long voltage = GTEnergy.voltage(device.getOutputTier());
        long amperage = device.getOutputAmperage();
        if (amperage <= 0) {
            return 0;
        }
        // 버퍼가 1암페어분(V)보다 적으면 보내지 않고 다음 틱으로 넘긴다
        long availableAmps = Math.min(amperage, energy / voltage);
        if (availableAmps <= 0) {
            return 0;
        }
        long usedTotal = 0;
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            ConnectionTransfer transfer = transfers.get(dir);
            if (transfer == null) {
                continue;
            }
            usedTotal += transfer.sendToTile(voltage, availableAmps - usedTotal);
            if (usedTotal >= availableAmps) {
                break;
            }
        }
        return usedTotal * voltage;
    }

    @Override
    public void updateTransfers(@Nonnull ForgeDirection... faces) {
        World world = device.getFluxWorld();
        if (world == null) {
            return;
        }
        Coord4D coords = device.getCoords();
        if (coords == null) {
            return;
        }
        FluxPlugHandler.updateSidedTransfers(world, coords.getX(), coords.getY(), coords.getZ(), transfers, faces);
    }

    /**
     * 음이 아닌 두 수의 곱. 넘치면 Long.MAX_VALUE, 어느 한쪽이 0 이하면 0.
     */
    private static long saturatedMultiply(long a, long b) {
        if (a <= 0 || b <= 0) {
            return 0;
        }
        if (a > Long.MAX_VALUE / b) {
            return Long.MAX_VALUE;
        }
        return a * b;
    }
}
