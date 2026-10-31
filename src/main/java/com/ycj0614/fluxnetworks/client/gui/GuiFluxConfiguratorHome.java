package com.ycj0614.fluxnetworks.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumNavigationTabs;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.api.utils.FluxConfigurationType;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiButtonCore;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiTabCore;
import com.ycj0614.fluxnetworks.client.gui.button.NormalButton;
import com.ycj0614.fluxnetworks.client.gui.button.SlidedSwitchButton;
import com.ycj0614.fluxnetworks.client.gui.button.TextboxButton;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.item.ItemConfigurator;
import com.ycj0614.fluxnetworks.common.network.PacketConfiguratorSettings;
import com.ycj0614.fluxnetworks.common.network.PacketNetworkUpdateRequest;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPlug;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPoint;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** 구성기 홈 화면: 구성기에 저장할 이름·우선순위·한도 설정 */
@SideOnly(Side.CLIENT)
public class GuiFluxConfiguratorHome extends GuiTabCore {

    public NormalButton apply;
    public TextboxButton fluxName, priority, limit;

    public SlidedSwitchButton surge, disableLimit, chunkLoad;

    public ItemStack stack;
    public NBTTagCompound configTag;

    public String stackCustomName;
    public int stackPriority;
    public long stackLimit;

    public boolean stackSurgeMode;
    public boolean stackDisableLimit;
    public boolean stackChunkLoading;

    private int timer;

    public GuiFluxConfiguratorHome(EntityPlayer player, ItemConfigurator.NetworkConnector connector) {
        super(player, connector);
        this.stack = connector.stack;
        updateSettingsFromTag();
    }

    @Override
    public EnumNavigationTabs getNavigationTab() {
        return EnumNavigationTabs.TAB_HOME;
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        super.drawForegroundLayer(mouseX, mouseY);
        int color = network.getSetting(NetworkSettings.NETWORK_COLOR);
        renderNetwork(network.getSetting(NetworkSettings.NETWORK_NAME), color, 20, 8);
        drawCenteredString(
            fontRendererObj,
            EnumChatFormatting.RED + FluxNetworks.proxy.getFeedback(false)
                .getInfo(),
            89,
            150,
            0xffffff);

        fontRendererObj.drawString(FluxTranslate.SURGE_MODE.t(), 20, 90, color);
        fontRendererObj.drawString(FluxTranslate.DISABLE_LIMIT.t(), 20, 102, color);
        fontRendererObj.drawString(FluxTranslate.CHUNK_LOADING.t(), 20, 114, color);
    }

    @Override
    public void initGui() {
        super.initGui();
        configureNavigationButtons(EnumNavigationTabs.TAB_HOME, navigationTabs);

        int color = network.getSetting(NetworkSettings.NETWORK_COLOR) | 0xff000000;
        fluxName = TextboxButton.create(this, FluxTranslate.NAME.t() + ": ", 0, fontRendererObj, 16, 28, 144, 12)
            .setOutlineColor(color);
        fluxName.setMaxStringLength(TileFluxCore.MAX_CUSTOM_NAME_LENGTH);
        fluxName.setText(stackCustomName);

        priority = TextboxButton.create(this, FluxTranslate.PRIORITY.t() + ": ", 1, fontRendererObj, 16, 45, 144, 12)
            .setOutlineColor(color)
            .setDigitsOnly();
        priority.setMaxStringLength(5);
        priority.setText(String.valueOf(stackPriority));

        limit = TextboxButton.create(this, FluxTranslate.TRANSFER_LIMIT.t() + ": ", 2, fontRendererObj, 16, 62, 144, 12)
            .setOutlineColor(color)
            .setDigitsOnly();
        limit.setMaxStringLength(9);
        limit.setText(String.valueOf(stackLimit));

        switches.add(surge = new SlidedSwitchButton(140, 90, 1, guiLeft, guiTop, stackSurgeMode));
        switches.add(disableLimit = new SlidedSwitchButton(140, 102, 2, guiLeft, guiTop, stackDisableLimit));
        // 원작과 다름: 원작은 이 스위치가 주석 처리되어 있었다. 원작 위치 y=144는 적용 버튼과 겹쳐서 y=114에 둔다
        switches.add(chunkLoad = new SlidedSwitchButton(140, 114, 3, guiLeft, guiTop, stackChunkLoading));
        buttons.add(apply = new NormalButton(FluxTranslate.APPLY.t(), xSize / 2 - 36 / 2, 138, 36, 12, 3));
        apply.clickable = configTag == null;

        textBoxes.add(fluxName);
        textBoxes.add(priority);
        textBoxes.add(limit);
    }

    @Override
    public void onTextBoxChanged(TextboxButton text) {
        super.onTextBoxChanged(text);
        if (text == fluxName) {
            stackCustomName = fluxName.getText();
            onSettingsChanged();
        } else if (text == priority) {
            stackPriority = priority.getIntegerFromText(false);
            onSettingsChanged();
        } else if (text == limit) {
            stackLimit = limit.getLongFromText(true);
            limit.setText(String.valueOf(stackLimit));
            onSettingsChanged();
        }
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            if (button instanceof SlidedSwitchButton) {
                SlidedSwitchButton switchButton = (SlidedSwitchButton) button;
                switchButton.switchButton();
                switch (switchButton.id) {
                    case 1:
                        stackSurgeMode = switchButton.slideControl;
                        onSettingsChanged();
                        break;
                    case 2:
                        stackDisableLimit = switchButton.slideControl;
                        onSettingsChanged();
                        break;
                    case 3:
                        stackChunkLoading = switchButton.slideControl;
                        onSettingsChanged();
                        break;
                }
            }
            if (button == apply) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger(FluxConfigurationType.NETWORK.getNBTName(), network.getNetworkID());
                tag.setInteger(FluxConfigurationType.PRIORITY.getNBTName(), stackPriority);
                tag.setLong(FluxConfigurationType.TRANSFER.getNBTName(), stackLimit);
                tag.setBoolean(FluxConfigurationType.PRIORITY_SETTING.getNBTName(), stackSurgeMode);
                tag.setBoolean(FluxConfigurationType.TRANSFER_SETTING.getNBTName(), stackDisableLimit);
                tag.setBoolean(FluxConfigurationType.CHUNK_LOADING.getNBTName(), stackChunkLoading);
                // 원작과 다름: 장치에서 복사해 둔 EU 설정은 이 화면에서 고치지 않으므로 그대로 넘긴다
                // (서버의 sanitize는 보낸 키만 남긴다)
                copyEUSetting(tag, TileFluxPlug.NBT_MAX_INPUT_TIER);
                copyEUSetting(tag, TileFluxPoint.NBT_OUTPUT_TIER);
                copyEUSetting(tag, TileFluxPoint.NBT_OUTPUT_AMPERAGE);

                PacketHandler.network
                    .sendToServer(new PacketConfiguratorSettings.ConfiguratorSettingsMessage(stackCustomName, tag));
                // 서버가 같은 값을 다시 보내 주지만, 버튼 상태를 바로 맞추려고 클라이언트에도 쓴다
                stack.setTagInfo(FluxUtils.CONFIGS_TAG, tag);
                if (!stackCustomName.isEmpty()) {
                    stack.setStackDisplayName(stackCustomName);
                }
                updateSettingsFromTag();
                apply.setUnclickable();
            }
        }
    }

    private void copyEUSetting(NBTTagCompound tag, String key) {
        if (configTag != null && configTag.hasKey(key)) {
            tag.setTag(
                key,
                configTag.getTag(key)
                    .copy());
        }
    }

    public void updateSettingsFromTag() {
        configTag = ItemConfigurator.getConfigs(stack);
        stackCustomName = stack.getDisplayName();
        if (configTag != null) {
            stackPriority = configTag.getInteger(FluxConfigurationType.PRIORITY.getNBTName());
            stackSurgeMode = configTag.getBoolean(FluxConfigurationType.PRIORITY_SETTING.getNBTName());
            stackLimit = configTag.getLong(FluxConfigurationType.TRANSFER.getNBTName());
            stackDisableLimit = configTag.getBoolean(FluxConfigurationType.TRANSFER_SETTING.getNBTName());
            stackChunkLoading = configTag.getBoolean(FluxConfigurationType.CHUNK_LOADING.getNBTName());
        } else {
            stackPriority = 0;
            stackSurgeMode = false;
            stackLimit = FluxConfig.defaultLimit;
            stackDisableLimit = false;
            stackChunkLoading = false;
        }
    }

    public void onSettingsChanged() {
        if (configTag == null) {
            apply.clickable = true;
        } else {
            apply.clickable = network.getNetworkID() != ItemConfigurator.getNetworkID(configTag)
                || stackPriority != configTag.getInteger(FluxConfigurationType.PRIORITY.getNBTName())
                || stackLimit != configTag.getLong(FluxConfigurationType.TRANSFER.getNBTName())
                || stackSurgeMode != configTag.getBoolean(FluxConfigurationType.PRIORITY_SETTING.getNBTName())
                || stackDisableLimit != configTag.getBoolean(FluxConfigurationType.TRANSFER_SETTING.getNBTName())
                || stackChunkLoading != configTag.getBoolean(FluxConfigurationType.CHUNK_LOADING.getNBTName())
                || !stackCustomName.equals(stack.getDisplayName());
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (timer == 0) {
            PacketHandler.network.sendToServer(
                new PacketNetworkUpdateRequest.UpdateRequestMessage(network.getNetworkID(), NBTType.NETWORK_GENERAL));
        }
        timer++;
        timer %= 100;
    }
}
