package com.ycj0614.fluxnetworks.api.network;

import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.tiles.IFluxController;
import com.ycj0614.fluxnetworks.api.tiles.IFluxPlug;
import com.ycj0614.fluxnetworks.api.tiles.IFluxPoint;
import com.ycj0614.fluxnetworks.api.tiles.IFluxStorage;

/**
 * Logic types in flux networks
 */
public enum FluxLogicType {

    ANY(IFluxConnector.class),
    PLUG(IFluxPlug.class), // plug and storage
    POINT(IFluxPoint.class), // point, storage and controller
    STORAGE(IFluxStorage.class),
    CONTROLLER(IFluxController.class);

    private final Class<? extends IFluxConnector> clazz;

    FluxLogicType(Class<? extends IFluxConnector> clazz) {
        this.clazz = clazz;
    }

    /**
     * Get valid logic types for a flux device
     *
     * @param tile the tile entity that extends IFluxDevice
     * @return a set of logic types that the given device can work as
     */
    @Nonnull
    public static Set<FluxLogicType> getValidTypes(IFluxConnector tile) {
        Set<FluxLogicType> valid = EnumSet.noneOf(FluxLogicType.class);
        for (FluxLogicType type : FluxLogicType.values()) {
            if (type.clazz.isInstance(tile)) {
                valid.add(type);
            }
        }
        return valid;
    }
}
