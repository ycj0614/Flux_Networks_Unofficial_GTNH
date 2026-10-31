package com.ycj0614.fluxnetworks.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.ycj0614.fluxnetworks.client.FluxColorHandler;
import com.ycj0614.fluxnetworks.common.block.BlockFluxController;
import com.ycj0614.fluxnetworks.common.block.BlockFluxCore;
import com.ycj0614.fluxnetworks.common.block.BlockFluxPlug;
import com.ycj0614.fluxnetworks.common.block.BlockFluxPoint;
import com.ycj0614.fluxnetworks.common.block.BlockFluxStorage;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 원작 blockstate + JSON 모델 렌더링 (월드). 플러그·포인트는 원작 BlockSidedConnection처럼
 * 타일의 connections[]가 1인 면에 연결 팔을 붙인다. 아이템은 ItemFluxDeviceRenderer가 그린다.
 */
@SideOnly(Side.CLIENT)
public class BlockFluxRenderer implements ISimpleBlockRenderingHandler {

    private final int renderId;

    public BlockFluxRenderer(int renderId) {
        this.renderId = renderId;
    }

    /** 블록 종류별 본체 모델 */
    public static FluxModelRenderer.Element[] getModel(Block block) {
        if (block instanceof BlockFluxPlug) {
            return FluxBlockModels.PLUG;
        }
        if (block instanceof BlockFluxPoint) {
            return FluxBlockModels.POINT;
        }
        if (block instanceof BlockFluxController) {
            return FluxBlockModels.CONTROLLER;
        }
        if (block instanceof BlockFluxStorage) {
            return FluxBlockModels.STORAGE;
        }
        return null;
    }

    /** 면 연결 팔을 그리는 블록인지 (원작 BlockSidedConnection) */
    public static boolean hasConnections(Block block) {
        return block instanceof BlockFluxPlug || block instanceof BlockFluxPoint;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        // 아이템은 ItemFluxDeviceRenderer(IItemRenderer)가 그린다 (네트워크 색에 ItemStack이 필요)
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        FluxModelRenderer.Element[] model = getModel(block);
        if (model == null) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        TileFluxCore fluxCore = tile instanceof TileFluxCore ? (TileFluxCore) tile : null;
        boolean connected = fluxCore != null && fluxCore.connected;
        int tint = fluxCore != null ? FluxColorHandler.getBlockColor(fluxCore) : FluxColorHandler.NO_NETWORK_COLOR;
        IIcon[] icons = ((BlockFluxCore) block).getModelIcons(connected);
        IIcon override = renderer.hasOverrideBlockTexture() ? renderer.overrideBlockTexture : null;

        Tessellator t = Tessellator.instance;
        t.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        FluxModelRenderer.render(t, model, icons, override, null, tint, BlockFluxCore.TEX_COLOUR, true, x, y, z);
        if (fluxCore != null && hasConnections(block)) {
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                if (fluxCore.connections[dir.ordinal()] == 1) {
                    FluxModelRenderer.render(
                        t,
                        FluxBlockModels.CONNECTION,
                        icons,
                        override,
                        dir,
                        tint,
                        BlockFluxCore.TEX_COLOUR,
                        true,
                        x,
                        y,
                        z);
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return renderId;
    }
}
