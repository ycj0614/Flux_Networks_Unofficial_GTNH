package com.ycj0614.fluxnetworks.common.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.api.network.ConnectionType;
import com.ycj0614.fluxnetworks.api.network.ITransferHandler;
import com.ycj0614.fluxnetworks.api.tiles.IFluxPlug;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.connection.transfer.FluxPlugHandler;
import com.ycj0614.fluxnetworks.common.core.GTEnergy;

import io.netty.buffer.ByteBuf;

public class TileFluxPlug extends TileFluxConnector implements IFluxPlug {

    /** 최대 입력 티어 NBT 키 (ALL_SAVE, TILE_UPDATE, TILE_DROP 공통) */
    public static final String NBT_MAX_INPUT_TIER = "mt";

    private final FluxPlugHandler handler = new FluxPlugHandler(this);

    protected int maxInputTier = FluxConfig.defaultPlugMaxInputTier;

    public TileFluxPlug() {
        customName = "Flux Plug";
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.PLUG;
    }

    @Override
    public ITransferHandler getTransferHandler() {
        return handler;
    }

    // ---- EU 설정 ----

    @Override
    public int getMaxInputTier() {
        return maxInputTier;
    }

    public void setMaxInputTier(int tier) {
        maxInputTier = GTEnergy.clampTier(tier);
        notifySettingChanged();
    }

    // ---- ITileByteBuf: ID 6 = 최대 입력 티어(int). 나머지는 TileFluxCore (ID 표는 TileFluxCore.writePacket 위) ----

    @Override
    public void writePacket(ByteBuf buf, int id) {
        if (id == 6) {
            buf.writeInt(maxInputTier);
        } else {
            super.writePacket(buf, id);
        }
    }

    @Override
    public void readPacket(ByteBuf buf, int id) {
        if (id == 6) {
            setMaxInputTier(buf.readInt()); // setter가 범위 보정과 동기화를 한다
        } else {
            super.readPacket(buf, id);
        }
    }

    @Override
    public NBTTagCompound writeCustomNBT(NBTTagCompound tag, NBTType type) {
        super.writeCustomNBT(tag, type);
        if (isTileNBT(type)) {
            tag.setInteger(NBT_MAX_INPUT_TIER, maxInputTier);
        }
        return tag;
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag, NBTType type) {
        super.readCustomNBT(tag, type);
        if (isTileNBT(type) && tag.hasKey(NBT_MAX_INPUT_TIER)) {
            maxInputTier = GTEnergy.clampTier(tag.getInteger(NBT_MAX_INPUT_TIER));
        }
    }

    // ---- IEnergyConnected: GT 케이블·발전기에서 EU를 받는다 ----

    @Override
    public long injectEnergyUnits(ForgeDirection side, long aVoltage, long aAmperage) {
        if (worldObj == null || worldObj.isRemote || isInvalid()) {
            return 0;
        }
        return handler.injectEnergyUnits(side, aVoltage, aAmperage);
    }

    /**
     * 모든 면과 UNKNOWN에서 true. GT 케이블은 이 값이 true인 타일만 소비자로 등록한다.
     */
    @Override
    public boolean inputEnergyFrom(ForgeDirection side) {
        return true;
    }

    @Override
    public boolean outputsEnergyTo(ForgeDirection side) {
        return false;
    }
}
