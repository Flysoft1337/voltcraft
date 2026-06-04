package com.voltcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.voltcraft.VoltCraft;
import com.voltcraft.electric.WireType;
import com.voltcraft.network.WireConnectionSyncPacket.WireConnectionData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * 线缆渲染器。
 * 负责渲染悬链线效果的线缆连接，以及待连接时的手持线缆和范围指示。
 */
@EventBusSubscriber(modid = VoltCraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class WireRenderer {

    private static final double SAG = 0.1;
    private static long lastOutOfRangeMessageTick = 0;
    private static boolean wasOutOfRange = false;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 cameraPos = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // 渲染已建立的连接
        var connections = WireRendererState.getConnections();
        if (!connections.isEmpty()) {
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
            for (WireConnectionData data : connections) {
                renderWire(poseStack, consumer, Vec3.atCenterOf(data.startPos()),
                        Vec3.atCenterOf(data.endPos()), getWireColor(data.wireType()), false);
            }
            bufferSource.endBatch(RenderType.lines());
        }

        // 渲染待连接的手持线缆
        if (WireRendererState.hasPendingWire()) {
            renderPendingWire(mc.player, poseStack, bufferSource);
        }

        poseStack.popPose();
    }

    private static void renderPendingWire(Player player, PoseStack poseStack,
                                           MultiBufferSource.BufferSource bufferSource) {
        BlockPos startPos = WireRendererState.getPendingStartPos();
        WireType wireType = WireRendererState.getPendingWireType();

        Vec3 start = Vec3.atCenterOf(startPos);
        // 手的位置：玩家眼睛前方稍微偏下
        Vec3 handPos = player.getPosition(1.0f).add(0, 1.2, 0);

        double distance = startPos.getCenter().distanceTo(handPos);
        boolean outOfRange = distance > wireType.maxDistance();

        // 渲染手持线缆
        int color = outOfRange ? 0xFF0000 : getWireColor(wireType); // 超出范围用红色
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        renderWire(poseStack, lineConsumer, Vec3.atCenterOf(startPos), handPos, color, true);
        bufferSource.endBatch(RenderType.lines());

        // 超出范围时渲染范围指示圆 + 显示提示
        if (outOfRange) {
            renderRangeCircle(poseStack, startPos, wireType.maxDistance());
            // 进入超出范围状态时显示一次提示
            if (!wasOutOfRange) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("voltcraft.wire.out_of_range"),
                        true
                );
                wasOutOfRange = true;
            }
        } else {
            wasOutOfRange = false;
        }
    }

    /**
     * 以起始点为中心，maxDistance为半径，渲染红色粒子圆（XZ平面）。
     */
    private static void renderRangeCircle(PoseStack poseStack, BlockPos center, int radius) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 centerVec = center.getCenter();
        int segments = 64;

        for (int i = 0; i < segments; i++) {
            double angle1 = 2 * Math.PI * i / segments;
            double angle2 = 2 * Math.PI * (i + 1) / segments;

            double x1 = centerVec.x + radius * Math.cos(angle1);
            double z1 = centerVec.z + radius * Math.sin(angle1);
            double x2 = centerVec.x + radius * Math.cos(angle2);
            double z2 = centerVec.z + radius * Math.sin(angle2);

            // 在圆周上每间隔几个位置生成粒子
            if (i % 2 == 0) {
                mc.level.addParticle(
                        net.minecraft.core.particles.DustParticleOptions.REDSTONE,
                        x1, centerVec.y, z1,
                        0, 0, 0
                );
            }
        }
    }

    private static void renderWire(PoseStack poseStack, VertexConsumer consumer,
                                    Vec3 start, Vec3 end, int color, boolean isPending) {

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = 255;

        int segments = isPending ? 8 : 16;
        Vec3[] points = calculateCatenary(start, end, isPending ? 0.05 : SAG, segments);

        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < points.length - 1; i++) {
            Vec3 p1 = points[i];
            Vec3 p2 = points[i + 1];

            consumer.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
                    .setColor(r, g, b, a)
                    .setNormal(0, 1, 0);
            consumer.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z)
                    .setColor(r, g, b, a)
                    .setNormal(0, 1, 0);
        }
    }

    private static Vec3[] calculateCatenary(Vec3 start, Vec3 end, double sag, int segments) {
        Vec3[] points = new Vec3[segments + 1];

        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;

            double x = start.x + dx * t;
            double z = start.z + dz * t;

            double sagAmount = sag * horizontalDistance * t * (1 - t);
            double y = start.y + dy * t - sagAmount;

            points[i] = new Vec3(x, y, z);
        }

        return points;
    }

    private static int getWireColor(WireType wireType) {
        return switch (wireType) {
            case COPPER -> 0xB87333;
            case TIN -> 0x808080;
            case SILVER -> 0xC0C0C0;
            case IRISITE -> 0x7FFFD4;
        };
    }
}
