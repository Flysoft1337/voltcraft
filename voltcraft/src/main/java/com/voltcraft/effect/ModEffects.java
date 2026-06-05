package com.voltcraft.effect;

import com.voltcraft.VoltCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, VoltCraft.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> RADIATION =
            EFFECTS.register("radiation", RadiationEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
