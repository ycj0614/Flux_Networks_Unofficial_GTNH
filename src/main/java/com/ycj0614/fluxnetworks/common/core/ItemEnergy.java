package com.ycj0614.fluxnetworks.common.core;

import net.minecraft.item.ItemStack;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;

/**
 * 전기 아이템 충전 헬퍼. 원작 IItemEnergyHandler/ItemEnergyHandler 대신 쓴다.
 * <p>
 * GTNH의 전기 아이템(GT 배터리·도구·전기 갑옷, IC2·GT++ 아이템)은 모두 IC2 IElectricItem 기반이다.
 * 아이템 충전에 한해 IC2 아이템 API를 허용하며, IC2 클래스를 import하는 곳은 이 클래스뿐이다.
 * ElectricItem.manager는 ISpecialElectricItem의 전용 매니저로 넘겨주는 게이트웨이라 따로 나누지 않는다.
 * <p>
 * 원작과 다름: 컨트롤러가 4틱마다 충전하므로 한 번에 아이템 전송 한도 × 4까지 넣는다 (틱당으로는 전송 한도).
 * 아이템 티어 제한은 무시한다.
 */
public final class ItemEnergy {

    /** 컨트롤러 충전 주기 (틱). 한 번에 전송 한도 × CHARGE_INTERVAL까지 넣는다 */
    public static final int CHARGE_INTERVAL = 4;

    private ItemEnergy() {}

    /**
     * @return 충전할 수 있는 전기 아이템이고 아직 가득 차지 않았으면 true
     */
    public static boolean canCharge(ItemStack stack) {
        if (!isElectricItem(stack)) {
            return false;
        }
        IElectricItem item = (IElectricItem) stack.getItem();
        return ElectricItem.manager.getCharge(stack) < item.getMaxCharge(stack);
    }

    /**
     * @param eu       넣으려는 EU
     * @param simulate true면 실제로 넣지 않고 넣을 수 있는 양만 계산
     * @return 실제로 넣은 (simulate면 넣을 수 있는) EU
     */
    public static long charge(ItemStack stack, long eu, boolean simulate) {
        if (eu <= 0 || !isElectricItem(stack)) {
            return 0;
        }
        IElectricItem item = (IElectricItem) stack.getItem();
        double limit = item.getTransferLimit(stack) * CHARGE_INTERVAL;
        double amount = Math.min((double) eu, limit);
        if (amount <= 0) {
            return 0;
        }
        double charged = ElectricItem.manager.charge(stack, amount, Integer.MAX_VALUE, true, simulate);
        if (charged <= 0) {
            return 0;
        }
        // 소수점 이하는 버린다 (네트워크 에너지는 long). 넣은 양보다 많이 빼지 않도록 eu로 자른다
        return Math.min((long) charged, eu);
    }

    /** GTModHandler.isElectricItem과 같은 조건. RF 아이템 등은 무시한다 */
    private static boolean isElectricItem(ItemStack stack) {
        return stack != null && stack.getItem() instanceof IElectricItem
            && ((IElectricItem) stack.getItem()).getTier(stack) < Integer.MAX_VALUE;
    }
}
