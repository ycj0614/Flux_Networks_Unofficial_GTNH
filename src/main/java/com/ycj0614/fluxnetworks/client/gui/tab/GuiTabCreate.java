package com.ycj0614.fluxnetworks.client.gui.tab;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.gui.EnumNavigationTabs;
import com.ycj0614.fluxnetworks.api.gui.EnumNetworkColor;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.SecurityType;
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

/** 네트워크 생성 탭 */
@SideOnly(Side.CLIENT)
public class GuiTabCreate extends GuiTabEditAbstract {

    public NormalButton create;

    public GuiTabCreate(EntityPlayer player, INetworkConnector connector) {
        super(player, connector);
        securityType = SecurityType.ENCRYPTED;
    }

    @Override
    public EnumNavigationTabs getNavigationTab() {
        return EnumNavigationTabs.TAB_CREATE;
    }

    @Override
    public void initGui() {
        super.initGui();
        name.setText(mc.thePlayer.getCommandSenderName() + "'s Network");

        int i = 0;
        for (EnumNetworkColor color : EnumNetworkColor.values()) {
            colorButtons.add(new ColorButton(48 + (i >= 7 ? i - 7 : i) * 16, 96 + (i >= 7 ? 1 : 0) * 16, color.color));
            i++;
        }
        color = colorButtons.get(0);
        color.selected = true;

        buttons.add(create = new NormalButton(FluxTranslate.CREATE.t(), 70, 150, 36, 12, 3).setUnclickable());
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        super.drawForegroundLayer(mouseX, mouseY);
        renderNetwork(name.getText(), color.color, 20, 129);
        drawCenteredString(
            fontRendererObj,
            EnumChatFormatting.RED + FluxNetworks.proxy.getFeedback(false)
                .getInfo(),
            88,
            150,
            0xffffff);
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (button instanceof NormalButton) {
            if (mouseButton == 0 && button.id == 3) {
                PacketHandler.network.sendToServer(
                    new PacketGeneral.GeneralMessage(
                        PacketGeneralType.CREATE_NETWORK,
                        PacketGeneralHandler
                            .getCreateNetworkPacket(name.getText(), color.color, securityType, password.getText())));
            }
        }
    }

    @Override
    public void onEditSettingsChanged() {
        if (create != null) {
            create.clickable = (!securityType.isEncrypted() || !password.getText()
                .isEmpty()) && !name.getText()
                    .isEmpty();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (FluxNetworks.proxy.getFeedback(true) == EnumFeedbackInfo.SUCCESS) {
            switchTab(EnumNavigationTabs.TAB_SELECTION, player, connector);
            FluxNetworks.proxy.setFeedback(EnumFeedbackInfo.NONE, true);
        }
    }
}
