package com.ycj0614.fluxnetworks.client.gui.basic;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.client.gui.popups.GuiPopCore;
import com.ycj0614.fluxnetworks.common.core.ContainerCore;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class GuiPopUpHost extends GuiDraw {

    public INetworkConnector connector;
    public EntityPlayer player;

    public GuiPopUpHost(EntityPlayer player, INetworkConnector connector) {
        super(new ContainerCore(player, connector));
        this.player = player;
        this.connector = connector;
    }

    protected void drawForegroundLayer(int mouseX, int mouseY) {}

    protected void drawBackgroundLayer(float partialTicks, int mouseX, int mouseY) {}

    protected abstract void drawFluxDefaultBackground();

    protected void mouseMainClicked(int mouseX, int mouseY, int mouseButton) {}

    protected void mouseScroll(int mouseX, int mouseY, int scroll) {}

    protected void keyTypedMain(char typedChar, int keyCode) {}

    @Override
    public final void handleMouseInput() {
        super.handleMouseInput();

        if (hasActivePopup()) return;

        int i = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int j = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        int k = Integer.signum(Mouse.getEventDWheel());
        if (k != 0) {
            mouseScroll(i, j, k);
        }
    }

    //// 팝업 \\\\

    public GuiPopCore<?> currentPopUp;

    public final boolean hasActivePopup() {
        return currentPopUp != null;
    }

    public final void openPopUp(GuiPopCore<?> popUp) {
        if (popUp == null) {
            return;
        }
        if (currentPopUp != null) {
            currentPopUp.closePopUp();
            currentPopUp = null;
        }
        currentPopUp = popUp;
        currentPopUp.openPopUp();
        onPopUpOpen(popUp);
        currentPopUp.initGui();
    }

    public void onPopUpOpen(GuiPopCore<?> popUp) {}

    public final void closePopUp() {
        if (currentPopUp != null) {
            onPopUpClose(currentPopUp);
            currentPopUp.closePopUp();
            currentPopUp = null;
        }
    }

    /** 팝업에서 입력한 값을 받을 때 쓴다 */
    public void onPopUpClose(GuiPopCore<?> popUp) {}

    @Override
    public void initGui() {
        super.initGui();
        if (currentPopUp != null) {
            currentPopUp.initGui();
        }
    }

    @Override
    protected final void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        drawForegroundLayer(mouseX, mouseY);
        if (currentPopUp != null) {
            currentPopUp.drawGuiContainerForegroundLayer(mouseX, mouseY);
        }
    }

    @Override
    protected final void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawDefaultBackground();
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        drawFluxDefaultBackground();
        drawBackgroundLayer(partialTicks, mouseX, mouseY);

        GL11.glPopMatrix();

        // 원작 그대로 한 번 더 호출한다 (그래서 스위치 애니메이션이 두 배 빠르다)
        drawBackgroundLayer(partialTicks, mouseX, mouseY);

        if (currentPopUp != null) {
            currentPopUp.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        }
    }

    @Override
    protected final void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (currentPopUp != null) {
            currentPopUp.mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            mouseMainClicked(mouseX, mouseY, mouseButton);
        }
    }

    /** GuiContainer.keyTyped(ESC·인벤토리 키로 닫기)는 부르지 않는다. 닫기는 GuiTabCore·GuiPopCore가 처리한다 */
    @Override
    protected final void keyTyped(char typedChar, int keyCode) {
        if (currentPopUp != null) {
            currentPopUp.keyTyped(typedChar, keyCode);
        } else {
            keyTypedMain(typedChar, keyCode);
        }
    }

    @Override
    public void setWorldAndResolution(Minecraft mc, int width, int height) {
        super.setWorldAndResolution(mc, width, height);
        if (currentPopUp != null) {
            currentPopUp.setWorldAndResolution(mc, width, height);
        }
    }
}
