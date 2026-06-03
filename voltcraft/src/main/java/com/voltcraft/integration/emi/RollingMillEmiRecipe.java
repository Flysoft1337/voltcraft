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

public class RollingMillEmiRecipe implements EmiRecipe {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "rolling_mill");

    private final EmiIngredient input;
    private final EmiStack output;

    public RollingMillEmiRecipe(EmiIngredient input, EmiStack output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return VoltCraftEmiPlugin.ROLLING_MILL_CATEGORY;
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
        recipes.add(new RollingMillEmiRecipe(EmiStack.of(ModItems.COPPER_PLATE.get()), EmiStack.of(ModItems.COPPER_WIRE.get())));
        recipes.add(new RollingMillEmiRecipe(EmiStack.of(ModItems.TIN_PLATE.get()), EmiStack.of(ModItems.TIN_WIRE.get())));
        recipes.add(new RollingMillEmiRecipe(EmiStack.of(ModItems.SILVER_PLATE.get()), EmiStack.of(ModItems.SILVER_WIRE.get())));
        recipes.add(new RollingMillEmiRecipe(EmiStack.of(ModItems.IRON_PLATE.get()), EmiStack.of(ModItems.IRON_WIRE.get())));
        recipes.add(new RollingMillEmiRecipe(EmiStack.of(ModItems.ZINC_PLATE.get()), EmiStack.of(ModItems.ZINC_WIRE.get())));
        recipes.add(new RollingMillEmiRecipe(EmiStack.of(ModItems.LEAD_PLATE.get()), EmiStack.of(ModItems.LEAD_WIRE.get())));
        recipes.add(new RollingMillEmiRecipe(EmiStack.of(ModItems.NICKEL_PLATE.get()), EmiStack.of(ModItems.NICKEL_WIRE.get())));
        recipes.add(new RollingMillEmiRecipe(EmiStack.of(ModItems.MANGANESE_PLATE.get()), EmiStack.of(ModItems.MANGANESE_WIRE.get())));
        recipes.add(new RollingMillEmiRecipe(EmiStack.of(ModItems.IRISITE_PLATE.get()), EmiStack.of(ModItems.IRISITE_WIRE.get())));
        return recipes;
    }
}
