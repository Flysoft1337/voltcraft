package com.voltcraft.blockentity;

import com.voltcraft.block.BreakerBlock;
import com.voltcraft.electric.CableTier;
import com.voltcraft.electric.protection.BreakerState;
import com.voltcraft.electric.wire.TopAnchorLayout;
import com.voltcraft.electric.wire.WireAnchor;
import com.voltcraft.electric.wire.WireAnchorOwner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * 三相空气开关（v2 顶面 6 柱）。
 *
 * 拓扑：顶面 6 柱（3 in + 3 out），FACING 仅作把手装饰。
 * 三相能量流：L_IN → L_OUT、N_IN → N_OUT、E_IN → E_OUT。
 *
 * 跳闸：
 *   * 任一相 instant 流量 > 200% rated → TRIPPED_OVERLOAD
 *   * 任一相 持续 > 120% rated 100 tick → TRIPPED_OVERLOAD
 *   * |L_flow - N_flow| > 阈值 持续 10 tick → TRIPPED_LEAKAGE（RCD）
 */
public class BreakerBlockEntity extends BlockEntity implements WireAnchorOwner {

    private static final String NBT_BUFFER_PREFIX = "Buf";
    private static final String NBT_OVERLOAD_TICKS = "OverloadTicks";

    private static final double OVERLOAD_FACTOR_HOLD = 1.20;
    private static final int OVERLOAD_HOLD_TICKS = 100;
    private static final double OVERLOAD_FACTOR_INSTANT = 2.00;

    private static final long LEAKAGE_TRIP_FE_PER_TICK = 32;
    private static final int LEAKAGE_HOLD_TICKS = 10;

    private final CableTier tier;
    private final EnergyStorage[] buffers = new EnergyStorage[TopAnchorLayout.COUNT];
    private final WireAnchor[] anchors;

    private int overloadTicks;
    private int leakageTicks;
    private long lastFlow;
    private long lastLeakage;

    public BreakerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, CableTier tier) {
        super(type, pos, state);
        this.tier = tier;
        int rate = tier.ratedTransfer();
        for (int i = 0; i < TopAnchorLayout.COUNT; i++) {
            buffers[i] = new EnergyStorage(rate * 2, rate * 2, rate * 2);
        }
        this.anchors = TopAnchorLayout.createAnchors(tier);
    }

    public CableTier tier() { return tier; }
    public long lastFlow() { return lastFlow; }
    public long lastLeakage() { return lastLeakage; }

    private BreakerState currentState() { return getBlockState().getValue(BreakerBlock.STATE); }

    /** 仅供旧 BlockState 渲染兼容；语义已退化（顶面 6 柱不分 sideA/B）。 */
    public Direction sideA() { return getBlockState().getValue(BreakerBlock.FACING).getClockWise(); }
    public Direction sideB() { return getBlockState().getValue(BreakerBlock.FACING).getCounterClockWise(); }

    @Override
    @Nullable
    public WireAnchor anchor(int index) {
        return (index < 0 || index >= TopAnchorLayout.COUNT) ? null : anchors[index];
    }

    @Override public int anchorCount() { return TopAnchorLayout.COUNT; }

    @Override
    public IEnergyStorage anchorBuffer(int index) {
        if (index < 0 || index >= TopAnchorLayout.COUNT) return null;
        if (!currentState().conducts()) return BlockedHandler.INSTANCE;
        return buffers[index];
    }

    @Override
    public Vec3 anchorWorldPos(WireAnchor anchor, BlockPos blockPos) {
        Direction facing = getBlockState().getValue(BreakerBlock.FACING);
        return TopAnchorLayout.worldPos(facing, anchor.localOffset(), blockPos);
    }

    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        BreakerState state = currentState();
        if (!state.conducts()) {
            lastFlow = 0;
            lastLeakage = 0;
            return;
        }

        long lFlow = transfer(buffers[TopAnchorLayout.L_IN], buffers[TopAnchorLayout.L_OUT]);
        long nFlow = transfer(buffers[TopAnchorLayout.N_IN], buffers[TopAnchorLayout.N_OUT]);
        long eFlow = transfer(buffers[TopAnchorLayout.E_IN], buffers[TopAnchorLayout.E_OUT]);

        lastFlow = lFlow + nFlow + eFlow;
        long maxPhase = Math.max(lFlow, Math.max(nFlow, eFlow));

        // RCD：L 与 N 流量差视作漏电
        long leakage = Math.abs(lFlow - nFlow);
        lastLeakage = leakage;

        evaluateOverload(level, maxPhase);
        evaluateLeakage(level, leakage);
    }

    private static long transfer(EnergyStorage src, EnergyStorage dst) {
        int avail = src.extractEnergy(Integer.MAX_VALUE, true);
        if (avail <= 0) return 0;
        int accepted = dst.receiveEnergy(avail, true);
        if (accepted <= 0) return 0;
        src.extractEnergy(accepted, false);
        dst.receiveEnergy(accepted, false);
        return accepted;
    }

    private void evaluateOverload(Level level, long phaseFlow) {
        long rated = tier.ratedTransfer();
        double factor = (double) phaseFlow / rated;
        if (factor > OVERLOAD_FACTOR_INSTANT) {
            trip(level, BreakerState.TRIPPED_OVERLOAD);
            return;
        }
        if (factor > OVERLOAD_FACTOR_HOLD) {
            overloadTicks++;
            if (overloadTicks >= OVERLOAD_HOLD_TICKS) {
                trip(level, BreakerState.TRIPPED_OVERLOAD);
            }
        } else if (overloadTicks > 0) {
            overloadTicks--;
        }
    }

    private void evaluateLeakage(Level level, long leakage) {
        if (leakage > LEAKAGE_TRIP_FE_PER_TICK) {
            leakageTicks++;
            if (leakageTicks >= LEAKAGE_HOLD_TICKS) {
                trip(level, BreakerState.TRIPPED_LEAKAGE);
            }
        } else if (leakageTicks > 0) {
            leakageTicks--;
        }
    }

    private void trip(Level level, BreakerState reason) {
        overloadTicks = 0;
        leakageTicks = 0;
        BlockState newState = getBlockState().setValue(BreakerBlock.STATE, reason);
        level.setBlock(getBlockPos(), newState, 3);
        for (EnergyStorage b : buffers) {
            b.extractEnergy(b.getEnergyStored(), false);
        }
        setChanged();
    }

    public void reset() {
        Level level = getLevel();
        if (level == null) return;
        BlockState newState = getBlockState().setValue(BreakerBlock.STATE, BreakerState.CLOSED);
        level.setBlock(getBlockPos(), newState, 3);
        overloadTicks = 0;
        leakageTicks = 0;
        setChanged();
    }

    public void setState(BreakerState newStateValue) {
        Level level = getLevel();
        if (level == null) return;
        level.setBlock(getBlockPos(), getBlockState().setValue(BreakerBlock.STATE, newStateValue), 3);
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < TopAnchorLayout.COUNT; i++) {
            String k = NBT_BUFFER_PREFIX + i;
            if (tag.contains(k)) buffers[i].deserializeNBT(registries, tag.get(k));
        }
        overloadTicks = tag.getInt(NBT_OVERLOAD_TICKS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < TopAnchorLayout.COUNT; i++) {
            tag.put(NBT_BUFFER_PREFIX + i, buffers[i].serializeNBT(registries));
        }
        tag.putInt(NBT_OVERLOAD_TICKS, overloadTicks);
    }

    private static final class BlockedHandler implements IEnergyStorage {
        static final BlockedHandler INSTANCE = new BlockedHandler();
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return 0; }
        @Override public int getMaxEnergyStored() { return 0; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return false; }
    }
}
