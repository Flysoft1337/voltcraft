package com.voltcraft.electric;

import net.minecraft.util.StringRepresentable;

/**
 * 电缆等级。与 VoltageTier 一一对应（设计文档 2.2.2 强绑定）。
 * ratedTransfer 是 FE/t 上限，超过则触发空开过载逻辑（第二阶段）。
 */
public enum CableTier implements StringRepresentable {
    LOW("low_voltage", VoltageTier.LOW, 1_000),
    MEDIUM("medium_voltage", VoltageTier.MEDIUM, 8_000),
    HIGH("high_voltage", VoltageTier.HIGH, 32_000),
    EXTRA_HIGH("extra_high_voltage", VoltageTier.EXTRA_HIGH, 128_000);

    private final String name;
    private final VoltageTier voltage;
    private final int ratedTransfer;

    CableTier(String name, VoltageTier voltage, int ratedTransfer) {
        this.name = name;
        this.voltage = voltage;
        this.ratedTransfer = ratedTransfer;
    }

    public VoltageTier voltage() {
        return voltage;
    }

    public int ratedTransfer() {
        return ratedTransfer;
    }

    /** 注册名前缀，例如 low_voltage_cable / medium_voltage_cable。 */
    public String blockName() {
        return name + "_cable";
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
