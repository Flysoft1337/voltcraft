package com.voltcraft.item;

import com.voltcraft.registry.ModDataComponents;
import com.voltcraft.registry.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class SodiumIngotItem extends Item {

    private static final int OXIDATION_TIME = 600; // 30秒
    private static final float EXPLOSION_POWER = 1.5f;
    private static final int EXPLOSION_DELAY = 40; // 2秒
    private static final float FLOAT_UP_SPEED = 0.25f; // 上浮速度

    public SodiumIngotItem(Properties properties) {
        super(properties);
    }

    // === 背包氧化：只在状态变化时写两次组件 ===

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide()) return;

        long startTime = stack.getOrDefault(ModDataComponents.OXIDATION_START_TIME.get(), 0L);

        // 已经完成转换（startTime == -1），不再处理
        if (startTime == -1) return;

        if (isInWater(entity)) {
            // 在水中：重置计时（仅在之前有值时才写）
            if (startTime != 0) {
                stack.set(ModDataComponents.OXIDATION_START_TIME.get(), 0L);
            }
        } else {
            // 在空气中
            if (startTime == 0) {
                // 记录开始时间
                stack.set(ModDataComponents.OXIDATION_START_TIME.get(), level.getGameTime());
            } else if (level.getGameTime() - startTime >= OXIDATION_TIME) {
                // 30秒到了，一次性转换
                stack.set(ModDataComponents.OXIDATION_START_TIME.get(), -1L);
                if (entity instanceof Player player) {
                    ItemStack oxideStack = new ItemStack(ModItems.SODIUM_OXIDE_INGOT.get(), stack.getCount());
                    player.getInventory().setItem(slot, oxideStack);
                }
            }
            // 时间未到 → 什么都不做，零组件写入
        }
    }

    // === 物品实体：水中浮力 + 自燃 + 爆炸 ===

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide()) return false;

        if (isInWater(level, entity)) {
            // 浮在水面上（每tick持续执行）
            floatOnWater(level, entity);

            // 一次性点燃
            if (!entity.getPersistentData().getBoolean("SodiumIgnited")) {
                entity.setRemainingFireTicks(300);
                entity.getPersistentData().putBoolean("SodiumIgnited", true);
            }

            // 爆炸计时
            int timer = entity.getPersistentData().getInt("SodiumExplosionTimer") + 1;
            entity.getPersistentData().putInt("SodiumExplosionTimer", timer);

            // 粒子
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

            // 爆炸
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

    // === 浮力：直接往上推 ===

    private void floatOnWater(Level level, ItemEntity entity) {
        entity.setDeltaMovement(0, FLOAT_UP_SPEED, 0);
        entity.fallDistance = 0;
    }

    private boolean isInWater(Entity entity) {
        BlockPos pos = entity.blockPosition();
        return entity.level().getFluidState(pos).is(Fluids.WATER) ||
               entity.level().getFluidState(pos).is(Fluids.FLOWING_WATER);
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
