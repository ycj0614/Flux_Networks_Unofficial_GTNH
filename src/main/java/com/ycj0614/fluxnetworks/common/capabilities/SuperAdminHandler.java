package com.ycj0614.fluxnetworks.common.capabilities;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * 원작 CapabilityHandler(AttachCapabilitiesEvent) 대체. MinecraftForge.EVENT_BUS에 등록한다.
 * 원작에 PlayerEvent.Clone 처리가 없으므로 사망 후 복사도 하지 않는다.
 */
public class SuperAdminHandler {

    @SubscribeEvent
    public void onEntityConstructing(EntityEvent.EntityConstructing event) {
        if (event.entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.entity;
            // 같은 이름으로 두 번 등록하면 Forge가 이름 뒤에 숫자를 붙여 따로 등록하므로 먼저 확인한다
            if (SuperAdminData.get(player) == null) {
                player.registerExtendedProperties(SuperAdminData.PROP_NAME, new SuperAdminData());
            }
        }
    }
}
