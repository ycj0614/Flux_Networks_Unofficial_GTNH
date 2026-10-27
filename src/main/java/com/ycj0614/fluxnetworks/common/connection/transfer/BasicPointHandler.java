package com.ycj0614.fluxnetworks.common.connection.transfer;

import com.ycj0614.fluxnetworks.api.tiles.IFluxPoint;

/**
 * 출력 장치(포인트 계열)의 공통 핸들러.
 * <p>
 * GT EU에는 시뮬레이션이 없으므로 sendToConsumers는 실제 전송만 한다. 출력 한도(틱당 V × A)는
 * 각 구현이 지킨다.
 * <p>
 * 3단계의 FluxStorageHandler, 4단계의 FluxControllerHandler도 이 클래스를 상속하며
 * 새 sendToConsumers(long) 시그니처를 구현해야 한다.
 */
public abstract class BasicPointHandler<T extends IFluxPoint> extends BasicTransferHandler<T> {

    protected long demand;

    public BasicPointHandler(T device) {
        super(device);
    }

    @Override
    public void onCycleEnd() {
        buffer += change = -sendToConsumers(buffer);
    }

    @Override
    public void addToBuffer(long energy) {
        if (energy <= 0) {
            return;
        }
        buffer += energy;
    }

    @Override
    public long getRequest() {
        return Math.max(demand - buffer, 0);
    }

    /**
     * 버퍼에서 최대 energy만큼 인접 소비자에게 실제로 보낸다.
     *
     * @param energy 보낼 수 있는 최대 EU (현재 버퍼)
     * @return 실제로 보낸 EU (0 ~ energy)
     */
    protected abstract long sendToConsumers(long energy);
}
