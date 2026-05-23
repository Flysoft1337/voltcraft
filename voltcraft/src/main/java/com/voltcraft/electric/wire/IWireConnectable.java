package com.voltcraft.electric.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 可连接线缆的方块接口。
 * 实现此接口的方块可以被线圈连接。
 */
public interface IWireConnectable {

    /**
     * 获取此方块上的连接点列表。
     * 一个方块可以有多个连接点（如变压器有输入/输出两个点）。
     *
     * @param pos 方块位置
     * @param state 方块状态
     * @return 连接点列表
     */
    List<WireEndpoint> getWireEndpoints(BlockPos pos, BlockState state);

    /**
     * 当线缆连接到此方块时调用。
     *
     * @param connection 建立的连接
     */
    default void onWireConnected(WireConnection connection) {}

    /**
     * 当线缆从此方块断开时调用。
     *
     * @param connection 断开的连接
     */
    default void onWireDisconnected(WireConnection connection) {}

    /**
     * 检查是否允许从指定端点连接到目标端点。
     *
     * @param from 本方块的端点
     * @param to 目标端点
     * @return 是否允许连接
     */
    default boolean canConnectTo(WireEndpoint from, WireEndpoint to) {
        return true;
    }
}
