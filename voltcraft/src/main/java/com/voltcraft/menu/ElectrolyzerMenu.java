package com.voltcraft.menu;

import com.voltcraft.blockentity.ElectrolyzerBlockEntity;
import com.voltcraft.registry.ModBlocks;
import com.voltcraft.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 电解槽容器
 */
public class ElectrolyzerMenu extends AbstractMachineMenu {

    public ElectrolyzerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
             playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
             new SimpleContainerData(4));
    }

    public ElectrolyzerMenu(int containerId, Inventory playerInventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ELECTROLYZER.get(), containerId, playerInventory, entity, data,
              ModBlocks.ELECTROLYZER.get(), ((ElectrolyzerBlockEntity) entity)::isInputItem);
    }
}
