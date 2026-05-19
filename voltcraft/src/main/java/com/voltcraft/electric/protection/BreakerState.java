package com.voltcraft.electric.protection;

import net.minecraft.util.StringRepresentable;

/**
 * 空开/漏保的运行状态。
 *
 * - CLOSED：合闸，正常导通
 * - OPEN_MANUAL：玩家手动拉闸（断电），不是故障
 * - TRIPPED_OVERLOAD：过载跳闸（电流 >120% 持续 5s 或 >200% 立即）
 * - TRIPPED_SHORT：短路跳闸（火线直接接零线/地线）
 * - TRIPPED_LEAKAGE：漏电跳闸（漏保专属）
 *
 * 行为差异：
 * - OPEN_MANUAL 与 TRIPPED_*：都不导通；玩家右键都能闭合
 * - TRIPPED_*：故障状态，由系统设置；带故障颜色
 */
public enum BreakerState implements StringRepresentable {
    CLOSED("closed"),
    OPEN_MANUAL("open_manual"),
    TRIPPED_OVERLOAD("tripped_overload"),
    TRIPPED_SHORT("tripped_short"),
    TRIPPED_LEAKAGE("tripped_leakage");

    private final String name;

    BreakerState(String name) {
        this.name = name;
    }

    /** 是否处于"故障跳闸"状态。手动断开 OPEN_MANUAL 不算故障。 */
    public boolean isTripped() {
        return this == TRIPPED_OVERLOAD || this == TRIPPED_SHORT || this == TRIPPED_LEAKAGE;
    }

    /** 是否导通——只有 CLOSED 才让电流通过。 */
    public boolean conducts() {
        return this == CLOSED;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
