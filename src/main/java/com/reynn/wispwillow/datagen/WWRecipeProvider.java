package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.alchemy.AlchemyDefinitions;
import com.reynn.wispwillow.blocks.RuneType;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import com.reynn.wispwillow.entity.familiar.SoulPacts;
import com.reynn.wispwillow.gateway.GatewayRecipeBuilder;
import com.reynn.wispwillow.infusion.InfusionDefinitions;
import com.reynn.wispwillow.infusion.InfusionRecipeBuilder;
import com.reynn.wispwillow.ritual.RitualDefinitions;
import com.reynn.wispwillow.ritual.RitualRecipe;
import com.reynn.wispwillow.ritual.RitualRecipeBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Map.entry;

public class WWRecipeProvider extends RecipeProvider {

    public WWRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    private static String pathOf(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    /**
     * Namespaces a custom recipe id.
     *
     * <p>The {@code save(output, String)} overload runs the string through
     * {@link ResourceLocation#parse}, which defaults the namespace to {@code minecraft} —
     * so every custom-named recipe here used to be written into the vanilla namespace.</p>
     */
    private static ResourceLocation ww(String path) {
        return ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, path);
    }

    /** 9x {@code small} arranged in a square becomes one {@code large}. */
    private ShapedRecipeBuilder x9material(ItemLike small, ItemLike large) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, large)
                .pattern("nnn")
                .pattern("nnn")
                .pattern("nnn")
                .define('n', small)
                .unlockedBy("has_" + pathOf(small), has(small));
    }

    /** One {@code large} unpacks into nine {@code small}. */
    private ShapelessRecipeBuilder unpack9(ItemLike large, ItemLike small) {
        return ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, new ItemStack(small.asItem(), 9))
                .requires(large)
                .unlockedBy("has_" + pathOf(large), has(large));
    }

    private void raw_to_ingot(RecipeOutput out, ItemLike raw, ItemLike ingot, float experience) {
        String rawName = BuiltInRegistries.ITEM.getKey(raw.asItem()).getPath();
        String ingotName = BuiltInRegistries.ITEM.getKey(ingot.asItem()).getPath();
        SimpleCookingRecipeBuilder.smelting(
                        Ingredient.of(raw),
                        RecipeCategory.MISC,
                        ingot,
                        experience,
                        200
                )
                .unlockedBy("has_"+rawName, has(raw)).save(out, ww("smelt_"+rawName+"_to_"+ingotName));
        SimpleCookingRecipeBuilder.blasting(
                        Ingredient.of(raw),
                        RecipeCategory.MISC,
                        ingot,
                        experience,
                        100
                )
                .unlockedBy("has_"+rawName, has(raw)).save(out, ww("blast_"+rawName+"_to_"+ingotName));
    }

    @Override
    protected void buildRecipes(RecipeOutput out) {
        ItemStack waterPotion = new ItemStack(Items.POTION);
        waterPotion.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Potions.WATER));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.CHALK.get(DyeColor.WHITE))
                .pattern("   ")
                .pattern(" x ")
                .pattern("ooo")
                .define('x', Items.WHITE_CONCRETE_POWDER)
                .define('o', DataComponentIngredient.of(true, waterPotion))
                .unlockedBy("chalk_powder", has(Items.WHITE_CONCRETE_POWDER))
                .save(out);

        ItemRegistry.CHALK.forEach((dye, item) -> {
            if (dye != DyeColor.WHITE) {
                ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, item)
                        .requires(DyeItem.byColor(dye).asItem(), 1)
                        .requires(ItemRegistry.CHALK.get(DyeColor.WHITE), 1)
                        .unlockedBy("chalk", has(ItemRegistry.CHALK.get(DyeColor.WHITE)))
                        .unlockedBy("chalk_dye_"+dye.getName(), has(DyeItem.byColor(dye).asItem()))
                        .save(out);
            }
        });

        x9material(ItemRegistry.ASTRALITE_INGOT, BlockRegistry.ASTRALITE_BLOCK).save(out, ww("astralite_ingot_to_block"));
        x9material(ItemRegistry.ASTRALITE_NUGGET, ItemRegistry.ASTRALITE_INGOT).save(out, ww("astralite_nugget_to_ingot"));
        // This was 9 ingots -> 1 nugget, i.e. an 81x loss. Nuggets unpack from an ingot.
        unpack9(ItemRegistry.ASTRALITE_INGOT, ItemRegistry.ASTRALITE_NUGGET).save(out, ww("astralite_ingot_to_nugget"));
        raw_to_ingot(out, ItemRegistry.RAW_ASTRALITE.get(), ItemRegistry.ASTRALITE_INGOT.get(), 0.4f);
        raw_to_ingot(out, BlockRegistry.DEEPSLATE_ASTRALITE_ORE.get(), ItemRegistry.ASTRALITE_INGOT.get(), 0.4f);
        raw_to_ingot(out, BlockRegistry.ASTRALITE_ORE.get(), ItemRegistry.ASTRALITE_INGOT.get(), 0.4f);

        unpack9(BlockRegistry.ASTRALITE_BLOCK, ItemRegistry.ASTRALITE_INGOT).save(out);

        x9material(ItemRegistry.AMETHYST_COAL, BlockRegistry.AMETHYST_COAL_BLOCK).save(out);
        unpack9(BlockRegistry.AMETHYST_COAL_BLOCK, ItemRegistry.AMETHYST_COAL).save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.CONDENSED_DUST)
                .pattern("xox")
                .pattern("oxo")
                .pattern("xox")
                .define('o', ItemRegistry.DUSTCONVERT)
                .define('x', Items.EMERALD)
                // ...was unlocked by the item it produces, so it never showed up.
                .unlockedBy("has_dust_of_convert", has(ItemRegistry.DUSTCONVERT)).save(out);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, new ItemStack(ItemRegistry.DUSTCONVERT.get(), 8))
                .requires(Items.DIAMOND)
                .requires(Items.LAPIS_LAZULI)
                .requires(Items.REDSTONE)
                .requires(Items.REDSTONE)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, new ItemStack(BlockRegistry.WILLOW_TORCH, 4))
                .pattern("   ")
                .pattern(" x ")
                .pattern(" i ")
                .define('i', Items.STICK)
                .define('x', ItemRegistry.AMETHYST_COAL)
                .unlockedBy("has_amethyst_coal", has(ItemRegistry.AMETHYST_COAL))
                .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.ALTAR)
                .pattern(" o ")
                .pattern("oxo")
                .pattern("xxx")
                .define('x', BlockRegistry.WILLOW_PLANKS)
                .define('o', Items.AMETHYST_SHARD)
                .unlockedBy("has_willow_planks", has(BlockRegistry.WILLOW_PLANKS)).save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.CHUNK_LOADER)
                .pattern("ada")
                .pattern("asa")
                .pattern("aoa")
                .define('s', Items.DIAMOND)
                .define('d', Items.NETHER_STAR)
                .define('o', Blocks.CRYING_OBSIDIAN)
                .define('a', Items.AMETHYST_BLOCK)
                .unlockedBy("has_amethyst_block", has(Blocks.AMETHYST_BLOCK)).save(out);


//        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.FLUXIA_LAMP)
//                .pattern("xax")
//                .pattern("glg")
//                .pattern("xxx")
//                .define('x', BlockRegistry.WILLOW_PLANKS)
//                .define('a', Items.AMETHYST_SHARD)
//                .define('g', Items.GLASS)
//                .define('l', Items.GLOWSTONE_DUST)
//                .unlockedBy("has_willow_planks", has(BlockRegistry.WILLOW_PLANKS)).save(out);

        Map<ItemLike, ItemLike> itemConvert = Map.ofEntries(
                entry(Items.CHARCOAL, Items.COAL),
                entry(Items.COAL, Items.CHARCOAL)
        );

        itemConvert.forEach((item1, item2) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item2)
                    .requires(ItemRegistry.DUSTCONVERT)
                    .requires(item1)
                    .unlockedBy("has_dust_of_convert", has(ItemRegistry.DUSTCONVERT))
                    .save(out, ww(String.format("dust_convert_%s_to_%s",
                            BuiltInRegistries.ITEM.getKey(item1.asItem()).getPath(),
                            BuiltInRegistries.ITEM.getKey(item2.asItem()).getPath())));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item2)
                    .requires(ItemRegistry.CONDENSED_DUST)
                    .requires(item1)
                    .unlockedBy("has_dust_of_convert", has(ItemRegistry.DUSTCONVERT))
                    .save(out, ww(String.format("condensed_dust_convert_%s_to_%s",
                            BuiltInRegistries.ITEM.getKey(item1.asItem()).getPath(),
                            BuiltInRegistries.ITEM.getKey(item2.asItem()).getPath())));
        });

        buildFenRecipes(out);

        //WILLOW WOOD SET
        planksFromLog(out, BlockRegistry.WILLOW_PLANKS, ItemRegistry.WILLOW_LOGS_TAG, 4);

        fenceBuilder(BlockRegistry.WILLOW_FENCE, Ingredient.of(BlockRegistry.WILLOW_PLANKS.get()))
                .unlockedBy("has_willow_planks", has(BlockRegistry.WILLOW_PLANKS)).save(out);
        fenceGateBuilder(BlockRegistry.WILLOW_FENCE_GATE, Ingredient.of(BlockRegistry.WILLOW_PLANKS.get()))
                .unlockedBy("has_willow_planks", has(BlockRegistry.WILLOW_PLANKS)).save(out);
        doorBuilder(BlockRegistry.WILLOW_DOOR, Ingredient.of(BlockRegistry.WILLOW_PLANKS.get()))
                .unlockedBy("has_willow_planks", has(BlockRegistry.WILLOW_PLANKS)).save(out);
        trapdoorBuilder(BlockRegistry.WILLOW_TRAPDOOR, Ingredient.of(BlockRegistry.WILLOW_PLANKS.get()))
                .unlockedBy("has_willow_planks", has(BlockRegistry.WILLOW_PLANKS)).save(out);
        buttonBuilder(BlockRegistry.WILLOW_BUTTON, Ingredient.of(BlockRegistry.WILLOW_PLANKS.get()))
                .unlockedBy("has_willow_planks", has(BlockRegistry.WILLOW_PLANKS)).save(out);
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.WILLOW_SLAB, Ingredient.of(BlockRegistry.WILLOW_PLANKS.get()))
                .unlockedBy("has_willow_planks", has(BlockRegistry.WILLOW_PLANKS)).save(out);
        stairBuilder(BlockRegistry.WILLOW_STAIRS, Ingredient.of(BlockRegistry.WILLOW_PLANKS.get()))
                .unlockedBy("has_willow_planks", has(BlockRegistry.WILLOW_PLANKS)).save(out);
        pressurePlateBuilder(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.WILLOW_PRESSURE_PLATE, Ingredient.of(BlockRegistry.WILLOW_PLANKS.get()))
                .unlockedBy("has_willow_planks", has(BlockRegistry.WILLOW_PLANKS)).save(out);

        //SOULWOOD SET
        // Unlocked by the planks throughout rather than by the log, so the whole set appears at
        // once the first time someone brings timber back out of the Spirit Realm.
        planksFromLog(out, BlockRegistry.SOULWOOD_PLANKS, ItemRegistry.SOULWOOD_LOGS_TAG, 4);

        fenceBuilder(BlockRegistry.SOULWOOD_FENCE, Ingredient.of(BlockRegistry.SOULWOOD_PLANKS.get()))
                .unlockedBy("has_soulwood_planks", has(BlockRegistry.SOULWOOD_PLANKS)).save(out);
        fenceGateBuilder(BlockRegistry.SOULWOOD_FENCE_GATE, Ingredient.of(BlockRegistry.SOULWOOD_PLANKS.get()))
                .unlockedBy("has_soulwood_planks", has(BlockRegistry.SOULWOOD_PLANKS)).save(out);
        doorBuilder(BlockRegistry.SOULWOOD_DOOR, Ingredient.of(BlockRegistry.SOULWOOD_PLANKS.get()))
                .unlockedBy("has_soulwood_planks", has(BlockRegistry.SOULWOOD_PLANKS)).save(out);
        trapdoorBuilder(BlockRegistry.SOULWOOD_TRAPDOOR, Ingredient.of(BlockRegistry.SOULWOOD_PLANKS.get()))
                .unlockedBy("has_soulwood_planks", has(BlockRegistry.SOULWOOD_PLANKS)).save(out);
        buttonBuilder(BlockRegistry.SOULWOOD_BUTTON, Ingredient.of(BlockRegistry.SOULWOOD_PLANKS.get()))
                .unlockedBy("has_soulwood_planks", has(BlockRegistry.SOULWOOD_PLANKS)).save(out);
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.SOULWOOD_SLAB, Ingredient.of(BlockRegistry.SOULWOOD_PLANKS.get()))
                .unlockedBy("has_soulwood_planks", has(BlockRegistry.SOULWOOD_PLANKS)).save(out);
        stairBuilder(BlockRegistry.SOULWOOD_STAIRS, Ingredient.of(BlockRegistry.SOULWOOD_PLANKS.get()))
                .unlockedBy("has_soulwood_planks", has(BlockRegistry.SOULWOOD_PLANKS)).save(out);
        pressurePlateBuilder(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.SOULWOOD_PRESSURE_PLATE, Ingredient.of(BlockRegistry.SOULWOOD_PLANKS.get()))
                .unlockedBy("has_soulwood_planks", has(BlockRegistry.SOULWOOD_PLANKS)).save(out);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, new ItemStack(BlockRegistry.SNAD, 8))
                .requires(Ingredient.of(ItemTags.SAND), 4)
                .requires(Items.BONE_MEAL)
                .requires(Items.AMETHYST_SHARD)
                .unlockedBy("has_shard", has(Items.AMETHYST_SHARD))
                .save(out);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, new ItemStack(BlockRegistry.AMETHYST_SUGAR_CANE, 3))
                .requires(Items.SUGAR_CANE, 3)
                .requires(Items.AMETHYST_SHARD, 2)
                .unlockedBy("has_shard", has(Items.AMETHYST_SHARD))
                .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.WILLOW_LANTERN)
                .pattern("nnn")
                .pattern("non")
                .pattern("nnn")
                .define('n', Items.IRON_NUGGET)
                .define('o', ItemRegistry.WILLOW_TORCH)
                .unlockedBy("has_willow_torch", has(ItemRegistry.WILLOW_TORCH)).save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.FLUX_LINKING_WAND)
                .pattern("nnn")
                .pattern("nxn")
                .pattern("  n")
                .define('n', Items.STICK)
                .define('x', Items.AMETHYST_SHARD)
                .unlockedBy("has_shard", has(Items.AMETHYST_SHARD)).save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.MANA_POOL.get())
                .pattern("   ")
                .pattern("xox")
                .pattern("xxx")
                .define('x', BlockRegistry.FLUXITE)
                .define('o', Items.AMETHYST_SHARD)
                .unlockedBy("has_shard", has(Items.AMETHYST_SHARD)).save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ItemRegistry.FLUXIA_TABLET.get())
                .pattern("xxx")
                .pattern("xox")
                .pattern("xnx")
                .define('x', BlockRegistry.FLUXITE)
                .define('o', ItemRegistry.ASTRALITE_NUGGET)
                .define('n', Items.NETHERITE_INGOT)
                .unlockedBy("has_shard", has(Items.AMETHYST_SHARD)).save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.FLUXIA_SPITTER.get())
                .pattern("xxx")
                .pattern("xot")
                .pattern("xxx")
                .define('x', BlockRegistry.FLUXITE)
                .define('o', Items.AMETHYST_SHARD)
                .define('t', ItemRegistry.FLUXIA_TABLET)
                .unlockedBy("has_shard", has(Items.AMETHYST_SHARD)).save(out);

        // The Fluxia Lamp block was registered but had no recipe, so it was
        // creative-only in every released build.
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.FLUXIA_LAMP.get())
                .pattern("xax")
                .pattern("glg")
                .pattern("xxx")
                .define('x', BlockRegistry.WILLOW_PLANKS)
                .define('a', Items.AMETHYST_SHARD)
                .define('g', Items.GLASS)
                .define('l', Items.GLOWSTONE_DUST)
                .unlockedBy("has_willow_planks", has(BlockRegistry.WILLOW_PLANKS)).save(out);

        // Arcane Goggles: astralite frame, amethyst lenses, glass to see through.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.ARCANE_GOGGLES)
                .pattern("nan")
                .pattern("gog")
                .pattern("   ")
                .define('n', ItemRegistry.ASTRALITE_NUGGET)
                .define('a', ItemRegistry.ASTRALITE_INGOT)
                .define('g', Items.GLASS)
                .define('o', Items.AMETHYST_SHARD)
                .unlockedBy("has_astralite", has(ItemRegistry.ASTRALITE_INGOT)).save(out);

        // Soul Wisp: a phantom membrane bound with amethyst and an echo of the End.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.SOUL_WISP, 2)
                .pattern(" a ")
                .pattern("ape")
                .pattern(" a ")
                .define('a', Items.AMETHYST_SHARD)
                .define('p', Items.PHANTOM_MEMBRANE)
                .define('e', Items.ENDER_PEARL)
                .unlockedBy("has_membrane", has(Items.PHANTOM_MEMBRANE)).save(out);

        // Amethyst Paper: pressed like ordinary paper, from the mod's own cane.
        // The Soul Pact itself is no longer craftable -- it is the "soul_pact" rite, so the
        // familiar chain starts at a crop and an altar rather than a crafting table.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.AMETHYST_PAPER, 3)
                .pattern("   ")
                .pattern("ccc")
                .pattern("   ")
                .define('c', BlockRegistry.AMETHYST_SUGAR_CANE)
                .unlockedBy("has_amethyst_sugar_cane", has(BlockRegistry.AMETHYST_SUGAR_CANE)).save(out);

        // Fluxia Dynamo: a furnace that pays out in current instead of heat.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.FLUXIA_DYNAMO)
                .pattern("fff")
                .pattern("cnc")
                .pattern("faf")
                .define('f', BlockRegistry.FLUXITE)
                .define('c', ItemRegistry.AMETHYST_COAL)
                .define('n', Items.FURNACE)
                .define('a', ItemRegistry.ASTRALITE_INGOT)
                .unlockedBy("has_amethyst_coal", has(ItemRegistry.AMETHYST_COAL)).save(out);

        // Astral Font: a basin of astralite, open to the sky.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.ASTRAL_FONT)
                .pattern("a a")
                .pattern("aga")
                .pattern("fff")
                .define('a', ItemRegistry.ASTRALITE_INGOT)
                .define('g', Items.GLASS)
                .define('f', BlockRegistry.FLUXITE)
                .unlockedBy("has_astralite", has(ItemRegistry.ASTRALITE_INGOT)).save(out);

        // Gateway Core: the keystone of the Soul Gateway. Deliberately the most expensive
        // single crafting recipe in the mod — everything downstream of it is permanent.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.GATEWAY_CORE)
                .pattern("awa")
                .pattern("wsw")
                .pattern("ara")
                .define('a', ItemRegistry.ASTRALITE_INGOT)
                .define('w', BlockRegistry.WILLOW_LOG)
                .define('s', Items.NETHER_STAR)
                .define('r', BlockRegistry.RUNES.get(RuneType.BLOOD))
                .unlockedBy("has_nether_star", has(Items.NETHER_STAR)).save(out);

        // Alchemical Cauldron: astralite for the rim, fluxite for the body. It sits after
        // the dynamo and the pool, which is the whole gate on the brews it unlocks.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.ALCHEMICAL_CAULDRON)
                .pattern("a a")
                .pattern("f f")
                .pattern("faf")
                .define('a', ItemRegistry.ASTRALITE_INGOT)
                .define('f', BlockRegistry.FLUXITE)
                .unlockedBy("has_astralite_ingot", has(ItemRegistry.ASTRALITE_INGOT)).save(out);

        // ---- Empyrite -----------------------------------------------------
        // Packing and unpacking only. There is no crafting route to the ingot itself; the
        // cauldron is the only source, which is the point of the tier.
        x9material(ItemRegistry.EMPYRITE_INGOT, BlockRegistry.EMPYRITE_BLOCK).save(out, ww("empyrite_ingot_to_block"));
        unpack9(BlockRegistry.EMPYRITE_BLOCK, ItemRegistry.EMPYRITE_INGOT).save(out, ww("empyrite_block_to_ingot"));
        x9material(ItemRegistry.EMPYRITE_NUGGET, ItemRegistry.EMPYRITE_INGOT).save(out, ww("empyrite_nugget_to_ingot"));
        unpack9(ItemRegistry.EMPYRITE_INGOT, ItemRegistry.EMPYRITE_NUGGET).save(out, ww("empyrite_ingot_to_nugget"));

        empyriteTool(out, ItemRegistry.EMPYRITE_SWORD, "eee", " i ", " i ", 2);
        empyriteTool(out, ItemRegistry.EMPYRITE_PICKAXE, "eee", " i ", " i ", 3);
        empyriteTool(out, ItemRegistry.EMPYRITE_AXE, "ee ", "ei ", " i ", 3);
        empyriteTool(out, ItemRegistry.EMPYRITE_SHOVEL, " e ", " i ", " i ", 1);
        empyriteTool(out, ItemRegistry.EMPYRITE_HOE, "ee ", " i ", " i ", 2);

        empyriteArmour(out, ItemRegistry.EMPYRITE_HELMET, "eee", "e e", "   ");
        empyriteArmour(out, ItemRegistry.EMPYRITE_CHESTPLATE, "e e", "eee", "eee");
        empyriteArmour(out, ItemRegistry.EMPYRITE_LEGGINGS, "eee", "e e", "e e");
        empyriteArmour(out, ItemRegistry.EMPYRITE_BOOTS, "   ", "e e", "e e");

        buildRituals(out);
        buildInfusions(out);
        buildGateways(out);
        AlchemyDefinitions.build(out, WWRecipeProvider::has);
    }

    /**
     * A tool from empyrite plate and astralite hafts.
     *
     * <p>Astralite for the handle rather than a plain stick: the tier is meant to be
     * expensive from end to end, and a netherite-plus tool held together with a twig would
     * undercut that in the one place the player looks at it.</p>
     */
    /** The Willow Fen: its stone, its amber and sap, mud from bog water, and flower dyes. */
    private void buildFenRecipes(RecipeOutput out) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.FENSTONE, 8)
                .pattern("ccc")
                .pattern("cpc")
                .pattern("ccc")
                .define('c', Items.COBBLESTONE)
                .define('p', BlockRegistry.PEAT)
                .unlockedBy("has_peat", has(BlockRegistry.PEAT))
                .save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.FENSTONE_BRICKS, 4)
                .pattern("ss")
                .pattern("ss")
                .define('s', BlockRegistry.FENSTONE)
                .unlockedBy("has_fenstone", has(BlockRegistry.FENSTONE))
                .save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.MOSSY_FENSTONE_BRICKS)
                .requires(BlockRegistry.FENSTONE_BRICKS)
                .requires(Ingredient.of(Items.VINE, Items.MOSS_BLOCK, BlockRegistry.FEN_MOSS.get()))
                .unlockedBy("has_fenstone_bricks", has(BlockRegistry.FENSTONE_BRICKS))
                .save(out);
        stairBuilder(BlockRegistry.FENSTONE_BRICK_STAIRS, Ingredient.of(BlockRegistry.FENSTONE_BRICKS))
                .unlockedBy("has_fenstone_bricks", has(BlockRegistry.FENSTONE_BRICKS))
                .save(out);
        slab(out, RecipeCategory.BUILDING_BLOCKS, BlockRegistry.FENSTONE_BRICK_SLAB, BlockRegistry.FENSTONE_BRICKS);
        wall(out, RecipeCategory.DECORATIONS, BlockRegistry.FENSTONE_BRICK_WALL, BlockRegistry.FENSTONE_BRICKS);

        fenstoneCut(out, BlockRegistry.FENSTONE, BlockRegistry.FENSTONE_BRICKS, 1);
        for (ItemLike from : new ItemLike[]{BlockRegistry.FENSTONE, BlockRegistry.FENSTONE_BRICKS}) {
            fenstoneCut(out, from, BlockRegistry.FENSTONE_BRICK_STAIRS, 1);
            fenstoneCut(out, from, BlockRegistry.FENSTONE_BRICK_SLAB, 2);
            fenstoneCut(out, from, BlockRegistry.FENSTONE_BRICK_WALL, 1);
        }

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.MOSSY_FENSTONE)
                .requires(BlockRegistry.FENSTONE)
                .requires(Ingredient.of(Items.VINE, Items.MOSS_BLOCK, BlockRegistry.FEN_MOSS.get()))
                .unlockedBy("has_fenstone", has(BlockRegistry.FENSTONE))
                .save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.FENSTONE_PILLAR, 2)
                .pattern("s")
                .pattern("s")
                .define('s', BlockRegistry.FENSTONE)
                .unlockedBy("has_fenstone", has(BlockRegistry.FENSTONE))
                .save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.PEAT_BRICKS, 4)
                .pattern("pp")
                .pattern("pp")
                .define('p', BlockRegistry.PEAT)
                .unlockedBy("has_peat", has(BlockRegistry.PEAT))
                .save(out);
        fenstoneCut(out, BlockRegistry.FENSTONE, BlockRegistry.FENSTONE_PILLAR, 1);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlockRegistry.WILLOW_AMBER)
                .pattern("ss")
                .pattern("ss")
                .define('s', ItemRegistry.WILLOW_SAP_BOTTLE)
                .unlockedBy("has_willow_sap", has(ItemRegistry.WILLOW_SAP_BOTTLE))
                .save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemRegistry.WILLOW_SAP_BUCKET.get())
                .requires(Items.BUCKET)
                .requires(ItemRegistry.WILLOW_SAP_BOTTLE, 3)
                .unlockedBy("has_willow_sap", has(ItemRegistry.WILLOW_SAP_BOTTLE))
                .save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, new ItemStack(Blocks.MUD, 8))
                .requires(ItemRegistry.BOG_WATER_BUCKET.get())
                .requires(Items.DIRT, 8)
                .unlockedBy("has_bog_water", has(ItemRegistry.BOG_WATER_BUCKET.get()))
                .save(out, ww("mud_from_bog_water"));

        flowerDye(out, BlockRegistry.BOGBELL, Items.BLUE_DYE);
        flowerDye(out, BlockRegistry.DUSKPETAL, Items.PURPLE_DYE);
        flowerDye(out, BlockRegistry.MOONLILY, Items.WHITE_DYE);

        // Spirit down is fleece by another name, so it bales into wool. The only way to bring
        // anything back out of the Spirit Realm, and it is renewable: shear, do not kill.
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, Items.WHITE_WOOL)
                .pattern("dd")
                .pattern("dd")
                .define('d', ItemRegistry.SPIRIT_DOWN)
                .unlockedBy("has_spirit_down", has(ItemRegistry.SPIRIT_DOWN))
                .save(out, ww("white_wool_from_spirit_down"));

        // Spun instead of baled: three down twist into two threads, four threads weave a bolt,
        // and the bolt takes the vanilla armour patterns. A full set is 24 bolts: 144 down.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.GLIMMER_THREAD.get(), 2)
                .pattern("d").pattern("d").pattern("d")
                .define('d', ItemRegistry.SPIRIT_DOWN)
                .unlockedBy("has_spirit_down", has(ItemRegistry.SPIRIT_DOWN))
                .save(out, ww("glimmer_thread"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.GLIMMERWEAVE.get())
                .pattern("tt").pattern("tt")
                .define('t', ItemRegistry.GLIMMER_THREAD)
                .unlockedBy("has_glimmer_thread", has(ItemRegistry.GLIMMER_THREAD))
                .save(out, ww("glimmerweave"));
        glimmerweaveArmour(out, ItemRegistry.GLIMMERWEAVE_HOOD, "ggg", "g g", "   ");
        glimmerweaveArmour(out, ItemRegistry.GLIMMERWEAVE_ROBE, "g g", "ggg", "ggg");
        glimmerweaveArmour(out, ItemRegistry.GLIMMERWEAVE_LEGGINGS, "ggg", "g g", "g g");
        glimmerweaveArmour(out, ItemRegistry.GLIMMERWEAVE_SLIPPERS, "   ", "g g", "g g");
    }

    /** Stonecutting, saved under the mod's namespace (vanilla's helper would write to minecraft:). */
    private void fenstoneCut(RecipeOutput out, ItemLike from, ItemLike to, int count) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(from), RecipeCategory.BUILDING_BLOCKS, to, count)
                .unlockedBy("has_" + pathOf(from), has(from))
                .save(out, ww(pathOf(to) + "_from_" + pathOf(from) + "_stonecutting"));
    }

    private void flowerDye(RecipeOutput out, ItemLike flower, ItemLike dye) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, dye)
                .requires(flower)
                .unlockedBy("has_" + pathOf(flower), has(flower))
                .save(out, ww(pathOf(dye) + "_from_" + pathOf(flower)));
    }

    private void empyriteTool(RecipeOutput out, ItemLike result,
                              String top, String middle, String bottom, int plate) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern(top).pattern(middle).pattern(bottom)
                .define('e', ItemRegistry.EMPYRITE_INGOT)
                .define('i', ItemRegistry.ASTRALITE_INGOT)
                .unlockedBy("has_empyrite", has(ItemRegistry.EMPYRITE_INGOT))
                .save(out, ww(pathOf(result)));
    }

    private void glimmerweaveArmour(RecipeOutput out, ItemLike result,
                                    String top, String middle, String bottom) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern(top).pattern(middle).pattern(bottom)
                .define('g', ItemRegistry.GLIMMERWEAVE)
                .unlockedBy("has_glimmerweave", has(ItemRegistry.GLIMMERWEAVE))
                .save(out, ww(pathOf(result)));
    }

    private void empyriteArmour(RecipeOutput out, ItemLike result,
                                String top, String middle, String bottom) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern(top).pattern(middle).pattern(bottom)
                .define('e', ItemRegistry.EMPYRITE_INGOT)
                .unlockedBy("has_empyrite", has(ItemRegistry.EMPYRITE_INGOT))
                .save(out, ww(pathOf(result)));
    }

    /**
     * Emits one {@code wispwillow:infusion} recipe per entry in
     * {@link InfusionDefinitions}, the same way rituals are emitted.
     */
    /**
     * Emits one {@code wispwillow:gateway} recipe per bond in {@link SoulPacts}: the sealed
     * pact of that creature, cast into an open gateway, becomes that familiar.
     */
    private void buildGateways(RecipeOutput out) {
        for (var bond : SoulPacts.bonds().entrySet()) {
            GatewayRecipeBuilder b = new GatewayRecipeBuilder(bond.getKey(), bond.getValue().get());
            b.unlockedBy("has_sealed_pact", has(ItemRegistry.SEALED_SOUL_PACT.get()));
            b.save(out, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "gateway/" + bond.getKey().getPath()));
        }
    }

    private void buildInfusions(RecipeOutput out) {
        for (InfusionDefinitions.Def def : InfusionDefinitions.all()) {
            InfusionRecipeBuilder b = new InfusionRecipeBuilder(def.result());
            ItemLike unlock = null;
            for (ItemStack in : def.inputs()) {
                // A stack of N in the table means N separate single-item ingredients.
                Ingredient ing = Ingredient.of(in.getItem());
                for (int i = 0; i < in.getCount(); i++) b.input(ing);
                if (unlock == null) unlock = in.getItem();
            }
            b.fluxia(def.fluxia());
            ItemLike finalUnlock = unlock == null ? Items.AMETHYST_SHARD : unlock;
            b.unlockedBy("has_" + pathOf(finalUnlock), has(finalUnlock))
                    .save(out, def.id());
        }
    }

    /**
     * Emits one {@code wispwillow:ritual} recipe per entry in {@link RitualDefinitions}.
     * The grimoire is generated from the same table, so the two cannot disagree.
     */
    private void buildRituals(RecipeOutput out) {
        for (RitualDefinitions.Def def : RitualDefinitions.all()) {
            RitualRecipeBuilder rb = new RitualRecipeBuilder(def.grid(), def.icon().copy());

            for (RitualRecipe.Cell c : def.pattern()) {
                rb.cell(c.dx(), c.dz(), c.type().orElse(null), c.color().orElse(null), c.required());
            }

            // A reagent with count N is N separate single-item ingredients: the altar
            // consumes one item per ingredient from the drops lying in the circle.
            for (RitualDefinitions.Reagent r : def.reagents()) {
                Ingredient ing = Ingredient.of(r.stack().getItem());
                for (int i = 0; i < r.count(); i++) rb.input(ing);
            }

            def.resultItem().ifPresent(stack -> rb.resultItem(stack.copy()));
            def.resultMob().ifPresent(id -> rb.resultMob(id, def.resultMobCount()));

            if (def.fluxia() > 0) rb.fluxiaRequired(def.fluxia());
            rb.minTier(def.minTier());
            def.vigil().ifPresent(rb::vigil);
            def.awakens().ifPresent(rb::awakens);
            def.imbues().ifPresent(rb::imbues);

            rb.unlockedBy("has_" + BuiltInRegistries.ITEM.getKey(def.unlockItem().asItem()).getPath(),
                            has(def.unlockItem()))
                    .save(out, def.id());
        }
    }
}
