package com.reynn.wispwillow.datagen.book;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

/**
 * Small fluent builders for Patchouli's book JSON, so the grimoire can be written as
 * readable Java instead of a few thousand lines of hand-maintained JSON.
 */
public final class BookJson {
    private BookJson() {}

    // ------------------------------------------------------------------
    // Patchouli inline formatting helpers
    // ------------------------------------------------------------------

    /** Paragraph break. */
    public static final String P = "$(br2)";
    /** Line break. */
    public static final String BR = "$(br)";
    /** Bullet. */
    public static final String LI = "$(li)";

    public static String bold(String s)   { return "$(bold)" + s + "$()"; }
    public static String italic(String s) { return "$(italic)" + s + "$()"; }
    /** Patchouli's built-in "item name" colour. */
    public static String item(String s)   { return "$(item)" + s + "$()"; }
    /** Patchouli's built-in "concept" colour. */
    public static String thing(String s)  { return "$(thing)" + s + "$()"; }

    public static String color(int rgb, String s) {
        return String.format("$(#%06X)%s$()", rgb & 0xFFFFFF, s);
    }

    /** Internal book link: {@code link("fluxia_network/mana_pool", "the pool")}. */
    public static String link(String entryPath, String text) {
        return "$(l:" + entryPath + ")" + text + "$(/l)";
    }

    public static String id(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).toString();
    }

    // ------------------------------------------------------------------
    // Pages
    // ------------------------------------------------------------------

    public static JsonObject text(String body) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "patchouli:text");
        o.addProperty("text", body);
        return o;
    }

    public static JsonObject text(String title, String body) {
        JsonObject o = text(body);
        o.addProperty("title", title);
        return o;
    }

    public static JsonObject spotlight(ItemLike item, String title, String body) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "patchouli:spotlight");
        o.addProperty("item", id(item));
        o.addProperty("title", title);
        o.addProperty("text", body);
        o.addProperty("link_recipe", false);
        return o;
    }

    public static JsonObject spotlightStack(ItemStack stack, String title, String body) {
        // Patchouli's spotlight takes a plain item id; the count is stated in the title.
        return spotlight(stack.getItem(), title, body);
    }

    /**
     * A display name that is correct at data-generation time.
     *
     * <p>{@code getHoverName()} resolves against the loaded language file. Vanilla's is
     * loaded during datagen, but this mod's {@code en_us.json} is being written by the
     * same run, so every modded item came out as a raw {@code item.wispwillow.*} key.
     * For our own items we derive the name from the registry path instead.</p>
     */
    public static String displayName(ItemStack stack) {
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"wispwillow".equals(key.getNamespace())) {
            return stack.getHoverName().getString();
        }
        String override = MOD_ITEM_NAMES.get(key.getPath());
        return override != null ? override : prettify(key.getPath());
    }

    /** Mod items whose display name isn't just their path title-cased. */
    private static final java.util.Map<String, String> MOD_ITEM_NAMES = java.util.Map.of(
            "mana_pool", "Willow Fluxia Pool",
            "solrosa", "Solrosa"
    );

    public static String prettify(String snakeCase) {
        String[] parts = snakeCase.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    public static JsonObject crafting(String recipe, String title, String body) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "patchouli:crafting");
        o.addProperty("recipe", recipe);
        if (title != null) o.addProperty("title", title);
        if (body != null) o.addProperty("text", body);
        return o;
    }

    public static JsonObject crafting2(String recipe, String recipe2, String title, String body) {
        JsonObject o = crafting(recipe, title, body);
        o.addProperty("recipe2", recipe2);
        return o;
    }

    public static JsonObject smelting(String recipe, String title, String body) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "patchouli:smelting");
        o.addProperty("recipe", recipe);
        if (title != null) o.addProperty("title", title);
        if (body != null) o.addProperty("text", body);
        return o;
    }

    public static JsonObject entity(String entityId, float scale, String name, String body) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "patchouli:entity");
        o.addProperty("entity", entityId);
        o.addProperty("scale", scale);
        o.addProperty("name", name);
        o.addProperty("text", body);
        return o;
    }

    /**
     * A multi-layer multiblock. Patchouli stores layer {@code i} at
     * {@code y = height - i - 1}, so the <em>first</em> list is the topmost layer and the
     * last is the floor — the opposite of the order it reads like.
     */
    public static JsonObject multiblockLayers(String name, List<List<String>> layers,
                                              List<String> mappingKeys, List<String> mappingValues,
                                              String body) {
        JsonObject mb = new JsonObject();

        JsonArray pattern = new JsonArray();
        for (List<String> layer : layers) {
            JsonArray rows = new JsonArray();
            for (String r : layer) rows.add(r);
            pattern.add(rows);
        }
        mb.add("pattern", pattern);

        JsonObject mapping = new JsonObject();
        for (int i = 0; i < mappingKeys.size(); i++) {
            mapping.addProperty(mappingKeys.get(i), mappingValues.get(i));
        }
        mb.add("mapping", mapping);
        mb.addProperty("symmetrical", true);

        JsonObject o = new JsonObject();
        o.addProperty("type", "patchouli:multiblock");
        o.addProperty("name", name);
        o.addProperty("enable_visualize", true);
        o.add("multiblock", mb);
        o.addProperty("text", body == null ? "" : body);
        return o;
    }

    /** A one-layer, top-down multiblock, which is exactly what a chalk circle is. */
    public static JsonObject multiblock(String name, List<String> rows,
                                        List<String> mappingKeys, List<String> mappingValues,
                                        String body) {
        JsonObject mb = new JsonObject();

        JsonArray layer = new JsonArray();
        for (String r : rows) layer.add(r);
        JsonArray pattern = new JsonArray();
        pattern.add(layer);
        mb.add("pattern", pattern);

        JsonObject mapping = new JsonObject();
        for (int i = 0; i < mappingKeys.size(); i++) {
            mapping.addProperty(mappingKeys.get(i), mappingValues.get(i));
        }
        mb.add("mapping", mapping);
        // The altar never rotates a pattern when matching, so neither should the preview.
        mb.addProperty("symmetrical", true);

        JsonObject o = new JsonObject();
        o.addProperty("type", "patchouli:multiblock");
        o.addProperty("name", name);
        o.addProperty("enable_visualize", true);
        o.add("multiblock", mb);
        o.addProperty("text", body == null ? "" : body);
        return o;
    }

    // ------------------------------------------------------------------
    // Page fitting
    // ------------------------------------------------------------------

    /**
     * How much body text each kind of page has room for, in visible characters.
     *
     * <p>Patchouli does not reflow or paginate: text past the bottom of the page is simply
     * not drawn, and the last visible line gets clipped mid-glyph — which reads as the page
     * having been shot at rather than as text having run out. These numbers are the point
     * where that starts, measured against the widest pages in this book.</p>
     *
     * <p>A title costs about a line and a half. A picture — the spotlight item, the
     * multiblock viewer, a crafting grid — costs most of the page, which is why those
     * budgets are so much smaller than a plain text page's.</p>
     */
    private static final int BUDGET_TEXT_TITLED = 380;
    private static final int BUDGET_TEXT_PLAIN = 440;
    private static final int BUDGET_ILLUSTRATED = 260;

    /** Formatting codes take no space on screen, so they must not count against the budget. */
    private static final java.util.regex.Pattern CODE =
            java.util.regex.Pattern.compile("\\$\\([^)]*\\)");

    private static int visibleLength(String s) {
        return CODE.matcher(s).replaceAll("").length();
    }

    /**
     * Splits one page into as many as its text needs.
     *
     * <p>The overflow always continues onto plain, untitled text pages: the picture and the
     * heading belong to the first page, and repeating either would read as a second entry
     * rather than as the rest of the first.</p>
     */
    public static List<JsonObject> fit(JsonObject page) {
        String body = page.has("text") ? page.get("text").getAsString() : "";
        if (body.isEmpty()) return List.of(page);

        String type = page.has("type") ? page.get("type").getAsString() : "";
        boolean illustrated = !type.equals("patchouli:text");
        boolean titled = page.has("title");

        int firstBudget = illustrated ? BUDGET_ILLUSTRATED
                : (titled ? BUDGET_TEXT_TITLED : BUDGET_TEXT_PLAIN);
        if (visibleLength(body) <= firstBudget) return List.of(page);

        List<String> chunks = split(body, firstBudget, BUDGET_TEXT_PLAIN);

        List<JsonObject> out = new ArrayList<>();
        JsonObject head = page.deepCopy();
        head.addProperty("text", chunks.get(0));
        out.add(head);
        for (int i = 1; i < chunks.size(); i++) out.add(text(chunks.get(i)));
        return out;
    }

    /**
     * Greedily packs a body into chunks, breaking only at paragraph or bullet boundaries.
     *
     * <p>Never mid-sentence, and never inside a formatting code — half a {@code $(bold)}
     * would leak the raw code onto the page and leave the rest of the entry emboldened.</p>
     */
    private static List<String> split(String body, int firstBudget, int restBudget) {
        List<String> blocks = blocks(body);

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int used = 0;
        int budget = firstBudget;

        // A single paragraph can be longer than a whole page on its own, in which case there
        // is no paragraph break to land on and it has to come apart at sentences instead.
        // Measured against the tightest budget in play, not the roomiest: an illustrated
        // first page has far less space than the continuations after it, and a paragraph
        // that fits a continuation can still overrun the page carrying the picture.
        int tightest = Math.min(firstBudget, restBudget);
        List<String> pieces = new ArrayList<>();
        for (String block : blocks) {
            if (visibleLength(block) > tightest) pieces.addAll(sentences(block, tightest));
            else pieces.add(block);
        }

        for (String block : pieces) {
            int len = visibleLength(block);
            if (used > 0 && used + len > budget) {
                chunks.add(trimLeadingBreak(current.toString()));
                current.setLength(0);
                used = 0;
                budget = restBudget;
            }
            current.append(block);
            used += len;
        }
        if (current.length() > 0) chunks.add(trimLeadingBreak(current.toString()));
        return chunks;
    }

    /**
     * Last-resort split, at sentence ends, for a paragraph too long to fit any page.
     *
     * <p>A sentence boundary is the only cut left that does not read as damage. Formatting
     * codes are all of the form {@code $(name)} and never contain a full stop, so scanning
     * for ". " cannot land inside one.</p>
     */
    private static List<String> sentences(String block, int budget) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int used = 0;

        int from = 0;
        while (from < block.length()) {
            int stop = block.indexOf(". ", from);
            int to = stop < 0 ? block.length() : stop + 2;
            String sentence = block.substring(from, to);
            int len = visibleLength(sentence);

            if (used > 0 && used + len > budget) {
                out.add(current.toString());
                current.setLength(0);
                used = 0;
            }
            current.append(sentence);
            used += len;
            from = to;
        }
        if (current.length() > 0) out.add(current.toString());
        return out;
    }

    /**
     * Cuts a body at every paragraph break and bullet, keeping each separator attached to
     * the block it introduces.
     *
     * <p>These are the only two places Patchouli starts a new visual line, so they are the
     * only places a split can land without the seam being visible.</p>
     */
    private static List<String> blocks(String body) {
        List<Integer> cuts = new ArrayList<>();
        for (int i = 0; i < body.length(); ) {
            int p = body.indexOf(P, i);
            int l = body.indexOf(LI, i);
            int next = p < 0 ? l : (l < 0 ? p : Math.min(p, l));
            if (next < 0) break;
            if (next > 0) cuts.add(next);
            i = next + 1;
        }

        List<String> blocks = new ArrayList<>();
        int from = 0;
        for (int cut : cuts) {
            if (cut > from) blocks.add(body.substring(from, cut));
            from = cut;
        }
        if (from < body.length()) blocks.add(body.substring(from));
        return blocks;
    }

    /** A continuation page should not open with a blank line. */
    private static String trimLeadingBreak(String s) {
        return s.startsWith(P) ? s.substring(P.length()) : s;
    }

    // ------------------------------------------------------------------
    // Entries & categories
    // ------------------------------------------------------------------

    public static final class Entry {
        private final JsonObject root = new JsonObject();
        private final JsonArray pages = new JsonArray();
        private final String category;
        private final String path;

        public Entry(String categoryId, String path, String name, String iconId) {
            this.category = categoryId;
            this.path = path;
            root.addProperty("name", name);
            root.addProperty("icon", iconId);
            root.addProperty("category", "wispwillow:" + categoryId);
        }

        public Entry sortnum(int n) { root.addProperty("sortnum", n); return this; }
        public Entry priority(boolean p) { root.addProperty("priority", p); return this; }
        public Entry readByDefault(boolean b) { root.addProperty("read_by_default", b); return this; }
        public Entry flag(String f) { root.addProperty("flag", f); return this; }

        /** Adds a page, splitting it across continuations if its text will not fit. */
        public Entry page(JsonObject page) {
            for (JsonObject part : fit(page)) pages.add(part);
            return this;
        }

        public Entry pages(List<JsonObject> more) {
            for (JsonObject p : more) page(p);
            return this;
        }

        public String categoryId() { return category; }
        public String path() { return path; }

        public JsonObject build() {
            root.add("pages", pages);
            return root;
        }
    }

    public static final class Category {
        private final JsonObject root = new JsonObject();
        private final String path;

        public Category(String path, String name, String description, String iconId, int sortnum) {
            this.path = path;
            root.addProperty("name", name);
            root.addProperty("description", description);
            root.addProperty("icon", iconId);
            root.addProperty("sortnum", sortnum);
        }

        public String path() { return path; }
        public JsonObject build() { return root; }
    }

    /** Splits a list of bullet lines into pages of at most {@code perPage} lines. */
    public static List<JsonObject> bulletPages(String title, String intro, List<String> lines, int perPage) {
        List<JsonObject> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += perPage) {
            StringBuilder sb = new StringBuilder();
            if (i == 0 && intro != null && !intro.isEmpty()) sb.append(intro).append(P);
            for (int j = i; j < Math.min(i + perPage, lines.size()); j++) {
                sb.append(LI).append(lines.get(j));
            }
            out.add(text(i == 0 ? title : title + " (cont.)", sb.toString()));
        }
        return out;
    }
}
