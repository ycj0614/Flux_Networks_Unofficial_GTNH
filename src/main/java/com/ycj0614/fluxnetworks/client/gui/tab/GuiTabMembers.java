package com.ycj0614.fluxnetworks.client.gui.tab;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.gui.EnumNavigationTabs;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.NetworkMember;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiDraw;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiTabPages;
import com.ycj0614.fluxnetworks.client.gui.popups.GuiPopUserEdit;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.network.PacketNetworkUpdateRequest;
import com.ycj0614.fluxnetworks.common.network.PacketPermissionRequest;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** 멤버 탭: 멤버 목록. 클릭하면 권한 변경 팝업 */
@SideOnly(Side.CLIENT)
public class GuiTabMembers extends GuiTabPages<NetworkMember> {

    public NetworkMember selectedPlayer;

    private int timer;

    public GuiTabMembers(EntityPlayer player, INetworkConnector connector) {
        super(player, connector);
        gridStartX = 15;
        gridStartY = 22;
        gridHeight = 13;
        gridPerPage = 10;
        elementHeight = 12;
        elementWidth = 146;
        PacketHandler.network.sendToServer(
            new PacketNetworkUpdateRequest.UpdateRequestMessage(network.getNetworkID(), NBTType.NETWORK_PLAYERS));
    }

    @Override
    public EnumNavigationTabs getNavigationTab() {
        return EnumNavigationTabs.TAB_MEMBER;
    }

    private UUID getPlayerUUID() {
        return player.getGameProfile()
            .getId();
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        if (networkValid) {
            String str2 = accessPermission.getName();
            fontRendererObj.drawString(str2, 158 - fontRendererObj.getStringWidth(str2), 10, 0xffffff);
            fontRendererObj.drawString(
                FluxTranslate.SORT_BY.t() + ": " + EnumChatFormatting.AQUA + FluxTranslate.SORTING_SMART.t(),
                19,
                10,
                0xffffff);
            super.drawForegroundLayer(mouseX, mouseY);
        } else {
            super.drawForegroundLayer(mouseX, mouseY);
            renderNavigationPrompt(FluxTranslate.ERROR_NO_SELECTED.t(), FluxTranslate.TAB_SELECTION.t());
        }
    }

    @Override
    public void initGui() {
        // 이름으로 멤버를 추가하는 입력칸과 "+" 버튼(PacketGeneralType.ADD_MEMBER)은 원작에서도 주석 처리된 기능이라 옮기지 않았다
        super.initGui();
        configureNavigationButtons(EnumNavigationTabs.TAB_MEMBER, navigationTabs);
    }

    @Override
    protected void onElementClicked(NetworkMember element, int mouseButton) {
        if (mouseButton == 0) {
            selectedPlayer = element;
            openPopUp(new GuiPopUserEdit(this, player, connector));
        }
    }

    @Override
    public void renderElement(NetworkMember element, int x, int y) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        int color = element.getAccessPermission().color;

        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;

        GL11.glColor4f(f, f1, f2, 0.8f);

        mc.getTextureManager()
            .bindTexture(GuiDraw.GUI_BAR);
        drawTexturedModalRect(x, y, 0, 16, elementWidth, elementHeight);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        if (element.getPlayerUUID()
            .equals(getPlayerUUID())) {
            drawRect(x - 4, y + 1, x - 2, y + elementHeight - 1, 0xccffffff);
            drawRect(x + elementWidth + 2, y + 1, x + elementWidth + 4, y + elementHeight - 1, 0xccffffff);
        }

        fontRendererObj.drawString(EnumChatFormatting.WHITE + element.getCachedName(), x + 4, y + 2, 0xffffff);

        String p = element.getAccessPermission()
            .getName();
        fontRendererObj.drawString(p, x + 142 - fontRendererObj.getStringWidth(p), y + 2, 0xffffff);

        GL11.glPopMatrix();
    }

    @Override
    public void renderElementTooltip(NetworkMember element, int mouseX, int mouseY) {
        if (hasActivePopup()) {
            return;
        }
        List<String> strings = new ArrayList<>();
        strings.add(FluxTranslate.USERNAME.t() + ": " + EnumChatFormatting.AQUA + element.getCachedName());
        String permission = element.getAccessPermission()
            .getName()
            + (element.getPlayerUUID()
                .equals(getPlayerUUID()) ? " (" + FluxTranslate.YOU.t() + ")" : "");
        strings.add(FluxTranslate.ACCESS.t() + ": " + EnumChatFormatting.RESET + permission);
        drawHoverTooltip(strings, mouseX + 4, mouseY - 8);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (timer == 0) {
            PacketHandler.network.sendToServer(
                new PacketPermissionRequest.PermissionRequestMessage(network.getNetworkID(), getPlayerUUID()));
        }
        if (timer % 2 == 0) {
            refreshPages(new ArrayList<>(network.getSetting(NetworkSettings.NETWORK_PLAYERS)));
            if (FluxNetworks.proxy.getFeedback(true) == EnumFeedbackInfo.SUCCESS) {
                if (hasActivePopup() && selectedPlayer != null) {
                    NetworkMember found = null;
                    for (NetworkMember m : elements) {
                        if (m.getPlayerUUID()
                            .equals(selectedPlayer.getPlayerUUID())) {
                            found = m;
                            break;
                        }
                    }
                    if (found != null) {
                        selectedPlayer = found;
                        openPopUp(new GuiPopUserEdit(this, player, connector));
                    } else {
                        closePopUp();
                    }
                }
                FluxNetworks.proxy.setFeedback(EnumFeedbackInfo.NONE, true);
            }
        }
        timer++;
        timer %= 40;
    }

    @Override
    protected void sortGrids(SortType sortType) {
        elements.sort(
            Comparator.comparing(NetworkMember::getAccessPermission)
                .thenComparing(NetworkMember::getCachedName));
        refreshCurrentPageInternal();
    }
}
