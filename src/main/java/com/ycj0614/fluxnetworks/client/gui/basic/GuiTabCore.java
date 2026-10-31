package com.ycj0614.fluxnetworks.client.gui.basic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.gui.EnumNavigationTabs;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.client.gui.GuiFluxAdminHome;
import com.ycj0614.fluxnetworks.client.gui.GuiFluxConfiguratorHome;
import com.ycj0614.fluxnetworks.client.gui.GuiFluxConnectorHome;
import com.ycj0614.fluxnetworks.client.gui.button.NavigationButton;
import com.ycj0614.fluxnetworks.client.gui.button.TextboxButton;
import com.ycj0614.fluxnetworks.client.gui.tab.GuiTabConnections;
import com.ycj0614.fluxnetworks.client.gui.tab.GuiTabCreate;
import com.ycj0614.fluxnetworks.client.gui.tab.GuiTabDetailedSelection;
import com.ycj0614.fluxnetworks.client.gui.tab.GuiTabMembers;
import com.ycj0614.fluxnetworks.client.gui.tab.GuiTabSelection;
import com.ycj0614.fluxnetworks.client.gui.tab.GuiTabSettings;
import com.ycj0614.fluxnetworks.client.gui.tab.GuiTabStatistics;
import com.ycj0614.fluxnetworks.client.gui.tab.GuiTabWireless;
import com.ycj0614.fluxnetworks.common.item.ItemAdminConfigurator;
import com.ycj0614.fluxnetworks.common.item.ItemConfigurator;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** 위쪽에 탭 버튼이 있는 GUI */
@SideOnly(Side.CLIENT)
public abstract class GuiTabCore extends GuiFluxCore {

    /** 원작 RegistrySounds.BUTTON_CLICK. 1.7.10은 sounds.json만 있으면 되고 등록 과정이 없다 */
    public static final ResourceLocation BUTTON_SOUND = new ResourceLocation(FluxNetworks.MODID, "button");

    protected List<NavigationButton> navigationButtons = new ArrayList<>();
    public EnumNavigationTabs[] navigationTabs;

    public GuiTabCore(EntityPlayer player, INetworkConnector connector) {
        super(player, connector);
        setDefaultTabs();
    }

    @Override
    public void initGui() {
        super.initGui();
        navigationButtons.clear();
        buttonLists.add(navigationButtons);
    }

    public abstract EnumNavigationTabs getNavigationTab();

    public void setDefaultTabs() {
        this.navigationTabs = EnumNavigationTabs.values();
    }

    public void setNavigationTabs(EnumNavigationTabs[] navigationTabs) {
        this.navigationTabs = navigationTabs;
    }

    /** ESC나 인벤토리 키: 홈 탭이면 닫고, 다른 탭이면 홈으로 간다 (텍스트 상자 입력 중이면 무시) */
    @Override
    protected void keyTypedMain(char c, int k) {
        super.keyTypedMain(c, k);
        if (k == 1 || isInventoryKey(k)) {
            if (textBoxes.stream()
                .noneMatch(TextboxButton::isFocused)) {
                if (this.getNavigationTab() == EnumNavigationTabs.TAB_HOME) {
                    mc.thePlayer.closeScreen();
                } else {
                    switchTab(EnumNavigationTabs.TAB_HOME, player, connector);
                    FluxNetworks.proxy.setFeedback(EnumFeedbackInfo.NONE, false);
                    playButtonSound();
                }
            }
        }
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && button instanceof NavigationButton) {
            switchTab(((NavigationButton) button).tab, player, connector);
            playButtonSound();
        }
    }

    public void configureNavigationButtons(EnumNavigationTabs currentTab, EnumNavigationTabs[] availableTabs) {
        int posCount = 0;
        for (EnumNavigationTabs tab : availableTabs) {
            NavigationButton button;
            if (tab != EnumNavigationTabs.TAB_CREATE) {
                button = new NavigationButton(12 + (18 * posCount), -16, tab);
                posCount++;
            } else {
                button = new NavigationButton(148, -16, tab);
            }
            // 원작과 다름: 원작은 navigationButtons.get(currentTab.ordinal())로 찾았다.
            // 탭 배열이 바뀌어도 맞도록 탭 값으로 찾는다
            if (tab == currentTab) {
                button.setMain();
            }
            navigationButtons.add(button);
        }
    }

    /** 설정에서 버튼 소리를 켰을 때만 재생 */
    public static void playButtonSound() {
        if (FluxConfig.enableButtonSound) {
            Minecraft.getMinecraft()
                .getSoundHandler()
                .playSound(PositionedSoundRecord.func_147674_a(BUTTON_SOUND, 1.0F));
        }
    }

    public static void switchTab(EnumNavigationTabs tab, EntityPlayer player, INetworkConnector connector) {
        GuiScreen screen = null;
        switch (tab) {
            case TAB_HOME:
                if (connector instanceof TileFluxCore) {
                    screen = new GuiFluxConnectorHome(player, (TileFluxCore) connector);
                } else if (connector instanceof ItemAdminConfigurator.AdminConnector) {
                    screen = new GuiFluxAdminHome(player, connector);
                } else if (connector instanceof ItemConfigurator.NetworkConnector) {
                    screen = new GuiFluxConfiguratorHome(player, (ItemConfigurator.NetworkConnector) connector);
                }
                break;
            case TAB_SELECTION:
                if (connector instanceof ItemAdminConfigurator.AdminConnector
                    && FluxNetworks.proxy.detailed_network_view) {
                    screen = new GuiTabDetailedSelection(player, connector);
                } else {
                    screen = new GuiTabSelection(player, connector);
                }
                break;
            case TAB_WIRELESS:
                screen = new GuiTabWireless(player, connector);
                break;
            case TAB_CONNECTION:
                screen = new GuiTabConnections(player, connector);
                break;
            case TAB_STATISTICS:
                screen = new GuiTabStatistics(player, connector);
                break;
            case TAB_MEMBER:
                screen = new GuiTabMembers(player, connector);
                break;
            case TAB_SETTING:
                screen = new GuiTabSettings(player, connector);
                break;
            case TAB_CREATE:
                screen = new GuiTabCreate(player, connector);
                break;
        }
        if (screen != null) {
            FMLCommonHandler.instance()
                .showGuiScreen(screen);
        } else {
            player.closeScreen();
        }
    }
}
