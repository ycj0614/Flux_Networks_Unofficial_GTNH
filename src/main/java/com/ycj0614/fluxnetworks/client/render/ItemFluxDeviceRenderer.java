package com.ycj0614.fluxnetworks.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.client.FluxColorHandler;
import com.ycj0614.fluxnetworks.common.block.BlockFluxCore;
import com.ycj0614.fluxnetworks.common.block.BlockFluxStorage;
import com.ycj0614.fluxnetworks.common.connection.transfer.FluxStorageHandler;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.data.FluxNetworkData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 플럭스 장치 아이템(플러그·포인트·스토리지 3종·컨트롤러). 원작 blockstate의 inventory 변형(플러그·포인트는 여섯 면 모두 연결 팔)을
 * 그리고 네트워크 색을 입힌다. 스토리지는 원작 ItemFluxStorageRenderer처럼 아이템 NBT의 에너지로 게이지도 그린다.
 */
@SideOnly(Side.CLIENT)
public class ItemFluxDeviceRenderer implements IItemRenderer {

    public static final ItemFluxDeviceRenderer INSTANCE = new ItemFluxDeviceRenderer();

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type != ItemRenderType.FIRST_PERSON_MAP;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return helper == ItemRendererHelper.ENTITY_ROTATION || helper == ItemRendererHelper.ENTITY_BOBBING
            || helper == ItemRendererHelper.INVENTORY_BLOCK
            || helper == ItemRendererHelper.EQUIPPED_BLOCK
            // 떨어진 아이템·3인칭 손에서 블록용 크기와 각도를 쓴다
            || helper == ItemRendererHelper.BLOCK_3D;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        Block block = Block.getBlockFromItem(stack.getItem());
        FluxModelRenderer.Element[] model = BlockFluxRenderer.getModel(block);
        if (model == null) {
            return;
        }
        BlockFluxCore fluxBlock = (BlockFluxCore) block;

        GL11.glPushMatrix();
        // 모델은 0~1로 그리므로 중심을 원점으로 옮긴다 (RenderBlocks.renderBlockAsItem과 같음).
        // 손에 든 모습은 EQUIPPED_BLOCK 헬퍼에서 Forge가 이미 -0.5만큼 옮겨 준다
        if (type == ItemRenderType.INVENTORY || type == ItemRenderType.ENTITY) {
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        }
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);

        int tint = FluxColorHandler.getItemColor(stack);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        FluxModelRenderer.render(
            t,
            model,
            fluxBlock.getModelIcons(fluxBlock.isItemModelConnected()),
            null,
            null,
            tint,
            BlockFluxCore.TEX_COLOUR,
            false,
            0,
            0,
            0);
        if (BlockFluxRenderer.hasConnections(block)) {
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                FluxModelRenderer.render(
                    t,
                    FluxBlockModels.CONNECTION,
                    fluxBlock.getModelIcons(true),
                    null,
                    dir,
                    tint,
                    BlockFluxCore.TEX_COLOUR,
                    false,
                    0,
                    0,
                    0);
            }
        }
        t.draw();

        if (block instanceof BlockFluxStorage) {
            renderStorageGauge(stack, (BlockFluxStorage) block);
        }
        GL11.glPopMatrix();
    }

    /** 원작 ItemFluxStorageRenderer: FLUX_DATA의 에너지와 네트워크 색으로 게이지 */
    private static void renderStorageGauge(ItemStack stack, BlockFluxStorage block) {
        if (!stack.hasTagCompound() || !stack.getTagCompound()
            .hasKey(FluxUtils.FLUX_DATA, Constants.NBT.TAG_COMPOUND)) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound()
            .getCompoundTag(FluxUtils.FLUX_DATA);
        int id = tag.hasKey(FluxNetworkData.NETWORK_ID) ? tag.getInteger(FluxNetworkData.NETWORK_ID) : -1;
        // 원작과 다름: 알파를 255로 고정한다 (게이지는 알파로 블렌딩하므로 캐시 색에 알파가 없으면 보이지 않는다)
        int colour = FluxColorHandler.getOrRequestNetworkColor(id) | 0xff000000;
        // 아이템을 그리는 쪽(GUI, 손, 엔티티)마다 GL 상태가 다르므로 블렌딩 상태를 되돌려 놓는다
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        TileFluxStorageRenderer
            .render(tag.getLong(FluxStorageHandler.NBT_ENERGY), block.getMaxStorage(), colour, 0.0D, 0.0D, 0.0D);
        GL11.glPopAttrib();
    }
}
