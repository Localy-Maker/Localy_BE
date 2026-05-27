package org.example.localy.entity.premium;

import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.PremiumErrorCode;

public enum PremiumPlan {
    SEVEN_DAY("7day", "7일권", 7, 50),
    THIRTY_DAY("30day", "30일권", 30, 200);

    private final String code;
    private final String displayName;
    private final int durationDays;
    private final int price;

    PremiumPlan(String code, String displayName, int durationDays, int price) {
        this.code = code;
        this.displayName = displayName;
        this.durationDays = durationDays;
        this.price = price;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public int getDurationDays() { return durationDays; }
    public int getPrice() { return price; }

    public static PremiumPlan fromCode(String code) {
        for (PremiumPlan plan : values()) {
            if (plan.code.equals(code)) {
                return plan;
            }
        }
        throw new CustomException(PremiumErrorCode.PLAN_NOT_FOUND);
    }
}
