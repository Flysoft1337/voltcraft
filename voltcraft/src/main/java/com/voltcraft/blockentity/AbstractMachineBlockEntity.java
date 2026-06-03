package com.voltcraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 通用机器方块实体基类。
 * 提供能量存储、物品处理、进度追踪、方向感知的通用实现。
 */
public abstract class AbstractMachineBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    protected final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case INPUT_SLOT -> isInputItem(stack);
                case OUTPUT_SLOT -> false;
                default -> false;
            };
        }
    };

    protected final EnergyStorage energyStorage;
    protected final IItemHandler inputHandler = new InputOnlyHandler();
    protected final IItemHandler outputHandler = new OutputOnlyHandler();

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
            // 客户端只读
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    protected int progress = 0;
    protected int maxProgress;

    protected AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                          int energyPerOperation, int processTime) {
        super(type, pos, state);
        this.maxProgress = processTime;
        this.energyStorage = new EnergyStorage(energyPerOperation * 4, energyPerOperation, 0);
    }

    protected abstract int getEnergyPerOperation();
    protected abstract boolean isInputItem(ItemStack stack);
    protected abstract ItemStack getOutputItem(ItemStack input);

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public IEnergyStorage getEnergyHandler(Direction side) {
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        return side == facing.getOpposite() ? energyStorage : null;
    }

    public IItemHandler getItemHandler(Direction side) {
        if (side == null) return itemHandler;
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();
        if (side == left) return inputHandler;
        if (side == right) return outputHandler;
        return null;
    }

    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }

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

    private boolean canProcess() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return false;
        if (energyStorage.getEnergyStored() < getEnergyPerOperation()) return false;
        if (!isInputItem(input)) return false;

        ItemStack output = getOutputItem(input);
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (currentOutput.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(currentOutput, output)) return false;
        return currentOutput.getCount() + output.getCount() <= currentOutput.getMaxStackSize();
    }

    private void processItem() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        ItemStack output = getOutputItem(input);

        energyStorage.extractEnergy(getEnergyPerOperation(), false);
        input.shrink(1);

        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, output.copy());
        } else {
            currentOutput.grow(output.getCount());
        }
        setChanged();
    }

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

    protected class InputOnlyHandler implements IItemHandler {
        @Override public int getSlots() { return 1; }

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

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isInputItem(stack);
        }
    }

    protected class OutputOnlyHandler implements IItemHandler {
        @Override public int getSlots() { return 1; }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? itemHandler.getStackInSlot(OUTPUT_SLOT) : ItemStack.EMPTY;
        }

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }

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

        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    }
}
