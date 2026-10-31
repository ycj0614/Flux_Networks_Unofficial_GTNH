package com.ycj0614.fluxnetworks.common.integration;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import baubles.api.BaublesApi;

/**
 * 바우블(Baubles-Expanded) 연동. baubles 클래스는 이 클래스에서만 import한다.
 * 모드가 없으면 이 클래스를 불러오는 순간 NoClassDefFoundError가 나므로, 반드시 CommonProxy.baublesLoaded를 확인한 뒤 부른다.
 */
public final class BaublesIntegration {

    private BaublesIntegration() {}

    /**
     * @return 플레이어의 바우블 인벤토리. API가 리플렉션에 실패하면 null
     */
    @Nullable
    public static IInventory getBaubles(EntityPlayer player) {
        return BaublesApi.getBaubles(player);
    }
}
