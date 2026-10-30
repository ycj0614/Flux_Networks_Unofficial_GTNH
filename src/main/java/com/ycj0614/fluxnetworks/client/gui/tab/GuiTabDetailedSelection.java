package com.ycj0614.fluxnetworks.client.gui.tab;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.connection.NetworkStatistics;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.network.PacketNetworkUpdateRequest;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** 관리자 구성기의 자세한 네트워크 선택 탭 (연결 수, 평균 틱 시간 표시) */
@SideOnly(Side.CLIENT)
public class GuiTabDetailedSelection extends GuiTabSelection {

    public GuiTabDetailedSelection(EntityPlayer player, INetworkConnector connector) {
        super(player, connector);
        gridStartX = 15;
        gridStartY = 22;
        gridHeight = 19;
        gridPerPage = 7;
        elementHeight = 18;
        elementWidth = 146;
    }

    @Override
    public void renderElement(IFluxNetwork element, int x, int y) {
        renderNetworkBar(element, x, y, 32);

        boolean selected = connector.getNetworkID() == element.getNetworkID();
        NetworkStatistics stats = element.getSetting(NetworkSettings.NETWORK_STATISTICS);
        GL11.glPushMatrix();
        GL11.glScaled(0.625, 0.625, 0.625);
        fontRendererObj.drawString(
            FluxTranslate.CONNECTIONS
                .t() + ": " + stats.getConnectionCount() + "  Avg: " + stats.averageTickMicro + " µs/t",
            (int) ((x + 4) * 1.6),
            (int) ((y + 11) * 1.6),
            selected ? 0xffffff : 0x404040);
        GL11.glPopMatrix();
    }

    @Override
    public void updateScreen() {
        if (timer2 == 1 && !current.isEmpty()) {
            PacketHandler.network.sendToServer(
                new PacketNetworkUpdateRequest.UpdateRequestMessage(
                    new ArrayList<>(current),
                    NBTType.NETWORK_STATISTICS));
        }
        super.updateScreen();
    }
}
