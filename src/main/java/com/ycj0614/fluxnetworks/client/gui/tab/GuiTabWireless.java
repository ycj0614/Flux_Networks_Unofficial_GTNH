package com.ycj0614.fluxnetworks.client.gui.tab;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.gui.EnumNavigationTabs;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.network.WirelessType;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiButtonCore;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiTabCore;
import com.ycj0614.fluxnetworks.client.gui.button.InventoryButton;
import com.ycj0614.fluxnetworks.client.gui.button.NormalButton;
import com.ycj0614.fluxnetworks.client.gui.button.SlidedSwitchButton;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.network.PacketGeneral;
import com.ycj0614.fluxnetworks.common.network.PacketGeneralHandler;
import com.ycj0614.fluxnetworks.common.network.PacketGeneralType;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 무선 충전 탭 (원작 그대로). 아래 스위치로 무선을 켜고, 인벤토리 그림의 칸을 눌러 충전할 슬롯 그룹을 고른 뒤 적용한다.
 * <p>
 * 원작과 다름: 1.7.10에는 왼손이 없어 왼손 칸을 그리지 않는다. 설정 비트는 서버 값을 그대로 돌려보낸다.
 */
@SideOnly(Side.CLIENT)
public class GuiTabWireless extends GuiTabCore {

    public List<InventoryButton> inventoryButtonList = new ArrayList<>();
    public NormalButton apply;

    public boolean[] settings = new boolean[WirelessType.values().length];

    public GuiTabWireless(EntityPlayer player, INetworkConnector connector) {
        super(player, connector);
    }

    @Override
    public EnumNavigationTabs getNavigationTab() {
        return EnumNavigationTabs.TAB_WIRELESS;
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        super.drawForegroundLayer(mouseX, mouseY);
        if (networkValid) {
            int colour = network.getSetting(NetworkSettings.NETWORK_COLOR);
            drawCenteredString(fontRendererObj, FluxTranslate.TAB_WIRELESS.t(), 88, 12, 0xb4b4b4);
            fontRendererObj.drawString(FluxTranslate.ENABLE_WIRELESS.t(), 20, 156, colour);
            drawCenteredString(
                fontRendererObj,
                EnumChatFormatting.RED + FluxNetworks.proxy.getFeedback(false)
                    .getInfo(),
                88,
                146,
                0xffffff);
        } else {
            renderNavigationPrompt(FluxTranslate.ERROR_NO_SELECTED.t(), FluxTranslate.TAB_SELECTION.t());
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        configureNavigationButtons(EnumNavigationTabs.TAB_WIRELESS, navigationTabs);
        inventoryButtonList.clear();
        buttonLists.add(inventoryButtonList);
        if (networkValid) {
            int setting = network.getSetting(NetworkSettings.NETWORK_WIRELESS);
            for (WirelessType types : WirelessType.values()) {
                settings[types.ordinal()] = types.isActivated(setting);
            }

            switches.add(new SlidedSwitchButton(140, 156, 4, guiLeft, guiTop, settings[0]));
            inventoryButtonList
                .add(new InventoryButton(WirelessType.ARMOR_SLOT, this, settings, 24, 32, 0, 80, 52, 16));
            InventoryButton baubles = new InventoryButton(WirelessType.BAUBLES, this, settings, 100, 32, 0, 80, 52, 16);
            // TODO 배치 V: 바우블 모드(Loader.isModLoaded("Baubles"))가 있을 때만 누를 수 있게 한다
            baubles.clickable = false;
            inventoryButtonList.add(baubles);
            inventoryButtonList.add(new InventoryButton(WirelessType.INVENTORY, this, settings, 32, 56, 0, 0, 112, 40));
            inventoryButtonList
                .add(new InventoryButton(WirelessType.HOT_BAR, this, settings, 32, 104, 112, 0, 112, 16));
            inventoryButtonList
                .add(new InventoryButton(WirelessType.RIGHT_HAND, this, settings, 136, 128, 52, 80, 16, 16));
            // 원작과 다름: 원작의 LEFT_HAND 칸(24, 128)은 1.7.10에 왼손이 없으므로 넣지 않는다

            apply = new NormalButton(FluxTranslate.APPLY.t(), 73, 130, 32, 12, 0).setUnclickable();
            buttons.add(apply);
        }
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (mouseButton != 0 || !networkValid) {
            return;
        }
        if (button instanceof InventoryButton) {
            switchSetting(((InventoryButton) button).chargeType);
        }
        if (button instanceof NormalButton && button.id == 0) {
            int wireless = (settings[0] ? 1 : 0) | (settings[1] ? 1 : 0) << 1
                | (settings[2] ? 1 : 0) << 2
                | (settings[3] ? 1 : 0) << 3
                | (settings[4] ? 1 : 0) << 4
                | (settings[5] ? 1 : 0) << 5;
            PacketHandler.network.sendToServer(
                new PacketGeneral.GeneralMessage(
                    PacketGeneralType.CHANGE_WIRELESS,
                    PacketGeneralHandler.getChangeWirelessPacket(network.getNetworkID(), wireless)));
        }
        if (button instanceof SlidedSwitchButton) {
            ((SlidedSwitchButton) button).switchButton();
            if (button.id == 4) {
                switchSetting(WirelessType.ENABLE_WIRELESS);
            }
        }
    }

    public void switchSetting(WirelessType type) {
        settings[type.ordinal()] = !settings[type.ordinal()];
        apply.clickable = true;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (FluxNetworks.proxy.getFeedback(true) == EnumFeedbackInfo.SUCCESS) {
            if (apply != null) {
                apply.clickable = false;
            }
            FluxNetworks.proxy.setFeedback(EnumFeedbackInfo.NONE, true);
        }
    }
}
