package com.ycj0614.fluxnetworks.common.registry;

import net.minecraft.block.Block;

import com.ycj0614.fluxnetworks.common.block.BlockFluxController;
import com.ycj0614.fluxnetworks.common.block.BlockFluxPlug;
import com.ycj0614.fluxnetworks.common.block.BlockFluxPoint;
import com.ycj0614.fluxnetworks.common.block.BlockFluxStorage;
import com.ycj0614.fluxnetworks.common.item.ItemFluxConnector;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * 원작은 정적 초기화 + 레지스트리 이벤트. 1.7.10에서는 preInit에서 register()를 부른다.
 * 등록 이름("fluxplug", "fluxpoint", "fluxstorage", "herculeanfluxstorage", "gargantuanfluxstorage", "fluxcontroller")은 월드
 * 저장 데이터에 기록되므로 이후
 * 바꾸지 않는다.
 */
public class RegistryBlocks {

    public static Block FLUX_PLUG, FLUX_POINT;
    public static Block FLUX_STORAGE_1, FLUX_STORAGE_2, FLUX_STORAGE_3;
    public static Block FLUX_CONTROLLER;

    // TODO 3단계: 원작의 FLUX_BLOCK (BlockCore "FluxBlock", Material.ROCK)

    public static void register() {
        FLUX_PLUG = new BlockFluxPlug();
        FLUX_POINT = new BlockFluxPoint();
        FLUX_STORAGE_1 = new BlockFluxStorage();
        FLUX_STORAGE_2 = new BlockFluxStorage.Herculean();
        FLUX_STORAGE_3 = new BlockFluxStorage.Gargantuan();
        FLUX_CONTROLLER = new BlockFluxController();

        // 커넥터 전용 ItemBlock(이름·툴팁). 등록 이름은 그대로 유지한다
        GameRegistry.registerBlock(FLUX_PLUG, ItemFluxConnector.class, "fluxplug");
        GameRegistry.registerBlock(FLUX_POINT, ItemFluxConnector.class, "fluxpoint");
        GameRegistry.registerBlock(FLUX_STORAGE_1, ItemFluxConnector.class, "fluxstorage");
        GameRegistry.registerBlock(FLUX_STORAGE_2, ItemFluxConnector.class, "herculeanfluxstorage");
        GameRegistry.registerBlock(FLUX_STORAGE_3, ItemFluxConnector.class, "gargantuanfluxstorage");
        GameRegistry.registerBlock(FLUX_CONTROLLER, ItemFluxConnector.class, "fluxcontroller");
    }
}
