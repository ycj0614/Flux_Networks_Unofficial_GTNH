package com.ycj0614.fluxnetworks.client.gui.basic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.network.AccessLevel;
import com.ycj0614.fluxnetworks.api.network.ConnectionType;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.api.utils.Coord4D;
import com.ycj0614.fluxnetworks.client.gui.button.NormalButton;
import com.ycj0614.fluxnetworks.client.gui.button.SlidedSwitchButton;
import com.ycj0614.fluxnetworks.client.gui.button.TextboxButton;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.item.ItemAdminConfigurator;
import com.ycj0614.fluxnetworks.common.item.ItemConfigurator;
import com.ycj0614.fluxnetworks.common.network.PacketSetConfiguratorNetwork;
import com.ycj0614.fluxnetworks.common.network.PacketTile;
import com.ycj0614.fluxnetworks.common.network.PacketTileHandler;
import com.ycj0614.fluxnetworks.common.network.PacketTileType;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 모든 플럭스 GUI의 바탕. 버튼 목록 관리, 네트워크 색 테두리, 접근 권한, 네트워크 연결 요청을 맡는다.
 */
@SideOnly(Side.CLIENT)
public abstract class GuiFluxCore extends GuiPopUpHost {

    public List<List<? extends GuiButtonCore>> buttonLists = new ArrayList<>();
    protected List<NormalButton> buttons = new ArrayList<>();
    protected List<TextboxButton> textBoxes = new ArrayList<>();
    protected List<SlidedSwitchButton> switches = new ArrayList<>();

    public IFluxNetwork network;
    /** PacketGUIPermission으로 받은 값. ClientProxy.setGuiAccessPermission이 열린 GUI에 넣어 준다 */
    public AccessLevel accessPermission = AccessLevel.NONE;
    protected boolean networkValid;
    private int timer1;

    public GuiFluxCore(EntityPlayer player, INetworkConnector connector) {
        super(player, connector);
        this.network = FluxNetworkCache.instance.getClientNetwork(connector.getNetworkID());
        this.networkValid = !network.isInvalid();
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonLists.clear();
        buttons.clear();
        switches.clear();
        textBoxes.clear();

        buttonLists.add(buttons);
        buttonLists.add(switches);
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        super.drawForegroundLayer(mouseX, mouseY);
        for (TextboxButton box : textBoxes) {
            box.drawTextBox();
        }
        for (List<? extends GuiButtonCore> list : buttonLists) {
            for (GuiButtonCore b : list) {
                b.drawButton(mc, mouseX, mouseY, guiLeft, guiTop);
            }
        }
    }

    @Override
    protected void drawBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawBackgroundLayer(partialTicks, mouseX, mouseY);
        for (List<? extends GuiButtonCore> list : buttonLists) {
            for (GuiButtonCore b : list) {
                b.updateButton(partialTicks, mouseX, mouseY);
            }
        }
    }

    @Override
    public void mouseMainClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseMainClicked(mouseX, mouseY, mouseButton);
        CYCLE: for (List<? extends GuiButtonCore> list : buttonLists) {
            for (GuiButtonCore button : list) {
                if (button.clickable && button.isMouseHovered(mc, mouseX - guiLeft, mouseY - guiTop)) {
                    onButtonClicked(button, mouseX - guiLeft, mouseY - guiTop, mouseButton);
                    break CYCLE;
                }
            }
        }
        for (TextboxButton box : textBoxes) {
            box.mouseClicked(mouseX - guiLeft, mouseY - guiTop, mouseButton);
        }
    }

    /** mouseX·mouseY는 GUI 왼쪽 위 기준 */
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {}

    @Override
    protected void keyTypedMain(char c, int k) {
        for (TextboxButton text : textBoxes) {
            if (text.isFocused()) {
                text.textboxKeyTyped(c, k);
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (timer1 == 0) {
            this.network = FluxNetworkCache.instance.getClientNetwork(connector.getNetworkID());
            this.networkValid = !network.isInvalid();
        }
        timer1++;
        timer1 %= 20;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        FluxNetworks.proxy.setFeedback(EnumFeedbackInfo.NONE, false);
        FluxNetworks.proxy.setFeedback(EnumFeedbackInfo.NONE, true);
    }

    /** 배경과 네트워크 색으로 칠한 테두리 */
    @Override
    protected void drawFluxDefaultBackground() {
        GL11.glPushMatrix();
        mc.getTextureManager()
            .bindTexture(BACKGROUND);
        this.drawTexturedModalRect(width / 2 - 128, height / 2 - 128, 0, 0, 256, 256);
        int color = network.getSetting(NetworkSettings.NETWORK_COLOR);
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        GL11.glColor3f(f, f1, f2);
        mc.getTextureManager()
            .bindTexture(FRAME);
        this.drawTexturedModalRect(width / 2 - 128, height / 2 - 128, 0, 0, 256, 256);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
    }

    /** 장치의 전송량과 버퍼(스토리지면 저장량)를 두 줄로 그린다 */
    protected void renderTransfer(IFluxConnector fluxConnector, int color, int x, int y) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);

        fontRendererObj.drawString(
            FluxUtils.getTransferInfo(fluxConnector.getConnectionType(), fluxConnector.getTransferChange()),
            x,
            y,
            color);
        fontRendererObj.drawString(
            (fluxConnector.getConnectionType()
                .isStorage() ? FluxTranslate.ENERGY.t() : FluxTranslate.BUFFER.t()) + ": "
                + EnumChatFormatting.BLUE
                + FluxUtils.format(fluxConnector.getTransferBuffer(), FluxUtils.TypeNumberFormat.COMMAS, false),
            x,
            y + 10,
            0xffffff);

        // TODO 배치 L: 장치 아이콘 표시 (원작: renderItemStack(fluxConnector.getDisplayStack(), x - 20, y + 1).
        // IFluxConnector에 getDisplayStack()이 아직 없다)

        GL11.glPopMatrix();
    }

    /** 장치 위에 마우스를 올렸을 때 보여 줄 툴팁 */
    protected List<String> getFluxInfo(IFluxConnector flux) {
        List<String> list = new ArrayList<>();
        list.add(EnumChatFormatting.BOLD + flux.getCustomName());
        if (flux.isChunkLoaded()) {
            if (flux.isForcedLoading()) {
                list.add(EnumChatFormatting.AQUA + FluxTranslate.FORCED_LOADING.t());
            }
            list.add(FluxUtils.getTransferInfo(flux.getConnectionType(), flux.getTransferChange()));
            String buffer = FluxUtils.format(flux.getTransferBuffer(), FluxUtils.TypeNumberFormat.COMMAS, false);
            if (flux.getConnectionType() == ConnectionType.STORAGE) {
                list.add(FluxTranslate.ENERGY_STORED.t() + ": " + EnumChatFormatting.BLUE + buffer);
            } else {
                list.add(FluxTranslate.INTERNAL_BUFFER.t() + ": " + EnumChatFormatting.BLUE + buffer);
            }
        } else {
            list.add(EnumChatFormatting.RED + FluxTranslate.CHUNK_UNLOADED.t());
            // TODO 배치 L: 언로드된 장치의 저장량·버퍼 표시 (원작: getDisplayStack()의 FLUX_DATA 하위 태그에서
            // "energy"가 있으면 ENERGY_STORED, 없으면 "buffer"를 INTERNAL_BUFFER로 표시)
        }

        list.add(
            FluxTranslate.TRANSFER_LIMIT.t() + ": "
                + EnumChatFormatting.GREEN
                + (flux.getDisableLimit() ? FluxTranslate.UNLIMITED.t() : flux.getLogicLimit()));
        list.add(
            FluxTranslate.PRIORITY.t() + ": "
                + EnumChatFormatting.GREEN
                + (flux.getSurgeMode() ? FluxTranslate.SURGE.t() : flux.getLogicPriority()));
        list.add(
            EnumChatFormatting.ITALIC + flux.getCoords()
                .getStringInfo());
        return list;
    }

    /** SuperAdmin 상태나 접근 권한이 바뀌었을 때 ClientProxy가 호출한다. 버튼을 다시 만들어야 하는 화면이 재정의한다 */
    public void onSuperAdminChanged() {}

    /** 장치·관리자 구성기·구성기에 맞게 네트워크 연결을 요청한다 */
    public void setConnectedNetwork(int networkID, String password) {
        if (connector instanceof IFluxConnector) {
            Coord4D coords = ((IFluxConnector) connector).getCoords();
            PacketHandler.network.sendToServer(
                new PacketTile.TileMessage(
                    PacketTileType.SET_NETWORK,
                    PacketTileHandler.getSetNetworkPacket(networkID, password),
                    coords.getX(),
                    coords.getY(),
                    coords.getZ(),
                    coords.getDimension()));
        }
        if (connector instanceof ItemAdminConfigurator.AdminConnector) {
            FluxNetworks.proxy.admin_viewing_network_id = networkID;
            FluxNetworks.proxy.admin_viewing_network = FluxNetworkCache.instance.getClientNetwork(networkID);
        }
        if (connector instanceof ItemConfigurator.NetworkConnector) {
            PacketHandler.network
                .sendToServer(new PacketSetConfiguratorNetwork.SetConfiguratorNetworkMessage(networkID, password));
        }
    }
}
