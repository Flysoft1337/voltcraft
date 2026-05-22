package com.voltcraft.item;

import net.minecraft.world.item.Item;

/**
 * 盐水桶 — 用于电解配方的原材料
 */
public class BrineBucketItem extends Item {

    public BrineBucketItem(Properties properties) {
        super(properties.stacksTo(1));
    }
}
