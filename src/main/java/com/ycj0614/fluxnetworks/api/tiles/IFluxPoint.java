package com.ycj0614.fluxnetworks.api.tiles;

/**
 * Flux Point: 네트워크의 EU를 인접 GT 장치로 내보낸다.
 */
public interface IFluxPoint extends IFluxConnector {

    /**
     * 출력 전압 티어 (0 = ULV ~ 14 = MAX). 기본값은 FluxConfig.defaultPointTier.
     * 인접 GT 기계의 입력 전압보다 높으면 GT 규칙대로 그 기계가 폭발한다 (의도된 동작).
     */
    int getOutputTier();

    /**
     * 틱당 최대 출력 암페어. 기본값은 FluxConfig.defaultPointAmperage.
     */
    long getOutputAmperage();
}
