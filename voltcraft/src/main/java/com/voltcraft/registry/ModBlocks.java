package com.voltcraft.registry;

import com.voltcraft.VoltCraft;
import com.voltcraft.block.BreakerBlock;
import com.voltcraft.block.ElectrolyzerBlock;
import com.voltcraft.block.PlatePressBlock;
import com.voltcraft.block.RollingMillBlock;
import com.voltcraft.block.TerminalBlock;
import com.voltcraft.block.TransformerBlock;
import com.voltcraft.electric.CableTier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(VoltCraft.MOD_ID);

    public static final Map<CableTier, DeferredBlock<TransformerBlock>> TRANSFORMERS = createTransformers();
    public static final Map<CableTier, DeferredBlock<BreakerBlock>> BREAKERS = createBreakers();
    public static final Map<CableTier, DeferredBlock<TerminalBlock>> TERMINALS = createTerminals();

    // Hemimorphite ore blocks
    public static final DeferredBlock<Block> HEMIMORPHITE_ORE = registerWithItem(
            "hemimorphite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
    );

    public static final DeferredBlock<Block> DEEPSLATE_HEMIMORPHITE_ORE = registerWithItem(
            "deepslate_hemimorphite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .destroyTime(4.5f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops())
    );

    // Rhodonite ore blocks (manganese ore)
    public static final DeferredBlock<Block> RHODONITE_ORE = registerWithItem(
            "rhodonite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
    );

    public static final DeferredBlock<Block> DEEPSLATE_RHODONITE_ORE = registerWithItem(
            "deepslate_rhodonite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .destroyTime(4.5f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops())
    );

    // Garnierite ore blocks (nickel ore)
    public static final DeferredBlock<Block> GARNIERITE_ORE = registerWithItem(
            "garnierite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
    );

    public static final DeferredBlock<Block> DEEPSLATE_GARNIERITE_ORE = registerWithItem(
            "deepslate_garnierite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .destroyTime(4.5f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops())
    );

    // Cerussite ore blocks (lead ore)
    public static final DeferredBlock<Block> CERUSSITE_ORE = registerWithItem(
            "cerussite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
    );

    public static final DeferredBlock<Block> DEEPSLATE_CERUSSITE_ORE = registerWithItem(
            "deepslate_cerussite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .destroyTime(4.5f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops())
    );

    // Spodumene ore blocks (lithium ore)
    public static final DeferredBlock<Block> SPODUMENE_ORE = registerWithItem(
            "spodumene_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
    );

    public static final DeferredBlock<Block> DEEPSLATE_SPODUMENE_ORE = registerWithItem(
            "deepslate_spodumene_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .destroyTime(4.5f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops())
    );

    // Argentite ore blocks (silver ore)
    public static final DeferredBlock<Block> ARGENTITE_ORE = registerWithItem(
            "argentite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
    );

    public static final DeferredBlock<Block> DEEPSLATE_ARGENTITE_ORE = registerWithItem(
            "deepslate_argentite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .destroyTime(4.5f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops())
    );

    // Cassiterite ore blocks (tin ore)
    public static final DeferredBlock<Block> CASSITERITE_ORE = registerWithItem(
            "cassiterite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
    );

    public static final DeferredBlock<Block> DEEPSLATE_CASSITERITE_ORE = registerWithItem(
            "deepslate_cassiterite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .destroyTime(4.5f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops())
    );

    // Electrolyzer block
    public static final DeferredBlock<ElectrolyzerBlock> ELECTROLYZER = registerWithItem(
            "electrolyzer",
            () -> new ElectrolyzerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .destroyTime(3.5f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops())
    );

    // Rolling Mill block
    public static final DeferredBlock<RollingMillBlock> ROLLING_MILL = registerWithItem(
            "rolling_mill",
            () -> new RollingMillBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .destroyTime(3.5f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops())
    );

    // Plate Press block
    public static final DeferredBlock<PlatePressBlock> PLATE_PRESS = registerWithItem(
            "plate_press",
            () -> new PlatePressBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .destroyTime(3.5f)
                    .explosionResistance(6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops())
    );

    private static Map<CableTier, DeferredBlock<TransformerBlock>> createTransformers() {
        EnumMap<CableTier, DeferredBlock<TransformerBlock>> map = new EnumMap<>(CableTier.class);
        for (CableTier tier : CableTier.values()) {
            map.put(tier, registerWithItem(
                    tier.getSerializedName() + "_transformer",
                    () -> new TransformerBlock(tier, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.5f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops())
            ));
        }
        return Map.copyOf(map);
    }

    private static Map<CableTier, DeferredBlock<BreakerBlock>> createBreakers() {
        EnumMap<CableTier, DeferredBlock<BreakerBlock>> map = new EnumMap<>(CableTier.class);
        for (CableTier tier : CableTier.values()) {
            map.put(tier, registerWithItem(
                    tier.getSerializedName() + "_breaker",
                    () -> new BreakerBlock(tier, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops())
            ));
        }
        return Map.copyOf(map);
    }

    private static Map<CableTier, DeferredBlock<TerminalBlock>> createTerminals() {
        EnumMap<CableTier, DeferredBlock<TerminalBlock>> map = new EnumMap<>(CableTier.class);
        for (CableTier tier : CableTier.values()) {
            map.put(tier, registerWithItem(
                    tier.getSerializedName() + "_terminal",
                    () -> new TerminalBlock(tier, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops())
            ));
        }
        return Map.copyOf(map);
    }

    private ModBlocks() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    private static <B extends Block> DeferredBlock<B> registerWithItem(
            String name, Supplier<B> blockSupplier) {
        DeferredBlock<B> block = BLOCKS.register(name, blockSupplier);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
}
