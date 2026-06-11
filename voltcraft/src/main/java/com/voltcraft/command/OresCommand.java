package com.voltcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调试命令：扫描周围矿石并用粒子标记。
 * 用法：/voltcraft ores [半径] [tick数]
 */
public class OresCommand {

    private static final Map<Block, Vector3f> ORE_COLORS = new HashMap<>();

    static {
        // VoltCraft 矿石 - 每种不同颜色
        registerColor("voltcraft:argentite_ore", 1.0f, 0.84f, 0.0f);       // 金色
        registerColor("voltcraft:deepslate_argentite_ore", 1.0f, 0.84f, 0.0f);
        registerColor("voltcraft:cassiterite_ore", 0.75f, 0.75f, 0.75f);   // 银灰
        registerColor("voltcraft:deepslate_cassiterite_ore", 0.75f, 0.75f, 0.75f);
        registerColor("voltcraft:cerussite_ore", 0.6f, 0.6f, 0.6f);        // 深灰
        registerColor("voltcraft:deepslate_cerussite_ore", 0.6f, 0.6f, 0.6f);
        registerColor("voltcraft:garnierite_ore", 0.0f, 1.0f, 0.5f);       // 绿色
        registerColor("voltcraft:deepslate_garnierite_ore", 0.0f, 1.0f, 0.5f);
        registerColor("voltcraft:hemimorphite_ore", 0.4f, 0.8f, 1.0f);     // 天蓝
        registerColor("voltcraft:deepslate_hemimorphite_ore", 0.4f, 0.8f, 1.0f);
        registerColor("voltcraft:rhodonite_ore", 1.0f, 0.4f, 0.7f);        // 粉红
        registerColor("voltcraft:deepslate_rhodonite_ore", 1.0f, 0.4f, 0.7f);
        registerColor("voltcraft:spodumene_ore", 0.8f, 0.5f, 1.0f);        // 紫色
        registerColor("voltcraft:deepslate_spodumene_ore", 0.8f, 0.5f, 1.0f);
        registerColor("voltcraft:irisite_ore", 0.5f, 1.0f, 0.85f);         // 碧绿
    }

    private static void registerColor(String blockId, float r, float g, float b) {
        Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.parse(blockId));
        if (block != Blocks.AIR) {
            ORE_COLORS.put(block, new Vector3f(r, g, b));
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("voltcraft")
                .then(Commands.literal("ores")
                        .executes(ctx -> scanOres(ctx, 32, 200))
                        .then(Commands.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(8, 128))
                                .executes(ctx -> scanOres(ctx, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "radius"), 200))
                                .then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(20, 6000))
                                        .executes(ctx -> scanOres(ctx,
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "radius"),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "ticks")))))));
    }

    private static int scanOres(CommandContext<CommandSourceStack> ctx, int radius, int durationTicks) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();

        Map<Block, List<BlockPos>> found = new HashMap<>();

        // 扫描区域
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    Block block = state.getBlock();
                    if (ORE_COLORS.containsKey(block)) {
                        found.computeIfAbsent(block, k -> new ArrayList<>()).add(pos.immutable());
                    }
                }
            }
        }

        // 输出结果
        int total = found.values().stream().mapToInt(List::size).sum();

        if (found.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e半径 " + radius + " 内未发现矿石"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§a扫描完成，发现 §f" + total + " §a个矿石方块："), false);
            for (var entry : found.entrySet()) {
                String blockName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(entry.getKey()).toString();
                int count = entry.getValue().size();
                Vector3f color = ORE_COLORS.get(entry.getKey());
                String colorName = getColorName(color);
                source.sendSuccess(() -> Component.literal("  §7- " + colorName + blockName + " §7× " + count), false);
            }
        }

        // 启动粒子标记任务
        if (!found.isEmpty()) {
            int finalDuration = durationTicks;
            source.sendSuccess(() -> Component.literal("§b粒子标记持续 " + (finalDuration / 20) + " 秒"), false);

            // 为每个矿石方块安排粒子任务
            List<Map.Entry<Block, List<BlockPos>>> entries = new ArrayList<>(found.entrySet());
            level.getServer().tell(new net.minecraft.server.TickTask(
                    level.getServer().getTickCount() + 1,
                    () -> showParticles(level, entries, finalDuration)
            ));
        }

        return 1;
    }

    private static void showParticles(ServerLevel level, List<Map.Entry<Block, List<BlockPos>>> entries, int remainingTicks) {
        if (remainingTicks <= 0) return;

        for (var entry : entries) {
            Vector3f color = ORE_COLORS.get(entry.getKey());
            if (color == null) continue;
            DustParticleOptions particle = new DustParticleOptions(color, 1.5f);

            for (BlockPos pos : entry.getValue()) {
                // 每个矿石方块的8个角显示粒子
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        for (int dz = 0; dz <= 1; dz++) {
                            if (remainingTicks % 5 == 0) { // 每5tick显示一次，减少性能开销
                                level.sendParticles(particle,
                                        pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz,
                                        1, 0, 0, 0, 0);
                            }
                        }
                    }
                }
            }
        }

        // 安排下一次
        int next = remainingTicks - 1;
        if (next > 0) {
            level.getServer().tell(new net.minecraft.server.TickTask(
                    level.getServer().getTickCount() + 1,
                    () -> showParticles(level, entries, next)
            ));
        }
    }

    private static String getColorName(Vector3f color) {
        if (color.x > 0.9f && color.y > 0.8f && color.z < 0.1f) return "§6"; // 金色
        if (color.x > 0.9f && color.y < 0.5f && color.z > 0.6f) return "§d"; // 粉红
        if (color.x < 0.1f && color.y > 0.9f && color.z > 0.4f) return "§a"; // 绿色
        if (color.x < 0.5f && color.y > 0.7f && color.z > 0.9f) return "§b"; // 天蓝
        if (color.x > 0.7f && color.y < 0.6f && color.z > 0.9f) return "§5"; // 紫色
        if (color.x > 0.4f && color.y > 0.9f && color.z > 0.8f) return "§e"; // 碧绿
        return "§f"; // 白色
    }
}
