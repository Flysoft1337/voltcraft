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
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class PlatePressEmiRecipe implements EmiRecipe {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "plate_press");

    private final EmiIngredient input;
    private final EmiStack output;

    public PlatePressEmiRecipe(EmiIngredient input, EmiStack output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return VoltCraftEmiPlugin.PLATE_PRESS_CATEGORY;
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
        recipes.add(new PlatePressEmiRecipe(EmiStack.of(Items.COPPER_INGOT), EmiStack.of(ModItems.COPPER_PLATE.get())));
        recipes.add(new PlatePressEmiRecipe(EmiStack.of(Items.IRON_INGOT), EmiStack.of(ModItems.IRON_PLATE.get())));
        recipes.add(new PlatePressEmiRecipe(EmiStack.of(ModItems.ZINC_INGOT.get()), EmiStack.of(ModItems.ZINC_PLATE.get())));
        recipes.add(new PlatePressEmiRecipe(EmiStack.of(ModItems.MANGANESE_INGOT.get()), EmiStack.of(ModItems.MANGANESE_PLATE.get())));
        recipes.add(new PlatePressEmiRecipe(EmiStack.of(ModItems.NICKEL_INGOT.get()), EmiStack.of(ModItems.NICKEL_PLATE.get())));
        recipes.add(new PlatePressEmiRecipe(EmiStack.of(ModItems.LEAD_INGOT.get()), EmiStack.of(ModItems.LEAD_PLATE.get())));
        recipes.add(new PlatePressEmiRecipe(EmiStack.of(ModItems.SILVER_INGOT.get()), EmiStack.of(ModItems.SILVER_PLATE.get())));
        recipes.add(new PlatePressEmiRecipe(EmiStack.of(ModItems.TIN_INGOT.get()), EmiStack.of(ModItems.TIN_PLATE.get())));
        recipes.add(new PlatePressEmiRecipe(EmiStack.of(ModItems.IRISITE_INGOT.get()), EmiStack.of(ModItems.IRISITE_PLATE.get())));
        return recipes;
    }
}
