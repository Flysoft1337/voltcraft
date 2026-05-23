package com.voltcraft.registry;

import com.mojang.serialization.Codec;
import com.voltcraft.VoltCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, VoltCraft.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENT =
            COMPONENTS.registerComponentType("fluid_content", builder -> builder
                    .persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    /** 钠锭氧化开始时间（gameTime tick），0表示未开始 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> OXIDATION_START_TIME =
            COMPONENTS.registerComponentType("oxidation_start_time", builder -> builder
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** 线圈起始位置（用于线缆连接） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> WIRE_START_POS =
            COMPONENTS.registerComponentType("wire_start_pos", builder -> builder
                    .persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC));

    private ModDataComponents() {}

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
