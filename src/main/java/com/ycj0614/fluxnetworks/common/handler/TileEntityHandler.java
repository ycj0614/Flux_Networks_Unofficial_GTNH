package com.ycj0614.fluxnetworks.common.handler;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPlug;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPoint;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IEnergyConnected;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaPipeEntity;
import gregtech.api.metatileentity.implementations.MTECable;

/**
 * GT5U 전용. 원작의 에너지 핸들러 목록(FE/RF/IC2/GTCE)은 모두 없애고,
 * 인접 타일이 GT의 {@link IEnergyConnected}인지만 본다.
 */
public class TileEntityHandler {

    /**
     * 타일 엔티티 등록. 이 이름은 월드 저장 데이터에 기록되므로 이후 바꾸지 않는다.
     */
    public static void registerTileEntity() {
        GameRegistry.registerTileEntity(TileFluxPlug.class, "fluxnetworks:FluxPlug");
        GameRegistry.registerTileEntity(TileFluxPoint.class, "fluxnetworks:FluxPoint");
        // TODO 3단계: 원작의 FluxController, FluxStorage, HerculeanFluxStorage, GargantuanFluxStorage 타일 등록
    }

    /**
     * 블록 ID("modid:name")와 메타데이터로 FluxConfig.blockBlacklist를 검사한다. 값 -1은 모든 메타.
     * <p>
     * 참고: GT 기계·케이블은 전부 같은 블록(gregtech:gt.blockmachines)이고, 월드 메타데이터는
     * 기계 종류(MetaTileEntity ID)가 아니다. 그래서 특정 GT 기계 하나만 골라 막을 수는 없다.
     */
    public static boolean isBlacklisted(TileEntity tile) {
        if (tile == null || FluxConfig.blockBlacklist.isEmpty()) {
            return false;
        }
        Block block = tile.getBlockType();
        if (block == null) {
            return false;
        }
        // TODO 확인: GameRegistry.findUniqueIdentifierFor(Block)와 UniqueIdentifier의 public 필드 modId, name
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(block);
        if (id == null) {
            return false;
        }
        Integer meta = FluxConfig.blockBlacklist.get(id.modId + ":" + id.name);
        if (meta == null) {
            return false;
        }
        return meta == -1 || meta == tile.getBlockMetadata();
    }

    /**
     * 플럭스 장치가 이 타일과 에너지를 주고받을 수 있는지.
     * GT 장치(IEnergyConnected)이고, 플럭스 장치가 아니며, 블랙리스트에 없어야 한다.
     */
    public static boolean canConnectEnergy(TileEntity tile) {
        return tile != null && !tile.isInvalid()
            && tile instanceof IEnergyConnected
            && !(tile instanceof IFluxConnector)
            && !isBlacklisted(tile);
    }

    /**
     * 연결 모양 표시용 (방향 정보가 없을 때). 조건은 canConnectEnergy와 같다.
     * 플럭스 장치의 connections[] 갱신은 아래의 방향 버전을 쓴다.
     */
    public static boolean canRenderConnection(TileEntity tile) {
        return canConnectEnergy(tile);
    }

    /**
     * 연결 모양 표시용. side = 플럭스 장치에서 본 tile의 방향.
     * <ul>
     * <li>GT 케이블: 플럭스 장치 쪽 면이 실제로 연결되어 있을 때만 true (GT6 방식은 연결이 수동)</li>
     * <li>케이블이 아닌 GT 파이프(유체·아이템): false</li>
     * <li>그 밖의 GT 장치: 플럭스 장치 쪽 면으로 EU를 받거나 내보낼 때만 true</li>
     * </ul>
     */
    public static boolean canRenderConnection(TileEntity tile, ForgeDirection side) {
        if (side == null || side == ForgeDirection.UNKNOWN || !canConnectEnergy(tile)) {
            return false;
        }
        ForgeDirection toUs = side.getOpposite();
        MetaPipeEntity pipe = getGTPipe(tile);
        if (pipe != null) {
            // TODO 확인: MetaPipeEntity.isConnectedAtSide(ForgeDirection)
            return pipe instanceof MTECable && pipe.isConnectedAtSide(toUs);
        }
        IEnergyConnected energy = (IEnergyConnected) tile;
        return energy.inputEnergyFrom(toUs) || energy.outputsEnergyTo(toUs);
    }

    /**
     * 플럭스 장치가 새로 놓였을 때 1단계: 이쪽으로 연결된 인접 GT 케이블을 끊는다. 서버 전용.
     * <p>
     * GT 케이블은 전력을 받을 대상(ConsumerNode)을 타일 객체째 캐시하고, 연결 상태가 이전 틱과 달라졌을 때만
     * 다시 만드는 것으로 보인다. 같은 틱에 끊고 바로 이으면 변화로 감지되지 않으므로,
     * 끊기와 다시 잇기({@link #reconnectAdjacentCables}) 사이에 틱 간격을 둔다.
     * 이쪽으로 연결되지 않은 케이블은 건드리지 않는다 (GT6 방식: 연결은 플레이어가 정한다).
     *
     * @return 끊은 방향 비트 (1 &lt;&lt; ForgeDirection.ordinal(), 플럭스 장치에서 본 방향)
     */
    public static int disconnectAdjacentCables(World world, int x, int y, int z) {
        if (world == null || world.isRemote) {
            return 0;
        }
        int mask = 0;
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            MTECable cable = getAdjacentCable(world, x, y, z, dir);
            if (cable == null) {
                continue;
            }
            ForgeDirection toUs = dir.getOpposite();
            if (cable.isConnectedAtSide(toUs)) {
                // TODO 확인: MetaPipeEntity.disconnect(ForgeDirection) (void)
                cable.disconnect(toUs);
                mask |= 1 << dir.ordinal();
            }
        }
        return mask;
    }

    /**
     * 2단계: {@link #disconnectAdjacentCables}로 끊은 방향을 다시 잇는다. 서버 전용.
     * 그 사이 케이블이 사라졌거나 이미 다시 연결된 방향은 건너뛴다.
     */
    public static void reconnectAdjacentCables(World world, int x, int y, int z, int mask) {
        if (world == null || world.isRemote || mask == 0) {
            return;
        }
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            if ((mask & (1 << dir.ordinal())) == 0) {
                continue;
            }
            MTECable cable = getAdjacentCable(world, x, y, z, dir);
            if (cable == null) {
                continue;
            }
            ForgeDirection toUs = dir.getOpposite();
            if (cable.isConnectedAtSide(toUs)) {
                continue;
            }
            // TODO 확인: MetaPipeEntity.connect(ForgeDirection) (int, 성공 시 1)
            if (cable.connect(toUs) <= 0) {
                FluxNetworks.logger
                    .warn("Flux device at {}, {}, {}: GT cable on side {} could not reconnect", x, y, z, dir);
            }
        }
    }

    @Nullable
    private static MTECable getAdjacentCable(World world, int x, int y, int z, ForgeDirection dir) {
        int tx = x + dir.offsetX;
        int ty = y + dir.offsetY;
        int tz = z + dir.offsetZ;
        // TODO 확인: World.blockExists(int, int, int) (배치 F에서도 사용)
        if (!world.blockExists(tx, ty, tz)) {
            return null;
        }
        MetaPipeEntity pipe = getGTPipe(world.getTileEntity(tx, ty, tz));
        return pipe instanceof MTECable ? (MTECable) pipe : null;
    }

    /** GT 파이프(케이블·유체·아이템)의 MetaPipeEntity. 파이프가 아니면 null */
    @Nullable
    private static MetaPipeEntity getGTPipe(@Nullable TileEntity tile) {
        if (tile instanceof IGregTechTileEntity) {
            // TODO 확인: IGregTechTileEntity.getMetaTileEntity()
            IMetaTileEntity mte = ((IGregTechTileEntity) tile).getMetaTileEntity();
            if (mte instanceof MetaPipeEntity) {
                return (MetaPipeEntity) mte;
            }
        }
        return null;
    }
}
