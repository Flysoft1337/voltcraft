package com.voltcraft.blockentity;

import com.voltcraft.block.CableBlock;
import com.voltcraft.block.TransformerBlock;
import com.voltcraft.electric.CableTier;
import com.voltcraft.electric.network.EnergyNetwork;
import com.voltcraft.electric.network.NetworkManager;
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
 * 变压器方块实体。
 *
 * 拓扑（玩家视角）：
 *   - FACING（铭牌面）：装饰，不参与电力——这里有警示牌+闪电+铭牌。
 *   - FACING.opposite()（输入面）：低压输入端，外部机器从此塞 FE。
 *   - 其余 4 面（顶/底/左/右）：高压输出端，连接同等级电缆。
 *
 * 数据流：
 *   外部 mod → inputBuffer (低压输入面 IEnergyStorage)
 *           → serverTick: 扫描 4 个输出面，对每个相邻同等级电缆网络写电压标签 + 推送电（损耗 1%）
 *
 * 损耗模型（设计文档 2.2.3）：功率守恒，FE/t 是功率单位，所以 FE 数值不变，仅扣损耗。
 */
public class TransformerBlockEntity extends BlockEntity {

    private static final String NBT_INPUT_BUFFER = "InputBuffer";

    private static final double LOSS_RATE = 0.01;

    private final CableTier outputTier;

    private final EnergyStorage inputBuffer;

    public TransformerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, CableTier outputTier) {
        super(type, pos, state);
        this.outputTier = outputTier;
        int rate = outputTier.ratedTransfer();
        // buffer 容量给予 8 倍单 tick 输出量；输出 = 4 面同时推送时合计上限
        this.inputBuffer = new EnergyStorage(rate * 8, rate * 4, 0);
    }

    public CableTier outputTier() {
        return outputTier;
    }

    /** 暴露给 IEnergyStorage Capability 的输入端。仅在 inputFace 暴露。 */
    public IEnergyStorage inputHandler() {
        return inputBuffer;
    }

    /**
     * 服务端每 tick：
     * 1. 扫描 4 个输出面（非 FACING / 非 inputFace），收集相邻同等级电缆的网络
     * 2. 对每个网络写电压标签
     * 3. 把 inputBuffer 的电按 LOSS_RATE 扣损耗后均分推入这些网络
     */
    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        Direction decor = getBlockState().getValue(TransformerBlock.FACING);
        Direction inputFace = decor.getOpposite();

        // 收集 4 个输出面相邻的网络（去重）
        java.util.List<EnergyNetwork> outputs = new java.util.ArrayList<>(4);
        java.util.Set<EnergyNetwork> seen = new java.util.HashSet<>();
        for (Direction d : Direction.values()) {
            if (d == decor || d == inputFace) continue;
            BlockPos op = getBlockPos().relative(d);
            BlockState bs = level.getBlockState(op);
            if (!(bs.getBlock() instanceof CableBlock cb) || cb.tier() != outputTier) continue;
            EnergyNetwork net = NetworkManager.get(level).networkAt(op);
            if (net == null || !seen.add(net)) continue;

            // 写电压标签（幂等）；冲突的网络跳过本次输出
            if (net.voltageTag() == null) {
                net.setVoltageTag(outputTier.voltage());
            } else if (net.voltageTag() != outputTier.voltage()) {
                continue;
            }
            outputs.add(net);
        }

        if (outputs.isEmpty()) return;

        int available = inputBuffer.getEnergyStored();
        if (available <= 0) return;

        long afterLoss = (long) (available * (1.0 - LOSS_RATE));
        if (afterLoss <= 0) return;

        // 均分给所有输出网络；任何一个吃满后多余电量在下一面继续派发
        long share = Math.max(1, afterLoss / outputs.size());
        long totalPushed = 0;
        long remaining = afterLoss;
        for (EnergyNetwork net : outputs) {
            if (remaining <= 0) break;
            long want = Math.min(share, remaining);
            long pushed = net.pushEnergy(level, want, false);
            if (pushed > 0) {
                totalPushed += pushed;
                remaining -= pushed;
            }
        }
        // 二轮：把上一轮没消化的余额尝试推给吃得下的网络
        if (remaining > 0) {
            for (EnergyNetwork net : outputs) {
                if (remaining <= 0) break;
                long pushed = net.pushEnergy(level, remaining, false);
                if (pushed > 0) {
                    totalPushed += pushed;
                    remaining -= pushed;
                }
            }
        }

        if (totalPushed <= 0) return;
        int consumed = (int) Math.min(Integer.MAX_VALUE, Math.ceil(totalPushed / (1.0 - LOSS_RATE)));
        consumed = Math.min(consumed, available);
        inputBuffer.extractEnergy(consumed, false);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NBT_INPUT_BUFFER)) {
            inputBuffer.deserializeNBT(registries, tag.get(NBT_INPUT_BUFFER));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(NBT_INPUT_BUFFER, inputBuffer.serializeNBT(registries));
    }

    /** 输入面：玩家右键放置时的反向（铭牌的反面）。外部机器从这里塞电。 */
    public Direction inputFace() {
        return getBlockState().getValue(TransformerBlock.FACING).getOpposite();
    }

    /** 仅用于 Jade 显示。装饰面方向 = FACING。 */
    public Direction decorFace() {
        return getBlockState().getValue(TransformerBlock.FACING);
    }
}
