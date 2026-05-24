package com.voltcraft.electric.wire;

import net.minecraft.core.BlockPos;

/**
 * 线缆连接端点。
 * 表示一个可以连接线缆的位置。
 *
 * @param pos 方块位置
 * @param endpointIndex 端点索引（用于区分同一方块上的多个连接点）
 */
public record WireEndpoint(BlockPos pos, int endpointIndex) {

    /**
     * 创建一个端点。
     *
     * @param pos 方块位置
     * @param endpointIndex 端点索引
     */
    public WireEndpoint {
        // 不可变记录类
    }

    /**
     * 获取端点的世界坐标（方块中心）。
     *
     * @return 世界坐标
     */
    public net.minecraft.world.phys.Vec3 getWorldPosition() {
        return net.minecraft.world.phys.Vec3.atCenterOf(pos);
    }

    @Override
    public String toString() {
        return "WireEndpoint{" + pos.toShortString() + ", index=" + endpointIndex + "}";
    }
}
