package com.voltcraft.event;

import com.voltcraft.VoltCraft;
import com.voltcraft.registry.ModMenuTypes;
import com.voltcraft.screen.ElectrolyzerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 客户端 Mod 事件总线监听器。注册 UI 界面等。
 */
@EventBusSubscriber(modid = VoltCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModBusEvents {

    private ClientModBusEvents() {}

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ELECTROLYZER.get(), ElectrolyzerScreen::new);
    }
}
