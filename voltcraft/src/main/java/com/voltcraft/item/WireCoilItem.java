package com.voltcraft.item;

import com.voltcraft.electric.Phase;
import com.voltcraft.electric.WireType;
import com.voltcraft.electric.wire.WireConnection;
import com.voltcraft.electric.wire.WireEndpoint;
import com.voltcraft.electric.wire.WireNetworkManager;
import com.voltcraft.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

        WireEndpoint target = getTargetEndpoint(player);
        if (target == null) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos startPos = stack.get(ModDataComponents.WIRE_START_POS.get());
        Integer startIndex = stack.get(ModDataComponents.WIRE_START_INDEX.get());

        if (startPos != null && startIndex != null) {
            WireEndpoint start = new WireEndpoint(startPos, startIndex);
            WireNetworkManager manager = WireNetworkManager.get(level);
            WireConnection connection = manager.addConnection(level, start, target, wireType);

            stack.remove(ModDataComponents.WIRE_START_POS.get());
            stack.remove(ModDataComponents.WIRE_START_INDEX.get());

            if (connection != null) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.displayClientMessage(
                        Component.translatable("voltcraft.wire.connected",
                                startPos.toShortString(), target.pos().toShortString()),
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

        stack.set(ModDataComponents.WIRE_START_POS.get(), target.pos());
        stack.set(ModDataComponents.WIRE_START_INDEX.get(), target.endpointIndex());
        player.displayClientMessage(
                Component.translatable("voltcraft.wire.start_set", target.pos().toShortString()),
                true
        );

        return InteractionResultHolder.success(stack);
    }

    @Nullable
    private WireEndpoint getTargetEndpoint(Player player) {
        HitResult hitResult = player.pick(6.0, 0.0f, false);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockHitResult blockHit = (BlockHitResult) hitResult;
        Direction side = blockHit.getDirection();
        BlockPos endpointPos = blockHit.getBlockPos().relative(side);
        int index = endpointIndex(side, blockHit);
        return new WireEndpoint(endpointPos, index, phaseFromEndpointIndex(index));
    }

    private int endpointIndex(Direction side, BlockHitResult hit) {
        double x = hit.getLocation().x - hit.getBlockPos().getX();
        double y = hit.getLocation().y - hit.getBlockPos().getY();
        double z = hit.getLocation().z - hit.getBlockPos().getZ();
        return switch (side) {
            case NORTH, SOUTH -> quadrant(x, y);
            case EAST, WEST -> quadrant(z, y);
            case UP, DOWN -> quadrant(x, z);
        };
    }

    private int quadrant(double horizontal, double vertical) {
        int right = horizontal >= 0.5 ? 1 : 0;
        int top = vertical >= 0.5 ? 2 : 0;
        return top + right;
    }

    private Phase phaseFromEndpointIndex(int endpointIndex) {
        return switch (endpointIndex) {
            case 0 -> Phase.LIVE;
            case 1 -> Phase.NEUTRAL;
            case 2 -> Phase.EARTH;
            default -> Phase.LIVE;
        };
    }
}
