package com.voltcraft.electric;

import net.minecraft.util.StringRepresentable;

/**
 * 电压等级。对应设计文档 2.2 节。
 * 数值是真实世界的伏特数，仅用于显示和计算电流；底层 FE/t 作为功率单位独立。
 */
public enum VoltageTier implements StringRepresentable {
    LOW("low", 220),
    MEDIUM("medium", 10_000),
    HIGH("high", 35_000),
    EXTRA_HIGH("extra_high", 110_000);

    private final String name;
    private final int volts;

    VoltageTier(String name, int volts) {
        this.name = name;
        this.volts = volts;
    }

    public int volts() {
        return volts;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
