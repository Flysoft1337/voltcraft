package com.voltcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.voltcraft.VoltCraft;
import com.voltcraft.electric.WireType;
import com.voltcraft.network.WireConnectionSyncPacket.WireConnectionData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * 线缆渲染器。
 * 负责渲染悬链线效果的线缆连接。
 */
@EventBusSubscriber(modid = VoltCraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class WireRenderer {

    private static final double SAG = 0.1; // 线缆下垂程度

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        if (Minecraft.getInstance().level == null) {
            return;
        }

        var connections = WireRendererState.getConnections();
        if (connections.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

        Vec3 cameraPos = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        for (WireConnectionData data : connections) {
            renderWire(poseStack, consumer, data);
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }

    private static void renderWire(PoseStack poseStack, VertexConsumer consumer, WireConnectionData data) {
        Vec3 start = Vec3.atCenterOf(data.startPos());
        Vec3 end = Vec3.atCenterOf(data.endPos());

        int color = getWireColor(data.wireType());
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = 255;

        int segments = 16;
        Vec3[] points = calculateCatenary(start, end, SAG, segments);

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

            // 抛物线近似悬链线
            double sagAmount = sag * horizontalDistance * t * (1 - t);
            double y = start.y + dy * t - sagAmount;

            points[i] = new Vec3(x, y, z);
        }

        return points;
    }

    private static int getWireColor(WireType wireType) {
        return switch (wireType) {
            case COPPER -> 0xB87333;  // 铜色
            case TIN -> 0x808080;     // 锡色
            case SILVER -> 0xC0C0C0;  // 银色
        };
    }
}
