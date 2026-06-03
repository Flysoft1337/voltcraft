package com.voltcraft.blockentity;

import com.voltcraft.registry.ModBlockEntities;
import com.voltcraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 制板机方块实体 — 将金属锭压制成金属板
 */
public class PlatePressBlockEntity extends AbstractMachineBlockEntity {

    public static final int ENERGY_PER_OPERATION = 500;
    public static final int PROCESS_TIME = 200;

    private static final Map<Item, Item> INGOT_TO_PLATE = Map.of(
            Items.COPPER_INGOT, ModItems.COPPER_PLATE.get(),
            Items.IRON_INGOT, ModItems.IRON_PLATE.get(),
            ModItems.ZINC_INGOT.get(), ModItems.ZINC_PLATE.get(),
            ModItems.MANGANESE_INGOT.get(), ModItems.MANGANESE_PLATE.get(),
            ModItems.NICKEL_INGOT.get(), ModItems.NICKEL_PLATE.get(),
            ModItems.LEAD_INGOT.get(), ModItems.LEAD_PLATE.get(),
            ModItems.SILVER_INGOT.get(), ModItems.SILVER_PLATE.get(),
            ModItems.TIN_INGOT.get(), ModItems.TIN_PLATE.get(),
            ModItems.IRISITE_INGOT.get(), ModItems.IRISITE_PLATE.get()
    );

    public PlatePressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLATE_PRESS.get(), pos, state, ENERGY_PER_OPERATION, PROCESS_TIME);
    }

    @Override
    protected int getEnergyPerOperation() { return ENERGY_PER_OPERATION; }

    @Override
    public boolean isInputItem(ItemStack stack) {
        return INGOT_TO_PLATE.containsKey(stack.getItem());
    }

    @Override
    protected ItemStack getOutputItem(ItemStack input) {
        Item plate = INGOT_TO_PLATE.get(input.getItem());
        return plate == null ? ItemStack.EMPTY : new ItemStack(plate);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.voltcraft.menu.PlatePressMenu(containerId, playerInventory, this, this.data);
    }
}
