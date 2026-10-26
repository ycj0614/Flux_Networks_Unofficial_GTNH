package com.ycj0614.fluxnetworks.api.network;

import javax.annotation.Nonnull;

import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.api.translate.Translation;

public enum WirelessType {

    ENABLE_WIRELESS(FluxTranslate.ENABLE_WIRELESS),
    RIGHT_HAND(FluxTranslate.RIGHT_HAND),
    LEFT_HAND(FluxTranslate.LEFT_HAND),
    HOT_BAR(FluxTranslate.HOT_BAR),
    ARMOR_SLOT(FluxTranslate.ARMOR),
    BAUBLES(FluxTranslate.BAUBLES), // Baubles 미지원, 순서 유지용
    INVENTORY(FluxTranslate.INVENTORY);

    private final Translation typeName;

    WirelessType(Translation typeName) {
        this.typeName = typeName;
    }

    public boolean isActivated(int setting) {
        if (this == INVENTORY) {
            return false;
        }
        return (setting >> ordinal() & 1) == 1;
    }

    public boolean isActivated(@Nonnull IFluxNetwork network) {
        return isActivated(network.getSetting(NetworkSettings.NETWORK_WIRELESS));
    }

    public String getTranslatedName() {
        return typeName.t();
    }
}
