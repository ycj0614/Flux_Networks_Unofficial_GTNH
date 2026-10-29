package com.ycj0614.fluxnetworks.common.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import com.ycj0614.fluxnetworks.api.network.INetworkConnector;
import com.ycj0614.fluxnetworks.common.network.PacketTile;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;

/**
 * 플럭스 GUI의 서버 쪽 컨테이너. 슬롯은 없고, 열고 닫을 때 connector.open/close만 부른다.
 */
public class ContainerCore extends Container {

    public INetworkConnector connector;

    public ContainerCore(EntityPlayer player, INetworkConnector connector) {
        this.connector = connector;
        this.connector.open(player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (connector instanceof TileFluxCore) {
            // 보안: 원작은 항상 true. 장치가 없어졌거나 8블록보다 멀어지면 서버가 GUI를 닫는다
            TileFluxCore tile = (TileFluxCore) connector;
            return !tile.isInvalid() && tile.getWorldObj() == player.worldObj
                && player.getDistanceSq(tile.xCoord + 0.5D, tile.yCoord + 0.5D, tile.zCoord + 0.5D)
                    <= PacketTile.MAX_DISTANCE_SQ;
        }
        return true;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        connector.close(player);
    }
}
