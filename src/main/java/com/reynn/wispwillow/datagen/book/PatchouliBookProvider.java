package com.reynn.wispwillow.datagen.book;

import com.google.gson.JsonObject;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.alchemy.AlchemyDefinitions;
import com.reynn.wispwillow.altar.AltarTier;
import com.reynn.wispwillow.awakening.AwakenedTraits;
import com.reynn.wispwillow.awakening.EmpyreanSet;
import com.reynn.wispwillow.awakening.Resonance;
import com.reynn.wispwillow.blocks.ManaPoolBlock;
import com.reynn.wispwillow.blocks.alchemy.AlchemicalCauldronBlock;
import com.reynn.wispwillow.blocks.alchemy.CauldronPattern;
import com.reynn.wispwillow.entity.familiar.FamiliarBoon;
import com.reynn.wispwillow.entity.familiar.SoulPacts;
import com.reynn.wispwillow.entity.familiar.FamiliarEntity;
import com.reynn.wispwillow.blocks.RuneType;
import com.reynn.wispwillow.blocks.fluxia.AstralFontEntity;
import com.reynn.wispwillow.blocks.fluxia.FluxiaDynamoEntity;
import com.reynn.wispwillow.blocks.fluxia.GatewayCoreBlockEntity;
import com.reynn.wispwillow.blocks.SigilType;
import com.reynn.wispwillow.infusion.InfusionDefinitions;
import com.reynn.wispwillow.items.FluxiaTabletItem;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.items.GlimmerweaveSet;
import com.reynn.wispwillow.registry.ItemRegistry;
import com.reynn.wispwillow.ritual.RitualDefinitions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.reynn.wispwillow.datagen.book.BookJson.*;

/**
 * Generates the whole Wisp &amp; Willow Grimoire.
 *
 * <p>Book metadata lives in the data pack; categories and entries live in the resource
 * pack, because the book is declared with {@code use_resource_pack}.</p>
 */
public class PatchouliBookProvider implements DataProvider {

    public static final String BOOK_ID = "wispwillow_grimoire";
    private static final String LANG = "en_us";

    private final PackOutput output;

    public PatchouliBookProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public String getName() {
        return "Wisp & Willow Patchouli Book";
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        Path dataRoot = output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(WispWillow.MODID).resolve("patchouli_books").resolve(BOOK_ID);
        Path assetRoot = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(WispWillow.MODID).resolve("patchouli_books").resolve(BOOK_ID).resolve(LANG);

        futures.add(DataProvider.saveStable(cache, bookJson(), dataRoot.resolve("book.json")));

        for (Category c : categories()) {
            futures.add(DataProvider.saveStable(cache, c.build(),
                    assetRoot.resolve("categories").resolve(c.path() + ".json")));
        }

        for (Entry e : entries()) {
            futures.add(DataProvider.saveStable(cache, e.build(),
                    assetRoot.resolve("entries").resolve(e.categoryId()).resolve(e.path() + ".json")));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    // ==================================================================
    // book.json
    // ==================================================================

    private static JsonObject bookJson() {
        JsonObject o = new JsonObject();
        o.addProperty("name", "Wisp & Willow Grimoire");
        o.addProperty("landing_text",
                "A willow's whisper, a wisp's lantern, and the quiet pull of Fluxia."
                        + P + "This grimoire records the rites, tools and sigils of the Wisp & Willow "
                        + "tradition — what each thing costs, what it gives back, and how to draw the "
                        + "circle so the altar will actually answer.");
        o.addProperty("subtitle", "Rites, Sigils & Currents");
        o.addProperty("version", 2);
        o.addProperty("model", "patchouli:book_purple");
        o.addProperty("book_texture", "patchouli:textures/gui/book_purple.png");
        o.addProperty("text_color", "B28BFF");
        o.addProperty("header_color", "D69BFF");
        o.addProperty("nameplate_color", "B46AFF");
        o.addProperty("link_color", "7FE8FF");
        o.addProperty("link_hover_color", "BFF3FF");
        o.addProperty("text_shadow", true);
        o.addProperty("progress_bar_color", "7FE8FF");
        o.addProperty("progress_bar_background", "2A1B3D");
        o.addProperty("show_progress", true);
        o.addProperty("i18n", false);
        o.addProperty("use_resource_pack", true);
        o.addProperty("dont_generate_book", false);
        o.addProperty("pause_game", false);
        o.addProperty("creative_tab", "minecraft:tools_and_utilities");
        return o;
    }

    // ==================================================================
    // Categories
    // ==================================================================

    private static List<Category> categories() {
        return List.of(
                new Category("intro", "Whispers of Fluxia",
                        "What Fluxia is, where to start, and how to read this book.",
                        id(ItemRegistry.FLUXIA_TABLET.get()), 0),
                new Category("chalk_and_sigils", "Chalk & Sigilcraft",
                        "Sixteen chalks, seven glyphs, and the geometry that binds them.",
                        id(ItemRegistry.CHALK.get(DyeColor.MAGENTA).get()), 1),
                new Category("altar", "The Altar & the Rite",
                        "The stone that reads your circle, and how to make it speak.",
                        id(BlockRegistry.ALTAR.get()), 2),
                new Category("fluxia_network", "Fluxia Networks",
                        "Flowers, pools, spitters and wands: making current and moving it.",
                        id(BlockRegistry.MANA_POOL.get()), 3),
                new Category("willow_wilds", "Willow & Wilds",
                        "The tree, the ores, the soils, and the small magics of the world.",
                        id(BlockRegistry.WILLOW_SAPLING.get()), 5),
                new Category("foundations", "Foundations & Infusion",
                        "Runes beneath the altar, and the pool that makes them.",
                        id(BlockRegistry.RUNES.get(RuneType.BLANK).get()), 4),
                new Category("rituals_minor", "Minor Works",
                        "Small circles and everyday workings: cheap, quick, forgiving.",
                        id(ItemRegistry.AMETHYST_COAL.get()), 7),
                new Category("rituals_major", "Greater Works",
                        "Wider circles, living summons, and work that bends the ground.",
                        "minecraft:ender_eye", 8),
                new Category("rituals_grand", "Grand Convergences",
                        "Rites that dare the world to answer. Bring offerings.",
                        "minecraft:dragon_egg", 9),
                new Category("rituals_imbuing", "The Empyrean Arts",
                        "Awakened metal, what it grows into, and the rites that teach it one more trick.",
                        id(ItemRegistry.EMPYRITE_SWORD.get()), 10)
        );
    }

    // ==================================================================
    // Entries
    // ==================================================================

    private static List<Entry> entries() {
        List<Entry> all = new ArrayList<>();
        all.addAll(introEntries());
        all.addAll(chalkEntries());
        all.addAll(altarEntries());
        all.addAll(fluxiaEntries());
        all.addAll(wildsEntries());
        all.addAll(foundationEntries());
        all.addAll(empyreanEntries());
        all.addAll(ritualEntries());
        return all;
    }

    // ---------------- Intro ----------------

    private static List<Entry> introEntries() {
        Entry what = new Entry("intro", "what_is_fluxia", "What is Fluxia?",
                id(ItemRegistry.FLUXIA_TABLET.get()))
                .sortnum(0).priority(true).readByDefault(true)
                .page(text(color(0xA28BFF, "Fluxia") + " is the subtle current that slips between mana, "
                        + "light and memory. It gathers in crystal, clings to roots and hums along chalk, "
                        + "but it is never quite any of those things."
                        + P + "The Wisp & Willow style does not force Fluxia. It coaxes it — with circles "
                        + "of chalk, carved fluxite, and tools that prefer guidance to command."))
                .page(text("Reading the Numbers",
                        "Fluxia is counted in " + thing("ƒ") + "."
                        + P + LI + "A " + item("Solrosa") + " makes 1 ƒ every three seconds."
                        + LI + "A " + item("Luxinia's Flower") + " makes 10 ƒ a second while it burns."
                        + LI + "A " + item("Willow Fluxia Pool") + " holds 40,000 ƒ."
                        + LI + "The cheapest rite in this book costs 20 ƒ; the most expensive, 3,000 ƒ."
                        + P + "Every device in the book lists its capacity and its rate. Where a number "
                        + "appears here, it is the number the code actually uses."))
                .page(text("How to Read This Book",
                        LI + "Blue text is a " + link("intro/getting_started", "link") + " to another entry."
                        + LI + "Crafting pages show the real recipe — hover a slot to see alternatives."
                        + LI + "Ritual pages have a " + bold("multiblock preview") + ". Press the eye "
                        + "icon to project the circle into the world and walk the layout."
                        + P + "Every ritual entry in this grimoire is generated from the same table the "
                        + "altar checks against, so a circle drawn here is the circle that works."));

        Entry start = new Entry("intro", "getting_started", "Getting Started",
                id(ItemRegistry.CHALK.get(DyeColor.WHITE).get()))
                .sortnum(1).priority(true)
                .page(text("First Steps",
                        "You need four things before any of the rest of this book is useful."
                        + P + LI + bold("1.") + " A " + item("Willow Sapling") + ", coaxed out of "
                        + link("willow_wilds/willow_infusion", "still water") + "."
                        + LI + bold("2.") + " " + item("White Chalk") + ", and a dye for every colour "
                        + "a circle asks for."
                        + LI + bold("3.") + " A " + item("Willow Altar") + ", built from willow planks "
                        + "and amethyst."
                        + LI + bold("4.") + " Something making " + thing("Fluxia") + " and a way to get "
                        + "it into the altar."))
                .page(text("A Working First Hour",
                        LI + "Drop an " + item("Oak Sapling") + " and a " + item("Deepslate") + " block "
                        + "into a water source. Plant the willow that floats up."
                        + LI + "Craft " + item("White Chalk") + " from white concrete powder and water bottles."
                        + LI + "Craft the " + item("Willow Altar") + "."
                        + LI + "Chalk the " + link("rituals_minor/minor_focus", "Minor Focus") + " circle — "
                        + "four white marks, 20 ƒ."
                        + LI + "Run " + link("rituals_minor/magicka_solidificus", "Magicka Solidificus")
                        + " for fluxite, then build a " + link("fluxia_network/mana_pool", "pool") + "."))
                .page(text("Where the Power Comes From",
                        "An altar has its own reservoir of " + bold("20,000 ƒ") + ", but nothing fills it "
                        + "on its own."
                        + P + "Craft a " + link("fluxia_network/flux_linking_wand", "Thornrose Staff")
                        + ", tap a generator, then tap the altar. That link is the whole of Fluxia "
                        + "logistics: source first, destination second."
                        + P + "Until an altar is linked to something, every rite in this book will tell "
                        + "you it does not have enough Fluxia — and it will be right."));

        return List.of(what, start);
    }

    // ---------------- Chalk & sigils ----------------

    private static List<Entry> chalkEntries() {
        Entry basics = new Entry("chalk_and_sigils", "chalk_basics", "Chalk Foundations",
                id(ItemRegistry.CHALK.get(DyeColor.WHITE).get()))
                .sortnum(0).priority(true)
                .page(crafting("wispwillow:chalk_white", "White Chalk",
                        "White concrete powder over three water bottles. Everything else in this "
                        + "chapter starts here."))
                .page(text("Colouring Chalk",
                        "Every other chalk is one white stick plus one vanilla dye, shapeless."
                        + P + "There are sixteen colours because rituals distinguish all sixteen. "
                        + "A circle that wants " + color(0xC0C0C0, "light grey") + " will not accept "
                        + color(0x808080, "grey") + ", and the altar will mark the offending glyph."))
                .page(crafting("wispwillow:chalk_magenta", "Any Colour",
                        "Shown with magenta; the same shape works for all fifteen coloured chalks."))
                .page(text("Drawing",
                        "Right-click the ground with chalk to lay a " + item("Chalk Sigil") + ". "
                        + "Sigils are flat, walk-through and break instantly, so a mistake costs nothing."
                        + P + "Sigils must all sit at the altar's own height. A circle drawn on a slope "
                        + "will not match, however good it looks."));

        Entry colours = new Entry("chalk_and_sigils", "chalk_colors", "The Sixteen Chalks",
                id(ItemRegistry.CHALK.get(DyeColor.MAGENTA).get()))
                .sortnum(1)
                .pages(chalkColourPages());

        Entry glyphs = new Entry("chalk_and_sigils", "sigil_types", "The Seven Glyphs",
                id(ItemRegistry.CHALK.get(DyeColor.PURPLE).get()))
                .sortnum(2)
                .page(text("Seven Shapes",
                        "Colour says " + italic("which") + " current a mark carries. Shape says "
                        + italic("what it does with it") + "."
                        + P + "There are seven glyph shapes. Any chalk can draw any of them; the shape is "
                        + "chosen by the sigil itself as you place it, and shown in the block's name."))
                .pages(sigilTypePages());

        Entry geometry = new Entry("chalk_and_sigils", "reading_a_circle", "Reading a Circle",
                id(BlockRegistry.SIGIL.get()))
                .sortnum(3)
                .page(text("Coordinates",
                        "Every placement in this book is written " + bold("(east, south)") + " relative "
                        + "to the altar."
                        + P + LI + "(0,-3) is three blocks " + bold("north") + "."
                        + LI + "(3,0) is three blocks " + bold("east") + "."
                        + LI + "(-1,-1) is one block north-west."
                        + P + "The altar itself is (0,0) and never carries a glyph."))
                .page(text("Radius",
                        "A circle's " + bold("radius") + " is the distance from the altar to its "
                        + "outermost mark, measured as the larger of the two axes."
                        + P + LI + "Radius 1 — a 3x3 circle."
                        + LI + "Radius 3 — a 7x7 circle."
                        + LI + "Radius 8 — a 17x17 circle, the largest in this book."
                        + P + "The altar scans out to the largest radius any known rite uses, so a "
                        + "stray sigil far from the centre can change which rite it thinks you meant."))
                .page(text("Orientation",
                        "Circles do " + bold("not") + " rotate."
                        + P + "A rite drawn ninety degrees around will not match, even though it looks "
                        + "identical. If an asymmetric circle refuses to complete, check the compass "
                        + "before you check the chalk."
                        + P + "Most circles in this book are symmetric and so the question never comes "
                        + "up — but " + link("rituals_major/crystallized_fertilization",
                        "Crystallized Fertilization") + " is deliberately lopsided."));

        return List.of(basics, colours, glyphs, geometry);
    }

    private static List<JsonObject> chalkColourPages() {
        record Group(String title, String blurb, DyeColor[] colors) {}

        Group[] groups = {
                new Group("Light & Shadow",
                        "Structure, absence and sharp contrast. The workhorses of large circles.",
                        new DyeColor[]{DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK}),
                new Group("Flame & Bloom",
                        "Fire, urgency and living heat.",
                        new DyeColor[]{DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.PINK, DyeColor.BROWN}),
                new Group("Leaf & Moss",
                        "Growth and rooted magic. Most crop and fertility work prefers these.",
                        new DyeColor[]{DyeColor.LIME, DyeColor.GREEN}),
                new Group("Sky & Song",
                        "Air, water, distance and echo.",
                        new DyeColor[]{DyeColor.LIGHT_BLUE, DyeColor.CYAN, DyeColor.BLUE,
                                DyeColor.PURPLE, DyeColor.MAGENTA}),
        };

        List<JsonObject> pages = new ArrayList<>();
        for (Group g : groups) {
            StringBuilder sb = new StringBuilder(g.blurb()).append(P);
            for (DyeColor c : g.colors()) {
                sb.append(LI).append(color(c.getTextureDiffuseColor(),
                        RitualEntryFactory.prettify(c.getName()) + " Chalk"));
            }
            pages.add(text(g.title(), sb.toString()));
        }
        return pages;
    }

    private static List<JsonObject> sigilTypePages() {
        record Glyph(SigilType type, String blurb) {}

        Glyph[] glyphs = {
                new Glyph(SigilType.BASE,
                        "A plain mark. It anchors and it holds; it does nothing clever. Most circles "
                        + "are mostly Base glyphs, and every large rite uses them as filler along its rings."),
                new Glyph(SigilType.RUNES,
                        "Dense script. Runes carry instruction rather than power, and they appear "
                        + "wherever a rite has to be told something specific about its outcome."),
                new Glyph(SigilType.STAR,
                        "A radiant point. Stars pull from far away, which is why the widest circles "
                        + "put them at the cardinal points where their reach is longest."),
                new Glyph(SigilType.TRIAD,
                        "Three bound together. Triads stabilise — they appear where a working would "
                        + "otherwise come apart under its own weight."),
                new Glyph(SigilType.SERPENT,
                        "A coiled line. Serpents move current around a ring rather than holding it in "
                        + "place, and pair naturally with Triads at the corners."),
                new Glyph(SigilType.ECLIPSE,
                        "A covered disc. Eclipses subtract: they take something out of the world rather "
                        + "than adding to it, and they dominate the darker circles."),
                new Glyph(SigilType.HEX,
                        "Six-fold interference. The rarest shape in this book, used where a rite has to "
                        + "act on matter that is already enchanted or already alive."),
        };

        List<JsonObject> pages = new ArrayList<>();
        for (Glyph g : glyphs) {
            pages.add(text(RitualEntryFactory.prettify(g.type().getSerializedName()), g.blurb()));
        }
        return pages;
    }

    // ---------------- Altar ----------------

    private static List<Entry> altarEntries() {
        Entry altar = new Entry("altar", "willow_altar", "The Willow Altar",
                id(BlockRegistry.ALTAR.get()))
                .sortnum(0).priority(true)
                .page(crafting("wispwillow:altar", "Willow Altar",
                        "Willow planks and amethyst. The whole of ritual magic runs through this block."))
                .page(text("What It Does",
                        "Once a second the altar reads the ground around it, compares what it finds "
                        + "against every known rite, and settles on the closest match."
                        + P + "It then shows you that guess: ghost glyphs where marks are missing, "
                        + color(0xFF4444, "red sparks") + " where a mark is the wrong shape, the "
                        + "reagents orbiting above it, and the result turning slowly at the top."
                        + P + "Right-click to attempt the rite."))
                .page(text("Reading the Altar",
                        LI + bold("Translucent glyphs") + " — a required mark is missing. Draw it."
                        + LI + color(0xFF4444, bold("Red sparks")) + " — there is a sigil there, but it "
                        + "is the wrong shape."
                        + LI + bold("Orbiting items") + " — reagents the rite still wants on the ground."
                        + LI + bold("Spinning result") + " — what you will get."
                        + P + "If nothing appears at all, the altar has not recognised a rite. Draw one "
                        + "more glyph from whichever circle you intended and it will lock on."))
                .page(text("Reserve & Cost",
                        LI + bold("Reserve: ") + thing("20,000 ƒ") + " held inside the altar."
                        + LI + bold("Spent: ") + "the rite's full cost, taken the moment it completes."
                        + LI + bold("Refused: ") + "if the reserve is short, the altar tells you exactly "
                        + "how much it has and how much it needs."
                        + P + "Fluxia is only spent on success. A rite that fails for a missing glyph or "
                        + "a missing reagent costs you nothing."))
                .page(text("Reagents",
                        "Reagents are " + bold("dropped on the ground") + ", not placed in a slot. "
                        + "Anything lying within the circle counts."
                        + P + "Buckets are containers, not offerings: the altar takes the liquid and "
                        + "hands the empty bucket back."
                        + P + "Items are only consumed when every other condition is already satisfied, "
                        + "so an incomplete circle will never eat your nether star."));

        Entry powering = new Entry("altar", "powering_the_altar", "Powering a Rite",
                id(BlockRegistry.MANA_POOL.get()))
                .sortnum(1)
                .page(text("Feeding the Stone",
                        "An altar generates nothing. It has to be given Fluxia."
                        + P + "The usual arrangement is a " + link("fluxia_network/luxinias_flower",
                        "Luxinia's Flower") + " into a " + link("fluxia_network/mana_pool", "pool")
                        + ", and the pool linked to the altar."
                        + P + "A pool pushes up to " + thing("800 ƒ") + " per target per second, so a "
                        + "full pool can top an empty altar off in under half a minute."))
                .page(text("Sizing a Supply",
                        LI + link("rituals_minor/minor_focus", "Minor Focus") + " — 20 ƒ. Two seconds "
                        + "of one flower."
                        + LI + link("rituals_minor/minor_arcana", "Minor Arcana") + " — 300 ƒ. One "
                        + "Amethyst Coal exactly."
                        + LI + link("rituals_major/starlight_cross", "Starlight Cross") + " — 1,500 ƒ."
                        + LI + link("rituals_grand/shrouded_conclave", "Shrouded Conclave") + " — 3,000 ƒ."
                        + P + "One Amethyst Coal burns for thirty seconds at 10 ƒ a second, so a flower "
                        + "converts each coal into " + thing("300 ƒ") + "."))
                .page(text("Why Isn't It Working?",
                        LI + bold("\"No valid ritual found\"") + " — no sigil near enough for the altar "
                        + "to guess at. Draw one."
                        + LI + bold("\"missing N glyph(s)\"") + " — look for the ghosts."
                        + LI + bold("\"not enough fluxia\"") + " — the message shows held vs. needed."
                        + LI + bold("\"missing reagents nearby\"") + " — drop them inside the circle."
                        + P + "If the altar names a rite you did not intend, you have accidentally drawn "
                        + "a better match for something else. Check for leftover chalk from a previous "
                        + "circle before you re-draw."));

        return List.of(altar, powering);
    }

    // ---------------- Fluxia network ----------------

    private static List<Entry> fluxiaEntries() {
        Entry basics = new Entry("fluxia_network", "network_basics", "Network Basics",
                id(BlockRegistry.MANA_POOL.get()))
                .sortnum(0).priority(true)
                .page(text("Three Kinds of Node",
                        LI + color(0x7FE8FF, "Pools") + " store Fluxia and share it out across "
                        + "everything they are linked to."
                        + LI + color(0xFFAA55, "Spitters") + " hold a charge and fire it at exactly one "
                        + "target."
                        + LI + color(0xA28BFF, "Machines") + " — flowers, altars — either make Fluxia or "
                        + "spend it."
                        + P + "Nodes do not connect by touching. They connect because you told them to, "
                        + "with the " + link("fluxia_network/flux_linking_wand", "linking wand") + "."))
                .page(text("Priority",
                        "When a pool distributes, it fills in a fixed order:"
                        + P + LI + bold("1.") + " Machines that want power."
                        + LI + bold("2.") + " Batteries."
                        + LI + bold("3.") + " Other pools."
                        + LI + bold("4.") + " Anything else on the graph."
                        + P + "Machines first means an altar mid-rite always wins over topping up a "
                        + "second pool. Links are followed transitively, so A→B→C forms one network."))
                .page(text("A Starter Layout",
                        item("Luxinia's Flower") + " → " + item("Willow Fluxia Pool") + " → "
                        + item("Willow Altar") + "."
                        + P + "That is enough for every rite in " + link("rituals_minor/minor_focus",
                        "Minor Works") + " and most of the Greater ones."
                        + P + "For the grand rites, add pools. Linking one pool into another lets them "
                        + "equalise, so a bank of pools behaves as one large reservoir that a single "
                        + "flower can fill overnight."));

        Entry pool = new Entry("fluxia_network", "mana_pool", "Willow Fluxia Pool",
                id(BlockRegistry.MANA_POOL.get()))
                .sortnum(1)
                .page(crafting("wispwillow:mana_pool", "Willow Fluxia Pool",
                        "A shallow fluxite basin. The backbone of any serious setup."))
                .page(text("Fluxia by the Bucket",
                        "Right-click the pool with an " + bold("empty bucket") + " and you get a "
                        + item("Bucket of Fluxia") + ", drawn straight out of the reserve. Right-click "
                        + "with a full one and it goes back in."
                        + P + LI + bold("Rate: ") + thing(ManaPoolBlock.FLUXIA_PER_BUCKET + " \u0192")
                        + " per bucket, both directions"
                        + LI + bold("Loss: ") + "none"
                        + P + "This is where liquid Fluxia comes from. The "
                        + link("fluxia_network/alchemical_cauldron", "Alchemical Cauldron")
                        + " wants it by the tank for half its brews, and a pool with a generator "
                        + "behind it will fill buckets all day."))
                .page(text("Numbers",
                        LI + bold("Capacity: ") + thing("40,000 ƒ")
                        + LI + bold("Output: ") + thing("800 ƒ") + " per linked target, once a second"
                        + LI + bold("Tablet drain: ") + thing("800 ƒ") + " per pull"
                        + P + "A pool that is linked to nothing simply fills up and waits."))
                .page(text("Drinking Tablets",
                        "Throw a charged " + link("fluxia_network/fluxia_tablet", "Fluxia Tablet")
                        + " into a pool and the pool empties it, renaming the floating item as it goes "
                        + "so you can watch the level fall."
                        + P + "This is the practical way to move Fluxia between bases: fill a tablet at "
                        + "one pool, walk, throw it into another."));

        Entry luxinia = new Entry("fluxia_network", "luxinias_flower", "Luxinia's Flower",
                id(BlockRegistry.LUXINIAS_FLOWER.get()))
                .sortnum(2)
                .page(spotlight(BlockRegistry.LUXINIAS_FLOWER.get(), "The Patient Generator",
                        "Drop " + item("Amethyst Coal") + " onto the flower and it eats one piece at a "
                        + "time, glowing and trailing sparks while it burns."))
                .page(text("Numbers",
                        LI + bold("Output: ") + thing("10 ƒ") + " a second while burning"
                        + LI + bold("Burn time: ") + "30 seconds per Amethyst Coal"
                        + LI + bold("Yield: ") + thing("300 ƒ") + " per coal"
                        + LI + bold("Buffer: ") + thing("800 ƒ")
                        + LI + bold("Push rate: ") + thing("40 ƒ") + " per target per second"
                        + P + "The flower will not eat fuel while its buffer is full, so a hopper feeding "
                        + "one coal at a time wastes nothing."))
                .page(text("Feeding It",
                        "Items dropped just above the flower are picked up automatically, which makes "
                        + "the obvious build a hopper pointed down into it."
                        + P + "Amethyst Coal comes from " + link("rituals_minor/minor_arcana",
                        "Minor Arcana") + ": one coal block becomes eighteen. That single rite is why "
                        + "the flower is worth building at all."));

        Entry solrosa = new Entry("fluxia_network", "solrosa", "Solrosa",
                id(BlockRegistry.SOLROSA_FLOWER.get()))
                .sortnum(3)
                .page(spotlight(BlockRegistry.SOLROSA_FLOWER.get(), "The Free Trickle",
                        "Solrosa needs no fuel at all. It simply produces, slowly, forever."))
                .page(text("Numbers",
                        LI + bold("Output: ") + thing("1 ƒ") + " every three seconds"
                        + LI + bold("Buffer: ") + thing("200 ƒ")
                        + LI + bold("Push rate: ") + thing("40 ƒ") + " per target per second"
                        + P + "That is 20 ƒ a minute — a " + link("rituals_minor/minor_focus",
                        "Minor Focus") + " every sixty seconds from a flower that costs nothing to run."
                        + P + "Solrosa scales by planting more of them, and a field of them will keep a "
                        + "pool topped up while you are away."))
                .page(text("Blooming One",
                        "Place or find a " + item("Wither Rose") + ", then drop a "
                        + item("Prismarine Shard") + " onto it."
                        + P + "After a moment the rose reacts and a " + item("Solrosa") + " grows in its "
                        + "place. See " + link("willow_wilds/world_rites", "World Rites") + " for the "
                        + "other transformations of this kind."));

        Entry spitter = new Entry("fluxia_network", "fluxia_spitter", "Fluxia Spitter",
                id(BlockRegistry.FLUXIA_SPITTER.get()))
                .sortnum(4)
                .page(crafting("wispwillow:fluxia_spitter", "Fluxia Spitter",
                        "A fluxite housing wrapped around a tablet. Fires a visible beam of Fluxia at "
                        + "whatever it is aimed at."))
                .page(text("Numbers",
                        LI + bold("Buffer: ") + thing("5,000 ƒ")
                        + LI + bold("Output: ") + thing("40 ƒ") + " a second"
                        + LI + bold("Targets: ") + "exactly one"
                        + P + "Linking a spitter to a new target " + bold("replaces") + " the old link. "
                        + "This is deliberate: a spitter is a directed hose, not a manifold."))
                .page(text("When to Use One",
                        "Pools already distribute to everything they are linked to, so a spitter is not "
                        + "a way to move more power — it is a way to move " + italic("less") + "."
                        + P + "Put one between a large pool and a hungry machine when you want that "
                        + "machine limited to a steady 40 ƒ a second, or when you want the beam itself "
                        + "as a visible sign that a line is live."));

        Entry tablet = new Entry("fluxia_network", "fluxia_tablet", "Fluxia Tablet",
                id(ItemRegistry.FLUXIA_TABLET.get()))
                .sortnum(5)
                .page(crafting("wispwillow:fluxia_tablet", "Fluxia Tablet",
                        "Fluxite around an astralite nugget and a netherite core."))
                .page(text("Numbers",
                        LI + bold("Capacity: ") + thing(FluxiaTabletItem.CAPACITY + " ƒ")
                        + LI + bold("Transfer: ") + thing(FluxiaTabletItem.IN_RATE + " ƒ") + " per operation"
                        + P + "Hover the tablet to read its charge. A tablet holds exactly as much as an "
                        + "altar's whole reserve, which makes it the simplest way to prime a remote "
                        + "circle: fill it at home, carry it out, throw it in the pool."))
                .page(text("As a Battery",
                        "Tablets are also the mod's portable battery. A "
                        + link("fluxia_network/mana_pool", "pool") + " will drain one thrown into it, "
                        + "and flowers will sip from one when their own reserves run low."
                        + P + "The creative tab contains both an empty and a full tablet."));

        Entry wand = new Entry("fluxia_network", "flux_linking_wand", "Thornrose Staff",
                id(ItemRegistry.FLUX_LINKING_WAND.get()))
                .sortnum(6).priority(true)
                .page(crafting("wispwillow:flux_linking_wand", "Thornrose Staff",
                        "Sticks for the shaft, an amethyst shard for the heart."))
                .page(text("Linking",
                        LI + bold("Right-click") + " a node to store it as the " + italic("source") + "."
                        + LI + bold("Right-click") + " a second node to make it the " + italic("target") + "."
                        + LI + bold("Sneak + right-click") + " to forget the stored source."
                        + P + "Links are one-way: Fluxia prefers to move from the first node into the "
                        + "second. Source first, destination second — the wand will tell you which it "
                        + "just recorded."))
                .page(text("Rules",
                        LI + "Pools and machines may link to many targets."
                        + LI + "Spitters keep exactly one outgoing link."
                        + LI + "Both ends must be in the same dimension; the wand refuses otherwise."
                        + P + "Links survive breaking and replacing nothing — if you break a node, "
                        + "re-link it. There is no penalty for re-linking something that is already "
                        + "linked."));

        Entry loader = new Entry("fluxia_network", "chunk_loader", "Chunk Loader",
                id(BlockRegistry.CHUNK_LOADER.get()))
                .sortnum(7)
                .page(crafting("wispwillow:chunk_loader", "Chunk Loader",
                        "Amethyst, a diamond, crying obsidian and a nether star. It is meant to be a "
                        + "late purchase."))
                .page(text("Radius",
                        "Right-click to cycle the radius from 0 to 4, in chunks:"
                        + P + LI + bold("0") + " — 1x1 chunk (just this one)"
                        + LI + bold("1") + " — 3x3 chunks"
                        + LI + bold("2") + " — 5x5 chunks"
                        + LI + bold("3") + " — 7x7 chunks"
                        + LI + bold("4") + " — 9x9 chunks"
                        + P + "The current setting is shown above your hotbar as you cycle it. Breaking "
                        + "the block releases every chunk it was holding."))
                .page(text("What It's For",
                        "Fluxia generation and ritual altars all stop dead in unloaded chunks."
                        + P + "One loader at radius 1 is usually enough to keep a whole base alive. "
                        + "Prefer the smallest radius that covers what you need — loaded chunks are the "
                        + "most expensive thing a server does."));


        Entry goggles = new Entry("fluxia_network", "arcane_goggles", "Arcane Goggles",
                id(ItemRegistry.ARCANE_GOGGLES.get()))
                .sortnum(8).priority(true)
                .page(crafting("wispwillow:arcane_goggles", "Arcane Goggles",
                        "An astralite frame, amethyst for the lenses, glass to see through. "
                        + "One point of armour and nothing else — you wear these to look, not to fight."))
                .page(text("Seeing Fluxia",
                        "Wear them on your head, or simply hold them in either hand."
                        + P + "While they are active, every Fluxia node within "
                        + bold(ArcaneVisionRange()) + " blocks shows a floating gauge, and the "
                        + link("fluxia_network/flux_linking_wand", "links") + " between nodes are "
                        + "drawn as faint violet threads."
                        + P + "The gauge runs " + color(0xE04050, "red") + " when empty, through "
                        + color(0xB28BFF, "violet") + ", to " + color(0x7FE8FF, "cyan") + " when full."))
                .page(text("The Readout",
                        "Look directly at a device and a panel opens beside your crosshair with its "
                        + "exact numbers."
                        + P + LI + bold("Pools, spitters, flowers") + " — stored Fluxia, rate, links."
                        + LI + bold("The altar") + " — reserve, the rite it has recognised, its cost, "
                        + "and whether the circle is complete."
                        + P + "The altar readout is the fastest way to find out why a rite will not fire."))
                .page(text("Arcs",
                        "Transfers are drawn as a mote of light travelling from source to destination, "
                        + "and " + bold("those are visible to everyone") + " — goggles or not."
                        + P + "A fatter, brighter arc means a larger transfer. If a network looks "
                        + "dark, nothing is moving: check the generator before you check the wiring."));

        return List.of(basics, pool, fluxiaLiquid(), luxinia, solrosa, generators(), spitter, tablet, wand,
                loader, goggles, alchemicalCauldron(), cauldronUpgrades(), empyrite());
    }

    /** The cauldron: the mod's only multiblock machine, and its only fluid machine. */
    private static Entry alchemicalCauldron() {
        return new Entry("fluxia_network", "alchemical_cauldron", "The Alchemical Cauldron",
                id(BlockRegistry.ALCHEMICAL_CAULDRON.get()))
                .sortnum(9)
                .page(text("A Pot That Listens",
                        "Every other Fluxia machine does one thing to one stack. The cauldron holds "
                        + "four reagents and a body of liquid at once, and does not care what order "
                        + "you put them in."
                        + P + "It is a pot. Pots do not have input faces."
                        + P + "It is also the only thing in this book you " + bold("build")
                        + " rather than place: a circle, four pillars, and room to stand between "
                        + "them."))
                .page(crafting("wispwillow:alchemical_cauldron", "Alchemical Cauldron",
                        "Astralite for the rim, " + item("Fluxite") + " for the body."
                        + P + "Expensive, but the leaching brew pays it back inside a stack of raw "
                        + "astralite."))
                .page(BookJson.multiblockLayers("The Ring", cauldronLayers(),
                        List.of("A", "W", "F", "0"),
                        List.of("wispwillow:astralite_block", "wispwillow:willow_log",
                                "wispwillow:fluxite", "wispwillow:alchemical_cauldron"),
                        "A circle of " + item("Fluxite") + " cut into the floor, four "
                        + item("Willow Log") + " pillars two blocks tall standing at the corners "
                        + "of a five by five, and an " + item("Astralite Block") + " capping each. "
                        + CauldronPattern.BLOCK_COUNT + " blocks and the pot."
                        + P + "Leave the column above the mouth open. Note the gap at the centre "
                        + "of the circle, directly under the pot: that one is yours. Fluxite makes "
                        + "the floor look whole; a hopper drains it."
                        + P + "Nothing at the pot's own level is asked for, so pipes, chests and "
                        + "importers may stand against its sides without breaking the ring."))
                .page(text("Lighting It",
                        "Right-click the pot once the ring is closed. Until then it will tell you "
                        + "which block is missing, and where."
                        + P + LI + bold("To light: ")
                        + thing(GoggleNumber(AlchemicalCauldronBlock.IGNITE_COST))
                        + LI + bold("To brew: ") + "whatever the recipe asks, spread across its run"
                        + P + "Like the " + link("foundations/soul_gateway", "gateway") + ", it will "
                        + "not draw power by touch — point a "
                        + link("fluxia_network/flux_linking_wand", "linking wand") + " at a charged "
                        + "pool, then at the cauldron."
                        + P + "It draws per tick rather than up front, so a pot on a thin "
                        + "supply slows down instead of refusing to start."
                        + P + bold("Sneak and right-click") + " a lit pot to put it out. Breaking a "
                        + "block out of the ring does the same thing, less politely. Either way the "
                        + "lighting cost is spent — relighting is another "
                        + thing(GoggleNumber(AlchemicalCauldronBlock.IGNITE_COST)) + "."))
                .page(text("Feeding It",
                        "By hand, by bucket, or by pipe."
                        + P + LI + "Any side but the bottom takes reagents and liquid."
                        + LI + "The bottom gives up finished goods — which is what the gap in "
                        + "the plinth is for."
                        + LI + "A held bucket works at any stage, even before the ring is built — "
                        + "it is still a cauldron."
                        + P + "The two chevrons under the tanks pour them out. There is no undo on "
                        + "that; it is there for when you have filled the reagent tank with the "
                        + "wrong thing, which you will."
                        + P + "The arrow between them pours the decanting tank back into the "
                        + "reagent tank, for the brews that make a liquid the next brew wants."
                        + P + bold("Where the liquid comes from: ") + "dip an empty bucket in a "
                        + link("fluxia_network/mana_pool", "Willow Fluxia Pool") + ". A bucket is "
                        + bold(ManaPoolBlock.FLUXIA_PER_BUCKET + " \u0192") + " either way, so a pool "
                        + "doubles as the tap."))
                .page(text("What It Brews",
                        AlchemyDefinitions.tiers().size() + " tiers, and none of them unlock an item "
                        + "you could not already get. The cauldron makes routes cheaper; it is not "
                        + "a gate."
                        + P + LI + bold("Steeping: ") + "cheap, immediate, no charge to speak of."
                        + LI + bold("Leaching: ") + "ore doubling. This is the one that pays."
                        + LI + bold("Condensing: ") + "solids into fluid and back, which is what "
                        + "makes the tanks worth plumbing."
                        + LI + bold("Transmutation: ") + "a project, not a habit."
                        + P + "Every brew follows."))
                .pages(alchemyPages());
    }

    /** One page per brew, generated from the same table the recipes come from. */
    private static List<JsonObject> alchemyPages() {
        List<JsonObject> pages = new ArrayList<>();
        for (AlchemyDefinitions.Def def : AlchemyDefinitions.all()) {
            StringBuilder sb = new StringBuilder(def.lore()).append(P);

            for (ItemStack in : def.inputs()) {
                sb.append(LI).append(bold(in.getCount() + "x ")).append(item(displayName(in)));
            }
            if (!def.fluidInput().isEmpty()) {
                sb.append(LI).append(bold(def.fluidInput().getAmount() + " mB "))
                        .append(item(fluidName(def.fluidInput())));
            }

            sb.append(LI).append(bold("Cost: ")).append(thing(GoggleNumber(def.fluxia())));
            sb.append(LI).append(bold("Takes: ")).append(seconds(def.ticks()));

            // Fluid-output brews have no item to spotlight, so they get a plain page headed
            // by what they actually make.
            if (def.result().isEmpty()) {
                sb.append(LI).append(bold("Yields: "))
                        .append(def.fluidResult().getAmount()).append(" mB ")
                        .append(item(fluidName(def.fluidResult())))
                        .append(P).append("Collected in the decanting tank, not a slot.");
                pages.add(text(def.tier() + ": " + fluidName(def.fluidResult()), sb.toString()));
            } else {
                pages.add(spotlightStack(def.result(),
                        def.result().getCount() + "x " + displayName(def.result()), sb.toString()));
            }
        }
        return pages;
    }

    private static String fluidName(net.neoforged.neoforge.fluids.FluidStack stack) {
        // Same trap as displayName: this mod's lang file is being written by this very run,
        // so getHoverName would hand back a raw translation key. Derive from the registry.
        String path = net.minecraft.core.registries.BuiltInRegistries.FLUID
                .getKey(stack.getFluid()).getPath();
        return RitualEntryFactory.prettify(path.replace("_fluid", ""));
    }

    private static String seconds(int ticks) {
        return String.format(java.util.Locale.ROOT, "%.0f seconds", ticks / 20.0f);
    }

    /**
     * The cauldron pattern as Patchouli wants it: topmost layer first. Astralite caps,
     * then the willow pillars around the pot, then the fluxite plinth it stands on.
     */
    private static List<List<String>> cauldronLayers() {
        return List.of(
                List.of("A   A",
                        "     ",
                        "     ",
                        "     ",
                        "A   A"),
                List.of("W   W",
                        "     ",
                        "     ",
                        "     ",
                        "W   W"),
                List.of("W   W",
                        "     ",
                        "  0  ",
                        "     ",
                        "W   W"),
                List.of("  F  ",
                        " FFF ",
                        "FF FF",
                        " FFF ",
                        "  F  "));
    }

    /** The upgrade tray, and why it is not simply "more of everything". */
    private static Entry cauldronUpgrades() {
        return new Entry("fluxia_network", "cauldron_upgrades", "Tuning the Cauldron",
                id(ItemRegistry.UPGRADE_HASTE.get()))
                .sortnum(10)
                .page(text("The Tray",
                        "Three gold-framed slots on the right of the cauldron's interface. Drop "
                        + "upgrades in and they take effect immediately; take them out and it "
                        + "goes back to what it was."
                        + P + "They are not reagents. Nothing pipes into the tray and nothing "
                        + "pipes out of it — a hopper that helpfully filled this with whatever it "
                        + "was carrying would be an expensive accident."))
                .page(spotlight(ItemRegistry.UPGRADE_HASTE.get(), "Ember Plate",
                        "Drives the fire harder. Each one takes about a fifth off the time every "
                        + "brew needs."
                        + P + "It does not make anything cheaper. Time and charge are separate "
                        + "problems and they have separate answers."))
                .page(spotlight(ItemRegistry.UPGRADE_THRIFT.get(), "Miser's Lining",
                        "Converter dust worked into the lining so less of the charge escapes as "
                        + "light. About a fifth off what a brew costs."))
                .page(spotlight(ItemRegistry.UPGRADE_CAPACITY.get(), "Deepening Sigil",
                        "Eight more buckets in each tank, both sides. Worth fitting the moment "
                        + "you are piping fluid in faster than a brew can drink it."))
                .page(text("Diminishing Returns",
                        "Every upgrade takes a fixed " + bold("fraction") + " of what is left, "
                        + "not a fixed amount. So the second Ember Plate is worth less than the "
                        + "first, and the tenth would be worth almost nothing."
                        + P + "That is deliberate. On a straight subtraction, five of them would "
                        + "have made brewing instant and free, and a machine with no running cost "
                        + "is not a machine, it is a button."
                        + P + "Wear the goggles and the cauldron will tell you exactly what the "
                        + "tray is currently buying you."));
    }

    /** The tier above astralite. */
    private static Entry empyrite() {
        return new Entry("fluxia_network", "empyrite", "Empyrite",
                id(ItemRegistry.EMPYRITE_INGOT.get()))
                .sortnum(11)
                .page(spotlight(ItemRegistry.EMPYRITE_INGOT.get(), "Empyrite",
                        "Astralite taken past what the star-metal will hold on its own."
                        + P + "There is no ore for it. No furnace will make it, no villager "
                        + "trades it, and it does not occur anywhere in the world. The cauldron "
                        + "is the only source there is."))
                .page(text("Where the Two Halves Meet",
                        "The brew wants " + item("Amethyst Coal") + ", and the only way to get "
                        + "that is the " + link("rituals_minor/minor_focus", "Minor Arcana") + " "
                        + "rite at an altar."
                        + P + "That is on purpose. Everything else in this book lets you pick a "
                        + "lane — rituals or machines — and stay in it. This one thing does not: "
                        + "to reach the top tier you have to have done both."))
                .page(text("What It Makes",
                        "A full set of tools and armour, a shade better than netherite in every "
                        + "number and a great deal better to enchant."
                        + P + LI + bold("Enchantability: ") + "22, against netherite's 15"
                        + LI + bold("Durability: ") + "2,800 on tools"
                        + LI + bold("Armour: ") + "one point over netherite, and it keeps the "
                        + "knockback resistance"
                        + P + "The tools are hafted with " + item("Astralite Ingot") + " rather "
                        + "than sticks. A tier this expensive held together with a twig would "
                        + "look silly in the hand, which is the one place you always see it."))
                .page(text("Renewable Stone",
                        "One more brew comes with the tier: " + item("Fluxite") + " grown out of "
                        + item("Fluxia Condensed Dust") + " and amethyst."
                        + P + "Slower than a pickaxe and far dearer, and the only thing in the "
                        + "mod that makes the stone renewable at all — which starts to matter "
                        + "once the circle under every cauldron wants twelve of it."));
    }

    private static String ArcaneVisionRange() {
        return String.valueOf(com.reynn.wispwillow.client.goggles.ArcaneVision.OVERLAY_RANGE);
    }

    // ---------------- Willow & Wilds ----------------

    private static List<Entry> wildsEntries() {
        Entry infusion = new Entry("willow_wilds", "willow_infusion", "Willow Infusion",
                id(BlockRegistry.WILLOW_SAPLING.get()))
                .sortnum(0).priority(true)
                .page(spotlight(BlockRegistry.WILLOW_SAPLING.get(), "The First Sapling",
                        "There is no recipe for a willow. There is a small world rite instead."
                        + P + "Find or place a " + bold("water source block") + ", then drop an "
                        + item("Oak Sapling") + " and a " + item("Deepslate") + " block into it."))
                .page(text("The Rite",
                        "After a moment the water reacts — a chime, a scatter of sparks — and a single "
                        + item("Willow Sapling") + " floats up."
                        + P + "One is enough. Willow saplings grow into willow trees, and willow trees "
                        + "drop more saplings, so the rite is only ever performed once."))
                .page(text("The Tree",
                        "A grown willow gives " + item("Willow Log") + ", " + item("Willow Leaves")
                        + ", and occasional " + item("Flowering Willow Leaves") + " and "
                        + item("Willow Vine") + "."
                        + P + "Willow planks make the full vanilla wood set — stairs, slabs, fences, "
                        + "gates, doors, trapdoors, buttons, pressure plates — and they are what the "
                        + link("altar/willow_altar", "altar") + " is built from."));

        Entry rites = new Entry("willow_wilds", "world_rites", "World Rites",
                id(BlockRegistry.SOLROSA_FLOWER.get()))
                .sortnum(1)
                .page(text("Magic Without an Altar",
                        "Two transformations in this mod need no circle, no chalk and no Fluxia. You "
                        + "simply put the right things in the right place and wait a moment."
                        + P + "They are the mod's on-ramp: both are performable the first night, with "
                        + "materials you already have."))
                .page(text("Willow Infusion",
                        LI + bold("Where: ") + "a water source block"
                        + LI + bold("Drop in: ") + item("Oak Sapling") + " + " + item("Deepslate")
                        + LI + bold("Get: ") + item("Willow Sapling")
                        + P + "Covered in full under "
                        + link("willow_wilds/willow_infusion", "Willow Infusion") + "."))
                .page(text("Solrosa Bloom",
                        LI + bold("Where: ") + "on a " + item("Wither Rose")
                        + LI + bold("Drop in: ") + item("Prismarine Shard")
                        + LI + bold("Get: ") + item("Solrosa") + ", growing in the rose's place"
                        + P + "A Solrosa is a free, permanent trickle of Fluxia. See "
                        + link("fluxia_network/solrosa", "Solrosa") + "."));

        Entry astralite = new Entry("willow_wilds", "astralite", "Astralite",
                id(ItemRegistry.ASTRALITE_INGOT.get()))
                .sortnum(2)
                .page(spotlight(BlockRegistry.ASTRALITE_ORE.get(), "Astralite Ore",
                        "Found underground in both stone and deepslate forms. It behaves like diamond "
                        + "ore does: mine it, then cook what you get."))
                .page(smelting("wispwillow:smelt_raw_astralite_to_astralite_ingot", "Smelting",
                        "Raw astralite becomes an ingot in a furnace, or twice as fast in a blast "
                        + "furnace. The ore blocks themselves can also be smelted directly."))
                .page(crafting2("wispwillow:astralite_ingot_to_block", "wispwillow:astralite_nugget_to_ingot",
                        "Packing", "Nine nuggets to an ingot, nine ingots to a block, both reversible."))
                .page(text("What It's For",
                        "Astralite is a structural metal rather than a magical one. It appears in the "
                        + link("fluxia_network/fluxia_tablet", "Fluxia Tablet") + " — a device that has "
                        + "to hold a great deal of something without leaking."));

        Entry fluxite = new Entry("willow_wilds", "fluxite", "Fluxite",
                id(BlockRegistry.FLUXITE.get()))
                .sortnum(3)
                .page(spotlight(BlockRegistry.FLUXITE.get(), "Stone That Remembers",
                        "Fluxite is ordinary stone that has been taught to hold a charge. It is the "
                        + "base material of every Fluxia device in the mod."))
                .page(text("Getting It",
                        "Fluxite has no crafting recipe. It comes from "
                        + link("rituals_minor/magicka_solidificus", "Magicka Solidificus") + ": six "
                        + "stone, a diamond and a lily of the valley become eight fluxite for 100 ƒ."
                        + P + "That is the first rite most players set up permanently, because pools, "
                        + "spitters and tablets all want it by the stack."));

        Entry coal = new Entry("willow_wilds", "amethyst_coal", "Amethyst Coal",
                id(ItemRegistry.AMETHYST_COAL.get()))
                .sortnum(4)
                .page(spotlight(ItemRegistry.AMETHYST_COAL.get(), "Violet Fire",
                        "Amethyst Coal burns for " + bold("3,200 ticks") + " — twice as long as ordinary "
                        + "coal — and it is the only fuel a "
                        + link("fluxia_network/luxinias_flower", "Luxinia's Flower") + " will eat."))
                .page(text("Making It",
                        "From " + link("rituals_minor/minor_arcana", "Minor Arcana") + ": two amethyst "
                        + "shards and one coal block become " + bold("eighteen") + " Amethyst Coal for "
                        + "300 ƒ."
                        + P + "Nine of them pack into an " + item("Amethyst Coal Block") + ", worth "
                        + bold("32,000 ticks") + " of furnace time — comfortably better than a coal block."))
                .page(crafting("wispwillow:willow_torch", "Willow Torch",
                        "Amethyst Coal on a stick gives four torches that burn violet. They are easier "
                        + "on the eyes over a chalk circle than orange ones."))
                .page(crafting("wispwillow:willow_lantern", "Willow Lantern",
                        "Iron nuggets around a willow torch, as usual. Also see the "
                        + item("Fluxia Lamp") + " for a full-block light."));

        Entry soils = new Entry("willow_wilds", "soils", "Strange Soils",
                id(BlockRegistry.SNAD.get()))
                .sortnum(5)
                .page(crafting("wispwillow:snad", "Snad",
                        "Four sand, bone meal and an amethyst shard make eight."))
                .page(text("What Snad Does",
                        "Snad accelerates whatever grows directly on top of it, and then keeps walking "
                        + "up the column doing the same to every block of the same kind above."
                        + P + LI + bold("Speed: ") + "16 extra random ticks per tick, by default"
                        + LI + bold("Height: ") + "up to 16 blocks of column"
                        + LI + bold("Applies to: ") + "sugar cane, cactus, bamboo, amethyst sugar cane"
                        + P + "All four numbers, and the list itself, are configurable in the server "
                        + "config under " + thing("snad") + "."))
                .page(spotlight(BlockRegistry.AMETHYST_FARMLAND.get(), "Amethyst Farmland",
                        "Crystal-threaded soil from "
                        + link("rituals_major/crystallized_fertilization", "Crystallized Fertilization")
                        + ". It never dries out and it never reverts to dirt when trampled."))
                .page(crafting("wispwillow:amethyst_sugar_cane", "Amethyst Sugar Cane",
                        "Three sugar cane and two shards. It grows taller than ordinary cane and is "
                        + "the intended partner for a Snad column."));

        Entry dust = new Entry("willow_wilds", "conversion_dust", "Conversion Dust",
                id(ItemRegistry.DUSTCONVERT.get()))
                .sortnum(6)
                .page(crafting("wispwillow:fluxia_converter_dust", "Fluxia Conversion Dust",
                        "A diamond, lapis and two redstone make eight doses of dust."))
                .page(text("Transmutation",
                        "Combine a dose of dust with an item in a crafting grid to turn it into its "
                        + "counterpart. Coal becomes charcoal; charcoal becomes coal."
                        + P + "The dust is consumed by the conversion, which is what keeps it honest."))
                .page(crafting("wispwillow:fluxia_condensed_dust", "Condensed Dust",
                        "Four doses bound with emerald. Condensed dust performs the same conversions "
                        + "and is the compact way to carry a stack of them."));

        Entry ench = new Entry("willow_wilds", "volatile_experiment", "Volatile Experiment",
                "minecraft:enchanted_book")
                .sortnum(7)
                .page(text("An Unwise Enchantment",
                        color(0xD69BFF, "Volatile Experiment") + " is a weapon enchantment up to "
                        + bold("level III") + ", found on the enchanting table like any other."
                        + P + "On hit, it sometimes does " + italic("something") + ". Which something is "
                        + "not up to you."))
                .page(text("What Can Happen",
                        LI + "You are blinded for five seconds."
                        + LI + "The victim receives " + bold("every") + " status effect in the game at once."
                        + LI + "Five to fifteen lightning bolts strike where the victim stands."
                        + LI + "The victim is flung a very long way — or, rarely, simply dies."
                        + P + "Higher levels trigger it more often. It is exclusive with the other "
                        + "damage enchantments, and it is not recommended for indoor use."))
                .page(text("Willow Hut",
                        "Small willow-built structures generate rarely in the overworld, stocked with a "
                        + "little of what a beginning practitioner needs."
                        + P + "Finding one early saves a fair amount of time; not finding one costs you "
                        + "nothing you could not make yourself."));

        List<Entry> entries = new ArrayList<>(List.of(infusion, rites, astralite, fluxite, coal, soils, dust, ench, oasisEntry()));
        entries.addAll(fenEntries());
        return entries;
    }

    // ---------------- The Willow Fen ----------------

    private static List<Entry> fenEntries() {
        Entry fen = new Entry("willow_wilds", "willow_fen", "The Willow Fen",
                id(BlockRegistry.WEEPING_LEAVES.get()))
                .sortnum(9)
                .page(text("Where the Water Stands Still",
                        "Somewhere the swamps turn wetter, and become something else. The water goes dark "
                        + "and green, mist hangs in the air, weeping trees lean over the pools trailing "
                        + item("Fen Moss") + ", and lilies glow faintly after dark."
                        + P + "Ask the world where: " + thing("/locate biome wispwillow:willow_fen") + "."))
                .page(text("Traces Elsewhere",
                        "Ordinary swamps carry a hint of it — " + item("Whisperreed") + " at the waterline "
                        + "and the odd " + item("Moonlily") + " — and rivers grow reeds along their banks."
                        + P + "Everything else is the fen's own: its trees, its pools, its peat, its birds, "
                        + "and the ruins in its reeds."))
                .page(spotlight(BlockRegistry.WEEPING_LEAVES.get(), "Not a Willow",
                        "The fen's weeping trees look like willows and are not. They are dark oak under a "
                        + "drooping crown, and their leaves drop sticks, never saplings."
                        + P + "The true willow still has to be "
                        + link("willow_wilds/willow_infusion", "coaxed out of still water") + "."))
                .page(text("Ruins in the Reeds",
                        "Someone built here before you. Three kinds of ruin lie half-swallowed by the marsh:"
                        + P + LI + bold("Fen shrines: ") + "moss-grown platforms with a lantern at each corner"
                        + LI + bold("Sunken ruins: ") + "broken walls standing in the water, with a cache left behind"
                        + LI + bold("Standing stones: ") + "a ring of fenstone monoliths, some of them fallen"
                        + P + "Try " + thing("/locate structure wispwillow:sunken_ruin") + "."));

        Entry flora = new Entry("willow_wilds", "fen_flora", "Fen Flora",
                id(BlockRegistry.MOONLILY.get()))
                .sortnum(10)
                .page(spotlight(BlockRegistry.WHISPERREED.get(), "Whisperreed",
                        "A tall reed that grows with its feet in the water and its head in the air. It takes "
                        + "root in soil, sand or gravel, dry or under one block of water, and never deeper."))
                .page(spotlight(BlockRegistry.MOONLILY.get(), "Moonlily",
                        "A lily pad carrying a pale flower and a little light. After dark it lets off the odd "
                        + "drifting mote. Place it on still water like any lily pad."))
                .page(spotlight(BlockRegistry.WISPCAP.get(), "Wispcap",
                        "Small glowing mushrooms, bright enough to see by. Unlike ordinary mushrooms they never "
                        + "spread and never wilt in daylight, so they stay where you put them: on soil or on a log."))
                .page(spotlight(BlockRegistry.FEN_MOSS.get(), "Fen Moss",
                        "Hangs in long drapes from leaves and from the underside of solid blocks, and can be "
                        + "climbed like a vine. Only shears will take it with you."))
                .page(text("Flowers",
                        item("Bogbell") + " hangs blue bells low over wet ground; " + item("Duskpetal")
                        + " opens violet at the edge of the trees."
                        + P + "Both make dye and both go in a flower pot. In suspicious stew, bogbell gives water "
                        + "breathing and duskpetal night vision."));

        Entry stone = new Entry("willow_wilds", "fen_stone", "Peat & Fenstone",
                id(BlockRegistry.FENSTONE_BRICKS.get()))
                .sortnum(11)
                .page(spotlight(BlockRegistry.PEAT.get(), "Peat",
                        "Dug from the beds of bog pools and from under swamp water. Anything that grows in dirt "
                        + "grows in peat, and it burns: one block smelts four items."))
                .page(crafting("wispwillow:fenstone", "Fenstone",
                        "Eight cobblestone around a block of peat. The green-grey stone the old fen builders used."))
                .page(crafting("wispwillow:fenstone_bricks", "Fenstone Bricks",
                        "Four fenstone make four bricks. Add vines, moss or fen moss for the mossy kind. Stairs, "
                        + "slabs and walls take the usual shapes, or come from a stonecutter."))
                .page(crafting("wispwillow:fenstone_pillar", "Pillars & Mossy Fenstone",
                        "Two fenstone stacked give two fluted pillars; a stonecutter does the same. "
                        + item("Mossy Fenstone") + " is the raw stone with moss in its hollows, for older work."))
                .page(crafting("wispwillow:peat_bricks", "Peat Bricks",
                        "Four peat cut and dried into bricks. Tidier than the raw sod, and they burn twice "
                        + "as long: one block smelts eight items."));

        Entry sap = new Entry("willow_wilds", "willow_sap", "Willow Sap",
                id(ItemRegistry.WILLOW_SAP_BOTTLE.get()))
                .sortnum(12)
                .page(spotlight(ItemRegistry.WILLOW_SAP_BOTTLE.get(), "Tapping",
                        "Use a " + item("Glass Bottle") + " on a " + item("Willow Log") + ". The bottle fills "
                        + "with sap and the log is left stripped."
                        + P + "Drunk, it takes the edge off hunger, gives a few seconds of Regeneration, and "
                        + "settles poison the way honey does."))
                .page(crafting("wispwillow:willow_sap_bucket", "A Bucket of Sap",
                        "Three bottles pour into a bucket. Poured out, sap creeps slowly, never makes new "
                        + "sources, and holds on to anything that wades in."))
                .page(crafting("wispwillow:willow_amber", "Willow Amber",
                        "Four bottles set into a block of amber: glassy, warm, and faintly lit from within."));

        Entry bog = new Entry("willow_wilds", "bog_water", "Bog Water",
                id(ItemRegistry.BOG_WATER_BUCKET.get()))
                .sortnum(13)
                .page(spotlight(ItemRegistry.BOG_WATER_BUCKET.get(), "Not Quite Water",
                        "The fen's small pools are dark and close. You wade through them more than swim, and "
                        + "under the surface you can see only a few blocks."
                        + P + "A bucket of it still does what water does for farmland and for fire."))
                .page(text("Making Mud",
                        "A bucket of bog water and eight dirt make eight " + item("Mud") + ", and you keep the bucket."));

        Entry wildlife = new Entry("willow_wilds", "fen_wildlife", "Fen Wildlife",
                id(ItemRegistry.FEN_HERON_SPAWN_EGG.get()))
                .sortnum(14)
                .page(entity("wispwillow:fen_heron", 0.6F, "Fen Heron",
                        "Grey wading birds that stalk the shallows. They startle easily: sneak, or bring fish, "
                        + "and they will let you close."))
                .page(text("Fishing",
                        "A heron left alone in shallow water stands stock still with its head lowered, then "
                        + "strikes at something below the surface."
                        + P + "Feed two of them fish and they will raise a chick."))
                .page(entity("wispwillow:wispfly", 2.0F, "Wispfly",
                        "Drifting points of light that come out over the fen after dark and hang low above "
                        + "the reeds. Harmless, and gone by morning."));

        Entry hound = new Entry("willow_wilds", "willow_hound", "The Willow Hound",
                id(ItemRegistry.WILLOW_HOUND_SPAWN_EGG.get()))
                .sortnum(15)
                .page(entity("wispwillow:willow_hound", 0.9F, "Woven, Not Born",
                        "A hound of bound withes with a knot of sap-light where its heart would be. They "
                        + "range the fen in ones and twos, and they are not familiars: a hound belongs to a "
                        + "person, not to an altar."))
                .page(text("Taming",
                        "Feed it a " + item("Bottle of Willow Sap") + " — the same sap you tap a willow "
                        + "for, and what the light in its chest is made of. It takes a few bottles, and the "
                        + "glass comes back each time."
                        + P + "Once it is yours, an empty hand tells it to sit or to follow."))
                .page(text("Fetching",
                        "A tamed hound that has not been told to stay will go and collect loose items from "
                        + "around you and set them down at your feet. It carries one thing at a time and it "
                        + "never eats what it is carrying, however full your pack is."
                        + P + "It will also fight whatever attacks you, and unlike a familiar it can die "
                        + "doing it. Feed it sap to mend it."));

        Entry openWay = new Entry("willow_wilds", "spirit_realm", "The Spirit Realm",
                id(BlockRegistry.WEEPING_LEAVES.get()))
                .sortnum(16)
                .page(text("Islands Over Nothing",
                        "Broken land adrift in a dark with no sun in it — and on top of every island, "
                        + "grass. Pale, ashen, and unmistakably grass, which is the part of the place that "
                        + "will not sit still in your head."
                        + P + "There is no floor under any of it. Walk off an edge and you keep going."))
                .page(text("The Open Way",
                        "Lay the " + bold("Vigil of the Open Way") + " on a Willow Altar. The rite hangs a "
                        + "gate in the air above the altar itself — walk into it and it takes you through."
                        + P + "The gate mends itself every second the vigil burns, so breaking it cannot "
                        + "strand anyone already on the far side."
                        + P + "The vigil lasts half an hour. When it ends — spent, broken, or starved of "
                        + "flux — the gate shuts and everyone it took is put back exactly where they were "
                        + "standing when they left. That exit is unconditional: a realm you could only "
                        + "leave by keeping an altar fed would be a trap."))
                .page(text("Coming Back",
                        "A second gate stands a couple of paces from wherever you land. Walk into that one "
                        + "and it puts you home again — you need not wait out the half hour."
                        + P + "It is left standing where it was raised, so the way out is still there the "
                        + "next time you come through."))
                .page(text("Nothing To Dig",
                        "No caves, no ore, no springs. The islands are end stone under their skin of soil "
                        + "and that is the whole of them."
                        + P + "What the realm is worth the trip for is what grows and lives on it."))
                .page(spotlight(BlockRegistry.SOULWOOD_LOG.get(), "Soulwood",
                        "Tall pale trees stand in ones and twos on the larger islands, and their leaves "
                        + "give off a light of their own — which is how you find a stand of them across "
                        + "the dark."
                        + P + "There is no sapling. The leaves drop sticks and nothing else, so a grove "
                        + "you cut is a grove that is gone."))
                .page(text("What Comes With You",
                        "Tamed animals standing near you when you step through go with you, and come back "
                        + "with you at the end. Ones you have told to sit stay where they are — " + bold("stay")
                        + " means stay, including when the world changes around them."
                        + P + "Familiars never travel. They belong to their altar, and the altar is not "
                        + "going anywhere."));

        Entry fawn = new Entry("willow_wilds", "glimmerfawn", "Glimmerfawn",
                id(ItemRegistry.SPIRIT_DOWN.get()))
                .sortnum(17)
                .page(entity("wispwillow:glimmerfawn", 0.6F, "The Only Herd",
                        "Pale deer on thin legs, the one creature in the Spirit Realm with any weight to "
                        + "it. They can be tamed, they can be bred, and they grow a coat worth taking."))
                .page(text("Moonlily",
                        "All three run on " + item("Moonlily") + ", which grows only in the Willow Fen. "
                        + "You cannot start a herd from inside the realm — carry flowers in with you, and "
                        + "remember the way out closes after half an hour."
                        + P + "A flower tames, breeds, raises a fawn, and brings a shorn coat straight "
                        + "back instead of waiting the five minutes it takes to regrow."))
                .page(text("Shearing",
                        "Shears take the coat off as " + item("Spirit Down") + " and leave the fawn alive "
                        + "to grow another. A shorn one is visibly narrower than the rest of the herd."
                        + P + "Killing one gives less down and no second harvest. Shear, do not kill."))
                .page(crafting("wispwillow:white_wool_from_spirit_down", "Baling It",
                        "Four down bale into wool. It is the only thing the Spirit Realm exports, and the "
                        + "only renewable thing in it."));

        Entry weave = new Entry("willow_wilds", "glimmerweave", "Glimmerweave",
                id(ItemRegistry.GLIMMERWEAVE_ROBE.get()))
                .sortnum(18)
                .page(crafting("wispwillow:glimmer_thread", "Spun, Not Baled",
                        "Twist three down together instead of baling them and you get a thread that "
                        + "still gives off a little of the fawn's light. Four threads weave a bolt of "
                        + item("Glimmerweave") + ", and the bolt takes every pattern wool does."))
                .page(crafting("wispwillow:glimmerweave_robe", "The Set",
                        "Hood, robe, leggings and slippers. Iron's protection in cloth, nothing against "
                        + "a blow's weight, and more willing to take an enchantment than anything else "
                        + "in this book. Mend it with the bolt."))
                .page(text("What the Cloth Remembers",
                        bold(GlimmerweaveSet.DOWNSOFT + " pieces — Downsoft: ") + "you land like down. "
                        + "No fall hurts you, from any height."
                        + P + bold(GlimmerweaveSet.GLIMMER_SIGHT + " pieces — Glimmer-sight: ")
                        + "you see in the dark the way the herd does in its realm."));

        return List.of(fen, flora, stone, sap, bog, wildlife, hound, openWay, fawn, weave);
    }

    // ---------------- Foundations & Infusion ----------------

    /** What the liquid does to anything that goes under it. */
    private static Entry fluxiaLiquid() {
        return new Entry("fluxia_network", "liquid_fluxia", "Liquid Fluxia",
                id(ItemRegistry.FLUXIA_FLUID_BUCKET.get()))
                .sortnum(9)
                .page(spotlight(ItemRegistry.FLUXIA_FLUID_BUCKET.get(), "Going Under",
                        "Fluxia in a pool is current you can swim in, and it does not care for the "
                        + "distinction between you and the stone. Put your head under and you come "
                        + thing("Unmoored") + ": for a little while, the ground is not in your way."))
                .page(text("Drifting",
                        "You sink on your own, through whatever is beneath you."
                        + P + LI + bold("Sneak: ") + "drop faster"
                        + LI + bold("Hold jump: ") + "rise back the way you came"
                        + LI + bold("Sideways: ") + "barely at all, once you are inside rock"
                        + P + "It lasts six seconds after your eyes leave the Fluxia, and it is topped "
                        + "back up the whole time you are under. Come out into open air and it ends at "
                        + "once, with a soft landing if you dropped out of a ceiling."))
                .page(text("The Price",
                        "Be inside the rock when it runs out and the rock wins: you will suffocate "
                        + "exactly as you would standing in a wall. Six seconds is about fifteen blocks "
                        + "of drift, so count before you dive."
                        + P + "You cannot drift out of the bottom of the world, and bedrock will not "
                        + "have you. Nor will " + link("willow_wilds/fluxia_oasis", "Warded Slate") + "."));
    }

    /** The cavern, and the thing underneath it. */
    private static Entry oasisEntry() {
        return new Entry("willow_wilds", "fluxia_oasis", "The Fluxia Oasis",
                id(BlockRegistry.WARDED_SLATE.get()))
                .sortnum(8)
                .page(text("A Light Under the Hill",
                        "Somewhere underground — mossy, lit by glow berries, walls of calcite and "
                        + "basalt threaded with amethyst and fluxite — there is a cavern grown around a "
                        + "pool of " + link("fluxia_network/liquid_fluxia", "Liquid Fluxia") + ", with a "
                        + "willow leaning over the water."
                        + P + "They never break the surface, so caving is how you find one. Or ask the "
                        + "world directly: " + thing("/locate structure wispwillow:fluxia_oasis") + "."))
                .page(spotlight(BlockRegistry.WARDED_SLATE.get(), "Warded Slate",
                        "The one stone Fluxia will not carry you through. The oasis pool is lined with "
                        + "it, which is why going under there puts you on the bottom of the basin "
                        + "instead of through it."
                        + P + "It drops itself. A floor of it is a floor no one phases into."))
                .page(text("What Lies Beneath",
                        "Something is sealed under that pool, and no tunnel reaches it."
                        + P + "Set into the warded floor are " + bold("four weeping stones") + ". Three "
                        + "of them are backed by more ward a block down, and you will feel yourself stop. "
                        + "One is not."
                        + P + italic("Of four that weep, the thin one lies in the willow's shade.")));
    }

    private static List<Entry> foundationEntries() {
        Entry tiers = new Entry("foundations", "altar_tiers", "Raising the Altar",
                id(BlockRegistry.RUNES.get(RuneType.BLANK).get()))
                .sortnum(0).priority(true).readByDefault(true)
                .page(text("Depth, Not Width",
                        "A bare altar is " + bold("tier I") + ". It will perform the small rites and "
                        + "nothing else."
                        + P + "To go further you build a " + thing("foundation") + ": complete rings of "
                        + "runes laid one block " + bold("below") + " the altar, in the floor you are "
                        + "already standing on."
                        + P + "The chalk circle sits above; the runes sit beneath. They never collide."))
                .page(text("The Rings",
                        "Each " + bold("complete") + " ring raises the altar one tier. A ring with a "
                        + "single gap in it counts for nothing."
                        + P + LI + bold("Tier II") + " - ring at 1 block out (" + AltarTier.runesForRing(1) + " runes)"
                        + LI + bold("Tier III") + " - ring at 2 (" + AltarTier.runesForRing(2) + " more)"
                        + LI + bold("Tier IV") + " - ring at 3 (" + AltarTier.runesForRing(3) + " more)"
                        + LI + bold("Tier V") + " - ring at 4 (" + AltarTier.runesForRing(4) + " more)"
                        + P + "A full tier V foundation is " + bold(String.valueOf(AltarTier.runesForTier(5)))
                        + " runes. It is meant to be the work of a while."))
                .page(text("Which Rune to Lay",
                        "Every rune counts toward the tier. Three of them also do something."
                        + P + LI + color(RuneType.BLANK.color(), "Blank") + " - structure only, and cheap."
                        + LI + color(RuneType.WATER.color(), "Water") + " - +2,000 to the reserve, each."
                        + LI + color(RuneType.EARTH.color(), "Earth") + " - every rite costs 0.5% less, each."
                        + LI + color(RuneType.BLOOD.color(), "Blood") + " - 0.4% chance per rune of a doubled result."
                        + P + "Efficiency and yield each cap at 50%, so there is a point past which more "
                        + "of the same rune stops paying."))
                .pages(tierPreviews())
                .page(text("Reading Your Altar",
                        "Put on the " + link("fluxia_network/arcane_goggles", "Arcane Goggles") + " and "
                        + "look at the altar. It reports its tier, its rune count, the boons the "
                        + "foundation grants, and - if the staged rite is out of reach - exactly which "
                        + "tier you still need."
                        + P + "A rite you cannot yet perform says so plainly rather than failing silently."));

        Entry infusion = new Entry("foundations", "fluxia_infusion", "Fluxia Infusion",
                id(BlockRegistry.MANA_POOL.get()))
                .sortnum(1).priority(true)
                .page(text("Crafting in a Pool",
                        "Throw items into a charged " + link("fluxia_network/mana_pool", "Willow Fluxia Pool")
                        + ". If they match an infusion and the pool holds enough charge, they transmute."
                        + P + "The pool wants the exact ingredients and " + bold("nothing else") + " - "
                        + "stray items floating in it stop anything from happening rather than being eaten."
                        + P + "Short of charge, the pool puffs smoke and waits."))
                .pages(infusionPages());

        Entry flora = new Entry("foundations", "functional_flora", "Functional Flora",
                id(BlockRegistry.VERDANT_BLOOM.get()))
                .sortnum(2)
                .page(text("Flowers That Spend",
                        "Luxinia and Solrosa " + italic("make") + " Fluxia. These three " + italic("spend") + " it."
                        + P + "Link a bloom to a pool with the "
                        + link("fluxia_network/flux_linking_wand", "wand") + " like any other machine. "
                        + "A bloom only draws charge on a tick where it actually did something, so one "
                        + "sitting over barren ground costs nothing."))
                .page(spotlight(BlockRegistry.VERDANT_BLOOM.get(), "Verdant Bloom",
                        "Bonemeals one growable within 5 blocks every 2 seconds, for 60 " + thing("f")
                        + " a time."
                        + P + "Pairs obviously with " + link("willow_wilds/soils", "Snad") + " and amethyst "
                        + "farmland."))
                .page(spotlight(BlockRegistry.GATHERING_BLOOM.get(), "Gathering Bloom",
                        "Sweeps loose items within 6 blocks into the inventory " + bold("directly below it")
                        + ", twice a second, for 10 " + thing("f") + " a sweep."
                        + P + "With no inventory beneath, it pulls the drops to itself instead - still "
                        + "useful under a mob farm."))
                .page(spotlight(BlockRegistry.WARDING_BLOOM.get(), "Warding Bloom",
                        "Banishes freshly-spawned hostiles within 12 blocks, once a second, for 40 "
                        + thing("f") + "."
                        + P + "It only takes monsters at full health that spawned naturally, so nothing "
                        + "you led home and nothing mid-fight is ever deleted out from under you."));

        Entry tablets = new Entry("foundations", "tablet_tiers", "Greater Vessels",
                id(ItemRegistry.GREATER_TABLET.get()))
                .sortnum(3)
                .page(text("Bigger Tablets",
                        "The base " + link("fluxia_network/fluxia_tablet", "Fluxia Tablet") + " holds 20,000 "
                        + thing("f") + ". Infusion folds it into something larger."
                        + P + LI + item("Greater Fluxia Tablet") + " - 100,000"
                        + LI + item("Grand Fluxia Tablet") + " - 500,000"
                        + P + "Throughput scales with them, so a Grand tablet can prime an entire bank "
                        + "of pools in one trip."))
                .page(text("Carrying Charge",
                        "An upgraded tablet keeps whatever it already held. Beyond the Grand tablet you "
                        + "are expected to stop carrying Fluxia around and start building pools."));

        return List.of(tiers, infusion, flora, tablets, familiars(), soulGateway());
    }

    /**
     * A two-layer multiblock per tier: the rune ring on the lower layer, the altar on the
     * upper one. Press the eye icon to project it into the world and simply walk the shape
     * — which is the fastest way to settle where the runes actually go.
     */
    private static List<JsonObject> tierPreviews() {
        List<JsonObject> pages = new ArrayList<>();

        for (int tier = 2; tier <= AltarTier.MAX_TIER; tier++) {
            int rings = tier - 1;
            int radius = rings;                 // ring N sits at radius N
            int size = radius * 2 + 1;

            // Rune layer: every complete ring up to this tier. This is the BOTTOM layer.
            List<String> runeLayer = new ArrayList<>(size);
            for (int z = -radius; z <= radius; z++) {
                StringBuilder row = new StringBuilder(size);
                for (int x = -radius; x <= radius; x++) {
                    int ring = Math.max(Math.abs(x), Math.abs(z));
                    row.append(ring >= 1 && ring <= rings ? 'R' : ' ');
                }
                runeLayer.add(row.toString());
            }

            // Altar layer: just the altar, directly above the middle. This is the TOP layer.
            List<String> altarLayer = new ArrayList<>(size);
            for (int z = -radius; z <= radius; z++) {
                StringBuilder row = new StringBuilder(size);
                for (int x = -radius; x <= radius; x++) {
                    row.append(x == 0 && z == 0 ? '0' : ' ');
                }
                altarLayer.add(row.toString());
            }

            String body = "Tier " + roman(tier) + ": " + AltarTier.runesForTier(tier)
                    + " runes in " + rings + (rings == 1 ? " ring" : " rings")
                    + ", laid one block below the altar. Any rune works; the mix decides the boons.";

            // Patchouli stores pattern layer i at y = (height - i - 1), so the FIRST
            // layer in this list is the topmost one. Altar first, runes beneath.
            pages.add(BookJson.multiblockLayers("Tier " + roman(tier) + " Foundation",
                    List.of(altarLayer, runeLayer),
                    List.of("R", "0"),
                    List.of("wispwillow:blank_rune", "wispwillow:altar"),
                    body));
        }
        return pages;
    }

    private static Entry familiars() {
        return new Entry("foundations", "familiars", "Familiars",
                "wispwillow:axolotl_familar_spawn_egg")
                .sortnum(4)
                .page(text("Bound Company",
                        "A familiar is a small, deathless thing that attaches itself to an altar and "
                        + "drifts in a slow orbit around it."
                        + P + "It cannot be hurt, drowned, burned or despawned. Break its altar and it "
                        + "looks for another within " + FamiliarEntity.SEARCH_RADIUS + " blocks; finding "
                        + "none, it settles where it stands and waits to be found."))
                .page(text("What You Need",
                        "Five things, in this order. None of them are optional."
                        + P + LI + item("Amethyst Paper") + " - pressed from Amethyst Sugar Cane."
                        + LI + link("rituals_major/soul_pact", "The Written Pact") + " at an altar, "
                        + "which turns that paper into blank " + item("Soul Pacts") + "."
                        + LI + "A charged " + link("fluxia_network/mana_pool", "Fluxia Pool") + " - twice."
                        + LI + "A creature that answers. See the next page."
                        + LI + "A built and lit " + link("foundations/soul_gateway", "Soul Gateway") + "."
                        + P + "The full sequence, with costs, is in "
                        + link("foundations/soul_gateway", "The Soul Gateway") + "."))
                .pages(pactSpeciesPages())
                .page(text("What They Give",
                        "A familiar in service grants its boon to the altar, stacking with the rune "
                        + "foundation beneath it."
                        + P + LI + item("Axolotl Familiar") + " - " + FamiliarBoon.FRUGAL.summary().getString()
                        + LI + item("Wisp Familiar") + " - " + FamiliarBoon.CAPACIOUS.summary().getString()
                        + LI + item("Frog Familiar") + " - " + FamiliarBoon.BOUNTIFUL.summary().getString()
                        + LI + item("Bat Familiar") + " - " + FamiliarBoon.BALANCED.summary().getString()
                        + P + "Combined efficiency and bonus chance are each capped at 75%, however many "
                        + "runes and familiars you gather."))
                .page(text("What Cheaper Means",
                        "Efficiency comes off " + bold("both") + " halves of a rite's price: the "
                        + item("Fluxia") + " drawn from the altar's reserve, and the reagents laid on "
                        + "the ground for it."
                        + P + "A rite wanting four ingots wants two at half efficiency. The altar shows "
                        + "you the reduced number in the orbit above it, with the old one beside it, so "
                        + "you always lay down what it will actually take."
                        + P + bold("Never below one.") + " A reagent a rite asks for is a reagent it "
                        + "goes on asking for, whatever is orbiting your altar - the discount makes a "
                        + "rite cheaper, never free."))
                .page(text("How Many An Altar Keeps",
                        bold("One familiar per altar tier.") + " A bare altar attends to one; a fully "
                        + "founded one attends to " + AltarTier.MAX_TIER + "."
                        + P + "Bring more than that and the extras still orbit, still belong to the "
                        + "altar, and still cannot be killed - they simply contribute nothing until you "
                        + "lay another ring of " + link("foundations/altar_tiers", "runes") + " beneath "
                        + "them. When an altar is over its limit the closest familiars are the ones kept."
                        + P + "The " + link("fluxia_network/arcane_goggles", "goggles") + " show this as "
                        + thing("2 / 3") + " - in service over capacity - and say so in red when an "
                        + "altar is ignoring one."))
                .page(spotlight(ItemRegistry.SOUL_WISP.get(), "Soul Wisp",
                        "The only thing that will send a familiar away."
                        + P + "Touch a familiar with one and it rises, spins, and dissolves into soul-light "
                        + "over about two seconds. The wisp is consumed with it."
                        + P + "There is no other way to remove one, which is deliberate - a familiar you "
                        + "can lose to a stray arrow is not much of a companion."));
    }

    /**
     * The creatures a pact will take, generated from the bond table itself so the book cannot
     * claim a species the pact would refuse.
     */
    private static List<JsonObject> pactSpeciesPages() {
        List<String> lines = new ArrayList<>();
        for (ResourceLocation source : SoulPacts.sources()) {
            EntityType<?> familiar = SoulPacts.familiarFor(source);
            if (familiar == null) continue;
            String becomes = prettify(BuiltInRegistries.ENTITY_TYPE.getKey(familiar).getPath());
            lines.add(item(prettify(source.getPath())) + " becomes a " + item(becomes));
        }
        return bulletPages("Who Answers",
                "A primed pact only takes a creature the gateway has a casting for. Press one "
                + "against anything else and nothing answers - the creature is unharmed and the pact "
                + "is unspent. These are the castings this book was written with; a pack may add "
                + "its own, and they need not produce familiars at all.",
                lines, 6);
    }

    /** The two generators that make a standing Fluxia load affordable. */
    private static Entry generators() {
        return new Entry("fluxia_network", "generators", "Dynamo & Font",
                id(BlockRegistry.FLUXIA_DYNAMO.get()))
                .sortnum(3)
                .page(text("Beyond Flowers",
                        "A Luxinias is a candle. It will fill a pool overnight, and that is enough for "
                        + "runes and tablets."
                        + P + "It is nowhere near enough for anything that " + bold("stays") + " on. A "
                        + link("foundations/soul_gateway", "Soul Gateway") + " burns "
                        + thing(GoggleNumber(GatewayCoreBlockEntity.UPKEEP_PER_TICK * 20)) + " every "
                        + "second it is lit. These two blocks exist for that kind of load."))
                .page(crafting("wispwillow:fluxia_dynamo", "Fluxia Dynamo",
                        "Drop fuel on top of it - anything a furnace would burn, including "
                        + item("Amethyst Coal") + " - and it turns the burn into current at "
                        + thing(GoggleNumber(FluxiaDynamoEntity.FLUXIA_PER_SECOND)) + " a second."
                        + P + bold(count(dynamosPerGateway()) + " of these") + " will hold a gateway "
                        + "open indefinitely, at the cost of having to keep it fed."))
                .page(crafting("wispwillow:astral_font", "Astral Font",
                        "Asks for no fuel at all, only an unobstructed view of the sky."
                        + P + LI + bold("By day: ") + thing(GoggleNumber(AstralFontEntity.DAY_RATE)) + "/s"
                        + LI + bold("By night: ") + thing(GoggleNumber(AstralFontEntity.NIGHT_RATE)) + "/s"
                        + LI + bold("Rain: ") + "halved. " + bold("Thunder: ") + "halved again."
                        + P + "Put anything solid above one and it stops entirely."
                        + P + bold(count(fontsPerGateway()) + " of them") + " under a clear night sky "
                        + "will hold a gateway open. Expect to want twice that, for the afternoons."))
                .page(text("Which To Build",
                        "Neither, alone, is the answer."
                        + P + "Fonts cost nothing to run but halve in bad weather and stop under a roof. "
                        + "Dynamos do not care about the sky, and eat everything you feed them."
                        + P + "The usual arrangement is a bank of fonts as a floor, a "
                        + link("fluxia_network/mana_pool", "pool") + " to buffer them, and a dynamo or "
                        + "two wired in for when the sky lets you down."));
    }

    /** The pact chain, and the gateway that spends it. */
    private static Entry soulGateway() {
        return new Entry("foundations", "soul_gateway", "The Soul Gateway",
                id(BlockRegistry.GATEWAY_CORE.get()))
                .sortnum(5)
                .page(text("Taking a Soul",
                        "Familiars are not crafted. They are negotiated for, in four steps, and the "
                        + "negotiation is expensive at every one."
                        + P + LI + "Craft a " + item("Soul Pact") + " - blank vellum."
                        + LI + "Drop it in a charged " + link("fluxia_network/mana_pool", "pool") + " to "
                        + "prime it."
                        + LI + "Right-click a creature. The pact closes around it."
                        + LI + "Drop the bound pact in a pool again to seal it."))
                .page(spotlight(ItemRegistry.SOUL_PACT.get(), "Soul Pact",
                        "Not crafted. Written, at an altar, by "
                        + link("rituals_major/soul_pact", "The Written Pact") + " — four at a time, "
                        + "on " + item("Amethyst Paper") + " pressed from "
                        + link("willow_wilds/soils", "Amethyst Sugar Cane") + "."
                        + P + "Worthless until a pool has had it. A primed pact will only take a "
                        + "creature the tradition knows how to keep: press one against a cow and "
                        + "nothing answers, and the cow is unharmed and the pact unspent."))
                .page(text("Building the Gateway",
                        "A wall of " + item("Willow Logs") + ", five by five, with a three by three hole "
                        + "in the middle. The " + item("Gateway Core") + " replaces the middle block of "
                        + "the bottom row."
                        + P + "Build the wall whichever way suits the ground; the core finds it when you "
                        + "open the gateway and turns to face you, and that is the side familiars step "
                        + "out of. Grass, snow, vines and shallow water in the opening are no obstacle - "
                        + "the light pushes them aside."
                        + P + "Fifteen logs and the core. Knock a single log out while it is lit and the "
                        + "gateway collapses."))
                .page(crafting("wispwillow:gateway_core", "Gateway Core",
                        "The most expensive thing in this book, and deliberately so - everything it "
                        + "produces is permanent."
                        + P + "It is a Fluxia machine like any other, which means it will not draw "
                        + "power by touch. Point a " + link("fluxia_network/flux_linking_wand", "linking wand")
                        + " at a charged pool, then at the core, and the pool will feed it."))
                .page(text("Lighting It",
                        "Right-click the core to open the gateway, and again to shut it."
                        + P + LI + bold("To open: ") + thing(GoggleNumber(GatewayCoreBlockEntity.OPEN_COST))
                        + LI + bold("To stay open: ")
                        + thing(GoggleNumber(GatewayCoreBlockEntity.UPKEEP_PER_TICK * 20)) + " per second"
                        + LI + bold("Per pact cast: ")
                        + thing(GoggleNumber(GatewayCoreBlockEntity.CAST_COST))
                        + P + "Run the core dry and it shuts itself, loudly. Wear the Arcane Goggles and "
                        + "it will tell you how many seconds it has left."))
                .page(text("Casting a Pact",
                        "Throw the " + item("Sealed Soul Pact") + " into the lit opening."
                        + P + "It falls through the light, the gateway takes its price, and the familiar "
                        + "steps out in front of the core. It will bind itself to the nearest "
                        + link("altar/willow_altar", "altar") + " within "
                        + FamiliarEntity.SEARCH_RADIUS + " blocks on its own."
                        + P + "If the core cannot afford the crossing the pact simply lies in the light, "
                        + "smoking gently, until you feed it."));
    }


    /** Small counts read better as words in prose. */
    private static String count(int n) {
        return switch (n) {
            case 1 -> "One";
            case 2 -> "Two";
            case 3 -> "Three";
            case 4 -> "Four";
            default -> Integer.toString(n);
        };
    }

    /** Dynamos needed to cover a gateway's standing upkeep, rounded up. */
    private static int dynamosPerGateway() {
        int upkeep = GatewayCoreBlockEntity.UPKEEP_PER_TICK * 20;
        return Math.max(1, (upkeep + FluxiaDynamoEntity.FLUXIA_PER_SECOND - 1)
                / FluxiaDynamoEntity.FLUXIA_PER_SECOND);
    }

    /** Astral fonts needed to cover a gateway at night, rounded up. */
    private static int fontsPerGateway() {
        int upkeep = GatewayCoreBlockEntity.UPKEEP_PER_TICK * 20;
        return Math.max(1, (upkeep + AstralFontEntity.NIGHT_RATE - 1) / AstralFontEntity.NIGHT_RATE);
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "V";
        };
    }

    /** One page per infusion, generated from the same table the recipes come from. */
    private static List<JsonObject> infusionPages() {
        List<JsonObject> pages = new ArrayList<>();
        for (InfusionDefinitions.Def def : InfusionDefinitions.all()) {
            StringBuilder sb = new StringBuilder(def.lore()).append(P);
            for (var in : def.inputs()) {
                sb.append(LI).append(bold(in.getCount() + "x ")).append(item(displayName(in)));
            }
            sb.append(LI).append(bold("Cost: ")).append(thing(GoggleNumber(def.fluxia())));
            pages.add(spotlightStack(def.result(), displayName(def.result()), sb.toString()));
        }
        return pages;
    }

    private static String GoggleNumber(int v) {
        return String.format(java.util.Locale.ROOT, "%,d", v) + " \u0192";
    }

    // ---------------- The Empyrean Arts ----------------

    /**
     * What awakening grows into: the traits, resonance, and the Regalia. The imbuing rites
     * themselves are generated into the same category from {@link RitualDefinitions}.
     */
    private static List<Entry> empyreanEntries() {
        Entry awakened = new Entry("rituals_imbuing", "awakened_empyrite", "Awakened Empyrite",
                id(ItemRegistry.EMPYRITE_SWORD.get()))
                .sortnum(0).priority(true).readByDefault(true)
                .page(text("Waking the Metal",
                        "The " + link("rituals_grand/awakening_empyrite", "Awakening") + " lights more "
                        + "than the surface of the metal. An awakened piece is stronger than it was the "
                        + "moment it leaves the circle, and it goes on getting stronger with use."
                        + P + "Ingots and nuggets wake too, but only something you wield or wear has "
                        + "anything to grow into."))
                .page(text("What Waking Gives",
                        LI + bold("Swords and axes: ") + "+" + num(AwakenedTraits.meleeDamage(1))
                        + " attack damage, rising to +" + num(AwakenedTraits.meleeDamage(Resonance.MAX_LEVEL))
                        + LI + bold("Tools: ") + "+" + num(AwakenedTraits.miningEfficiency(1))
                        + " mining speed, rising to +" + num(AwakenedTraits.miningEfficiency(Resonance.MAX_LEVEL))
                        + ", and up to " + num(AwakenedTraits.reach(Resonance.MAX_LEVEL)) + " blocks more reach"
                        + LI + bold("Armour: ") + "a heart apiece, and " + num(AwakenedTraits.toughness(1))
                        + " toughness rising to " + num(AwakenedTraits.toughness(Resonance.MAX_LEVEL))
                        + P + "An axe is a weapon and a tool at once, and wakes as both."))
                .page(text("Resonance",
                        "Awakened metal remembers its work. Blades resonate with every foe they fell, "
                        + "tools with every block they break — ores ring louder — and armour with every "
                        + "blow it turns."
                        + P + resonanceLevels()
                        + P + "Everything a piece does grows with it: its waking and its imbuement alike."))
                .page(text("One More Trick",
                        "Once awake, a piece can be taught one thing more by an imbuing rite. Each suits "
                        + "one kind of piece: blades, tools, or one of the four pieces of armour."
                        + P + "Every imbuing circle is the awakening's orange serpents on the far corners, "
                        + "with stars, hexes and triads in three new colours. Learn one and you have the "
                        + "shape of them all."
                        + P + "A piece holds one imbuement. Imbue it again and the old one burns away; its "
                        + "resonance stays."));

        Entry regalia = new Entry("rituals_imbuing", "empyrean_regalia", "Empyrean Regalia",
                id(ItemRegistry.EMPYRITE_CHESTPLATE.get()))
                .sortnum(1).priority(true)
                .page(text("The Regalia",
                        "Awakened empyrite armour knows its own. Worn together, the pieces lend each "
                        + "other something none of them has alone."
                        + P + "Only awakened pieces count toward it. The set is the reward for the rite, "
                        + "not for the smithing."))
                .page(text("Two Pieces, Four Pieces",
                        bold(EmpyreanSet.EMBERPROOF + " pieces — Emberproof: ") + "fire cannot touch you, "
                        + "and lava bites half as hard."
                        + P + bold(EmpyreanSet.AEGIS + " pieces — Radiant Aegis: ")
                        + Math.round(EmpyreanSet.AEGIS_REDUCTION * 100) + "% less damage from nearly every "
                        + "source, anything that strikes you in melee bursts into flame, and every "
                        + "awakened piece you carry resonates twice as fast."));

        return List.of(awakened, regalia);
    }

    private static String resonanceLevels() {
        StringBuilder sb = new StringBuilder();
        for (int level = 1; level <= Resonance.MAX_LEVEL; level++) {
            sb.append(LI).append(bold(Resonance.roman(level) + ": "))
                    .append(level == 1 ? "from the moment it wakes"
                            : String.format(java.util.Locale.ROOT, "%,d", Resonance.threshold(level)) + " resonance");
        }
        return sb.toString();
    }

    private static String num(float value) {
        return value == Math.round(value) ? String.valueOf(Math.round(value))
                : String.format(java.util.Locale.ROOT, "%.2f", value).replaceAll("0+$", "");
    }

    // ---------------- Rituals (generated) ----------------

    private static List<Entry> ritualEntries() {
        List<Entry> out = new ArrayList<>();

        for (RitualDefinitions.Chapter chapter : RitualDefinitions.Chapter.values()) {
            List<RitualDefinitions.Def> defs = new ArrayList<>(RitualDefinitions.inChapter(chapter));
            // Cheapest first, so a chapter reads as a progression.
            defs.sort((a, b) -> Integer.compare(a.fluxia(), b.fluxia()));

            int sort = 0;
            for (RitualDefinitions.Def def : defs) {
                out.add(RitualEntryFactory.create(def, sort++));
            }
        }
        return out;
    }
}
