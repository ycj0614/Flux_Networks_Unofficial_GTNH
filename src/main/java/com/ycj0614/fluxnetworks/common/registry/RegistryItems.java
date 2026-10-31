package com.ycj0614.fluxnetworks.common.registry;

import net.minecraft.item.Item;

import com.ycj0614.fluxnetworks.common.item.ItemAdminConfigurator;
import com.ycj0614.fluxnetworks.common.item.ItemConfigurator;
import com.ycj0614.fluxnetworks.common.item.ItemCore;
import com.ycj0614.fluxnetworks.common.item.ItemFlux;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * 원작은 정적 초기화 + 레지스트리 이벤트. 1.7.10에서는 preInit에서 register()를 부른다.
 * 등록 이름("flux", "fluxcore", "fluxconfigurator", "adminconfigurator")은 월드 저장 데이터에 기록되므로 이후 바꾸지 않는다.
 */
public class RegistryItems {

    public static Item FLUX, FLUX_CORE;
    public static Item FLUX_CONFIGURATOR, ADMIN_CONFIGURATOR;

    public static void register() {
        FLUX = new ItemFlux();
        FLUX_CORE = new ItemCore("fluxcore");
        FLUX_CONFIGURATOR = new ItemConfigurator();
        ADMIN_CONFIGURATOR = new ItemAdminConfigurator();

        GameRegistry.registerItem(FLUX, "flux");
        GameRegistry.registerItem(FLUX_CORE, "fluxcore");
        GameRegistry.registerItem(FLUX_CONFIGURATOR, "fluxconfigurator");
        GameRegistry.registerItem(ADMIN_CONFIGURATOR, "adminconfigurator");
    }
}
