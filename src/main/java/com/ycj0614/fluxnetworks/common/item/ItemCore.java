package com.ycj0614.fluxnetworks.common.item;

import net.minecraft.item.Item;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.common.CommonProxy;

/**
 * 원작 ItemCore. 원작의 RegistryItems.ITEMS 자동 추가와 registerModels는 1.7.10에 없으므로
 * 등록은 RegistryItems.register()에서, 아이콘은 setTextureName으로 한다.
 */
public class ItemCore extends Item {

    /**
     * @param name 번역 키·아이콘 이름 (item.fluxnetworks.&lt;name&gt;.name, textures/items/&lt;name&gt;.png)
     */
    public ItemCore(String name) {
        setUnlocalizedName(FluxNetworks.MODID + "." + name);
        setTextureName(FluxNetworks.MODID + ":" + name);
        setCreativeTab(CommonProxy.creativeTabs);
    }
}
