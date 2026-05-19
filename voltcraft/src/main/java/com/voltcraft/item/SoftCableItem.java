package com.voltcraft.item;

import com.voltcraft.electric.CableTier;
import com.voltcraft.electric.Phase;
import com.voltcraft.electric.wire.WireAnchor;
import com.voltcraft.electric.wire.WireAnchorOwner;
import com.voltcraft.electric.wire.WireAnchorRef;
import com.voltcraft.entity.SoftCableEntity;
import com.voltcraft.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 软线物品：本身就是线，不是工具。
 *
 * 一个 SoftCableItem 实例绑定一个 {@link CableTier}（每个等级一个独立物品）。
 *
 * 流程：
 *   1. 手持本物品右键一个 {@link WireAnchorOwner} 方块 → 选距玩家瞄准点最近、空闲、
 *      tier 匹配的 anchor，记入 ItemStack CustomData
 *   2. 再次右键另一个 owner 的 anchor → 校验：
 *        * tier 一致（不一致 = 跨等级，禁止）
 *        * phase 一致
 *        * 一端 INPUT 一端 OUTPUT（同向不允许）
 *      通过则创建 SoftCableEntity，消耗 1 个物品，清空 CustomData
 *   3. 任意时刻右键空气 / 重新选择 → 不重置；要重置就丢弃整个 stack
 */
public class SoftCableItem extends Item {

    private static final String NBT_OWNER_X = "X";
    private static final String NBT_OWNER_Y = "Y";
    private static final String NBT_OWNER_Z = "Z";
    private static final String NBT_ANCHOR_IDX = "Idx";
    private static final String NBT_PHASE = "Phase";

    private final CableTier tier;

    public SoftCableItem(CableTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CableTier tier() { return tier; }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockPos pos = ctx.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof WireAnchorOwner owner)) return InteractionResult.PASS;

        // 选离瞄准点最近、空闲、且 tier 匹配的 anchor
        Vec3 hit = ctx.getClickLocation();
        WireAnchor target = pickAnchor(owner, pos, hit, tier);
        if (target == null) {
            player.displayClientMessage(
                    Component.translatable("voltcraft.soft_cable.no_anchor")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        ItemStack stack = ctx.getItemInHand();
        SelectedAnchor first = readSelected(stack);

        if (first == null) {
            writeSelected(stack, new SelectedAnchor(pos.immutable(), target.index(),
                    target.phase(), target.direction()));
            player.displayClientMessage(
                    Component.translatable("voltcraft.soft_cable.first_picked",
                            target.phase().shortLabel(),
                            target.direction().name(),
                            pos.toShortString())
                            .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }

        if (first.owner.equals(pos)) {
            player.displayClientMessage(
                    Component.translatable("voltcraft.soft_cable.same_block")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        if (first.phase != target.phase()) {
            player.displayClientMessage(
                    Component.translatable("voltcraft.soft_cable.phase_mismatch",
                            first.phase.shortLabel(), target.phase().shortLabel())
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        if (first.direction == target.direction()) {
            player.displayClientMessage(
                    Component.translatable("voltcraft.soft_cable.same_direction")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        BlockEntity beA = level.getBlockEntity(first.owner);
        if (!(beA instanceof WireAnchorOwner ownerA)) {
            clearSelected(stack);
            return InteractionResult.CONSUME;
        }
        WireAnchor anchorA = ownerA.anchor(first.anchorIndex);
        if (anchorA == null || !anchorA.isFree()) {
            clearSelected(stack);
            player.displayClientMessage(
                    Component.translatable("voltcraft.soft_cable.first_taken")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        if (anchorA.tier() != tier) {
            // 选过头了：第一端 anchor 不是本物品 tier。重置以让玩家重选。
            clearSelected(stack);
            return InteractionResult.CONSUME;
        }

        SoftCableEntity wire = SoftCableEntity.place(level, ModEntities.SOFT_CABLE.get(),
                new WireAnchorRef(first.owner, first.anchorIndex),
                new WireAnchorRef(pos, target.index()),
                first.phase, tier);
        if (!level.addFreshEntity(wire)) {
            clearSelected(stack);
            return InteractionResult.CONSUME;
        }
        anchorA.connect(wire.getId());
        target.connect(wire.getId());
        beA.setChanged();
        be.setChanged();
        clearSelected(stack);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        player.displayClientMessage(
                Component.translatable("voltcraft.soft_cable.connected",
                        first.phase.shortLabel())
                        .withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.CONSUME;
    }

    @Nullable
    private static WireAnchor pickAnchor(WireAnchorOwner owner, BlockPos pos, Vec3 aim, CableTier tier) {
        WireAnchor best = null;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < owner.anchorCount(); i++) {
            WireAnchor a = owner.anchor(i);
            if (a == null || !a.isFree()) continue;
            if (a.tier() != tier) continue;
            Vec3 worldPos = owner.anchorWorldPos(a, pos);
            double d = worldPos.distanceToSqr(aim);
            if (d < bestDist) {
                bestDist = d;
                best = a;
            }
        }
        return best;
    }

    @Nullable
    private static SelectedAnchor readSelected(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        if (!tag.contains(NBT_OWNER_X)) return null;
        Phase[] all = Phase.values();
        int phaseIdx = tag.getInt(NBT_PHASE);
        WireAnchor.Direction dir = tag.getBoolean("Out")
                ? WireAnchor.Direction.OUTPUT
                : WireAnchor.Direction.INPUT;
        return new SelectedAnchor(
                new BlockPos(tag.getInt(NBT_OWNER_X), tag.getInt(NBT_OWNER_Y), tag.getInt(NBT_OWNER_Z)),
                tag.getInt(NBT_ANCHOR_IDX),
                all[Math.min(phaseIdx, all.length - 1)],
                dir
        );
    }

    private static void writeSelected(ItemStack stack, SelectedAnchor sel) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(NBT_OWNER_X, sel.owner.getX());
        tag.putInt(NBT_OWNER_Y, sel.owner.getY());
        tag.putInt(NBT_OWNER_Z, sel.owner.getZ());
        tag.putInt(NBT_ANCHOR_IDX, sel.anchorIndex);
        tag.putInt(NBT_PHASE, sel.phase.ordinal());
        tag.putBoolean("Out", sel.direction == WireAnchor.Direction.OUTPUT);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void clearSelected(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    private record SelectedAnchor(BlockPos owner, int anchorIndex,
                                  Phase phase, WireAnchor.Direction direction) {}
}
