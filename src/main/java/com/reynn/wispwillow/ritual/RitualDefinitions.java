package com.reynn.wispwillow.ritual;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.awakening.Imbuement;
import com.reynn.wispwillow.blocks.SigilType;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import com.reynn.wispwillow.registry.TagRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The single source of truth for every ritual in the mod.
 *
 * <p>The recipe provider turns these into {@code wispwillow:ritual} recipes and the
 * Patchouli provider turns the exact same objects into book entries, so a circle drawn
 * in the grimoire is by construction the circle the altar checks for. Previously the two
 * were maintained by hand and had already drifted apart in several places.</p>
 */
public final class RitualDefinitions {
    private RitualDefinitions() {}

    /** Which grimoire chapter a rite is filed under. */
    public enum Chapter {
        MINOR("rituals_minor"),
        MAJOR("rituals_major"),
        GRAND("rituals_grand"),
        IMBUING("rituals_imbuing");

        public final String categoryId;
        Chapter(String categoryId) { this.categoryId = categoryId; }
    }

    /** A reagent that must be lying on the ground inside the circle. */
    public record Reagent(ItemStack stack) {
        public int count() { return stack.getCount(); }
    }

    public record Def(
            String path,
            String displayName,
            Chapter chapter,
            RitualRecipe.Grid grid,
            List<RitualRecipe.Cell> pattern,
            List<Reagent> reagents,
            Optional<ItemStack> resultItem,
            Optional<ResourceLocation> resultMob,
            int resultMobCount,
            int fluxia,
            int minTier,
            ItemStack icon,
            ItemLike unlockItem,
            String lore,
            String notes,
            Optional<Vigil> vigil,
            Optional<TagKey<Item>> awakens,
            Optional<Imbuement> imbues
    ) {
        /** True if this rite is kept rather than completed. */
        public boolean isVigil() { return vigil.isPresent(); }

        /** True if this rite embues what is already in the circle instead of making something. */
        public boolean isAwakening() { return awakens.isPresent(); }

        /** True if this rite imbues awakened pieces lying in the circle. */
        public boolean isImbuing() { return imbues.isPresent(); }

        public ResourceLocation id() {
            return ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "ritual/" + path);
        }

        /** Language key the altar uses when it names this rite in chat. */
        public String langKey() {
            return WispWillow.MODID + ".ritual/" + path;
        }

        /** Chebyshev radius actually used by the pattern (never smaller than the grid). */
        public int radius() {
            int r = grid.radius;
            for (var c : pattern) r = Math.max(r, Math.max(Math.abs(c.dx()), Math.abs(c.dz())));
            return r;
        }
    }

    // ------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------

    public static final class Builder {
        private Optional<Vigil> vigil = Optional.empty();
        private Optional<TagKey<Item>> awakens = Optional.empty();
        private Optional<Imbuement> imbues = Optional.empty();
        private final String path;
        private final String displayName;
        private final Chapter chapter;
        private final RitualRecipe.Grid grid;
        private final List<RitualRecipe.Cell> pattern = new ArrayList<>();
        private final List<Reagent> reagents = new ArrayList<>();
        private Optional<ItemStack> resultItem = Optional.empty();
        private Optional<ResourceLocation> resultMob = Optional.empty();
        private int resultMobCount = 1;
        private int fluxia = 0;
        private int minTier = 1;
        private ItemStack icon = ItemStack.EMPTY;
        private ItemLike unlockItem = Items.AMETHYST_SHARD;
        private String lore = "";
        private String notes = "";

        private Builder(String path, String displayName, Chapter chapter, RitualRecipe.Grid grid) {
            this.path = path;
            this.displayName = displayName;
            this.chapter = chapter;
            this.grid = grid;
        }

        /** One glyph the rite requires at (dx, dz) relative to the altar. */
        public Builder cell(int dx, int dz, SigilType type, DyeColor color) {
            pattern.add(new RitualRecipe.Cell(dx, dz, Optional.ofNullable(type), Optional.ofNullable(color), true));
            return this;
        }

        /** Four glyphs at the cardinal points of the given radius. */
        public Builder cardinals(int r, SigilType type, DyeColor color) {
            return cell(0, -r, type, color).cell(r, 0, type, color)
                    .cell(0, r, type, color).cell(-r, 0, type, color);
        }

        /** Four glyphs at the diagonal corners of the given radius. */
        public Builder diagonals(int r, SigilType type, DyeColor color) {
            return cell(r, -r, type, color).cell(r, r, type, color)
                    .cell(-r, r, type, color).cell(-r, -r, type, color);
        }

        /** A purely decorative mark: the altar accepts the rite whether or not it is drawn. */
        public Builder guide(int dx, int dz) {
            pattern.add(new RitualRecipe.Cell(dx, dz, Optional.empty(), Optional.empty(), false));
            return this;
        }

        public Builder guideCardinals(int r) {
            return guide(0, -r).guide(r, 0).guide(0, r).guide(-r, 0);
        }

        public Builder guideDiagonals(int r) {
            return guide(r, -r).guide(r, r).guide(-r, r).guide(-r, -r);
        }

        public Builder reagent(ItemLike item, int count) {
            reagents.add(new Reagent(new ItemStack(item, count)));
            return this;
        }

        public Builder reagent(ItemLike item) { return reagent(item, 1); }

        public Builder fluxia(int f) { this.fluxia = f; return this; }

        /** Minimum altar tier this rite demands. See {@link com.reynn.wispwillow.altar.AltarTier}. */
        public Builder tier(int t) { this.minTier = t; return this; }

        public Builder result(ItemLike item, int count) {
            this.resultItem = Optional.of(new ItemStack(item, count));
            return this;
        }

        public Builder summon(String entityId, int count) {
            this.resultMob = Optional.of(ResourceLocation.parse(entityId));
            this.resultMobCount = Math.max(1, count);
            return this;
        }

        /**
         * Makes this a vigil: a rite the altar keeps burning rather than one it finishes.
         *
         * <p>Upkeep defaults to the effect's own rate, so the rites read as "this is a storm"
         * rather than as a table of numbers that all have to agree with each other.</p>
         *
         * @param minutes how long a full burn lasts
         */
        public Builder vigil(Vigil.Effect effect, int minutes) {
            this.vigil = Optional.of(new Vigil(effect, minutes * 60 * 20, effect.defaultUpkeep()));
            return this;
        }

        /**
         * Makes this rite embue the tagged items already lying in the circle rather than
         * produce something new.
         *
         * <p>Those items are not reagents: do not also list them with {@link #reagent},
         * or the altar will eat the very gear the rite is supposed to hand back.</p>
         */
        public Builder awakens(TagKey<Item> tag) { this.awakens = Optional.of(tag); return this; }

        /**
         * Makes this rite imbue the awakened pieces already lying in the circle. As with
         * {@link #awakens}, those pieces are not reagents and must not be listed as one.
         */
        public Builder imbues(Imbuement imbuement) { this.imbues = Optional.of(imbuement); return this; }

        public Builder icon(ItemLike item) { this.icon = new ItemStack(item); return this; }
        public Builder unlockedBy(ItemLike item) { this.unlockItem = item; return this; }
        public Builder lore(String text) { this.lore = text; return this; }
        public Builder notes(String text) { this.notes = text; return this; }

        private Def build() {
            ItemStack finalIcon = icon;
            if (finalIcon.isEmpty()) {
                finalIcon = resultItem.map(ItemStack::copy).orElse(new ItemStack(Items.AMETHYST_SHARD));
                finalIcon.setCount(1);
            }
            return new Def(path, displayName, chapter, grid,
                    List.copyOf(pattern), List.copyOf(reagents),
                    resultItem, resultMob, resultMobCount, fluxia, minTier,
                    finalIcon, unlockItem, lore, notes, vigil, awakens, imbues);
        }
    }

    private static Builder ritual(String path, String name, Chapter chapter, RitualRecipe.Grid grid) {
        return new Builder(path, name, chapter, grid);
    }

    // ------------------------------------------------------------------
    // The rites
    // ------------------------------------------------------------------

    private static final List<Def> ALL = build();

    public static List<Def> all() { return ALL; }

    public static List<Def> inChapter(Chapter chapter) {
        List<Def> out = new ArrayList<>();
        for (Def d : ALL) if (d.chapter() == chapter) out.add(d);
        return out;
    }

    @Nullable
    public static Def byPath(String path) {
        for (Def d : ALL) if (d.path().equals(path)) return d;
        return null;
    }

    private static List<Def> build() {
        List<Def> out = new ArrayList<>();

        // ============ 3x3 — radius 1 ============

        out.add(ritual("minor_focus", "Minor Focus", Chapter.MINOR, RitualRecipe.Grid.G3)
                .cardinals(1, SigilType.BASE, DyeColor.WHITE)
                .reagent(Items.AMETHYST_SHARD)
                .reagent(Items.BONE_MEAL)
                .fluxia(20)
                .tier(1)
                .result(Items.AMETHYST_SHARD, 4)
                .icon(Items.AMETHYST_SHARD)
                .unlockedBy(Items.AMETHYST_SHARD)
                .lore("The first circle every apprentice draws. Four plain marks in white chalk, "
                        + "one shard, and a pinch of bone meal — and the shard comes back fourfold.")
                .notes("Cheap enough to run in a loop while you learn the rhythm of an altar. "
                        + "It pays back its own chalk within two castings.")
                .build());

        out.add(ritual("minor_binding", "Minor Binding", Chapter.MINOR, RitualRecipe.Grid.G3)
                .diagonals(1, SigilType.BASE, DyeColor.LIGHT_GRAY)
                .reagent(Items.STRING)
                .reagent(Items.COPPER_INGOT)
                .fluxia(120)
                .tier(1)
                .result(Items.GLOWSTONE_DUST, 12)
                .icon(Items.GLOWSTONE_DUST)
                .unlockedBy(Items.STRING)
                .lore("A lattice woven on the diagonals, holding a thread of copper still long "
                        + "enough for loose light to settle onto it.")
                .notes("Note the corners: this is the first circle that does not use the cardinal "
                        + "points at all, and drawing it on the cardinals instead will match nothing.")
                .build());

        out.add(ritual("flame_bloom", "Flame Bloom", Chapter.MINOR, RitualRecipe.Grid.G3)
                .cardinals(1, SigilType.BASE, DyeColor.RED)
                .diagonals(1, SigilType.STAR, DyeColor.RED)
                .reagent(Items.BLAZE_ROD)
                .reagent(Items.MAGMA_CREAM, 2)
                .fluxia(250)
                .tier(2)
                .result(Items.BLAZE_ROD, 3)
                .icon(Items.BLAZE_ROD)
                .unlockedBy(Items.BLAZE_ROD)
                .lore("Fire wrapped in petals of chalk. Every tile of the ring is filled — plain "
                        + "marks on the cross, stars at the corners — and all of it in red.")
                .notes("The first full ring in the book: eight glyphs, no gaps. It gives back "
                        + "three rods for the one you feed it, which is the point — a fortress "
                        + "trip should not be a prerequisite for every brewing stand you own.")
                .build());

        out.add(ritual("minor_arcana", "Minor Arcana", Chapter.MINOR, RitualRecipe.Grid.G3)
                .cell(-1, 0, SigilType.ECLIPSE, DyeColor.BLACK)
                .cell(1, 0, SigilType.ECLIPSE, DyeColor.BLACK)
                .cell(0, -1, SigilType.BASE, DyeColor.BLACK)
                .cell(0, 1, SigilType.BASE, DyeColor.BLACK)
                .reagent(Items.AMETHYST_SHARD, 2)
                .reagent(Items.COAL_BLOCK)
                .fluxia(300)
                .tier(2)
                .result(ItemRegistry.AMETHYST_COAL.get(), 18)
                .icon(ItemRegistry.AMETHYST_COAL.get())
                .unlockedBy(Items.AMETHYST_SHARD)
                .lore("Shadow braided into crystal. A block of coal goes in whole and comes out "
                        + "as eighteen violet embers that burn far longer than they have any right to.")
                .notes("Mind the axes — Eclipse glyphs sit east and west, plain glyphs north and "
                        + "south. Swapping them is the single most common mistake in this chapter.")
                .build());

        out.add(ritual("magicka_solidificus", "Magicka Solidificus", Chapter.MINOR, RitualRecipe.Grid.G3)
                .cardinals(1, SigilType.SERPENT, DyeColor.YELLOW)
                .diagonals(1, SigilType.TRIAD, DyeColor.BLUE)
                .reagent(Items.STONE, 6)
                .reagent(Items.DIAMOND)
                .reagent(Items.LILY_OF_THE_VALLEY)
                .fluxia(150)
                .tier(2)
                .result(BlockRegistry.FLUXITE.get(), 24)
                .icon(BlockRegistry.FLUXITE.get())
                .unlockedBy(Items.DIAMOND)
                .lore("Stone taught to remember. Serpents carry the current around the ring while "
                        + "the triads hold it in place long enough for the rock to take the impression.")
                .notes("Fluxite is the backbone of every Fluxia device you will build, so this "
                        + "circle tends to stay chalked for a very long time.")
                .build());

        // ============ 6x6 — radius 3 ============

        out.add(ritual("starlight_cross", "Starlight Cross", Chapter.MAJOR, RitualRecipe.Grid.G6)
                .cardinals(3, SigilType.STAR, DyeColor.LIGHT_BLUE)
                .reagent(Items.GLOW_INK_SAC)
                .reagent(Items.PHANTOM_MEMBRANE)
                .fluxia(1500)
                .tier(3)
                .summon("minecraft:allay", 1)
                .icon(Items.ALLAY_SPAWN_EGG)
                .unlockedBy(Items.GLOW_INK_SAC)
                .lore("Only four glyphs, but set three blocks out — a wide, quiet cross that a "
                        + "wanderer can see from a long way off.")
                .notes("Four marks and fifteen hundred Fluxia. This is the circle that teaches you "
                        + "that a rite's price lives in its power, not its chalk.")
                .build());

        out.add(ritual("crystallized_fertilization", "Crystallized Fertilization", Chapter.MAJOR, RitualRecipe.Grid.G6)
                .cell(0, -3, SigilType.STAR, DyeColor.LIGHT_BLUE)
                .cell(0, 3, SigilType.STAR, DyeColor.LIGHT_BLUE)
                .cell(-1, -1, SigilType.STAR, DyeColor.LIGHT_BLUE)
                .cell(1, 1, SigilType.STAR, DyeColor.LIGHT_BLUE)
                .cell(3, 0, SigilType.HEX, DyeColor.MAGENTA)
                .cell(-3, 0, SigilType.HEX, DyeColor.MAGENTA)
                .reagent(Items.AMETHYST_SHARD, 2)
                .reagent(Items.WATER_BUCKET)
                .reagent(Items.DIRT, 4)
                .fluxia(300)
                .tier(2)
                .result(BlockRegistry.AMETHYST_FARMLAND.get(), 4)
                .icon(BlockRegistry.AMETHYST_FARMLAND.get())
                .unlockedBy(Items.AMETHYST_SHARD)
                .lore("Sharp crystal threaded through soft soil. Deliberately lopsided: two stars "
                        + "on the north-south line, two more tucked in on one diagonal pair, and hexes east and west.")
                .notes("The bucket is a container, not a reagent — the altar returns it empty once "
                        + "the rite completes.")
                .build());

        out.add(ritual("lunar_gate", "Lunar Gate", Chapter.MAJOR, RitualRecipe.Grid.G6)
                .cardinals(3, SigilType.BASE, DyeColor.MAGENTA)
                .diagonals(2, SigilType.ECLIPSE, DyeColor.PURPLE)
                .reagent(Items.ENDER_PEARL)
                .reagent(Items.AMETHYST_SHARD)
                .fluxia(350)
                .tier(3)
                .result(Items.ENDER_EYE, 3)
                .icon(Items.ENDER_EYE)
                .unlockedBy(Items.ENDER_PEARL)
                .lore("Two rings that do not share a radius: plain magenta marks at three paces, "
                        + "purple eclipses at two, sitting on the diagonals inside them.")
                .notes("Turning one pearl into two eyes makes stronghold hunting considerably "
                        + "less arithmetic.")
                .build());

        out.add(ritual("soul_pact", "The Written Pact", Chapter.MAJOR, RitualRecipe.Grid.G6)
                .cardinals(3, SigilType.RUNES, DyeColor.PURPLE)
                .diagonals(2, SigilType.ECLIPSE, DyeColor.BLACK)
                .cardinals(1, SigilType.BASE, DyeColor.WHITE)
                .reagent(ItemRegistry.AMETHYST_PAPER.get(), 4)
                .reagent(Items.SOUL_SAND)
                .reagent(Items.AMETHYST_SHARD, 2)
                .fluxia(500)
                .tier(2)
                .result(ItemRegistry.SOUL_PACT.get(), 4)
                .icon(ItemRegistry.SOUL_PACT.get())
                .unlockedBy(ItemRegistry.AMETHYST_PAPER.get())
                .lore("Amethyst paper laid on soul sand, inside runes that spell out an offer. "
                        + "Nothing signs it here — the pact is only the paper and the wording. "
                        + "What answers, answers later.")
                .notes("Four pacts a casting, and every one of them still blank. This is the "
                        + "first step of the familiar chain, not the last: see the Soul Gateway.")
                .build());

        // ============ 9x9 — radius 4 ============

        out.add(ritual("verdant_wilds", "Verdant Wilds", Chapter.GRAND, RitualRecipe.Grid.G9)
                .cell(0, -4, SigilType.BASE, DyeColor.LIME)
                .cell(4, 0, SigilType.BASE, DyeColor.LIME)
                .cell(0, 4, SigilType.BASE, DyeColor.LIME)
                .cell(-4, 0, SigilType.BASE, DyeColor.LIME)
                .diagonals(4, SigilType.BASE, DyeColor.GREEN)
                .guideCardinals(2)
                .reagent(Items.HONEYCOMB, 4)
                .reagent(Items.FLOWERING_AZALEA)
                .fluxia(600)
                .tier(3)
                .summon("minecraft:bee", 3)
                .icon(Items.BEEHIVE)
                .unlockedBy(Items.HONEYCOMB)
                .lore("A wide ring for bees and blossoms, alternating lime on the cross and deep "
                        + "green at the corners — the colours of new growth and old moss.")
                .notes("Have a hive ready before you cast. Three bees arrive irritated and "
                        + "immediately start looking for somewhere to live.")
                .build());

        // ------------------------------------------------------------------
        // Vigils: rites the altar keeps, rather than rites it finishes.
        //
        // All four are laid on the same nine-wide ring on purpose. Once you have learned to
        // draw one vigil you can draw any of them, and the difference is the colour of the
        // chalk and what you put on the ground — which is exactly how they should read.
        // ------------------------------------------------------------------

        out.add(ritual("vigil_clear_skies", "Vigil of Clear Skies", Chapter.MAJOR, RitualRecipe.Grid.G9)
                .cell(0, -4, SigilType.BASE, DyeColor.LIGHT_BLUE)
                .cell(4, 0, SigilType.BASE, DyeColor.LIGHT_BLUE)
                .cell(0, 4, SigilType.BASE, DyeColor.LIGHT_BLUE)
                .cell(-4, 0, SigilType.BASE, DyeColor.LIGHT_BLUE)
                .diagonals(3, SigilType.BASE, DyeColor.WHITE)
                .guideCardinals(2)
                .reagent(Items.GLASS_BOTTLE, 2)
                .reagent(Items.FEATHER, 3)
                .fluxia(1_500)
                .tier(2)
                .vigil(Vigil.Effect.CLEAR_SKIES, 20)
                .icon(Items.FEATHER)
                .unlockedBy(Items.FEATHER)
                .lore("Pale blue on the cross, white at the corners. The altar holds the sky "
                        + "open and will not do anything else while it does.")
                .notes("Twenty minutes. It re-asserts itself every second, so a storm rolling "
                        + "in mid-vigil loses the argument rather than the altar.")
                .build());

        out.add(ritual("vigil_open_way", "Vigil of the Open Way", Chapter.MAJOR, RitualRecipe.Grid.G9)
                .cell(0, -4, SigilType.BASE, DyeColor.PURPLE)
                .cell(4, 0, SigilType.BASE, DyeColor.PURPLE)
                .cell(0, 4, SigilType.BASE, DyeColor.PURPLE)
                .cell(-4, 0, SigilType.BASE, DyeColor.PURPLE)
                .diagonals(3, SigilType.BASE, DyeColor.MAGENTA)
                .guideCardinals(2)
                .reagent(Items.ENDER_EYE, 2)
                .reagent(Items.SOUL_LANTERN, 1)
                .reagent(Items.AMETHYST_SHARD, 4)
                .fluxia(25_000)
                .tier(4)
                .vigil(Vigil.Effect.OPEN_WAY, 30)
                .icon(Items.ENDER_EYE)
                .unlockedBy(Items.ENDER_EYE)
                .lore("Purple on the cross, magenta at the corners. The altar stops being a "
                        + "place where things are made and becomes a door.")
                .notes("Half an hour. Stand on the altar while it burns and it takes you "
                        + "through; when it stops, for any reason, it puts everyone back where "
                        + "they were standing. There is no other way home, and that is the point.")
                .build());

        out.add(ritual("vigil_gathering_storm", "Vigil of the Gathering Storm", Chapter.MAJOR, RitualRecipe.Grid.G9)
                .cell(0, -4, SigilType.BASE, DyeColor.PURPLE)
                .cell(4, 0, SigilType.BASE, DyeColor.PURPLE)
                .cell(0, 4, SigilType.BASE, DyeColor.PURPLE)
                .cell(-4, 0, SigilType.BASE, DyeColor.PURPLE)
                .diagonals(3, SigilType.BASE, DyeColor.GRAY)
                .guideCardinals(2)
                .reagent(Items.COPPER_INGOT, 4)
                .reagent(Items.GUNPOWDER, 2)
                .fluxia(1_800)
                .tier(3)
                .vigil(Vigil.Effect.GATHERING_STORM, 15)
                .icon(Items.LIGHTNING_ROD)
                .unlockedBy(Items.COPPER_INGOT)
                .lore("The same ring in purple and grey. Copper to call it down and powder to "
                        + "make it mean it.")
                .notes("Fifteen minutes of thunder. Worth pairing with lightning rods and a "
                        + "very clear idea of where your roof is.")
                .build());

        out.add(ritual("vigil_verdant_tide", "Vigil of the Verdant Tide", Chapter.MAJOR, RitualRecipe.Grid.G9)
                .cell(0, -4, SigilType.BASE, DyeColor.LIME)
                .cell(4, 0, SigilType.BASE, DyeColor.LIME)
                .cell(0, 4, SigilType.BASE, DyeColor.LIME)
                .cell(-4, 0, SigilType.BASE, DyeColor.LIME)
                .diagonals(3, SigilType.BASE, DyeColor.BROWN)
                .guideCardinals(2)
                .reagent(Items.BONE_MEAL, 8)
                .reagent(Items.MOSS_BLOCK)
                .fluxia(1_200)
                .tier(2)
                .vigil(Vigil.Effect.VERDANT_TIDE, 25)
                .icon(Items.BONE_MEAL)
                .unlockedBy(Items.BONE_MEAL)
                .lore("Green on the cross, brown at the corners — growth over soil. Everything "
                        + "inside the ring that can grow, does, over and over.")
                .notes("It calls each block's own growth rather than forcing a crop age, so it "
                        + "hurries saplings, cane and anything another mod has planted just the "
                        + "same as wheat.")
                .build());

        out.add(ritual("vigil_warding_light", "Vigil of the Warding Light", Chapter.MAJOR, RitualRecipe.Grid.G9)
                .cell(0, -4, SigilType.BASE, DyeColor.YELLOW)
                .cell(4, 0, SigilType.BASE, DyeColor.YELLOW)
                .cell(0, 4, SigilType.BASE, DyeColor.YELLOW)
                .cell(-4, 0, SigilType.BASE, DyeColor.YELLOW)
                .diagonals(3, SigilType.BASE, DyeColor.ORANGE)
                .guideCardinals(2)
                .reagent(Items.GLOWSTONE_DUST, 4)
                .reagent(Items.GOLDEN_APPLE)
                .fluxia(2_000)
                .tier(3)
                .vigil(Vigil.Effect.WARDING_LIGHT, 20)
                .icon(Items.GLOWSTONE_DUST)
                .unlockedBy(Items.GLOWSTONE_DUST)
                .lore("Yellow and orange, the colours of a held lamp. Nothing hostile keeps its "
                        + "footing inside the ring.")
                .notes("It shoves rather than forbids. Things still wander in; they simply do "
                        + "not stay, and you can watch them fail to.")
                .build());

        // ------------------------------------------------------------------
        // Omni Chalk: sixteen glyphs, one per dye, and sixteen sticks to draw them with.
        //
        // The circle is the bill of materials. There is no way to lay this rite without
        // owning every colour of chalk already, which is the only gate it needs — after
        // this one you never draw a circle by hand again.
        // ------------------------------------------------------------------
        Builder omni = ritual("omni_chalk", "The Whole Spectrum", Chapter.MAJOR, RitualRecipe.Grid.G9)
                .cell(0, -4, SigilType.ECLIPSE, DyeColor.RED)
                .cell(4, 0, SigilType.ECLIPSE, DyeColor.YELLOW)
                .cell(0, 4, SigilType.ECLIPSE, DyeColor.GREEN)
                .cell(-4, 0, SigilType.ECLIPSE, DyeColor.BLUE)
                .cell(4, -4, SigilType.STAR, DyeColor.ORANGE)
                .cell(4, 4, SigilType.STAR, DyeColor.LIME)
                .cell(-4, 4, SigilType.STAR, DyeColor.CYAN)
                .cell(-4, -4, SigilType.STAR, DyeColor.MAGENTA)
                .cell(0, -3, SigilType.HEX, DyeColor.PINK)
                .cell(3, 0, SigilType.HEX, DyeColor.LIGHT_BLUE)
                .cell(0, 3, SigilType.HEX, DyeColor.LIGHT_GRAY)
                .cell(-3, 0, SigilType.HEX, DyeColor.PURPLE)
                .cell(3, -3, SigilType.TRIAD, DyeColor.WHITE)
                .cell(3, 3, SigilType.TRIAD, DyeColor.BROWN)
                .cell(-3, 3, SigilType.TRIAD, DyeColor.GRAY)
                .cell(-3, -3, SigilType.TRIAD, DyeColor.BLACK);
        for (DyeColor dye : DyeColor.values()) {
            omni.reagent(ItemRegistry.CHALK.get(dye).get());
        }
        out.add(omni
                .fluxia(5_000)
                .tier(3)
                .result(ItemRegistry.OMNI_CHALK.get(), 1)
                .icon(ItemRegistry.OMNI_CHALK.get())
                .unlockedBy(ItemRegistry.CHALK.get(DyeColor.WHITE).get())
                .lore("Sixteen marks, no two alike, and sixteen sticks laid down inside them. "
                        + "What gets up off the stone is all of them at once.")
                .notes("Every stick is consumed. Draw the circle before you decide you can "
                        + "spare them — this is the last time you will need to draw one by hand.")
                .build());

        out.add(ritual("grand_convergence", "Grand Convergence", Chapter.GRAND, RitualRecipe.Grid.G9)
                .cardinals(4, SigilType.STAR, DyeColor.CYAN)
                .diagonals(4, SigilType.ECLIPSE, DyeColor.PURPLE)
                .cardinals(2, SigilType.BASE, DyeColor.LIGHT_GRAY)
                .diagonals(2, SigilType.RUNES, DyeColor.WHITE)
                .reagent(Items.IRON_BLOCK)
                .reagent(Items.PUMPKIN)
                .reagent(Items.AMETHYST_BLOCK)
                .fluxia(1250)
                .tier(4)
                .summon("minecraft:iron_golem", 1)
                .icon(Items.IRON_BLOCK)
                .unlockedBy(Items.IRON_BLOCK)
                .lore("Two concentric rings that between them convince metal to walk: cyan stars "
                        + "and purple eclipses on the outside, grey marks and white runes within.")
                .notes("The same ingredients as a hand-built golem, but the circle does the "
                        + "shaping — you never have to get the pumpkin on last.")
                .build());

        out.add(ritual("awakening_empyrite", "Awakening Empyrite", Chapter.GRAND, RitualRecipe.Grid.G9)
                .cardinals(4, SigilType.STAR, DyeColor.YELLOW)
                .diagonals(4, SigilType.SERPENT, DyeColor.ORANGE)
                .cardinals(2, SigilType.RUNES, DyeColor.YELLOW)
                .diagonals(2, SigilType.BASE, DyeColor.ORANGE)
                .guideCardinals(1)
                .reagent(Items.GOLD_BLOCK)
                .reagent(Items.GLOWSTONE, 2)
                .reagent(Items.BLAZE_POWDER, 4)
                .fluxia(2_500)
                .tier(4)
                .awakens(TagRegistry.Items.EMPYRITE)
                .icon(ItemRegistry.EMPYRITE_INGOT.get())
                .unlockedBy(ItemRegistry.EMPYRITE_INGOT.get())
                .lore("Empyrite comes out of the crucible asleep — sound metal, dark as a shaded "
                        + "pine, and entirely inert. This circle wakes it. Gold and glowstone on a "
                        + "ring of yellow stars, serpents of orange carrying the heat inward, and "
                        + "every piece of empyrite you have laid on the ground comes up lit from "
                        + "within.")
                .notes("The empyrite is not a reagent. Drop it inside the circle and it stays "
                        + "yours: the rite embues every piece at once and hands them all back "
                        + "exactly as they were — enchantments, wear, names and trim untouched. "
                        + "Awakening is not an enchantment and cannot be ground off, and an "
                        + "awakened piece carries a gold glint ever after, in place of the usual "
                        + "violet, whatever you go on to enchant it with.")
                .build());

        // ============ 11x11 — radius 5 ============

        out.add(ritual("draconic_ascension", "Draconic Ascension", Chapter.GRAND, RitualRecipe.Grid.G12)
                .cardinals(5, SigilType.STAR, DyeColor.PURPLE)
                .diagonals(5, SigilType.ECLIPSE, DyeColor.BLACK)
                .guideDiagonals(4)
                .cardinals(3, SigilType.BASE, DyeColor.LIGHT_GRAY)
                .diagonals(3, SigilType.RUNES, DyeColor.MAGENTA)
                .guideCardinals(2)
                .cardinals(1, SigilType.BASE, DyeColor.PURPLE)
                .diagonals(1, SigilType.STAR, DyeColor.PINK)
                .reagent(Items.NETHER_STAR)
                .reagent(Items.DRAGON_HEAD)
                .reagent(Items.DRAGON_BREATH, 4)
                .reagent(Items.END_CRYSTAL, 4)
                .reagent(Items.ECHO_SHARD, 4)
                .reagent(Items.NETHERITE_INGOT, 4)
                .reagent(Items.SHULKER_SHELL, 4)
                .reagent(Items.AMETHYST_BLOCK, 4)
                .reagent(Items.OBSIDIAN, 8)
                .fluxia(2000)
                .tier(5)
                .result(Items.DRAGON_EGG, 1)
                .icon(Items.DRAGON_EGG)
                .unlockedBy(Items.DRAGON_BREATH)
                .lore("Three rings and thirty-three trophies, arranged into a single impossible "
                        + "question. The world, having no better answer, lays an egg.")
                .notes("The Fluxia cost is almost modest. The price of this rite is measured in "
                        + "matter — and in how far you had to travel for it.")
                .build());

        // ============ 17x17 — radius 8 ============

        {
            Builder conclave = ritual("shrouded_conclave", "Shrouded Conclave", Chapter.GRAND, RitualRecipe.Grid.G18)
                    .reagent(Items.BONE_BLOCK, 4)
                    .reagent(Items.SOUL_SOIL, 4)
                    .reagent(Items.OBSIDIAN, 4)
                    .reagent(Items.AMETHYST_BLOCK, 4)
                    .reagent(Items.WITHER_SKELETON_SKULL, 3)
                    .fluxia(3000)
                .tier(5)
                    .summon("minecraft:wither", 3)
                    .icon(Items.WITHER_SKELETON_SKULL)
                    .unlockedBy(Items.WITHER_SKELETON_SKULL)
                    .lore("The largest circle in this book, and the only one that should never be "
                            + "drawn near anything you would miss. A solid bone-white frame, then ring "
                            + "upon ring inward, all of it converging on the altar you are standing at.")
                    .notes("Three Withers. Three. Build the containment first, chalk the circle "
                            + "second, and bring the skulls last.");

            // Solid outer frame at radius 8: eclipses at the corners, stars on the cross,
            // plain marks filling every remaining edge tile.
            addFullRing(conclave, 8, SigilType.ECLIPSE, DyeColor.BLACK,
                    SigilType.STAR, DyeColor.PURPLE, SigilType.BASE, DyeColor.LIGHT_GRAY);
            // Sparse rings inward, every second tile.
            addSparseRing(conclave, 6, SigilType.RUNES, DyeColor.MAGENTA,
                    SigilType.BASE, DyeColor.LIGHT_GRAY, SigilType.ECLIPSE, DyeColor.PURPLE);
            addSparseRing(conclave, 4, SigilType.RUNES, DyeColor.MAGENTA,
                    SigilType.BASE, DyeColor.LIGHT_GRAY, SigilType.BASE, DyeColor.LIGHT_GRAY);
            conclave.diagonals(2, SigilType.RUNES, DyeColor.MAGENTA)
                    .cardinals(2, SigilType.STAR, DyeColor.PURPLE)
                    .diagonals(1, SigilType.RUNES, DyeColor.MAGENTA)
                    .cardinals(1, SigilType.BASE, DyeColor.LIGHT_GRAY);

            out.add(conclave.build());
        }

        // ============ Imbuings — radius 4 ============
        //
        // Every imbuing circle is the awakening's orange serpents with the rest re-keyed:
        // stars on the far cross, hexes on the near cross, triads on the near corners. Only
        // the three colours change from rite to rite, so drawing one teaches the shape of all
        // ten, and no two of them share more than half their glyphs.

        out.add(imbuing(Imbuement.SUNFIRE, "Sunfire Imbuing", DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW)
                .reagent(Items.BLAZE_ROD, 4)
                .reagent(Items.FIRE_CHARGE, 4)
                .reagent(Items.MAGMA_BLOCK, 2)
                .fluxia(3_500)
                .lore("Waking the metal lit it; this rite teaches it to share. The edge comes back "
                        + "hot enough to leave its mark on anything it touches.")
                .notes("The undead take it worst. Anything that already fears daylight remembers why.")
                .build());

        out.add(imbuing(Imbuement.SIPHON, "Siphon Imbuing", DyeColor.PURPLE, DyeColor.RED, DyeColor.BLACK)
                .reagent(Items.GHAST_TEAR, 2)
                .reagent(Items.FERMENTED_SPIDER_EYE, 4)
                .reagent(Items.NETHER_WART, 8)
                .fluxia(3_500)
                .lore("A thirsty edge. Tears for grief, wart for appetite, and a purple ring to hold "
                        + "the two together until the blade learns to drink.")
                .notes("It heals by what a strike actually lands, after armour, so it rewards hitting "
                        + "hard things hard.")
                .build());

        out.add(imbuing(Imbuement.REAPING, "Reaping Imbuing", DyeColor.GRAY, DyeColor.BLACK, DyeColor.WHITE)
                .reagent(Items.BONE_BLOCK, 4)
                .reagent(Items.SOUL_SOIL, 4)
                .reagent(Items.ECHO_SHARD, 2)
                .fluxia(4_000)
                .lore("Bone, soul soil and the echo of something deep. The blade learns to take a "
                        + "little more from every life it ends.")
                .notes("Experience only, and a great deal of it. It does nothing for drops.")
                .build());

        out.add(imbuing(Imbuement.KILNHEART, "Kilnheart Imbuing", DyeColor.ORANGE, DyeColor.RED, DyeColor.BROWN)
                .reagent(Items.BLAST_FURNACE)
                .reagent(Items.MAGMA_CREAM, 4)
                .reagent(Items.COAL_BLOCK, 2)
                .fluxia(3_000)
                .lore("A furnace folded into the head of a pick. What comes out of the stone comes "
                        + "out finished.")
                .notes("Only ores and raw metal are fired. Cobblestone stays cobblestone, and an axe "
                        + "will not turn your logs to charcoal.")
                .build());

        out.add(imbuing(Imbuement.VEINSENSE, "Veinsense Imbuing", DyeColor.BROWN, DyeColor.LIGHT_GRAY, DyeColor.CYAN)
                .reagent(Items.RAW_IRON_BLOCK)
                .reagent(Items.RAW_GOLD_BLOCK)
                .reagent(Items.AMETHYST_BLOCK, 4)
                .reagent(Items.DIAMOND, 2)
                .fluxia(5_000)
                .lore("Raw iron, raw gold and amethyst, so the metal learns the taste of a seam and "
                        + "follows it through the rock.")
                .notes("It follows the same block only, a vein rather than every ore nearby. Each "
                        + "block still costs durability, and it stops a point short of breaking the tool.")
                .build());

        out.add(imbuing(Imbuement.TETHER, "Tether Imbuing", DyeColor.MAGENTA, DyeColor.PURPLE, DyeColor.PINK)
                .reagent(Items.ENDER_PEARL, 4)
                .reagent(Items.HOPPER)
                .reagent(Items.LEAD, 2)
                .fluxia(3_000)
                .lore("Pearls for the pull, a hopper for the catch, and a lead to keep it all on a "
                        + "short line.")
                .notes("If your pack is full, whatever will not fit drops where it always would.")
                .build());

        out.add(imbuing(Imbuement.CLARITY, "Clarity Imbuing", DyeColor.LIGHT_BLUE, DyeColor.WHITE, DyeColor.CYAN)
                .reagent(Items.GOLDEN_CARROT, 8)
                .reagent(Items.GLOW_INK_SAC, 4)
                .reagent(Items.ENDER_EYE)
                .fluxia(3_000)
                .lore("Things that see in the dark and a thing that sees far. The helm learns both.")
                .notes("The sight is renewed while the helm is worn and fades a little while after "
                        + "it comes off.")
                .build());

        out.add(imbuing(Imbuement.BULWARK, "Bulwark Imbuing", DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.WHITE)
                .reagent(Items.SHIELD)
                .reagent(Items.IRON_BLOCK, 2)
                .reagent(Items.OBSIDIAN, 4)
                .reagent(Items.TURTLE_SCUTE, 2)
                .fluxia(4_500)
                .lore("A shield, iron and obsidian, set in grey and white. The cuirass learns to raise "
                        + "a wall of light between you and the next blow.")
                .notes("The shielding only comes back while you go unharmed. Keep taking hits and it "
                        + "waits until you step away.")
                .build());

        out.add(imbuing(Imbuement.STRIDE, "Stride Imbuing", DyeColor.LIME, DyeColor.GREEN, DyeColor.YELLOW)
                .reagent(Items.SUGAR, 16)
                .reagent(Items.RABBIT_FOOT, 2)
                .reagent(Items.FEATHER, 4)
                .fluxia(3_000)
                .lore("Sugar, rabbits' feet and feathers on a ring of green. The tassets learn that "
                        + "walking is only falling forward and catching yourself.")
                .notes("A whole block is a step, not a jump — mind it on stairs of slabs.")
                .build());

        out.add(imbuing(Imbuement.SKYSTEP, "Skystep Imbuing", DyeColor.WHITE, DyeColor.LIGHT_BLUE, DyeColor.LIGHT_GRAY)
                .reagent(Items.PHANTOM_MEMBRANE, 4)
                .reagent(Items.FEATHER, 8)
                .reagent(Items.SLIME_BALL, 4)
                .fluxia(3_500)
                .lore("Phantom membrane and feathers, white and pale blue. The greaves learn that "
                        + "the ground is only a suggestion.")
                .notes("No fall hurts, from any height. The jump is modest on purpose: these are not wings.")
                .build());

        return Collections.unmodifiableList(out);
    }

    /** The shared imbuing circle, keyed by three colours. */
    private static Builder imbuing(Imbuement imbuement, String name, DyeColor far, DyeColor near, DyeColor corners) {
        return ritual("imbue_" + imbuement.getSerializedName(), name, Chapter.IMBUING, RitualRecipe.Grid.G9)
                .diagonals(4, SigilType.SERPENT, DyeColor.ORANGE)
                .cardinals(4, SigilType.STAR, far)
                .cardinals(2, SigilType.HEX, near)
                .diagonals(2, SigilType.TRIAD, corners)
                // The same foundation the awakening asks for: nothing reaches here without it.
                .tier(4)
                .imbues(imbuement)
                .icon(iconFor(imbuement.target()))
                .unlockedBy(ItemRegistry.EMPYRITE_INGOT.get());
    }

    private static ItemLike iconFor(Imbuement.Target target) {
        return switch (target) {
            case MELEE -> ItemRegistry.EMPYRITE_SWORD.get();
            case TOOL -> ItemRegistry.EMPYRITE_PICKAXE.get();
            case HEAD -> ItemRegistry.EMPYRITE_HELMET.get();
            case CHEST -> ItemRegistry.EMPYRITE_CHESTPLATE.get();
            case LEGS -> ItemRegistry.EMPYRITE_LEGGINGS.get();
            case FEET -> ItemRegistry.EMPYRITE_BOOTS.get();
        };
    }

    /** Every tile on the square ring of the given radius. */
    private static void addFullRing(Builder b, int r,
                                    SigilType cornerType, DyeColor cornerColor,
                                    SigilType cardinalType, DyeColor cardinalColor,
                                    SigilType edgeType, DyeColor edgeColor) {
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != r) continue;
                if (Math.abs(x) == r && Math.abs(z) == r) b.cell(x, z, cornerType, cornerColor);
                else if (x == 0 || z == 0) b.cell(x, z, cardinalType, cardinalColor);
                else b.cell(x, z, edgeType, edgeColor);
            }
        }
    }

    /** Every second tile on the square ring of the given radius (r must be even). */
    private static void addSparseRing(Builder b, int r,
                                      SigilType cornerType, DyeColor cornerColor,
                                      SigilType cardinalType, DyeColor cardinalColor,
                                      SigilType edgeType, DyeColor edgeColor) {
        for (int x = -r; x <= r; x += 2) {
            for (int z = -r; z <= r; z += 2) {
                if (Math.max(Math.abs(x), Math.abs(z)) != r) continue;
                if (Math.abs(x) == r && Math.abs(z) == r) b.cell(x, z, cornerType, cornerColor);
                else if (x == 0 || z == 0) b.cell(x, z, cardinalType, cardinalColor);
                else b.cell(x, z, edgeType, edgeColor);
            }
        }
    }
}
