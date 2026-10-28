package com.ycj0614.fluxnetworks.common.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPoint;

public class BlockFluxPoint extends BlockFluxCore {

    public BlockFluxPoint() {
        super("fluxpoint", "fluxpoint");
        // TODO 3단계: 원작 경계 상자 (0.375 ~ 0.625)
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileFluxPoint();
    }
}
