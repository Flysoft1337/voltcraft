package com.voltcraft.screen;

import com.voltcraft.VoltCraft;
import com.voltcraft.menu.RollingMillMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RollingMillScreen extends AbstractMachineScreen<RollingMillMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "textures/gui/rolling_mill.png");

    public RollingMillScreen(RollingMillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }
}
