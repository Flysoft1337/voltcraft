package com.voltcraft.item;

import com.voltcraft.registry.ModDataComponents;
import com.voltcraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

public class SodiumIngotItem extends Item {

    private static final int OXIDATION_TIME = 600; // 30秒
    private static final float EXPLOSION_POWER = 1.5f;
    private static final int EXPLOSION_DELAY = 40; // 2秒
    private static final float FLOAT_UP_SPEED = 0.05f;

    public SodiumIngotItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide()) return;

        long startTime = stack.getOrDefault(ModDataComponents.OXIDATION_START_TIME.get(), 0L);
        if (startTime == -1) return;

        if (isInWater(entity)) {
            if (startTime != 0) {
                stack.set(ModDataComponents.OXIDATION_START_TIME.get(), 0L);
            }
        } else {
            if (startTime == 0) {
                stack.set(ModDataComponents.OXIDATION_START_TIME.get(), level.getGameTime());
            } else if (level.getGameTime() - startTime >= OXIDATION_TIME) {
                stack.set(ModDataComponents.OXIDATION_START_TIME.get(), -1L);
                if (entity instanceof Player player) {
                    ItemStack oxideStack = new ItemStack(ModItems.SODIUM_OXIDE_INGOT.get(), stack.getCount());
                    player.getInventory().setItem(slot, oxideStack);
                }
            }
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide()) return false;

        if (ItemFluidUtil.isInWater(level, entity)) {
            ItemFluidUtil.floatOnWater(entity, FLOAT_UP_SPEED);

            if (!entity.getPersistentData().getBoolean("SodiumIgnited")) {
                entity.setRemainingFireTicks(300);
                entity.getPersistentData().putBoolean("SodiumIgnited", true);
            }

            int timer = entity.getPersistentData().getInt("SodiumExplosionTimer") + 1;
            entity.getPersistentData().putInt("SodiumExplosionTimer", timer);

            if (level.random.nextInt(2) == 0) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                        entity.getX() + (level.random.nextDouble() - 0.5) * 0.5,
                        entity.getY() + 0.5,
                        entity.getZ() + (level.random.nextDouble() - 0.5) * 0.5,
                        0, 0.05, 0);
                level.addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                        entity.getX() + (level.random.nextDouble() - 0.5) * 0.3,
                        entity.getY() + 0.3,
                        entity.getZ() + (level.random.nextDouble() - 0.5) * 0.3,
                        0, 0.1, 0);
            }

            if (timer >= EXPLOSION_DELAY) {
                level.explode(entity, entity.getX(), entity.getY(), entity.getZ(),
                        EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
                for (int i = 0; i < 10; i++) {
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                            entity.getX() + (level.random.nextDouble() - 0.5),
                            entity.getY() + level.random.nextDouble(),
                            entity.getZ() + (level.random.nextDouble() - 0.5),
                            0, 0.1, 0);
                }
                entity.discard();
                return true;
            }
        }

        return false;
    }

    private boolean isInWater(Entity entity) {
        BlockPos pos = entity.blockPosition();
        return entity.level().getFluidState(pos).is(Fluids.WATER) ||
               entity.level().getFluidState(pos).is(Fluids.FLOWING_WATER);
    }
}
