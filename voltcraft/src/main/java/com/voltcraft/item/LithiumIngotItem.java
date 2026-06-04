package com.voltcraft.item;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class LithiumIngotItem extends Item {

    private static final int BURN_DURATION = 200;
    private static final float FLOAT_UP_SPEED = 0.05f;

    public LithiumIngotItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide()) return false;

        if (ItemFluidUtil.isInWater(level, entity)) {
            ItemFluidUtil.floatOnWater(entity, FLOAT_UP_SPEED);

            if (!entity.getPersistentData().getBoolean("LithiumIgnited")) {
                entity.setRemainingFireTicks(BURN_DURATION);
                entity.getPersistentData().putBoolean("LithiumIgnited", true);
            }

            if (level.random.nextInt(3) == 0) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                        entity.getX() + (level.random.nextDouble() - 0.5) * 0.5,
                        entity.getY() + 0.5,
                        entity.getZ() + (level.random.nextDouble() - 0.5) * 0.5,
                        0, 0.05, 0);
            }

            if (entity.getRemainingFireTicks() <= 1) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        entity.getX(), entity.getY() + 0.5, entity.getZ(), 0, 0.1, 0);
                entity.discard();
                return true;
            }
        }

        return false;
    }
}
