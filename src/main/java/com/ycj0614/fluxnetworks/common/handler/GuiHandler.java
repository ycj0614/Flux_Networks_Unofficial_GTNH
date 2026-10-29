package com.ycj0614.fluxnetworks.common.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

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
        // TODO 배치 L: 클라이언트 GUI 화면 반환 (원작: ID 0 → GuiFluxConnectorHome(player, 타일),
        // ID 1 → 관리자 구성기면 GuiFluxAdminHome(player, getAdminConnector()),
        // 구성기면 GuiFluxConfiguratorHome(player, getNetworkConnector(stack, world)))
        return null;
    }
}
