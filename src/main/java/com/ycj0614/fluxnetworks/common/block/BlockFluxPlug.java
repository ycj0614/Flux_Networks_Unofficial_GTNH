package com.ycj0614.fluxnetworks.common.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPlug;

public class BlockFluxPlug extends BlockFluxCore {

    public BlockFluxPlug() {
        super("fluxplug");
        setBlockBounds(0.25F, 0.25F, 0.25F, 0.75F, 0.75F, 0.75F);
        setModelTexture(TEX_0, "flux_plug_on", "flux_plug_off");
        setModelTexture(TEX_COLOUR, "flux_plug_colour");
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileFluxPlug();
    }
}
