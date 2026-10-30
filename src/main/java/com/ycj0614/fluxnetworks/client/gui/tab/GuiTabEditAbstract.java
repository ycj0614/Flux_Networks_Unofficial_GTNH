package com.ycj0614.fluxnetworks.client.gui.tab;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.ycj0614.fluxnetworks.api.gui.EnumNavigationTabs;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.SecurityType;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiButtonCore;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiTabCore;
import com.ycj0614.fluxnetworks.client.gui.button.ColorButton;
import com.ycj0614.fluxnetworks.client.gui.button.TextboxButton;
import com.ycj0614.fluxnetworks.client.gui.popups.GuiPopCore;
import com.ycj0614.fluxnetworks.client.gui.popups.GuiPopCustomColour;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.network.PacketGeneralHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 네트워크 설정·생성 탭의 공통 부분.
 * 원작과 다름: 에너지 종류(EnergyType) 줄이 없다. 이 모드는 EU만 쓴다
 */
@SideOnly(Side.CLIENT)
public abstract class GuiTabEditAbstract extends GuiTabCore {

    protected List<ColorButton> colorButtons = new ArrayList<>();

    protected SecurityType securityType;
    public ColorButton color;
    public TextboxButton name;
    public TextboxButton password;

    public GuiTabEditAbstract(EntityPlayer player, INetworkConnector connector) {
        super(player, connector);
    }

    public abstract void onEditSettingsChanged();

    @Override
    public void initGui() {
        super.initGui();
        colorButtons.clear();
        buttonLists.add(colorButtons);
        configureNavigationButtons(getNavigationTab(), navigationTabs);

        if (networkValid || getNavigationTab() == EnumNavigationTabs.TAB_CREATE) {
            int l = fontRendererObj.getStringWidth(FluxTranslate.NETWORK_NAME.t());
            name = TextboxButton.create(this, "", 1, fontRendererObj, 20 + l, 28, 140 - l, 12);
            name.setMaxStringLength(PacketGeneralHandler.MAX_NETWORK_NAME_LENGTH);

            l = fontRendererObj.getStringWidth(FluxTranslate.NETWORK_PASSWORD.t());
            password = TextboxButton.create(this, "", 2, fontRendererObj, 20 + l, 62, 140 - l, 12)
                .setTextInvisible();
            password.setMaxStringLength(PacketGeneralHandler.MAX_PASSWORD_LENGTH);
            password.setVisible(securityType.isEncrypted());

            textBoxes.add(name);
            textBoxes.add(password);
        }
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        super.drawForegroundLayer(mouseX, mouseY);
        // name이 null이면 initGui 때 네트워크가 없었던 것 (화면을 연 뒤 네트워크가 생겨도 다시 열 때까지 그리지 않는다)
        if ((getNavigationTab() == EnumNavigationTabs.TAB_CREATE || networkValid) && name != null) {
            drawCenteredString(fontRendererObj, getNavigationTab().getTranslatedName(), 88, 10, 0xb4b4b4);
            fontRendererObj.drawString(FluxTranslate.NETWORK_NAME.t() + ":", 14, 30, 0x606060);
            fontRendererObj.drawString(
                FluxTranslate.NETWORK_SECURITY.t() + ": " + EnumChatFormatting.AQUA + securityType.getName(),
                14,
                50,
                0x606060);
            if (securityType == SecurityType.ENCRYPTED) {
                fontRendererObj.drawString(FluxTranslate.NETWORK_PASSWORD.t() + ": ", 14, 64, 0x606060);
            }
            fontRendererObj.drawString(FluxTranslate.NETWORK_COLOR.t() + ":", 14, 97, 0x606060);
        }
    }

    @Override
    public void mouseMainClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseMainClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && password != null) {
            if (mouseX > guiLeft + 50 && mouseX < guiLeft + 150 && mouseY > guiTop + 48 && mouseY < guiTop + 60) {
                securityType = FluxUtils.incrementEnum(securityType, SecurityType.values());
                password.setText("");
                password.setVisible(securityType.isEncrypted());
                onEditSettingsChanged();
            }
        }
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (button instanceof ColorButton) {
            color.selected = false;
            color = (ColorButton) button;
            color.selected = true;
            onEditSettingsChanged();
            if (mouseButton == 1) {
                openPopUp(new GuiPopCustomColour(this, color.color, player, connector));
            }
        }
    }

    @Override
    public void onPopUpClose(GuiPopCore<?> popUp) {
        super.onPopUpClose(popUp);
        if (popUp instanceof GuiPopCustomColour) {
            this.color.color = ((GuiPopCustomColour) popUp).currentColour;
            onEditSettingsChanged();
        }
    }

    @Override
    protected void keyTypedMain(char c, int k) {
        super.keyTypedMain(c, k);
        onEditSettingsChanged();
    }
}
