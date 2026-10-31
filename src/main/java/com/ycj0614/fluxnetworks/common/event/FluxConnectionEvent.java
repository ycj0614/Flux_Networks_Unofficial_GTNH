package com.ycj0614.fluxnetworks.common.event;

import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;

import cpw.mods.fml.common.eventhandler.Event;

// 1.7.10 EventBus.register()는 이벤트 클래스를 인자 없는 생성자로 한 번 생성한다.
// EventSubscriptionTransformer가 기본 생성자를 자동으로 추가하므로 원작 그대로 둔다 (2단계 멀티플레이 테스트에서 확인).
public class FluxConnectionEvent extends Event {

    public final IFluxConnector flux;

    public FluxConnectionEvent(IFluxConnector flux) {
        super();
        this.flux = flux;
    }

    public static class Connected extends FluxConnectionEvent {

        public final IFluxNetwork network;

        public Connected(IFluxConnector flux, IFluxNetwork network) {
            super(flux);
            this.network = network;
        }
    }

    public static class Disconnected extends FluxConnectionEvent {

        public final IFluxNetwork network;

        public Disconnected(IFluxConnector flux, IFluxNetwork network) {
            super(flux);
            this.network = network;
        }
    }
}
