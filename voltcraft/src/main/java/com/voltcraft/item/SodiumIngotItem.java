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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 钠锭 — 遇水爆炸，在空气中氧化
 *
 * 特殊行为：
 * - 在手中或背包中超过30秒自动变成氧化钠锭（一次性转换）
 * - 投入水中自燃且发生小型爆炸（浮在水面上）
 */
public class SodiumIngotItem extends Item {

    /** 氧化时间（tick）：30秒 = 600 tick */
    private static final int OXIDATION_TIME = 600;

    /** 爆炸强度 */
    private static final float EXPLOSION_POWER = 1.5f;

    /** 自燃持续时间（tick） */
    private static final int BURN_DURATION = 100; // 5秒

    /** 爆爆炸延迟（tick）：接触水后2秒爆炸 */
    private static final int EXPLOSION_DELAY = 40;

    public SodiumIngotItem(Properties properties) {
        super(properties);
    }

    // === 背包中的氧化逻辑（一次性转换） ===

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide()) {
            return;
        }

        // 获取或初始化氧化计时器
        int oxidationProgress = stack.getOrDefault(ModDataComponents.OXIDATION_PROGRESS.get(), 0);

        // 如果已经氧化完成，不再处理
        if (oxidationProgress >= OXIDATION_TIME) {
            return;
        }

        // 检查是否暴露在空气中（不在水中）
        if (!isInWater(entity)) {
            oxidationProgress++;

            if (oxidationProgress >= OXIDATION_TIME) {
                // 氧化完成，一次性变成氧化钠锭
                ItemStack oxideStack = new ItemStack(ModItems.SODIUM_OXIDE_INGOT.get(), stack.getCount());

                // 替换背包中的物品
                if (entity instanceof Player player) {
                    player.getInventory().setItem(slot, oxideStack);
                }

                return;
            }
        } else {
            // 在水中，重置氧化进度
            oxidationProgress = 0;
        }

        // 保存氧化进度
        stack.set(ModDataComponents.OXIDATION_PROGRESS.get(), oxidationProgress);
    }

    // === 物品实体的水中爆炸逻辑 ===

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();

        if (level.isClientSide()) {
            return false;
        }

        BlockPos pos = entity.blockPosition();

        // 检测是否在水中
        if (isInWater(level, pos)) {
            // 保持浮在水面上（阻止下沉）
            floatOnWater(entity);

            // 开始或持续自燃
            if (entity.getRemainingFireTicks() <= 0) {
                entity.setRemainingFireTicks(BURN_DURATION);
            }

            // 获取或初始化爆炸计时器
            int explosionTimer = getExplosionTimer(entity);
            explosionTimer++;

            // 产生火焰粒子效果
            if (level.random.nextInt(2) == 0) {
                level.addParticle(
                        net.minecraft.core.particles.ParticleTypes.FLAME,
                        entity.getX() + (level.random.nextDouble() - 0.5) * 0.5,
                        entity.getY() + 0.5,
                        entity.getZ() + (level.random.nextDouble() - 0.5) * 0.5,
                        0, 0.05, 0
                );

                // 钠遇水还会产生氢气气泡
                level.addParticle(
                        net.minecraft.core.particles.ParticleTypes.BUBBLE,
                        entity.getX() + (level.random.nextDouble() - 0.5) * 0.3,
                        entity.getY() + 0.3,
                        entity.getZ() + (level.random.nextDouble() - 0.5) * 0.3,
                        0, 0.1, 0
                );
            }

            // 延迟爆炸
            if (explosionTimer >= EXPLOSION_DELAY) {
                // 小型爆炸（不破坏方块，但造成伤害和击退）
                level.explode(
                        entity,
                        entity.getX(), entity.getY(), entity.getZ(),
                        EXPLOSION_POWER,
                        Level.ExplosionInteraction.NONE // 不破坏方块
                );

                // 产生大量烟雾
                for (int i = 0; i < 10; i++) {
                    level.addParticle(
                            net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                            entity.getX() + (level.random.nextDouble() - 0.5),
                            entity.getY() + level.random.nextDouble(),
                            entity.getZ() + (level.random.nextDouble() - 0.5),
                            0, 0.1, 0
                    );
                }

                // 爆炸后消失
                entity.discard();
                return true;
            }

            // 保存爆炸计时器
            setExplosionTimer(entity, explosionTimer);
        }

        return false;
    }

    // === 辅助方法 ===

    /**
     * 让物品实体浮在水面上
     */
    private void floatOnWater(ItemEntity entity) {
        // 获取水面高度
        BlockPos pos = entity.blockPosition();
        double waterSurfaceY = pos.getY() + 1.0; // 水面上方

        // 如果实体低于水面，向上推
        if (entity.getY() < waterSurfaceY) {
            Vec3 motion = entity.getDeltaMovement();
            // 设置向上的速度，抵消重力
            entity.setDeltaMovement(motion.x, 0.1, motion.z);
            // 重置下落距离，防止摔落伤害
            entity.fallDistance = 0;
        }

        // 减少水平移动（模拟水的阻力）
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * 0.95, motion.y, motion.z * 0.95);
    }

    /**
     * 获取爆炸计时器
     */
    private int getExplosionTimer(ItemEntity entity) {
        // 使用实体的持久化数据存储计时器
        if (entity.getPersistentData().contains("SodiumExplosionTimer")) {
            return entity.getPersistentData().getInt("SodiumExplosionTimer");
        }
        return 0;
    }

    /**
     * 设置爆炸计时器
     */
    private void setExplosionTimer(ItemEntity entity, int timer) {
        entity.getPersistentData().putInt("SodiumExplosionTimer", timer);
    }

    /**
     * 检测实体是否在水中
     */
    private boolean isInWater(Entity entity) {
        BlockPos pos = entity.blockPosition();
        BlockState state = entity.level().getBlockState(pos);
        return state.getFluidState().is(Fluids.WATER) ||
               state.getFluidState().is(Fluids.FLOWING_WATER);
    }

    /**
     * 检测物品实体是否在水中（更宽范围的检测）
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
