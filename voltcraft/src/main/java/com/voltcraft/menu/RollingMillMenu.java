package com.voltcraft.menu;

import com.voltcraft.blockentity.RollingMillBlockEntity;
import com.voltcraft.registry.ModBlocks;
import com.voltcraft.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 轧机容器
 */
public class RollingMillMenu extends AbstractMachineMenu {

    public RollingMillMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
             playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
             new SimpleContainerData(4));
    }

    public RollingMillMenu(int containerId, Inventory playerInventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ROLLING_MILL.get(), containerId, playerInventory, entity, data,
              ModBlocks.ROLLING_MILL.get(), ((RollingMillBlockEntity) entity)::isInputItem);
    }
}
