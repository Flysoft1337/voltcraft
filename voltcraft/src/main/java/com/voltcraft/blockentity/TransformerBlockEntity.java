package com.voltcraft.blockentity;

import com.voltcraft.block.TransformerBlock;
import com.voltcraft.electric.CableTier;
import com.voltcraft.electric.Phase;
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
 * 升压变压器（三相重构后）。
 *
 * 拓扑：
 *   * FACING（铭牌面）：装饰
 *   * FACING.opposite()（输入面）：低压侧 IEnergyStorage，外部 mod 塞 FE
 *   * UP / FACING.clockWise / FACING.counterClockWise：三个输出接线柱（L/N/E）
 *   * DOWN：底座，无功能
 *
 * 三个输出 anchor 各自持有独立 buffer。serverTick 把 inputBuffer 的电按 1% 损耗扣除后，
 * 平分塞进 L 和 N anchor buffer（E 仅承载漏电流，由 RCD 注入）。
 * 软线 entity tick 时从 anchor buffer 抽电送到对端。
 */
public class TransformerBlockEntity extends BlockEntity implements WireAnchorOwner {

    private static final String NBT_INPUT_BUFFER = "InputBuffer";
    private static final String NBT_BUFFER_L = "BufferL";
    private static final String NBT_BUFFER_N = "BufferN";
    private static final String NBT_BUFFER_E = "BufferE";

    private static final double LOSS_RATE = 0.01;

    /** anchor index 约定。L/N/E 按 1/2/3 排列，0 留给将来可能的低压侧 anchor 扩展。 */
    public static final int ANCHOR_L = 0;
    public static final int ANCHOR_N = 1;
    public static final int ANCHOR_E = 2;

    private final CableTier outputTier;

    private final EnergyStorage inputBuffer;
    private final EnergyStorage bufferL;
    private final EnergyStorage bufferN;
    private final EnergyStorage bufferE;

    private final WireAnchor anchorL;
    private final WireAnchor anchorN;
    private final WireAnchor anchorE;

    public TransformerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, CableTier outputTier) {
        super(type, pos, state);
        this.outputTier = outputTier;
        int rate = outputTier.ratedTransfer();
        this.inputBuffer = new EnergyStorage(rate * 8, rate * 4, 0);
        // 每相输出端 buffer：容量 = 单 tick 额定流量；输入/输出限制同
        this.bufferL = new EnergyStorage(rate, rate, rate);
        this.bufferN = new EnergyStorage(rate, rate, rate);
        this.bufferE = new EnergyStorage(rate, rate, rate);
        // anchor 本地坐标：方块顶面三个柱子
        // L 在右上 (0.75, 1.05, 0.5)、N 在左上 (0.25, 1.05, 0.5)、E 在中后 (0.5, 1.05, 0.85)
        this.anchorL = new WireAnchor(ANCHOR_L, Phase.LIVE, new Vec3(0.75, 1.05, 0.5));
        this.anchorN = new WireAnchor(ANCHOR_N, Phase.NEUTRAL, new Vec3(0.25, 1.05, 0.5));
        this.anchorE = new WireAnchor(ANCHOR_E, Phase.EARTH, new Vec3(0.5, 1.05, 0.85));
    }

    public CableTier outputTier() { return outputTier; }

    public IEnergyStorage inputHandler() { return inputBuffer; }

    /** 暴露给软线 entity 的 anchor buffer 接口。 */
    public IEnergyStorage anchorBuffer(int anchorIndex) {
        return switch (anchorIndex) {
            case ANCHOR_L -> bufferL;
            case ANCHOR_N -> bufferN;
            case ANCHOR_E -> bufferE;
            default -> null;
        };
    }

    @Override
    @Nullable
    public WireAnchor anchor(int index) {
        return switch (index) {
            case ANCHOR_L -> anchorL;
            case ANCHOR_N -> anchorN;
            case ANCHOR_E -> anchorE;
            default -> null;
        };
    }

    @Override
    public int anchorCount() { return 3; }

    @Override
    public Vec3 anchorWorldPos(WireAnchor anchor, BlockPos blockPos) {
        // 顶面三个柱子按 FACING 旋转：水平面内 (x', z') = R(decor)·(localOffset.x-0.5, localOffset.z-0.5) + 0.5
        Direction decor = getBlockState().getValue(TransformerBlock.FACING);
        Vec3 lo = anchor.localOffset();
        double dx = lo.x - 0.5;
        double dz = lo.z - 0.5;
        double rx, rz;
        switch (decor) {
            case NORTH -> { rx = dx; rz = dz; }
            case SOUTH -> { rx = -dx; rz = -dz; }
            case EAST  -> { rx = -dz; rz = dx; }
            case WEST  -> { rx = dz; rz = -dx; }
            default    -> { rx = dx; rz = dz; }
        }
        return new Vec3(blockPos.getX() + 0.5 + rx, blockPos.getY() + lo.y, blockPos.getZ() + 0.5 + rz);
    }

    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        int available = inputBuffer.getEnergyStored();
        if (available <= 0) return;

        // 损耗后平分给 L 和 N（E 不主动分配）
        long afterLoss = (long) (available * (1.0 - LOSS_RATE));
        if (afterLoss <= 0) return;

        long perPhase = afterLoss / 2;
        int pushedL = bufferL.receiveEnergy((int) Math.min(Integer.MAX_VALUE, perPhase), false);
        int pushedN = bufferN.receiveEnergy((int) Math.min(Integer.MAX_VALUE, perPhase), false);

        long totalPushed = (long) pushedL + pushedN;
        if (totalPushed <= 0) return;

        int consumed = (int) Math.min(Integer.MAX_VALUE, Math.ceil(totalPushed / (1.0 - LOSS_RATE)));
        consumed = Math.min(consumed, available);
        inputBuffer.extractEnergy(consumed, false);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NBT_INPUT_BUFFER)) inputBuffer.deserializeNBT(registries, tag.get(NBT_INPUT_BUFFER));
        if (tag.contains(NBT_BUFFER_L)) bufferL.deserializeNBT(registries, tag.get(NBT_BUFFER_L));
        if (tag.contains(NBT_BUFFER_N)) bufferN.deserializeNBT(registries, tag.get(NBT_BUFFER_N));
        if (tag.contains(NBT_BUFFER_E)) bufferE.deserializeNBT(registries, tag.get(NBT_BUFFER_E));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(NBT_INPUT_BUFFER, inputBuffer.serializeNBT(registries));
        tag.put(NBT_BUFFER_L, bufferL.serializeNBT(registries));
        tag.put(NBT_BUFFER_N, bufferN.serializeNBT(registries));
        tag.put(NBT_BUFFER_E, bufferE.serializeNBT(registries));
    }

    /** 输入面：FACING 反向。外部机器从这里塞电。 */
    public Direction inputFace() {
        return getBlockState().getValue(TransformerBlock.FACING).getOpposite();
    }

    /** 装饰面方向 = FACING。仅 Jade 用。 */
    public Direction decorFace() {
        return getBlockState().getValue(TransformerBlock.FACING);
    }
}
