package com.ycj0614.fluxnetworks.common.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxStorage;

/**
 * 플럭스 스토리지 블록 3종. 모델 텍스처는 원작 tier1~3 (켜짐), tierN_off (꺼짐), tierN_colour.
 * 에너지 게이지는 TileFluxStorageRenderer(월드)와 ItemFluxDeviceRenderer(아이템)가 그린다.
 */
public class BlockFluxStorage extends BlockFluxCore {

    public BlockFluxStorage() {
        this("fluxstorage", 1);
    }

    protected BlockFluxStorage(String name, int tier) {
        super(name);
        setModelTexture(TEX_0, "gray");
        setModelTexture(TEX_1, "tier" + tier, "tier" + tier + "_off");
        setModelTexture(TEX_COLOUR, "tier" + tier + "_colour");
        setParticleSlot(TEX_1);
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
            super("herculeanfluxstorage", 2);
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
            super("gargantuanfluxstorage", 3);
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
