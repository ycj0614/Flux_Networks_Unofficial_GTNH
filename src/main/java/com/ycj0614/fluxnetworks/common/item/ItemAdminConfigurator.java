package com.ycj0614.fluxnetworks.common.item;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.common.handler.GuiHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 관리자 구성기. 장치에는 쓰지 않고, GUI에서 SuperAdmin이 모든 네트워크를 열람한다.
 */
public class ItemAdminConfigurator extends ItemConfigurator {

    public ItemAdminConfigurator() {
        super("adminconfigurator", "admin_configurator");
    }

    /** 원작 adminconfigurator 모델은 1레이어 (admin_configurator) */
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg) {
        itemIcon = reg.registerIcon(getIconString());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int damage, int pass) {
        return itemIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return 0xffffff;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        return false;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        // 공중이나 플럭스 장치가 아닌 블록을 우클릭하면 GUI를 연다 (플럭스 장치는 onItemUse가 처리)
        if (!world.isRemote) {
            player.openGui(FluxNetworks.instance, GuiHandler.GUI_ITEM, world, 0, 0, 0);
        }
        return stack;
    }

    private static final AdminConnector connector = new AdminConnector();

    public static INetworkConnector getAdminConnector() {
        return connector;
    }

    /** 클라이언트의 관리자 열람 네트워크(CommonProxy.admin_viewing_network)를 가리킨다 */
    public static class AdminConnector implements INetworkConnector {

        @Override
        public int getNetworkID() {
            return FluxNetworks.proxy.admin_viewing_network_id;
        }

        @Override
        public IFluxNetwork getNetwork() {
            return FluxNetworks.proxy.admin_viewing_network;
        }

        @Override
        public void open(EntityPlayer player) {}

        @Override
        public void close(EntityPlayer player) {}
    }
}
