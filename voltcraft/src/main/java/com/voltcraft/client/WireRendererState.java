package com.voltcraft.client;

import com.voltcraft.network.WireConnectionSyncPacket.WireConnectionData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 客户端线缆渲染状态。
 * 存储从服务端同步过来的连接信息，供 WireRenderer 使用。
 */
public final class WireRendererState {

    private static Set<WireConnectionData> connections = Collections.emptySet();

    private WireRendererState() {}

    /**
     * 更新连接数据（由网络包处理调用）。
     */
    public static void setConnections(Set<WireConnectionData> newConnections) {
        connections = Collections.unmodifiableSet(new HashSet<>(newConnections));
    }

    /**
     * 获取当前所有连接（供渲染器使用）。
     */
    public static Set<WireConnectionData> getConnections() {
        return connections;
    }
}
