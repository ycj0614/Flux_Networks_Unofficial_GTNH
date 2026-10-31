package com.ycj0614.fluxnetworks.common.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.common.tileentity.TileFluxController;

/**
 * 플럭스 컨트롤러 블록. 아이콘은 fluxcontroller_on/off (원작 모델 텍스처). 툴팁은 ItemFluxConnector가 맡는다.
 * TODO 배치 R: 원작 경계 상자 (0.0625 ~ 0.9375)와 원작 모양 렌더링
 */
public class BlockFluxController extends BlockFluxCore {

    public BlockFluxController() {
        super("fluxcontroller", "fluxcontroller");
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileFluxController();
    }
}
