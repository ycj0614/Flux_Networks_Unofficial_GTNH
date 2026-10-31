package com.ycj0614.fluxnetworks.common.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.api.utils.FluxConfigurationType;
import com.ycj0614.fluxnetworks.client.FluxColorHandler;
import com.ycj0614.fluxnetworks.common.CommonProxy;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.handler.GuiHandler;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 플럭스 구성기. 웅크리고 장치 우클릭 = 설정 복사, 그냥 우클릭 = 붙여넣기.
 * 설정은 아이템 NBT의 {@link FluxUtils#CONFIGS_TAG} 하위 태그에 둔다 (키는 FluxConfigurationType과 EU 설정 키).
 */
public class ItemConfigurator extends Item {

    public static final String CONFIG_COPIED_KEY = "info.fluxnetworks.configurator.copied";
    public static final String CONFIG_PASTED_KEY = "info.fluxnetworks.configurator.pasted";

    public ItemConfigurator() {
        this("fluxconfigurator", "flux_configurator_layer0");
    }

    /**
     * @param name        번역 키용 이름 (item.fluxnetworks.&lt;name&gt;.name)
     * @param textureName 아이콘 이름 (textures/items/&lt;textureName&gt;.png)
     */
    protected ItemConfigurator(String name, String textureName) {
        setUnlocalizedName(FluxNetworks.MODID + "." + name);
        setTextureName(FluxNetworks.MODID + ":" + textureName);
        setCreativeTab(CommonProxy.creativeTabs);
    }

    // ---- 아이콘: 원작 2레이어 모델 (layer0 본체, layer1에 네트워크 색) ----

    @SideOnly(Side.CLIENT)
    private IIcon layer1;

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg) {
        itemIcon = reg.registerIcon(getIconString());
        layer1 = reg.registerIcon(FluxNetworks.MODID + ":flux_configurator_layer1");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int damage, int pass) {
        return pass == 1 ? layer1 : itemIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return pass == 1 ? FluxColorHandler.getConfiguratorColor(stack) & 0xffffff : 0xffffff;
    }

    /**
     * 장치를 우클릭했을 때. 웅크리지 않으면 BlockFluxCore.onBlockActivated가 false를 돌려줘서 여기로 온다.
     * 플럭스 장치가 아니면 false (→ onItemRightClick)
     */
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileFluxCore)) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }
        TileFluxCore fluxCore = (TileFluxCore) tile;
        if (!fluxCore.canAccess(player)) {
            // 원작은 액션바(sendStatusMessage). 1.7.10에는 없으므로 채팅으로 보낸다
            ChatComponentTranslation text = new ChatComponentTranslation(FluxTranslate.ACCESS_DENIED_KEY);
            text.getChatStyle()
                .setBold(true)
                .setColor(EnumChatFormatting.DARK_RED);
            player.addChatMessage(text);
            return true;
        }
        if (player.isSneaking()) {
            stack.setTagInfo(FluxUtils.CONFIGS_TAG, fluxCore.copyConfiguration(new NBTTagCompound()));
            player.addChatMessage(new ChatComponentTranslation(CONFIG_COPIED_KEY));
        } else {
            NBTTagCompound configs = getConfigs(stack);
            if (configs != null && !configs.hasNoTags()) {
                String networkKey = FluxConfigurationType.NETWORK.getNBTName();
                if (configs.hasKey(networkKey) && FluxUtils.hasOtherController(
                    FluxNetworkCache.instance.getNetwork(configs.getInteger(networkKey)),
                    fluxCore)) {
                    // 네트워크는 붙여넣지 않고 나머지 설정만 붙여넣는다 (FluxConfigurationType.pasteNetwork)
                    ChatComponentTranslation text = new ChatComponentTranslation(FluxTranslate.HAS_CONTROLLER.key);
                    text.getChatStyle()
                        .setColor(EnumChatFormatting.RED);
                    player.addChatMessage(text);
                }
                fluxCore.pasteConfiguration(configs);
                player.addChatMessage(new ChatComponentTranslation(CONFIG_PASTED_KEY));
            }
        }
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        // 공중이나 플럭스 장치가 아닌 블록을 우클릭하면 GUI를 연다 (플럭스 장치는 onItemUse가 처리)
        if (!world.isRemote) {
            player.openGui(FluxNetworks.instance, GuiHandler.GUI_ITEM, world, 0, 0, 0);
        }
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        NBTTagCompound tag = getConfigs(stack);
        if (tag != null) {
            tooltip.add(
                FluxTranslate.NETWORK_FULL_NAME.t() + ": "
                    + EnumChatFormatting.WHITE
                    + FluxColorHandler.getOrRequestNetworkName(getNetworkID(tag)));
        }
        super.addInformation(stack, player, tooltip, advanced);
    }

    /** 구성기 설정 태그. 없으면 null */
    @Nullable
    public static NBTTagCompound getConfigs(ItemStack stack) {
        if (stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey(FluxUtils.CONFIGS_TAG, Constants.NBT.TAG_COMPOUND)) {
            return stack.getTagCompound()
                .getCompoundTag(FluxUtils.CONFIGS_TAG);
        }
        return null;
    }

    /** 설정 태그에 저장된 네트워크 ID. 없으면 -1 */
    public static int getNetworkID(@Nullable NBTTagCompound configs) {
        String key = FluxConfigurationType.NETWORK.getNBTName();
        return configs != null && configs.hasKey(key, Constants.NBT.TAG_INT) ? configs.getInteger(key) : -1;
    }

    public static class NetworkConnector implements INetworkConnector {

        public ItemStack stack;
        public int networkID;
        public IFluxNetwork network;

        public NetworkConnector(ItemStack stack, int networkID, IFluxNetwork network) {
            this.stack = stack;
            this.networkID = networkID;
            this.network = network;
        }

        @Override
        public int getNetworkID() {
            return networkID;
        }

        @Override
        public IFluxNetwork getNetwork() {
            return network;
        }

        @Override
        public void open(EntityPlayer player) {

        }

        @Override
        public void close(EntityPlayer player) {

        }
    }

    public static NetworkConnector getNetworkConnector(ItemStack stack, World world) {
        int networkID = getNetworkID(getConfigs(stack));
        IFluxNetwork network = world.isRemote ? FluxNetworkCache.instance.getClientNetwork(networkID)
            : FluxNetworkCache.instance.getNetwork(networkID);
        return new NetworkConnector(stack, networkID, network);
    }
}
