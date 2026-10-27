package com.ycj0614.fluxnetworks.common.connection.transfer;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.tiles.IFluxPlug;
import com.ycj0614.fluxnetworks.api.utils.Coord4D;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkServer;
import com.ycj0614.fluxnetworks.common.core.GTEnergy;
import com.ycj0614.fluxnetworks.common.handler.TileEntityHandler;

public class FluxPlugHandler extends BasicTransferHandler<IFluxPlug> {

    private final Map<ForgeDirection, ConnectionTransfer> transfers = new EnumMap<>(ForgeDirection.class);

    private long received;
    private long removed;

    public FluxPlugHandler(IFluxPlug fluxPlug) {
        super(fluxPlug);
    }

    @Override
    public void onCycleStart() {
        for (ConnectionTransfer transfer : transfers.values()) {
            if (transfer != null) {
                transfer.onCycleStart();
            }
        }
    }

    @Override
    public void onCycleEnd() {
        change = received;
        received = 0;
        removed = 0;
    }

    @Override
    public long removeFromBuffer(long energy) {
        long a = Math.min(Math.min(energy, buffer), device.getLogicLimit() - removed);
        if (a <= 0) {
            return 0;
        }
        buffer -= a;
        removed += a;
        return a;
    }

    /**
     * 외부에서 EU를 받는다. 네트워크 전체 요청량(bufferLimiter)과 플러그 한도를 넘게는 받지 않는다 (역압).
     * <p>
     * 원작과 달리 side에 해당하는 transfer가 없어도 받는다. GT는 side로 UNKNOWN(특정 면 없음)을 보낼 수 있고,
     * 인접 정보 갱신 시점이 어긋날 수도 있기 때문이다. 통계는 transfer가 있을 때만 기록한다.
     */
    @Override
    public long receiveFromSupplier(long amount, @Nonnull ForgeDirection side, boolean simulate) {
        IFluxNetwork network = device.getNetwork();
        if (network == null || network.isInvalid()) {
            return 0;
        }
        if (!(network instanceof FluxNetworkServer)) {
            return 0;
        }
        long a = ((FluxNetworkServer) network).bufferLimiter - buffer;
        if (a <= 0) {
            return 0;
        }
        a = Math.min(amount, Math.min(a, device.getLogicLimit()) - buffer);
        if (a <= 0) {
            return 0;
        }
        if (!simulate) {
            buffer += a;
            received += a; // external additions happen outside the transfer cycle.
            ConnectionTransfer transfer = transfers.get(side);
            if (transfer != null) {
                transfer.onEnergyReceived(a);
            }
        }
        return a;
    }

    /**
     * GT 에너지 입력. 플러그 타일의 IEnergyConnected.injectEnergyUnits가 이 메서드로 위임한다.
     * 암페어 단위로만 받는다 (1암페어 미만으로 남는 공간은 버린다).
     *
     * @param side 우리 블록의 면 (0~5) 또는 UNKNOWN(특정 면 없음)
     * @return 사용한 암페어. 과전압이면 0 (폭발 없음)
     */
    public long injectEnergyUnits(ForgeDirection side, long voltage, long amperage) {
        if (voltage <= 0 || amperage <= 0) {
            return 0;
        }
        if (!device.isActive()) {
            return 0;
        }
        if (voltage > GTEnergy.voltage(device.getMaxInputTier())) {
            return 0; // 과전압 거부, 폭발 없음
        }
        if (side == null) {
            side = ForgeDirection.UNKNOWN;
        }
        long amps = Math.min(amperage, Long.MAX_VALUE / voltage);
        long canAccept = receiveFromSupplier(voltage * amps, side, true);
        long acceptedAmps = canAccept / voltage;
        if (acceptedAmps <= 0) {
            return 0;
        }
        receiveFromSupplier(voltage * acceptedAmps, side, false);
        return acceptedAmps;
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
        updateSidedTransfers(world, coords.getX(), coords.getY(), coords.getZ(), transfers, faces);
    }

    static void updateSidedTransfers(World world, int x, int y, int z,
        Map<ForgeDirection, ConnectionTransfer> transfers, @Nonnull ForgeDirection[] faces) {
        if (world == null) {
            return;
        }
        for (ForgeDirection dir : faces) {
            if (dir == null || dir == ForgeDirection.UNKNOWN) {
                continue;
            }
            int tx = x + dir.offsetX;
            int ty = y + dir.offsetY;
            int tz = z + dir.offsetZ;
            // 인접 청크가 로드되지 않았으면 getTileEntity가 청크를 강제로 불러오므로 연결을 끊고 넘어간다.
            // 청크가 다시 로드된 뒤에는 타일 쪽의 주기적 updateTransfers 호출로 다시 연결된다 (배치 F).
            // TODO 확인: World.blockExists(int, int, int) (1.7.10 MCP 이름, 1.8+의 isBlockLoaded)
            if (!world.blockExists(tx, ty, tz)) {
                transfers.remove(dir);
                continue;
            }
            TileEntity tile = world.getTileEntity(tx, ty, tz);
            if (TileEntityHandler.canConnectEnergy(tile)) {
                ConnectionTransfer transfer = transfers.get(dir);
                if (transfer == null || transfer.getTile() != tile) {
                    transfers.put(dir, new ConnectionTransfer(tile, dir));
                }
            } else {
                transfers.remove(dir);
            }
        }
    }
}
