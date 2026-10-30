package com.ycj0614.fluxnetworks.client.gui.button;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.client.gui.basic.GuiButtonCore;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiDraw;

/**
 * 밀어서 켜고 끄는 스위치
 */
public class SlidedSwitchButton extends GuiButtonCore {

    /** 켜짐/꺼짐 */
    public boolean slideControl = false;

    /** 손잡이 위치 (0~8) */
    protected float center;

    private final int guiLeft, guiTop;

    public SlidedSwitchButton(int x, int y, int id, int guiLeft, int guiTop, boolean defaultControl) {
        super(x, y, 16, 8, id);
        if (defaultControl) {
            slideControl = true;
            center = 8;
        }
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, int guiLeft, int guiTop) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0f);

        int s = getState(mc, mouseX, mouseY);

        mc.getTextureManager()
            .bindTexture(GuiDraw.BUTTONS);

        // 초록 배경
        drawTexturedRectangular(x, y, 32, 32, center * 2, 8);

        // 손잡이
        drawTexturedModalRect(x + (int) center, y, 16 * s, 40, 8, 8);

        // 테두리
        drawTexturedModalRect(x, y, 16 * s, 32, 16, 8);

        GL11.glPopMatrix();
    }

    private int getState(Minecraft mc, int mouseX, int mouseY) {
        return slideControl || isMouseHovered(mc, mouseX - guiLeft, mouseY - guiTop) ? 0 : 1;
    }

    public void switchButton() {
        slideControl = !slideControl;
    }

    @Override
    public void updateButton(float partialTicks, int mouseX, int mouseY) {
        float par = partialTicks * 4;
        if (slideControl) {
            if (center <= 8 - par) {
                center += par;
            } else {
                center = 8;
            }
        } else {
            if (center >= par) {
                center -= par;
            } else {
                center = 0;
            }
        }
    }
}
