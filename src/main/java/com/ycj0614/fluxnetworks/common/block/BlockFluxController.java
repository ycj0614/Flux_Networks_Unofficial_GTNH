package com.ycj0614.fluxnetworks.common.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.common.tileentity.TileFluxController;

/**
 * 플럭스 컨트롤러 블록. 툴팁은 ItemFluxConnector가 맡는다.
 */
public class BlockFluxController extends BlockFluxCore {

    public BlockFluxController() {
        super("fluxcontroller");
        float p = 0.0625F;
        setBlockBounds(p, p, p, 1 - p, 1 - p, 1 - p);
        setModelTexture(TEX_0, "fluxcontroller_bottom");
        setModelTexture(TEX_1, "flux_controller");
        setModelTexture(TEX_TOP, "fluxcontroller_on", "fluxcontroller_off");
        setModelTexture(TEX_COLOUR, "fluxcontroller_colour");
    }

    /** 원작 blockstate의 기본값이 fluxcontroller_off라서 아이템은 꺼진 윗면으로 보인다 */
    @Override
    public boolean isItemModelConnected() {
        return false;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileFluxController();
    }
}
