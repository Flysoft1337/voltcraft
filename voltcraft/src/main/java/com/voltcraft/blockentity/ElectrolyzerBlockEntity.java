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
 * 电解槽方块实体 — 处理电解配方
 */
public class ElectrolyzerBlockEntity extends AbstractMachineBlockEntity {

    public static final int ENERGY_PER_OPERATION = 1000;
    public static final int PROCESS_TIME = 200;

    private static final Map<Item, ItemStack> ELECTROLYSIS_RECIPES = Map.of(
            Items.WATER_BUCKET, new ItemStack(ModItems.LITHIUM_INGOT.get(), 1),
            ModItems.BRINE_BUCKET.get(), new ItemStack(ModItems.SODIUM_INGOT.get(), 2)
    );

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTROLYZER.get(), pos, state, ENERGY_PER_OPERATION, PROCESS_TIME);
    }

    @Override
    protected int getEnergyPerOperation() { return ENERGY_PER_OPERATION; }

    @Override
    public boolean isInputItem(ItemStack stack) {
        return ELECTROLYSIS_RECIPES.containsKey(stack.getItem());
    }

    @Override
    protected ItemStack getOutputItem(ItemStack input) {
        ItemStack result = ELECTROLYSIS_RECIPES.get(input.getItem());
        return result == null ? ItemStack.EMPTY : result.copy();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.voltcraft.menu.ElectrolyzerMenu(containerId, playerInventory, this, this.data);
    }
}
