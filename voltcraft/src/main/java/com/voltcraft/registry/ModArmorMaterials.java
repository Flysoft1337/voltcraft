package com.voltcraft.registry;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;

import static com.voltcraft.VoltCraft.MOD_ID;

public final class ModArmorMaterials {

    public static final Holder<ArmorMaterial> GOGGLES = register("goggles", Util.make(
            new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.HELMET, 2);
                map.put(ArmorItem.Type.CHESTPLATE, 0);
                map.put(ArmorItem.Type.LEGGINGS, 0);
                map.put(ArmorItem.Type.BOOTS, 0);
            }),
            15,
            SoundEvents.ARMOR_EQUIP_IRON,
            0.0F,
            0.0F,
            Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT)
    );

    private static Holder<ArmorMaterial> register(String name,
                                                   EnumMap<ArmorItem.Type, Integer> defense,
                                                   int enchantability,
                                                   Holder<net.minecraft.sounds.SoundEvent> equipSound,
                                                   float toughness,
                                                   float knockbackResistance,
                                                   Ingredient repairIngredient) {
        ResourceLocation assetId = ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(assetId));
        ArmorMaterial material = new ArmorMaterial(defense, enchantability, equipSound,
                () -> repairIngredient, layers, toughness, knockbackResistance);
        return net.minecraft.core.registries.BuiltInRegistries.ARMOR_MATERIAL
                .wrapAsHolder(material);
    }

    private ModArmorMaterials() {}
}
