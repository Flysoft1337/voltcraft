package com.voltcraft.item;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 锂锭 — 遇水自燃
 *
 * 特殊行为：
 * - 扔进水中会自燃（类似岩浆，但保留时间更长）
 * - 燃烧一段时间后消失
 */
public class LithiumIngotItem extends Item {

    /** 自燃持续时间（tick），比岩浆销毁时间长 */
    private static final int BURN_DURATION = 200; // 10秒

    public LithiumIngotItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();

        if (level.isClientSide()) {
            return false;
        }

        BlockPos pos = entity.blockPosition();
        BlockState state = level.getBlockState(pos);

        // 检测是否在水中
        if (isInWater(level, pos)) {
            // 开始自燃
            entity.setRemainingFireTicks(BURN_DURATION);

            // 产生火焰粒子效果
            if (level.random.nextInt(3) == 0) {
                level.addParticle(
                        net.minecraft.core.particles.ParticleTypes.FLAME,
                        entity.getX() + (level.random.nextDouble() - 0.5) * 0.5,
                        entity.getY() + 0.5,
                        entity.getZ() + (level.random.nextDouble() - 0.5) * 0.5,
                        0, 0.05, 0
                );
            }

            // 燃烧一段时间后消失
            if (entity.getRemainingFireTicks() <= 1) {
                // 产生烟雾粒子表示消失
                level.addParticle(
                        net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        entity.getX(), entity.getY() + 0.5, entity.getZ(),
                        0, 0.1, 0
                );
                entity.discard();
                return true;
            }
        }

        return false;
    }

    /**
     * 检测实体是否在水中
     */
    private boolean isInWater(Level level, BlockPos pos) {
        // 检查当前位置和周围是否有水
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = pos.offset(x, 0, z);
                BlockState state = level.getBlockState(checkPos);
                if (state.getFluidState().is(Fluids.WATER) ||
                    state.getFluidState().is(Fluids.FLOWING_WATER)) {
                    return true;
                }
            }
        }
        return false;
    }
}
