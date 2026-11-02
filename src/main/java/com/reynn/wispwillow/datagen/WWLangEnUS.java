package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.awakening.EmpyreanSet;
import com.reynn.wispwillow.awakening.Imbuement;
import com.reynn.wispwillow.blocks.RuneType;
import com.reynn.wispwillow.blocks.SigilType;
import com.reynn.wispwillow.compat.ponder.PonderText;
import com.reynn.wispwillow.registry.*;
import com.reynn.wispwillow.ritual.RitualDefinitions;
import com.reynn.wispwillow.ritual.Vigil;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.function.Supplier;

public class WWLangEnUS extends LanguageProvider {
    public WWLangEnUS(PackOutput output) { super(output, WispWillow.MODID, "en_us"); }

    public String getRitualLoc(String name) {
        return WispWillow.MODID + ".ritual/" + name;
    }

    /** The developer kit: the tab, the tools, and every line the tools say. */
    private void addDeveloperKit() {
        add("itemGroup.wispwillow.dev_tab", "Wisp & Willow: Developer");
        add("item.wispwillow.dev_tool", "Developer tool. Creative mode only.");
        add("item.wispwillow.dev_tool.denied", "That answers only to creative mode.");
        add(BlockRegistry.CREATIVE_MANA_POOL.get(), "Creative Fluxia Pool");
        add("goggles.wispwillow.bottomless", "bottomless");
        add(ItemRegistry.CREATIVE_FLUXIA_TABLET.get(), "Creative Fluxia Tablet");
        add("item.wispwillow.creative_fluxia_tablet.charge", "Fluxia: bottomless");
        add("item.wispwillow.creative_fluxia_tablet.desc", "Drop into a pool: fills it, and is still full after");
        add("item.wispwillow.creative_fluxia_tablet.sneak", "Sneak + right-click: fill your own reserve");
        add("item.wispwillow.creative_fluxia_tablet.filled", "Your reserve is full: %s ƒ.");
        add(ItemRegistry.FLUXIA_PROBE.get(), "Fluxia Probe");
        add("item.wispwillow.fluxia_probe.desc", "Right-click a Fluxia node: fill it and print its state");
        add("item.wispwillow.fluxia_probe.sneak", "Sneak + right-click: drain it instead");
        add(ItemRegistry.RITE_PRIMER.get(), "Rite Primer");
        add("item.wispwillow.rite_primer.desc", "Right-click an altar: drop the drawn rite's reagents and fill it");
        add("item.wispwillow.rite_primer.sneak", "Chalk the circle first, then right-click the altar to perform");
        add(ItemRegistry.RESONANCE_TUNER.get(), "Resonance Tuner");
        add("item.wispwillow.resonance_tuner.desc", "Hold Empyrite in the other hand. Use: awaken it, then raise resonance I to V");
        add("item.wispwillow.resonance_tuner.sneak", "Sneak + use: cycle its imbuement");
        add("item.wispwillow.resonance_tuner.no_piece", "Hold an Empyrite piece in your other hand.");
        add("item.wispwillow.resonance_tuner.asleep", "Awaken it first.");
        add("item.wispwillow.resonance_tuner.nothing_suits", "No imbuement suits that piece.");
        add("item.wispwillow.resonance_tuner.awakened", "Awakened. Resonance I.");
        add("item.wispwillow.resonance_tuner.resonance", "Resonance %s.");
        add("item.wispwillow.resonance_tuner.imbuement", "Imbuement: %s");
        add("item.wispwillow.resonance_tuner.none", "none");
        add(ItemRegistry.HOURGLASS.get(), "Hourglass");
        add("item.wispwillow.hourglass.desc", "Right-click a block: 64 random ticks at once");
        add("item.wispwillow.hourglass.sneak", "Sneak + right-click: 8 ticks on everything within 2 blocks");
        add("item.wispwillow.hourglass.nothing", "Nothing there takes random ticks.");
        add("item.wispwillow.hourglass.ticked", "%s random ticks over %s block(s).");
        add(ItemRegistry.SPIRIT_KEY.get(), "Spirit Key");
        add("item.wispwillow.spirit_key.desc", "Use: enter the Spirit Realm as the Open Way would take you");
        add("item.wispwillow.spirit_key.sneak", "Use again in the realm: come home");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup."+MainRegistry.EXAMPLE_TAB.getId().toLanguageKey(), "Wisp & Willow");
        add("block.wispwillow.altar", "Willow Altar");
        add("block.wispwillow.sigil", "Chalk Sigil");
        add(ItemRegistry.FLUXIA_TABLET.get(), "Fluxia Tablet");
        add(ItemRegistry.GREATER_TABLET.get(), "Greater Fluxia Tablet");
        add(ItemRegistry.GRAND_TABLET.get(), "Grand Fluxia Tablet");
        add("item.wispwillow.tablet.charge", "Fluxia: %s / %s");
        add("item.wispwillow.tablet.stored", "Stored %s Fluxia in the tablet.");
        add("item.wispwillow.tablet.empty", "You have no Fluxia to store.");
        add("item.wispwillow.tablet.hint_draw", "Sneak + right-click: draw from yourself");
        add("item.wispwillow.tablet.hint_pool", "Drop into a pool: fill the pool");
        add(BlockRegistry.VERDANT_BLOOM.get(), "Verdant Bloom");
        add(BlockRegistry.GATHERING_BLOOM.get(), "Gathering Bloom");
        add(BlockRegistry.WARDING_BLOOM.get(), "Warding Bloom");
        add(BlockRegistry.MANA_POOL.get(), "Willow Fluxia Pool");
        addDeveloperKit();
        add(BlockRegistry.FLUXIA_SPITTER.get(), "Fluxia Spitter");
        add(BlockRegistry.FLUXIA_LAMP.get(), "Fluxia Lamp");
        add(BlockRegistry.WILLOW_DOOR.get(), "Willow Door");
        add(BlockRegistry.WILLOW_TRAPDOOR.get(), "Willow Trapdoor");
        add(BlockRegistry.WILLOW_LOG.get(), "Willow Log");
        add(BlockRegistry.WILLOW_LEAVES.get(), "Willow Leaves");
        add(BlockRegistry.FLOWERING_WILLOW_LEAVES.get(), "Flowering Willow Leaves");
        add(BlockRegistry.STRIPPED_WILLOW_LOG.get(), "Stripped Willow Log");
        add(BlockRegistry.WILLOW_PLANKS.get(), "Willow Planks");
        add(BlockRegistry.WILLOW_SAPLING.get(), "Willow Sapling");
        add(BlockRegistry.LUXINIAS_FLOWER.get(), "Luxinia Flower");
        add(BlockRegistry.SOLROSA_FLOWER.get(), "Solrosa");
        add(BlockRegistry.WILLOW_FENCE.get(), "Willow Fence");
        add(BlockRegistry.WILLOW_FENCE_GATE.get(), "Willow Fence Gate");
        add(BlockRegistry.WILLOW_STAIRS.get(), "Willow Stairs");
        add(BlockRegistry.WILLOW_SLAB.get(), "Willow Slab");
        add(BlockRegistry.WILLOW_BUTTON.get(), "Willow Button");
        add(BlockRegistry.WILLOW_PRESSURE_PLATE.get(), "Willow Pressure Plate");
        add(BlockRegistry.WILLOW_VINE.get(), "Willow Vine");
        add(ItemRegistry.WILLOW_LOGS_TAG, "Willow Logs");
        add(BlockRegistry.WILLOW_LOGS_TAG, "Willow Log");
        add(ItemRegistry.DUSTCONVERT.get(), "Fluxia Conversion Dust");
        add(ItemRegistry.CONDENSED_DUST.get(), "Condensed Fluxia Conversion Dust");
        add(BlockRegistry.AMETHYST_FARMLAND.get(), "Amethyst Farmland");
        add(BlockRegistry.FLUXITE.get(), "Fluxite");
        add(BlockRegistry.WILLOW_LANTERN.get(), "Willow Lantern");
        add(ItemRegistry.WILLOW_TORCH.get(), "Willow Torch");
        add(BlockRegistry.SNAD.get(), "Snad");
        add(BlockRegistry.CHUNK_LOADER.get(), "Chunk Loader");
        add(BlockRegistry.AMETHYST_COAL_BLOCK.get(), "Amethyst Coal Block");
        add(ItemRegistry.AMETHYST_COAL.get(), "Amethyst Coal");

        add(BlockRegistry.POTTED_LUXINIAS_FLOWER.get(), "Potted Luxinia Flower");

        add(ItemRegistry.AXOLOTL_FAMILAR_SPAWN_EGG.get(), "Axolotl Familiar Spawn Egg");
        add(ItemRegistry.WISP_FAMILIAR_SPAWN_EGG.get(), "Wisp Familiar Spawn Egg");
        add(EntityRegistry.WISP_FAMILIAR.get(), "Wisp Familiar");
        add(ItemRegistry.SOUL_WISP.get(), "Soul Wisp");

        // ---- The Willow Fen ----
        add(BlockRegistry.WEEPING_LEAVES.get(), "Weeping Leaves");
        add(BlockRegistry.FEN_MOSS.get(), "Fen Moss");
        add(BlockRegistry.WHISPERREED.get(), "Whisperreed");
        add(BlockRegistry.MOONLILY.get(), "Moonlily");
        add(BlockRegistry.WISPCAP.get(), "Wispcap");
        add(BlockRegistry.BOGBELL.get(), "Bogbell");
        add(BlockRegistry.DUSKPETAL.get(), "Duskpetal");
        add(BlockRegistry.POTTED_WISPCAP.get(), "Potted Wispcap");
        add(BlockRegistry.POTTED_BOGBELL.get(), "Potted Bogbell");
        add(BlockRegistry.POTTED_DUSKPETAL.get(), "Potted Duskpetal");
        add(BlockRegistry.PEAT.get(), "Peat");
        add(BlockRegistry.FENSTONE.get(), "Fenstone");
        add(BlockRegistry.FENSTONE_BRICKS.get(), "Fenstone Bricks");
        add(BlockRegistry.MOSSY_FENSTONE_BRICKS.get(), "Mossy Fenstone Bricks");
        add(BlockRegistry.FENSTONE_BRICK_STAIRS.get(), "Fenstone Brick Stairs");
        add(BlockRegistry.FENSTONE_BRICK_SLAB.get(), "Fenstone Brick Slab");
        add(BlockRegistry.FENSTONE_BRICK_WALL.get(), "Fenstone Brick Wall");
        add(BlockRegistry.WILLOW_AMBER.get(), "Willow Amber");
        add(BlockRegistry.CYPRESS_NEEDLES.get(), "Cypress Needles");
        add(BlockRegistry.MOSSY_FENSTONE.get(), "Mossy Fenstone");
        add(BlockRegistry.FENSTONE_PILLAR.get(), "Fenstone Pillar");
        add(BlockRegistry.PEAT_BRICKS.get(), "Peat Bricks");
        add("biome.wispwillow.willow_fen", "Willow Fen");

        // ---- The Spirit Realm ----
        add("biome.wispwillow.spirit_meadow", "Spirit Meadow");
        add("vigil.wispwillow.open_way", "Open Way");
        add("vigil.wispwillow.open_way.entered", "The way opens, and you are through.");
        add("vigil.wispwillow.open_way.returned", "The way closes. You are set down where you left.");
        add("vigil.wispwillow.open_way.cast_out", "The way collapses and casts you out.");
        add(BlockRegistry.BOG_WATER_BLOCK.get(), "Bog Water");
        add(BlockRegistry.WILLOW_SAP_BLOCK.get(), "Willow Sap");
        add("fluid_type.wispwillow.bog_water", "Bog Water");
        add("fluid_type.wispwillow.willow_sap", "Willow Sap");
        add(ItemRegistry.BOG_WATER_BUCKET.get(), "Bucket of Bog Water");
        add(ItemRegistry.WILLOW_SAP_BUCKET.get(), "Bucket of Willow Sap");
        add(ItemRegistry.WILLOW_SAP_BOTTLE.get(), "Bottle of Willow Sap");
        add(ItemRegistry.WISPFLY_SPAWN_EGG.get(), "Wispfly Spawn Egg");
        add(ItemRegistry.FEN_HERON_SPAWN_EGG.get(), "Fen Heron Spawn Egg");
        add(ItemRegistry.WILLOW_HOUND_SPAWN_EGG.get(), "Willow Hound Spawn Egg");
        add(ItemRegistry.GLIMMERFAWN_SPAWN_EGG.get(), "Glimmerfawn Spawn Egg");
        add(ItemRegistry.SPIRIT_DOWN.get(), "Spirit Down");
        add(ItemRegistry.GLIMMER_THREAD.get(), "Glimmer Thread");
        add(ItemRegistry.GLIMMERWEAVE.get(), "Glimmerweave");
        add(ItemRegistry.GLIMMERWEAVE_HOOD.get(), "Glimmerweave Hood");
        add(ItemRegistry.GLIMMERWEAVE_ROBE.get(), "Glimmerweave Robe");
        add(ItemRegistry.GLIMMERWEAVE_LEGGINGS.get(), "Glimmerweave Leggings");
        add(ItemRegistry.GLIMMERWEAVE_SLIPPERS.get(), "Glimmerweave Slippers");
        add("item.wispwillow.glimmerweave.set.two", "Downsoft (2 pieces): no fall damage");
        add("item.wispwillow.glimmerweave.set.four", "Glimmer-sight (4 pieces): see in the dark");
        add(EntityRegistry.WISPFLY.get(), "Wispfly");
        add(EntityRegistry.FEN_HERON.get(), "Fen Heron");
        add(EntityRegistry.WILLOW_HOUND.get(), "Willow Hound");
        add(EntityRegistry.GLIMMERFAWN.get(), "Glimmerfawn");
        add("item.wispwillow.soul_wisp.desc", "Releases a bound familiar.");
        add("item.wispwillow.soul_wisp.hint", "Right-click a familiar to dismiss it.");
        add("item.wispwillow.soul_wisp.released", "%s dissolves into soul-light.");

        // ---- Soul pacts and the Soul Gateway ----
        add(ItemRegistry.AMETHYST_PAPER.get(), "Amethyst Paper");
        add(ItemRegistry.SOUL_PACT.get(), "Soul Pact");
        add(ItemRegistry.PRIMED_SOUL_PACT.get(), "Primed Soul Pact");
        add(ItemRegistry.BOUND_SOUL_PACT.get(), "Bound Soul Pact");
        add(ItemRegistry.SEALED_SOUL_PACT.get(), "Sealed Soul Pact");
        // Overridden name once a pact actually holds something.
        add("item.wispwillow.bound_soul_pact.filled", "Bound Soul Pact (%s)");
        add("item.wispwillow.sealed_soul_pact.filled", "Sealed Soul Pact (%s)");

        add("item.wispwillow.soul_pact.desc.empty", "Blank vellum. It is waiting to be asked.");
        add("item.wispwillow.soul_pact.desc.primed", "Humming with current, and listening.");
        add("item.wispwillow.soul_pact.desc.bound", "Something is in here, and it is not settled.");
        add("item.wispwillow.soul_pact.desc.sealed", "Sealed. Whatever is inside has stopped arguing.");

        add("item.wispwillow.soul_pact.hint.empty", "Drop into a charged Fluxia Pool to prime it.");
        add("item.wispwillow.soul_pact.hint.primed", "Right-click a creature to take its soul.");
        add("item.wispwillow.soul_pact.hint.bound", "Drop into a charged Fluxia Pool to seal it.");
        add("item.wispwillow.soul_pact.hint.sealed", "Cast into an open Soul Gateway.");

        add("item.wispwillow.soul_pact.holds", "Holds: %s");
        add("item.wispwillow.soul_pact.nothing", "nothing");
        add("item.wispwillow.soul_pact.captured", "The pact closes around the %s.");
        add("item.wispwillow.soul_pact.no_answer", "Nothing in the %s answers.");
        add("item.wispwillow.soul_pact.already_bound", "That %s is already spoken for.");

        add(BlockRegistry.GATEWAY_CORE.get(), "Gateway Core");

        // ---- Alchemical Cauldron ----
        add(BlockRegistry.ALCHEMICAL_CAULDRON.get(), "Alchemical Cauldron");
        add("block."+WispWillow.MODID+".alchemical_cauldron.incomplete",
                "The circle is not closed. Missing %s at %s, %s, %s.");
        add("block."+WispWillow.MODID+".alchemical_cauldron.no_fluxia",
                "Not enough fluxia to light it. It wants %s ƒ, and holds %s.");

        add("gui."+WispWillow.MODID+".cauldron.input_tank",  "Reagent Tank");
        add("gui."+WispWillow.MODID+".cauldron.output_tank", "Decanting Tank");
        add("gui."+WispWillow.MODID+".cauldron.empty",       "Empty");
        add("gui."+WispWillow.MODID+".cauldron.amount",      "%s / %s mB");
        add("gui."+WispWillow.MODID+".cauldron.fluxia",      "Fluxia: %s / %s ƒ");
        add("gui."+WispWillow.MODID+".cauldron.dump",        "Pour it out. Nothing is recovered.");
        add("gui."+WispWillow.MODID+".cauldron.recirculate", "Pour the decanting tank back in");
        add("gui."+WispWillow.MODID+".cauldron.recirculate.hint",
                "For brews that condense a solid into a fluid the next brew wants.");

        add("block."+WispWillow.MODID+".alchemical_cauldron.doused",
                "The fire goes out. The %s ƒ you spent lighting it does not come back.");

        add("block."+WispWillow.MODID+".mana_pool.too_thin",
                "The pool is too thin to fill a bucket. It wants %s ƒ, and holds %s.");
        add("block."+WispWillow.MODID+".mana_pool.full", "The pool will not take any more.");
        add(BlockRegistry.GATEWAY_PORTAL.get(), "Soul Gateway");
        add(BlockRegistry.SPIRIT_PORTAL.get(), "The Open Way");
        add(BlockRegistry.SOULWOOD_LOG.get(), "Soulwood Log");
        add(BlockRegistry.SOULWOOD_LEAVES.get(), "Soulwood Leaves");
        add(BlockRegistry.STRIPPED_SOULWOOD_LOG.get(), "Stripped Soulwood Log");
        add(BlockRegistry.SOULWOOD_PLANKS.get(), "Soulwood Planks");
        add(BlockRegistry.SOULWOOD_STAIRS.get(), "Soulwood Stairs");
        add(BlockRegistry.SOULWOOD_SLAB.get(), "Soulwood Slab");
        add(BlockRegistry.SOULWOOD_FENCE.get(), "Soulwood Fence");
        add(BlockRegistry.SOULWOOD_FENCE_GATE.get(), "Soulwood Fence Gate");
        add(BlockRegistry.SOULWOOD_DOOR.get(), "Soulwood Door");
        add(BlockRegistry.SOULWOOD_TRAPDOOR.get(), "Soulwood Trapdoor");
        add(BlockRegistry.SOULWOOD_BUTTON.get(), "Soulwood Button");
        add(BlockRegistry.SOULWOOD_PRESSURE_PLATE.get(), "Soulwood Pressure Plate");
        add(BlockRegistry.GLIMMERBLOOM.get(), "Glimmerbloom");
        add(BlockRegistry.VEILCAP.get(), "Veilcap");
        add(BlockRegistry.MANABLOOM.get(), "Manabloom");
        add(BlockRegistry.EMBERBLOOM.get(), "Emberbloom");
        add(BlockRegistry.DUSKBLOOM.get(), "Duskbloom");
        add(BlockRegistry.SPIRITSTONE.get(), "Spiritstone");
        add(BlockRegistry.SPIRITSTONE_BRICKS.get(), "Spiritstone Bricks");

        // The realm's three new biomes. Spirit Meadow is deliberately not here — it is already
        // translated up with the Willow Fen, and LanguageProvider throws outright on a duplicate
        // key rather than letting the second one win.
        add("biome.wispwillow.soulwood_grove", "Soulwood Grove");
        add("biome.wispwillow.hollow_barrens", "Hollow Barrens");
        add("biome.wispwillow.mistfall_shallows", "Mistfall Shallows");
        add(BlockRegistry.FLUXIA_DYNAMO.get(), "Fluxia Dynamo");
        add(BlockRegistry.ASTRAL_FONT.get(), "Astral Font");

        add("block.wispwillow.gateway_core.opened", "The gateway opens.");
        add("block.wispwillow.gateway_core.closed", "The gateway closes.");
        add("block.wispwillow.gateway_core.already_open", "The gateway is already open.");
        add("block.wispwillow.gateway_core.frame_incomplete",
                "The willow frame is unfinished: %s of its fifteen logs still missing. Five by five, with a three by three opening.");
        add("block.wispwillow.gateway_core.opening_blocked",
                "The opening is not clear: something solid fills %s of the nine cells in the hole.");
        add("block.wispwillow.gateway_core.needs_charge",
                "The core needs %s Fluxia to open. It holds %s.");
        add("familiar.wispwillow.boon.frugal", "Frugal");
        add("familiar.wispwillow.boon.bountiful", "Bountiful");
        add("familiar.wispwillow.boon.capacious", "Capacious");
        add("familiar.wispwillow.boon.balanced", "Balanced");

        for (RuneType rune : RuneType.values()) {
            add(BlockRegistry.RUNES.get(rune).get(), pretty(rune.getSerializedName()) + " Rune");
        }

        add(ItemRegistry.ASTRALITE_INGOT.get(), "Astralite Ingot");
        add(ItemRegistry.ASTRALITE_NUGGET.get(), "Astralite Nugget");
        add(ItemRegistry.RAW_ASTRALITE.get(), "Raw Astralite");
        add(BlockRegistry.ASTRALITE_BLOCK.get(), "Block of Astralite");
        add(BlockRegistry.ASTRALITE_ORE.get(), "Astralite Ore");
        add(BlockRegistry.DEEPSLATE_ASTRALITE_ORE.get(), "Deepslate Astralite Ore");

        add("enchantment.wispwillow.volatile_experiment", "Volatile Experiment");

        // Tag display names. Without these, EMI and JEI show the raw "#wispwillow:soil"
        // id in ingredient tooltips instead of a readable name.
        add("tag.item.wispwillow.soil", "Soils");
        add("tag.item.wispwillow.soil.amethyst_farmland", "Amethyst Farmland");
        add("tag.item.wispwillow.soil.snad", "Snad");


        add(EntityRegistry.AXOLOTL_FAMILIAR.get(), "Axolotl Familiar");
        add(EntityRegistry.FROG_FAMILIAR.get(), "Frog Familiar");
        add(EntityRegistry.BAT_FAMILIAR.get(), "Bat Familiar");
        add(EntityRegistry.RAVEN_FAMILIAR.get(), "Raven Familiar");

        // ---- Empyrite ----
        add(ItemRegistry.EMPYRITE_INGOT.get(), "Empyrite Ingot");
        add(ItemRegistry.EMPYRITE_NUGGET.get(), "Empyrite Nugget");
        add(BlockRegistry.EMPYRITE_BLOCK.get(), "Block of Empyrite");
        add(ItemRegistry.EMPYRITE_SWORD.get(), "Empyrite Sword");
        add(ItemRegistry.EMPYRITE_PICKAXE.get(), "Empyrite Pickaxe");
        add(ItemRegistry.EMPYRITE_AXE.get(), "Empyrite Axe");
        add(ItemRegistry.EMPYRITE_SHOVEL.get(), "Empyrite Shovel");
        add(ItemRegistry.EMPYRITE_HOE.get(), "Empyrite Hoe");
        add(ItemRegistry.EMPYRITE_HELMET.get(), "Empyrite Helm");
        add(ItemRegistry.EMPYRITE_CHESTPLATE.get(), "Empyrite Cuirass");
        add(ItemRegistry.EMPYRITE_LEGGINGS.get(), "Empyrite Tassets");
        add(ItemRegistry.EMPYRITE_BOOTS.get(), "Empyrite Greaves");

        add(ItemRegistry.UPGRADE_HASTE.get(), "Ember Plate");
        add(ItemRegistry.UPGRADE_THRIFT.get(), "Miser's Lining");
        add(ItemRegistry.UPGRADE_CAPACITY.get(), "Deepening Sigil");
        add("gui."+WispWillow.MODID+".cauldron.upgrades", "Upgrades");
        add(ItemRegistry.BAT_FAMILIAR_SPAWN_EGG.get(), "Bat Familiar Spawn Egg");
        add(ItemRegistry.FROG_FAMILIAR_SPAWN_EGG.get(), "Frog Familiar Spawn Egg");
        addFluidSet("Vibrant Fluxia", BlockRegistry.FLUXIA_FLUID_BLOCK, ItemRegistry.FLUXIA_FLUID_BUCKET, null);

        // Ritual display names come from the same table the recipes and the grimoire do,
        // so adding a rite in one place cannot leave an untranslated key behind.
        for (RitualDefinitions.Def def : RitualDefinitions.all()) {
            add(def.langKey(), def.displayName());
        }

        // Altar feedback
        add("ritual.wispwillow.none", "No valid ritual found.");
        add("ritual.wispwillow.missing_glyphs", "Ritual candidate: %s — missing %s glyph(s).");
        add("ritual.wispwillow.not_enough_fluxia", "Ritual candidate: %s — not enough Fluxia. [%s / %s ƒ]");
        add("ritual.wispwillow.missing_reagents", "Ritual candidate: %s — missing reagents nearby.");
        add("ritual.wispwillow.complete", "Ritual completed: %s");
        add("ritual.wispwillow.awakened", "Ritual completed: %s — %s piece(s) of empyrite now burn from within.");
        add("ritual.wispwillow.nothing_to_awaken", "Ritual candidate: %s — no sleeping empyrite lies in the circle.");
        add("item.wispwillow.awakened", "Awakened");
        add("ritual.wispwillow.runes_misplaced", "Those %s runes sit beside the altar; a foundation must be laid one block BELOW it.");
        add("ritual.wispwillow.tier_too_low", "Ritual candidate: %s — needs a tier %s altar (yours is tier %s).");

        // ---- Omni Chalk ----
        add(ItemRegistry.OMNI_CHALK.get(), "Omni Chalk");
        add("item.wispwillow.omni_chalk.auto", "whatever the altar has recognised");
        add("item.wispwillow.omni_chalk.set_to", "Set to: %s");
        add("item.wispwillow.omni_chalk.selected", "Omni Chalk: %s");
        add("item.wispwillow.omni_chalk.hint_draw", "Right-click an altar: draw the rite");
        add("item.wispwillow.omni_chalk.hint_erase", "Sneak + right-click an altar: rub the circle out");
        add("item.wispwillow.omni_chalk.hint_scroll", "Sneak + scroll: choose a rite");
        add("item.wispwillow.omni_chalk.hint_chapter", "Sneak + right-click the air: next chapter");
        add("item.wispwillow.omni_chalk.no_rite", "No rite selected, and this altar has not recognised one.");
        add("item.wispwillow.omni_chalk.already_drawn", "%s is already drawn.");
        add("item.wispwillow.omni_chalk.blocked_only", "%s cannot be drawn: %s squares are obstructed.");
        add("item.wispwillow.omni_chalk.drew", "Drew %s glyphs — %s.");
        add("item.wispwillow.omni_chalk.drew_blocked", "Drew %s glyphs — %s. %s squares were obstructed.");
        add("item.wispwillow.omni_chalk.drew_partial", "Drew %s glyphs — %s. The chalk ran out %s short.");
        add("item.wispwillow.omni_chalk.worn_out", "The chalk is worn down to nothing.");
        add("item.wispwillow.omni_chalk.erased", "Rubbed out %s glyphs.");
        add("item.wispwillow.omni_chalk.nothing_to_erase", "There is nothing drawn around this altar.");

        // Vigils. Each effect is named as well as each rite: the rite is what you drew,
        // the effect is what the altar is doing, and the book talks about both.
        add(Vigil.Effect.CLEAR_SKIES.translationKey(), "Clear Skies");
        add(Vigil.Effect.GATHERING_STORM.translationKey(), "Gathering Storm");
        add(Vigil.Effect.VERDANT_TIDE.translationKey(), "Verdant Tide");
        add(Vigil.Effect.WARDING_LIGHT.translationKey(), "Warding Light");
        add("ritual.wispwillow.vigil_unnamed", "a vigil");
        add("ritual.wispwillow.vigil_begun", "Vigil struck: %s — burning for %s.");
        add("ritual.wispwillow.vigil_busy", "The altar is keeping %s (%s left). Sneak and click to end it.");
        add("ritual.wispwillow.vigil_spent", "The vigil %s has run its course.");
        add("ritual.wispwillow.vigil_broken", "The circle beneath %s is broken; the vigil gutters out.");
        add("ritual.wispwillow.vigil_starved", "%s has run dry of Fluxia and gone out.");
        add("ritual.wispwillow.vigil_cancelled", "You let %s go out.");
        add("goggles.wispwillow.vigil", "Keeping");
        add("goggles.wispwillow.vigil_remaining", "Remaining");
        add("goggles.wispwillow.vigil_upkeep", "Upkeep");
        add("tooltip.wispwillow.tablet_charge", "Tablet: %s / %s ƒ");
        add(BlockRegistry.AMETHYST_SUGAR_CANE.get(), "Amethyst Sugar Cane");
        //fluxia wand

        add(ItemRegistry.FLUX_LINKING_WAND.get(), "Thornrose Staff");
        add(ItemRegistry.FLUX_LINKING_WAND.get().getDescriptionId()+".desc", "Links Fluxia pools, batteries, and machines together.");
        add(ItemRegistry.FLUX_LINKING_WAND.get().getDescriptionId()+".hint_first", "§7Right-click a Fluxia source (pool, battery) to set origin.");
        add(ItemRegistry.FLUX_LINKING_WAND.get().getDescriptionId()+".hint_second", "§7Right-click a Fluxia target (machine, spitter) to link.");
        add(ItemRegistry.FLUX_LINKING_WAND.get().getDescriptionId()+".hint_clear", "§7Sneak + right-click to clear stored origin.");
        add(ItemRegistry.FLUX_LINKING_WAND.get().getDescriptionId()+".set_source", "Stored source at %s");
        add(ItemRegistry.FLUX_LINKING_WAND.get().getDescriptionId()+".cleared", "Cleared stored source.");
        add(ItemRegistry.FLUX_LINKING_WAND.get().getDescriptionId()+".dim_mismatch", "Dimension mismatch: source in %s, target in %s");
        add(ItemRegistry.FLUX_LINKING_WAND.get().getDescriptionId()+".link_success", "Linked %s → %s");
        add(ItemRegistry.FLUX_LINKING_WAND.get().getDescriptionId()+".link_failed", "Failed to link %s → %s");


        //end

        addAdvancementLang("root", "Wisp & Willow", "An enchanting book is yearning for power...");
        addAdvancementLang("first_steps", "First Steps", "Harness your inner Fluxia and gather a willow sapling.");
        addAdvancementLang("altar", "Magical Journey", "Shadow Money Wizard Gang, Legalize nuclear bombs (Wha'? Wha'?)");
        addAdvancementLang("chalk", "Designing Sigils", "Use chalk to draw all ("+ SigilType.values().length+") sigils for magical rituals.");
        addAdvancementLang("tablet", "§5Portable Fluxia", "Craft a fluxia tablet to carry around extra or portable fluxia.");
        addAdvancementLang("m_coal", "Minor Arcana", "Perform the 'Minor Arcana' ritual and obtain amethyst coal.");
        addAdvancementLang("farm", "Crystalized Farmland", "Perform the 'Crystalized Farmland' ritual and obtain amethyst farmland.");
        addAdvancementLang("cauldron", "§5The Pot Boils", "Craft an Alchemical Cauldron, then close a ring around it and light it.");
        addAdvancementLang("empyrite", "Above the Nether", "Brew an Empyrite Ingot. There is no ore for this one.");
        addAdvancementLang("awakening", "§6Wake the Metal", "Perform the Awakening Empyrite rite on a piece of your gear.");
        addAdvancementLang("imbuing", "One More Trick", "Lay an imbuement on an awakened piece at the altar.");
        addAdvancementLang("resonance", "§6Fully Resonant", "Carry a piece all the way to Resonance V.");
        addAdvancementLang("regalia", "§6The Empyrean Regalia", "Wear all four awakened empyrite pieces at once.");
        addAdvancementLang("phasing", "§bUnmoored", "Go under in Liquid Fluxia and feel the stone let go of you.");
        addAdvancementLang("oasis", "§bA Light Under the Hill", "Find a Fluxia Oasis.");
        addAdvancementLang("oasis_cache", "§bBeneath the Veil", "Open the cache sealed under the oasis pool. Four stones weep; only one is thin.");

        add("effect."+WispWillow.MODID+".phasing", "Phasing");
        addBlock(BlockRegistry.WARDED_SLATE, "Warded Slate");

        // The riddle on the sign by the oasis pool. Fifteen characters a line, at most.
        add("sign."+WispWillow.MODID+".oasis.1", "Four stones weep");
        add("sign."+WispWillow.MODID+".oasis.2", "but only one");
        add("sign."+WispWillow.MODID+".oasis.3", "is thin: the one");
        add("sign."+WispWillow.MODID+".oasis.4", "in willow shade");

        for (DyeColor c : DyeColor.values()) {
            add(ItemRegistry.CHALK.get(c).get(), "Chalk (" + pretty(c.getName()) + ")");
        }

        add("tooltip."+WispWillow.MODID+".capacity", "Capacity: %s");

        // Arcane Goggles
        add(ItemRegistry.ARCANE_GOGGLES.get(), "Arcane Goggles");
        add("item."+WispWillow.MODID+".arcane_goggles.desc", "Lets you see Fluxia move.");
        add("item."+WispWillow.MODID+".arcane_goggles.hint", "Wear them, or just hold them.");
        addGoggleLang("stored", "Stored: ");
        addGoggleLang("buffer", "Buffer: ");
        addGoggleLang("charge", "Charge: ");
        addGoggleLang("reserve", "Reserve: ");
        addGoggleLang("links", "Links: ");
        addGoggleLang("output", "Output: ");
        addGoggleLang("target", "Target: ");
        addGoggleLang("fuel", "Fuel: ");
        addGoggleLang("rite", "Rite: ");
        addGoggleLang("cost", "Cost: ");
        addGoggleLang("circle", "Circle: ");
        addGoggleLang("tier", "Altar tier: ");
        addGoggleLang("boons", "Boons: ");
        addGoggleLang("reagents", "Reagents: ");
        addGoggleLang("needs_tier", "Requires: ");
        addGoggleLang("foundation", "Foundation: ");
        addGoggleLang("familiars", "Familiars: ");
        add("goggles.wispwillow.foundation_hint", "  Lay them one block BELOW the altar.");
        add("goggles.wispwillow.familiar_hint", "  Raise the altar's tier to keep more.");
        add("goggles.wispwillow.boons_hint", "  Cheaper in Fluxia and in reagents alike.");
        addGoggleLang("upgrades", "Fitted: ");
        addGoggleLang("cauldron_structure", "Ring: ");
        addGoggleLang("cauldron_fire", "Fire: ");
        addGoggleLang("cauldron_reagent", "Reagents: ");
        addGoggleLang("cauldron_decant", "Decanting: ");
        addGoggleLang("progress", "Progress: ");
        addGoggleLang("effect", "Effect: ");
        addGoggleLang("radius", "Radius: ");
        addGoggleLang("status", "Status: ");
        add("goggles.wispwillow.effect.verdant", "Accelerates growth");
        add("goggles.wispwillow.effect.gathering", "Gathers dropped items");
        add("goggles.wispwillow.effect.warding", "Wards off hostiles");
        addGoggleLang("items", "Items: ");
        addGoggleLang("fluids", "Fluids: ");
        addGoggleLang("free", "Free: ");
        addGoggleLang("gateway_state", "Gateway: ");
        addGoggleLang("gateway_frame", "Frame: ");
        addGoggleLang("upkeep", "Upkeep: ");
        addGoggleLang("cast_cost", "Per pact: ");
        addGoggleLang("burns_for", "Burns for: ");
        addGoggleLang("sky", "Sky: ");

        // Sigil shapes, used by the JEI ritual pages and by tooltips.
        for (SigilType t : SigilType.values()) {
            add("sigil."+WispWillow.MODID+"."+t.getSerializedName(), pretty(t.getSerializedName()));
        }
        add("sigil."+WispWillow.MODID+".any", "Any Glyph");

        // Recipe-viewer pages (JEI). The keys keep the emi. prefix they were first written under.
        add("emi.category."+WispWillow.MODID+".ritual", "Ritual Circle");
        add("emi.category."+WispWillow.MODID+".infusion", "Fluxia Infusion");
        add("emi."+WispWillow.MODID+".infusion.title", "Drop into a charged pool");
        add("emi."+WispWillow.MODID+".ritual.cost", "Fluxia: %s ƒ");
        add("emi."+WispWillow.MODID+".ritual.grid", "%sx%s");
        add("emi."+WispWillow.MODID+".ritual.summons", "Summons %sx %s");
        add("emi."+WispWillow.MODID+".ritual.glyph", "%s in %s");
        add("emi."+WispWillow.MODID+".ritual.optional", "Optional (decorative)");
        add("emi.category."+WispWillow.MODID+".gateway", "Soul Gateway");
        add("emi."+WispWillow.MODID+".gateway.title", "Cast into an open Soul Gateway");
        add("emi."+WispWillow.MODID+".gateway.summons", "Summons %s");
        add("emi."+WispWillow.MODID+".gateway.summons_count", "Summons %s x %s");
        add("emi."+WispWillow.MODID+".gateway.drops", "Nothing steps out; the items fall in front of the core");
        add("entity."+WispWillow.MODID+".overlay_host", "Arcane Overlay");
        add("emi.category."+WispWillow.MODID+".alchemy", "Alchemical Cauldron");
        add("emi."+WispWillow.MODID+".alchemy.title", "Brewed in a lit cauldron");
        add("emi."+WispWillow.MODID+".alchemy.cost", "Fluxia: %s ƒ");
        add("emi."+WispWillow.MODID+".alchemy.time", "Takes %s");
        add("emi."+WispWillow.MODID+".alchemy.fluid_in", "In: %s mB");
        add("emi."+WispWillow.MODID+".alchemy.fluid_out", "Out: %s mB");

        addConfigLang("snad", "Snad Config");
        addConfigLang("speed", "Snad Tick Speed");
        addConfigLang("maxHeight", "Max Height Check Limit");
        addConfigLang("chancePermille", "Snad Tick Chance");
        addConfigLang("forceMaxAge", "Force Max Age");
        addConfigLang("whitelist", "Snad Whitelist");

        addSoundLang("infusion", "Item Infusion");

        // Ponder reads its scene text from here, never from the scene code.
        PonderText.langEntries().forEach(this::add);

        addAwakeningLang();
    }

    /** Resonance, imbuements and the Regalia. Numbers come from the classes that apply them. */
    private void addAwakeningLang() {
        add("awakening.wispwillow.resonance", "Resonance %s");
        add("awakening.wispwillow.progress", "%s / %s resonance toward %s");
        add("awakening.wispwillow.maxed", "Fully resonant");
        add("awakening.wispwillow.dormant", "Dormant — the Awakening rite would wake it");
        add("awakening.wispwillow.imbued", "Imbued: %s");
        add("awakening.wispwillow.resonance_up", "%s resonates more deeply — Resonance %s");
        add("awakening.wispwillow.set", "Empyrean Regalia (%s/%s)");
        add("awakening.wispwillow.set.two", "(" + EmpyreanSet.EMBERPROOF + ") Emberproof: fire cannot "
                + "touch you, and lava bites half as hard");
        add("awakening.wispwillow.set.four", "(" + EmpyreanSet.AEGIS + ") Radiant Aegis: "
                + Math.round(EmpyreanSet.AEGIS_REDUCTION * 100) + "%% less damage, melee attackers "
                + "burst into flame, and resonance grows twice as fast");

        for (Imbuement imbuement : Imbuement.values()) {
            add(imbuement.langKey(), imbuement.englishName());
            add(imbuement.langKey() + ".desc", imbuement.englishDescriptionTemplate());
        }

        add("ritual.wispwillow.imbued", "Ritual completed: %s — %s piece(s) now carry %s.");
        add("ritual.wispwillow.nothing_to_imbue",
                "Ritual candidate: %s — no awakened piece in the circle can take this imbuing.");
        add("emi.wispwillow.ritual.imbues", "Imbues: %s");
        add("emi.wispwillow.ritual.awakens", "Awakens empyrite");
    }

    private void addGoggleLang(String key, String label) {
        add("goggles."+WispWillow.MODID+"."+key, label);
    }

    private void addSoundLang(String configName, String name) {
        add("sound."+WispWillow.MODID+"."+configName, name);
    }

    private void addConfigLang(String configName, String name) {
        add("wispwillow.configuration."+configName, name);
    }

    private void addScreenLang(String screenName, String title) {
        add("screen."+WispWillow.MODID+"."+screenName, title);
    }

    private void addAdvancementLang(String advancementName, String title, String description) {
        String preName = "advancements."+ WispWillow.MODID +"."+advancementName;
        add(preName+".title", title);
        add(preName+".description", description);
    }

    private void addFluidSet(String name, Supplier<? extends LiquidBlock> block, Supplier<? extends BucketItem> bucket, Supplier<? extends AbstractCauldronBlock> cauldron) {
        if (null != block) {
            addBlock(block, name);
            add(block.get().fluid.getFluidType().getDescriptionId(), name);
        }
        if (null != bucket) {
            addItem(bucket, name + " Bucket");
        }
    }
    private static String pretty(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1).replace('_',' ');
    }
}
