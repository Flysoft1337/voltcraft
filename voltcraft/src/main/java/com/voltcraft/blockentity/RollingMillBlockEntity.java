package com.voltcraft.blockentity;

import com.voltcraft.registry.ModBlockEntities;
import com.voltcraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 轧机方块实体 — 将金属板轧制成金属线
 */
public class RollingMillBlockEntity extends AbstractMachineBlockEntity {

    public static final int ENERGY_PER_OPERATION = 500;
    public static final int PROCESS_TIME = 200;

    private static final Map<Item, Item> PLATE_TO_WIRE = Map.ofEntries(
            Map.entry(ModItems.COPPER_PLATE.get(), ModItems.COPPER_WIRE.get()),
            Map.entry(ModItems.TIN_PLATE.get(), ModItems.TIN_WIRE.get()),
            Map.entry(ModItems.SILVER_PLATE.get(), ModItems.SILVER_WIRE.get()),
            Map.entry(ModItems.IRON_PLATE.get(), ModItems.IRON_WIRE.get()),
            Map.entry(ModItems.ZINC_PLATE.get(), ModItems.ZINC_WIRE.get()),
            Map.entry(ModItems.LEAD_PLATE.get(), ModItems.LEAD_WIRE.get()),
            Map.entry(ModItems.NICKEL_PLATE.get(), ModItems.NICKEL_WIRE.get()),
            Map.entry(ModItems.MANGANESE_PLATE.get(), ModItems.MANGANESE_WIRE.get()),
            Map.entry(ModItems.IRISITE_PLATE.get(), ModItems.IRISITE_WIRE.get())
    );

    public RollingMillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROLLING_MILL.get(), pos, state, ENERGY_PER_OPERATION, PROCESS_TIME);
    }

    @Override
    protected int getEnergyPerOperation() { return ENERGY_PER_OPERATION; }

    @Override
    public boolean isInputItem(ItemStack stack) {
        return PLATE_TO_WIRE.containsKey(stack.getItem());
    }

    @Override
    protected ItemStack getOutputItem(ItemStack input) {
        Item wire = PLATE_TO_WIRE.get(input.getItem());
        return wire == null ? ItemStack.EMPTY : new ItemStack(wire);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.voltcraft.menu.RollingMillMenu(containerId, playerInventory, this, this.data);
    }
}
