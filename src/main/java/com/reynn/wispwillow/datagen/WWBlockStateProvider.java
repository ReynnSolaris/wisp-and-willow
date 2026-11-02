package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.blocks.RuneType;
import com.reynn.wispwillow.blocks.SigilBlock;
import com.reynn.wispwillow.blocks.SigilType;
import com.reynn.wispwillow.blocks.alchemy.AlchemicalCauldronBlock;
import com.reynn.wispwillow.blocks.fen.FenMossBlock;
import com.reynn.wispwillow.blocks.fen.WhisperreedBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import com.reynn.wispwillow.blocks.fluxia.FluxiaDynamoBlock;
import com.reynn.wispwillow.blocks.fluxia.GatewayCoreBlock;
import com.reynn.wispwillow.blocks.fluxia.GatewayPortalBlock;
import com.reynn.wispwillow.registry.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.client.model.generators.*;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class WWBlockStateProvider extends BlockStateProvider {
    private PackOutput output;
    public WWBlockStateProvider(PackOutput output, ExistingFileHelper efh) {
        super(output, WispWillow.MODID, efh);
        this.output = output;
    }

    @Override
    protected void registerStatesAndModels() {

        Map<SigilType, ModelFile> modelsByType = new EnumMap<>(SigilType.class);
        for (SigilType t : SigilType.values()) {
            String name = "sigil/" + t.modelName(); // textures/block/sigil/<name>.png
            BlockModelBuilder m = models().getBuilder("sigil_" + t.modelName())
                    .parent(models().getExistingFile(mcLoc("block/block")))
                    .texture("layer0", modLoc("block/" + name))
                    .texture("particle", modLoc("block/" + name))
                    .renderType("minecraft:cutout")
                    .element()
                    .from(0f, 0f, 0f).to(16f, 0.01f, 16f)
                    .face(Direction.UP).uvs(0,0,16,16).texture("#layer0").tintindex(0).end()
                    .face(Direction.DOWN).uvs(0,0,16,16).texture("#layer0").tintindex(0).end()
                    .end();
            modelsByType.put(t, m);
        }

        getVariantBuilder(BlockRegistry.SIGIL.get()).forAllStates(state ->
                ConfiguredModel.builder()
                        .modelFile(modelsByType.get(state.getValue(SigilBlock.TYPE)))
                        .build()
        );

        // Block item: use BASE model by default (purely cosmetic in inventory)
        simpleBlockItem(BlockRegistry.SIGIL.get(), modelsByType.get(SigilType.BASE));
          fenceGateBlock(BlockRegistry.WILLOW_FENCE_GATE.get(), blockTexture(BlockRegistry.WILLOW_PLANKS.get()));



        simpleBlock(BlockRegistry.CHUNK_LOADER.get());
        simpleBlock(BlockRegistry.ASTRALITE_BLOCK.get());

        simpleBlock(BlockRegistry.ASTRALITE_ORE.get());
        simpleBlock(BlockRegistry.DEEPSLATE_ASTRALITE_ORE.get());

        logBlock(BlockRegistry.WILLOW_LOG.get());
        logBlock(BlockRegistry.STRIPPED_WILLOW_LOG.get());
        simpleBlock(BlockRegistry.WILLOW_PLANKS.get());
        stairsBlock(BlockRegistry.WILLOW_STAIRS.get(), blockTexture(BlockRegistry.WILLOW_PLANKS.get()));
        slabBlock(BlockRegistry.WILLOW_SLAB.get(), blockTexture(BlockRegistry.WILLOW_PLANKS.get()), blockTexture(BlockRegistry.WILLOW_PLANKS.get()));
        fenceBlock(BlockRegistry.WILLOW_FENCE.get(), blockTexture(BlockRegistry.WILLOW_PLANKS.get()));
        leavesBlock(BlockRegistry.WILLOW_LEAVES);
        leavesBlock(BlockRegistry.FLOWERING_WILLOW_LEAVES);
        saplingBlock(BlockRegistry.WILLOW_SAPLING);
        blockItem(BlockRegistry.WILLOW_TRAPDOOR, "_bottom");
        blockItem(BlockRegistry.WILLOW_FENCE_GATE);
        blockItem(BlockRegistry.WILLOW_STAIRS);
        blockItem(BlockRegistry.WILLOW_SLAB);
        buttonBlock(BlockRegistry.WILLOW_BUTTON.get(),  blockTexture(BlockRegistry.WILLOW_PLANKS.get()));
        pressurePlateBlock(BlockRegistry.WILLOW_PRESSURE_PLATE.get(),  blockTexture(BlockRegistry.WILLOW_PLANKS.get()));
        blockItem(BlockRegistry.WILLOW_PRESSURE_PLATE);

        simpleBlock(BlockRegistry.SNAD.get());
        blockItem(BlockRegistry.ALTAR);
        simpleBlock(BlockRegistry.AMETHYST_COAL_BLOCK.get());
        blockItem(BlockRegistry.POTTED_LUXINIAS_FLOWER);
        ModelFile spitterModel = models().getExistingFile(modLoc("block/fluxia_spitter"));
        directionalBlock(BlockRegistry.FLUXIA_SPITTER.get(), spitterModel);
        simpleBlock(BlockRegistry.FLUXITE.get());
        blockItem(BlockRegistry.MANA_POOL);
        blockItem(BlockRegistry.CREATIVE_MANA_POOL);   // blockstate + model are hand-written, like the pool's
        pottedPlantBlock(BlockRegistry.LUXINIAS_FLOWER, BlockRegistry.POTTED_LUXINIAS_FLOWER);
        plantModel(BlockRegistry.SOLROSA_FLOWER);
        lanternBlock(BlockRegistry.WILLOW_LANTERN, "willow_lantern");
        saplingBlock(BlockRegistry.AMETHYST_SUGAR_CANE);
        vineLikeBlock(BlockRegistry.WILLOW_VINE);
        willowTorchWithWall(BlockRegistry.WILLOW_TORCH, BlockRegistry.WILLOW_TORCH_WALL, "willow_torch");
        soulGatewayModels();
        alchemicalCauldronModels();

        // Empyrite storage: a plain cube, so the blockstate provider can do the whole job.
        simpleBlockWithItem(BlockRegistry.EMPYRITE_BLOCK.get(),
                models().cubeAll("empyrite_block", modLoc("block/empyrite_block")));

        simpleBlockWithItem(BlockRegistry.WARDED_SLATE.get(),
                models().cubeAll("warded_slate", modLoc("block/warded_slate")));

        //saplingBlock(BlockRegistry.LUXINIAS_FLOWER);
        doorBlockWithRenderType(BlockRegistry.WILLOW_DOOR.get(), modLoc("block/willow_door_bottom"), modLoc("block/willow_door_top"), "cutout");
        trapdoorBlockWithRenderType(BlockRegistry.WILLOW_TRAPDOOR.get(), modLoc("block/willow_trapdoor"), true, "cutout");

        farmlandBlock(BlockRegistry.AMETHYST_FARMLAND, "amethyst_farmland");

        fluxiaFluidBlock();
        runeBlocks();
        functionalBlooms();
        fenBlocks();
    }

    /** The Willow Fen: plants, stone, amber, and the particle-only models the two liquids need. */
    private void fenBlocks() {
        leavesBlock(BlockRegistry.WEEPING_LEAVES);

        ModelFile moss = models().cross("fen_moss", modLoc("block/fen_moss")).renderType("cutout");
        ModelFile mossTip = models().cross("fen_moss_tip", modLoc("block/fen_moss_tip")).renderType("cutout");
        getVariantBuilder(BlockRegistry.FEN_MOSS.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(FenMossBlock.TIP) ? mossTip : moss).build());
        flatItem("fen_moss", "fen_moss_tip");

        ModelFile reedBottom = models().cross("whisperreed_bottom", modLoc("block/whisperreed_bottom")).renderType("cutout");
        ModelFile reedTop = models().cross("whisperreed_top", modLoc("block/whisperreed_top")).renderType("cutout");
        getVariantBuilder(BlockRegistry.WHISPERREED.get()).forAllStatesExcept(state -> ConfiguredModel.builder()
                        .modelFile(state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? reedTop : reedBottom)
                        .build(),
                WhisperreedBlock.WATERLOGGED);
        flatItem("whisperreed", "whisperreed_top");

        // The vanilla lily pad model lies flat on the water; four turns so a patch doesn't tile.
        ModelFile lily = models().withExistingParent("moonlily", mcLoc("block/lily_pad"))
                .texture("texture", modLoc("block/moonlily"))
                .texture("particle", modLoc("block/moonlily"))
                .renderType("cutout");
        getVariantBuilder(BlockRegistry.MOONLILY.get()).partialState().setModels(
                new ConfiguredModel(lily, 0, 0, false),
                new ConfiguredModel(lily, 0, 90, false),
                new ConfiguredModel(lily, 0, 180, false),
                new ConfiguredModel(lily, 0, 270, false));
        flatItem("moonlily", "moonlily");

        pottedPlantBlock(BlockRegistry.WISPCAP, BlockRegistry.POTTED_WISPCAP);
        pottedPlantBlock(BlockRegistry.BOGBELL, BlockRegistry.POTTED_BOGBELL);
        pottedPlantBlock(BlockRegistry.DUSKPETAL, BlockRegistry.POTTED_DUSKPETAL);
        flatItem("wispcap", "wispcap");
        flatItem("bogbell", "bogbell");
        flatItem("duskpetal", "duskpetal");

        leavesBlock(BlockRegistry.CYPRESS_NEEDLES);
        axisBlock(BlockRegistry.FENSTONE_PILLAR.get(), modLoc("block/fenstone_pillar"), modLoc("block/fenstone_pillar_top"));
        blockItem(BlockRegistry.FENSTONE_PILLAR);

        for (DeferredBlock<Block> cube : List.of(BlockRegistry.PEAT, BlockRegistry.FENSTONE,
                BlockRegistry.FENSTONE_BRICKS, BlockRegistry.MOSSY_FENSTONE_BRICKS,
                BlockRegistry.MOSSY_FENSTONE, BlockRegistry.PEAT_BRICKS)) {
            String name = cube.getId().getPath();
            simpleBlockWithItem(cube.get(), models().cubeAll(name, modLoc("block/" + name)));
        }
        ResourceLocation bricks = modLoc("block/fenstone_bricks");
        stairsBlock(BlockRegistry.FENSTONE_BRICK_STAIRS.get(), bricks);
        slabBlock(BlockRegistry.FENSTONE_BRICK_SLAB.get(), bricks, bricks);
        wallBlock(BlockRegistry.FENSTONE_BRICK_WALL.get(), bricks);
        blockItem(BlockRegistry.FENSTONE_BRICK_STAIRS);
        blockItem(BlockRegistry.FENSTONE_BRICK_SLAB);
        simpleBlockItem(BlockRegistry.FENSTONE_BRICK_WALL.get(),
                models().wallInventory("fenstone_brick_wall_inventory", bricks));

        simpleBlockWithItem(BlockRegistry.WILLOW_AMBER.get(),
                models().cubeAll("willow_amber", modLoc("block/willow_amber")).renderType("translucent"));

        particleOnlyFluid(BlockRegistry.BOG_WATER_BLOCK.get(), "bog_water");
        particleOnlyFluid(BlockRegistry.WILLOW_SAP_BLOCK.get(), "willow_sap");
    }

    /** A flat inventory sprite drawn from one of the block textures. */
    private void flatItem(String item, String blockTexture) {
        itemModels().withExistingParent(item, mcLoc("item/generated"))
                .texture("layer0", modLoc("block/" + blockTexture));
    }

    /** Liquids draw themselves; the blockstate only has to name a particle sprite. */
    private void particleOnlyFluid(Block block, String name) {
        ModelFile fluid = models().getBuilder(name).texture("particle", modLoc("block/" + name + "_still"));
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder().modelFile(fluid).build());
    }

    /** The functional flora render as ordinary cross-model flowers. */
    private void functionalBlooms() {
        for (var block : List.of(BlockRegistry.VERDANT_BLOOM, BlockRegistry.GATHERING_BLOOM,
                BlockRegistry.WARDING_BLOOM)) {
            String name = BuiltInRegistries.BLOCK.getKey(block.get()).getPath();
            ModelFile cross = models().cross(name, modLoc("block/" + name))
                    .renderType("cutout");
            getVariantBuilder(block.get())
                    .forAllStates(s -> ConfiguredModel.builder().modelFile(cross).build());
            itemModels().withExistingParent(name, mcLoc("item/generated"))
                    .texture("layer0", modLoc("block/" + name));
        }
    }

    /**
     * Foundation runes: a fluxite-sided block with the rune glyph carved into its top
     * face, which is the only face you see once it is laid into a floor.
     */
    private void runeBlocks() {
        for (RuneType type : RuneType.values()) {
            Block block = BlockRegistry.RUNES.get(type).get();
            String name = type.itemName();

            /*ModelFile model = models().cubeBottomTop(name,
                    modLoc("block/fluxite_invert"),          // sides
                    modLoc("block/fluxite_invert"),          // bottom
                    modLoc("block/fluxite_invert"));//modLoc("item/" + name));          // top: the existing rune art
           */
            ModelFile model = models().getExistingFile(modLoc("block/runeblock_" + name.replace("_rune","")));

            simpleBlock(block, model);
            simpleBlockItem(block, model);
        }
    }

    /**
     * The Fluxia source block had no blockstate at all, so the client logged a missing
     * model for all sixteen fluid levels on every launch and had no particle texture to
     * use when the fluid was disturbed.
     *
     * <p>Vanilla handles water and lava the same way: one model that carries nothing but a
     * particle texture, mapped to every level.</p>
     */
    private void fluxiaFluidBlock() {
        ModelFile fluid = models().getBuilder("fluxia_fluid_block")
                .texture("particle", modLoc("block/fluxia_fluid_still"));

        getVariantBuilder(BlockRegistry.FLUXIA_FLUID_BLOCK.get())
                .forAllStates(state -> ConfiguredModel.builder().modelFile(fluid).build());
    }

    private void vineLikeBlock(DeferredBlock<? extends Block> reg) {
        Block block = reg.get();
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();

        // Use the block's standard texture (assets/wispwillow/textures/block/<name>.png)
        var tex = blockTexture(block);

        // === Model: match vanilla vine.json ===
        BlockModelBuilder vineModel = models().getBuilder(name)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", tex)
                .texture("vine", tex)
                .ao(false)
                .renderType("minecraft:cutout")
                .element()
                .from(0f, 0f, 0.8f)
                .to(16f, 16f, 0.8f)
                .shade(false)
                .face(Direction.NORTH)
                .uvs(16, 0, 0, 16)
                .texture("#vine")
                .tintindex(0)
                .ao(false)
                .end()
                .face(Direction.SOUTH)
                .uvs(0, 0, 16, 16)
                .texture("#vine")
                .ao(false)
                .tintindex(0)
                .end()
                .end();

        // === Blockstates: copy vanilla rotations ===
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block);

        // north
        builder.part()
                .modelFile(vineModel)
                .addModel()
                .condition(BlockStateProperties.NORTH, true);

        // east (y = 90)
        builder.part()
                .modelFile(vineModel)
                .uvLock(true)
                .rotationY(90)
                .addModel()
                .condition(BlockStateProperties.EAST, true);

        // south (y = 180)
        builder.part()
                .modelFile(vineModel)
                .uvLock(true)
                .rotationY(180)
                .addModel()
                .condition(BlockStateProperties.SOUTH, true);

        // west (y = 270)
        builder.part()
                .modelFile(vineModel)
                .uvLock(true)
                .rotationY(270)
                .addModel()
                .condition(BlockStateProperties.WEST, true);

        // up (x = 270)
        builder.part()
                .modelFile(vineModel)
                .uvLock(true)
                .rotationX(270)
                .addModel()
                .condition(BlockStateProperties.UP, true);

        // optional: fallback when *no* face is true (vanilla has this)
        builder.part()
                .modelFile(vineModel)
                .uvLock(true)
                .addModel()
                .condition(BlockStateProperties.NORTH, false)
                .condition(BlockStateProperties.EAST,  false)
                .condition(BlockStateProperties.SOUTH, false)
                .condition(BlockStateProperties.WEST,  false)
                .condition(BlockStateProperties.UP,    false);

        // Item: use the same flat model
       // simpleBlockItem(block, vineModel);
    }

    private void multifaceBlock(DeferredBlock<? extends Block> reg) {
        Block block = reg.get();
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();

        // Use the block's normal texture location
        ResourceLocation tex = blockTexture(block); // -> wispwillow:block/<registry_name>

        // Model: vertical sheet, we rotate it to each face
        BlockModelBuilder faceModel = models().getBuilder(name)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("texture", tex)
                .texture("particle", tex)
                .renderType("minecraft:cutout")
                .element()
                .from(0, 0, 0)     // vertical plane
                .to(16, 16, 0.01f)
                .face(Direction.SOUTH)
                .uvs(0, 0, 16, 16)
                .texture("#texture")
                .end()
                .end();

        MultiPartBlockStateBuilder builder = getMultipartBuilder(block);

        // Match vanilla multiface generator orientation
        addMultifacePart(builder, BlockStateProperties.NORTH, faceModel, 180, 0);
        addMultifacePart(builder, BlockStateProperties.SOUTH, faceModel, 0,   0);
        addMultifacePart(builder, BlockStateProperties.EAST,  faceModel, 90,  0);
        addMultifacePart(builder, BlockStateProperties.WEST,  faceModel, 270, 0);
        addMultifacePart(builder, BlockStateProperties.UP,    faceModel, 0,   270);
        addMultifacePart(builder, BlockStateProperties.DOWN,  faceModel, 0,   90);

        // Item uses the same model
        simpleBlockItem(block, faceModel);
    }

    private void addMultifacePart(MultiPartBlockStateBuilder builder,
                                  BooleanProperty property,
                                  ModelFile model,
                                  int yRot,
                                  int xRot) {

        ConfiguredModel.Builder<MultiPartBlockStateBuilder.PartBuilder> part = builder.part()
                .modelFile(model)
                .uvLock(true);

        if (yRot != 0) part.rotationY(yRot);
        if (xRot != 0) part.rotationX(xRot);

        part.addModel().condition(property, true);;
    }

    private void lanternBlock(DeferredBlock<? extends Block> reg, String texBase) {
        Block block = reg.get();

        ResourceLocation tex       = modLoc("block/" + texBase);
        ResourceLocation emissive  = modLoc("block/" + texBase + "_emissive");

        // Standing lantern
        ModelFile lanternModel = models()
                .withExistingParent(texBase, mcLoc("block/template_lantern"))
                .texture("lantern", tex)
                .texture("emissive", emissive)
                .renderType("minecraft:cutout");

        // Hanging lantern
        ModelFile lanternHanging = models()
                .withExistingParent(texBase + "_hanging", mcLoc("block/template_hanging_lantern"))
                .texture("lantern", tex)
                .texture("emissive", emissive)
                .renderType("minecraft:cutout");

        // Blockstates: pick model based on HANGING
        getVariantBuilder(block).forAllStates(state -> {
            boolean hanging = state.getValue(LanternBlock.HANGING);
            return ConfiguredModel.builder()
                    .modelFile(hanging ? lanternHanging : lanternModel)
                    .build();
        });

        // Item uses the standing model
        simpleBlockItem(block, lanternModel);
    }

    private void plantModel(DeferredBlock<? extends Block> plant) {
        Block plantBlock  = plant.get();
        String plantName  = BuiltInRegistries.BLOCK.getKey(plantBlock).getPath();
        var plantModel = models()
                .cross(plantName, blockTexture(plantBlock))
                .renderType("cutout");
        simpleBlock(plantBlock, plantModel);
    }

    private void pottedPlantBlock(DeferredBlock<? extends Block> plant,
                                  DeferredBlock<? extends Block> potted) {

        Block plantBlock  = plant.get();
        Block pottedBlock = potted.get();

        String plantName  = BuiltInRegistries.BLOCK.getKey(plantBlock).getPath();
        String pottedName = BuiltInRegistries.BLOCK.getKey(pottedBlock).getPath();

        // === Plant model: simple cross ===
        var plantModel = models()
                .cross(plantName, blockTexture(plantBlock))
                .renderType("cutout");
        simpleBlock(plantBlock, plantModel);   // world + item (if you want item, you already handle via saplingBlock)

        // === Potted model: vanilla flower_pot_cross parent ===
        var pottedModel = models()
                .withExistingParent(pottedName, mcLoc("block/flower_pot_cross"))
                .texture("plant", blockTexture(plantBlock))   // re-use plant texture
                .renderType("cutout");

        // blockstate → single model
        simpleBlock(pottedBlock, pottedModel);
        // (no item for potted block, same as vanilla)
    }

    private void willowTorchWithWall(DeferredBlock<? extends Block> torch,
                                     DeferredBlock<? extends Block> wallTorch,
                                     String baseName) {

        Block torchBlock = torch.get();
        Block wallBlock  = wallTorch.get();

        var tex      = modLoc("block/" + baseName);
        var emissive = modLoc("block/" + baseName + "_emissive");

        // === MODELS ===

        // Standing torch model (centered on block)
        ModelFile torchModel = models()
                .withExistingParent(baseName, mcLoc("block/template_torch"))
                .texture("torch", tex)
                .texture("emissive", emissive)
                .renderType("minecraft:cutout");

        // Wall torch model (stuck to block side!)
        ModelFile wallTorchModel = models()
                .withExistingParent(baseName + "_wall", mcLoc("block/template_torch_wall"))
                .texture("torch", tex)
                .texture("emissive", emissive)
                .renderType("minecraft:cutout");

        // === BLOCKSTATES ===

        // Standing torch: single model, no rotation
        getVariantBuilder(torchBlock).forAllStates(state ->
                ConfiguredModel.builder().modelFile(torchModel).build()
        );

        // Wall torch: rotate around Y based on FACING
        getVariantBuilder(wallBlock).forAllStates(state -> {
            Direction facing = state.getValue(WallTorchBlock.FACING);
            int yRot = switch (facing) {
                case EAST  -> 0;
                case SOUTH -> 90;
                case WEST  -> 180;
                default    -> 270; // NORTH
            };
            return ConfiguredModel.builder()
                    .modelFile(wallTorchModel)
                    .rotationY(yRot)
                    .build();
        });

        // Item: uses the standing model
        simpleBlockItem(torchBlock, torchModel);
        // (no item for the wall torch)
    }

    private void saplingBlock(DeferredBlock<Block> blockRegistryObject) {
        simpleBlock(blockRegistryObject.get(),
                models().cross(BuiltInRegistries.BLOCK.getKey(blockRegistryObject.get()).getPath(), blockTexture(blockRegistryObject.get())).renderType("cutout"));
    }

    private void leavesBlock(DeferredBlock<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(),
                models().singleTexture(BuiltInRegistries.BLOCK.getKey(blockRegistryObject.get()).getPath(), ResourceLocation.parse("minecraft:block/leaves"),
                        "all", blockTexture(blockRegistryObject.get())).renderType("cutout"));
    }

    private void blockItem(DeferredBlock<?> deferredBlock) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile(WispWillow.MODID + ":block/" + deferredBlock.getId().getPath()));
    }

    private void blockItem(DeferredBlock<?> deferredBlock, String appendix) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile(WispWillow.MODID + ":block/" + deferredBlock.getId().getPath() + appendix));
    }

    private void farmlandBlock(DeferredBlock<? extends Block> reg, String textureBaseName) {
        Block block = reg.get();

        // Textures
        var topDry   = modLoc("block/" + textureBaseName );
        var topMoist = modLoc("block/" + textureBaseName + "_moist");
        var sideTex  = modLoc("block/" + textureBaseName + "_side");
        var dirtTex  = mcLoc("block/dirt"); // bottom

        // Build the two models
        BlockModelBuilder dryModel   = farmlandModel(block, "",   topDry,   sideTex, dirtTex);
        BlockModelBuilder moistModel = farmlandModel(block, "_moist", topMoist, sideTex, dirtTex);

        // Blockstate variants: moisture = 0 -> dry, 1–7 -> moist
        getVariantBuilder(block).forAllStates(state -> {
            int moisture = state.getValue(FarmBlock.MOISTURE); // if you have your own property, swap this
            ModelFile chosen = (moisture == 0) ? dryModel : moistModel;
            return ConfiguredModel.builder().modelFile(chosen).build();
        });

        // Item: show the moist version
        simpleBlockItem(block, moistModel);
    }

    /**
     * Single farmland model: 15px tall slab with custom top/side/bottom textures.
     */
    private BlockModelBuilder farmlandModel(Block block,
                                            String suffix,
                                            ResourceLocation topTex,
                                            ResourceLocation sideTex,
                                            ResourceLocation dirtTex) {
        String baseName = BuiltInRegistries.BLOCK.getKey(block).getPath() + suffix;

        return models().getBuilder(baseName)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", sideTex)
                .texture("top",      topTex)
                .texture("side",     sideTex)
                .texture("dirt",     dirtTex)
                .element()
                // 15 high, like vanilla farmland
                .from(0f, 0f, 0f).to(16f, 15f, 16f)
                .face(Direction.UP)
                .uvs(0, 0, 16, 16)
                .texture("#top")
                .end()
                .face(Direction.DOWN)
                .uvs(0, 0, 16, 16)
                .texture("#dirt")
                .end()
                .face(Direction.NORTH)
                // 1px of top, rest side (0..16 x, 1..16 y)
                .uvs(0, 1, 16, 16)
                .texture("#side")
                .end()
                .face(Direction.SOUTH)
                .uvs(0, 1, 16, 16)
                .texture("#side")
                .end()
                .face(Direction.WEST)
                .uvs(0, 1, 16, 16)
                .texture("#side")
                .end()
                .face(Direction.EAST)
                .uvs(0, 1, 16, 16)
                .texture("#side")
                .end()
                .end();
    }

    /**
     * Models for the Soul Gateway and the two generators that keep it lit.
     *
     * <p>PLACEHOLDER ART. Every texture referenced here is an existing block texture borrowed
     * from elsewhere in the mod, so nothing renders as missing-texture while the real art is
     * drawn. Each one is named in the comment beside it; drop a real PNG at
     * {@code textures/block/<name>.png} and swap the reference.</p>
     */
    private void soulGatewayModels() {
        // Fluxia Dynamo -- wants: block/fluxia_dynamo, block/fluxia_dynamo_lit
        ModelFile dynamoOff = models().cubeAll("fluxia_dynamo", modLoc("block/fluxia_dynamo"));
        ModelFile dynamoOn  = models().cubeAll("fluxia_dynamo_lit", modLoc("block/fluxia_dynamo_lit"));
        getVariantBuilder(BlockRegistry.FLUXIA_DYNAMO.get()).forAllStates(state ->
                ConfiguredModel.builder()
                        .modelFile(state.getValue(FluxiaDynamoBlock.LIT) ? dynamoOn : dynamoOff)
                        .build());
        simpleBlockItem(BlockRegistry.FLUXIA_DYNAMO.get(), dynamoOff);

        // Astral Font -- wants: block/astral_font
        ModelFile font = models().cubeAll("astral_font", modLoc("block/astral_font"));
        simpleBlock(BlockRegistry.ASTRAL_FONT.get(), font);
        simpleBlockItem(BlockRegistry.ASTRAL_FONT.get(), font);

        // Gateway Core -- wants: block/gateway_core, block/gateway_core_open
        ModelFile coreOff = models().cubeAll("gateway_core", modLoc("block/gateway_core"));
        ModelFile coreOn  = models().cubeAll("gateway_core_open", modLoc("block/gateway_core_open"));
        getVariantBuilder(BlockRegistry.GATEWAY_CORE.get()).forAllStates(state -> {
            int rotation = (int) state.getValue(GatewayCoreBlock.FACING).toYRot();
            return ConfiguredModel.builder()
                    .modelFile(state.getValue(GatewayCoreBlock.OPEN) ? coreOn : coreOff)
                    .rotationY((rotation + 180) % 360)
                    .build();
        });
        simpleBlockItem(BlockRegistry.GATEWAY_CORE.get(), coreOff);

        gatewayPortalModels();
        spiritPortalModels();
        spiritTreeModels();
    }

    /**
     * The portal sheet has no model: {@link GatewayPortalBlock} renders INVISIBLE and
     * {@code GatewayPortalRenderer} draws it instead, which is how it gets a starfield that
     * moves with the camera. The blockstate still has to name something, so it names an
     * empty model — vanilla points {@code end_portal} at {@code block/air} for the same
     * reason.
     */
    /**
     * Two models for the cauldron, switched on {@code formed}.
     *
     * <p>{@code lit} deliberately does <em>not</em> change the model. Structural completion
     * is a shape change; paying to light it is a light change — two different facts, so two
     * different channels. The renderer carries the second one, which is why a player who
     * lights a finished ring sees the runes spin up rather than the block popping to a new
     * shape under their cursor.</p>
     */
    private void alchemicalCauldronModels() {
        // Hand-built pots (tools/gen_altar_cauldron.py writes them under src/main/resources).
        ModelFile unformed = models().getExistingFile(modLoc("block/alchemical_cauldron"));
        ModelFile formed = models().getExistingFile(modLoc("block/alchemical_cauldron_formed"));

        getVariantBuilder(BlockRegistry.ALCHEMICAL_CAULDRON.get())
                .forAllStates(state -> ConfiguredModel.builder()
                        .modelFile(state.getValue(AlchemicalCauldronBlock.FORMED) ? formed : unformed)
                        .build());

        simpleBlockItem(BlockRegistry.ALCHEMICAL_CAULDRON.get(), unformed);
    }

    private void gatewayPortalModels() {
        ModelFile empty = models().getExistingFile(mcLoc("block/air"));
        getVariantBuilder(BlockRegistry.GATEWAY_PORTAL.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(empty).build());
    }

    /** Soulwood: an ordinary log and leaf pair, so nothing exotic is needed here. */
    private void spiritTreeModels() {
        logBlock(BlockRegistry.SOULWOOD_LOG.get());
        blockItem(BlockRegistry.SOULWOOD_LOG);
        leavesBlock(BlockRegistry.SOULWOOD_LEAVES);

        // The worked half of the set. Every one of these takes its face from the planks except the
        // door and trapdoor, which have sprites of their own.
        logBlock(BlockRegistry.STRIPPED_SOULWOOD_LOG.get());
        blockItem(BlockRegistry.STRIPPED_SOULWOOD_LOG);
        simpleBlock(BlockRegistry.SOULWOOD_PLANKS.get());
        stairsBlock(BlockRegistry.SOULWOOD_STAIRS.get(), blockTexture(BlockRegistry.SOULWOOD_PLANKS.get()));
        slabBlock(BlockRegistry.SOULWOOD_SLAB.get(), blockTexture(BlockRegistry.SOULWOOD_PLANKS.get()),
                blockTexture(BlockRegistry.SOULWOOD_PLANKS.get()));
        fenceBlock(BlockRegistry.SOULWOOD_FENCE.get(), blockTexture(BlockRegistry.SOULWOOD_PLANKS.get()));
        fenceGateBlock(BlockRegistry.SOULWOOD_FENCE_GATE.get(), blockTexture(BlockRegistry.SOULWOOD_PLANKS.get()));
        buttonBlock(BlockRegistry.SOULWOOD_BUTTON.get(), blockTexture(BlockRegistry.SOULWOOD_PLANKS.get()));
        pressurePlateBlock(BlockRegistry.SOULWOOD_PRESSURE_PLATE.get(), blockTexture(BlockRegistry.SOULWOOD_PLANKS.get()));
        doorBlockWithRenderType(BlockRegistry.SOULWOOD_DOOR.get(), modLoc("block/soulwood_door_bottom"),
                modLoc("block/soulwood_door_top"), "cutout");
        trapdoorBlockWithRenderType(BlockRegistry.SOULWOOD_TRAPDOOR.get(), modLoc("block/soulwood_trapdoor"), true, "cutout");

        blockItem(BlockRegistry.SOULWOOD_STAIRS);
        blockItem(BlockRegistry.SOULWOOD_SLAB);
        blockItem(BlockRegistry.SOULWOOD_FENCE_GATE);
        blockItem(BlockRegistry.SOULWOOD_PRESSURE_PLATE);
        blockItem(BlockRegistry.SOULWOOD_TRAPDOOR, "_bottom");

        // Cross models with a cutout render type, like every other small plant in the mod.
        saplingBlock(BlockRegistry.GLIMMERBLOOM);
        saplingBlock(BlockRegistry.VEILCAP);
        saplingBlock(BlockRegistry.MANABLOOM);
        saplingBlock(BlockRegistry.EMBERBLOOM);
        saplingBlock(BlockRegistry.DUSKBLOOM);

        simpleBlock(BlockRegistry.SPIRITSTONE.get());
        simpleBlock(BlockRegistry.SPIRITSTONE_BRICKS.get());
    }

    /** Same deal as the gateway sheet: rendered, not modelled, so the blockstate names air. */
    private void spiritPortalModels() {
        ModelFile empty = models().getExistingFile(mcLoc("block/air"));
        getVariantBuilder(BlockRegistry.SPIRIT_PORTAL.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(empty).build());
    }
}
