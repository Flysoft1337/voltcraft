package com.voltcraft.blockentity;

import com.voltcraft.block.CableBlock;
import com.voltcraft.block.TerminalBlock;
import com.voltcraft.electric.CableTier;
import com.voltcraft.electric.network.EnergyNetwork;
import com.voltcraft.electric.network.NetworkManager;
import com.voltcraft.electric.protection.WiringState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 接线端子方块实体。
 *
 * 双 buffer 设计避免自循环：
 *   * outgoing：外部机器 receive 进来的，serverTick 把它 push 到电缆网络
 *   * incoming：serverTick 从电缆网络 pull 进来的，外部机器从机器面 extract
 *
 * 机器面对外暴露的 IEnergyStorage 把 receive 路由到 outgoing、extract 路由到 incoming，
 * 玩家视角是单一接线端子，本质是双向桥。
 *
 * 网络侧不暴露 capability —— 否则 distributeTick 会把端子识别为消费者，
 * 把电塞回端子 buffer，再被 serverTick push 回同一网络（自循环、零真实流量）。
 *
 * 短路状态：machineHandler 拒收，电缆网络被打 shortCircuitSource。
 */
public class TerminalBlockEntity extends BlockEntity {

    private static final String NBT_OUTGOING = "Outgoing";
    private static final String NBT_INCOMING = "Incoming";

    private final CableTier tier;

    /** 机器→网络方向的缓存。serverTick 推空。 */
    private final EnergyStorage outgoing;

    /** 网络→机器方向的缓存。serverTick 主动从网络拉电填充；外部机器 extract 取走。 */
    private final EnergyStorage incoming;

    /** 端子总通流量（push + pull），用于 Jade。 */
    private long lastFlow;

    public TerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, CableTier tier) {
        super(type, pos, state);
        this.tier = tier;
        int rate = tier.ratedTransfer();
        // 每个方向只缓冲 1 tick 的额定流量，端子是即时桥接，不当储能
        this.outgoing = new EnergyStorage(rate, rate, rate);
        this.incoming = new EnergyStorage(rate, rate, rate);
    }

    public CableTier tier() { return tier; }

    public long lastFlow() { return lastFlow; }

    public WiringState wiring() {
        return getBlockState().getValue(TerminalBlock.WIRING);
    }

    public Direction machineFace() {
        return getBlockState().getValue(TerminalBlock.FACING);
    }

    public boolean isCableFace(Direction d) {
        return d != machineFace();
    }

    /** 单一对外句柄；receive→outgoing，extract→incoming。 */
    public IEnergyStorage machineHandler() {
        if (!wiring().conducts()) return BlockedHandler.INSTANCE;
        return splitHandler;
    }

    /** 暴露给电缆侧（5 个非机器面）：仅 receive 到 incoming，绝不 extract。
     *  这是反向通路：变压器→电缆网络→端子 incoming→外部机器 extract。
     *  必须暴露此 cap，否则 distributeTick 反压扫描看不到端子，认为下游零空间，整网零流量。 */
    public IEnergyStorage cableHandler() {
        if (!wiring().conducts()) return BlockedHandler.INSTANCE;
        return cableSideHandler;
    }

    /** 用于 Jade 显示一个综合的 "buffer 占用率"。 */
    public int bufferStored() { return outgoing.getEnergyStored() + incoming.getEnergyStored(); }
    public int bufferCapacity() { return outgoing.getMaxEnergyStored() + incoming.getMaxEnergyStored(); }

    private final IEnergyStorage splitHandler = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            return outgoing.receiveEnergy(maxReceive, simulate);
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            return incoming.extractEnergy(maxExtract, simulate);
        }
        @Override public int getEnergyStored() { return outgoing.getEnergyStored() + incoming.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return outgoing.getMaxEnergyStored() + incoming.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    };

    /** 电缆侧 handler：只往 incoming 收电，不让外面抽。绝对不会形成自循环
     *  ——因为 incoming 的电只能由 splitHandler.extract 流出（即外部机器拿走），
     *  没有任何代码路径会把 incoming 推回网络。 */
    private final IEnergyStorage cableSideHandler = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            return incoming.receiveEnergy(maxReceive, simulate);
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return incoming.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return incoming.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        WiringState wiring = wiring();

        // 找到电缆侧网络：扫描 5 个非机器面，取第一根同等级电缆
        EnergyNetwork net = null;
        for (Direction d : Direction.values()) {
            if (d == machineFace()) continue;
            BlockPos cablePos = getBlockPos().relative(d);
            BlockState cs = level.getBlockState(cablePos);
            if (cs.getBlock() instanceof CableBlock cb && cb.tier() == tier) {
                net = NetworkManager.get(level).networkAt(cablePos);
                if (net != null) break;
            }
        }

        // 短路：写标志，清空两个 buffer
        if (wiring.isShort()) {
            if (net != null) net.reportShortCircuit(getBlockPos());
            outgoing.extractEnergy(outgoing.getEnergyStored(), false);
            incoming.extractEnergy(incoming.getEnergyStored(), false);
            lastFlow = 0;
            return;
        }

        if (net == null) {
            lastFlow = 0;
            return;
        }

        long flow = 0;

        // 方向 A：outgoing（外部机器塞的）→ 网络
        int outAvail = outgoing.getEnergyStored();
        if (outAvail > 0) {
            long pushed = net.pushEnergy(level, outAvail, false);
            if (pushed > 0) {
                outgoing.extractEnergy((int) Math.min(Integer.MAX_VALUE, pushed), false);
                flow += pushed;
            }
        }

        // 方向 B：incoming → 机器面邻居（外部机器）
        // 多数耗电方块是被动接收的，不会主动从邻居 extract，所以端子主动 push。
        int inAvail = incoming.getEnergyStored();
        if (inAvail > 0) {
            BlockPos machinePos = getBlockPos().relative(machineFace());
            BlockEntity mbe = level.getBlockEntity(machinePos);
            if (mbe != null) {
                IEnergyStorage sink = level.getCapability(
                        Capabilities.EnergyStorage.BLOCK,
                        machinePos,
                        level.getBlockState(machinePos),
                        mbe,
                        machineFace().getOpposite()
                );
                if (sink != null && sink.canReceive()) {
                    int pushed = sink.receiveEnergy(inAvail, false);
                    if (pushed > 0) {
                        incoming.extractEnergy(pushed, false);
                        flow += pushed;
                    }
                }
            }
        }

        lastFlow = flow;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NBT_OUTGOING)) outgoing.deserializeNBT(registries, tag.get(NBT_OUTGOING));
        if (tag.contains(NBT_INCOMING)) incoming.deserializeNBT(registries, tag.get(NBT_INCOMING));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(NBT_OUTGOING, outgoing.serializeNBT(registries));
        tag.put(NBT_INCOMING, incoming.serializeNBT(registries));
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
