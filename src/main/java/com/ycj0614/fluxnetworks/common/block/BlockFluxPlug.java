package com.ycj0614.fluxnetworks.common.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPlug;

public class BlockFluxPlug extends BlockFluxCore {

    public BlockFluxPlug() {
        super("fluxplug", "fluxplug");
        // TODO 3단계: 원작 경계 상자 (0.25 ~ 0.75)
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileFluxPlug();
    }
}
