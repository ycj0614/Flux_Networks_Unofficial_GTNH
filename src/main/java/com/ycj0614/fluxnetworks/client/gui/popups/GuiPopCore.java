package com.ycj0614.fluxnetworks.client.gui.popups;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiDraw;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiPopUpHost;
import com.ycj0614.fluxnetworks.client.gui.button.NormalButton;
import com.ycj0614.fluxnetworks.client.gui.button.SlidedSwitchButton;
import com.ycj0614.fluxnetworks.client.gui.button.TextboxButton;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 호스트 GUI 위에 겹쳐 그리는 팝업. 그리기·입력은 호스트(GuiPopUpHost)가 넘겨준다.
 */
@SideOnly(Side.CLIENT)
public class GuiPopCore<T extends GuiPopUpHost> extends GuiDraw {

    protected List<NormalButton> popButtons = new ArrayList<>();
    protected List<TextboxButton> popBoxes = new ArrayList<>();
    protected List<SlidedSwitchButton> popSwitches = new ArrayList<>();

    public T host;
    public EntityPlayer player;
    public INetworkConnector connector;

    public GuiPopCore(T host, EntityPlayer player, INetworkConnector connector) {
        super(host.inventorySlots);
        this.host = host;
        this.player = player;
        this.connector = connector;
    }

    @Override
    public void initGui() {
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        popBoxes.clear();
        popButtons.clear();
        popSwitches.clear();
    }

    public void openPopUp() {
        setWorldAndResolution(Minecraft.getMinecraft(), host.width, host.height);
    }

    public void closePopUp() {
        popButtons.clear();
        popBoxes.clear();
        popSwitches.clear();
        FluxNetworks.proxy.setFeedback(EnumFeedbackInfo.NONE, true);
    }

    @Override
    public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawGradientRect(-guiLeft, -guiTop, this.width, this.height, 0xa0101010, 0xb0101010);
        for (TextboxButton box : popBoxes) {
            box.drawTextBox();
        }
        for (NormalButton button : popButtons) {
            button.drawButton(mc, mouseX, mouseY, guiLeft, guiTop);
        }
    }

    @Override
    public void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        for (SlidedSwitchButton button : popSwitches) {
            button.updateButton(partialTicks * 4, mouseX, mouseY);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (TextboxButton box : popBoxes) {
            box.mouseClicked(mouseX - guiLeft, mouseY - guiTop, mouseButton);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1 || isInventoryKey(keyCode)) {
            if (popBoxes.stream()
                .noneMatch(TextboxButton::isFocused)) {
                host.closePopUp();
            }
        }
        for (TextboxButton text : popBoxes) {
            if (text.isFocused()) {
                text.textboxKeyTyped(typedChar, keyCode);
            }
        }
    }
}
