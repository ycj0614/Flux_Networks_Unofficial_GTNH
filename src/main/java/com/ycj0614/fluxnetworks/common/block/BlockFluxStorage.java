package com.ycj0614.fluxnetworks.common.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxStorage;

/**
 * 플럭스 스토리지 블록 3종. 아이콘은 fluxstorage1~3_on/off (원작 모델 텍스처 tier1~3).
 * TODO 배치 R: 원작 모양 렌더링, ItemFluxStorageRenderer(아이템 에너지 게이지)
 */
public class BlockFluxStorage extends BlockFluxCore {

    public BlockFluxStorage() {
        this("fluxstorage", "fluxstorage1");
    }

    protected BlockFluxStorage(String name, String iconName) {
        super(name, iconName);
    }

    /** 저장 용량 (EU). 아이템 툴팁이 쓴다 */
    public long getMaxStorage() {
        return FluxConfig.basicCapacity;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileFluxStorage();
    }

    public static class Herculean extends BlockFluxStorage {

        public Herculean() {
            super("herculeanfluxstorage", "fluxstorage2");
        }

        @Override
        public long getMaxStorage() {
            return FluxConfig.herculeanCapacity;
        }

        @Override
        public TileEntity createTileEntity(World world, int metadata) {
            return new TileFluxStorage.Herculean();
        }
    }

    public static class Gargantuan extends BlockFluxStorage {

        public Gargantuan() {
            super("gargantuanfluxstorage", "fluxstorage3");
        }

        @Override
        public long getMaxStorage() {
            return FluxConfig.gargantuanCapacity;
        }

        @Override
        public TileEntity createTileEntity(World world, int metadata) {
            return new TileFluxStorage.Gargantuan();
        }
    }
}
