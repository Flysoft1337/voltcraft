package com.voltcraft.electric;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 跳闸/短路时挂载的空能量句柄：拒绝一切能量交互。
 */
public final class BlockedHandler implements IEnergyStorage {

    public static final BlockedHandler INSTANCE = new BlockedHandler();

    private BlockedHandler() {}

    @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
    @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
    @Override public int getEnergyStored() { return 0; }
    @Override public int getMaxEnergyStored() { return 0; }
    @Override public boolean canExtract() { return false; }
    @Override public boolean canReceive() { return false; }
}
