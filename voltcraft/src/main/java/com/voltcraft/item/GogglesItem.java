package com.voltcraft.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public class GogglesItem extends ArmorItem {

    public GogglesItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties);
    }
}
