package com.voltcraft.blockentity;

import com.voltcraft.electric.CableTier;
import com.voltcraft.electric.VoltageTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 电缆方块实体。
 *
 * 当前阶段持有：
 * - cableTier：从方块继承的等级（不存盘，因为 Block 决定了它）
 * - voltageTag：当前线路电压标签，由变压器写入；null 表示线路尚未通电（未被任何变压器加入过）
 *
 * 后续阶段会扩展：
 * - 引用所属 EnergyNetwork
 * - 连接方向位图（用于渲染和扫描）
 */
public class CableBlockEntity extends BlockEntity {

    private static final String NBT_VOLTAGE_TAG = "VoltageTag";

    private final CableTier cableTier;

    @Nullable
    private VoltageTier voltageTag;

    public CableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, CableTier cableTier) {
        super(type, pos, state);
        this.cableTier = cableTier;
    }

    public CableTier cableTier() {
        return cableTier;
    }

    @Nullable
    public VoltageTier voltageTag() {
        return voltageTag;
    }

    public void setVoltageTag(@Nullable VoltageTier voltageTag) {
        if (voltageTag != null && voltageTag != cableTier.voltage()) {
            throw new IllegalArgumentException(
                    "Voltage tag " + voltageTag + " incompatible with cable tier " + cableTier);
        }
        this.voltageTag = voltageTag;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NBT_VOLTAGE_TAG)) {
            String name = tag.getString(NBT_VOLTAGE_TAG);
            this.voltageTag = parseVoltage(name);
        } else {
            this.voltageTag = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (voltageTag != null) {
            tag.putString(NBT_VOLTAGE_TAG, voltageTag.getSerializedName());
        }
    }

    @Nullable
    private static VoltageTier parseVoltage(String name) {
        for (VoltageTier v : VoltageTier.values()) {
            if (v.getSerializedName().equals(name)) {
                return v;
            }
        }
        return null;
    }
}
