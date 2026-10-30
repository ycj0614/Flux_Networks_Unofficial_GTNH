package com.ycj0614.fluxnetworks.client.gui.basic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class GuiButtonCore extends Gui {

    public boolean clickable = true;

    public int x;
    public int y;

    public int width;
    public int height;

    public int id;

    protected String text;

    public GuiButtonCore(int x, int y, int width, int height, int id) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.id = id;
    }

    /** guiLeft·guiTop만큼 옮겨진 좌표계에서 그린다. mouseX·mouseY는 화면 좌표 */
    public abstract void drawButton(Minecraft mc, int mouseX, int mouseY, int guiLeft, int guiTop);

    public void updateButton(float partialTicks, int mouseX, int mouseY) {}

    protected int getHoverState(boolean b) {
        return b ? 1 : 0;
    }

    public boolean isMouseHovered(Minecraft mc, int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width && mouseY < this.y + this.height && mouseY >= this.y;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    /** 실수 좌표·크기를 받는 drawTexturedModalRect (256×256 텍스처 기준) */
    public void drawTexturedRectangular(double x, double y, double textureX, double textureY, double width,
        double height) {
        float f = 0.00390625F;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, zLevel, textureX * f, (textureY + height) * f);
        tessellator.addVertexWithUV(x + width, y + height, zLevel, (textureX + width) * f, (textureY + height) * f);
        tessellator.addVertexWithUV(x + width, y, zLevel, (textureX + width) * f, textureY * f);
        tessellator.addVertexWithUV(x, y, zLevel, textureX * f, textureY * f);
        tessellator.draw();
    }

    /** 실수 좌표를 받는 drawRect (페이지 표시줄처럼 칸 폭이 정수가 아닐 때 쓴다) */
    public static void drawRect(double left, double top, double right, double bottom, int color) {
        float f3 = (float) (color >> 24 & 255) / 255.0F;
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.instance;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glColor4f(f, f1, f2, f3);
        tessellator.startDrawingQuads();
        tessellator.addVertex(left, bottom, 0.0D);
        tessellator.addVertex(right, bottom, 0.0D);
        tessellator.addVertex(right, top, 0.0D);
        tessellator.addVertex(left, top, 0.0D);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
