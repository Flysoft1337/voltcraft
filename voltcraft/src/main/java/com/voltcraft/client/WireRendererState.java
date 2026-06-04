package com.voltcraft.client;

import com.voltcraft.electric.WireType;
import com.voltcraft.network.WireConnectionSyncPacket.WireConnectionData;
import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 客户端线缆渲染状态。
 * 存储从服务端同步过来的连接信息和待连接状态，供 WireRenderer 使用。
 */
public final class WireRendererState {

    private static Set<WireConnectionData> connections = Collections.emptySet();

    // 待连接线缆状态（客户端）
    private static BlockPos pendingStartPos = null;
    private static WireType pendingWireType = null;

    private WireRendererState() {}

    public static void setConnections(Set<WireConnectionData> newConnections) {
        connections = Collections.unmodifiableSet(new HashSet<>(newConnections));
    }

    public static Set<WireConnectionData> getConnections() {
        return connections;
    }

    public static void setPendingWire(BlockPos startPos, WireType wireType) {
        pendingStartPos = startPos;
        pendingWireType = wireType;
    }

    public static void clearPendingWire() {
        pendingStartPos = null;
        pendingWireType = null;
    }

    public static BlockPos getPendingStartPos() {
        return pendingStartPos;
    }

    public static WireType getPendingWireType() {
        return pendingWireType;
    }

    public static boolean hasPendingWire() {
        return pendingStartPos != null && pendingWireType != null;
    }
}
