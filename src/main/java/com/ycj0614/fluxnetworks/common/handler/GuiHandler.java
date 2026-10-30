package com.ycj0614.fluxnetworks.common.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.common.core.ContainerCore;
import com.ycj0614.fluxnetworks.common.item.ItemAdminConfigurator;
import com.ycj0614.fluxnetworks.common.item.ItemConfigurator;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;

import cpw.mods.fml.common.network.IGuiHandler;

/**
 * GUI ID 0 = 장치(x, y, z), 1 = 손에 든 구성기·관리자 구성기 (좌표는 쓰지 않음).
 * CommonProxy.init에서 NetworkRegistry.INSTANCE.registerGuiHandler로 등록한다.
 */
public class GuiHandler implements IGuiHandler {

    public static final int GUI_TILE = 0;
    public static final int GUI_ITEM = 1;

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_TILE) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileFluxCore) {
                return new ContainerCore(player, (TileFluxCore) tile);
            }
        }
        if (ID == GUI_ITEM) {
            ItemStack stack = player.getHeldItem();
            if (stack != null && stack.getItem() instanceof ItemAdminConfigurator) {
                return new ContainerCore(player, ItemAdminConfigurator.getAdminConnector());
            }
            if (stack != null && stack.getItem() instanceof ItemConfigurator) {
                return new ContainerCore(player, ItemConfigurator.getNetworkConnector(stack, world));
            }
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        // 원작과 다름: 공용 클래스가 GUI 클래스를 직접 참조하지 않도록 ClientProxy에서 화면을 만든다
        return FluxNetworks.proxy.getClientGuiElement(ID, player, world, x, y, z);
    }
}
