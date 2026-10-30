package com.ycj0614.fluxnetworks.client.gui.button;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.network.WirelessType;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiButtonCore;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiDraw;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiFluxCore;

/**
 * 무선 충전 탭의 인벤토리 칸 버튼.
 * 원작과 다름: 원작은 GuiTabWireless를 호스트로 받고 host.settings를 읽었다. 무선 탭은 4단계라
 * 호스트는 GuiFluxCore로, 켜짐 상태 배열은 생성자에서 따로 받는다.
 * TODO 4단계: GuiTabWireless를 만들면 그 settings 배열을 넘긴다 (원작: settings[chargeType.ordinal()]가 켜져 있으면 아래 칸 텍스처)
 */
public class InventoryButton extends GuiButtonCore {

    public WirelessType chargeType;
    private final int texX, texY;
    public GuiFluxCore host;
    private final boolean[] settings;

    public InventoryButton(WirelessType chargeType, GuiFluxCore host, boolean[] settings, int x, int y, int texX,
        int texY, int width, int height) {
        super(x, y, width, height, chargeType.ordinal());
        this.chargeType = chargeType;
        this.texX = texX;
        this.texY = texY;
        this.host = host;
        this.settings = settings;
        this.text = chargeType.getTranslatedName();
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, int guiLeft, int guiTop) {
        int colour = host.network.getSetting(NetworkSettings.NETWORK_COLOR);
        GL11.glColor4f((colour >> 16 & 255) / 255.0F, (colour >> 8 & 255) / 255.0F, (colour & 255) / 255.0F, 1.0f);
        boolean hover = isMouseHovered(mc, mouseX - guiLeft, mouseY - guiTop);
        mc.getTextureManager()
            .bindTexture(GuiDraw.INVENTORY);
        drawTexturedRectangular(x, y, texX, texY + height * (settings[id] ? 1 : 0), width, height);

        if (hover) {
            mc.fontRenderer
                .drawString(text, x + (width - mc.fontRenderer.getStringWidth(text)) / 2 + 1, y - 9, 0xFFFFFF);
        }
    }
}
