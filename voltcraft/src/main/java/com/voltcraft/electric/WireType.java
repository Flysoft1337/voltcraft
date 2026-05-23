package com.voltcraft.electric;

import net.minecraft.util.StringRepresentable;

/**
 * 线缆类型枚举。
 * 每种线缆对应一个电压等级和最大连接距离。
 */
public enum WireType implements StringRepresentable {
    COPPER("copper", CableTier.LOW, 16),      // 低压，16格
    TIN("tin", CableTier.MEDIUM, 24),          // 中压，24格
    SILVER("silver", CableTier.HIGH, 32);      // 高压，32格

    private final String name;
    private final CableTier tier;
    private final int maxDistance;

    WireType(String name, CableTier tier, int maxDistance) {
        this.name = name;
        this.tier = tier;
        this.maxDistance = maxDistance;
    }

    public String getName() {
        return name;
    }

    public CableTier tier() {
        return tier;
    }

    public int maxDistance() {
        return maxDistance;
    }

    public VoltageTier voltage() {
        return tier.voltage();
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
