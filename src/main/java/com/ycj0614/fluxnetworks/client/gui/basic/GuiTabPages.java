package com.ycj0614.fluxnetworks.client.gui.basic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;

import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.api.translate.Translation;
import com.ycj0614.fluxnetworks.client.gui.button.PageLabelButton;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** 여러 페이지로 나뉘는 목록 탭 (네트워크 선택, 연결 목록 등) */
@SideOnly(Side.CLIENT)
public abstract class GuiTabPages<T> extends GuiTabCore {

    public List<T> elements = new ArrayList<>();
    protected List<T> current = new ArrayList<>();
    protected SortType sortType = SortType.ID;
    protected PageLabelButton labelButton;
    private boolean init;

    public int page = 1, currentPages = 1, pages = 1, gridPerPage = 1, gridStartX = 0, gridStartY = 0, gridHeight = 0,
        elementHeight = 0, elementWidth = 0;

    public GuiTabPages(EntityPlayer player, INetworkConnector connector) {
        super(player, connector);
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        super.drawForegroundLayer(mouseX, mouseY);
        if (pages > 1) {
            labelButton.drawButton(mc, mouseX, mouseY, guiLeft, guiTop);
        }
        int i = 0;
        for (T s : current) {
            int y = (gridStartY + gridHeight * i);
            renderElement(s, gridStartX, y);
            i++;
        }
        i = 0;
        for (T s : current) {
            int y = (gridStartY + gridHeight * i);
            if (mouseX >= gridStartX + guiLeft && mouseY >= y + guiTop
                && mouseX < (gridStartX + elementWidth) + guiLeft
                && mouseY < y + elementHeight + guiTop) {
                renderElementTooltip(s, mouseX - guiLeft, mouseY - guiTop);
            }
            i++;
        }
    }

    /** mouseX·mouseY는 GUI 왼쪽 위 기준 */
    public T getHoveredElement(int mouseX, int mouseY) {
        if (current.isEmpty()) return null;
        for (int i = 0; i < currentPages; i++) {
            int y = (gridStartY + gridHeight * i);
            if (mouseX >= gridStartX && mouseY >= y
                && mouseX < (gridStartX + elementWidth)
                && mouseY < y + elementHeight) {
                if (current.get(i) != null) {
                    return current.get(i);
                }
            }
        }
        return null;
    }

    protected abstract void onElementClicked(T element, int mouseButton);

    @Override
    public void mouseMainClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseMainClicked(mouseX, mouseY, mouseButton);
        T e = getHoveredElement(mouseX - guiLeft, mouseY - guiTop);
        if (e != null) {
            onElementClicked(e, mouseButton);
        }
        if (pages > 1 && labelButton.isMouseHovered(mc, mouseX - guiLeft, mouseY - guiTop)) {
            if (page != labelButton.hoveredPage) {
                page = Math.max(labelButton.hoveredPage, 1);
                refreshCurrentPage();
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        labelButton = new PageLabelButton(
            14,
            157,
            page,
            pages,
            network.getSetting(NetworkSettings.NETWORK_COLOR),
            guiLeft,
            guiTop);
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int scroll) {
        super.mouseScroll(mouseX, mouseY, scroll);

        if (scroll == -1 && page < pages) {
            page++;
            refreshCurrentPage();
        } else if (scroll == 1 && page > 1) {
            page--;
            refreshCurrentPage();
        }
    }

    public abstract void renderElement(T element, int x, int y);

    public abstract void renderElementTooltip(T element, int mouseX, int mouseY);

    protected void refreshPages(List<T> elements) {
        this.elements = elements;
        pages = (int) Math.ceil(elements.size() / (double) gridPerPage);
        sortGrids(sortType);
        if (!init) {
            refreshCurrentPage();
            init = true;
        } else {
            refreshCurrentPageInternal();
        }
    }

    protected void refreshCurrentPage() {
        refreshCurrentPageInternal();
        labelButton.refreshPages(page, pages);
    }

    protected void refreshCurrentPageInternal() {
        if (elements.isEmpty()) return;

        current.clear();
        int a = (page - 1) * gridPerPage;
        int b = Math.min(elements.size(), page * gridPerPage);
        currentPages = b - a;

        for (int i = a; i < b; i++) {
            current.add(elements.get(i));
        }
    }

    protected void sortGrids(SortType sortType) {}

    public enum SortType {

        ID(FluxTranslate.SORTING_ID),
        NAME(FluxTranslate.SORTING_NAME);

        private final Translation name;

        SortType(Translation name) {
            this.name = name;
        }

        public String getTranslatedName() {
            return name.t();
        }
    }
}
