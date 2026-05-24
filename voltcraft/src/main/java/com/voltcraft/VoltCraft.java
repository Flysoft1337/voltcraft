package com.voltcraft;

import com.mojang.logging.LogUtils;
import com.voltcraft.network.WireConnectionSyncPacket;
import com.voltcraft.registry.ModBlockEntities;
import com.voltcraft.registry.ModBlocks;
import com.voltcraft.registry.ModCreativeTabs;
import com.voltcraft.registry.ModDataComponents;
import com.voltcraft.registry.ModItems;
import com.voltcraft.registry.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(VoltCraft.MOD_ID)
public class VoltCraft {

    public static final String MOD_ID = "voltcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VoltCraft(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        // 注册网络包
        modEventBus.addListener(this::registerPackets);

        LOGGER.info("VoltCraft initialized");
    }

    private void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MOD_ID);
        registrar.playToClient(
                WireConnectionSyncPacket.TYPE,
                WireConnectionSyncPacket.CODEC,
                WireConnectionSyncPacket::handle
        );
    }
}
