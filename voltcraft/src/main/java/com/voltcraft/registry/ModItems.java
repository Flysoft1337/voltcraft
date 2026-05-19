package com.voltcraft.registry;

import com.voltcraft.VoltCraft;
import com.voltcraft.electric.CableTier;
import com.voltcraft.item.SoftCableItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(VoltCraft.MOD_ID);

    public static final Map<CableTier, DeferredItem<Item>> SOFT_CABLES = new EnumMap<>(CableTier.class);

    static {
        for (CableTier tier : CableTier.values()) {
            DeferredItem<Item> entry = ITEMS.register(
                    tier.getSerializedName() + "_soft_cable",
                    () -> new SoftCableItem(tier, new Item.Properties().stacksTo(64)));
            SOFT_CABLES.put(tier, entry);
        }
    }

    private ModItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
