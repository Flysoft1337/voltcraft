package com.voltcraft.blockentity;

import com.voltcraft.block.BreakerBlock;
import com.voltcraft.block.CableBlock;
import com.voltcraft.electric.CableTier;
import com.voltcraft.electric.network.EnergyNetwork;
import com.voltcraft.electric.network.NetworkManager;
import com.voltcraft.electric.protection.BreakerState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * 空气开关方块实体。
 *
 * 拓扑：FACING 是把手朝向（视觉特征）。两侧水平方向是 sideA / sideB，
 * 各自接同等级电缆网络。
 *
 * 双 buffer 跨边推送：
 *   * bufferA：sideA 网络通过 capA 塞进来的电；serverTick 把它 push 到 sideB 网络
 *   * bufferB：sideB 网络通过 capB 塞进来的电；serverTick 把它 push 到 sideA 网络
 * 自循环不可能——每个 buffer 只能推给"另一边"网络。
 *
 * 跳闸阈值（设计文档 4.2）：
 * - 流量 > 200% 额定 → 立刻 TRIPPED_OVERLOAD
 * - 流量 > 120% 额定 持续 100 tick → TRIPPED_OVERLOAD
 * 任一侧网络被打 shortCircuitSource → TRIPPED_SHORT。
 */
public class BreakerBlockEntity extends BlockEntity {

    private static final String NBT_BUFFER_A = "BufferA";
    private static final String NBT_BUFFER_B = "BufferB";
    private static final String NBT_OVERLOAD_TICKS = "OverloadTicks";

    private static final double OVERLOAD_FACTOR_HOLD = 1.20;
    private static final int OVERLOAD_HOLD_TICKS = 100;
    private static final double OVERLOAD_FACTOR_INSTANT = 2.00;

    private final CableTier tier;
    private final EnergyStorage bufferA;
    private final EnergyStorage bufferB;

    private int overloadTicks;
    private long lastFlow;

    public BreakerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, CableTier tier) {
        super(type, pos, state);
        this.tier = tier;
        int rate = tier.ratedTransfer();
        // 容量 = 2×rate（过载情况下 1 tick 可瞬时塞 200%），输入/输出上限 = 2×rate
        this.bufferA = new EnergyStorage(rate * 2, rate * 2, rate * 2);
        this.bufferB = new EnergyStorage(rate * 2, rate * 2, rate * 2);
    }

    public CableTier tier() { return tier; }

    public long lastFlow() { return lastFlow; }

    private BreakerState currentState() {
        return getBlockState().getValue(BreakerBlock.STATE);
    }

    public Direction sideA() {
        return getBlockState().getValue(BreakerBlock.FACING).getClockWise();
    }
    public Direction sideB() {
        return getBlockState().getValue(BreakerBlock.FACING).getCounterClockWise();
    }

    /** sideA 面对外暴露：只 receive 进 bufferA。 */
    public IEnergyStorage handlerA() {
        if (!currentState().conducts()) return BlockedHandler.INSTANCE;
        return capA;
    }

    /** sideB 面对外暴露：只 receive 进 bufferB。 */
    public IEnergyStorage handlerB() {
        if (!currentState().conducts()) return BlockedHandler.INSTANCE;
        return capB;
    }

    private final IEnergyStorage capA = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            return bufferA.receiveEnergy(maxReceive, simulate);
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return bufferA.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return bufferA.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private final IEnergyStorage capB = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            return bufferB.receiveEnergy(maxReceive, simulate);
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return bufferB.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return bufferB.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        BreakerState state = currentState();
        if (!state.conducts()) {
            lastFlow = 0;
            return;
        }

        EnergyNetwork netA = networkOnSide(level, sideA());
        EnergyNetwork netB = networkOnSide(level, sideB());

        // 透传电压标签
        if (netA != null && netB != null) {
            if (netA.voltageTag() != null && netB.voltageTag() == null) {
                netB.setVoltageTag(netA.voltageTag());
            } else if (netB.voltageTag() != null && netA.voltageTag() == null) {
                netA.setVoltageTag(netB.voltageTag());
            }
        }

        // 短路检测
        if ((netA != null && netA.hasShortCircuit()) || (netB != null && netB.hasShortCircuit())) {
            trip(level, BreakerState.TRIPPED_SHORT);
            return;
        }

        long flow = 0;

        // bufferA → netB
        int aAvail = bufferA.getEnergyStored();
        if (aAvail > 0 && netB != null) {
            long pushed = netB.pushEnergy(level, aAvail, false);
            if (pushed > 0) {
                bufferA.extractEnergy((int) Math.min(Integer.MAX_VALUE, pushed), false);
                flow += pushed;
            }
        }

        // bufferB → netA
        int bAvail = bufferB.getEnergyStored();
        if (bAvail > 0 && netA != null) {
            long pushed = netA.pushEnergy(level, bAvail, false);
            if (pushed > 0) {
                bufferB.extractEnergy((int) Math.min(Integer.MAX_VALUE, pushed), false);
                flow += pushed;
            }
        }

        lastFlow = flow;
        evaluateOverload(level, flow);
    }

    @Nullable
    private EnergyNetwork networkOnSide(Level level, Direction d) {
        BlockPos pos = getBlockPos().relative(d);
        BlockState bs = level.getBlockState(pos);
        if (bs.getBlock() instanceof CableBlock cb && cb.tier() == tier) {
            return NetworkManager.get(level).networkAt(pos);
        }
        return null;
    }

    private void evaluateOverload(Level level, long flow) {
        long rated = tier.ratedTransfer();
        double factor = (double) flow / rated;

        if (factor > OVERLOAD_FACTOR_INSTANT) {
            trip(level, BreakerState.TRIPPED_OVERLOAD);
            return;
        }
        if (factor > OVERLOAD_FACTOR_HOLD) {
            overloadTicks++;
            if (overloadTicks >= OVERLOAD_HOLD_TICKS) {
                trip(level, BreakerState.TRIPPED_OVERLOAD);
            }
        } else {
            decayOverload();
        }
    }

    private void decayOverload() {
        if (overloadTicks > 0) overloadTicks--;
    }

    private void trip(Level level, BreakerState reason) {
        overloadTicks = 0;
        BlockState newState = getBlockState().setValue(BreakerBlock.STATE, reason);
        level.setBlock(getBlockPos(), newState, 3);
        bufferA.extractEnergy(bufferA.getEnergyStored(), false);
        bufferB.extractEnergy(bufferB.getEnergyStored(), false);
        setChanged();
    }

    public void reset() {
        Level level = getLevel();
        if (level == null) return;
        BlockState newState = getBlockState().setValue(BreakerBlock.STATE, BreakerState.CLOSED);
        level.setBlock(getBlockPos(), newState, 3);
        overloadTicks = 0;
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
        if (tag.contains(NBT_BUFFER_A)) bufferA.deserializeNBT(registries, tag.get(NBT_BUFFER_A));
        if (tag.contains(NBT_BUFFER_B)) bufferB.deserializeNBT(registries, tag.get(NBT_BUFFER_B));
        overloadTicks = tag.getInt(NBT_OVERLOAD_TICKS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(NBT_BUFFER_A, bufferA.serializeNBT(registries));
        tag.put(NBT_BUFFER_B, bufferB.serializeNBT(registries));
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
