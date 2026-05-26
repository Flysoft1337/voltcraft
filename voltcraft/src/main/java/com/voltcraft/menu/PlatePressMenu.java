package com.voltcraft.menu;

import com.voltcraft.blockentity.PlatePressBlockEntity;
import com.voltcraft.registry.ModBlocks;
import com.voltcraft.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 制板机容器 — 客户端和服务端同步
 */
public class PlatePressMenu extends AbstractContainerMenu {

    private final PlatePressBlockEntity blockEntity;
    private final ContainerData data;

    // 客户端构造函数（从网络包创建）
    public PlatePressMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
             playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
             new SimpleContainerData(4));
    }

    // 服务端构造函数
    public PlatePressMenu(int containerId, Inventory playerInventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.PLATE_PRESS.get(), containerId);
        checkContainerSize(playerInventory, 2);
        this.blockEntity = (PlatePressBlockEntity) entity;
        this.data = data;

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        this.addSlot(new SlotItemHandler(this.blockEntity.getItemHandler(),
                PlatePressBlockEntity.INPUT_SLOT, 56, 17));

        this.addSlot(new SlotItemHandler(this.blockEntity.getItemHandler(),
                PlatePressBlockEntity.OUTPUT_SLOT, 116, 35));

        addDataSlots(data);
    }

    public int getScaledProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);
        int arrowWidth = 24;

        if (maxProgress == 0 || progress == 0) return 0;
        return progress * arrowWidth / maxProgress;
    }

    public int getEnergyStored() {
        return this.data.get(2);
    }

    public int getMaxEnergyStored() {
        return this.data.get(3);
    }

    public int getScaledEnergy() {
        int energy = getEnergyStored();
        int maxEnergy = getMaxEnergyStored();
        int energyHeight = 46;

        if (maxEnergy == 0 || energy == 0) return 0;
        return energy * energyHeight / maxEnergy;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < 2) {
                if (!this.moveItemStackTo(stack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, result);
            } else {
                if (PlatePressBlockEntity.isInputItem(stack)) {
                    if (!this.moveItemStackTo(stack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 29) {
                    if (!this.moveItemStackTo(stack, 29, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 38) {
                    if (!this.moveItemStackTo(stack, 2, 29, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                         player, ModBlocks.PLATE_PRESS.get());
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

    public static boolean isInputItem(ItemStack stack) {
        return PlatePressBlockEntity.isInputItem(stack);
    }
}
