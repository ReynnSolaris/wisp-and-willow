package com.reynn.wispwillow.compat.jei;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.infusion.InfusionRecipe;
import com.reynn.wispwillow.registry.BlockRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

import static com.reynn.wispwillow.compat.jei.JeiSlots.PAD;
import static com.reynn.wispwillow.compat.jei.JeiSlots.SLOT;

/** One JEI page per Fluxia Infusion: what goes in the pool, the charge, what comes out. */
final class InfusionJeiCategory extends AbstractRecipeCategory<RecipeHolder<InfusionRecipe>> {

    static final RecipeType<RecipeHolder<InfusionRecipe>> TYPE = RecipeType.createFromVanilla(InfusionRecipe.TYPE);

    private static final int PER_ROW = 4;
    private static final int TOP = 14;
    private static final int WIDTH = 132;
    /** Two rows of reagents covers every infusion; taller ones still draw, just closer to the cost line. */
    private static final int HEIGHT = TOP + 2 * SLOT + 18;

    InfusionJeiCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("emi.category." + WispWillow.MODID + ".infusion"),
                gui.createDrawableItemLike(BlockRegistry.MANA_POOL.get()), WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<InfusionRecipe> holder, IFocusGroup focuses) {
        InfusionRecipe recipe = holder.value();
        List<List<ItemStack>> inputs = JeiSlots.collapse(recipe.inputs());
        for (int i = 0; i < inputs.size(); i++) {
            builder.addInputSlot(PAD + 1 + (i % PER_ROW) * SLOT, TOP + 1 + (i / PER_ROW) * SLOT)
                    .setStandardSlotBackground()
                    .addItemStacks(inputs.get(i));
        }
        int lastRow = JeiSlots.rows(inputs.size(), PER_ROW) - 1;
        builder.addOutputSlot(WIDTH - SLOT - PAD + 1, TOP + 1 + lastRow * SLOT)
                .setOutputSlotBackground()
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<InfusionRecipe> holder, IRecipeSlotsView slots, GuiGraphics g,
                     double mouseX, double mouseY) {
        JeiSlots.text(g, Component.translatable("emi.wispwillow.infusion.title")
                .withStyle(ChatFormatting.DARK_PURPLE), PAD, 0);
        JeiSlots.text(g, Component.translatable("emi.wispwillow.ritual.cost", holder.value().fluxia())
                .withStyle(ChatFormatting.DARK_AQUA), PAD, HEIGHT - 10);
    }
}
