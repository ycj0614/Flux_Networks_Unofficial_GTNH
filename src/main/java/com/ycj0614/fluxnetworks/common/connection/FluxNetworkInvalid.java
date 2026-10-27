package com.ycj0614.fluxnetworks.common.connection;

import java.util.UUID;

import com.ycj0614.fluxnetworks.api.network.SecurityType;

public class FluxNetworkInvalid extends FluxNetworkBase {

    public static final FluxNetworkInvalid instance = new FluxNetworkInvalid();

    private FluxNetworkInvalid() {
        super(-1, "Please select a network", SecurityType.PUBLIC, 0xb6b6b6, new UUID(-1, -1), "");
    }

    @Override
    public boolean isInvalid() {
        return true;
    }
}
