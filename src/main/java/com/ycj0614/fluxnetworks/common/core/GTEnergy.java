package com.ycj0614.fluxnetworks.common.core;

import gregtech.api.enums.GTValues;

/**
 * GT5U 전압 티어 공용 헬퍼. GT 클래스(GTValues)를 직접 쓰는 곳은 여기로 모은다.
 * <p>
 * 유효한 티어는 0(ULV) ~ 14(MAX). GTValues.V[15](MAX+)는 GT 내부 오류 방지용이라 쓰지 않는다.
 */
public final class GTEnergy {

    public static final int MIN_TIER = 0, MAX_TIER = 14;

    private GTEnergy() {}

    public static int clampTier(int tier) {
        if (tier < MIN_TIER) {
            return MIN_TIER;
        }
        if (tier > MAX_TIER) {
            return MAX_TIER;
        }
        return tier;
    }

    /**
     * @return 해당 티어의 전압 (EU/A)
     */
    public static long voltage(int tier) {
        return GTValues.V[clampTier(tier)];
    }

    /**
     * @return 티어 이름 ("ULV", "LV", ... "MAX")
     */
    public static String tierName(int tier) {
        return GTValues.VN[clampTier(tier)];
    }
}
