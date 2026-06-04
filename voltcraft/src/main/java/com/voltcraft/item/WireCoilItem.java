package com.voltcraft.item;

import com.voltcraft.electric.WireType;
import com.voltcraft.electric.wire.WireConnection;
import com.voltcraft.electric.wire.WireEndpoint;
import com.voltcraft.electric.wire.WireNetworkManager;
import com.voltcraft.network.PendingWireSyncPacket;
import com.voltcraft.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * 线圈物品。
 * 右键第一个点记录位置，右键第二个点建立连接。
 * 客户端会渲染手持线缆连接到起始点，超出范围时显示红色指示。
 */
public class WireCoilItem extends Item {

    private final WireType wireType;

    public WireCoilItem(WireType wireType, Item.Properties properties) {
        super(properties);
        this.wireType = wireType;
    }

    public WireType wireType() {
        return wireType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos targetPos = getTargetBlockPos(player);
        if (targetPos == null) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos startPos = stack.get(ModDataComponents.WIRE_START_POS.get());

        if (startPos != null) {
            // 第二次右键：尝试建立连接
            WireEndpoint start = new WireEndpoint(startPos, 0);
            WireEndpoint end = new WireEndpoint(targetPos, 0);

            WireNetworkManager manager = WireNetworkManager.get(level);
            WireConnection connection = manager.addConnection(level, start, end, wireType);

            // 清除起始位置
            stack.remove(ModDataComponents.WIRE_START_POS.get());

            // 通知客户端清除待连接状态
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer,
                        new PendingWireSyncPacket(BlockPos.ZERO, wireType, false));
            }

            if (connection != null) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.displayClientMessage(
                        Component.translatable("voltcraft.wire.connected",
                                startPos.toShortString(), targetPos.toShortString()),
                        true
                );
            } else {
                player.displayClientMessage(
                        Component.translatable("voltcraft.wire.failed"),
                        true
                );
            }

            return InteractionResultHolder.success(stack);
        }

        // 第一次右键：记录起始位置
        stack.set(ModDataComponents.WIRE_START_POS.get(), targetPos);
        player.displayClientMessage(
                Component.translatable("voltcraft.wire.start_set", targetPos.toShortString()),
                true
        );

        // 通知客户端设置待连接状态
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer,
                    new PendingWireSyncPacket(targetPos, wireType, true));
        }

        return InteractionResultHolder.success(stack);
    }

    /**
     * 每tick检测：如果玩家不再手持线圈，清除客户端的待连接状态。
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer player)) return;

        // 如果有起始位置但不在主手/副手，清除待连接状态
        BlockPos startPos = stack.get(ModDataComponents.WIRE_START_POS.get());
        if (startPos != null && !selected) {
            // 检查另一只手是否也是这个物品
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            boolean inOtherHand = (mainHand.getItem() instanceof WireCoilItem && slot != player.getInventory().selected)
                    || (offHand.getItem() instanceof WireCoilItem && slot != 40);

            if (!inOtherHand) {
                // 不在任何手中，清除待连接状态
                PacketDistributor.sendToPlayer(player,
                        new PendingWireSyncPacket(BlockPos.ZERO, wireType, false));
            }
        }

        // 如果在手中且有起始位置，确保客户端状态同步
        if (selected && startPos != null) {
            PacketDistributor.sendToPlayer(player,
                    new PendingWireSyncPacket(startPos, wireType, true));
        }
    }

    @Nullable
    private BlockPos getTargetBlockPos(Player player) {
        HitResult hitResult = player.pick(6.0, 0.0f, false);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            return blockHit.getBlockPos();
        }
        return null;
    }
}
