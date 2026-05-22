package com.voltcraft.event;

import com.voltcraft.item.FlaskItem;
import com.voltcraft.item.TestTubeItem;
import com.voltcraft.registry.ModDataComponents;
import com.voltcraft.registry.ModItems;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

import static com.voltcraft.VoltCraft.MOD_ID;

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Test tube — 250 mB
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new FluidHandlerItemStack(ModDataComponents.FLUID_CONTENT, stack, TestTubeItem.CAPACITY),
                ModItems.TEST_TUBE.get()
        );

        // Flask — 500 mB
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new FluidHandlerItemStack(ModDataComponents.FLUID_CONTENT, stack, FlaskItem.CAPACITY),
                ModItems.FLASK.get()
        );
    }
}
