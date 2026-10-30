package com.ycj0614.fluxnetworks.client.gui.tab;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.api.gui.EnumNavigationTabs;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.client.gui.LineChart;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiTabCore;
import com.ycj0614.fluxnetworks.common.connection.NetworkStatistics;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.network.PacketNetworkUpdateRequest;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** 통계 탭: 장치 수, 입출력, 버퍼, 에너지 변화 그래프 (단위 EU) */
@SideOnly(Side.CLIENT)
public class GuiTabStatistics extends GuiTabCore {

    private LineChart chart;
    private int timer = 0;

    public GuiTabStatistics(EntityPlayer player, INetworkConnector connector) {
        super(player, connector);
    }

    @Override
    public EnumNavigationTabs getNavigationTab() {
        return EnumNavigationTabs.TAB_STATISTICS;
    }

    /** 원작과 다름: 원작은 생성 때 받은 객체를 계속 썼다. 네트워크 객체가 바뀌어도 맞도록 매번 꺼낸다 */
    private NetworkStatistics getStats() {
        return network.getSetting(NetworkSettings.NETWORK_STATISTICS);
    }

    private String line(String name, String value) {
        return EnumChatFormatting.GRAY + name + EnumChatFormatting.GRAY + ": " + EnumChatFormatting.RESET + value;
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        super.drawForegroundLayer(mouseX, mouseY);
        if (networkValid) {
            NetworkStatistics stats = getStats();
            int color = network.getSetting(NetworkSettings.NETWORK_COLOR);
            renderNetwork(network.getSetting(NetworkSettings.NETWORK_NAME), color, 20, 8);

            fontRendererObj
                .drawString(line(FluxTranslate.PLUGS.t(), String.valueOf(stats.fluxPlugCount)), 12, 24, color);
            fontRendererObj
                .drawString(line(FluxTranslate.POINTS.t(), String.valueOf(stats.fluxPointCount)), 12, 36, color);
            fontRendererObj
                .drawString(line(FluxTranslate.STORAGES.t(), String.valueOf(stats.fluxStorageCount)), 82, 24, color);
            fontRendererObj.drawString(
                line(FluxTranslate.CONTROLLERS.t(), String.valueOf(stats.fluxControllerCount)),
                82,
                36,
                color);
            fontRendererObj.drawString(
                line(
                    FluxTranslate.INPUT.t(),
                    FluxUtils.format(stats.energyInput, FluxUtils.TypeNumberFormat.COMPACT, true)),
                12,
                48,
                color);
            fontRendererObj.drawString(
                line(
                    FluxTranslate.OUTPUT.t(),
                    FluxUtils.format(stats.energyOutput, FluxUtils.TypeNumberFormat.COMPACT, true)),
                12,
                60,
                color);
            fontRendererObj.drawString(
                line(
                    FluxTranslate.BUFFER.t(),
                    FluxUtils.format(stats.totalBuffer, FluxUtils.TypeNumberFormat.COMPACT, false)),
                12,
                72,
                color);
            fontRendererObj.drawString(
                line(
                    FluxTranslate.ENERGY.t(),
                    FluxUtils.format(stats.totalEnergy, FluxUtils.TypeNumberFormat.COMPACT, false)),
                12,
                84,
                color);

            GL11.glPushMatrix();
            GL11.glScaled(0.75, 0.75, 0.75);
            drawCenteredString(
                fontRendererObj,
                FluxTranslate.AVERAGE_TICK.t() + ": " + stats.averageTickMicro + " µs/t",
                (int) ((xSize / 2) * (1 / 0.75)),
                (int) ((ySize - 2) * (1 / 0.75)),
                color);
            GL11.glPopMatrix();
        } else {
            renderNavigationPrompt(FluxTranslate.ERROR_NO_SELECTED.t(), FluxTranslate.TAB_SELECTION.t());
        }
    }

    @Override
    protected void drawBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawBackgroundLayer(partialTicks, mouseX, mouseY);
        if (networkValid && chart != null) {
            chart.drawChart(mc);
            chart.updateHeight(partialTicks);
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        configureNavigationButtons(EnumNavigationTabs.TAB_STATISTICS, navigationTabs);

        if (networkValid) {
            chart = new LineChart(width / 2 - 48, height / 2 + 20, 50, NetworkStatistics.CHANGE_COUNT, "s", "EU");
            chart.updateData(getStats().energyChange);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (networkValid) {
            if (timer == 0) {
                PacketHandler.network.sendToServer(
                    new PacketNetworkUpdateRequest.UpdateRequestMessage(
                        network.getNetworkID(),
                        NBTType.NETWORK_STATISTICS));
            }
            if (timer == 1 && chart != null) {
                chart.updateData(getStats().energyChange);
            }
            timer++;
            timer %= 20;
        }
    }
}
