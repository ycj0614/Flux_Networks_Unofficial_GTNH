package com.ycj0614.fluxnetworks.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumNavigationTabs;
import com.ycj0614.fluxnetworks.api.network.AccessLevel;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiButtonCore;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiTabCore;
import com.ycj0614.fluxnetworks.client.gui.button.SlidedSwitchButton;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.network.PacketActivateSuperAdmin;
import com.ycj0614.fluxnetworks.common.network.PacketNetworkUpdateRequest;
import com.ycj0614.fluxnetworks.common.network.PacketPermissionRequest;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** 관리자 구성기 홈 화면: SuperAdmin 켜기·끄기, 자세한 네트워크 목록 보기 */
@SideOnly(Side.CLIENT)
public class GuiFluxAdminHome extends GuiTabCore {

    private int timer;
    public SlidedSwitchButton detailed_network_view, super_admin;

    public GuiFluxAdminHome(EntityPlayer player, INetworkConnector connector) {
        super(player, connector);
    }

    @Override
    public EnumNavigationTabs getNavigationTab() {
        return EnumNavigationTabs.TAB_HOME;
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        super.drawForegroundLayer(mouseX, mouseY);
        int color = network.getSetting(NetworkSettings.NETWORK_COLOR);
        renderNetwork(network.getSetting(NetworkSettings.NETWORK_NAME), color, 20, 8);
        drawCenteredString(
            fontRendererObj,
            EnumChatFormatting.RED + FluxNetworks.proxy.getFeedback(false)
                .getInfo(),
            89,
            150,
            0xffffff);

        fontRendererObj.drawString(AccessLevel.SUPER_ADMIN.getName(), 20, 30, color);
        fontRendererObj.drawString("Detailed Network View", 20, 42, color);
    }

    @Override
    public void initGui() {
        super.initGui();
        configureNavigationButtons(EnumNavigationTabs.TAB_HOME, navigationTabs);

        super_admin = new SlidedSwitchButton(140, 30, 0, guiLeft, guiTop, FluxNetworkCache.instance.superAdminClient);
        switches.add(super_admin);

        detailed_network_view = new SlidedSwitchButton(
            140,
            42,
            1,
            guiLeft,
            guiTop,
            FluxNetworks.proxy.detailed_network_view);
        switches.add(detailed_network_view);
    }

    @Override
    public void onSuperAdminChanged() {
        super.onSuperAdminChanged();
        if (super_admin != null) {
            super_admin.slideControl = FluxNetworkCache.instance.superAdminClient;
        }
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && button instanceof SlidedSwitchButton) {
            SlidedSwitchButton switchButton = (SlidedSwitchButton) button;
            switchButton.switchButton();
            switch (switchButton.id) {
                case 0:
                    PacketHandler.network.sendToServer(new PacketActivateSuperAdmin.ActivateSuperAdminMessage());
                    break;
                case 1:
                    FluxNetworks.proxy.detailed_network_view = switchButton.slideControl;
                    break;
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (timer == 0) {
            PacketHandler.network.sendToServer(
                new PacketNetworkUpdateRequest.UpdateRequestMessage(network.getNetworkID(), NBTType.NETWORK_GENERAL));
            PacketHandler.network.sendToServer(
                new PacketPermissionRequest.PermissionRequestMessage(
                    network.getNetworkID(),
                    player.getGameProfile()
                        .getId()));
        }
        timer++;
        timer %= 100;
    }
}
