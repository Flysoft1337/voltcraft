package com.voltcraft.registry;

import com.voltcraft.VoltCraft;
import com.voltcraft.item.BrineBucketItem;
import com.voltcraft.item.FlaskItem;
import com.voltcraft.item.GogglesItem;
import com.voltcraft.item.LithiumIngotItem;
import com.voltcraft.item.SodiumIngotItem;
import com.voltcraft.item.TestTubeItem;
import com.voltcraft.item.ToolItem;
import com.voltcraft.item.WireCoilItem;
import com.voltcraft.electric.WireType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(VoltCraft.MOD_ID);

    // Hemimorphite items (zinc ore)
    public static final DeferredItem<Item> RAW_HEMIMORPHITE = ITEMS.register(
            "raw_hemimorphite",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> ZINC_INGOT = ITEMS.register(
            "zinc_ingot",
            () -> new Item(new Item.Properties())
    );

    // Manganese items (from rhodonite ore)
    public static final DeferredItem<Item> RAW_MANGANESE = ITEMS.register(
            "raw_manganese",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> MANGANESE_INGOT = ITEMS.register(
            "manganese_ingot",
            () -> new Item(new Item.Properties())
    );

    // Nickel items (from garnierite ore)
    public static final DeferredItem<Item> RAW_NICKEL = ITEMS.register(
            "raw_nickel",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> NICKEL_INGOT = ITEMS.register(
            "nickel_ingot",
            () -> new Item(new Item.Properties())
    );

    // Lead items (from cerussite ore)
    public static final DeferredItem<Item> RAW_LEAD = ITEMS.register(
            "raw_lead",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.register(
            "lead_ingot",
            () -> new Item(new Item.Properties())
    );

    // Lithium items (from spodumene ore)
    public static final DeferredItem<Item> RAW_SPODUMENE = ITEMS.register(
            "raw_spodumene",
            () -> new Item(new Item.Properties())
    );

    /** 锂锭 — 遇水自燃 */
    public static final DeferredItem<LithiumIngotItem> LITHIUM_INGOT = ITEMS.register(
            "lithium_ingot",
            () -> new LithiumIngotItem(new Item.Properties())
    );

    // Sodium items
    /** 钠锭 — 在空气中氧化，遇水爆炸 */
    public static final DeferredItem<SodiumIngotItem> SODIUM_INGOT = ITEMS.register(
            "sodium_ingot",
            () -> new SodiumIngotItem(new Item.Properties())
    );

    public static final DeferredItem<Item> SODIUM_OXIDE_INGOT = ITEMS.register(
            "sodium_oxide_ingot",
            () -> new Item(new Item.Properties())
    );

    // Parts items
    public static final DeferredItem<Item> SPRING = ITEMS.register(
            "spring",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> FUSE = ITEMS.register(
            "fuse",
            () -> new Item(new Item.Properties())
    );

    // Lab / experimental items
    public static final DeferredItem<Item> ZINC_MANGANESE_BATTERY = ITEMS.register(
            "zinc_manganese_battery",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> CARBON_ROD = ITEMS.register(
            "carbon_rod",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> TEST_TUBE_ELECTROLYTE_PASTE = ITEMS.register(
            "test_tube_electrolyte_paste",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<TestTubeItem> TEST_TUBE = ITEMS.register(
            "test_tube",
            () -> new TestTubeItem(new Item.Properties())
    );

    public static final DeferredItem<FlaskItem> FLASK = ITEMS.register(
            "flask",
            () -> new FlaskItem(new Item.Properties())
    );

    // Parts - screws
    public static final DeferredItem<Item> SCREW = ITEMS.register(
            "screw",
            () -> new Item(new Item.Properties())
    );

    // Parts - brine bucket
    public static final DeferredItem<BrineBucketItem> BRINE_BUCKET = ITEMS.register(
            "brine_bucket",
            () -> new BrineBucketItem(new Item.Properties())
    );

    // Tools
    public static final DeferredItem<ToolItem> HAMMER = ITEMS.register(
            "hammer",
            () -> new ToolItem(new Item.Properties().durability(1000))
    );

    public static final DeferredItem<ToolItem> WRENCH = ITEMS.register(
            "wrench",
            () -> new ToolItem(new Item.Properties().durability(1200))
    );

    public static final DeferredItem<GogglesItem> GOGGLES = ITEMS.register(
            "goggles",
            () -> new GogglesItem(ModArmorMaterials.GOGGLES, new Item.Properties())
    );

    // Plates
    public static final DeferredItem<Item> IRON_PLATE = ITEMS.register(
            "iron_plate",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> ZINC_PLATE = ITEMS.register(
            "zinc_plate",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> MANGANESE_PLATE = ITEMS.register(
            "manganese_plate",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> NICKEL_PLATE = ITEMS.register(
            "nickel_plate",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> LEAD_PLATE = ITEMS.register(
            "lead_plate",
            () -> new Item(new Item.Properties())
    );

    // Silver items (from argentite ore)
    public static final DeferredItem<Item> RAW_ARGENTITE = ITEMS.register(
            "raw_argentite",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> SILVER_INGOT = ITEMS.register(
            "silver_ingot",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> SILVER_PLATE = ITEMS.register(
            "silver_plate",
            () -> new Item(new Item.Properties())
    );

    // Tin items (from cassiterite ore)
    public static final DeferredItem<Item> RAW_CASSITERITE = ITEMS.register(
            "raw_cassiterite",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> TIN_INGOT = ITEMS.register(
            "tin_ingot",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> TIN_PLATE = ITEMS.register(
            "tin_plate",
            () -> new Item(new Item.Properties())
    );

    // Wire coil items
    public static final DeferredItem<WireCoilItem> COPPER_WIRE_COIL = ITEMS.register(
            "copper_wire_coil",
            () -> new WireCoilItem(WireType.COPPER, new Item.Properties())
    );

    public static final DeferredItem<WireCoilItem> TIN_WIRE_COIL = ITEMS.register(
            "tin_wire_coil",
            () -> new WireCoilItem(WireType.TIN, new Item.Properties())
    );

    public static final DeferredItem<WireCoilItem> SILVER_WIRE_COIL = ITEMS.register(
            "silver_wire_coil",
            () -> new WireCoilItem(WireType.SILVER, new Item.Properties())
    );

    private ModItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
