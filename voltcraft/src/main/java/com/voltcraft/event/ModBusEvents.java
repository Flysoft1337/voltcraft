package com.voltcraft.event;

import com.voltcraft.VoltCraft;
import com.voltcraft.block.ElectrolyzerBlock;
import com.voltcraft.blockentity.BreakerBlockEntity;
import com.voltcraft.blockentity.ElectrolyzerBlockEntity;
import com.voltcraft.blockentity.TerminalBlockEntity;
import com.voltcraft.blockentity.TransformerBlockEntity;
import com.voltcraft.registry.ModBlockEntities;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Mod 事件总线监听器。注册 Capability 等启动期回调。
 */
@EventBusSubscriber(modid = VoltCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModBusEvents {

    private ModBusEvents() {}

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        // 变压器仅在输入面（FACING 反方向）暴露低压侧 IEnergyStorage
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.TRANSFORMER.get(),
                (TransformerBlockEntity be, Direction side) -> {
                    if (side == null) return be.inputHandler();
                    return side == be.inputFace() ? be.inputHandler() : null;
                }
        );

        // 空开仅在输入面暴露；跳闸时返回 BlockedHandler 拒收
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.BREAKER.get(),
                (BreakerBlockEntity be, Direction side) -> {
                    if (side == null) return be.inputHandler();
                    return side == be.inputFace() ? be.inputHandler() : null;
                }
        );

        // 接线端子仅在机器面暴露；短路时拒收
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.TERMINAL.get(),
                (TerminalBlockEntity be, Direction side) -> {
                    if (side == null) return be.machineHandler();
                    return side == be.machineFace() ? be.machineHandler() : null;
                }
        );

        // 水解槽：背面输入能量，左侧输入物品，右侧输出物品
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ELECTROLYZER.get(),
                (ElectrolyzerBlockEntity be, Direction side) -> {
                    if (side == null) return be.getEnergyStorage();
                    Direction facing = be.getBlockState().getValue(ElectrolyzerBlock.FACING);
                    return side == facing.getOpposite() ? be.getEnergyStorage() : null;
                }
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ELECTROLYZER.get(),
                (ElectrolyzerBlockEntity be, Direction side) -> {
                    if (side == null) return be.getItemHandler();
                    Direction facing = be.getBlockState().getValue(ElectrolyzerBlock.FACING);
                    Direction left = facing.getCounterClockWise();
                    Direction right = facing.getClockWise();
                    if (side == left) {
                        return be.getItemHandler(); // 左侧输入
                    } else if (side == right) {
                        return be.getItemHandler(); // 右侧输出（需要区分）
                    }
                    return null;
                }
        );
    }
}
