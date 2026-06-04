package com.voltcraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

/**
 * 物品流体交互工具方法。
 */
public final class ItemFluidUtil {

    private ItemFluidUtil() {}

    /**
     * 检查物品实体是否在水中（当前位置或下方一格）。
     */
    public static boolean isInWater(Level level, ItemEntity entity) {
        BlockPos pos = entity.blockPosition();
        if (isWaterAt(level, pos)) return true;
        return isWaterAt(level, pos.below());
    }

    /**
     * 让物品实体浮在水面上。
     */
    public static void floatOnWater(ItemEntity entity, float speed) {
        entity.setDeltaMovement(0, speed, 0);
        entity.fallDistance = 0;
    }

    private static boolean isWaterAt(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(Fluids.WATER) ||
               level.getFluidState(pos).is(Fluids.FLOWING_WATER);
    }
}
