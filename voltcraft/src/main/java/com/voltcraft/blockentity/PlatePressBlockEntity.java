package com.voltcraft.blockentity;

import com.voltcraft.block.PlatePressBlock;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 制板机方块实体 — 将金属锭压制成金属板
 *
 * 能量输入：背面
 * 物品输入：左侧
 * 物品输出：右侧
 */
public class PlatePressBlockEntity extends BlockEntity implements MenuProvider {

    public static final int ENERGY_PER_OPERATION = 500;
    public static final int PROCESS_TIME = 200;

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    // 配方映射：锭 → 板
    private static final Map<Item, Item> INGOT_TO_PLATE = Map.of(
            Items.COPPER_INGOT, ModItems.COPPER_PLATE.get(),
            Items.IRON_INGOT, ModItems.IRON_PLATE.get(),
            ModItems.ZINC_INGOT.get(), ModItems.ZINC_PLATE.get(),
            ModItems.MANGANESE_INGOT.get(), ModItems.MANGANESE_PLATE.get(),
            ModItems.NICKEL_INGOT.get(), ModItems.NICKEL_PLATE.get(),
            ModItems.LEAD_INGOT.get(), ModItems.LEAD_PLATE.get(),
            ModItems.SILVER_INGOT.get(), ModItems.SILVER_PLATE.get(),
            ModItems.TIN_INGOT.get(), ModItems.TIN_PLATE.get()
    );

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
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

    private final EnergyStorage energyStorage = new EnergyStorage(
            ENERGY_PER_OPERATION * 4,
            ENERGY_PER_OPERATION,
            0
    );

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
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    private int progress = 0;
    private int maxProgress = PROCESS_TIME;

    public PlatePressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLATE_PRESS.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.voltcraft.plate_press");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.voltcraft.menu.PlatePressMenu(containerId, playerInventory, this, this.data);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public IEnergyStorage getEnergyHandler(Direction side) {
        Direction facing = getBlockState().getValue(PlatePressBlock.FACING);
        if (side == facing.getOpposite()) {
            return energyStorage;
        }
        return null;
    }

    public IItemHandler getItemHandler(Direction side) {
        if (side == null) return itemHandler;

        Direction facing = getBlockState().getValue(PlatePressBlock.FACING);
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();

        if (side == left) {
            return new InputOnlyHandler();
        } else if (side == right) {
            return new OutputOnlyHandler();
        }
        return null;
    }

    public static boolean isInputItem(ItemStack stack) {
        return INGOT_TO_PLATE.containsKey(stack.getItem());
    }

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

        if (energyStorage.getEnergyStored() < ENERGY_PER_OPERATION) return false;

        if (!isInputItem(input)) return false;

        ItemStack output = getOutputItem(input);
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (currentOutput.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(currentOutput, output)) return false;
        return currentOutput.getCount() + output.getCount() <= currentOutput.getMaxStackSize();
    }

    private ItemStack getOutputItem(ItemStack input) {
        Item plate = INGOT_TO_PLATE.get(input.getItem());
        if (plate == null) return ItemStack.EMPTY;
        return new ItemStack(plate);
    }

    private void processItem() {
        if (!canProcess()) return;

        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        ItemStack output = getOutputItem(input);

        energyStorage.extractEnergy(ENERGY_PER_OPERATION, false);
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
            return ItemStack.EMPTY;
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

    private class OutputOnlyHandler implements IItemHandler {
        @Override
        public int getSlots() { return 1; }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? itemHandler.getStackInSlot(OUTPUT_SLOT) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
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
