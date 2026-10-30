package com.ycj0614.fluxnetworks.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumNavigationTabs;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiButtonCore;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiTabCore;
import com.ycj0614.fluxnetworks.client.gui.button.SlidedSwitchButton;
import com.ycj0614.fluxnetworks.client.gui.button.TextboxButton;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.core.GTEnergy;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.network.PacketByteBuf;
import com.ycj0614.fluxnetworks.common.network.PacketNetworkUpdateRequest;
import com.ycj0614.fluxnetworks.common.network.PacketTile;
import com.ycj0614.fluxnetworks.common.network.PacketTileHandler;
import com.ycj0614.fluxnetworks.common.network.PacketTileType;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPlug;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPoint;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 장치 홈 화면. 원작과 다름: GT EU 설정 줄이 있다.
 * 플러그는 최대 입력 티어, 포인트는 출력 티어와 출력 암페어 (포인트는 한도·한도 해제를 쓰지 않으므로 그 자리에 암페어를 둔다)
 */
@SideOnly(Side.CLIENT)
public class GuiFluxConnectorHome extends GuiTabCore {

    /** 티어 선택 줄 위치 (GUI 왼쪽 위 기준) */
    private static final int TIER_X = 16, TIER_Y = 79, TIER_WIDTH = 144, TIER_HEIGHT = 12;

    public TextboxButton fluxName, priority, limit, amperage;

    public SlidedSwitchButton surge, disableLimit, chunkLoad;

    private final TileFluxCore tileEntity;
    private int timer;

    public GuiFluxConnectorHome(EntityPlayer player, TileFluxCore tileEntity) {
        super(player, tileEntity);
        this.tileEntity = tileEntity;
    }

    @Override
    public EnumNavigationTabs getNavigationTab() {
        return EnumNavigationTabs.TAB_HOME;
    }

    private boolean isPoint() {
        return tileEntity instanceof TileFluxPoint;
    }

    private boolean hasTier() {
        return tileEntity instanceof TileFluxPlug || tileEntity instanceof TileFluxPoint;
    }

    private int getTier() {
        if (tileEntity instanceof TileFluxPlug) {
            return ((TileFluxPlug) tileEntity).getMaxInputTier();
        }
        if (tileEntity instanceof TileFluxPoint) {
            return ((TileFluxPoint) tileEntity).getOutputTier();
        }
        return 0;
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        super.drawForegroundLayer(mouseX, mouseY);
        int color = network.getSetting(NetworkSettings.NETWORK_COLOR);
        renderNetwork(network.getSetting(NetworkSettings.NETWORK_NAME), color, 20, 8);
        if (hasTier()) {
            renderTierSelector(mouseX, mouseY, color | 0xff000000);
        }
        renderTransfer(tileEntity, 0xffffff, 30, 96);
        drawCenteredString(
            fontRendererObj,
            EnumChatFormatting.RED + FluxNetworks.proxy.getFeedback(false)
                .getInfo(),
            89,
            150,
            0xffffff);

        fontRendererObj.drawString(FluxTranslate.SURGE_MODE.t(), 20, 120, color);
        if (!isPoint()) {
            fontRendererObj.drawString(FluxTranslate.DISABLE_LIMIT.t(), 20, 132, color);
        }
        if (!tileEntity.getConnectionType()
            .isStorage()) {
            fontRendererObj.drawString(FluxTranslate.CHUNK_LOADING.t(), 20, 144, color);
        }

        if (hasTier() && isTierHovered(mouseX - guiLeft, mouseY - guiTop)) {
            drawHoverTooltip(FluxTranslate.TIER_CLICK.t(), mouseX - guiLeft + 4, mouseY - guiTop - 14);
        }
    }

    /** 텍스트 상자와 같은 모양의 테두리 안에 "이름: LV (32 EU/t)"를 그린다 */
    private void renderTierSelector(int mouseX, int mouseY, int outlineColor) {
        boolean hovered = isTierHovered(mouseX - guiLeft, mouseY - guiTop);
        int frame = hovered ? 0xffffffff : outlineColor;
        drawRect(TIER_X - 1, TIER_Y - 1, TIER_X + TIER_WIDTH + 1, TIER_Y, frame);
        drawRect(TIER_X - 1, TIER_Y + TIER_HEIGHT, TIER_X + TIER_WIDTH + 1, TIER_Y + TIER_HEIGHT + 1, frame);
        drawRect(TIER_X - 1, TIER_Y, TIER_X, TIER_Y + TIER_HEIGHT, frame);
        drawRect(TIER_X + TIER_WIDTH, TIER_Y, TIER_X + TIER_WIDTH + 1, TIER_Y + TIER_HEIGHT, frame);
        drawRect(TIER_X, TIER_Y, TIER_X + TIER_WIDTH, TIER_Y + TIER_HEIGHT, 0x20000000);

        String label = (isPoint() ? FluxTranslate.OUTPUT_TIER.t() : FluxTranslate.MAX_INPUT_TIER.t()) + ": ";
        fontRendererObj.drawString(label, TIER_X, TIER_Y + 2, outlineColor);
        fontRendererObj.drawString(
            EnumChatFormatting.AQUA + formatTier(getTier()),
            TIER_X + fontRendererObj.getStringWidth(label),
            TIER_Y + 2,
            0xffffff);
    }

    /** "LV (32 EU/t)" 형식 */
    public static String formatTier(int tier) {
        return GTEnergy.tierName(tier) + " ("
            + FluxUtils.format(GTEnergy.voltage(tier), FluxUtils.TypeNumberFormat.COMMAS, true)
            + ")";
    }

    private boolean isTierHovered(int x, int y) {
        return x >= TIER_X && x < TIER_X + TIER_WIDTH && y >= TIER_Y && y < TIER_Y + TIER_HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();
        configureNavigationButtons(EnumNavigationTabs.TAB_HOME, navigationTabs);

        int color = network.getSetting(NetworkSettings.NETWORK_COLOR) | 0xff000000;
        fluxName = TextboxButton.create(this, FluxTranslate.NAME.t() + ": ", 0, fontRendererObj, 16, 28, 144, 12)
            .setOutlineColor(color);
        fluxName.setMaxStringLength(TileFluxCore.MAX_CUSTOM_NAME_LENGTH);
        fluxName.setText(tileEntity.getCustomName());

        priority = TextboxButton.create(this, FluxTranslate.PRIORITY.t() + ": ", 1, fontRendererObj, 16, 45, 144, 12)
            .setOutlineColor(color)
            .setDigitsOnly();
        priority.setMaxStringLength(5);
        priority.setText(String.valueOf(tileEntity.priority));

        textBoxes.add(fluxName);
        textBoxes.add(priority);

        if (isPoint()) {
            amperage = TextboxButton
                .create(this, FluxTranslate.OUTPUT_AMPERAGE.t() + ": ", 2, fontRendererObj, 16, 62, 144, 12)
                .setOutlineColor(color)
                .setDigitsOnly();
            amperage.setMaxStringLength(9);
            amperage.setText(String.valueOf(((TileFluxPoint) tileEntity).getOutputAmperage()));
            textBoxes.add(amperage);
        } else {
            limit = TextboxButton
                .create(this, FluxTranslate.TRANSFER_LIMIT.t() + ": ", 2, fontRendererObj, 16, 62, 144, 12)
                .setOutlineColor(color)
                .setDigitsOnly();
            limit.setMaxStringLength(9);
            limit.setText(String.valueOf(tileEntity.limit));
            textBoxes.add(limit);
        }

        surge = new SlidedSwitchButton(140, 120, 1, guiLeft, guiTop, tileEntity.surgeMode);
        switches.add(surge);
        if (!isPoint()) {
            disableLimit = new SlidedSwitchButton(140, 132, 2, guiLeft, guiTop, tileEntity.disableLimit);
            switches.add(disableLimit);
        }

        if (!tileEntity.getConnectionType()
            .isStorage()) {
            chunkLoad = new SlidedSwitchButton(140, 144, 3, guiLeft, guiTop, tileEntity.chunkLoading);
            switches.add(chunkLoad);
        }
    }

    @Override
    public void onTextBoxChanged(TextboxButton text) {
        if (text == fluxName) {
            tileEntity.customName = fluxName.getText();
            sendTilePacket(1);
        } else if (text == priority) {
            tileEntity.priority = priority.getIntegerFromText(false);
            sendTilePacket(2);
        } else if (text == limit) {
            tileEntity.limit = Math.min(limit.getLongFromText(true), tileEntity.getMaxTransferLimit());
            limit.setText(String.valueOf(tileEntity.limit));
            sendTilePacket(3);
        } else if (text == amperage) {
            TileFluxPoint point = (TileFluxPoint) tileEntity;
            point.setOutputAmperage(amperage.getLongFromText(true));
            amperage.setText(String.valueOf(point.getOutputAmperage()));
            sendTilePacket(7);
        }
    }

    @Override
    public void mouseMainClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseMainClicked(mouseX, mouseY, mouseButton);
        if (hasTier() && isTierHovered(mouseX - guiLeft, mouseY - guiTop) && (mouseButton == 0 || mouseButton == 1)) {
            // 좌클릭은 다음 티어, 우클릭은 이전 티어. 끝에서는 반대쪽으로 넘어간다
            int count = GTEnergy.MAX_TIER - GTEnergy.MIN_TIER + 1;
            int tier = getTier() - GTEnergy.MIN_TIER + (mouseButton == 0 ? 1 : count - 1);
            tier = tier % count + GTEnergy.MIN_TIER;
            if (tileEntity instanceof TileFluxPlug) {
                ((TileFluxPlug) tileEntity).setMaxInputTier(tier);
            } else {
                ((TileFluxPoint) tileEntity).setOutputTier(tier);
            }
            sendTilePacket(6);
            playButtonSound();
        }
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && button instanceof SlidedSwitchButton) {
            SlidedSwitchButton switchButton = (SlidedSwitchButton) button;
            switchButton.switchButton();
            switch (switchButton.id) {
                case 1:
                    tileEntity.surgeMode = switchButton.slideControl;
                    sendTilePacket(4);
                    break;
                case 2:
                    tileEntity.disableLimit = switchButton.slideControl;
                    sendTilePacket(5);
                    break;
                case 3:
                    PacketHandler.network.sendToServer(
                        new PacketTile.TileMessage(
                            PacketTileType.CHUNK_LOADING,
                            PacketTileHandler.getChunkLoadPacket(switchButton.slideControl),
                            tileEntity.xCoord,
                            tileEntity.yCoord,
                            tileEntity.zCoord,
                            tileEntity.getWorldObj().provider.dimensionId));
                    break;
            }
        }
    }

    /** ITileByteBuf ID로 클라이언트 타일의 값을 서버에 보낸다 */
    private void sendTilePacket(int id) {
        PacketHandler.network.sendToServer(
            new PacketByteBuf.ByteBufMessage(tileEntity, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, id));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (timer == 0) {
            PacketHandler.network.sendToServer(
                new PacketNetworkUpdateRequest.UpdateRequestMessage(network.getNetworkID(), NBTType.NETWORK_GENERAL));
        }
        if (timer % 4 == 0) {
            if (chunkLoad != null) {
                chunkLoad.slideControl = tileEntity.chunkLoading;
            }
        }
        timer++;
        timer %= 100;
    }
}
