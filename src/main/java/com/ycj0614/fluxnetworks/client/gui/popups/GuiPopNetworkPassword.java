package com.ycj0614.fluxnetworks.client.gui.popups;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.client.gui.button.NormalButton;
import com.ycj0614.fluxnetworks.client.gui.button.TextboxButton;
import com.ycj0614.fluxnetworks.client.gui.tab.GuiTabSelection;
import com.ycj0614.fluxnetworks.common.network.PacketGeneralHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** 비밀번호가 걸린 네트워크에 연결할 때 비밀번호를 받는 팝업 */
@SideOnly(Side.CLIENT)
public class GuiPopNetworkPassword extends GuiPopCore<GuiTabSelection> {

    public TextboxButton password;

    public GuiPopNetworkPassword(GuiTabSelection host, EntityPlayer player, INetworkConnector connector) {
        super(host, player, connector);
    }

    @Override
    public void initGui() {
        super.initGui();
        popButtons.add(new NormalButton(FluxTranslate.CANCEL.t(), 24, 86, 48, 12, 11));
        popButtons.add(new NormalButton(FluxTranslate.CONNECT.t(), 102, 86, 48, 12, 12));

        password = TextboxButton.create(this, "", 5, fontRendererObj, 70, 66, 81, 12);
        password.setTextInvisible();
        password.setMaxStringLength(PacketGeneralHandler.MAX_PASSWORD_LENGTH);

        popBoxes.add(password);
    }

    @Override
    public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        if (host.selectedNetwork != null) {
            drawCenteredString(
                fontRendererObj,
                FluxTranslate.CONNECTING_TO.t() + " " + host.selectedNetwork.getSetting(NetworkSettings.NETWORK_NAME),
                88,
                50,
                0xffffff);
        }
        drawCenteredString(fontRendererObj, FluxTranslate.NETWORK_PASSWORD.t() + ":", 40, 68, 0xffffff);

        drawCenteredString(
            fontRendererObj,
            EnumChatFormatting.RED + FluxNetworks.proxy.getFeedback(false)
                .getInfo(),
            88,
            110,
            0xffffff);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            for (NormalButton button : popButtons) {
                if (button.isMouseHovered(mc, mouseX - guiLeft, mouseY - guiTop)) {
                    if (button.id == 11) {
                        host.closePopUp();
                        return;
                    }
                    if (button.id == 12) {
                        if (!password.getText()
                            .isEmpty() && host.selectedNetwork != null) {
                            host.setConnectedNetwork(host.selectedNetwork.getNetworkID(), password.getText());
                            password.setText("");
                        }
                        return;
                    }
                }
            }
        }
    }
}
