package com.voltcraft.screen;

import com.voltcraft.VoltCraft;
import com.voltcraft.menu.PlatePressMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PlatePressScreen extends AbstractMachineScreen<PlatePressMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "textures/gui/plate_press.png");

    public PlatePressScreen(PlatePressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }
}
