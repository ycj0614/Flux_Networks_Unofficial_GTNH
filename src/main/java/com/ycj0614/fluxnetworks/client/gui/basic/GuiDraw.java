package com.ycj0614.fluxnetworks.client.gui.basic;

import java.awt.Color;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.FluxNetworks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

//// 그리기 메서드와 텍스처만 둔다 \\\\
@SideOnly(Side.CLIENT)
public abstract class GuiDraw extends GuiContainer implements ITextBoxButton {

    public static final ResourceLocation BACKGROUND = new ResourceLocation(
        FluxNetworks.MODID,
        "textures/gui/gui_default_background.png");
    public static final ResourceLocation FRAME = new ResourceLocation(
        FluxNetworks.MODID,
        "textures/gui/gui_default_frame.png");
    public static final ResourceLocation GUI_BAR = new ResourceLocation(FluxNetworks.MODID, "textures/gui/gui_bar.png");
    public static final ResourceLocation INVENTORY = new ResourceLocation(
        FluxNetworks.MODID,
        "textures/gui/inventory_configuration.png");
    public static final ResourceLocation BUTTONS = new ResourceLocation(
        FluxNetworks.MODID,
        "textures/gui/gui_button.png");

    public GuiDraw(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    /** 인벤토리 키인지 확인 (1.12의 keyBindInventory.isActiveAndMatches 대체) */
    protected boolean isInventoryKey(int keyCode) {
        return keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode();
    }

    protected void renderNetwork(String name, int color, int x, int y) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        GL11.glColor3f(f, f1, f2);

        mc.getTextureManager()
            .bindTexture(GUI_BAR);
        drawTexturedModalRect(x, y, 0, 0, 135, 12);
        fontRendererObj.drawString(name, x + 4, y + 2, 0xffffff);

        GL11.glPopMatrix();
    }

    protected void renderNavigationPrompt(String error, String prompt) {
        GL11.glPushMatrix();
        drawCenteredString(fontRendererObj, error, xSize / 2, 16, 0x808080);
        GL11.glScaled(0.625, 0.625, 0.625);
        drawCenteredString(
            fontRendererObj,
            I18n.format("info.fluxnetworks.clickabove", EnumChatFormatting.AQUA + prompt + EnumChatFormatting.RESET),
            (int) (xSize / 2 * 1.6),
            (int) (26 * 1.6),
            0x808080);
        GL11.glScaled(1.6, 1.6, 1.6);
        GL11.glPopMatrix();
    }

    protected void renderItemStack(ItemStack stack, int x, int y) {
        renderItemStack(stack, x, y, "");
    }

    protected void renderItemStack(ItemStack stack, int x, int y, String text) {
        if (stack == null) {
            return;
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glTranslatef(0.0F, 0.0F, 32.0F);
        this.zLevel = 200.0F;
        itemRender.zLevel = 200.0F;

        RenderHelper.enableGUIStandardItemLighting();
        itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, x, y);
        itemRender.renderItemOverlayIntoGUI(fontRendererObj, mc.getTextureManager(), stack, x, y, text);
        RenderHelper.disableStandardItemLighting();

        this.zLevel = 0.0F;
        itemRender.zLevel = 0.0F;
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    public void drawColorRect(int x, int y, int height, int width, int color) {
        drawRect(x - 1, y - 1, x + width + 1, y, color);
        drawRect(x - 1, y + height, x + width + 1, y + height + 1, color);
        drawRect(x - 1, y, x, y + height, color);
        drawRect(x + width, y, x + width + 1, y + height, color);
    }

    public void drawRectWithBackground(int x, int y, int height, int width, int frameColor, int backColor) {
        drawRect(x - 1, y - 1, x + width + 1, y, frameColor);
        drawRect(x - 1, y + height, x + width + 1, y + height + 1, frameColor);
        drawRect(x, y, x + width, y + height, backColor);
    }

    protected void drawHoverTooltip(List<String> strings, int x, int y) {
        int maxLength = 0;
        for (String s : strings) {
            maxLength = Math.max(fontRendererObj.getStringWidth(s), maxLength);
        }
        drawRectWithBackground(x, y, strings.size() * 9 + 3, maxLength + 4, 0x80ffffff, 0xc0000000);
        int i = 0;
        for (String s : strings) {
            fontRendererObj.drawString(s, x + 2, y + 2 + 9 * i, 0xffffff);
            i++;
        }
    }

    protected void drawHoverTooltip(String text, int x, int y) {
        int maxLength = fontRendererObj.getStringWidth(text);
        drawRect(x, y, x + maxLength + 4, y + 12, 0x80000000);
        fontRendererObj.drawString(text, x + 2, y + 2, Color.GREEN.getRGB());
    }
}
