package com.ycj0614.fluxnetworks.common.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.INetworkConnector;

/**
 * 관리자 구성기. 장치에는 쓰지 않고, GUI에서 SuperAdmin이 모든 네트워크를 열람한다.
 */
public class ItemAdminConfigurator extends ItemConfigurator {

    public ItemAdminConfigurator() {
        super("adminconfigurator", "admin_configurator");
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        return false;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        // TODO 배치 L: 서버에서 player.openGui(FluxNetworks.instance, GuiHandler.GUI_ITEM, world, 0, 0, 0)
        // (원작: 우클릭하면 관리자 GUI(GuiFluxAdminHome)를 연다. GUI 화면이 생길 때까지 열지 않는다)
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
