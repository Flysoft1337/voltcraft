package com.voltcraft.electric.wire;

import com.voltcraft.electric.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * 线缆连接。
 * 表示两个端点之间的一条线缆连接。
 */
public class WireConnection {

    private final WireEndpoint start;
    private final WireEndpoint end;
    private final WireType wireType;
    private final double distance;

    public WireConnection(WireEndpoint start, WireEndpoint end, WireType wireType) {
        this.start = start;
        this.end = end;
        this.wireType = wireType;
        this.distance = calculateDistance(start.pos(), end.pos());
    }

    public WireEndpoint start() {
        return start;
    }

    public WireEndpoint end() {
        return end;
    }

    public WireType wireType() {
        return wireType;
    }

    public double distance() {
        return distance;
    }

    /**
     * 检查此连接是否包含指定位置。
     *
     * @param pos 要检查的位置
     * @return 是否包含
     */
    public boolean contains(BlockPos pos) {
        return start.pos().equals(pos) || end.pos().equals(pos);
    }

    /**
     * 检查此连接是否包含指定端点。
     *
     * @param endpoint 要检查的端点
     * @return 是否包含
     */
    public boolean contains(WireEndpoint endpoint) {
        return start.equals(endpoint) || end.equals(endpoint);
    }

    /**
     * 获取连接的另一个端点。
     *
     * @param endpoint 一个端点
     * @return 另一个端点
     */
    public WireEndpoint getOther(WireEndpoint endpoint) {
        if (start.equals(endpoint)) {
            return end;
        } else if (end.equals(endpoint)) {
            return start;
        }
        throw new IllegalArgumentException("Endpoint not part of this connection");
    }

    /**
     * 计算两个位置之间的距离。
     *
     * @param pos1 位置1
     * @param pos2 位置2
     * @return 距离
     */
    private double calculateDistance(BlockPos pos1, BlockPos pos2) {
        return Math.sqrt(pos1.distSqr(pos2));
    }

    /**
     * 检查连接是否有效（距离不超过最大限制）。
     *
     * @return 是否有效
     */
    public boolean isValid() {
        return distance <= wireType.maxDistance();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WireConnection other)) return false;
        return (start.equals(other.start) && end.equals(other.end)) ||
               (start.equals(other.end) && end.equals(other.start));
    }

    @Override
    public int hashCode() {
        // 确保相同连接（无论方向）具有相同的哈希值
        return start.hashCode() + end.hashCode();
    }

    @Override
    public String toString() {
        return "WireConnection{" + start + " -> " + end + ", type=" + wireType + ", dist=" + String.format("%.1f", distance) + "}";
    }
}
