package com.voltcraft.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 辐射效果
 * 按等级周期性造成伤害并削减最大生命值（半颗心 = 1HP）
 * 等级1(amp 0)：每15秒 | 等级2(amp 1)：每10秒 | 等级3(amp 2+)：每5秒
 */
public class RadiationEffect extends MobEffect {

    public static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("voltcraft", "radiation_max_health");
    public static final String NBT_KEY = "voltcraft:rad_health_loss";
    public static final String NBT_TICK = "voltcraft:rad_last_tick";
    private static final int[] INTERVALS = {300, 200, 100};

    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x00FF00);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        CompoundTag data = entity.getPersistentData();
        data.putInt(NBT_KEY, 0);
        data.putInt(NBT_TICK, entity.tickCount);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        int interval = INTERVALS[Math.min(amplifier, INTERVALS.length - 1)];
        CompoundTag data = entity.getPersistentData();
        int lastTick = data.getInt(NBT_TICK);

        if (entity.tickCount - lastTick >= interval) {
            data.putInt(NBT_TICK, entity.tickCount);

            float damage = 1.0f + amplifier * 0.5f;
            entity.hurt(entity.damageSources().wither(), damage);

            int count = data.getInt(NBT_KEY) + 1;
            data.putInt(NBT_KEY, count);

            var maxHealth = Attributes.MAX_HEALTH;
            if (maxHealth != null) {
                var attrInstance = entity.getAttribute(maxHealth);
                attrInstance.removeModifier(MODIFIER_ID);
                attrInstance.addTransientModifier(
                        new AttributeModifier(MODIFIER_ID, -count,
                                AttributeModifier.Operation.ADD_VALUE));
                entity.setHealth(Math.min(entity.getHealth(), entity.getMaxHealth()));
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributeMap) {
        var maxHealth = Attributes.MAX_HEALTH;
        if (maxHealth != null) {
            var attrInstance = attributeMap.getInstance(maxHealth);
            if (attrInstance != null) {
                attrInstance.removeModifier(MODIFIER_ID);
            }
        }
        super.removeAttributeModifiers(attributeMap);
    }
}
