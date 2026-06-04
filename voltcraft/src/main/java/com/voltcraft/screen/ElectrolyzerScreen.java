package com.voltcraft.screen;

import com.voltcraft.VoltCraft;
import com.voltcraft.menu.ElectrolyzerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ElectrolyzerScreen extends AbstractMachineScreen<ElectrolyzerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "textures/gui/electrolyzer.png");

    public ElectrolyzerScreen(ElectrolyzerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }
}
