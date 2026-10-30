package com.ycj0614.fluxnetworks.client.gui.tab;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.gui.EnumNavigationTabs;
import com.ycj0614.fluxnetworks.api.gui.EnumNetworkColor;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiButtonCore;
import com.ycj0614.fluxnetworks.client.gui.button.ColorButton;
import com.ycj0614.fluxnetworks.client.gui.button.NormalButton;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.network.PacketGeneral;
import com.ycj0614.fluxnetworks.common.network.PacketGeneralHandler;
import com.ycj0614.fluxnetworks.common.network.PacketGeneralType;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** 네트워크 설정 탭: 이름·보안·비밀번호·색 변경, 삭제 (왼쪽 Shift를 두 번 누르면 삭제 버튼이 켜진다) */
@SideOnly(Side.CLIENT)
public class GuiTabSettings extends GuiTabEditAbstract {

    public NormalButton apply, delete;
    public int deleteCount;

    public GuiTabSettings(EntityPlayer player, INetworkConnector connector) {
        super(player, connector);
        if (networkValid) {
            securityType = network.getSetting(NetworkSettings.NETWORK_SECURITY);
        }
    }

    @Override
    public EnumNavigationTabs getNavigationTab() {
        return EnumNavigationTabs.TAB_SETTING;
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        super.drawForegroundLayer(mouseX, mouseY);
        if (networkValid && delete != null) {
            if (mouseX > 30 + guiLeft && mouseX < 66 + guiLeft && mouseY > 140 + guiTop && mouseY < 152 + guiTop) {
                if (delete.clickable) {
                    drawCenteredString(
                        fontRendererObj,
                        EnumChatFormatting.BOLD + FluxTranslate.DELETE_NETWORK.t(),
                        48,
                        128,
                        0xff0000);
                } else {
                    drawCenteredString(fontRendererObj, FluxTranslate.DOUBLE_SHIFT.t(), 48, 128, 0xffffff);
                }
            }
            drawCenteredString(
                fontRendererObj,
                EnumChatFormatting.RED + FluxNetworks.proxy.getFeedback(false)
                    .getInfo(),
                88,
                156,
                0xffffff);
        } else {
            renderNavigationPrompt(FluxTranslate.ERROR_NO_SELECTED.t(), FluxTranslate.TAB_SELECTION.t());
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        if (networkValid) {
            name.setText(network.getNetworkName());

            password.setText(network.getSetting(NetworkSettings.NETWORK_PASSWORD));
            password.setVisible(
                network.getSetting(NetworkSettings.NETWORK_SECURITY)
                    .isEncrypted());

            buttons.add(apply = new NormalButton(FluxTranslate.APPLY.t(), 112, 140, 36, 12, 3).setUnclickable());
            buttons.add(delete = new NormalButton(FluxTranslate.DELETE.t(), 30, 140, 36, 12, 4).setUnclickable());

            int i = 0;
            boolean colorSet = false;
            int networkColor = network.getSetting(NetworkSettings.NETWORK_COLOR);
            for (EnumNetworkColor color : EnumNetworkColor.values()) {
                ColorButton b = new ColorButton(
                    48 + ((i >= 7 ? i - 7 : i) * 16),
                    96 + ((i >= 7 ? 1 : 0) * 16),
                    color.color);
                colorButtons.add(b);
                if (!colorSet && color.color == networkColor) {
                    this.color = b;
                    this.color.selected = true;
                    colorSet = true;
                }
                i++;
            }
            if (!colorSet) {
                // 원작과 다름: 원작은 화면 좌표(width / 2 - 56, height / 2 + 29)를 넣었다.
                // 버튼은 GUI 왼쪽 위 기준으로 그리므로 그 값에 해당하는 GUI 좌표(32, 112)를 쓴다
                ColorButton c = new ColorButton(32, 112, networkColor);
                colorButtons.add(c);
                this.color = c;
                this.color.selected = true;
            }
        }
    }

    @Override
    public void onEditSettingsChanged() {
        if (networkValid && apply != null) {
            apply.clickable = (!securityType.isEncrypted() || !password.getText()
                .isEmpty()) && !name.getText()
                    .isEmpty();
        }
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (networkValid && apply != null && button instanceof NormalButton) {
            switch (button.id) {
                case 3:
                    PacketHandler.network.sendToServer(
                        new PacketGeneral.GeneralMessage(
                            PacketGeneralType.EDIT_NETWORK,
                            PacketGeneralHandler.getNetworkEditPacket(
                                network.getNetworkID(),
                                name.getText(),
                                color.color,
                                securityType,
                                password.getText())));
                    break;
                case 4:
                    PacketHandler.network.sendToServer(
                        new PacketGeneral.GeneralMessage(
                            PacketGeneralType.DELETE_NETWORK,
                            PacketGeneralHandler.getDeleteNetworkPacket(network.getNetworkID())));
                    break;
            }
        }
    }

    @Override
    protected void keyTypedMain(char c, int k) {
        super.keyTypedMain(c, k);
        if (delete != null) {
            // 42 = 왼쪽 Shift
            if (k == 42) {
                deleteCount++;
                if (deleteCount > 1) {
                    delete.clickable = true;
                }
            } else {
                deleteCount = 0;
                delete.clickable = false;
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (FluxNetworks.proxy.getFeedback(true) == EnumFeedbackInfo.SUCCESS) {
            switchTab(EnumNavigationTabs.TAB_HOME, player, connector);
            FluxNetworks.proxy.setFeedback(EnumFeedbackInfo.NONE, true);
        }
        if (FluxNetworks.proxy.getFeedback(true) == EnumFeedbackInfo.SUCCESS_2) {
            if (apply != null) {
                apply.clickable = false;
            }
            FluxNetworks.proxy.setFeedback(EnumFeedbackInfo.NONE, true);
        }
    }
}
