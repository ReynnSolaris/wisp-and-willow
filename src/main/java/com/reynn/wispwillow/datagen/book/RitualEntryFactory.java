package com.reynn.wispwillow.datagen.book;

import com.google.gson.JsonObject;
import com.reynn.wispwillow.awakening.Imbuement;
import com.reynn.wispwillow.awakening.Resonance;
import com.reynn.wispwillow.blocks.SigilType;
import com.reynn.wispwillow.ritual.RitualDefinitions;
import com.reynn.wispwillow.ritual.RitualRecipe;
import com.reynn.wispwillow.ritual.Vigil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.util.*;

import static com.reynn.wispwillow.datagen.book.BookJson.*;

/**
 * Turns a {@link RitualDefinitions.Def} into a full grimoire entry: the layout as a
 * Patchouli multiblock, a written legend of every glyph, the reagent bill, the Fluxia
 * cost, and the result.
 *
 * <p>Everything here is derived, so the book cannot describe a circle the altar would
 * reject.</p>
 */
public final class RitualEntryFactory {
    private RitualEntryFactory() {}

    /** Characters used for multiblock mapping keys. '0' is reserved for the altar. */
    private static final String KEY_POOL = "ABCDEFGHIJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    public static Entry create(RitualDefinitions.Def def, int sortnum) {
        Entry e = new Entry(def.chapter().categoryId, def.path(), def.displayName(),
                BuiltInRegistries.ITEM.getKey(def.icon().getItem()).toString())
                .sortnum(sortnum);

        e.page(overviewPage(def));
        e.pages(reagentPages(def));
        e.page(layoutPage(def));
        e.pages(legendPages(def));
        e.page(resultPage(def));
        if (!def.notes().isEmpty()) {
            e.page(text("Marginalia", italic(def.notes())));
        }
        return e;
    }

    // ------------------------------------------------------------------
    // Pages
    // ------------------------------------------------------------------

    private static JsonObject overviewPage(RitualDefinitions.Def def) {
        int r = def.radius();
        int span = r * 2 + 1;

        StringBuilder sb = new StringBuilder();
        sb.append(def.lore()).append(P);
        sb.append(LI).append(bold("Circle: ")).append(span).append("x").append(span)
                .append(" (radius ").append(r).append(")");
        sb.append(LI).append(bold("Glyphs: ")).append(requiredCount(def)).append(" required");

        int optional = def.pattern().size() - requiredCount(def);
        if (optional > 0) sb.append(", ").append(optional).append(" optional");

        sb.append(LI).append(bold("Fluxia: ")).append(thing(def.fluxia() + " ƒ"));

        // A vigil yields nothing you can pick up, so the line that would say "nothing"
        // says what it costs to keep instead.
        if (def.isVigil()) {
            Vigil v = def.vigil().orElseThrow();
            sb.append(LI).append(bold("Burns for: ")).append(thing(Vigil.formatTicks(v.durationTicks())));
            sb.append(LI).append(bold("Upkeep: ")).append(thing(v.upkeepPerSecond() + " ƒ/s"));
        } else if (def.isImbuing()) {
            Imbuement imbuement = def.imbues().orElseThrow();
            sb.append(LI).append(bold("Imbues: "))
                    .append(BookJson.color(imbuement.color(), imbuement.englishName()))
                    .append(" on ").append(imbuement.target().english());
        } else {
            sb.append(LI).append(bold("Yields: ")).append(item(resultName(def)));
        }

        return text(def.displayName(), sb.toString());
    }

    private static List<JsonObject> reagentPages(RitualDefinitions.Def def) {
        if (def.reagents().isEmpty()) {
            return List.of(text("Offerings",
                    "This rite asks for no matter at all — only chalk, patience, and "
                            + thing(def.fluxia() + " ƒ") + " drawn from the altar's own reserve."));
        }

        List<String> lines = new ArrayList<>();
        for (RitualDefinitions.Reagent reagent : def.reagents()) {
            ItemStack stack = reagent.stack();
            lines.add(bold(stack.getCount() + "x ") + item(BookJson.displayName(stack)));
        }

        String intro = "Drop these on the ground inside the circle before you touch the altar. "
                + "Loose items anywhere within " + Math.max(3, def.radius()) + " blocks will be drawn in.";

        return BookJson.bulletPages("Offerings", intro, lines, 8);
    }

    private static JsonObject layoutPage(RitualDefinitions.Def def) {
        int r = def.radius();
        int size = r * 2 + 1;

        // Assign one mapping key per distinct glyph, in a stable order.
        LinkedHashMap<GlyphKey, Character> keys = new LinkedHashMap<>();
        int next = 0;
        for (RitualRecipe.Cell c : sortedRequired(def)) {
            GlyphKey g = GlyphKey.of(c);
            if (!keys.containsKey(g)) {
                keys.put(g, KEY_POOL.charAt(next++ % KEY_POOL.length()));
            }
        }

        char[][] grid = new char[size][size];
        for (char[] row : grid) Arrays.fill(row, ' ');
        grid[r][r] = '0'; // the altar

        for (RitualRecipe.Cell c : def.pattern()) {
            if (!c.required()) continue; // optional guides are described in text, not demanded
            if (c.dx() == 0 && c.dz() == 0) continue;
            grid[c.dz() + r][c.dx() + r] = keys.get(GlyphKey.of(c));
        }

        List<String> rows = new ArrayList<>(size);
        for (char[] row : grid) rows.add(new String(row));

        List<String> mapKeys = new ArrayList<>();
        List<String> mapValues = new ArrayList<>();
        mapKeys.add("0");
        mapValues.add("wispwillow:altar");
        for (var en : keys.entrySet()) {
            mapKeys.add(String.valueOf(en.getValue()));
            mapValues.add(en.getKey().blockState());
        }

        String body = "Rows run north to south, columns west to east. The altar sits at the centre; "
                + "every glyph is drawn on the ground at the altar's own height.";

        return BookJson.multiblock(def.displayName() + " Circle", rows, mapKeys, mapValues, body);
    }

    /** A written breakdown of the circle, ring by ring. */
    private static List<JsonObject> legendPages(RitualDefinitions.Def def) {
        Map<GlyphKey, List<RitualRecipe.Cell>> grouped = new LinkedHashMap<>();
        for (RitualRecipe.Cell c : sortedRequired(def)) {
            grouped.computeIfAbsent(GlyphKey.of(c), k -> new ArrayList<>()).add(c);
        }

        List<String> lines = new ArrayList<>();
        for (var en : grouped.entrySet()) {
            GlyphKey g = en.getKey();
            List<RitualRecipe.Cell> cells = en.getValue();
            lines.add(bold(cells.size() + "x ") + g.describe() + " — " + describePlacement(cells));
        }

        long optional = def.pattern().stream().filter(c -> !c.required()).count();
        String intro = "Read a placement as (east, south) from the altar, so (0,-3) is three blocks north.";
        if (optional > 0) {
            intro += " " + optional + " further marks are purely decorative; the altar ignores them.";
        }

        return BookJson.bulletPages("The Glyphs", intro, lines, 6);
    }

    private static JsonObject resultPage(RitualDefinitions.Def def) {
        if (def.isVigil()) {
            Vigil v = def.vigil().orElseThrow();
            return text("While It Burns",
                    "The altar keeps this rite rather than finishing it, and will perform no other "
                            + "while it does. It holds for " + thing(Vigil.formatTicks(v.durationTicks()))
                            + ", drawing " + thing(v.upkeepPerSecond() + " ƒ") + " a second out of its "
                            + "reserve — " + thing(v.totalCost() + " ƒ") + " over a full burn, on top of "
                            + "the " + thing(def.fluxia() + " ƒ") + " it costs to strike." + P
                            + "Three things end one: it runs its course, the circle beneath it is "
                            + "broken, or the Fluxia runs out. Sneak and click the altar to end it "
                            + "yourself.");
        }
        if (def.isImbuing()) {
            Imbuement imbuement = def.imbues().orElseThrow();
            String first = imbuement.englishDescription(1);
            String last = imbuement.englishDescription(Resonance.MAX_LEVEL);
            String effect = first.equals(last)
                    ? bold("Effect: ") + first + "."
                    : bold("At Resonance I: ") + first + "." + P + bold("At Resonance V: ") + last + ".";
            return text("The Imbuing",
                    "Makes nothing and takes nothing but the reagents. Every awakened piece in the "
                            + "circle that suits it — " + imbuement.target().english() + " — comes back "
                            + "carrying " + bold(imbuement.englishName()) + "."
                            + P + effect
                            + P + "A piece holds one imbuement. Imbue it again and the old one burns away.");
        }
        if (def.isAwakening()) {
            return BookJson.spotlightStack(def.icon(), "The Waking",
                    "This rite makes nothing. It changes what is already lying in the circle: every "
                            + "piece of empyrite on the ground within it wakes at once, and none of it "
                            + "is consumed." + P
                            + "What comes back is the same piece it was — every enchantment, every "
                            + "notch of wear, its name and its trim — carrying one thing more. An "
                            + "awakened piece is lit from inside with a gold light where other work "
                            + "shows violet, and shows it whether or not you ever enchant it. The "
                            + "waking is in the metal, not in the spell: no grindstone takes it back "
                            + "out, and no enchantment laid on afterwards covers it over.");
        }
        if (def.resultMob().isPresent()) {
            String plural = def.resultMobCount() > 1 ? " x" + def.resultMobCount() : "";
            return BookJson.entity(def.resultMob().get().toString(), 1.2f,
                    resultName(def) + plural,
                    "The rite calls it into being directly above the altar. Make sure there is room, "
                            + "and that you are happy to share it.");
        }
        ItemStack stack = def.resultItem().orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) {
            return text("Result", "This rite produces nothing tangible.");
        }
        return BookJson.spotlightStack(stack, resultName(def),
                "Drops onto the altar when the circle closes. If your inventory is full it will "
                        + "simply lie there — the altar never eats its own work.");
    }

    // ------------------------------------------------------------------
    // Description helpers
    // ------------------------------------------------------------------

    private static int requiredCount(RitualDefinitions.Def def) {
        return (int) def.pattern().stream().filter(RitualRecipe.Cell::required).count();
    }

    private static List<RitualRecipe.Cell> sortedRequired(RitualDefinitions.Def def) {
        List<RitualRecipe.Cell> cells = new ArrayList<>();
        for (RitualRecipe.Cell c : def.pattern()) {
            if (c.required() && !(c.dx() == 0 && c.dz() == 0)) cells.add(c);
        }
        // Outermost rings first: that is the order you actually chalk a circle in.
        cells.sort(Comparator
                .comparingInt((RitualRecipe.Cell c) -> -Math.max(Math.abs(c.dx()), Math.abs(c.dz())))
                .thenComparingInt(RitualRecipe.Cell::dz)
                .thenComparingInt(RitualRecipe.Cell::dx));
        return cells;
    }

    /** "the four cardinal points at radius 3", "the corners at radius 1", or a coordinate list. */
    private static String describePlacement(List<RitualRecipe.Cell> cells) {
        Set<Integer> radii = new HashSet<>();
        boolean allCardinal = true, allDiagonal = true;
        for (RitualRecipe.Cell c : cells) {
            int r = Math.max(Math.abs(c.dx()), Math.abs(c.dz()));
            radii.add(r);
            if (c.dx() != 0 && c.dz() != 0) allCardinal = false;
            if (Math.abs(c.dx()) != r || Math.abs(c.dz()) != r) allDiagonal = false;
        }

        if (radii.size() == 1) {
            int r = radii.iterator().next();
            if (cells.size() == 4 && allCardinal) return "the four cardinal points at radius " + r;
            if (cells.size() == 4 && allDiagonal) return "the four corners at radius " + r;
            if (cells.size() >= 8) return "the ring at radius " + r;
        }

        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (RitualRecipe.Cell c : cells) {
            if (shown > 0) sb.append(", ");
            if (shown == 6) { sb.append("…"); break; }
            sb.append('(').append(c.dx()).append(',').append(c.dz()).append(')');
            shown++;
        }
        return sb.toString();
    }

    private static String resultName(RitualDefinitions.Def def) {
        if (def.resultMob().isPresent()) {
            String path = def.resultMob().get().getPath();
            return prettify(path);
        }
        return def.resultItem()
                .map(s -> (s.getCount() > 1 ? s.getCount() + "x " : "") + BookJson.displayName(s))
                .orElse("nothing");
    }

    static String prettify(String snakeCase) {
        return BookJson.prettify(snakeCase);
    }

    // ------------------------------------------------------------------

    /** A (type, colour) pair — one distinct kind of chalk mark. */
    private record GlyphKey(Optional<SigilType> type, Optional<DyeColor> color) {
        static GlyphKey of(RitualRecipe.Cell c) { return new GlyphKey(c.type(), c.color()); }

        String blockState() {
            StringBuilder sb = new StringBuilder("wispwillow:sigil");
            List<String> props = new ArrayList<>(2);
            color.ifPresent(c -> props.add("color=" + c.getName()));
            type.ifPresent(t -> props.add("type=" + t.getSerializedName()));
            if (!props.isEmpty()) sb.append('[').append(String.join(",", props)).append(']');
            return sb.toString();
        }

        String describe() {
            String typeName = type.map(t -> prettify(t.getSerializedName())).orElse("any glyph");
            if (color.isEmpty()) return typeName;
            DyeColor c = color.get();
            return BookJson.color(c.getTextureDiffuseColor(), typeName + " in " + prettify(c.getName()));
        }
    }
}
