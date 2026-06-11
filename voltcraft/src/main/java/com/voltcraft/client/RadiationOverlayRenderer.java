package com.voltcraft.client;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.voltcraft.effect.RadiationEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * 辐射雪花屏叠加层渲染器
 * 效果类似冻伤叠加层，但屏幕中心也有雪花噪点
 * 随着最大生命值降低，噪点越来越密集、越来越不透明
 */
@EventBusSubscriber(modid = "voltcraft", value = Dist.CLIENT)
public class RadiationOverlayRenderer {

    private static final ResourceLocation CAMERA_OVERLAYS =
            ResourceLocation.withDefaultNamespace("camera_overlays");

    private static final int NOISE_WIDTH = 128;
    private static final int NOISE_HEIGHT = 72;

    private static DynamicTexture noiseTexture;
    private static ResourceLocation noiseTextureLocation;
    private static int lastUpdateTick = -1;

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        // 只在camera_overlays层之后渲染一次
        if (!CAMERA_OVERLAYS.equals(event.getName())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        if (!mc.player.hasEffect(com.voltcraft.effect.ModEffects.RADIATION)) return;

        var maxHealth = Attributes.MAX_HEALTH;
        if (maxHealth == null) return;

        var attrInstance = mc.player.getAttribute(maxHealth);
        AttributeModifier modifier = attrInstance.getModifier(RadiationEffect.MODIFIER_ID);
        if (modifier == null) return;

        double lost = -modifier.amount();
        if (lost <= 0) return;

        // 强度：每损失1HP增加约0.08，最大1.0
        float intensity = (float) Math.min(lost * 0.08, 1.0);

        GuiGraphics guiGraphics = event.getGuiGraphics();

        // 每2tick更新一次噪点纹理
        if (mc.player.tickCount != lastUpdateTick && mc.player.tickCount % 2 == 0) {
            updateNoiseTexture(mc.player.tickCount);
            lastUpdateTick = mc.player.tickCount;
        }

        renderStaticOverlay(guiGraphics, intensity);
    }

    private static void updateNoiseTexture(int tick) {
        if (noiseTexture == null) {
            noiseTexture = new DynamicTexture(NOISE_WIDTH, NOISE_HEIGHT, true);
            noiseTextureLocation = Minecraft.getInstance()
                    .getTextureManager()
                    .register("radiation_noise", noiseTexture);
        }

        NativeImage image = noiseTexture.getPixels();
        if (image == null) return;

        long seed = tick * 3417L + 83921L;

        for (int y = 0; y < NOISE_HEIGHT; y++) {
            for (int x = 0; x < NOISE_WIDTH; x++) {
                seed = seed * 6364136223846793005L + 1442695040888963407L;
                int rand = (int) (seed >>> 33);

                // 灰度噪点：少量亮像素形成雪花，大部分是暗的
                int gray = rand & 0xFF;
                int value = (rand % 5 == 0) ? (180 + (gray % 76)) : (gray % 40);

                int pixel = 0xFF000000 | (value << 16) | (value << 8) | value;
                image.setPixelRGBA(x, y, pixel);
            }
        }

        noiseTexture.upload();
    }

    private static void renderStaticOverlay(GuiGraphics guiGraphics, float intensity) {
        if (noiseTextureLocation == null) return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        // alpha随强度增加，从0.15到0.7
        float alpha = 0.15f + intensity * 0.55f;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        RenderSystem.setShaderTexture(0, noiseTextureLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MIN_FILTER, GlConst.GL_NEAREST);
        RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MAG_FILTER, GlConst.GL_NEAREST);

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.blit(noiseTextureLocation, 0, 0, -90, 0.0F, 0.0F,
                width, height, width, height);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
