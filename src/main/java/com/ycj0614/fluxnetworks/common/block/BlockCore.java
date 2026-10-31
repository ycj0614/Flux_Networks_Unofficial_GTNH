package com.ycj0614.fluxnetworks.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.common.CommonProxy;

/**
 * 원작 BlockCore (Flux Block 등 타일 없는 단순 블록). 원작의 RegistryBlocks.BLOCKS·ItemBlock 자동 추가와 registerModels는
 * 1.7.10에 없으므로 등록은 RegistryBlocks.register()에서, 아이콘은 setBlockTextureName으로 한다.
 * 원작처럼 경도를 따로 정하지 않는다 (0, 바로 부서짐).
 * <p>
 * 원작의 두 번째 생성자(special, ItemFluxConnector 사용)는 장치 블록용이었고 여기서는 BlockFluxCore가 맡으므로 옮기지 않는다.
 */
public class BlockCore extends Block {

    /**
     * @param name 번역 키·아이콘 이름 (tile.fluxnetworks.&lt;name&gt;.name, textures/blocks/&lt;name&gt;.png)
     */
    public BlockCore(String name, Material material) {
        super(material);
        setBlockName(FluxNetworks.MODID + "." + name);
        setBlockTextureName(FluxNetworks.MODID + ":" + name);
        setCreativeTab(CommonProxy.creativeTabs);
    }
}
