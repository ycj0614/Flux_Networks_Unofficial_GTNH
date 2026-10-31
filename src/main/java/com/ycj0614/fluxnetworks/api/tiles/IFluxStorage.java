package com.ycj0614.fluxnetworks.api.tiles;

/**
 * Flux Storage: 네트워크 안에서만 EU를 저장하고 꺼낸다. 전송 사이클에서 플러그(PLUG)와 포인트(POINT) 양쪽으로 쓰이므로 둘 다 상속한다.
 * <p>
 * 원작과 다름: IFluxPlug·IFluxPoint의 EU 설정(티어·암페어)은 GT와 직접 주고받을 때만 쓰므로, 스토리지는 쓰이지 않는 고정값을 돌려준다.
 */
public interface IFluxStorage extends IFluxPoint, IFluxPlug {
}
