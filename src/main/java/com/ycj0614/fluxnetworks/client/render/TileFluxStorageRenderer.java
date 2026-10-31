package com.ycj0614.fluxnetworks.client.render;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.common.tileentity.TileFluxStorage;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 원작 TileFluxStorageRenderer. 스토리지 안쪽에 저장량만큼 네트워크 색 게이지를 더하기 블렌딩으로 그린다.
 * 원작 GlStateManager 호출을 GL11로, EnumFacing의 각도·오프셋을 숫자로 옮겼다.
 */
@SideOnly(Side.CLIENT)
public class TileFluxStorageRenderer extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileFluxStorage)) {
            return;
        }
        TileFluxStorage te = (TileFluxStorage) tile;
        GL11.glEnable(GL11.GL_BLEND);
        render(te.getTransferBuffer(), te.getMaxTransferLimit(), te.color | 0xff000000, x, y, z);
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void render(long stored, long capacity, int colour, double x, double y, double z) {
        if (stored == 0 || capacity == 0) {
            return;
        }
        stored = Math.min(stored, capacity);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, 1, 1, 0);
        float full = 1 - 0.0625F * 3;
        float bottom = 0 + 0.0625F * 2;
        float left = 0 + 0.0625F * 2;
        float i = Math.max(0.0625F * 2.2F, ((float) stored * full / capacity) + bottom);

        float f3 = (float) (colour >> 24 & 255) / 255.0F;
        float f = (float) (colour >> 16 & 255) / 255.0F;
        float f1 = (float) (colour >> 8 & 255) / 255.0F;
        float f2 = (float) (colour & 255) / 255.0F;
        GL11.glColor4f(f, f1, f2, f3);

        // SOUTH (각도 0)
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0, 0.0626F);
        drawRect(left, i, 1 - 0.0625F * 2, bottom);
        GL11.glPopMatrix();

        // NORTH (각도 180)
        GL11.glPushMatrix();
        GL11.glRotatef(180, 0, 1, 0);
        GL11.glTranslated(-1, 0, -1 + 0.0625);
        drawRect(left, i, 1 - 0.0625F * 2, bottom);
        GL11.glPopMatrix();

        // EAST (각도 270)
        GL11.glPushMatrix();
        GL11.glRotatef(270, 0, 1, 0);
        GL11.glTranslated(0, 0, -1 + 0.0625);
        drawRect(left, i, 1 - 0.0625F * 2, bottom);
        GL11.glPopMatrix();

        // WEST (각도 90)
        GL11.glPushMatrix();
        GL11.glRotatef(90, 0, 1, 0);
        GL11.glTranslated(-1, 0, 0.0625);
        drawRect(left, i, 1 - 0.0625F * 2, bottom);
        GL11.glPopMatrix();

        // 윗면
        GL11.glPushMatrix();
        GL11.glRotatef(90, 1, 0, 0);
        GL11.glTranslatef(0, 0, -i);
        drawRect(0 + 0.0625F, 1 - 0.0625F, 1 - 0.0625F, 0 + 0.0625F);
        GL11.glPopMatrix();

        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
        GL11.glColor4f(1, 1, 1, 1);
    }

    /** 원작 RenderUtils.drawRect (z = 0 평면의 사각형, 위치만) */
    private static void drawRect(float left, float top, float right, float bottom) {
        if (left < right) {
            float i = left;
            left = right;
            right = i;
        }
        if (top < bottom) {
            float j = top;
            top = bottom;
            bottom = j;
        }
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertex(left, bottom, 0.0D);
        t.addVertex(right, bottom, 0.0D);
        t.addVertex(right, top, 0.0D);
        t.addVertex(left, top, 0.0D);
        t.draw();
    }
}
