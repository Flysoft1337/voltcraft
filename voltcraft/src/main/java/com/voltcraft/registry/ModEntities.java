package com.voltcraft.registry;

import com.voltcraft.VoltCraft;
import com.voltcraft.entity.SoftCableEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, VoltCraft.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<SoftCableEntity>> SOFT_CABLE =
            ENTITIES.register("soft_cable", () ->
                    EntityType.Builder.<SoftCableEntity>of(SoftCableEntity::new, MobCategory.MISC)
                            // 软线没有"实际位置"概念，AABB 在运行时按两端拉伸；这里给个安全初值
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(16)
                            .updateInterval(20)
                            .noSummon()
                            .build("soft_cable"));

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
