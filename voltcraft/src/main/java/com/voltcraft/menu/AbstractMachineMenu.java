package com.voltcraft.menu;

import com.voltcraft.blockentity.AbstractMachineBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.Predicate;

/**
 * 通用机器菜单基类。
 * 提供进度条、能量条、shift-click逻辑的通用实现。
 */
public abstract class AbstractMachineMenu extends AbstractContainerMenu {

    protected final AbstractMachineBlockEntity blockEntity;
    protected final ContainerData data;
    private final Block machineBlock;
    private final Predicate<ItemStack> inputItemCheck;

    protected AbstractMachineMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                                   BlockEntity entity, ContainerData data, Block machineBlock,
                                   Predicate<ItemStack> inputItemCheck) {
        super(type, containerId);
        checkContainerSize(playerInventory, 2);
        this.blockEntity = (AbstractMachineBlockEntity) entity;
        this.data = data;
        this.machineBlock = machineBlock;
        this.inputItemCheck = inputItemCheck;

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        this.addSlot(new SlotItemHandler(this.blockEntity.getItemHandler(),
                AbstractMachineBlockEntity.INPUT_SLOT, 56, 17));
        this.addSlot(new SlotItemHandler(this.blockEntity.getItemHandler(),
                AbstractMachineBlockEntity.OUTPUT_SLOT, 116, 35));

        addDataSlots(data);
    }

    public int getScaledProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);
        if (maxProgress == 0 || progress == 0) return 0;
        return progress * 24 / maxProgress;
    }

    public int getEnergyStored() { return this.data.get(2); }
    public int getMaxEnergyStored() { return this.data.get(3); }

    public int getScaledEnergy() {
        int energy = getEnergyStored();
        int maxEnergy = getMaxEnergyStored();
        if (maxEnergy == 0 || energy == 0) return 0;
        return energy * 46 / maxEnergy;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < 2) {
                if (!this.moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(stack, result);
            } else {
                if (inputItemCheck.test(stack)) {
                    if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (index < 29) {
                    if (!this.moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY;
                } else if (index < 38) {
                    if (!this.moveItemStackTo(stack, 2, 29, false)) return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                         player, machineBlock);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
