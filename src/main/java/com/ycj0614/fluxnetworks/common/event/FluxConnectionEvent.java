package com.ycj0614.fluxnetworks.common.event;

import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;

import cpw.mods.fml.common.eventhandler.Event;

// TODO 확인: 1.7.10 EventBus.register()는 이벤트 클래스를 인자 없는 생성자로 한 번 생성한다.
// 1.12와 마찬가지로 EventSubscriptionTransformer가 기본 생성자를 자동으로 추가한다고 보고 원작 그대로 두었다.
// 등록 시 로그에 NoSuchMethodException이 찍히면 각 클래스에 public 기본 생성자를 추가해야 한다.
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
