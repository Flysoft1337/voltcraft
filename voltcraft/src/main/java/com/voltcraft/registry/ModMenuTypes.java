package com.voltcraft.registry;

import com.voltcraft.VoltCraft;
import com.voltcraft.menu.ElectrolyzerMenu;
import com.voltcraft.menu.PlatePressMenu;
import com.voltcraft.menu.RollingMillMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 菜单类型注册
 */
public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, VoltCraft.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ElectrolyzerMenu>> ELECTROLYZER =
            registerMenuType("electrolyzer", ElectrolyzerMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<RollingMillMenu>> ROLLING_MILL =
            registerMenuType("rolling_mill", RollingMillMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<PlatePressMenu>> PLATE_PRESS =
            registerMenuType("plate_press", PlatePressMenu::new);

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(
            String name, IContainerFactory<T> factory) {
        return MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }

    private ModMenuTypes() {}

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
