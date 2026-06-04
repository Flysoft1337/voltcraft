package com.voltcraft.integration.jade;

import com.voltcraft.VoltCraft;
import com.voltcraft.blockentity.ElectrolyzerBlockEntity;
import com.voltcraft.blockentity.PlatePressBlockEntity;
import com.voltcraft.blockentity.RollingMillBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * 通用机器 Jade 信息：能量 + 进度
 * 适用于电解槽、制板机、轧机
 */
public enum MachineJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "machine");

    private static final String KEY_ENERGY_STORED = "EnergyStored";
    private static final String KEY_ENERGY_MAX = "EnergyMax";
    private static final String KEY_PROGRESS = "Progress";
    private static final String KEY_MAX_PROGRESS = "MaxProgress";

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();

        if (be instanceof ElectrolyzerBlockEntity electrolyzer) {
            data.putInt(KEY_ENERGY_STORED, electrolyzer.getEnergyStored());
            data.putInt(KEY_ENERGY_MAX, electrolyzer.getMaxEnergyStored());
            data.putInt(KEY_PROGRESS, electrolyzer.getProgress());
            data.putInt(KEY_MAX_PROGRESS, electrolyzer.getMaxProgress());
        } else if (be instanceof PlatePressBlockEntity platePress) {
            data.putInt(KEY_ENERGY_STORED, platePress.getEnergyStored());
            data.putInt(KEY_ENERGY_MAX, platePress.getMaxEnergyStored());
            data.putInt(KEY_PROGRESS, platePress.getProgress());
            data.putInt(KEY_MAX_PROGRESS, platePress.getMaxProgress());
        } else if (be instanceof RollingMillBlockEntity rollingMill) {
            data.putInt(KEY_ENERGY_STORED, rollingMill.getEnergyStored());
            data.putInt(KEY_ENERGY_MAX, rollingMill.getMaxEnergyStored());
            data.putInt(KEY_PROGRESS, rollingMill.getProgress());
            data.putInt(KEY_MAX_PROGRESS, rollingMill.getMaxProgress());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data == null || data.isEmpty()) return;

        // 能量信息
        if (data.contains(KEY_ENERGY_STORED) && data.contains(KEY_ENERGY_MAX)) {
            int stored = data.getInt(KEY_ENERGY_STORED);
            int max = data.getInt(KEY_ENERGY_MAX);
            tooltip.add(Component.translatable("voltcraft.jade.buffer", stored, max));
        }

        // 进度信息
        if (data.contains(KEY_PROGRESS) && data.contains(KEY_MAX_PROGRESS)) {
            int progress = data.getInt(KEY_PROGRESS);
            int maxProgress = data.getInt(KEY_MAX_PROGRESS);
            if (maxProgress > 0) {
                int percent = (progress * 100) / maxProgress;
                tooltip.add(Component.translatable("voltcraft.jade.progress", percent));
            }
        }
    }
}
