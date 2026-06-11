package com.voltcraft.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 处理辐射效果移除后的最大生命值缓慢恢复
 * 每30秒（600tick）恢复1HP最大生命上限
 */
@EventBusSubscriber(modid = "voltcraft")
public class RadiationCleanupHandler {

    private static final ResourceLocation MODIFIER_ID = RadiationEffect.MODIFIER_ID;
    private static final String NBT_KEY = RadiationEffect.NBT_KEY;
    private static final String NBT_TICK = RadiationEffect.NBT_TICK;
    private static final String NBT_RECOVERING = "voltcraft:rad_recovering";
    private static final String NBT_RECOVER_AMOUNT = "voltcraft:rad_recover_amount";
    private static final String NBT_RECOVER_TICK = "voltcraft:rad_recover_tick";
    private static final int RECOVER_INTERVAL = 600; // 30秒

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        startRecovery(event.getEntity(), event.getEffectInstance());
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        startRecovery(event.getEntity(), event.getEffectInstance());
    }

    private static void startRecovery(LivingEntity entity, MobEffectInstance effect) {
        if (effect != null && effect.getEffect().value() instanceof RadiationEffect) {
            int lost = entity.getPersistentData().getInt(NBT_KEY);
            entity.getPersistentData().remove(NBT_KEY);
            entity.getPersistentData().remove(NBT_TICK);

            if (lost > 0) {
                entity.getPersistentData().putInt(NBT_RECOVERING, 1);
                entity.getPersistentData().putInt(NBT_RECOVER_AMOUNT, lost);
                entity.getPersistentData().putInt(NBT_RECOVER_TICK, entity.tickCount);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity living)) continue;
                var data = living.getPersistentData();
                if (data.getInt(NBT_RECOVERING) != 1) continue;

                int remaining = data.getInt(NBT_RECOVER_AMOUNT);
                if (remaining <= 0) {
                    finishRecovery(living);
                    continue;
                }

                int lastTick = data.getInt(NBT_RECOVER_TICK);
                if (living.tickCount - lastTick >= RECOVER_INTERVAL) {
                    data.putInt(NBT_RECOVER_TICK, living.tickCount);
                    remaining--;
                    data.putInt(NBT_RECOVER_AMOUNT, remaining);

                    var maxHealth = Attributes.MAX_HEALTH;
                    if (maxHealth != null) {
                        var attrInstance = living.getAttribute(maxHealth);
                        attrInstance.removeModifier(MODIFIER_ID);
                        if (remaining > 0) {
                            attrInstance.addTransientModifier(
                                    new AttributeModifier(MODIFIER_ID, -remaining,
                                            AttributeModifier.Operation.ADD_VALUE));
                        }
                        living.setHealth(Math.min(living.getHealth(), living.getMaxHealth()));
                    }

                    if (remaining <= 0) {
                        finishRecovery(living);
                    }
                }
            }
        }
    }

    private static void finishRecovery(LivingEntity entity) {
        var data = entity.getPersistentData();
        data.remove(NBT_RECOVERING);
        data.remove(NBT_RECOVER_AMOUNT);
        data.remove(NBT_RECOVER_TICK);

        var maxHealth = Attributes.MAX_HEALTH;
        if (maxHealth != null) {
            entity.getAttribute(maxHealth).removeModifier(MODIFIER_ID);
            entity.setHealth(Math.min(entity.getHealth(), entity.getMaxHealth()));
        }
    }
}
