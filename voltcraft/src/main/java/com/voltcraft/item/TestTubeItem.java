package com.voltcraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * 试管 — 容量 250 mB（1/4 桶）
 */
public class TestTubeItem extends Item {

    public static final int CAPACITY = FluidType.BUCKET_VOLUME / 4; // 250 mB

    public TestTubeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        var fluid = FluidUtil.getFluidContained(stack);
        if (fluid.isPresent() && !fluid.get().isEmpty()) {
            return Component.literal(super.getName(stack).getString()
                    + " (" + fluid.get().getFluidType().getDescription().getString() + ")");
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);

        // Try picking up fluid from world
        var result = FluidUtil.tryPickUpFluid(stack, player, level, hit.getBlockPos(), hit.getDirection());
        if (result.isSuccess()) {
            return InteractionResultHolder.sidedSuccess(result.getResult(), level.isClientSide());
        }

        // Try placing fluid into world
        hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        for (var pos : new BlockPos[]{hit.getBlockPos(), hit.getBlockPos().relative(hit.getDirection())}) {
            if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
                return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
            }
        }

        return super.use(level, player, hand);
    }
}
