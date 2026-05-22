package com.voltcraft.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.voltcraft.VoltCraft;
import com.voltcraft.menu.ElectrolyzerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 水解槽 UI 界面
 */
public class ElectrolyzerScreen extends AbstractContainerScreen<ElectrolyzerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "textures/gui/electrolyzer.png");

    public ElectrolyzerScreen(ElectrolyzerMenu menu, Inventory inventory, Component title) {
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

        // 绘制背景
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // 绘制进度箭头
        renderProgressArrow(guiGraphics, x, y);

        // 绘制能量条
        renderEnergyBar(guiGraphics, x, y);
    }

    /**
     * 绘制进度箭头
     */
    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if (menu.getScaledProgress() > 0) {
            int progress = menu.getScaledProgress();
            // 箭头位置：从 (76, 35) 开始，宽度 24 像素
            guiGraphics.blit(TEXTURE,
                    x + 76, y + 35,
                    176, 0,  // 纹理中的箭头位置
                    progress, 16);  // 箭头宽度和高度
        }
    }

    /**
     * 绘制能量条
     */
    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        int energyHeight = menu.getScaledEnergy();
        if (energyHeight > 0) {
            // 能量条位置：从 (8, 16) 开始，高度从底部向上绘制
            guiGraphics.blit(TEXTURE,
                    x + 8, y + 16 + (46 - energyHeight),
                    176, 16 + (46 - energyHeight),  // 纹理中的能量条位置
                    16, energyHeight);  // 宽度和高度
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
        // 绘制标题
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        // 绘制玩家背包标题
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }
}
