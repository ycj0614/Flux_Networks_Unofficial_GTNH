package com.ycj0614.fluxnetworks.common.connection;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import com.ycj0614.fluxnetworks.api.network.FluxLogicType;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.tiles.IFluxPlug;
import com.ycj0614.fluxnetworks.api.tiles.IFluxPoint;
import com.ycj0614.fluxnetworks.api.tiles.IFluxStorage;

/**
 * 모든 에너지 수치는 EU 단위 (내부 단위 EU 1:1)
 */
public class NetworkStatistics {

    public static final int CHANGE_COUNT = 6;

    private final IFluxNetwork network;

    private int timer;

    public int fluxPlugCount;
    public int fluxPointCount;
    public int fluxControllerCount;
    public int fluxStorageCount;

    public long energyInput;
    public long energyOutput;

    public final long[] energyChange = new long[CHANGE_COUNT];

    public long totalBuffer;
    public long totalEnergy;

    private long energyChange5;
    private long energyInput4;
    private long energyOutput4;

    public int averageTickMicro;
    private long runningTotalNano;

    private long startNanoTime;

    public NetworkStatistics(IFluxNetwork network) {
        this.network = network;
    }

    public void startProfiling() {
        startNanoTime = System.nanoTime();
    }

    public void stopProfiling() {
        if (timer == 0) {
            weakestTick();
        }
        if (timer % 5 == 0) {
            weakTick();
        }
        if (timer % 20 == 0) {
            weakerTick();
        }
        runningTotalNano += System.nanoTime() - startNanoTime;

        timer = ++timer % 100;
    }

    /**
     * Called every 5 ticks
     */
    private void weakTick() {
        List<IFluxPlug> plugs = network.getConnections(FluxLogicType.PLUG);
        plugs.forEach(p -> {
            if (!p.getConnectionType()
                .isStorage()) {
                energyInput4 += p.getTransferChange();
            }
        });
        List<IFluxPoint> points = network.getConnections(FluxLogicType.POINT);
        points.forEach(p -> {
            if (!p.getConnectionType()
                .isStorage()) {
                energyOutput4 -= p.getTransferChange();
            }
        });
    }

    /**
     * Called every 20 ticks
     */
    private void weakerTick() {
        totalBuffer = 0;
        totalEnergy = 0;
        List<IFluxConnector> devices = network.getConnections(FluxLogicType.ANY);
        devices.forEach(p -> {
            if (!p.getConnectionType()
                .isStorage()) {
                totalBuffer += p.getTransferBuffer();
            }
        });
        List<IFluxStorage> storages = network.getConnections(FluxLogicType.STORAGE);
        storages.forEach(p -> totalEnergy += p.getTransferBuffer());
        fluxControllerCount = network.getConnections(FluxLogicType.CONTROLLER)
            .size();
        fluxStorageCount = storages.size();
        fluxPlugCount = network.getConnections(FluxLogicType.PLUG)
            .size() - fluxStorageCount;
        fluxPointCount = network.getConnections(FluxLogicType.POINT)
            .size() - fluxStorageCount
            - fluxControllerCount;
        energyInput = energyInput4 / 4;
        energyOutput = energyOutput4 / 4;
        energyInput4 = 0;
        energyOutput4 = 0;
        energyChange5 += Math.max(energyInput, energyOutput);

        averageTickMicro = (int) Math.min(runningTotalNano / 20000, Integer.MAX_VALUE);
        runningTotalNano = 0;
    }

    /**
     * Called every 100 ticks
     */
    private void weakestTick() {
        for (int i = 1; i < CHANGE_COUNT; i++) {
            energyChange[i - 1] = energyChange[i];
        }
        energyChange[CHANGE_COUNT - 1] = energyChange5 / 5;
        energyChange5 = 0;
    }

    public int getConnectionCount() {
        return this.fluxPlugCount + this.fluxPointCount + this.fluxStorageCount + this.fluxControllerCount;
    }

    public void writeNBT(NBTTagCompound tag) {
        tag.setInteger("i1", fluxPlugCount);
        tag.setInteger("i2", fluxPointCount);
        tag.setInteger("i3", fluxControllerCount);
        tag.setInteger("i4", fluxStorageCount);
        tag.setLong("l1", energyInput);
        tag.setLong("l2", energyOutput);
        tag.setLong("l3", totalBuffer);
        tag.setLong("l4", totalEnergy);
        tag.setInteger("9", averageTickMicro);

        for (int i = 0; i < energyChange.length; i++) {
            tag.setLong("a" + i, energyChange[i]);
        }
    }

    public void readNBT(NBTTagCompound tag) {
        fluxPlugCount = tag.getInteger("i1");
        fluxPointCount = tag.getInteger("i2");
        fluxControllerCount = tag.getInteger("i3");
        fluxStorageCount = tag.getInteger("i4");
        energyInput = tag.getLong("l1");
        energyOutput = tag.getLong("l2");
        totalBuffer = tag.getLong("l3");
        totalEnergy = tag.getLong("l4");
        averageTickMicro = tag.getInteger("9");
        for (int i = 0; i < CHANGE_COUNT; i++) {
            energyChange[i] = tag.getLong("a" + i);
        }
    }
}
