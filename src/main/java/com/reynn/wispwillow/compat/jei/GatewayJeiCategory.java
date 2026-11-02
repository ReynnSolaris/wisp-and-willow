package com.reynn.wispwillow.compat.jei;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.gateway.GatewayRecipe;
import com.reynn.wispwillow.items.SoulPactItem;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeHolder;

import static com.reynn.wispwillow.compat.jei.JeiSlots.PAD;
import static com.reynn.wispwillow.compat.jei.JeiSlots.SLOT;

/**
 * One JEI page per Soul Gateway casting: the sealed pact that goes in (and the creature
 * it came from), the charge, and the familiar that steps out.
 */
final class GatewayJeiCategory extends AbstractRecipeCategory<RecipeHolder<GatewayRecipe>> {

    static final RecipeType<RecipeHolder<GatewayRecipe>> TYPE = RecipeType.createFromVanilla(GatewayRecipe.TYPE);

    private static final int TOP = 14;
    private static final int WIDTH = 132;
    private static final int HEIGHT = TOP + SLOT + 24;

    GatewayJeiCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("emi.category." + WispWillow.MODID + ".gateway"),
                gui.createDrawableItemLike(BlockRegistry.GATEWAY_CORE.get()), WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<GatewayRecipe> holder, IFocusGroup focuses) {
        GatewayRecipe recipe = holder.value();

        ItemStack pact = new ItemStack(ItemRegistry.SEALED_SOUL_PACT.get());
        SoulPactItem.setSoul(pact, recipe.soul());
        builder.addInputSlot(PAD + 1, TOP + 1)
                .setStandardSlotBackground()
                .addItemStack(pact);

        // The creature the soul was taken from, for the eye: its vanilla egg, shown but not
        // an ingredient, so looking up "allay" in JEI does not claim it is consumed here.
        EntityType<?> source = recipe.sourceType();
        SpawnEggItem sourceEgg = source == null ? null : SpawnEggItem.byId(source);
        if (sourceEgg != null) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, PAD + 1 + SLOT + 6, TOP + 1)
                    .addItemStack(new ItemStack(sourceEgg));
        }

        // What comes out. A creature is shown as its own egg (a soul wisp if it has none),
        // then any items the casting drops, right to left from the edge.
        int x = WIDTH - SLOT - PAD + 1;
        if (recipe.summons().isPresent()) {
            EntityType<?> out = recipe.summons().get();
            SpawnEggItem egg = SpawnEggItem.byId(out);
            ItemStack icon = egg != null ? new ItemStack(egg) : new ItemStack(ItemRegistry.SOUL_WISP.get());
            icon.setCount(recipe.count());
            builder.addOutputSlot(x, TOP + 1).setOutputSlotBackground().addItemStack(icon);
            x -= SLOT + 2;
        }
        for (int i = recipe.results().size() - 1; i >= 0; i--) {
            builder.addOutputSlot(x, TOP + 1).setOutputSlotBackground().addItemStack(recipe.results().get(i).copy());
            x -= SLOT + 2;
        }
    }

    @Override
    public void draw(RecipeHolder<GatewayRecipe> holder, IRecipeSlotsView slots, GuiGraphics g,
                     double mouseX, double mouseY) {
        GatewayRecipe recipe = holder.value();
        JeiSlots.text(g, Component.translatable("emi.wispwillow.gateway.title")
                .withStyle(ChatFormatting.DARK_PURPLE), PAD, 0);
        JeiSlots.text(g, Component.translatable("emi.wispwillow.ritual.cost", JeiSlots.num(recipe.fluxia()))
                .withStyle(ChatFormatting.DARK_AQUA), PAD, HEIGHT - 20);
        MutableComponent what = recipe.summons()
                .map(out -> recipe.count() > 1
                        ? Component.translatable("emi.wispwillow.gateway.summons_count", recipe.count(), out.getDescription())
                        : Component.translatable("emi.wispwillow.gateway.summons", out.getDescription()))
                .orElseGet(() -> Component.translatable("emi.wispwillow.gateway.drops"));
        JeiSlots.text(g, what.withStyle(ChatFormatting.DARK_GRAY), PAD, HEIGHT - 10);
    }
}
