package com.ycj0614.fluxnetworks.common.connection.transfer;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxStorage;

/**
 * 스토리지 전송 핸들러 (원작 그대로). 네트워크 안에서만 에너지를 주고받는다.
 * 저장 한도는 TileFluxStorage.getMaxTransferLimit(), 틱당 입출력 한도는 getLogicLimit().
 */
public class FluxStorageHandler extends BasicTransferHandler<TileFluxStorage> {

    /** 저장 에너지 NBT 키 (ALL_SAVE, TILE_DROP). 아이템 툴팁도 이 키를 읽는다 */
    public static final String NBT_ENERGY = "energy";

    private long added;
    private long removed;

    public FluxStorageHandler(TileFluxStorage fluxStorage) {
        super(fluxStorage);
    }

    @Override
    public void onCycleStart() {}

    @Override
    public void onCycleEnd() {
        change = added - removed;
        added = 0;
        removed = 0;
    }

    @Override
    public void addToBuffer(long energy) {
        if (energy <= 0) {
            return;
        }
        buffer += energy;
        added += energy;
        device.markServerEnergyChanged();
    }

    @Override
    public long removeFromBuffer(long energy) {
        long a = Math.min(Math.min(energy, buffer), device.getLogicLimit() - removed);
        if (a <= 0) {
            return 0;
        }
        buffer -= a;
        removed += a;
        device.markServerEnergyChanged();
        return a;
    }

    @Override
    public long getRequest() {
        return Math.max(0, Math.min(device.getMaxTransferLimit() - buffer, device.getLogicLimit() - added));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag, NBTType type) {
        if (type == NBTType.TILE_UPDATE) {
            super.writeCustomNBT(tag, type); // 클라이언트 GUI용 "buffer"와 변화량
        }
        if (type == NBTType.ALL_SAVE || type == NBTType.TILE_DROP) {
            tag.setLong(NBT_ENERGY, buffer);
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag, NBTType type) {
        if (type == NBTType.TILE_UPDATE) {
            super.readCustomNBT(tag, type);
        }
        if (type == NBTType.ALL_SAVE || type == NBTType.TILE_DROP) {
            buffer = tag.getLong(NBT_ENERGY);
        }
    }

    /** 스토리지는 인접 블록과 에너지를 주고받지 않는다 */
    @Override
    public void updateTransfers(@Nonnull ForgeDirection... faces) {}
}
