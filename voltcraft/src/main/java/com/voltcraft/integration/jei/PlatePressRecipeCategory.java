package com.voltcraft.integration.jei;

import com.voltcraft.VoltCraft;
import com.voltcraft.registry.ModBlocks;
import com.voltcraft.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class PlatePressRecipeCategory implements IRecipeCategory<PlatePressRecipeCategory.Recipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "plate_press");
    public static final RecipeType<Recipe> RECIPE_TYPE = new RecipeType<>(UID, Recipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public PlatePressRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(ResourceLocation.fromNamespaceAndPath("jei", "textures/gui/gui_vanilla.png"), 0, 220, 82, 34);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.PLATE_PRESS.get()));
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.voltcraft.jei.category.plate_press");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 9)
                .addItemStack(recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 61, 9)
                .addItemStack(recipe.output());
    }

    public static List<Recipe> getRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(new Recipe(new ItemStack(Items.COPPER_INGOT), new ItemStack(ModItems.COPPER_PLATE.get())));
        recipes.add(new Recipe(new ItemStack(Items.IRON_INGOT), new ItemStack(ModItems.IRON_PLATE.get())));
        recipes.add(new Recipe(new ItemStack(ModItems.ZINC_INGOT.get()), new ItemStack(ModItems.ZINC_PLATE.get())));
        recipes.add(new Recipe(new ItemStack(ModItems.MANGANESE_INGOT.get()), new ItemStack(ModItems.MANGANESE_PLATE.get())));
        recipes.add(new Recipe(new ItemStack(ModItems.NICKEL_INGOT.get()), new ItemStack(ModItems.NICKEL_PLATE.get())));
        recipes.add(new Recipe(new ItemStack(ModItems.LEAD_INGOT.get()), new ItemStack(ModItems.LEAD_PLATE.get())));
        recipes.add(new Recipe(new ItemStack(ModItems.SILVER_INGOT.get()), new ItemStack(ModItems.SILVER_PLATE.get())));
        recipes.add(new Recipe(new ItemStack(ModItems.TIN_INGOT.get()), new ItemStack(ModItems.TIN_PLATE.get())));
        recipes.add(new Recipe(new ItemStack(ModItems.IRISITE_INGOT.get()), new ItemStack(ModItems.IRISITE_PLATE.get())));
        return recipes;
    }

    public record Recipe(ItemStack input, ItemStack output) {
    }
}
