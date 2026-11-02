package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.FluidRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class WWItemModelProvider extends ItemModelProvider {
    public WWItemModelProvider(PackOutput output, ExistingFileHelper efh) {
        super(output, WispWillow.MODID, efh);
    }

    /** A tool model: the flat sprite, held like a tool rather than like a slab. */
    private void handheld(net.minecraft.world.item.Item item) {
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        singleTexture(path, mcLoc("item/handheld"), "layer0", modLoc("item/" + path));
    }

    /**
     * An armour model that shows smithing trims in the inventory.
     *
     * <p>Worn trims need nothing from us — the armour layer builds its texture name from the
     * <em>trim</em> material, and every one of those sprites is vanilla. The inventory sprite
     * is the half that does not come free: it is driven by {@code trim_type} overrides on the
     * item model, and without them a trimmed piece looks bare in the slot while looking
     * trimmed on the body.</p>
     *
     * <p>The overlay sprites are vanilla too, but they are produced by a paletted-permutations
     * atlas source rather than shipped as files, so {@code ExistingFileHelper} cannot see them
     * and would reject the model. Declaring them generated is what says "this one really does
     * exist at runtime" rather than silencing a genuine missing-texture check.</p>
     *
     * <p>Driven off {@link ItemModelGenerators#GENERATED_TRIM_MODELS} so the material list and
     * the predicate thresholds stay whatever vanilla says they are. None of the darker variants
     * apply here: those exist so an iron trim stays visible on iron, and empyrite is not a
     * vanilla armour material, so every material resolves to its plain name.</p>
     */
    private void armorWithTrims(ArmorItem item) {
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        String slot = item.getType().getName();
        ItemModelBuilder base = basicItem(item);

        for (ItemModelGenerators.TrimModelData trim : ItemModelGenerators.GENERATED_TRIM_MODELS) {
            String material = trim.name(item.getMaterial());
            String trimmed = path + "_" + material + "_trim";
            ResourceLocation overlay =
                    ResourceLocation.withDefaultNamespace("trims/items/" + slot + "_trim_" + material);

            existingFileHelper.trackGenerated(overlay, TEXTURE);
            withExistingParent(trimmed, mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/" + path))
                    .texture("layer1", overlay);

            base.override()
                    .predicate(ItemModelGenerators.TRIM_TYPE_PREDICATE_ID, trim.itemModelIndex())
                    .model(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile(
                            modLoc("item/" + trimmed)))
                    .end();
        }
    }

    @Override
    protected void registerModels() {
        withExistingParent("sigil", modLoc("block/sigil_base"));
        withExistingParent("fluxia_spitter", modLoc("block/fluxia_spitter"));
        withExistingParent("fluxia_lamp", modLoc("block/fluxia_lamp"));

        singleTexture("fluxia_tablet", mcLoc("item/generated"), "layer0", modLoc("item/fluxia_tablet"));
        basicItem(ItemRegistry.GREATER_TABLET.get());
        basicItem(ItemRegistry.GRAND_TABLET.get());
        // ---- Developer kit ----
        basicItem(ItemRegistry.CREATIVE_FLUXIA_TABLET.get());
        handheld(ItemRegistry.FLUXIA_PROBE.get());
        basicItem(ItemRegistry.RITE_PRIMER.get());
        handheld(ItemRegistry.RESONANCE_TUNER.get());
        basicItem(ItemRegistry.HOURGLASS.get());
        basicItem(ItemRegistry.SPIRIT_KEY.get());
        simpleBlockItem(BlockRegistry.SNAD.get());
        simpleBlockItem(BlockRegistry.WILLOW_LEAVES.get());
        simpleBlockItem(BlockRegistry.FLOWERING_WILLOW_LEAVES.get());
        simpleBlockItem(BlockRegistry.WILLOW_LOG.get());

        basicItem(ItemRegistry.RAW_ASTRALITE.get());
        basicItem(ItemRegistry.ASTRALITE_INGOT.get());
        basicItem(ItemRegistry.ASTRALITE_NUGGET.get());
        simpleBlockItem(BlockRegistry.ASTRALITE_BLOCK.get());
        simpleBlockItem(BlockRegistry.ASTRALITE_ORE.get());
        simpleBlockItem(BlockRegistry.DEEPSLATE_ASTRALITE_ORE.get());
        // Rune item models come from the blockstate provider (they are blocks now).
        basicItem(ItemRegistry.ARCANE_GOGGLES.get());
        basicItem(ItemRegistry.SOUL_WISP.get());

        // PLACEHOLDER ART: vanilla item models stand in until the pact sprites exist, the
        // same trick the fluid disks use above. Real art goes at textures/item/<name>.png,
        // then each line becomes basicItem(...).
        //withExistingParent("amethyst_paper", mcLoc("item/paper"));
        basicItem(ItemRegistry.AMETHYST_PAPER.get());
        basicItem(ItemRegistry.SOUL_PACT.get());
        basicItem(ItemRegistry.PRIMED_SOUL_PACT.get());
        basicItem(ItemRegistry.BOUND_SOUL_PACT.get());
        basicItem(ItemRegistry.SEALED_SOUL_PACT.get());
        // ---- Spawn eggs ----
        // Every egg is its own creature, as vanilla's are in newer versions: one hand-drawn
        // sprite per egg (textures/item/<id>.png, from tools/gen_item_sprites.py). The eggs are
        // registered with white tints, so the sprite shows as drawn.
        basicItem(ItemRegistry.WISP_FAMILIAR_SPAWN_EGG.get());
        basicItem(ItemRegistry.BAT_FAMILIAR_SPAWN_EGG.get());
        basicItem(ItemRegistry.FROG_FAMILIAR_SPAWN_EGG.get());
        basicItem(ItemRegistry.AXOLOTL_FAMILAR_SPAWN_EGG.get());
        // ---- Empyrite ----
        basicItem(ItemRegistry.EMPYRITE_INGOT.get());
        basicItem(ItemRegistry.EMPYRITE_NUGGET.get());
        // Tools and armour hang off the handheld parent so they sit in the hand correctly;
        // basicItem would leave them flat, like a nugget held edge-on.
        handheld(ItemRegistry.EMPYRITE_SWORD.get());
        handheld(ItemRegistry.EMPYRITE_PICKAXE.get());
        handheld(ItemRegistry.EMPYRITE_AXE.get());
        handheld(ItemRegistry.EMPYRITE_SHOVEL.get());
        handheld(ItemRegistry.EMPYRITE_HOE.get());
        armorWithTrims((ArmorItem) ItemRegistry.EMPYRITE_HELMET.get());
        armorWithTrims((ArmorItem) ItemRegistry.EMPYRITE_CHESTPLATE.get());
        armorWithTrims((ArmorItem) ItemRegistry.EMPYRITE_LEGGINGS.get());
        armorWithTrims((ArmorItem) ItemRegistry.EMPYRITE_BOOTS.get());
        // ---- Glimmerweave ----
        basicItem(ItemRegistry.GLIMMER_THREAD.get());
        basicItem(ItemRegistry.GLIMMERWEAVE.get());
        armorWithTrims((ArmorItem) ItemRegistry.GLIMMERWEAVE_HOOD.get());
        armorWithTrims((ArmorItem) ItemRegistry.GLIMMERWEAVE_ROBE.get());
        armorWithTrims((ArmorItem) ItemRegistry.GLIMMERWEAVE_LEGGINGS.get());
        armorWithTrims((ArmorItem) ItemRegistry.GLIMMERWEAVE_SLIPPERS.get());

        basicItem(ItemRegistry.UPGRADE_HASTE.get());
        basicItem(ItemRegistry.UPGRADE_THRIFT.get());
        basicItem(ItemRegistry.UPGRADE_CAPACITY.get());

        basicItem(ItemRegistry.DUSTCONVERT.get());
        basicItem(ItemRegistry.CONDENSED_DUST.get());

        simpleBlockItem(BlockRegistry.CHUNK_LOADER.get());
        simpleBlockItem(BlockRegistry.WILLOW_PLANKS.get());
        simpleBlockItem(BlockRegistry.AMETHYST_COAL_BLOCK.get());
        simpleBlockItem(BlockRegistry.STRIPPED_WILLOW_LOG.get());
        saplingItem(BlockRegistry.WILLOW_SAPLING);
        basicItem(BlockRegistry.WILLOW_DOOR.get().asItem());
        // The flux linking wand is a GeckoLib model; its builtin/entity item model is hand-written in
        // src/main/resources and must not be emitted here, or the generated copy would shadow it.
        simpleBlockItem(BlockRegistry.FLUXITE.get());
        saplingItem(BlockRegistry.LUXINIAS_FLOWER);
        saplingItem(BlockRegistry.SOLROSA_FLOWER);
        saplingItem(BlockRegistry.WILLOW_VINE);
        fenceItem(BlockRegistry.WILLOW_FENCE, BlockRegistry.WILLOW_PLANKS);
        buttonItem(BlockRegistry.WILLOW_BUTTON, BlockRegistry.WILLOW_PLANKS);

        simpleBlockItem(BlockRegistry.SOULWOOD_PLANKS.get());
        simpleBlockItem(BlockRegistry.STRIPPED_SOULWOOD_LOG.get());
        fenceItem(BlockRegistry.SOULWOOD_FENCE, BlockRegistry.SOULWOOD_PLANKS);
        buttonItem(BlockRegistry.SOULWOOD_BUTTON, BlockRegistry.SOULWOOD_PLANKS);
        basicItem(BlockRegistry.SOULWOOD_DOOR.get().asItem());
        saplingItem(BlockRegistry.GLIMMERBLOOM);
        saplingItem(BlockRegistry.VEILCAP);
        saplingItem(BlockRegistry.MANABLOOM);
        saplingItem(BlockRegistry.EMBERBLOOM);
        saplingItem(BlockRegistry.DUSKBLOOM);
        simpleBlockItem(BlockRegistry.SPIRITSTONE.get());
        simpleBlockItem(BlockRegistry.SPIRITSTONE_BRICKS.get());

        basicItem(ItemRegistry.AMETHYST_COAL.get());
        singleTexture("amethyst_sugar_cane", mcLoc("item/generated"), "layer0", modLoc("item/amethyst_sugar_cane"));

        torch("willow_torch",  modLoc("block/willow_torch"));

        for (DyeColor color : DyeColor.values()) {
            singleTexture("chalk_"+ color.getName(), mcLoc("item/generated"), "layer0", modLoc("item/chalk"));
        }
        // Same sprite as every other chalk; the colour handler cycles the hue instead of
        // a dye supplying it.
        singleTexture("omni_chalk", mcLoc("item/generated"), "layer0", modLoc("item/chalk"));

        // ---- The Willow Fen ----
        // Fen block items (plants, stone, amber) come from the blockstate provider, beside their models.
        basicItem(ItemRegistry.WISPFLY_SPAWN_EGG.get());
        basicItem(ItemRegistry.FEN_HERON_SPAWN_EGG.get());
        basicItem(ItemRegistry.WILLOW_HOUND_SPAWN_EGG.get());
        basicItem(ItemRegistry.GLIMMERFAWN_SPAWN_EGG.get());
        basicItem(ItemRegistry.SPIRIT_DOWN.get());
        basicItem(ItemRegistry.WILLOW_SAP_BOTTLE.get());
        fluidBucket(ItemRegistry.BOG_WATER_BUCKET.getId().getPath(), FluidRegistry.BOG_WATER.get());
        fluidBucket(ItemRegistry.WILLOW_SAP_BUCKET.getId().getPath(), FluidRegistry.WILLOW_SAP.get());

        getBuilder(ItemRegistry.FLUXIA_FLUID_BUCKET.getRegisteredName())
                .parent(getExistingFile(ResourceLocation.fromNamespaceAndPath("neoforge", "item/bucket")))
                .customLoader(DynamicFluidContainerModelBuilder::begin)
                .applyFluidLuminosity(true)
                .coverIsMask(false)
                .flipGas(false)
                .fluid(FluidRegistry.FLUXIA_FLUID.get());
    }
    /** NeoForge's dynamic bucket: the vanilla bucket with the fluid's own still sprite inside. */
    private void fluidBucket(String name, net.minecraft.world.level.material.Fluid fluid) {
        getBuilder(name)
                .parent(getExistingFile(ResourceLocation.fromNamespaceAndPath("neoforge", "item/bucket")))
                .customLoader(DynamicFluidContainerModelBuilder::begin)
                .applyFluidLuminosity(true)
                .coverIsMask(false)
                .flipGas(false)
                .fluid(fluid);
    }

    private ItemModelBuilder saplingItem(DeferredBlock<?> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(WispWillow.MODID,"block/" + item.getId().getPath()));
    }

    public void buttonItem(DeferredBlock<?> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/button_inventory"))
                .texture("texture",  ResourceLocation.fromNamespaceAndPath(WispWillow.MODID,
                        "block/" + baseBlock.getId().getPath()));
    }

    public void fenceItem(DeferredBlock<?> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/fence_inventory"))
                .texture("texture",  ResourceLocation.fromNamespaceAndPath(WispWillow.MODID,
                        "block/" + baseBlock.getId().getPath()));
    }

    public void wallItem(DeferredBlock<?> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/wall_inventory"))
                .texture("wall",  ResourceLocation.fromNamespaceAndPath(WispWillow.MODID,
                        "block/" + baseBlock.getId().getPath()));
    }
}
