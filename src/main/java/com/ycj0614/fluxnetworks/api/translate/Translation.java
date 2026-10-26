package com.ycj0614.fluxnetworks.api.translate;

import net.minecraft.util.StatCollector;

public class Translation {

    public String key;

    public Translation(String key) {
        this.key = key;
    }

    public String t() {
        return StatCollector.translateToLocal(key);
    }

}
