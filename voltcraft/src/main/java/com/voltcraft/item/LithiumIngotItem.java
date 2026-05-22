package com.voltcraft.item;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.BlockPos;
public class LithiumIngotItem extends Item {

    private static final int BURN_DURATION = 200; // 10秒
    private static final float FLOAT_UP_SPEED = 0.25f; // 上浮速度

    public LithiumIngotItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide()) return false;

        if (isInWater(level, entity)) {
            // 浮在水面上
            floatOnWater(level, entity);

            // 入水瞬间点燃一次
            if (!entity.getPersistentData().getBoolean("LithiumIgnited")) {
                entity.setRemainingFireTicks(BURN_DURATION);
                entity.getPersistentData().putBoolean("LithiumIgnited", true);
            }

            // 粒子
            if (level.random.nextInt(3) == 0) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                        entity.getX() + (level.random.nextDouble() - 0.5) * 0.5,
                        entity.getY() + 0.5,
                        entity.getZ() + (level.random.nextDouble() - 0.5) * 0.5,
                        0, 0.05, 0);
            }

            // 烧完销毁
            if (entity.getRemainingFireTicks() <= 1) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        entity.getX(), entity.getY() + 0.5, entity.getZ(), 0, 0.1, 0);
                entity.discard();
                return true;
            }
        }

        return false;
    }

    private void floatOnWater(Level level, ItemEntity entity) {
        entity.setDeltaMovement(0, FLOAT_UP_SPEED, 0);
        entity.fallDistance = 0;
    }

    private boolean isInWater(Level level, ItemEntity entity) {
        BlockPos pos = entity.blockPosition();
        if (level.getFluidState(pos).is(Fluids.WATER) ||
            level.getFluidState(pos).is(Fluids.FLOWING_WATER)) return true;
        BlockPos below = pos.below();
        return level.getFluidState(below).is(Fluids.WATER) ||
               level.getFluidState(below).is(Fluids.FLOWING_WATER);
    }
}
