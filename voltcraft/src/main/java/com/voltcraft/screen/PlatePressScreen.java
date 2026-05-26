package com.voltcraft.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.voltcraft.VoltCraft;
import com.voltcraft.menu.PlatePressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 制板机 UI 界面
 */
public class PlatePressScreen extends AbstractContainerScreen<PlatePressMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "textures/gui/plate_press.png");

    public PlatePressScreen(PlatePressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderProgressArrow(guiGraphics, x, y);
        renderEnergyBar(guiGraphics, x, y);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if (menu.getScaledProgress() > 0) {
            int progress = menu.getScaledProgress();
            guiGraphics.blit(TEXTURE,
                    x + 76, y + 35,
                    176, 0,
                    progress, 16);
        }
    }

    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        int energyHeight = menu.getScaledEnergy();
        if (energyHeight > 0) {
            guiGraphics.blit(TEXTURE,
                    x + 8, y + 16 + (46 - energyHeight),
                    176, 16 + (46 - energyHeight),
                    16, energyHeight);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }
}
