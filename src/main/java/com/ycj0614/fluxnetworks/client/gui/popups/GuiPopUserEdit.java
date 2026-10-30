package com.ycj0614.fluxnetworks.client.gui.popups;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.network.AccessLevel;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.client.gui.button.NormalButton;
import com.ycj0614.fluxnetworks.client.gui.tab.GuiTabMembers;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.network.PacketGeneral;
import com.ycj0614.fluxnetworks.common.network.PacketGeneralHandler;
import com.ycj0614.fluxnetworks.common.network.PacketGeneralType;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 멤버 권한 변경 팝업. 버튼 id가 CHANGE_PERMISSION의 type이다
 * (0 사용자로 추가, 1 관리자로, 2 사용자로, 3 멤버 해제, 4 소유권 이전 — 소유권 이전은 왼쪽 Shift를 두 번 눌러야 켜진다).
 * 권한 확인은 서버가 다시 한다
 */
@SideOnly(Side.CLIENT)
public class GuiPopUserEdit extends GuiPopCore<GuiTabMembers> {

    public NormalButton transferOwnership;
    public int transferOwnershipCount;

    public GuiPopUserEdit(GuiTabMembers host, EntityPlayer player, INetworkConnector connector) {
        super(host, player, connector);
    }

    private void addButton(String text, int index, int id, int color, boolean clickable) {
        int length = Math.max(64, fontRendererObj.getStringWidth(text) + 4);
        NormalButton button = new NormalButton(text, 88 - length / 2, 76 + 16 * index, length, 12, id);
        if (color != 0) {
            button.setTextColor(color);
        }
        if (!clickable) {
            button.setUnclickable();
        }
        if (id == 4) {
            transferOwnership = button;
        }
        popButtons.add(button);
    }

    @Override
    public void initGui() {
        super.initGui();
        transferOwnership = null;
        boolean editPermission = host.accessPermission.canEdit();
        boolean ownerPermission = host.accessPermission.canDelete();
        AccessLevel selected = host.selectedPlayer.getAccessPermission();
        if (selected != AccessLevel.OWNER && editPermission) {
            int i = 0;
            if (selected == AccessLevel.NONE || selected == AccessLevel.SUPER_ADMIN) {
                addButton(FluxTranslate.SET_USER.t(), i++, 0, 0, true);
                if (selected == AccessLevel.SUPER_ADMIN && ownerPermission) {
                    addButton(FluxTranslate.TRANSFER_OWNERSHIP.t(), i++, 4, 0xffaa00aa, false);
                }
            } else {
                if (ownerPermission) {
                    if (selected == AccessLevel.USER) {
                        addButton(FluxTranslate.SET_ADMIN.t(), i++, 1, 0, true);
                    } else if (selected == AccessLevel.ADMIN) {
                        addButton(FluxTranslate.SET_USER.t(), i++, 2, 0, true);
                    }
                }
                if (!selected.canEdit() || ownerPermission) {
                    addButton(FluxTranslate.CANCEL_MEMBERSHIP.t(), i++, 3, 0xffff5555, true);
                }
                if (ownerPermission) {
                    addButton(FluxTranslate.TRANSFER_OWNERSHIP.t(), i++, 4, 0xffaa00aa, false);
                }
            }
        }
    }

    @Override
    public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawRectWithBackground(20, 34, 100, 138, 0xccffffff, 0x80000000);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        drawCenteredString(
            fontRendererObj,
            EnumChatFormatting.RED + FluxNetworks.proxy.getFeedback(false)
                .getInfo(),
            88,
            162,
            0xffffff);
        drawCenteredString(
            fontRendererObj,
            EnumChatFormatting.AQUA + host.selectedPlayer.getCachedName(),
            88,
            38,
            0xffffff);
        drawCenteredString(
            fontRendererObj,
            host.selectedPlayer.getAccessPermission()
                .getName(),
            88,
            48,
            0xffffff);
        String text = host.selectedPlayer.getPlayerUUID()
            .toString();
        GL11.glPushMatrix();
        GL11.glScaled(0.625, 0.625, 0.625);
        drawCenteredString(
            fontRendererObj,
            "UUID: " + text.substring(0, 16),
            (int) (88 * 1.6),
            (int) (60 * 1.6),
            0xffffff);
        drawCenteredString(fontRendererObj, text.substring(16), (int) (88 * 1.6), (int) (66 * 1.6), 0xffffff);
        GL11.glPopMatrix();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }
        for (NormalButton button : popButtons) {
            if (button.clickable && button.isMouseHovered(mc, mouseX - guiLeft, mouseY - guiTop)) {
                PacketHandler.network.sendToServer(
                    new PacketGeneral.GeneralMessage(
                        PacketGeneralType.CHANGE_PERMISSION,
                        PacketGeneralHandler.getChangePermissionPacket(
                            host.network.getNetworkID(),
                            host.selectedPlayer.getPlayerUUID(),
                            button.id)));
                return;
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
        if (transferOwnership != null) {
            // 42 = 왼쪽 Shift
            if (keyCode == 42) {
                transferOwnershipCount++;
                if (transferOwnershipCount > 1) {
                    transferOwnership.clickable = true;
                }
            } else {
                transferOwnershipCount = 0;
                transferOwnership.clickable = false;
            }
        }
    }
}
