package com.voltcraft.electric.wire;

import com.voltcraft.electric.CableTier;
import com.voltcraft.electric.Phase;
import com.voltcraft.electric.VoltageTier;
import com.voltcraft.electric.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 线缆网络。
 * 管理一组通过线缆连接的端点，以及能量传输逻辑。
 */
public class WireNetwork {

    private final UUID id = UUID.randomUUID();
    private final WireType wireType;
    private final Phase phase;
    private final Set<WireConnection> connections = new HashSet<>();
    private final Set<BlockPos> endpoints = new HashSet<>();

    @Nullable
    private VoltageTier voltageTag;

    /** 本 tick 由变压器/储能等推入的 FE，等待 tick 末分发。 */
    private long pendingInput;

    /** 上一 tick 实际通过网络的 FE 流量。用于电流推算。 */
    private long lastFlow;

    /**
     * 短路源位置。非 null 表示本网络上有端子检测到短路。
     */
    @Nullable
    private BlockPos shortCircuitSource;
    private int shortCircuitStaleTicks;
    private static final int SHORT_STALE_THRESHOLD = 20; // 1s 没续写就清空

    public WireNetwork(WireType wireType, Phase phase) {
        this.wireType = wireType;
        this.phase = phase;
    }

    public UUID id() {
        return id;
    }

    public WireType wireType() {
        return wireType;
    }

    public Phase phase() {
        return phase;
    }

    public CableTier cableTier() {
        return wireType.tier();
    }

    public Set<WireConnection> connections() {
        return Collections.unmodifiableSet(connections);
    }

    public Set<BlockPos> endpoints() {
        return Collections.unmodifiableSet(endpoints);
    }

    public int size() {
        return endpoints.size();
    }

    public boolean contains(BlockPos pos) {
        return endpoints.contains(pos);
    }

    /**
     * 添加一个连接到网络。
     *
     * @param connection 要添加的连接
     */
    public void addConnection(WireConnection connection) {
        if (connection.start().phase() != phase || connection.end().phase() != phase) {
            throw new IllegalArgumentException("Connection phase does not match network phase");
        }
        connections.add(connection);
        endpoints.add(connection.start().pos().immutable());
        endpoints.add(connection.end().pos().immutable());
    }

    /**
     * 移除一个连接。
     *
     * @param connection 要移除的连接
     */
    public void removeConnection(WireConnection connection) {
        connections.remove(connection);
        // 重新计算端点
        recomputeEndpoints();
    }

    /**
     * 重新计算端点集合。
     */
    private void recomputeEndpoints() {
        endpoints.clear();
        for (WireConnection conn : connections) {
            endpoints.add(conn.start().pos().immutable());
            endpoints.add(conn.end().pos().immutable());
        }
    }

    @Nullable
    public VoltageTier voltageTag() {
        return voltageTag;
    }

    public void setVoltageTag(@Nullable VoltageTier voltage) {
        if (voltage != null && voltage != wireType.voltage()) {
            throw new IllegalArgumentException(
                    "Voltage " + voltage + " incompatible with " + wireType);
        }
        this.voltageTag = voltage;
    }

    /**
     * 端子在 serverTick 中调用，标记本网络上有短路。
     */
    public void reportShortCircuit(BlockPos source) {
        this.shortCircuitSource = source.immutable();
        this.shortCircuitStaleTicks = 0;
    }

    @Nullable
    public BlockPos shortCircuitSource() {
        return shortCircuitSource;
    }

    public boolean hasShortCircuit() {
        return shortCircuitSource != null;
    }

    public long lastFlow() {
        return lastFlow;
    }

    /**
     * 自动推算电流（A）= 上 tick FE/t ÷ 电压（V）。
     */
    public double currentAmps() {
        if (voltageTag == null || voltageTag.volts() == 0) return 0.0;
        return (double) lastFlow / voltageTag.volts();
    }

    /**
     * 由变压器/储能调用，向网络注入 FE。
     *
     * @param amount FE 数量
     * @param simulate 是否模拟
     * @return 实际接受的 FE 数量
     */
    public long pushEnergy(long amount, boolean simulate) {
        if (amount <= 0) return 0;
        long capacity = (long) wireType.tier().ratedTransfer() - pendingInput;
        long accepted = Math.max(0, Math.min(amount, capacity));
        if (!simulate) pendingInput += accepted;
        return accepted;
    }

    /**
     * 服务端 tick 末调用：分发本 tick 的 pending 输入。
     *
     * @param level 世界
     */
    public void distributeTick(Level level) {
        // 短路标志衰减
        if (shortCircuitSource != null) {
            shortCircuitStaleTicks++;
            if (shortCircuitStaleTicks >= SHORT_STALE_THRESHOLD) {
                shortCircuitSource = null;
                shortCircuitStaleTicks = 0;
            }
        }
        // 短路时整网停止传输
        if (shortCircuitSource != null) {
            pendingInput = 0;
            lastFlow = 0;
            return;
        }

        if (voltageTag == null) {
            pendingInput = 0;
            lastFlow = 0;
            return;
        }

        Endpoints ep = collectEndpoints(level);
        long ratedCap = wireType.tier().ratedTransfer();
        long budget = Math.min(pendingInput, ratedCap);
        pendingInput = 0;

        if (budget <= 0 || ep.consumers.isEmpty()) {
            lastFlow = 0;
            return;
        }

        // 分发能量给消费者
        long remaining = budget;
        long delivered = 0;
        boolean progress = true;
        while (remaining > 0 && progress) {
            progress = false;
            long perConsumer = Math.max(1, remaining / ep.consumers.size());
            for (IEnergyStorage c : ep.consumers) {
                if (remaining <= 0) break;
                int give = (int) Math.min(perConsumer, Math.min(Integer.MAX_VALUE, remaining));
                int accepted = c.receiveEnergy(give, false);
                if (accepted > 0) {
                    remaining -= accepted;
                    delivered += accepted;
                    progress = true;
                }
            }
        }

        lastFlow = delivered;
    }

    /**
     * 扫描所有端点相邻的 IEnergyStorage，分类为生产者/消费者。
     */
    private Endpoints collectEndpoints(Level level) {
        List<IEnergyStorage> producers = new ArrayList<>();
        List<IEnergyStorage> consumers = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();

        for (BlockPos endpoint : endpoints) {
            for (Direction d : Direction.values()) {
                BlockPos neighbor = endpoint.relative(d);
                if (endpoints.contains(neighbor)) continue; // 同网络端点
                if (!seen.add(neighbor)) continue; // 去重

                BlockEntity be = level.getBlockEntity(neighbor);
                if (be == null) continue;

                IEnergyStorage es = level.getCapability(
                        Capabilities.EnergyStorage.BLOCK,
                        neighbor,
                        be.getBlockState(),
                        be,
                        d.getOpposite()
                );
                if (es == null) continue;

                if (es.canReceive()) {
                    consumers.add(es);
                } else if (es.canExtract()) {
                    producers.add(es);
                }
            }
        }

        return new Endpoints(producers, consumers);
    }

    private record Endpoints(List<IEnergyStorage> producers, List<IEnergyStorage> consumers) {}

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WireNetwork other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "WireNetwork{id=" + id + ", type=" + wireType + ", phase=" + phase
                + ", voltage=" + voltageTag + ", endpoints=" + endpoints.size()
                + ", connections=" + connections.size() + "}";
    }
}
