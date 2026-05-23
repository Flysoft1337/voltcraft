package com.voltcraft.item;

import com.voltcraft.electric.WireType;
import com.voltcraft.electric.wire.WireConnection;
import com.voltcraft.electric.wire.WireEndpoint;
import com.voltcraft.electric.wire.WireNetworkManager;
import com.voltcraft.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 线圈物品。
 * 右键第一个点记录位置，右键第二个点建立连接。
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

        // 获取玩家看向的方块位置
        BlockPos targetPos = getTargetBlockPos(player);
        if (targetPos == null) {
            return InteractionResultHolder.pass(stack);
        }

        // 检查是否已有起始位置
        BlockPos startPos = stack.get(ModDataComponents.WIRE_START_POS.get());

        if (startPos != null) {
            // 第二次右键：尝试建立连接
            WireEndpoint start = new WireEndpoint(startPos, 0);
            WireEndpoint end = new WireEndpoint(targetPos, 0);

            WireNetworkManager manager = WireNetworkManager.get(level);
            WireConnection connection = manager.addConnection(level, start, end, wireType);

            // 清除起始位置
            stack.remove(ModDataComponents.WIRE_START_POS.get());

            if (connection != null) {
                // 连接成功
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.displayClientMessage(
                        Component.translatable("voltcraft.wire.connected",
                                startPos.toShortString(), targetPos.toShortString()),
                        true
                );
            } else {
                // 连接失败（距离超限或已存在）
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

        return InteractionResultHolder.success(stack);
    }

    @Nullable
    private BlockPos getTargetBlockPos(Player player) {
        // 使用射线追踪获取玩家看向的方块位置，最大距离 6 格
        HitResult hitResult = player.pick(6.0, 0.0f, false);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            return blockHit.getBlockPos();
        }
        return null;
    }
}
