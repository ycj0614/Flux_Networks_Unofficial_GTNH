package com.ycj0614.fluxnetworks.client.gui.button;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.client.gui.basic.GuiButtonCore;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiDraw;

/**
 * 연결 탭의 일괄 편집 아이콘 버튼 (gui_button.png의 (16 × id, 48) 칸)
 */
public class BatchEditButton extends GuiButtonCore {

    public BatchEditButton(int x, int y, int id, String text) {
        super(x, y, 12, 12, id);
        this.text = text;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, int guiLeft, int guiTop) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        if (clickable) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0f);
        } else {
            GL11.glColor4f(0.5F, 0.5F, 0.5F, 1.0f);
        }

        boolean b = isMouseHovered(mc, mouseX - guiLeft, mouseY - guiTop);

        mc.getTextureManager()
            .bindTexture(GuiDraw.BUTTONS);
        drawTexturedModalRect(x, y, 16 * id, 48 + (b ? 16 : 0), 12, 12);

        if (clickable && b) {
            mc.fontRenderer.drawString(text, x - mc.fontRenderer.getStringWidth(text) / 2 + 6, y - 9, 0xFFFFFF);
        }

        GL11.glPopMatrix();
    }

    public BatchEditButton setUnclickable() {
        clickable = false;
        return this;
    }
}
