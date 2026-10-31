package com.ycj0614.fluxnetworks.common.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPoint;

public class BlockFluxPoint extends BlockFluxCore {

    public BlockFluxPoint() {
        super("fluxpoint");
        setBlockBounds(0.375F, 0.375F, 0.375F, 0.625F, 0.625F, 0.625F);
        setModelTexture(TEX_0, "flux_point_on", "flux_point_off");
        setModelTexture(TEX_COLOUR, "flux_point_colour");
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileFluxPoint();
    }
}
