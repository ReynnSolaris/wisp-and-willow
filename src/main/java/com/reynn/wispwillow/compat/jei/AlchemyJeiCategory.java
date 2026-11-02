package com.reynn.wispwillow.compat.jei;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.alchemy.AlchemyRecipe;
import com.reynn.wispwillow.client.goggles.GoggleText;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.RecipeRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.function.Supplier;

import static com.reynn.wispwillow.compat.jei.JeiSlots.PAD;
import static com.reynn.wispwillow.compat.jei.JeiSlots.SLOT;

/**
 * One JEI page per cauldron brew: the reagents and the optional liquid that go in, the charge
 * it costs, and whatever comes out on either side.
 *
 * <p>The fluids get real slots, since JEI's fluid slots carry their amount in the tooltip. The
 * amounts are still spelled out in text underneath, because a brew that wants 500 mB is
 * materially different from one that wants 4,000 and a tooltip is easy to miss.</p>
 */
final class AlchemyJeiCategory extends AbstractRecipeCategory<RecipeHolder<AlchemyRecipe>> {

    /** Deferred: the cauldron's recipe type is a registry entry, not a static constant. */
    static final Supplier<RecipeType<RecipeHolder<AlchemyRecipe>>> TYPE =
            RecipeType.createFromDeferredVanilla(RecipeRegistry.ALCHEMY_TYPE);

    private static final int TOP = 14;
    /** Wide enough for "Fluxia: 16,000 ƒ" and "In: 2,000 mB" to share a line. */
    private static final int WIDTH = 160;
    /** Two rows of reagents at most (the pot has four slots), then two lines of cost text. */
    private static final int HEIGHT = TOP + 2 * SLOT + 26;

    AlchemyJeiCategory(IGuiHelper gui) {
        super(TYPE.get(), Component.translatable("emi.category." + WispWillow.MODID + ".alchemy"),
                gui.createDrawableItemLike(BlockRegistry.ALCHEMICAL_CAULDRON.get()), WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AlchemyRecipe> holder, IFocusGroup focuses) {
        AlchemyRecipe recipe = holder.value();
        int i = 0;
        for (SizedIngredient ing : recipe.ingredients()) {
            builder.addInputSlot(PAD + 1 + (i % 4) * SLOT, TOP + 1 + (i / 4) * SLOT)
                    .setStandardSlotBackground()
                    .addItemStacks(JeiSlots.stacks(ing.ingredient(), ing.count()));
            i++;
        }
        FluidStack fluidIn = recipe.fluidInput();
        if (!fluidIn.isEmpty()) {
            builder.addInputSlot(PAD + 1 + (i % 4) * SLOT, TOP + 1 + (i / 4) * SLOT)
                    .setStandardSlotBackground()
                    .setFluidRenderer(fluidIn.getAmount(), false, 16, 16)
                    .addFluidStack(fluidIn.getFluid(), fluidIn.getAmount());
        }

        int out = 0;
        if (!recipe.result().isEmpty()) {
            builder.addOutputSlot(WIDTH - SLOT - PAD + 1, TOP + 1 + out++ * SLOT)
                    .setOutputSlotBackground()
                    .addItemStack(recipe.result());
        }
        FluidStack fluidOut = recipe.fluidResult();
        if (!fluidOut.isEmpty()) {
            builder.addOutputSlot(WIDTH - SLOT - PAD + 1, TOP + 1 + out * SLOT)
                    .setOutputSlotBackground()
                    .setFluidRenderer(fluidOut.getAmount(), false, 16, 16)
                    .addFluidStack(fluidOut.getFluid(), fluidOut.getAmount());
        }
    }

    @Override
    public void draw(RecipeHolder<AlchemyRecipe> holder, IRecipeSlotsView slots, GuiGraphics g,
                     double mouseX, double mouseY) {
        AlchemyRecipe recipe = holder.value();
        JeiSlots.text(g, Component.translatable("emi.wispwillow.alchemy.title")
                .withStyle(ChatFormatting.DARK_PURPLE), PAD, 0);

        int textY = HEIGHT - 20;
        JeiSlots.text(g, Component.translatable("emi.wispwillow.alchemy.cost",
                GoggleText.num(recipe.fluxia())).withStyle(ChatFormatting.DARK_AQUA), PAD, textY);
        JeiSlots.text(g, Component.translatable("emi.wispwillow.alchemy.time",
                GoggleText.seconds(recipe.ticks())).withStyle(ChatFormatting.DARK_GRAY), PAD, textY + 10);

        fluidNote(g, recipe.fluidInput(), "emi.wispwillow.alchemy.fluid_in", textY);
        fluidNote(g, recipe.fluidResult(), "emi.wispwillow.alchemy.fluid_out", textY + 10);
    }

    /** Right-aligned amount for a fluid, so the volume reads without hovering. */
    private static void fluidNote(GuiGraphics g, FluidStack fluid, String key, int y) {
        if (fluid.isEmpty()) return;
        JeiSlots.textRight(g, Component.translatable(key, GoggleText.num(fluid.getAmount()))
                .withStyle(ChatFormatting.DARK_AQUA), WIDTH - PAD, y);
    }
}
