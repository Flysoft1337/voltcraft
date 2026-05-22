package com.voltcraft.blockentity;

import com.voltcraft.block.ElectrolyzerBlock;
import com.voltcraft.registry.ModBlockEntities;
import com.voltcraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 电解槽方块实体 — 处理电解配方
 *
 * 能量输入：背面（FACING 的反方向）
 * 物品输入：左侧（FACING 的左侧）
 * 物品输出：右侧（FACING 的右侧）
 */
public class ElectrolyzerBlockEntity extends BlockEntity implements MenuProvider {

    // 配方定义
    public static final int ENERGY_PER_OPERATION = 1000; // 每次操作需要 1000 FE
    public static final int PROCESS_TIME = 200; // 处理时间（tick）

    // 槽位索引
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case INPUT_SLOT -> isInputItem(stack);
                case OUTPUT_SLOT -> false; // 输出槽不能手动放入
                default -> false;
            };
        }
    };

    private final EnergyStorage energyStorage = new EnergyStorage(
            ENERGY_PER_OPERATION * 4, // 容量
            ENERGY_PER_OPERATION,      // 输入上限
            0                          // 不能输出
    );

    // 处理进度数据，用于 UI 同步
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energyStorage.getEnergyStored();
                case 3 -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端不需要设置
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    private int progress = 0;
    private int maxProgress = PROCESS_TIME;

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTROLYZER.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.voltcraft.electrolyzer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.voltcraft.menu.ElectrolyzerMenu(containerId, playerInventory, this, this.data);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    /**
     * 获取指定方向的能量处理器
     * 背面输入能量
     */
    public IEnergyStorage getEnergyHandler(Direction side) {
        Direction facing = getBlockState().getValue(ElectrolyzerBlock.FACING);
        if (side == facing.getOpposite()) {
            return energyStorage; // 背面可以输入能量
        }
        return null; // 其他面不能交互能量
    }

    /**
     * 获取指定方向的物品处理器
     * 左侧输入，右侧输出
     */
    public IItemHandler getItemHandler(Direction side) {
        if (side == null) return itemHandler; // 内部访问

        Direction facing = getBlockState().getValue(ElectrolyzerBlock.FACING);
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();

        if (side == left) {
            return new InputOnlyHandler(); // 左侧只能输入
        } else if (side == right) {
            return new OutputOnlyHandler(); // 右侧只能输出
        }
        return null; // 其他面不能交互物品
    }

    /**
     * 判断是否为有效的输入物品
     */
    public static boolean isInputItem(ItemStack stack) {
        // 盐水桶 或 水桶
        return stack.is(ModItems.BRINE_BUCKET.get()) ||
               stack.is(net.minecraft.world.item.Items.WATER_BUCKET);
    }

    /**
     * 获取当前处理进度（0-1）
     */
    public float getProgress() {
        if (maxProgress == 0) return 0;
        return (float) progress / maxProgress;
    }

    public int getProgressScaled(int pixels) {
        if (maxProgress == 0 || progress == 0) return 0;
        return progress * pixels / maxProgress;
    }

    /**
     * 服务端 tick 处理
     */
    public void serverTick() {
        if (canProcess()) {
            progress++;
            if (progress >= maxProgress) {
                processItem();
                progress = 0;
            }
        } else {
            progress = 0;
        }
    }

    /**
     * 检查是否可以处理
     */
    private boolean canProcess() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return false;

        // 检查是否有足够能量
        if (energyStorage.getEnergyStored() < ENERGY_PER_OPERATION) return false;

        // 检查输入物品是否有效
        if (!isInputItem(input)) return false;

        // 检查输出槽是否有空间
        ItemStack output = getOutputItem(input);
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (currentOutput.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(currentOutput, output)) return false;
        return currentOutput.getCount() + output.getCount() <= currentOutput.getMaxStackSize();
    }

    /**
     * 获取输出物品
     */
    private ItemStack getOutputItem(ItemStack input) {
        // TODO: 实现电解配方
        return ItemStack.EMPTY;
    }

    /**
     * 处理物品
     */
    private void processItem() {
        if (!canProcess()) return;

        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        ItemStack output = getOutputItem(input);

        // 消耗能量
        energyStorage.extractEnergy(ENERGY_PER_OPERATION, false);

        // 消耗输入物品
        input.shrink(1);

        // 添加输出物品
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, output.copy());
        } else {
            currentOutput.grow(output.getCount());
        }

        setChanged();
    }

    /**
     * 掉落物品
     */
    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.put("energy", energyStorage.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        energyStorage.deserializeNBT(registries, tag.getCompound("energy"));
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
    }

    /**
     * 只允许输入的物品处理器（左侧）
     */
    private class InputOnlyHandler implements IItemHandler {
        @Override
        public int getSlots() { return 1; }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? itemHandler.getStackInSlot(INPUT_SLOT) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0) return stack;
            if (!isItemValid(slot, stack)) return stack;

            ItemStack existing = itemHandler.getStackInSlot(INPUT_SLOT);
            int maxInsert = Math.min(stack.getCount(), getSlotLimit(slot) - existing.getCount());

            if (maxInsert <= 0) return stack;

            if (!simulate) {
                if (existing.isEmpty()) {
                    itemHandler.setStackInSlot(INPUT_SLOT, stack.split(maxInsert));
                } else {
                    existing.grow(maxInsert);
                    stack.shrink(maxInsert);
                }
                setChanged();
            }

            ItemStack remainder = stack.copy();
            remainder.shrink(maxInsert);
            return remainder;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY; // 不能提取
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isInputItem(stack);
        }
    }

    /**
     * 只允许输出的物品处理器（右侧）
     */
    private class OutputOnlyHandler implements IItemHandler {
        @Override
        public int getSlots() { return 1; }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? itemHandler.getStackInSlot(OUTPUT_SLOT) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack; // 不能插入
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0) return ItemStack.EMPTY;

            ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (output.isEmpty()) return ItemStack.EMPTY;

            int extract = Math.min(amount, output.getCount());
            ItemStack result = output.copy();
            result.setCount(extract);

            if (!simulate) {
                output.shrink(extract);
                setChanged();
            }

            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }
}
