package com.ycj0614.fluxnetworks.common.handler;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.interfaces.tileentity.IEnergyConnected;

/**
 * GT5U 전용. 원작의 에너지 핸들러 목록(FE/RF/IC2/GTCE)은 모두 없애고,
 * 인접 타일이 GT의 {@link IEnergyConnected}인지만 본다.
 */
public class TileEntityHandler {

    // TODO 배치 G: 타일 엔티티 등록 (원작은 GameRegistry.registerTileEntity로 FluxController, FluxPoint, FluxPlug, FluxStorage 등 등록)

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
     * 연결 모양 표시용 (원작과 같은 목적). 조건은 canConnectEnergy와 같다.
     */
    public static boolean canRenderConnection(TileEntity tile) {
        return canConnectEnergy(tile);
    }
}
