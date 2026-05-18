package com.voltcraft.block;

import com.voltcraft.blockentity.CableBlockEntity;
import com.voltcraft.electric.CableTier;
import com.voltcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 参数化电缆方块。所有电压等级共享同一个 Block 类，
 * 通过 CableTier 区分上限和电压。
 *
 * 当前阶段：方块外观 + BlockEntity 持有电压标签 NBT；
 * 后续阶段：连接方向 BlockState、FE Capability、EnergyNetwork 扫描。
 */
public class CableBlock extends Block implements EntityBlock {

    private final CableTier tier;

    public CableBlock(CableTier tier, BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CableTier tier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(ModBlockEntities.CABLE.get(), pos, state, tier);
    }
}
