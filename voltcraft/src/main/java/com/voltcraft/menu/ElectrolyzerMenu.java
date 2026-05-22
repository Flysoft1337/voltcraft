package com.voltcraft.menu;

import com.voltcraft.blockentity.ElectrolyzerBlockEntity;
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
 * 电解槽容器 — 客户端和服务端同步
 */
public class ElectrolyzerMenu extends AbstractContainerMenu {

    private final ElectrolyzerBlockEntity blockEntity;
    private final ContainerData data;

    // 客户端构造函数（从网络包创建）
    public ElectrolyzerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
             playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
             new SimpleContainerData(4));
    }

    // 服务端构造函数
    public ElectrolyzerMenu(int containerId, Inventory playerInventory, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ELECTROLYZER.get(), containerId);
        checkContainerSize(playerInventory, 2);
        this.blockEntity = (ElectrolyzerBlockEntity) entity;
        this.data = data;

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        // 添加电解槽槽位
        // 输入槽（上方）
        this.addSlot(new SlotItemHandler(this.blockEntity.getItemHandler(),
                ElectrolyzerBlockEntity.INPUT_SLOT, 56, 17));

        // 输出槽（右侧）
        this.addSlot(new SlotItemHandler(this.blockEntity.getItemHandler(),
                ElectrolyzerBlockEntity.OUTPUT_SLOT, 116, 35));

        addDataSlots(data);
    }

    /**
     * 获取当前处理进度（用于 UI 箭头动画）
     */
    public int getScaledProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);
        int arrowWidth = 24; // 箭头宽度（像素）

        if (maxProgress == 0 || progress == 0) return 0;
        return progress * arrowWidth / maxProgress;
    }

    /**
     * 获取当前能量（用于能量条显示）
     */
    public int getEnergyStored() {
        return this.data.get(2);
    }

    public int getMaxEnergyStored() {
        return this.data.get(3);
    }

    public int getScaledEnergy() {
        int energy = getEnergyStored();
        int maxEnergy = getMaxEnergyStored();
        int energyHeight = 46; // 能量条高度（像素）

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

            // 从电解槽槽位移到玩家背包
            if (index < 2) {
                if (!this.moveItemStackTo(stack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, result);
            }
            // 从玩家背包移到电解槽
            else {
                // 尝试放入输入槽
                if (ElectrolyzerBlockEntity.isInputItem(stack)) {
                    if (!this.moveItemStackTo(stack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 从快捷栏移到背包
                else if (index < 29) {
                    if (!this.moveItemStackTo(stack, 29, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 从背包移到快捷栏
                else if (index < 38) {
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
                         player, ModBlocks.ELECTROLYZER.get());
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

    /**
     * 判断是否为有效的输入物品（静态方法，供 Menu 使用）
     */
    public static boolean isInputItem(ItemStack stack) {
        return ElectrolyzerBlockEntity.isInputItem(stack);
    }
}
