package com.voltcraft.menu;

import com.voltcraft.blockentity.PlatePressBlockEntity;
import com.voltcraft.registry.ModBlocks;
import com.voltcraft.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 制板机容器
 */
public class PlatePressMenu extends AbstractMachineMenu {

    public PlatePressMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
             playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
             new SimpleContainerData(4));
    }

    public PlatePressMenu(int containerId, Inventory playerInventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.PLATE_PRESS.get(), containerId, playerInventory, entity, data,
              ModBlocks.PLATE_PRESS.get(), ((PlatePressBlockEntity) entity)::isInputItem);
    }
}
