package com.voltcraft.electric.wire;

import com.voltcraft.VoltCraft;
import com.voltcraft.electric.WireType;
import com.voltcraft.network.WireConnectionSyncPacket;
import com.voltcraft.network.WireConnectionSyncPacket.WireConnectionData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线缆网络管理器。
 * 管理单个 Level 中的所有线缆连接和网络。
 */
public final class WireNetworkManager {

    private static final Map<ResourceKey<Level>, WireNetworkManager> INSTANCES = new ConcurrentHashMap<>();

    /**
     * 获取或创建指定 Level 的管理器。仅服务端使用。
     */
    public static WireNetworkManager get(LevelAccessor level) {
        if (!(level instanceof Level lvl) || lvl.isClientSide) {
            throw new IllegalStateException("WireNetworkManager is server-side only");
        }
        return INSTANCES.computeIfAbsent(lvl.dimension(), k -> {
            WireNetworkManager manager = new WireNetworkManager();
            if (lvl instanceof ServerLevel serverLevel) {
                manager.load(serverLevel.getDataStorage().computeIfAbsent(
                        WireNetworkSavedData.FACTORY,
                        WireNetworkSavedData.ID
                ).connections());
            }
            return manager;
        });
    }

    /**
     * 世界卸载时调用，丢弃数据。
     */
    public static void onLevelUnload(Level level) {
        if (!level.isClientSide) {
            INSTANCES.remove(level.dimension());
        }
    }

    private final Map<BlockPos, Set<WireNetwork>> networksByEndpoint = new HashMap<>();
    private final Set<WireConnection> allConnections = new HashSet<>();
    private final Set<WireNetwork> allNetworks = new HashSet<>();

    private WireNetworkManager() {}

    private void load(Set<WireConnection> connections) {
        for (WireConnection connection : connections) {
            addConnectionInternal(connection);
        }
    }

    /**
     * 添加一条连接。
     *
     * @param level 世界
     * @param start 起始端点
     * @param end 结束端点
     * @param wireType 线缆类型
     * @return 建立的连接，如果失败返回 null
     */
    @Nullable
    public WireConnection addConnection(Level level, WireEndpoint start, WireEndpoint end, WireType wireType) {
        // 验证距离
        double distance = Math.sqrt(start.pos().distSqr(end.pos()));
        if (distance > wireType.maxDistance()) {
            VoltCraft.LOGGER.debug("Connection rejected: distance {} exceeds max {}", distance, wireType.maxDistance());
            return null;
        }

        // 检查是否已存在相同连接
        WireConnection newConn = new WireConnection(start, end, wireType);
        if (allConnections.contains(newConn)) {
            VoltCraft.LOGGER.debug("Connection already exists: {}", newConn);
            return null;
        }

        addConnectionInternal(newConn);
        markSavedDataDirty(level);

        VoltCraft.LOGGER.debug("Connection added: {}", newConn);

        syncToClients(level);

        return newConn;
    }

    private void addConnectionInternal(WireConnection connection) {
        WireNetwork network = findOrCreateNetwork(connection.start(), connection.end(), connection.wireType());
        network.addConnection(connection);
        allConnections.add(connection);
        addToEndpointIndex(connection.start().pos(), network);
        addToEndpointIndex(connection.end().pos(), network);
    }

    /**
     * 移除一条连接。
     *
     * @param connection 要移除的连接
     */
    public void removeConnection(WireConnection connection) {
        if (!allConnections.remove(connection)) {
            return;
        }

        // 找到所属网络
        WireNetwork network = findNetworkForConnection(connection);
        if (network == null) {
            return;
        }

        // 从网络移除
        network.removeConnection(connection);

        // 从端点索引移除
        removeFromEndpointIndex(connection.start().pos(), network);
        removeFromEndpointIndex(connection.end().pos(), network);

        // 如果网络为空，移除
        if (network.size() == 0) {
            allNetworks.remove(network);
            VoltCraft.LOGGER.debug("Network emptied: {}", network.id());
            return;
        }

        // 检查网络是否分裂
        checkNetworkSplit(network);

        VoltCraft.LOGGER.debug("Connection removed: {}", connection);
    }

    /**
     * 移除指定位置的所有连接。
     *
     * @param pos 要移除的位置
     */
    public void removeConnectionsAt(BlockPos pos) {
        removeConnectionsAt(pos, null);
    }

    /**
     * 移除指定位置的所有连接。
     *
     * @param pos 要移除的位置
     * @param level 世界（用于同步，可为 null）
     */
    public void removeConnectionsAt(BlockPos pos, @Nullable Level level) {
        Set<WireConnection> toRemove = new HashSet<>();
        for (WireConnection conn : allConnections) {
            if (conn.contains(pos)) {
                toRemove.add(conn);
            }
        }

        for (WireConnection conn : toRemove) {
            removeConnection(conn);
        }

        if (!toRemove.isEmpty() && level != null) {
            markSavedDataDirty(level);
            syncToClients(level);
        }
    }

    /**
     * 查找或创建网络。
     */
    private WireNetwork findOrCreateNetwork(WireEndpoint start, WireEndpoint end, WireType wireType) {
        // 查找起始端点所在的网络
        Set<WireNetwork> startNets = networksByEndpoint.getOrDefault(start.pos(), Set.of());
        // 查找结束端点所在的网络
        Set<WireNetwork> endNets = networksByEndpoint.getOrDefault(end.pos(), Set.of());

        // 收集所有相关网络
        Set<WireNetwork> allRelated = new HashSet<>();
        allRelated.addAll(startNets);
        allRelated.addAll(endNets);

        // 过滤出相同类型的网络
        List<WireNetwork> sameType = allRelated.stream()
                .filter(n -> n.wireType() == wireType)
                .toList();

        if (sameType.isEmpty()) {
            // 创建新网络
            WireNetwork newNet = new WireNetwork(wireType);
            allNetworks.add(newNet);
            return newNet;
        }

        // 合并网络
        WireNetwork primary = sameType.get(0);
        for (int i = 1; i < sameType.size(); i++) {
            WireNetwork other = sameType.get(i);
            mergeNetworks(primary, other);
        }

        return primary;
    }

    /**
     * 合并两个网络。
     */
    private void mergeNetworks(WireNetwork target, WireNetwork source) {
        // 复制连接
        for (WireConnection conn : source.connections()) {
            target.addConnection(conn);
            // 更新端点索引
            addToEndpointIndex(conn.start().pos(), target);
            addToEndpointIndex(conn.end().pos(), target);
        }

        // 电压标签合并
        if (target.voltageTag() == null && source.voltageTag() != null) {
            target.setVoltageTag(source.voltageTag());
        }

        // 移除源网络
        allNetworks.remove(source);

        VoltCraft.LOGGER.debug("Network merged: {} -> {}", source.id(), target.id());
    }

    /**
     * 检查网络是否分裂。
     */
    private void checkNetworkSplit(WireNetwork network) {
        // 使用 BFS 找连通分量
        Set<BlockPos> visited = new HashSet<>();
        List<Set<BlockPos>> components = new ArrayList<>();

        for (BlockPos endpoint : network.endpoints()) {
            if (visited.contains(endpoint)) continue;
            Set<BlockPos> component = bfs(endpoint, network, visited);
            if (!component.isEmpty()) {
                components.add(component);
            }
        }

        if (components.size() <= 1) {
            return; // 未分裂
        }

        // 分裂：保留第一个组件在原网络，其余创建新网络
        Set<BlockPos> keep = components.get(0);
        Set<BlockPos> toSplit = new HashSet<>(network.endpoints());
        toSplit.removeAll(keep);

        // 创建新网络
        WireNetwork newNet = new WireNetwork(network.wireType());
        allNetworks.add(newNet);

        // 移动连接到新网络
        Set<WireConnection> toMove = new HashSet<>();
        for (WireConnection conn : network.connections()) {
            if (toSplit.contains(conn.start().pos()) || toSplit.contains(conn.end().pos())) {
                toMove.add(conn);
            }
        }

        for (WireConnection conn : toMove) {
            network.removeConnection(conn);
            newNet.addConnection(conn);
            // 更新端点索引
            removeFromEndpointIndex(conn.start().pos(), network);
            removeFromEndpointIndex(conn.end().pos(), network);
            addToEndpointIndex(conn.start().pos(), newNet);
            addToEndpointIndex(conn.end().pos(), newNet);
        }

        VoltCraft.LOGGER.debug("Network split: {} -> {} + {}", network.id(), network.id(), newNet.id());
    }

    /**
     * BFS 找连通分量。
     */
    private Set<BlockPos> bfs(BlockPos start, WireNetwork network, Set<BlockPos> visited) {
        Set<BlockPos> component = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            if (!network.contains(cur)) continue;
            component.add(cur);

            // 查找与当前端点相连的所有端点
            for (WireConnection conn : network.connections()) {
                BlockPos next = null;
                if (conn.start().pos().equals(cur)) {
                    next = conn.end().pos();
                } else if (conn.end().pos().equals(cur)) {
                    next = conn.start().pos();
                }

                if (next != null && !visited.contains(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }

        return component;
    }

    /**
     * 查找连接所属的网络。
     */
    @Nullable
    private WireNetwork findNetworkForConnection(WireConnection connection) {
        Set<WireNetwork> nets = networksByEndpoint.getOrDefault(connection.start().pos(), Set.of());
        for (WireNetwork net : nets) {
            if (net.connections().contains(connection)) {
                return net;
            }
        }
        return null;
    }

    /**
     * 添加端点到网络索引。
     */
    private void addToEndpointIndex(BlockPos pos, WireNetwork network) {
        networksByEndpoint.computeIfAbsent(pos.immutable(), k -> new HashSet<>()).add(network);
    }

    /**
     * 从端点索引移除网络。
     */
    private void removeFromEndpointIndex(BlockPos pos, WireNetwork network) {
        Set<WireNetwork> nets = networksByEndpoint.get(pos);
        if (nets != null) {
            nets.remove(network);
            if (nets.isEmpty()) {
                networksByEndpoint.remove(pos);
            }
        }
    }

    /**
     * 获取指定位置的网络。
     */
    @Nullable
    public WireNetwork networkAt(BlockPos pos) {
        Set<WireNetwork> nets = networksByEndpoint.get(pos);
        if (nets == null || nets.isEmpty()) {
            return null;
        }
        return nets.iterator().next();
    }

    /**
     * 获取所有不重复的网络。
     */
    public Set<WireNetwork> distinctNetworks() {
        return Collections.unmodifiableSet(allNetworks);
    }

    /**
     * 服务端 tick 末调用：驱动所有网络分发能量。
     */
    public void tickAll(Level level) {
        for (WireNetwork net : allNetworks) {
            net.distributeTick(level);
        }
    }

    /**
     * 获取指定位置的所有连接。
     */
    public Set<WireConnection> getConnectionsAt(BlockPos pos) {
        Set<WireConnection> result = new HashSet<>();
        for (WireConnection conn : allConnections) {
            if (conn.contains(pos)) {
                result.add(conn);
            }
        }
        return result;
    }

    /**
     * 获取所有连接。
     */
    public Set<WireConnection> allConnections() {
        return Collections.unmodifiableSet(allConnections);
    }

    /**
     * 获取跟踪的端点数量。
     */
    public int trackedEndpointCount() {
        return networksByEndpoint.size();
    }

    /**
     * 获取不同网络的数量。
     */
    public int distinctNetworkCount() {
        return allNetworks.size();
    }

    public void syncToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildSyncPacket());
    }

    private void markSavedDataDirty(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getDataStorage()
                    .computeIfAbsent(WireNetworkSavedData.FACTORY, WireNetworkSavedData.ID)
                    .replaceConnections(allConnections);
        }
    }

    /**
     * 同步所有连接到客户端。
     */
    private void syncToClients(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        WireConnectionSyncPacket packet = buildSyncPacket();
        for (ServerPlayer player : serverLevel.players()) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }

    private WireConnectionSyncPacket buildSyncPacket() {
        Set<WireConnectionData> data = new HashSet<>();
        for (WireConnection conn : allConnections) {
            data.add(new WireConnectionData(
                    conn.start().pos(),
                    conn.start().endpointIndex(),
                    conn.start().phase(),
                    conn.end().pos(),
                    conn.end().endpointIndex(),
                    conn.end().phase(),
                    conn.wireType()
            ));
        }
        return new WireConnectionSyncPacket(data);
    }
}
