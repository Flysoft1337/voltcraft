package com.voltcraft.electric.protection;

import net.minecraft.util.StringRepresentable;

/**
 * 空开/漏保的运行状态。
 *
 * 设计文档 4.2：
 * - CLOSED：合闸，正常导通
 * - TRIPPED_OVERLOAD：过载跳闸（电流 >120% 持续 5s 或 >200% 立即）
 * - TRIPPED_SHORT：短路跳闸（火线直接接零线/地线）
 * - TRIPPED_LEAKAGE：漏电跳闸（漏保专属，第二阶段后续启用）
 *
 * 跳闸状态下设备不导通，需要玩家右键合闸恢复。
 */
public enum BreakerState implements StringRepresentable {
    CLOSED("closed"),
    TRIPPED_OVERLOAD("tripped_overload"),
    TRIPPED_SHORT("tripped_short"),
    TRIPPED_LEAKAGE("tripped_leakage");

    private final String name;

    BreakerState(String name) {
        this.name = name;
    }

    public boolean isTripped() {
        return this != CLOSED;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
