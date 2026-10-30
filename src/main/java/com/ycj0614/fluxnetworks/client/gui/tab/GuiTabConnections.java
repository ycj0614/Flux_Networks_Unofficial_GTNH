package com.ycj0614.fluxnetworks.client.gui.tab;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.gui.EnumNavigationTabs;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.api.utils.Coord4D;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiButtonCore;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiDraw;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiTabPages;
import com.ycj0614.fluxnetworks.client.gui.button.BatchEditButton;
import com.ycj0614.fluxnetworks.client.gui.popups.GuiPopConnectionEdit;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.network.PacketBatchEditing;
import com.ycj0614.fluxnetworks.common.network.PacketConnectionUpdateRequest;
import com.ycj0614.fluxnetworks.common.network.PacketNetworkUpdateRequest;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** 연결 탭: 네트워크의 장치 목록. 좌클릭은 단일 편집, 우클릭은 일괄 편집 선택 */
@SideOnly(Side.CLIENT)
public class GuiTabConnections extends GuiTabPages<IFluxConnector> {

    private final List<BatchEditButton> editButtons = new ArrayList<>();

    public List<IFluxConnector> batchConnections = new ArrayList<>();
    public IFluxConnector singleConnection;

    public BatchEditButton clear, edit, disconnect;

    private int timer = 3;

    public GuiTabConnections(EntityPlayer player, INetworkConnector connector) {
        super(player, connector);
        gridStartX = 15;
        gridStartY = 22;
        gridHeight = 19;
        gridPerPage = 7;
        elementHeight = 18;
        elementWidth = 146;
        PacketHandler.network.sendToServer(
            new PacketNetworkUpdateRequest.UpdateRequestMessage(network.getNetworkID(), NBTType.NETWORK_CONNECTIONS));
    }

    @Override
    public EnumNavigationTabs getNavigationTab() {
        return EnumNavigationTabs.TAB_CONNECTION;
    }

    @Override
    public void initGui() {
        super.initGui();
        configureNavigationButtons(EnumNavigationTabs.TAB_CONNECTION, navigationTabs);
        editButtons.clear();
        buttonLists.add(editButtons);

        if (networkValid) {
            clear = new BatchEditButton(118, 8, 0, FluxTranslate.BATCH_CLEAR_BUTTON.t()).setUnclickable();
            edit = new BatchEditButton(132, 8, 1, FluxTranslate.BATCH_EDIT_BUTTON.t()).setUnclickable();
            disconnect = new BatchEditButton(146, 8, 2, FluxTranslate.BATCH_DISCONNECT_BUTTON.t()).setUnclickable();
            editButtons.add(clear);
            editButtons.add(edit);
            editButtons.add(disconnect);
            setBatchButtons(!batchConnections.isEmpty());
        }
    }

    private void setBatchButtons(boolean clickable) {
        if (clear != null) {
            clear.clickable = clickable;
            edit.clickable = clickable;
            disconnect.clickable = clickable;
        }
    }

    @Override
    protected void onElementClicked(IFluxConnector element, int mouseButton) {
        if (mouseButton == 0 && batchConnections.isEmpty() && element.isChunkLoaded()) {
            singleConnection = element;
            openPopUp(new GuiPopConnectionEdit(this, false, player, connector));
        }
        if (mouseButton == 1 || (mouseButton == 0 && !batchConnections.isEmpty())) {
            if (batchConnections.contains(element)) {
                batchConnections.remove(element);
                if (batchConnections.isEmpty()) {
                    setBatchButtons(false);
                }
            } else if (element.isChunkLoaded()) {
                batchConnections.add(element);
                setBatchButtons(true);
            }
        }
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        if (networkValid) {
            if (!batchConnections.isEmpty()) {
                fontRendererObj.drawString(
                    FluxTranslate.SELECTED.t() + ": " + EnumChatFormatting.AQUA + batchConnections.size(),
                    20,
                    10,
                    0xffffff);
            } else {
                fontRendererObj.drawString(
                    FluxTranslate.SORT_BY.t() + ": " + EnumChatFormatting.AQUA + FluxTranslate.SORTING_SMART.t(),
                    19,
                    10,
                    0xffffff);
            }
            super.drawForegroundLayer(mouseX, mouseY);
            if (!hasActivePopup()) {
                drawCenteredString(
                    fontRendererObj,
                    EnumChatFormatting.RED + FluxNetworks.proxy.getFeedback(false)
                        .getInfo(),
                    88,
                    165,
                    0xffffff);
            }
        } else {
            super.drawForegroundLayer(mouseX, mouseY);
            renderNavigationPrompt(FluxTranslate.ERROR_NO_SELECTED.t(), FluxTranslate.TAB_SELECTION.t());
        }
    }

    @Override
    public void renderElement(IFluxConnector element, int x, int y) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        mc.getTextureManager()
            .bindTexture(GuiDraw.GUI_BAR);
        int fontColor = 0xffffff;
        int color = element.getConnectionType().color;

        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;

        if (!batchConnections.isEmpty()) {
            if (batchConnections.contains(element)) {
                drawRect(x - 5, y + 1, x - 3, y + elementHeight - 1, 0xccffffff);
                drawRect(x + elementWidth + 3, y + 1, x + elementWidth + 5, y + elementHeight - 1, 0xccffffff);
                GL11.glColor3f(f, f1, f2);
                drawTexturedModalRect(x, y, 0, 32, elementWidth, elementHeight);
            } else {
                drawRect(x - 5, y + 1, x - 3, y + elementHeight - 1, 0xaa606060);
                drawRect(x + elementWidth + 3, y + 1, x + elementWidth + 5, y + elementHeight - 1, 0xaa606060);
                GL11.glColor3f(f * 0.5f, f1 * 0.5f, f2 * 0.5f);
                drawTexturedModalRect(x, y, 0, 32, elementWidth, elementHeight);
                fontColor = 0xd0d0d0;
            }
        } else {
            GL11.glColor3f(f, f1, f2);
            drawTexturedModalRect(x, y, 0, 32, elementWidth, elementHeight);
        }
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        renderItemStack(element.getDisplayStack(), x + 2, y + 1);
        if (element.isChunkLoaded()) {
            fontRendererObj.drawString(element.getCustomName(), x + 21, y + 2, fontColor);
            GL11.glScaled(0.625, 0.625, 0.625);
            fontRendererObj.drawString(
                FluxUtils.getTransferInfo(element.getConnectionType(), element.getTransferChange()),
                (int) ((x + 21) * 1.6),
                (int) ((y + 11) * 1.6),
                fontColor);
            GL11.glScaled(1.6, 1.6, 1.6);
        } else {
            fontRendererObj.drawString(element.getCustomName(), x + 21, y + 5, 0x808080);
        }
        GL11.glPopMatrix();
    }

    @Override
    public void renderElementTooltip(IFluxConnector element, int mouseX, int mouseY) {
        if (!hasActivePopup()) {
            drawHoverTooltip(getFluxInfo(element), mouseX + 4, mouseY - 16);
        }
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (button instanceof BatchEditButton) {
            switch (button.id) {
                case 0:
                    batchConnections.clear();
                    setBatchButtons(false);
                    break;
                case 1:
                    openPopUp(new GuiPopConnectionEdit(this, true, player, connector));
                    break;
                case 2:
                    boolean[] b = new boolean[PacketBatchEditing.EDITION_COUNT];
                    b[6] = true; // 연결 해제
                    PacketHandler.network.sendToServer(
                        new PacketBatchEditing.BatchEditingMessage(
                            network.getNetworkID(),
                            getCoords(batchConnections),
                            new NBTTagCompound(),
                            b));
                    break;
            }
        }
    }

    public static List<Coord4D> getCoords(List<IFluxConnector> connectors) {
        List<Coord4D> list = new ArrayList<>();
        for (IFluxConnector c : connectors) {
            list.add(c.getCoords());
        }
        return list;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (!networkValid) {
            return;
        }
        if (timer == 4) {
            refreshPages(new ArrayList<>(network.getSetting(NetworkSettings.ALL_CONNECTORS)));
        }
        if (timer % 5 == 0 && !current.isEmpty()) {
            PacketHandler.network.sendToServer(
                new PacketConnectionUpdateRequest.ConnectionRequestMessage(network.getNetworkID(), getCoords(current)));
        }
        timer++;
        timer %= 20;
        if (FluxNetworks.proxy.getFeedback(true) == EnumFeedbackInfo.SUCCESS) {
            closePopUp();
            batchConnections.clear();
            setBatchButtons(false);
            refreshPages(new ArrayList<>(network.getSetting(NetworkSettings.ALL_CONNECTORS)));
            // 원작과 다름: 원작은 SUCCESS를 지우지 않아 매 틱 이 블록이 다시 돌았다
            FluxNetworks.proxy.setFeedback(EnumFeedbackInfo.NONE, true);
        }
        if (FluxNetworks.proxy.getFeedback(true) == EnumFeedbackInfo.SUCCESS_2) {
            closePopUp();
            elements.removeAll(batchConnections);
            batchConnections.clear();
            setBatchButtons(false);
            refreshPages(new ArrayList<>(network.getSetting(NetworkSettings.ALL_CONNECTORS)));
            FluxNetworks.proxy.setFeedback(EnumFeedbackInfo.NONE, true);
            if (connector instanceof IFluxConnector) {
                Coord4D self = ((IFluxConnector) connector).getCoords();
                boolean stillConnected = false;
                for (IFluxConnector f : elements) {
                    if (f.getCoords()
                        .equals(self)) {
                        stillConnected = true;
                        break;
                    }
                }
                // 이 장치 자신을 연결 해제했으면 선택 탭으로 간다
                if (!stillConnected) {
                    switchTab(EnumNavigationTabs.TAB_SELECTION, player, connector);
                    return;
                }
            }
            page = Math.max(1, Math.min(page, pages));
        }
    }

    @Override
    protected void sortGrids(SortType sortType) {
        elements.sort(
            Comparator.comparing(IFluxConnector::isChunkLoaded)
                .reversed()
                .thenComparing(
                    f -> f.getConnectionType()
                        .isStorage())
                .thenComparing(
                    f -> f.getConnectionType()
                        .isPlug())
                .thenComparing(
                    f -> f.getConnectionType()
                        .isPoint())
                .thenComparing(p -> -p.getLogicPriority()));
        refreshCurrentPageInternal();
    }
}
