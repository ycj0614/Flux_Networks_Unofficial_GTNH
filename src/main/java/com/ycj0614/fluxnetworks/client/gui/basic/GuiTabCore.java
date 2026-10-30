package com.ycj0614.fluxnetworks.client.gui.basic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.gui.EnumNavigationTabs;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.client.gui.button.NavigationButton;
import com.ycj0614.fluxnetworks.client.gui.button.TextboxButton;

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

    /** 원작과 다름: 무선 탭(4단계)은 뺀다 */
    public void setDefaultTabs() {
        List<EnumNavigationTabs> tabs = new ArrayList<>();
        for (EnumNavigationTabs tab : EnumNavigationTabs.values()) {
            // TODO 4단계: 무선 충전을 만들면 TAB_WIRELESS를 다시 넣는다 (원작: EnumNavigationTabs.values() 전부)
            if (tab != EnumNavigationTabs.TAB_WIRELESS) {
                tabs.add(tab);
            }
        }
        this.navigationTabs = tabs.toArray(new EnumNavigationTabs[0]);
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
            // 무선 탭을 빼면 순서가 밀리므로 탭 값으로 찾는다
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
        switch (tab) {
            case TAB_HOME:
                // TODO 배치 L: 홈 화면 열기 (원작: TileFluxCore면 GuiFluxConnectorHome, AdminConnector면 GuiFluxAdminHome,
                // NetworkConnector면 GuiFluxConfiguratorHome, 그 밖에는 player.closeScreen().
                // 화면 전환은 FMLCommonHandler.instance().showGuiScreen(...))
                player.closeScreen();
                break;
            case TAB_SELECTION:
                // TODO 배치 L: AdminConnector이고 proxy.detailed_network_view면 GuiTabDetailedSelection, 아니면 GuiTabSelection
                player.closeScreen();
                break;
            case TAB_WIRELESS:
                // TODO 4단계: GuiTabWireless
                player.closeScreen();
                break;
            case TAB_CONNECTION:
                // TODO 배치 L: GuiTabConnections
                player.closeScreen();
                break;
            case TAB_STATISTICS:
                // TODO 배치 L: GuiTabStatistics
                player.closeScreen();
                break;
            case TAB_MEMBER:
                // TODO 배치 L: GuiTabMembers
                player.closeScreen();
                break;
            case TAB_SETTING:
                // TODO 배치 L: GuiTabSettings
                player.closeScreen();
                break;
            case TAB_CREATE:
                // TODO 배치 L: GuiTabCreate
                player.closeScreen();
                break;
        }
    }
}
