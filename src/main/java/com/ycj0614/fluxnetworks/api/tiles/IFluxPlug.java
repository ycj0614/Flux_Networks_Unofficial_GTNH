package com.ycj0614.fluxnetworks.api.tiles;

/**
 * Flux Plug: 외부(GT 케이블·발전기)에서 EU를 받아 네트워크로 보낸다.
 */
public interface IFluxPlug extends IFluxConnector {

    /**
     * 플러그가 받는 최대 전압 티어 (0 = ULV ~ 14 = MAX).
     * 이 티어의 전압보다 높은 전압은 거부한다(0 암페어 반환, 폭발 없음).
     * 기본값은 FluxConfig.defaultPlugMaxInputTier.
     */
    int getMaxInputTier();
}
