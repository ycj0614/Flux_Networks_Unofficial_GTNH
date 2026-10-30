package com.ycj0614.fluxnetworks.client.gui.tab;

import java.util.ArrayList;
import java.util.Comparator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.gui.EnumNavigationTabs;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiDraw;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiTabPages;
import com.ycj0614.fluxnetworks.client.gui.popups.GuiPopNetworkPassword;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.item.ItemConfigurator;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** 네트워크 선택 탭 */
@SideOnly(Side.CLIENT)
public class GuiTabSelection extends GuiTabPages<IFluxNetwork> {

    public IFluxNetwork selectedNetwork;

    protected int timer2;

    public GuiTabSelection(EntityPlayer player, INetworkConnector connector) {
        super(player, connector);
        gridStartX = 15;
        gridStartY = 22;
        gridHeight = 13;
        gridPerPage = 10;
        elementHeight = 12;
        elementWidth = 146;
    }

    @Override
    public EnumNavigationTabs getNavigationTab() {
        return EnumNavigationTabs.TAB_SELECTION;
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        super.drawForegroundLayer(mouseX, mouseY);
        if (elements.isEmpty()) {
            renderNavigationPrompt(FluxTranslate.ERROR_NO_NETWORK.t(), FluxTranslate.TAB_CREATE.t());
        } else {
            String amount = FluxTranslate.TOTAL.t() + ": " + elements.size();
            fontRendererObj.drawString(amount, 158 - fontRendererObj.getStringWidth(amount), 10, 0xffffff);
            fontRendererObj.drawString(
                FluxTranslate.SORT_BY.t() + ": " + EnumChatFormatting.AQUA + sortType.getTranslatedName(),
                19,
                10,
                0xffffff);
            if (!hasActivePopup()) {
                drawCenteredString(
                    fontRendererObj,
                    EnumChatFormatting.RED + FluxNetworks.proxy.getFeedback(false)
                        .getInfo(),
                    88,
                    150,
                    0xffffff);
            }
        }
    }

    @Override
    protected void onElementClicked(IFluxNetwork element, int mouseButton) {
        if (mouseButton == 0) {
            selectedNetwork = element;
            setConnectedNetwork(element.getNetworkID(), "");
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        configureNavigationButtons(EnumNavigationTabs.TAB_SELECTION, navigationTabs);
    }

    @Override
    public void renderElement(IFluxNetwork element, int x, int y) {
        renderNetworkBar(element, x, y, 16);
    }

    /** 네트워크 막대 한 줄. textureY는 gui_bar.png의 막대 위치 (16 = 한 줄, 32 = 두 줄 높이) */
    protected void renderNetworkBar(IFluxNetwork element, int x, int y, int textureY) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        mc.getTextureManager()
            .bindTexture(GuiDraw.GUI_BAR);

        int color = element.getSetting(NetworkSettings.NETWORK_COLOR);

        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;

        boolean selected = connector.getNetworkID() == element.getNetworkID();
        boolean isEncrypted = element.getSetting(NetworkSettings.NETWORK_SECURITY)
            .isEncrypted();

        if (isEncrypted) {
            if (selected) {
                drawTexturedModalRect(x + 131, y, 159, 16, 16, elementHeight);
            } else {
                drawTexturedModalRect(x + 131, y, 175, 16, 16, elementHeight);
            }
        }

        String text = element.getSetting(NetworkSettings.NETWORK_NAME);

        if (selected) {
            GL11.glColor3f(f, f1, f2);
            drawTexturedModalRect(x, y, 0, textureY, elementWidth, elementHeight);
            fontRendererObj.drawString(text, x + 4, y + 2, 0xffffff);
        } else {
            GL11.glColor3f(f * 0.75f, f1 * 0.75f, f2 * 0.75f);
            drawTexturedModalRect(x, y, 0, textureY, elementWidth, elementHeight);
            fontRendererObj.drawString(text, x + 4, y + 2, 0x404040);
        }
        GL11.glColor3f(1.0f, 1.0f, 1.0f);

        GL11.glPopMatrix();
    }

    @Override
    public void renderElementTooltip(IFluxNetwork element, int mouseX, int mouseY) {}

    @Override
    public void mouseMainClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseMainClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            if (mouseX > guiLeft + 45 && mouseX < guiLeft + 75 && mouseY > guiTop + 10 && mouseY < guiTop + 17) {
                sortType = FluxUtils.incrementEnum(sortType, SortType.values());
                sortGrids(sortType);
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (timer2 == 0) {
            refreshPages(new ArrayList<>(FluxNetworkCache.instance.getAllClientNetworks()));
        }
        if (FluxNetworks.proxy.getFeedback(true) == EnumFeedbackInfo.SUCCESS) {
            closePopUp();
            if (connector instanceof ItemConfigurator.NetworkConnector && selectedNetwork != null) {
                ItemConfigurator.NetworkConnector networkConnector = (ItemConfigurator.NetworkConnector) connector;
                networkConnector.network = selectedNetwork;
                networkConnector.networkID = selectedNetwork.getNetworkID();

                this.network = selectedNetwork;
                this.networkValid = !selectedNetwork.isInvalid();
            }
            // 원작과 다름: 원작은 SUCCESS를 지우지 않아 팝업이 다시 닫히기를 반복했다
            FluxNetworks.proxy.setFeedback(EnumFeedbackInfo.NONE, true);
        }
        if (FluxNetworks.proxy.getFeedback(true) == EnumFeedbackInfo.PASSWORD_REQUIRE) {
            openPopUp(new GuiPopNetworkPassword(this, player, connector));
            FluxNetworks.proxy.setFeedback(EnumFeedbackInfo.NONE, true);
        }
        timer2++;
        timer2 %= 10;
    }

    @Override
    protected void sortGrids(SortType sortType) {
        switch (sortType) {
            case ID:
                elements.sort(Comparator.comparing(IFluxNetwork::getNetworkID));
                refreshCurrentPageInternal();
                break;
            case NAME:
                elements.sort(Comparator.comparing(IFluxNetwork::getNetworkName));
                refreshCurrentPageInternal();
                break;
        }
    }
}
