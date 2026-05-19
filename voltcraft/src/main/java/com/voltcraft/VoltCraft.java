package com.voltcraft;

import com.mojang.logging.LogUtils;
import com.voltcraft.registry.ModBlockEntities;
import com.voltcraft.registry.ModBlocks;
import com.voltcraft.registry.ModCreativeTabs;
import com.voltcraft.registry.ModEntities;
import com.voltcraft.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(VoltCraft.MOD_ID)
public class VoltCraft {

    public static final String MOD_ID = "voltcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VoltCraft(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        LOGGER.info("VoltCraft initialized");
    }
}
