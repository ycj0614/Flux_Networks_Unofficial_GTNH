package com.ycj0614.fluxnetworks.common.tileentity;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.api.network.ConnectionType;
import com.ycj0614.fluxnetworks.api.network.ITransferHandler;
import com.ycj0614.fluxnetworks.api.tiles.IFluxPoint;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.connection.transfer.FluxPointHandler;
import com.ycj0614.fluxnetworks.common.core.GTEnergy;

import io.netty.buffer.ByteBuf;

public class TileFluxPoint extends TileFluxConnector implements IFluxPoint {

    /** 출력 티어 NBT 키 (ALL_SAVE, TILE_UPDATE, TILE_DROP 공통) */
    public static final String NBT_OUTPUT_TIER = "ot";
    /** 출력 암페어 NBT 키 (long) */
    public static final String NBT_OUTPUT_AMPERAGE = "oa";

    private final FluxPointHandler handler = new FluxPointHandler(this);

    protected int outputTier = FluxConfig.defaultPointTier;
    protected long outputAmperage = Math.max(1L, FluxConfig.defaultPointAmperage);

    public TileFluxPoint() {
        customName = "Flux Point";
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.POINT;
    }

    @Nonnull
    @Override
    public ITransferHandler getTransferHandler() {
        return handler;
    }

    // ---- EU 설정 ----

    @Override
    public int getOutputTier() {
        return outputTier;
    }

    @Override
    public long getOutputAmperage() {
        return outputAmperage;
    }

    public void setOutputTier(int tier) {
        outputTier = GTEnergy.clampTier(tier);
        notifySettingChanged();
    }

    public void setOutputAmperage(long amperage) {
        outputAmperage = Math.max(1L, amperage);
        notifySettingChanged();
    }

    // ---- ITileByteBuf: ID 6 = 출력 티어(int), ID 7 = 출력 암페어(long). 나머지는 TileFluxCore ----

    @Override
    public void writePacket(ByteBuf buf, int id) {
        if (id == 6) {
            buf.writeInt(outputTier);
        } else if (id == 7) {
            buf.writeLong(outputAmperage);
        } else {
            super.writePacket(buf, id);
        }
    }

    @Override
    public void readPacket(ByteBuf buf, int id) {
        if (id == 6) {
            setOutputTier(buf.readInt()); // setter가 범위 보정과 동기화를 한다
        } else if (id == 7) {
            setOutputAmperage(buf.readLong());
        } else {
            super.readPacket(buf, id);
        }
    }

    /**
     * 틱당 최대 출력 = 전압(outputTier) × outputAmperage. 넘치면 Long.MAX_VALUE.
     * 원작의 limit, disableLimit은 포인트에서 쓰지 않는다 (필드와 NBT는 유지).
     */
    @Override
    public long getLogicLimit() {
        return getMaxOutputPerTick();
    }

    @Override
    public long getRawLimit() {
        return getMaxOutputPerTick();
    }

    private long getMaxOutputPerTick() {
        long voltage = GTEnergy.voltage(outputTier);
        long amperage = outputAmperage;
        if (voltage <= 0 || amperage <= 0) {
            return 0;
        }
        if (voltage > Long.MAX_VALUE / amperage) {
            return Long.MAX_VALUE;
        }
        return voltage * amperage;
    }

    @Override
    public NBTTagCompound writeCustomNBT(NBTTagCompound tag, NBTType type) {
        super.writeCustomNBT(tag, type);
        if (isTileNBT(type)) {
            tag.setInteger(NBT_OUTPUT_TIER, outputTier);
            tag.setLong(NBT_OUTPUT_AMPERAGE, outputAmperage);
        }
        return tag;
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag, NBTType type) {
        super.readCustomNBT(tag, type);
        if (isTileNBT(type)) {
            if (tag.hasKey(NBT_OUTPUT_TIER)) {
                outputTier = GTEnergy.clampTier(tag.getInteger(NBT_OUTPUT_TIER));
            }
            if (tag.hasKey(NBT_OUTPUT_AMPERAGE)) {
                outputAmperage = Math.max(1L, tag.getLong(NBT_OUTPUT_AMPERAGE));
            }
        }
    }

    // ---- IEnergyConnected: 인접 GT 장치로 EU를 내보낸다 (전송은 FluxPointHandler가 한다) ----

    @Override
    public long injectEnergyUnits(ForgeDirection side, long aVoltage, long aAmperage) {
        return 0;
    }

    @Override
    public boolean inputEnergyFrom(ForgeDirection side) {
        return false;
    }

    /**
     * 모든 면에서 true. GT 케이블이 이 면에 연결되려면 true여야 한다 (MTECable.canConnect).
     */
    @Override
    public boolean outputsEnergyTo(ForgeDirection side) {
        return true;
    }
}
