package com.reynn.wispwillow.registry;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.dev.CreativeFluxiaTabletItem;
import com.reynn.wispwillow.dev.FluxiaProbeItem;
import com.reynn.wispwillow.dev.HourglassItem;
import com.reynn.wispwillow.dev.ResonanceTunerItem;
import com.reynn.wispwillow.dev.RitePrimerItem;
import com.reynn.wispwillow.dev.SpiritKeyItem;
import com.reynn.wispwillow.items.*;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WispWillow.MODID);
    public static final Map<DyeColor, DeferredItem<Item>> CHALK = new EnumMap<>(DyeColor.class);
    static {
        for (DyeColor c : DyeColor.values()) {
            CHALK.put(c, ITEMS.register("chalk_" + c.getName(),
                    () -> new ChalkItem(new Item.Properties(), c)));
        }
    }
    /** Every colour in one stick: draws and erases whole circles. */
    public static final DeferredItem<Item> OMNI_CHALK = ITEMS.register("omni_chalk",
            () -> new OmniChalkItem(new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> FLUXIA_TABLET = ITEMS.register(
            "fluxia_tablet",
            () -> new FluxiaTabletItem(new Item.Properties().stacksTo(1))
    );

    /** Infused upgrades: five times, then twenty-five times the base tablet. */
    public static final DeferredItem<Item> GREATER_TABLET = ITEMS.register(
            "greater_fluxia_tablet",
            () -> new FluxiaTabletItem(100_000, 8_000,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE))
    );

    public static final DeferredItem<Item> GRAND_TABLET = ITEMS.register(
            "grand_fluxia_tablet",
            () -> new FluxiaTabletItem(500_000, 32_000,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
    );

    //FLUID
    public static final DeferredHolder<Item, BucketItem> FLUXIA_FLUID_BUCKET = ITEMS.register(
            "fluxia_fluid_bucket",
            () -> new BucketItem(FluidRegistry.FLUXIA_FLUID.get(), new Item.Properties())
    );

    //DISKS

    public static final DeferredItem<Item> DUSTCONVERT = ITEMS.register("fluxia_converter_dust",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> CONDENSED_DUST = ITEMS.register("fluxia_condensed_dust", () -> new CondensedDustItem(new Item.Properties()));

    // Runes are blocks now (see BlockRegistry.RUNES); their items come from the
    // block-item loop at the bottom of this class.

    //ASTRALITE

    public static final DeferredItem<Item> RAW_ASTRALITE = ITEMS.register("raw_astralite", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ASTRALITE_INGOT = ITEMS.register("astralite_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ASTRALITE_NUGGET = ITEMS.register("astralite_nugget", () -> new Item(new Item.Properties()));

    // ---- Empyrite ---------------------------------------------------------
    // The tier above astralite, and the mod's first. Every piece of it comes out of the
    // Alchemical Cauldron; there is no ore, no smelting recipe and no trade for it, which is
    // what stops the multiblock from being optional decoration.

    public static final DeferredItem<Item> EMPYRITE_INGOT =
            ITEMS.register("empyrite_ingot", () -> new Item(new Item.Properties().fireResistant()));
    public static final DeferredItem<Item> EMPYRITE_NUGGET =
            ITEMS.register("empyrite_nugget", () -> new Item(new Item.Properties().fireResistant()));

    public static final DeferredItem<Item> EMPYRITE_SWORD = ITEMS.register("empyrite_sword",
            () -> new SwordItem(ToolTiers.EMPYRITE, new Item.Properties().fireResistant()
                    .attributes(SwordItem.createAttributes(ToolTiers.EMPYRITE, 3, -2.4f))));
    public static final DeferredItem<Item> EMPYRITE_PICKAXE = ITEMS.register("empyrite_pickaxe",
            () -> new PickaxeItem(ToolTiers.EMPYRITE, new Item.Properties().fireResistant()
                    .attributes(PickaxeItem.createAttributes(ToolTiers.EMPYRITE, 1, -2.8f))));
    public static final DeferredItem<Item> EMPYRITE_AXE = ITEMS.register("empyrite_axe",
            () -> new AxeItem(ToolTiers.EMPYRITE, new Item.Properties().fireResistant()
                    .attributes(AxeItem.createAttributes(ToolTiers.EMPYRITE, 5.0f, -3.0f))));
    public static final DeferredItem<Item> EMPYRITE_SHOVEL = ITEMS.register("empyrite_shovel",
            () -> new ShovelItem(ToolTiers.EMPYRITE, new Item.Properties().fireResistant()
                    .attributes(ShovelItem.createAttributes(ToolTiers.EMPYRITE, 1.5f, -3.0f))));
    public static final DeferredItem<Item> EMPYRITE_HOE = ITEMS.register("empyrite_hoe",
            () -> new HoeItem(ToolTiers.EMPYRITE, new Item.Properties().fireResistant()
                    .attributes(HoeItem.createAttributes(ToolTiers.EMPYRITE, -3, 0.0f))));

    // Worn in 3D: WWGeoArmorItem is an ordinary ArmorItem that hands GeckoLib its renderer.
    public static final DeferredItem<Item> EMPYRITE_HELMET = ITEMS.register("empyrite_helmet",
            () -> new WWGeoArmorItem(ArmorMaterialRegistry.EMPYRITE, ArmorItem.Type.HELMET,
                    new Item.Properties().fireResistant().durability(ArmorItem.Type.HELMET.getDurability(45)), "empyrite", true));
    public static final DeferredItem<Item> EMPYRITE_CHESTPLATE = ITEMS.register("empyrite_chestplate",
            () -> new WWGeoArmorItem(ArmorMaterialRegistry.EMPYRITE, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().fireResistant().durability(ArmorItem.Type.CHESTPLATE.getDurability(45)), "empyrite", true));
    public static final DeferredItem<Item> EMPYRITE_LEGGINGS = ITEMS.register("empyrite_leggings",
            () -> new WWGeoArmorItem(ArmorMaterialRegistry.EMPYRITE, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().fireResistant().durability(ArmorItem.Type.LEGGINGS.getDurability(45)), "empyrite", true));
    public static final DeferredItem<Item> EMPYRITE_BOOTS = ITEMS.register("empyrite_boots",
            () -> new WWGeoArmorItem(ArmorMaterialRegistry.EMPYRITE, ArmorItem.Type.BOOTS,
                    new Item.Properties().fireResistant().durability(ArmorItem.Type.BOOTS.getDurability(45)), "empyrite", true));

    // ---- Cauldron upgrades ------------------------------------------------
    // Throughput progression. These do nothing in a crafting grid and nothing in a chest;
    // they exist to be dropped into the cauldron's upgrade tray.

    public static final DeferredItem<Item> UPGRADE_HASTE =
            ITEMS.register("upgrade_haste", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> UPGRADE_THRIFT =
            ITEMS.register("upgrade_thrift", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> UPGRADE_CAPACITY =
            ITEMS.register("upgrade_capacity", () -> new Item(new Item.Properties().stacksTo(16)));


    //OTHER

    public static final DeferredItem<Item> WILLOW_TORCH = ITEMS.register("willow_torch", () -> new StandingAndWallBlockItem(BlockRegistry.WILLOW_TORCH.get(), BlockRegistry.WILLOW_TORCH_WALL.get(), new Item.Properties(), Direction.DOWN));

    public static final DeferredItem<Item> AMETHYST_COAL = ITEMS.register("amethyst_coal", () -> new FuelItem(new Item.Properties(), 3200));
    //Tags

    public static final TagKey<Item> WILLOW_LOGS_TAG = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "willow_logs_item")
    );

    public static final TagKey<Item> SOULWOOD_LOGS_TAG = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "soulwood_logs_item")
    );

    public static final DeferredItem<Item> ARCANE_GOGGLES = ITEMS.register(
            "arcane_goggles",
            () -> new ArcaneGogglesItem(ArmorMaterialRegistry.ARCANE, new Item.Properties().stacksTo(1).durability(220))
    );

    public static final DeferredItem<Item> FLUX_LINKING_WAND = ITEMS.register(
            "flux_linking_wand",
            () -> new FluxLinkingWandItem(new Item.Properties().stacksTo(1))
    );

    /** The only way to dismiss a familiar. */
    public static final DeferredItem<Item> SOUL_WISP = ITEMS.register("soul_wisp",
            () -> new SoulWispItem(new Item.Properties().rarity(Rarity.RARE)));

    /**
     * Pressed from Amethyst Sugar Cane. The only thing a soul pact will be written on, which
     * is what puts the whole familiar chain behind a crop rather than behind a crafting table.
     */
    public static final DeferredItem<Item> AMETHYST_PAPER = ITEMS.register("amethyst_paper",
            () -> new Item(new Item.Properties()));

    // ---- Soul pacts: the road to a familiar -------------------------------
    // Four registered items rather than one with a flag, because the pool transmutes by
    // Ingredient and an Ingredient cannot tell a charged pact from an empty one.

    /** Paper and intent. Charge it in a pool. */
    public static final DeferredItem<Item> SOUL_PACT = ITEMS.register("soul_pact",
            () -> new SoulPactItem(SoulPactItem.Stage.EMPTY, new Item.Properties()));

    /** Charged, and will take a soul from anything the pact table knows. */
    public static final DeferredItem<Item> PRIMED_SOUL_PACT = ITEMS.register("primed_soul_pact",
            () -> new SoulPactItem(SoulPactItem.Stage.PRIMED,
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    /** Holds a creature. Seal it in a pool. */
    public static final DeferredItem<Item> BOUND_SOUL_PACT = ITEMS.register("bound_soul_pact",
            () -> new SoulPactItem(SoulPactItem.Stage.BOUND,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    /** Sealed and spendable. Cast it into an open Soul Gateway. */
    public static final DeferredItem<Item> SEALED_SOUL_PACT = ITEMS.register("sealed_soul_pact",
            () -> new SoulPactItem(SoulPactItem.Stage.SEALED,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    // ---- Spawn eggs ----
    // Every egg is its own hand-drawn creature sprite (textures/item/<id>.png, from
    // tools/gen_item_sprites.py) rather than the shared two-colour egg. DeferredSpawnEggItem still
    // registers a tint for layer 0, so the colours are white: a white tint leaves the sprite as drawn.
    private static final int EGG_UNTINTED = 0xFFFFFF;

    public static final DeferredItem<Item> WISP_FAMILIAR_SPAWN_EGG = ITEMS.register("wisp_familiar_spawn_egg",
            () -> new DeferredSpawnEggItem(EntityRegistry.WISP_FAMILIAR, EGG_UNTINTED, EGG_UNTINTED, new Item.Properties())
    );

    public static final DeferredItem<Item> BAT_FAMILIAR_SPAWN_EGG = ITEMS.register("bat_familiar_spawn_egg",
            () -> new DeferredSpawnEggItem(EntityRegistry.BAT_FAMILIAR, EGG_UNTINTED, EGG_UNTINTED, new Item.Properties())
    );

    public static final DeferredItem<Item> FROG_FAMILIAR_SPAWN_EGG = ITEMS.register("frog_familiar_spawn_egg",
            () -> new DeferredSpawnEggItem(EntityRegistry.FROG_FAMILIAR, EGG_UNTINTED, EGG_UNTINTED, new Item.Properties())
    );

    public static final DeferredItem<Item> AXOLOTL_FAMILAR_SPAWN_EGG = ITEMS.register("axolotl_familar_spawn_egg",
            () -> new DeferredSpawnEggItem(EntityRegistry.AXOLOTL_FAMILIAR, EGG_UNTINTED, EGG_UNTINTED, new Item.Properties())
    );

    // ---- The Willow Fen ----------------------------------------------------

    public static final DeferredHolder<Item, BucketItem> BOG_WATER_BUCKET = ITEMS.register("bog_water_bucket",
            () -> new BucketItem(FluidRegistry.BOG_WATER.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredHolder<Item, BucketItem> WILLOW_SAP_BUCKET = ITEMS.register("willow_sap_bucket",
            () -> new BucketItem(FluidRegistry.WILLOW_SAP.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    /** Tapped from a willow log with a glass bottle. */
    public static final DeferredItem<Item> WILLOW_SAP_BOTTLE = ITEMS.register("willow_sap_bottle",
            () -> new WillowSapBottleItem(new Item.Properties()
                    .craftRemainder(Items.GLASS_BOTTLE)
                    .stacksTo(16)
                    .food(new FoodProperties.Builder()
                            .nutrition(3)
                            .saturationModifier(0.2F)
                            .alwaysEdible()
                            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 1.0F)
                            .build())));

    // Tall and water-borne plants need the vanilla placement items, not a plain block item.
    public static final DeferredItem<Item> WHISPERREED = ITEMS.register("whisperreed",
            () -> new DoubleHighBlockItem(BlockRegistry.WHISPERREED.get(), new Item.Properties()));
    public static final DeferredItem<Item> MOONLILY = ITEMS.register("moonlily",
            () -> new PlaceOnWaterBlockItem(BlockRegistry.MOONLILY.get(), new Item.Properties()));

    public static final DeferredItem<Item> WISPFLY_SPAWN_EGG = ITEMS.register("wispfly_spawn_egg",
            () -> new DeferredSpawnEggItem(EntityRegistry.WISPFLY, EGG_UNTINTED, EGG_UNTINTED, new Item.Properties()));
    public static final DeferredItem<Item> FEN_HERON_SPAWN_EGG = ITEMS.register("fen_heron_spawn_egg",
            () -> new DeferredSpawnEggItem(EntityRegistry.FEN_HERON, EGG_UNTINTED, EGG_UNTINTED, new Item.Properties()));
    public static final DeferredItem<Item> WILLOW_HOUND_SPAWN_EGG = ITEMS.register("willow_hound_spawn_egg",
            () -> new DeferredSpawnEggItem(EntityRegistry.WILLOW_HOUND, EGG_UNTINTED, EGG_UNTINTED, new Item.Properties()));

    // ---- The Spirit Realm ----

    /** Shorn from a glimmerfawn. The realm's only renewable thing, and its only export. */
    public static final DeferredItem<Item> SPIRIT_DOWN = ITEMS.register("spirit_down",
            () -> new Item(new Item.Properties()));

    // ---- Glimmerweave: what the down becomes when it is spun instead of baled ----
    // Thread from down, cloth from thread, a robe from cloth. The material carries the numbers,
    // GlimmerweaveSet carries the magic, and WWGeoArmorItem draws the robes in 3D.

    /** Spun from spirit down. */
    public static final DeferredItem<Item> GLIMMER_THREAD = ITEMS.register("glimmer_thread",
            () -> new Item(new Item.Properties()));
    /** A bolt of cloth woven from glimmer thread; also what mends the set on an anvil. */
    public static final DeferredItem<Item> GLIMMERWEAVE = ITEMS.register("glimmerweave",
            () -> new Item(new Item.Properties()));

    // Durability multiplier 18: chain and iron are 15.
    public static final DeferredItem<Item> GLIMMERWEAVE_HOOD = ITEMS.register("glimmerweave_hood",
            () -> new WWGeoArmorItem(ArmorMaterialRegistry.GLIMMERWEAVE, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(18)), "glimmerweave", false));
    public static final DeferredItem<Item> GLIMMERWEAVE_ROBE = ITEMS.register("glimmerweave_robe",
            () -> new WWGeoArmorItem(ArmorMaterialRegistry.GLIMMERWEAVE, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(18)), "glimmerweave", false));
    public static final DeferredItem<Item> GLIMMERWEAVE_LEGGINGS = ITEMS.register("glimmerweave_leggings",
            () -> new WWGeoArmorItem(ArmorMaterialRegistry.GLIMMERWEAVE, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(18)), "glimmerweave", false));
    public static final DeferredItem<Item> GLIMMERWEAVE_SLIPPERS = ITEMS.register("glimmerweave_slippers",
            () -> new WWGeoArmorItem(ArmorMaterialRegistry.GLIMMERWEAVE, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(18)), "glimmerweave", false));

    public static final DeferredItem<Item> GLIMMERFAWN_SPAWN_EGG = ITEMS.register("glimmerfawn_spawn_egg",
            () -> new DeferredSpawnEggItem(EntityRegistry.GLIMMERFAWN, EGG_UNTINTED, EGG_UNTINTED, new Item.Properties()));

    // ---- Developer kit -----------------------------------------------------
    // Creative-tab only (MainRegistry.DEV_TAB), no recipes, and every tool checks
    // DevToolItem.gate before acting: creative mode or a listed developer. DevKit.isDevOnly
    // keeps them out of the player-facing tab.

    public static final DeferredItem<Item> CREATIVE_FLUXIA_TABLET = ITEMS.register("creative_fluxia_tablet",
            () -> new CreativeFluxiaTabletItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> FLUXIA_PROBE = ITEMS.register("fluxia_probe",
            () -> new FluxiaProbeItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> RITE_PRIMER = ITEMS.register("rite_primer",
            () -> new RitePrimerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> RESONANCE_TUNER = ITEMS.register("resonance_tuner",
            () -> new ResonanceTunerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> HOURGLASS = ITEMS.register("hourglass",
            () -> new HourglassItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> SPIRIT_KEY = ITEMS.register("spirit_key",
            () -> new SpiritKeyItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static void register(IEventBus eventBus) {
        for (DeferredHolder<Block, ? extends Block> entry : BlockRegistry.BLOCKS.getEntries()) {
            if (BlockRegistry.isItemless(entry)
                    // These have their own items, registered above.
                    || entry == BlockRegistry.WILLOW_TORCH
                    || entry == BlockRegistry.WHISPERREED || entry == BlockRegistry.MOONLILY)
                continue;
            ITEMS.registerSimpleBlockItem(entry);
        } // Auto Add Blocks
        ITEMS.register(eventBus);

    }
}
