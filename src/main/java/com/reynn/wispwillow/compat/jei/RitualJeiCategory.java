package com.reynn.wispwillow.compat.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.blocks.SigilType;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.ritual.RitualRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

import static com.reynn.wispwillow.compat.jei.JeiSlots.PAD;
import static com.reynn.wispwillow.compat.jei.JeiSlots.SLOT;

/**
 * One JEI page per ritual: the chalk circle drawn to scale, the reagents that must be lying
 * inside it, the Fluxia cost, and the result.
 */
final class RitualJeiCategory extends AbstractRecipeCategory<RecipeHolder<RitualRecipe>> {

    static final RecipeType<RecipeHolder<RitualRecipe>> TYPE = RecipeType.createFromVanilla(RitualRecipe.TYPE);

    /** Pixels per block in the circle preview. */
    private static final int CELL = 9;
    /** Largest circle drawn at full size before the cell shrinks. */
    private static final int MAX_PREVIEW_PX = 108;
    private static final int REAGENTS_PER_ROW = 4;
    private static final int GRID_X = PAD;
    private static final int GRID_Y = PAD + 10;

    private static final int WIDTH = GRID_X + MAX_PREVIEW_PX + PAD * 2 + REAGENTS_PER_ROW * SLOT + PAD;
    private static final int HEIGHT = GRID_Y + MAX_PREVIEW_PX + PAD + 12;

    RitualJeiCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("emi.category." + WispWillow.MODID + ".ritual"),
                gui.createDrawableItemLike(BlockRegistry.ALTAR.get()), WIDTH, HEIGHT);
    }

    /** Circle geometry for one recipe: radius, pixels per cell, total size. */
    private record Grid(int radius, int cellPx, int gridPx) {
        static Grid of(RitualRecipe recipe) {
            int radius = Math.max(1, recipe.maxRadius());
            int span = radius * 2 + 1;
            // Big circles (17x17) would otherwise blow past any sane page width.
            int cellPx = Math.max(3, Math.min(CELL, MAX_PREVIEW_PX / span));
            return new Grid(radius, cellPx, span * cellPx);
        }

        int column() { return GRID_X + gridPx + PAD * 2; }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RitualRecipe> holder, IFocusGroup focuses) {
        RitualRecipe recipe = holder.value();
        Grid grid = Grid.of(recipe);
        List<List<ItemStack>> reagents = JeiSlots.collapse(recipe.inputs());

        int col = grid.column();
        for (int i = 0; i < reagents.size(); i++) {
            builder.addInputSlot(col + 1 + (i % REAGENTS_PER_ROW) * SLOT, GRID_Y + 1 + (i / REAGENTS_PER_ROW) * SLOT)
                    .setStandardSlotBackground()
                    .addItemStacks(reagents.get(i));
        }
        recipe.resultItem().ifPresent(result ->
                builder.addOutputSlot(col + 1, resultY(reagents.size()) + 1)
                        .setOutputSlotBackground()
                        .addItemStack(result));
    }

    private static int resultY(int reagentCount) {
        return GRID_Y + JeiSlots.rows(reagentCount, REAGENTS_PER_ROW) * SLOT + 4;
    }

    @Override
    public void draw(RecipeHolder<RitualRecipe> holder, IRecipeSlotsView slots, GuiGraphics g,
                     double mouseX, double mouseY) {
        RitualRecipe recipe = holder.value();
        Grid grid = Grid.of(recipe);

        JeiSlots.text(g, Component.translatable(recipe.getId().toLanguageKey())
                .withStyle(ChatFormatting.DARK_PURPLE), PAD, 0);

        drawCircle(g, recipe, grid);

        // What the rite makes when there is no item to show in the result slot.
        int col = grid.column();
        int y = resultY(JeiSlots.collapse(recipe.inputs()).size()) + 5;
        if (recipe.resultItem().isEmpty()) {
            if (recipe.isImbuing()) {
                JeiSlots.text(g, Component.translatable("emi.wispwillow.ritual.imbues",
                        recipe.imbues().get().displayName()).withStyle(ChatFormatting.DARK_GRAY), col, y);
            } else if (recipe.isAwakening()) {
                JeiSlots.text(g, Component.translatable("emi.wispwillow.ritual.awakens")
                        .withStyle(s -> s.withColor(JeiSlots.DARK_GOLD)), col, y);
            } else {
                recipe.resultMobId().ifPresent(id -> {
                    EntityType<?> type = EntityType.byString(id.toString()).orElse(null);
                    Component name = type != null ? type.getDescription() : Component.literal(id.toString());
                    JeiSlots.text(g, Component.translatable("emi.wispwillow.ritual.summons",
                            recipe.resultMobCount(), name).withStyle(ChatFormatting.DARK_GRAY), col, y);
                });
            }
        }

        // Cost along the bottom, grid size bottom-right.
        int bottom = HEIGHT - 10;
        JeiSlots.text(g, Component.translatable("emi.wispwillow.ritual.cost",
                recipe.fluxiaRequired().orElse(0)).withStyle(ChatFormatting.DARK_AQUA), PAD, bottom);
        int span = grid.radius() * 2 + 1;
        JeiSlots.textRight(g, Component.translatable("emi.wispwillow.ritual.grid", span, span)
                .withStyle(ChatFormatting.DARK_GRAY), WIDTH - PAD, bottom);
    }

    /** Draws the chalk circle top-down, altar at the centre. */
    private static void drawCircle(GuiGraphics g, RitualRecipe recipe, Grid grid) {
        int cell = grid.cellPx();
        // Faint board so the circle reads as a diagram rather than floating glyphs.
        g.fill(GRID_X, GRID_Y, GRID_X + grid.gridPx(), GRID_Y + grid.gridPx(), 0x30000000);

        // Decorative marks are ghosted, which needs blending on for the plain textured blit.
        RenderSystem.enableBlend();
        for (RitualRecipe.Cell c : recipe.pattern()) {
            if (c.dx() == 0 && c.dz() == 0) continue;
            int px = GRID_X + (c.dx() + grid.radius()) * cell;
            int pz = GRID_Y + (c.dz() + grid.radius()) * cell;
            int rgb = c.color().map(DyeColor::getTextureDiffuseColor).orElse(0xFFFFFF);
            float alpha = c.required() ? 1.0f : 0.4f;
            // The glyph textures are white masks, so the chalk colour is a tint; the whole 16px
            // glyph is scaled down into the cell.
            g.setColor(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f, alpha);
            g.blit(sigilTexture(c.type().orElse(SigilType.BASE)), px, pz, cell, cell, 0f, 0f, 16, 16, 16, 16);
        }
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();

        // The altar itself.
        int c0x = GRID_X + grid.radius() * cell;
        int c0y = GRID_Y + grid.radius() * cell;
        g.fill(c0x, c0y, c0x + cell, c0y + cell, 0xFFB46AFF);
        g.renderOutline(c0x, c0y, cell, cell, 0xFFFFFFFF);
    }

    /** Hover text per glyph, so type and colour can be read without the book. */
    @Override
    public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<RitualRecipe> holder, IRecipeSlotsView slots,
                           double mouseX, double mouseY) {
        RitualRecipe recipe = holder.value();
        Grid grid = Grid.of(recipe);
        if (mouseX < GRID_X || mouseY < GRID_Y) return;
        int gx = (int) Math.floor((mouseX - GRID_X) / grid.cellPx()) - grid.radius();
        int gz = (int) Math.floor((mouseY - GRID_Y) / grid.cellPx()) - grid.radius();
        if (Math.abs(gx) > grid.radius() || Math.abs(gz) > grid.radius()) return;

        for (RitualRecipe.Cell c : recipe.pattern()) {
            if (c.dx() != gx || c.dz() != gz || (gx == 0 && gz == 0)) continue;
            tooltip.add(Component.translatable("emi.wispwillow.ritual.glyph",
                    Component.translatable("sigil.wispwillow." + c.type().map(SigilType::getSerializedName).orElse("any")),
                    Component.translatable("color.minecraft." + c.color().map(DyeColor::getName).orElse("white"))));
            tooltip.add(Component.literal("(" + c.dx() + ", " + c.dz() + ")").withStyle(ChatFormatting.DARK_GRAY));
            if (!c.required()) {
                tooltip.add(Component.translatable("emi.wispwillow.ritual.optional").withStyle(ChatFormatting.DARK_GRAY));
            }
            return;
        }
    }

    private static ResourceLocation sigilTexture(SigilType type) {
        return ResourceLocation.fromNamespaceAndPath(WispWillow.MODID,
                "textures/block/sigil/" + type.getSerializedName() + ".png");
    }
}
