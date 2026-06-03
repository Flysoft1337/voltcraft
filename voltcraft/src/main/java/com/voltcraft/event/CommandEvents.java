package com.voltcraft.event;

import com.voltcraft.VoltCraft;
import com.voltcraft.command.OresCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = VoltCraft.MOD_ID)
public class CommandEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        OresCommand.register(event.getDispatcher());
        VoltCraft.LOGGER.debug("VoltCraft commands registered");
    }
}
