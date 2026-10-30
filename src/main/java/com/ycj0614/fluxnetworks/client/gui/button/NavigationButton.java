package com.ycj0614.fluxnetworks.client.gui.button;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.api.gui.EnumNavigationTabs;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiButtonCore;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiDraw;

/**
 * GUI 위쪽의 탭 버튼. 아이콘은 gui_button.png의 (16 × 탭 순서, 0) 칸
 */
public class NavigationButton extends GuiButtonCore {

    public EnumNavigationTabs tab;
    public boolean isCurrentTab = false;

    public NavigationButton(int x, int y, EnumNavigationTabs tab) {
        super(x, y, 16, 16, 0);
        this.tab = tab;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, int guiLeft, int guiTop) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0f);
        mc.getTextureManager()
            .bindTexture(GuiDraw.BUTTONS);
        // 원작과 다름: 원작은 여기서 guiLeft·guiTop을 빼지 않아 마우스가 다른 곳에 있을 때 강조됐다
        boolean hovered = isMouseHovered(mc, mouseX - guiLeft, mouseY - guiTop);
        drawTexturedModalRect(x, y, 16 * tab.ordinal(), 16 * getHoverState(isCurrentTab || hovered), 16, 16);

        if (hovered) {
            FontRenderer fontRenderer = mc.fontRenderer;
            String text = tab.getTranslatedName();
            fontRenderer.drawString(text, x - fontRenderer.getStringWidth(text) / 2 + 8, y - 10, 0xFFFFFF);
        }
        GL11.glPopMatrix();
    }

    public NavigationButton setMain() {
        isCurrentTab = true;
        return this;
    }
}
