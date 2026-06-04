package com.voltcraft.integration.emi;

import com.voltcraft.VoltCraft;
import com.voltcraft.registry.ModItems;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class ElectrolyzerEmiRecipe implements EmiRecipe {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "electrolyzer");

    private final EmiIngredient input;
    private final EmiStack output;

    public ElectrolyzerEmiRecipe(EmiIngredient input, EmiStack output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return VoltCraftEmiPlugin.ELECTROLYZER_CATEGORY;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of(input);
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(output);
    }

    @Override
    public int getDisplayWidth() {
        return 76;
    }

    @Override
    public int getDisplayHeight() {
        return 18;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 24, 0);
        widgets.addSlot(input, 0, 0);
        widgets.addSlot(output, 58, 0);
    }

    public static List<EmiRecipe> getRecipes() {
        List<EmiRecipe> recipes = new ArrayList<>();
        recipes.add(new ElectrolyzerEmiRecipe(EmiStack.of(net.minecraft.world.item.Items.WATER_BUCKET), EmiStack.of(ModItems.LITHIUM_INGOT.get(), 1)));
        recipes.add(new ElectrolyzerEmiRecipe(EmiStack.of(ModItems.BRINE_BUCKET.get()), EmiStack.of(ModItems.SODIUM_INGOT.get(), 2)));
        return recipes;
    }
}
