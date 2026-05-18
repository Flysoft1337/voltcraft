package com.voltcraft.registry;

import com.voltcraft.VoltCraft;
import com.voltcraft.block.CableBlock;
import com.voltcraft.blockentity.CableBlockEntity;
import com.voltcraft.electric.CableTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VoltCraft.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> CABLE =
            BLOCK_ENTITIES.register("cable", () -> {
                Block[] cableBlocks = Arrays.stream(CableTier.values())
                        .map(tier -> ModBlocks.CABLES.get(tier).get())
                        .toArray(Block[]::new);
                return BlockEntityType.Builder.of(
                        (pos, state) -> {
                            // 通过 state 反查 CableBlock，再读出它的 tier 来构造
                            CableBlock block = (CableBlock) state.getBlock();
                            return new CableBlockEntity(ModBlockEntities.CABLE.get(), pos, state, block.tier());
                        },
                        cableBlocks
                ).build(null);
            });

    private ModBlockEntities() {}

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
