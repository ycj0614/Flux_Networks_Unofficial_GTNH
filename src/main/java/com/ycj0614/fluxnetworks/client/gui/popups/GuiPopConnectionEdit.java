package com.ycj0614.fluxnetworks.client.gui.popups;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.network.ConnectionType;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.api.utils.Coord4D;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiTabCore;
import com.ycj0614.fluxnetworks.client.gui.button.NormalButton;
import com.ycj0614.fluxnetworks.client.gui.button.SimpleToggleButton;
import com.ycj0614.fluxnetworks.client.gui.button.SlidedSwitchButton;
import com.ycj0614.fluxnetworks.client.gui.button.TextboxButton;
import com.ycj0614.fluxnetworks.client.gui.tab.GuiTabConnections;
import com.ycj0614.fluxnetworks.common.connection.FluxLiteConnector;
import com.ycj0614.fluxnetworks.common.core.GTEnergy;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.network.PacketBatchEditing;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPoint;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 연결 탭의 단일·일괄 편집 팝업.
 * <p>
 * 원작과 다름: GT EU 설정 줄(티어, 출력 암페어)이 있다. 티어는 플러그의 최대 입력 티어, 포인트의 출력 티어이고
 * 암페어는 포인트만 쓴다. 단일 편집은 해당하는 줄만 보이고, 일괄 편집은 두 줄 모두 보이며 해당하는 장치에만 적용된다.
 * 줄이 늘어난 만큼 원작보다 세로 간격을 좁혔다.
 */
@SideOnly(Side.CLIENT)
public class GuiPopConnectionEdit extends GuiPopCore<GuiTabConnections> {

    /** 텍스트 상자·티어 줄의 y (이름, 우선순위, 한도, 티어, 암페어) */
    private static final int Y_NAME = 28, Y_PRIORITY = 41, Y_LIMIT = 54, Y_TIER = 67, Y_AMPERAGE = 80;
    /** 스위치 줄의 y (서지, 한도 해제, 청크 로딩) */
    private static final int Y_SURGE = 94, Y_UNLIMITED = 105, Y_CHUNK = 116;
    private static final int ROW_HEIGHT = 12;

    public NormalButton apply;
    public TextboxButton fluxName, priority, limit, amperage;
    public SlidedSwitchButton surge, unlimited, chunkLoad;
    public SimpleToggleButton editName, editPriority, editLimit, editSurge, editUnlimited, editChunkLoad, editTier,
        editAmperage;

    public boolean batchMode;

    /** 티어 줄의 현재 값 */
    private int tier;
    private int rowX, rowWidth;

    private final List<SimpleToggleButton> toggleButtons = new ArrayList<>();

    public GuiPopConnectionEdit(GuiTabConnections host, boolean batchMode, EntityPlayer player,
        INetworkConnector connector) {
        super(host, player, connector);
        this.batchMode = batchMode;
    }

    private ConnectionType singleType() {
        return host.singleConnection.getConnectionType();
    }

    /** 티어 줄을 보여줄지 (단일 편집은 플러그·포인트만) */
    private boolean hasTier() {
        return batchMode || singleType().isPlug() || singleType().isPoint();
    }

    /** 암페어 줄을 보여줄지 (단일 편집은 포인트만) */
    private boolean hasAmperage() {
        return batchMode || singleType().isPoint();
    }

    private String tierLabel() {
        if (batchMode) {
            return FluxTranslate.TIER.t();
        }
        return singleType().isPoint() ? FluxTranslate.OUTPUT_TIER.t() : FluxTranslate.MAX_INPUT_TIER.t();
    }

    @Override
    public void initGui() {
        super.initGui();
        toggleButtons.clear();
        amperage = null;

        popButtons.add(new NormalButton(FluxTranslate.CANCEL.t(), 40, 140, 36, 12, 11));
        apply = new NormalButton(FluxTranslate.APPLY.t(), 100, 140, 36, 12, 12).setUnclickable();
        popButtons.add(apply);

        int color = host.network.getSetting(NetworkSettings.NETWORK_COLOR) | 0xff000000;
        rowX = batchMode ? 20 : 18;
        rowWidth = batchMode ? 136 : 140;
        fluxName = TextboxButton
            .create(this, FluxTranslate.NAME.t() + ": ", 0, fontRendererObj, rowX, Y_NAME, rowWidth, ROW_HEIGHT)
            .setOutlineColor(color);
        fluxName.setMaxStringLength(TileFluxCore.MAX_CUSTOM_NAME_LENGTH);

        priority = TextboxButton
            .create(this, FluxTranslate.PRIORITY.t() + ": ", 1, fontRendererObj, rowX, Y_PRIORITY, rowWidth, ROW_HEIGHT)
            .setOutlineColor(color)
            .setDigitsOnly();
        priority.setMaxStringLength(5);

        limit = TextboxButton
            .create(
                this,
                FluxTranslate.TRANSFER_LIMIT.t() + ": ",
                2,
                fontRendererObj,
                rowX,
                Y_LIMIT,
                rowWidth,
                ROW_HEIGHT)
            .setOutlineColor(color)
            .setDigitsOnly();
        limit.setMaxStringLength(9);

        if (hasAmperage()) {
            amperage = TextboxButton
                .create(
                    this,
                    FluxTranslate.OUTPUT_AMPERAGE.t() + ": ",
                    3,
                    fontRendererObj,
                    rowX,
                    Y_AMPERAGE,
                    rowWidth,
                    ROW_HEIGHT)
                .setOutlineColor(color)
                .setDigitsOnly();
            amperage.setMaxStringLength(9);
        }

        if (!batchMode) {
            IFluxConnector single = host.singleConnection;
            fluxName.setText(single.getCustomName());
            priority.setText(String.valueOf(single.getRawPriority()));
            limit.setText(String.valueOf(single.getRawLimit()));
            tier = GTEnergy.clampTier(FluxLiteConnector.getEUTier(single));
            if (amperage != null) {
                amperage.setText(String.valueOf(Math.max(1L, FluxLiteConnector.getEUAmperage(single))));
            }

            surge = new SlidedSwitchButton(140, Y_SURGE, 1, guiLeft, guiTop, single.getSurgeMode());
            unlimited = new SlidedSwitchButton(140, Y_UNLIMITED, 2, guiLeft, guiTop, single.getDisableLimit());

            popSwitches.add(surge);
            popSwitches.add(unlimited);

            if (!single.getConnectionType()
                .isStorage()) {
                chunkLoad = new SlidedSwitchButton(140, Y_CHUNK, 3, guiLeft, guiTop, single.isForcedLoading());
                popSwitches.add(chunkLoad);
            }
        } else {
            priority.setText(String.valueOf(0));
            limit.setText(String.valueOf(0));
            tier = GTEnergy.clampTier(FluxConfig.defaultPointTier);
            amperage.setText(String.valueOf(Math.max(1, FluxConfig.defaultPointAmperage)));

            editName = new SimpleToggleButton(10, Y_NAME + 3, guiLeft, guiTop, 0);
            editPriority = new SimpleToggleButton(10, Y_PRIORITY + 3, guiLeft, guiTop, 1);
            editLimit = new SimpleToggleButton(10, Y_LIMIT + 3, guiLeft, guiTop, 2);
            editTier = new SimpleToggleButton(10, Y_TIER + 3, guiLeft, guiTop, 7);
            editAmperage = new SimpleToggleButton(10, Y_AMPERAGE + 3, guiLeft, guiTop, 8);

            editSurge = new SimpleToggleButton(10, Y_SURGE, guiLeft, guiTop, 3);
            editUnlimited = new SimpleToggleButton(10, Y_UNLIMITED, guiLeft, guiTop, 4);
            editChunkLoad = new SimpleToggleButton(10, Y_CHUNK, guiLeft, guiTop, 5);

            toggleButtons.add(editName);
            toggleButtons.add(editPriority);
            toggleButtons.add(editLimit);
            toggleButtons.add(editTier);
            toggleButtons.add(editAmperage);

            toggleButtons.add(editSurge);
            toggleButtons.add(editUnlimited);
            toggleButtons.add(editChunkLoad);

            surge = new SlidedSwitchButton(140, Y_SURGE, 1, guiLeft, guiTop, false);
            unlimited = new SlidedSwitchButton(140, Y_UNLIMITED, 2, guiLeft, guiTop, false);
            chunkLoad = new SlidedSwitchButton(140, Y_CHUNK, 3, guiLeft, guiTop, false);
            popSwitches.add(surge);
            popSwitches.add(unlimited);
            popSwitches.add(chunkLoad);
        }

        popBoxes.add(fluxName);
        popBoxes.add(priority);
        popBoxes.add(limit);
        if (amperage != null) {
            popBoxes.add(amperage);
        }
    }

    private boolean isTierHovered(int x, int y) {
        return hasTier() && x >= rowX && x < rowX + rowWidth && y >= Y_TIER && y < Y_TIER + ROW_HEIGHT;
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
                128,
                0xffffff);
        } else {
            drawCenteredString(fontRendererObj, FluxTranslate.BATCH_EDIT.t(), 88, 17, 0xffffff);
            drawCenteredString(
                fontRendererObj,
                FluxTranslate.EDITING.t() + " " + host.batchConnections.size() + " " + FluxTranslate.CONNECTIONS.t(),
                88,
                128,
                0xffffff);
        }
        int color = host.network.getSetting(NetworkSettings.NETWORK_COLOR);
        boolean tierHovered = isTierHovered(mouseX - guiLeft, mouseY - guiTop);
        if (hasTier()) {
            drawTierSelector(rowX, Y_TIER, rowWidth, ROW_HEIGHT, tierLabel(), tier, tierHovered, color | 0xff000000);
        }
        fontRendererObj.drawString(FluxTranslate.SURGE_MODE.t(), 20, Y_SURGE, color);
        fontRendererObj.drawString(FluxTranslate.DISABLE_LIMIT.t(), 20, Y_UNLIMITED, color);
        if (batchMode || !singleType().isStorage()) {
            fontRendererObj.drawString(FluxTranslate.CHUNK_LOADING.t(), 20, Y_CHUNK, color);
        }
        drawCenteredString(
            fontRendererObj,
            EnumChatFormatting.RED + FluxNetworks.proxy.getFeedback(false)
                .getInfo(),
            88,
            155,
            0xffffff);
        if (tierHovered) {
            drawHoverTooltip(FluxTranslate.TIER_CLICK.t(), mouseX - guiLeft + 4, mouseY - guiTop - 14);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if ((mouseButton == 0 || mouseButton == 1) && isTierHovered(mouseX - guiLeft, mouseY - guiTop)) {
            tier = cycleTier(tier, mouseButton == 0);
            if (!batchMode) {
                apply.clickable = true;
            }
            GuiTabCore.playButtonSound();
            return;
        }
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
            b[7] = editTier.on;
            b[8] = editAmperage.on;
        } else {
            list = new ArrayList<>();
            list.add(host.singleConnection.getCoords());
            for (int i = 0; i < 6; i++) {
                b[i] = true;
            }
            b[7] = hasTier();
            b[8] = hasAmperage();
        }
        // 원작 FluxUtils.getBatchEditingTag
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(TileFluxCore.NBT_CUSTOM_NAME, fluxName.getText());
        tag.setInteger(TileFluxCore.NBT_PRIORITY, priority.getIntegerFromText(false));
        tag.setLong(TileFluxCore.NBT_LIMIT, limit.getLongFromText(true));
        tag.setBoolean(TileFluxCore.NBT_SURGE_MODE, surge.slideControl);
        tag.setBoolean(TileFluxCore.NBT_DISABLE_LIMIT, unlimited.slideControl);
        tag.setBoolean(PacketBatchEditing.CHUNK_LOAD, chunkLoad != null && chunkLoad.slideControl);
        tag.setInteger(PacketBatchEditing.TIER, tier);
        if (amperage != null) {
            tag.setLong(TileFluxPoint.NBT_OUTPUT_AMPERAGE, amperage.getLongFromText(true));
        }
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
