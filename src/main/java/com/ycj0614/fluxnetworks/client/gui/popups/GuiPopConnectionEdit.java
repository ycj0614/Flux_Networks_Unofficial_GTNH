package com.ycj0614.fluxnetworks.client.gui.popups;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.api.utils.Coord4D;
import com.ycj0614.fluxnetworks.client.gui.button.NormalButton;
import com.ycj0614.fluxnetworks.client.gui.button.SimpleToggleButton;
import com.ycj0614.fluxnetworks.client.gui.button.SlidedSwitchButton;
import com.ycj0614.fluxnetworks.client.gui.button.TextboxButton;
import com.ycj0614.fluxnetworks.client.gui.tab.GuiTabConnections;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.network.PacketBatchEditing;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 연결 탭의 단일·일괄 편집 팝업.
 * TODO 3단계: 티어·암페어 일괄 편집 (FluxLiteConnector와 PacketBatchEditing에 EU 설정을 추가해야 한다. 지금은 장치 홈 화면에서만 바꾼다)
 */
@SideOnly(Side.CLIENT)
public class GuiPopConnectionEdit extends GuiPopCore<GuiTabConnections> {

    public NormalButton apply;
    public TextboxButton fluxName, priority, limit;
    public SlidedSwitchButton surge, unlimited, chunkLoad;
    public SimpleToggleButton editName, editPriority, editLimit, editSurge, editUnlimited, editChunkLoad;

    public boolean batchMode;

    private final List<SimpleToggleButton> toggleButtons = new ArrayList<>();

    public GuiPopConnectionEdit(GuiTabConnections host, boolean batchMode, EntityPlayer player,
        INetworkConnector connector) {
        super(host, player, connector);
        this.batchMode = batchMode;
    }

    @Override
    public void initGui() {
        super.initGui();
        toggleButtons.clear();

        popButtons.add(new NormalButton(FluxTranslate.CANCEL.t(), 40, 140, 36, 12, 11));
        apply = new NormalButton(FluxTranslate.APPLY.t(), 100, 140, 36, 12, 12).setUnclickable();
        popButtons.add(apply);

        int color = host.network.getSetting(NetworkSettings.NETWORK_COLOR) | 0xff000000;
        int x = batchMode ? 20 : 18;
        int w = batchMode ? 136 : 140;
        fluxName = TextboxButton.create(this, FluxTranslate.NAME.t() + ": ", 0, fontRendererObj, x, 30, w, 12)
            .setOutlineColor(color);
        fluxName.setMaxStringLength(TileFluxCore.MAX_CUSTOM_NAME_LENGTH);

        priority = TextboxButton.create(this, FluxTranslate.PRIORITY.t() + ": ", 1, fontRendererObj, x, 47, w, 12)
            .setOutlineColor(color)
            .setDigitsOnly();
        priority.setMaxStringLength(5);

        limit = TextboxButton.create(this, FluxTranslate.TRANSFER_LIMIT.t() + ": ", 2, fontRendererObj, x, 64, w, 12)
            .setOutlineColor(color)
            .setDigitsOnly();
        limit.setMaxStringLength(9);

        if (!batchMode) {
            IFluxConnector single = host.singleConnection;
            fluxName.setText(single.getCustomName());
            priority.setText(String.valueOf(single.getRawPriority()));
            limit.setText(String.valueOf(single.getRawLimit()));

            surge = new SlidedSwitchButton(140, 82, 1, guiLeft, guiTop, single.getSurgeMode());
            unlimited = new SlidedSwitchButton(140, 94, 2, guiLeft, guiTop, single.getDisableLimit());

            popSwitches.add(surge);
            popSwitches.add(unlimited);

            if (!single.getConnectionType()
                .isStorage()) {
                chunkLoad = new SlidedSwitchButton(140, 106, 3, guiLeft, guiTop, single.isForcedLoading());
                popSwitches.add(chunkLoad);
            }
        } else {
            priority.setText(String.valueOf(0));
            limit.setText(String.valueOf(0));

            editName = new SimpleToggleButton(10, 33, guiLeft, guiTop, 0);
            editPriority = new SimpleToggleButton(10, 50, guiLeft, guiTop, 1);
            editLimit = new SimpleToggleButton(10, 67, guiLeft, guiTop, 2);

            editSurge = new SimpleToggleButton(10, 82, guiLeft, guiTop, 3);
            editUnlimited = new SimpleToggleButton(10, 94, guiLeft, guiTop, 4);
            editChunkLoad = new SimpleToggleButton(10, 106, guiLeft, guiTop, 5);

            toggleButtons.add(editName);
            toggleButtons.add(editPriority);
            toggleButtons.add(editLimit);

            toggleButtons.add(editSurge);
            toggleButtons.add(editUnlimited);
            toggleButtons.add(editChunkLoad);

            surge = new SlidedSwitchButton(140, 82, 1, guiLeft, guiTop, false);
            unlimited = new SlidedSwitchButton(140, 94, 2, guiLeft, guiTop, false);
            chunkLoad = new SlidedSwitchButton(140, 106, 3, guiLeft, guiTop, false);
            popSwitches.add(surge);
            popSwitches.add(unlimited);
            popSwitches.add(chunkLoad);
        }

        popBoxes.add(fluxName);
        popBoxes.add(priority);
        popBoxes.add(limit);
    }

    @Override
    public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawRectWithBackground(8, 13, 150, 160, 0xccffffff, 0xb0000000);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        for (SlidedSwitchButton button : popSwitches) {
            button.drawButton(mc, mouseX, mouseY, guiLeft, guiTop);
        }
        for (SimpleToggleButton button : toggleButtons) {
            button.drawButton(mc, mouseX, mouseY, guiLeft, guiTop);
        }
        if (!batchMode) {
            drawCenteredString(fontRendererObj, FluxTranslate.SINGLE_EDIT.t(), 88, 17, 0xffffff);
            drawCenteredString(
                fontRendererObj,
                host.singleConnection.getCoords()
                    .getStringInfo(),
                88,
                122,
                0xffffff);
        } else {
            drawCenteredString(fontRendererObj, FluxTranslate.BATCH_EDIT.t(), 88, 17, 0xffffff);
            drawCenteredString(
                fontRendererObj,
                FluxTranslate.EDITING.t() + " " + host.batchConnections.size() + " " + FluxTranslate.CONNECTIONS.t(),
                88,
                122,
                0xffffff);
        }
        int color = host.network.getSetting(NetworkSettings.NETWORK_COLOR);
        fontRendererObj.drawString(FluxTranslate.SURGE_MODE.t(), 20, 82, color);
        fontRendererObj.drawString(FluxTranslate.DISABLE_LIMIT.t(), 20, 94, color);
        if (batchMode || !host.singleConnection.getConnectionType()
            .isStorage()) {
            fontRendererObj.drawString(FluxTranslate.CHUNK_LOADING.t(), 20, 106, color);
        }
        drawCenteredString(
            fontRendererObj,
            EnumChatFormatting.RED + FluxNetworks.proxy.getFeedback(false)
                .getInfo(),
            88,
            155,
            0xffffff);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }
        for (NormalButton button : popButtons) {
            if (button.clickable && button.isMouseHovered(mc, mouseX - guiLeft, mouseY - guiTop)) {
                if (button.id == 11) {
                    host.closePopUp();
                    return;
                }
                if (button.id == 12) {
                    sendEdit();
                    return;
                }
            }
        }
        for (SlidedSwitchButton s : popSwitches) {
            if (s.isMouseHovered(mc, mouseX - guiLeft, mouseY - guiTop)) {
                s.switchButton();
                if (!batchMode) {
                    apply.clickable = true;
                }
            }
        }
        for (SimpleToggleButton s : toggleButtons) {
            if (s.isMouseHovered(mc, mouseX - guiLeft, mouseY - guiTop)) {
                s.on = !s.on;
                boolean any = false;
                for (SimpleToggleButton b : toggleButtons) {
                    any |= b.on;
                }
                apply.clickable = any;
            }
        }
    }

    private void sendEdit() {
        List<Coord4D> list;
        boolean[] b = new boolean[PacketBatchEditing.EDITION_COUNT];
        if (batchMode) {
            list = GuiTabConnections.getCoords(host.batchConnections);
            b[0] = editName.on;
            b[1] = editPriority.on;
            b[2] = editLimit.on;
            b[3] = editSurge.on;
            b[4] = editUnlimited.on;
            b[5] = editChunkLoad.on;
        } else {
            list = new ArrayList<>();
            list.add(host.singleConnection.getCoords());
            for (int i = 0; i < 6; i++) {
                b[i] = true;
            }
        }
        // 원작 FluxUtils.getBatchEditingTag
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(TileFluxCore.NBT_CUSTOM_NAME, fluxName.getText());
        tag.setInteger(TileFluxCore.NBT_PRIORITY, priority.getIntegerFromText(false));
        tag.setLong(TileFluxCore.NBT_LIMIT, limit.getLongFromText(true));
        tag.setBoolean(TileFluxCore.NBT_SURGE_MODE, surge.slideControl);
        tag.setBoolean(TileFluxCore.NBT_DISABLE_LIMIT, unlimited.slideControl);
        tag.setBoolean(PacketBatchEditing.CHUNK_LOAD, chunkLoad != null && chunkLoad.slideControl);
        PacketHandler.network
            .sendToServer(new PacketBatchEditing.BatchEditingMessage(host.network.getNetworkID(), list, tag, b));
    }

    @Override
    public void keyTyped(char c, int k) {
        if (k == 1 || isInventoryKey(k)) {
            boolean focused = false;
            for (TextboxButton box : popBoxes) {
                focused |= box.isFocused();
            }
            if (!focused) {
                host.closePopUp();
                return;
            }
            for (TextboxButton box : popBoxes) {
                box.setFocused(false);
            }
            return;
        }
        for (TextboxButton text : popBoxes) {
            if (text.isFocused()) {
                text.textboxKeyTyped(c, k);
                if (!batchMode) {
                    apply.clickable = true;
                }
            }
        }
    }
}
