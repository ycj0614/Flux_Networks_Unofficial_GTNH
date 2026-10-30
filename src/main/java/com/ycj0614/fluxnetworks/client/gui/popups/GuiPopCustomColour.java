package com.ycj0614.fluxnetworks.client.gui.popups;

import net.minecraft.entity.player.EntityPlayer;

import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiFluxCore;
import com.ycj0614.fluxnetworks.client.gui.button.NormalButton;
import com.ycj0614.fluxnetworks.client.gui.button.TextboxButton;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** 네트워크 색을 16진수로 직접 입력하는 팝업 (색 칸 우클릭) */
@SideOnly(Side.CLIENT)
public class GuiPopCustomColour extends GuiPopCore<GuiFluxCore> {

    public TextboxButton customColor;
    public NormalButton colorApply;
    public int currentColour;

    public GuiPopCustomColour(GuiFluxCore host, int currentColour, EntityPlayer player, INetworkConnector connector) {
        super(host, player, connector);
        this.currentColour = currentColour;
    }

    @Override
    public void initGui() {
        super.initGui();
        popButtons.add(new NormalButton(FluxTranslate.CANCEL.t(), 40, 86, 36, 12, 11));
        colorApply = new NormalButton(FluxTranslate.APPLY.t(), 100, 86, 36, 12, 12);
        popButtons.add(colorApply);

        customColor = TextboxButton.create(this, "0x", 7, fontRendererObj, 57, 64, 64, 12)
            .setHexOnly();
        customColor.setMaxStringLength(6);
        // 원작과 다름: 원작은 앞의 0을 빼고 넣어 "00ff00" 같은 색이 6자가 되지 않았다
        customColor.setText(String.format("%06x", currentColour & 0xffffff));

        popBoxes.add(customColor);
    }

    @Override
    public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawRectWithBackground(30, 44, 60, 118, 0xccffffff, 0x80000000);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        drawCenteredString(fontRendererObj, FluxTranslate.CUSTOM_COLOR.t(), 88, 48, 0xffffff);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            for (NormalButton button : popButtons) {
                if (button.clickable && button.isMouseHovered(mc, mouseX - guiLeft, mouseY - guiTop)) {
                    if (button.id == 11) {
                        host.closePopUp();
                    }
                    if (button.id == 12) {
                        currentColour = customColor.getIntegerFromHex();
                        host.closePopUp();
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void keyTyped(char c, int k) {
        super.keyTyped(c, k);
        if (colorApply != null) {
            colorApply.clickable = customColor.getText()
                .length() == 6;
        }
    }
}
