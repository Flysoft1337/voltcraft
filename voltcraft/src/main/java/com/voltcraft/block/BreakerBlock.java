package com.voltcraft.block;

import com.voltcraft.blockentity.BreakerBlockEntity;
import com.voltcraft.electric.CableTier;
import com.voltcraft.electric.protection.BreakerState;
import com.voltcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 空气开关：串联在电缆上，过载/短路时跳闸。
 *
 * 拓扑：本方块本身不是电缆；FACING 方向和反方向各连一根电缆，
 * 把两侧网络桥接起来。CLOSED 时桥接，TRIPPED 时切断。
 *
 * 玩家右键已跳闸的空开 → 合闸恢复。
 */
public class BreakerBlock extends Block implements EntityBlock {

    public static final EnumProperty<BreakerState> STATE = EnumProperty.create("state", BreakerState.class);
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;

    private final CableTier tier;

    public BreakerBlock(CableTier tier, BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(STATE, BreakerState.CLOSED));
    }

    public CableTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(STATE, BreakerState.CLOSED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BreakerBlockEntity(ModBlockEntities.BREAKER.get(), pos, state, tier);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return type == ModBlockEntities.BREAKER.get()
                ? (lvl, pos, st, be) -> ((BreakerBlockEntity) be).serverTick()
                : null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof BreakerBlockEntity be) {
            BreakerState cur = state.getValue(STATE);
            switch (cur) {
                case CLOSED -> {
                    // 玩家手动拉闸
                    be.setState(BreakerState.OPEN_MANUAL);
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, 0.7f);
                    if (player instanceof ServerPlayer sp) {
                        sp.displayClientMessage(Component.translatable("voltcraft.breaker.open_manual"), true);
                    }
                }
                case OPEN_MANUAL -> {
                    // 玩家手动合闸
                    be.reset();
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, 1.4f);
                    if (player instanceof ServerPlayer sp) {
                        sp.displayClientMessage(Component.translatable("voltcraft.breaker.close_manual"), true);
                    }
                }
                default -> {
                    // 故障跳闸：reset 重新合闸
                    be.reset();
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, 1.4f);
                    if (player instanceof ServerPlayer sp) {
                        sp.displayClientMessage(Component.translatable("voltcraft.breaker.reset"), true);
                    }
                }
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
