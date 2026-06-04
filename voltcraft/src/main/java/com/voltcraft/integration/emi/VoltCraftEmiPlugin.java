package com.voltcraft.integration.emi;

import com.voltcraft.VoltCraft;
import com.voltcraft.registry.ModBlocks;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;

@EmiEntrypoint
public class VoltCraftEmiPlugin implements EmiPlugin {

    public static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "emi_plugin");

    public static final EmiRecipeCategory PLATE_PRESS_CATEGORY =
            new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "plate_press"),
                    EmiStack.of(ModBlocks.PLATE_PRESS.get()));

    public static final EmiRecipeCategory ROLLING_MILL_CATEGORY =
            new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "rolling_mill"),
                    EmiStack.of(ModBlocks.ROLLING_MILL.get()));

    public static final EmiRecipeCategory ELECTROLYZER_CATEGORY =
            new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "electrolyzer"),
                    EmiStack.of(ModBlocks.ELECTROLYZER.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(PLATE_PRESS_CATEGORY);
        registry.addCategory(ROLLING_MILL_CATEGORY);
        registry.addCategory(ELECTROLYZER_CATEGORY);

        registry.addWorkstation(PLATE_PRESS_CATEGORY, EmiStack.of(ModBlocks.PLATE_PRESS.get()));
        registry.addWorkstation(ROLLING_MILL_CATEGORY, EmiStack.of(ModBlocks.ROLLING_MILL.get()));
        registry.addWorkstation(ELECTROLYZER_CATEGORY, EmiStack.of(ModBlocks.ELECTROLYZER.get()));

        // 注册配方
        PlatePressEmiRecipe.getRecipes().forEach(registry::addRecipe);
        RollingMillEmiRecipe.getRecipes().forEach(registry::addRecipe);
        ElectrolyzerEmiRecipe.getRecipes().forEach(registry::addRecipe);
    }
}
