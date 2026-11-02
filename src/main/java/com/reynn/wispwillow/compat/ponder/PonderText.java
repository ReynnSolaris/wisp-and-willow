package com.reynn.wispwillow.compat.ponder;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.blocks.RuneType;
import com.reynn.wispwillow.blocks.alchemy.AlchemicalCauldronBlock;
import com.reynn.wispwillow.blocks.alchemy.CauldronPattern;
import com.reynn.wispwillow.blocks.fluxia.GatewayCoreBlockEntity;
import com.reynn.wispwillow.ritual.RitualDefinitions;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Every line the Ponder scenes say, in English.
 *
 * <p>Outside its editing mode Ponder reads scene text from the language file, never from the
 * scene code, so these have to reach {@code en_us.json}. They live here rather than in the
 * scenes so that datagen can write them without Ponder installed.</p>
 *
 * <p>Ponder runs every line through {@code String.format}, arguments or not, so a literal
 * percent sign has to be written {@code %%}.</p>
 */
public final class PonderText {
    private PonderText() {}

    public static final String TAG_RITUALS = "rituals";
    public static final String TAG_STRUCTURES = "structures";

    public static ResourceLocation id(String key) {
        return ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, key);
    }

    /** The language key Ponder looks a shared line up by. */
    public static String sharedLangKey(String key) {
        return WispWillow.MODID + ".ponder.shared." + key;
    }

    public static String header(String sceneId) {
        return headers().getOrDefault(sceneId, sceneId);
    }

    public static String tagTitle(String tag) {
        return tags().get(tag)[0];
    }

    public static String tagDescription(String tag) {
        return tags().get(tag)[1];
    }

    public static String num(int n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    /** Everything below, under the keys Ponder asks the language file for. */
    public static Map<String, String> langEntries() {
        Map<String, String> out = new LinkedHashMap<>();
        headers().forEach((scene, title) -> out.put(WispWillow.MODID + ".ponder." + scene + ".header", title));
        shared().forEach((key, line) -> out.put(sharedLangKey(key), line));
        tags().forEach((tag, text) -> {
            out.put(WispWillow.MODID + ".ponder.tag." + tag, text[0]);
            out.put(WispWillow.MODID + ".ponder.tag." + tag + ".description", text[1]);
        });
        return out;
    }

    private static Map<String, String> headers() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(PonderLayouts.ALTAR_CASTING, "Casting a Ritual");
        m.put(PonderLayouts.ALTAR_FOUNDATION, "Raising the Altar");
        for (RitualDefinitions.Def def : RitualDefinitions.all()) {
            m.put(PonderLayouts.ritualId(def), escape(def.displayName()));
        }
        m.put(PonderLayouts.CAULDRON, "Building the Alchemical Cauldron");
        m.put(PonderLayouts.GATEWAY, "Building the Soul Gateway");
        return m;
    }

    private static Map<String, String[]> tags() {
        Map<String, String[]> m = new LinkedHashMap<>();
        m.put(TAG_RITUALS, new String[] {"Rituals", "Chalk circles cast at the Willow Altar"});
        m.put(TAG_STRUCTURES, new String[] {"Wisp & Willow Structures", "Things you build rather than place"});
        return m;
    }

    /** Scene lines by the short key the scenes ask for them with. */
    public static Map<String, String> shared() {
        Map<String, String> m = new LinkedHashMap<>();

        // ---- Casting a rite ----
        m.put("altar.intro", "The Willow Altar is where every rite is cast. Its circle is chalked "
                + "on the ground around it, at the altar's own height.");
        m.put("altar.chalk", "Right-click the top of any block with Chalk to draw a glyph there. "
                + "The glyph takes the colour of the chalk that drew it.");
        m.put("altar.cycle", "Click a glyph again to change its shape, or sneak and click to step "
                + "back. Each click also recolours it to the chalk in your hand.");
        m.put("altar.pattern", "Every rite asks for a circle of its own: each glyph the right shape "
                + "and colour, in its exact place. This one is %s.");
        m.put("altar.reagents", "Throw the rite's reagents on the ground inside the circle.");
        m.put("altar.fluxia", "Rites are paid for in Fluxia from the altar's own reserve. Point a "
                + "Thornrose Staff at a charged pool, then at the altar, to keep it fed.");
        m.put("altar.cast", "Then right-click the altar. If the circle, the reagents and the "
                + "reserve are all in order, the rite is cast.");
        m.put("altar.more", "Every rite has a scene like this one, on the item the grimoire files "
                + "it under. All of them are gathered under Rituals in the Ponder index.");

        // ---- Raising the altar ----
        m.put("foundation.intro", "A bare altar is tier I, and will only perform the smallest "
                + "rites. To go further it needs a foundation.");
        m.put("foundation.below", "A foundation is rings of runes laid in the floor one block "
                + "below the altar. The chalk sits above, the runes beneath, and they never collide.");
        m.put("foundation.tier", "Tier %1$s: a complete ring at radius %2$s, %3$s runes.");
        m.put("foundation.gap", "A ring with a single gap counts for nothing, and rings are read "
                + "outward: break this one and every ring beyond it is wasted too.");
        m.put("foundation.blank", "Every rune counts toward the tier. Blank runes do nothing "
                + "else, and are the cheap way to close a ring.");
        m.put("foundation.water", "Water runes deepen the altar's reserve by "
                + num(RuneType.WATER.capacityBonus()) + " Fluxia each.");
        m.put("foundation.earth", "Earth runes take " + pct(RuneType.EARTH.efficiencyBonus())
                + " off the cost of every rite, each.");
        m.put("foundation.blood", "Blood runes give each rite a " + pct(RuneType.BLOOD.yieldBonus())
                + " chance per rune of a doubled result.");
        m.put("foundation.caps", "Efficiency and yield each stop at 50%%, so past a point more of "
                + "the same rune stops paying. Mix them however you like.");
        m.put("foundation.misplaced", "Runes on the altar's own level do nothing. They belong in "
                + "the floor, one block down.");

        // ---- Each rite ----
        m.put("ritual.tier", "This rite needs a tier %1$s altar: %2$s runes, laid in complete "
                + "rings beneath it.");
        m.put("ritual.glyphs", "Chalk the circle outermost ring first. %s glyphs, every one the "
                + "right shape and colour.");
        m.put("ritual.reagents", "Lay the reagents inside the circle: %s.");
        m.put("ritual.fluxia", "Casting it draws %s Fluxia from the altar's reserve.");
        m.put("ritual.result.item", "The circle closes and gives back %s.");
        m.put("ritual.result.summon", "The circle closes and calls %s into being above the altar.");
        m.put("ritual.result.vigil", "The altar keeps this rite rather than finishing it. It burns "
                + "for %1$s, drawing %2$s Fluxia a second; sneak and right-click the altar to end "
                + "it early.");
        m.put("ritual.awaken.lay", "Lay the %s you want awakened inside the circle too. It is not "
                + "a reagent, and none of it is consumed.");
        m.put("ritual.result.awaken", "Every piece wakes at once and comes back exactly as it was, "
                + "lit from within.");
        m.put("ritual.imbue.lay", "Lay the awakened pieces you want imbued inside the circle. Only "
                + "awakened empyrite takes an imbuing, and none of it is consumed.");
        m.put("ritual.result.imbue", "Each piece comes back carrying %s. A piece holds one "
                + "imbuement; imbuing it again replaces the old one.");
        for (RitualDefinitions.Def def : RitualDefinitions.all()) {
            m.put("ritual." + def.path() + ".lore", escape(def.lore()));
        }

        // ---- The Alchemical Cauldron ----
        long fluxite = CauldronPattern.slots().stream().filter(s -> s.offset().getY() < 0).count();
        m.put("cauldron.intro", "The Alchemical Cauldron is built rather than placed: a circle in "
                + "the floor, four pillars, and room to stand between them.");
        m.put("cauldron.circle", "Cut a diamond of " + fluxite + " Fluxite into the floor around "
                + "where the pot will stand.");
        m.put("cauldron.core", "The pot goes in the middle. The block beneath it is left to you: "
                + "Fluxite makes the floor look whole, a hopper drains the finished goods.");
        m.put("cauldron.pillars", "A Willow Log pillar two blocks tall stands on each corner...");
        m.put("cauldron.caps", "...and an Astralite Block caps each one.");
        m.put("cauldron.open", "Leave the two blocks above the pot open. Nothing at the pot's own "
                + "level is asked for, so pipes and chests may stand against its sides.");
        m.put("cauldron.formed", "Close the ring and the pot shows it. Until then, right-clicking "
                + "it names the first missing block and where it goes.");
        m.put("cauldron.light", "Right-click the formed pot to light it for "
                + num(AlchemicalCauldronBlock.IGNITE_COST) + " Fluxia. It draws power like any "
                + "Fluxia machine: link a charged pool to it with a Thornrose Staff.");
        m.put("cauldron.use", "Once lit, right-click it to open its interface. Sneak and "
                + "right-click to put it out; breaking the ring does the same, less politely.");
        m.put("cauldron.feed", "A held bucket moves liquid at any stage, even before the ring is "
                + "built. Any side but the bottom takes reagents and liquid; the bottom gives up "
                + "finished goods.");

        // ---- The Soul Gateway ----
        int logs = PonderLayouts.gatewayFrame().size();
        m.put("gateway.core", "Place the Gateway Core first. Whichever way the wall is built, the "
                + "core finds it when you open the gateway, and familiars step out on the side "
                + "you opened it from.");
        m.put("gateway.frame", "Build a wall of " + logs + " Willow Logs around it: five by five "
                + "with a three by three hole, the core in the middle of the bottom row.");
        m.put("gateway.fluxia", "The core holds " + num(GatewayCoreBlockEntity.CAPACITY) + " Fluxia. "
                + "Point a Thornrose Staff at a charged pool, then at the core, and the pool "
                + "will feed it.");
        m.put("gateway.open", "Right-click the core to open the gateway for "
                + num(GatewayCoreBlockEntity.OPEN_COST) + " Fluxia. It burns "
                + num(GatewayCoreBlockEntity.UPKEEP_PER_TICK * 20) + " a second while it stands "
                + "open; right-click again to shut it.");
        m.put("gateway.collapse", "Knock a log out of the wall, or let the core run dry, and the "
                + "gateway shuts on its own.");
        m.put("gateway.cast", "Throw a Sealed Soul Pact into the lit opening. The gateway takes "
                + num(GatewayCoreBlockEntity.CAST_COST) + " Fluxia for it, and the familiar steps "
                + "out in front.");
        m.put("gateway.pacts", "A sealed pact starts blank, written at the altar. A charged pool "
                + "primes it, a creature fills it, and a second dip in the pool seals it.");

        return m;
    }

    /** A fraction as a percentage, already escaped for Ponder's formatter. */
    private static String pct(float fraction) {
        return String.format(Locale.ROOT, "%.1f%%%%", fraction * 100);
    }

    private static String escape(String text) {
        return text.replace("%", "%%");
    }
}
