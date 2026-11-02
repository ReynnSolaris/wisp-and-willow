package com.reynn.wispwillow.registry;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.dev.CreativeManaPoolBlock;
import com.reynn.wispwillow.blocks.*;
import com.reynn.wispwillow.blocks.alchemy.AlchemicalCauldronBlock;
import com.reynn.wispwillow.blocks.fluxia.AstralFontBlock;
import com.reynn.wispwillow.blocks.fluxia.ChunkLoaderBlock;
import com.reynn.wispwillow.blocks.fluxia.FluxiaDynamoBlock;
import com.reynn.wispwillow.blocks.fluxia.GatewayCoreBlock;
import com.reynn.wispwillow.blocks.fluxia.GatewayPortalBlock;
import com.reynn.wispwillow.blocks.spirit.SpiritPortalBlock;
import com.reynn.wispwillow.blocks.fluxia.BloomEntities;
import com.reynn.wispwillow.blocks.fluxia.FluxiaSpitter;
import com.reynn.wispwillow.blocks.fluxia.FunctionalBloomBlock;
import com.reynn.wispwillow.blocks.fluxia.LuxiniasFlower;
import com.reynn.wispwillow.blocks.fluxia.SolrosaFlower;
import com.reynn.wispwillow.blocks.fen.FenMossBlock;
import com.reynn.wispwillow.blocks.fen.MoonlilyBlock;
import com.reynn.wispwillow.blocks.fen.WhisperreedBlock;
import com.reynn.wispwillow.blocks.fen.WispcapBlock;
import com.reynn.wispwillow.worldgen.tree.WillowTreeGrower;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WispWillow.MODID);

    public static final DeferredBlock<Block> SIGIL = BLOCKS.register("sigil", () -> new SigilBlock(BlockBehaviour.Properties.of().instabreak()));
    public static final DeferredBlock<Block> ASTRALITE_BLOCK = BLOCKS.register("astralite_block", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredBlock<Block> ALTAR = BLOCKS.register("altar",
            () -> new com.reynn.wispwillow.blocks.AltarBlock(
                    BlockBehaviour.Properties.of().strength(2.0f).noOcclusion()
            )
    );

    public static final DeferredBlock<Block> CHUNK_LOADER =
            BLOCKS.register("chunk_loader",
                    () -> new ChunkLoaderBlock(Block.Properties.of()
                            .strength(3.0F).noOcclusion()));

    public static final DeferredBlock<Block> FLUXIA_SPITTER = BLOCKS.register("fluxia_spitter",
            () -> new FluxiaSpitter(
                    BlockBehaviour.Properties.of().strength(2.0f).noOcclusion()
            )
    );

    /** A lamp block in the redstone lamp's mould, glowing Fluxia cyan instead of glowstone. */
    public static final DeferredBlock<Block> FLUXIA_LAMP = BLOCKS.register("fluxia_lamp", () -> new Block(
            BlockBehaviour.Properties.of().strength(0.3f).sound(SoundType.GLASS)
                    .lightLevel((state) -> 15)
    ));

    /**
     * The one stone Fluxia will not carry you through.
     *
     * <p>Warded Slate is what makes the Fluxia Oasis a puzzle rather than a straight drop: the
     * pool is lined with it, so phasing into the basin puts you on your feet on the bottom
     * instead of through it. It is obsidian-hard and it drops itself, because a player who has
     * worked out what it does has earned the right to line their own floor with it.</p>
     */
    public static final DeferredBlock<Block> WARDED_SLATE = BLOCKS.register("warded_slate", () -> new Block(
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(12.0f, 1200.0f)
                    .requiresCorrectToolForDrops()
    ));

    //FLUID

    public static final DeferredHolder<Block, LiquidBlock> FLUXIA_FLUID_BLOCK = BLOCKS.register(
            "fluxia_fluid_block",
            () -> new LiquidBlock(
                    FluidRegistry.FLUXIA_FLUID.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .replaceable()
                            .noCollission()
                            .strength(100.0f)
                            // Bright enough to read as a light source across a dark cavern,
                            // which is what makes a Fluxia Oasis findable at all.
                            .lightLevel(state -> 6)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()
                            .sound(SoundType.EMPTY)
            )
    );

    //Crops
    public static final DeferredBlock<Block> AMETHYST_SUGAR_CANE = BLOCKS.register("amethyst_sugar_cane",
            () -> new AmethystSugarCaneBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SUGAR_CANE).randomTicks().instabreak())
    );

    //Willow Wood
    public static final DeferredBlock<RotatedPillarBlockExtended> WILLOW_LOG = BLOCKS.register("willow_log", () -> new RotatedPillarBlockExtended(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_WILLOW_LOG = BLOCKS.register("stripped_willow_log", () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
    public static final DeferredBlock<Block> WILLOW_LEAVES = BLOCKS.register("willow_leaves", () -> new ModifiedLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).randomTicks()));
    public static final DeferredBlock<Block> FLOWERING_WILLOW_LEAVES = BLOCKS.register("flowering_willow_leaves", () -> new ModifiedLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).randomTicks()));
    public static final DeferredBlock<Block> WILLOW_PLANKS = BLOCKS.registerSimpleBlock("willow_planks", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));
    public static final DeferredBlock<Block> WILLOW_SAPLING = BLOCKS.register("willow_sapling",
            ()-> new SaplingBlock(WillowTreeGrower.willowTreeGrower, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    public static final DeferredBlock<DoorBlock> WILLOW_DOOR = BLOCKS.register("willow_door", () -> new DoorBlock(BlockSetType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR)));
    public static final DeferredBlock<VineBlock> WILLOW_VINE = BLOCKS.register("willow_vine", () -> new VineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.VINE)));
    public static final DeferredBlock<TrapDoorBlock> WILLOW_TRAPDOOR = BLOCKS.register("willow_trapdoor", () -> new TrapDoorBlock(BlockSetType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR)));
    public static final DeferredBlock<StairBlock> WILLOW_STAIRS = BLOCKS.register("willow_stairs", () ->
            new StairBlock(WILLOW_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS))
    );
    public static final DeferredBlock<ButtonBlock> WILLOW_BUTTON = BLOCKS.register("willow_button", () ->
            new ButtonBlock(
                    BlockSetType.CHERRY,
                    20,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_BUTTON)
            )
    );
    public static final DeferredBlock<PressurePlateBlock> WILLOW_PRESSURE_PLATE = BLOCKS.register("willow_pressure_plate", ()-> new PressurePlateBlock(BlockSetType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PRESSURE_PLATE)));
    public static final DeferredBlock<SlabBlock> WILLOW_SLAB = BLOCKS.register("willow_slab", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)));
    public static final DeferredBlock<FenceBlock> WILLOW_FENCE = BLOCKS.register("willow_fence", () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_FENCE)));
    public static final DeferredBlock<FenceGateBlock> WILLOW_FENCE_GATE = BLOCKS.register("willow_fence_gate", () -> new FenceGateBlock(WoodType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_FENCE_GATE)));
    //

    public static final DeferredBlock<AmethystFarmland> AMETHYST_FARMLAND = BLOCKS.register("amethyst_farmland", () -> new AmethystFarmland(BlockBehaviour.Properties.ofFullCopy(Blocks.FARMLAND).randomTicks()));
    public static final DeferredBlock<LanternBlock> WILLOW_LANTERN = BLOCKS.register("willow_lantern",
            ()-> new LanternBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
            .pushReaction(PushReaction.DESTROY)
                    .emissiveRendering(BlockBehaviour.BlockStateBase::hasPostProcess))
    );
    public static final DeferredBlock<WillowTorchBlock> WILLOW_TORCH =
            BLOCKS.register("willow_torch",
                    () -> new WillowTorchBlock(ParticleRegistry.FLUX_SPARK,
                            BlockBehaviour.Properties.ofFullCopy(Blocks.TORCH).noOcclusion().instabreak()));

    public static final DeferredBlock<WillowWallTorchBlock> WILLOW_TORCH_WALL =
            BLOCKS.register("willow_torch_wall",
                    () -> new WillowWallTorchBlock(ParticleRegistry.FLUX_SPARK,
                            BlockBehaviour.Properties.ofFullCopy(Blocks.WALL_TORCH).noOcclusion().instabreak()));


    public static final DeferredBlock<Block> LUXINIAS_FLOWER = BLOCKS.register(
            "luxinias_flower",
            () -> new LuxiniasFlower(BlockBehaviour.Properties.ofFullCopy(Blocks.OXEYE_DAISY).noOcclusion().randomTicks())
    );

    public static final DeferredBlock<Block> SOLROSA_FLOWER = BLOCKS.register(
            "solrosa",
            () -> new SolrosaFlower(BlockBehaviour.Properties.ofFullCopy(Blocks.OXEYE_DAISY))
    );

    public static final DeferredBlock<Block> FLUXITE = BLOCKS.register(
            "fluxite",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE))
    );

    public static final DeferredBlock<Block> ASTRALITE_ORE = BLOCKS.register(
            "astralite_ore",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE))
    );

    public static final DeferredBlock<Block> DEEPSLATE_ASTRALITE_ORE = BLOCKS.register(
            "deepslate_astralite_ore",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE))
    );

    public static final DeferredBlock<Block> POTTED_LUXINIAS_FLOWER = BLOCKS.register("potted_luxinias_flower", ()->flowerPot(LUXINIAS_FLOWER.get()));

    public static final DeferredBlock<Block> SNAD = BLOCKS.register("snad", () -> new SnadBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).randomTicks()));

    public static final DeferredBlock<FuelBlock> AMETHYST_COAL_BLOCK = BLOCKS.register("amethyst_coal_block",
            () -> new FuelBlock(BlockBehaviour.Properties.of(), 32000));

    public static final DeferredBlock<Block> MANA_POOL =
            BLOCKS.register("mana_pool", () ->
                    new ManaPoolBlock(BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .noOcclusion() // pool is shallow
                    ));

    /** The bottomless pool of the developer kit ({@link CreativeManaPoolBlock}). Creative tab only. */
    public static final DeferredBlock<Block> CREATIVE_MANA_POOL =
            BLOCKS.register("creative_mana_pool", () ->
                    new CreativeManaPoolBlock(BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .noOcclusion()));


    /**
     * Foundation runes. These were previously plain items with textures, no recipe and no
     * behaviour; as blocks they carry the altar's tier system.
     */
    public static final Map<RuneType, DeferredBlock<Block>> RUNES = new EnumMap<>(RuneType.class);
    static {
        for (RuneType type : RuneType.values()) {
            RUNES.put(type, BLOCKS.register(type.itemName(),
                    () -> new RuneBlock(type, BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)
                            .strength(3.0f)
                            .lightLevel(state -> 3))));
        }
    }

    // ---- Functional flora: flowers that spend Fluxia to do work ----
    public static final DeferredBlock<Block> VERDANT_BLOOM = BLOCKS.register("verdant_bloom",
            () -> new FunctionalBloomBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OXEYE_DAISY).noOcclusion().lightLevel(s -> 6),
                    BloomEntities.Verdant::new, BlockEntityRegistry.VERDANT_BLOOM, 0x7BC24A));

    public static final DeferredBlock<Block> GATHERING_BLOOM = BLOCKS.register("gathering_bloom",
            () -> new FunctionalBloomBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OXEYE_DAISY).noOcclusion().lightLevel(s -> 6),
                    BloomEntities.Gathering::new, BlockEntityRegistry.GATHERING_BLOOM, 0x4FA8E8));

    public static final DeferredBlock<Block> WARDING_BLOOM = BLOCKS.register("warding_bloom",
            () -> new FunctionalBloomBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OXEYE_DAISY).noOcclusion().lightLevel(s -> 8),
                    BloomEntities.Warding::new, BlockEntityRegistry.WARDING_BLOOM, 0xC9A0FF));

    // ---- Generation: the two blocks that make a Soul Gateway affordable ----
    public static final DeferredBlock<Block> FLUXIA_DYNAMO = BLOCKS.register("fluxia_dynamo",
            () -> new FluxiaDynamoBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .lightLevel(state -> state.getValue(FluxiaDynamoBlock.LIT) ? 13 : 0)));

    public static final DeferredBlock<Block> ASTRAL_FONT = BLOCKS.register("astral_font",
            () -> new AstralFontBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .noOcclusion()
                    .lightLevel(state -> 7)));

    /** Storage for the tier above astralite. Fireproof like the ingot it packs. */
    public static final DeferredBlock<Block> EMPYRITE_BLOCK = BLOCKS.register("empyrite_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
                    .lightLevel(state -> 4)));

    // ---- The Alchemical Cauldron ----
    public static final DeferredBlock<Block> ALCHEMICAL_CAULDRON = BLOCKS.register("alchemical_cauldron",
            () -> new AlchemicalCauldronBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON)
                    .strength(3.5f)
                    .noOcclusion()
                    .lightLevel(AlchemicalCauldronBlock::lightEmission)));

    // ---- The Soul Gateway ----
    public static final DeferredBlock<Block> GATEWAY_CORE = BLOCKS.register("gateway_core",
            () -> new GatewayCoreBlock(BlockBehaviour.Properties.of()
                    .strength(4.0f)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(GatewayCoreBlock.OPEN) ? 12 : 2)));

    /**
     * The portal sheet itself. Placed only by an open gateway core — it has no block item
     * (see the skip list in {@link ItemRegistry#register}) and no loot table.
     */
    public static final DeferredBlock<Block> GATEWAY_PORTAL = BLOCKS.register("gateway_portal",
            () -> new GatewayPortalBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .noLootTable()
                    // The model is a set of flat planes, not a cube. Without this the game
                    // treats it as solid: it culls the neighbouring faces and blocks light.
                    .noOcclusion()
                    .strength(-1.0f, 3600000.0f)
                    .lightLevel(state -> 11)
                    .pushReaction(PushReaction.BLOCK)));

    // ---- The Spirit Realm ----

    /**
     * Soulwood: the one tree that grows on the islands.
     *
     * <p>Worldgen only, like the fen's weeping trees — there is no sapling, because the realm is
     * somewhere you visit rather than somewhere you farm. The leaves carry their own light, which
     * is most of what makes a grove of them read as a place rather than as scenery.</p>
     */
    public static final DeferredBlock<RotatedPillarBlock> SOULWOOD_LOG = BLOCKS.register("soulwood_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_LOG)));

    /**
     * A {@link ModifiedLeavesBlock} rather than a vanilla one, and that is structural.
     *
     * <p>The soulwood's crown floats clear of its trunk, so the only wood anywhere near a leaf is
     * the short core buried inside the canopy. Vanilla leaves die more than six blocks from the
     * nearest log, which a detached crown of any size would breach on its outer shell; this block
     * reaches fourteen, so the whole crown stays fed from that one hidden core.</p>
     */
    public static final DeferredBlock<Block> SOULWOOD_LEAVES = BLOCKS.register("soulwood_leaves",
            () -> new ModifiedLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
                    .lightLevel(state -> 5)));

    // ---- The Soulwood set --------------------------------------------------
    // A full wood set off a tree that only worldgen can plant. There is no sapling on purpose —
    // the realm is somewhere you raid for timber, not somewhere you farm — so every one of these
    // is gated behind actually keeping a Vigil of the Open Way burning and carrying logs home.
    // Cherry's block set and wood type throughout, matching the willow set: it only governs the
    // sounds a door and a button make, and a bespoke WoodType would need a sign material too.

    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_SOULWOOD_LOG = BLOCKS.register("stripped_soulwood_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_LOG)));

    public static final DeferredBlock<Block> SOULWOOD_PLANKS = BLOCKS.registerSimpleBlock("soulwood_planks",
            BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_PLANKS));

    public static final DeferredBlock<StairBlock> SOULWOOD_STAIRS = BLOCKS.register("soulwood_stairs",
            () -> new StairBlock(SOULWOOD_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_STAIRS)));

    public static final DeferredBlock<SlabBlock> SOULWOOD_SLAB = BLOCKS.register("soulwood_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_SLAB)));

    public static final DeferredBlock<FenceBlock> SOULWOOD_FENCE = BLOCKS.register("soulwood_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_FENCE)));

    public static final DeferredBlock<FenceGateBlock> SOULWOOD_FENCE_GATE = BLOCKS.register("soulwood_fence_gate",
            () -> new FenceGateBlock(WoodType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_FENCE_GATE)));

    public static final DeferredBlock<DoorBlock> SOULWOOD_DOOR = BLOCKS.register("soulwood_door",
            () -> new DoorBlock(BlockSetType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_DOOR)));

    public static final DeferredBlock<TrapDoorBlock> SOULWOOD_TRAPDOOR = BLOCKS.register("soulwood_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_TRAPDOOR)));

    public static final DeferredBlock<ButtonBlock> SOULWOOD_BUTTON = BLOCKS.register("soulwood_button",
            () -> new ButtonBlock(BlockSetType.CHERRY, 20,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_BUTTON)));

    public static final DeferredBlock<PressurePlateBlock> SOULWOOD_PRESSURE_PLATE = BLOCKS.register("soulwood_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.CHERRY,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PRESSURE_PLATE)));

    // ---- Spirit flora ------------------------------------------------------
    // The realm had exactly two growing things — grass and fern, both vanilla — which is most of
    // why the islands read as bare. These are the mod's own, they carry their own light, and unlike
    // the leaves they are not biome-tinted, so their colour lives in the sprite.

    /** A pale seven-pointed star on a thin stem. The commonest thing growing in the realm. */
    public static final DeferredBlock<Block> GLIMMERBLOOM = BLOCKS.register("glimmerbloom",
            () -> new FlowerBlock(MobEffects.GLOWING, 6.0F,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.ALLIUM)
                            .lightLevel(state -> 4)));

    /**
     * A low, fluted cap that leans where it grows.
     *
     * <p>Offset on XZ like the fen's wispcap, so a patch of them never lines up on the grid — which
     * is the single cheapest thing that stops ground cover reading as tiling.</p>
     */
    public static final DeferredBlock<Block> VEILCAP = BLOCKS.register("veilcap",
            () -> new FlowerBlock(MobEffects.NIGHT_VISION, 4.0F,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.BLUE_ORCHID)
                            .lightLevel(state -> 6)
                            .offsetType(BlockBehaviour.OffsetType.XZ)));

    // ---- The charged blooms ------------------------------------------------
    // Where the glimmerbloom and the veilcap belong to the realm and share its pale blue, these
    // three are the opposite idea: saturated, jewel-toned things that look like mana was
    // concentrated into them. Scattered thinly through the same patches, they are what stops a
    // meadow reading as one colour, and each takes a different vanilla silhouette so they are told
    // apart at a distance rather than only up close.

    public static final DeferredBlock<Block> MANABLOOM = BLOCKS.register("manabloom",
            () -> new FlowerBlock(MobEffects.MOVEMENT_SPEED, 5.0F,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.ALLIUM)
                            .lightLevel(state -> 4)));

    public static final DeferredBlock<Block> EMBERBLOOM = BLOCKS.register("emberbloom",
            () -> new FlowerBlock(MobEffects.FIRE_RESISTANCE, 5.0F,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OXEYE_DAISY)
                            .lightLevel(state -> 4)));

    public static final DeferredBlock<Block> DUSKBLOOM = BLOCKS.register("duskbloom",
            () -> new FlowerBlock(MobEffects.JUMP, 5.0F,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.AZURE_BLUET)
                            .lightLevel(state -> 4)
                            .offsetType(BlockBehaviour.OffsetType.XZ)));

    // ---- Spiritstone -------------------------------------------------------

    /**
     * The bedrock of the realm, in place of end stone.
     *
     * <p>End stone was never a deliberate choice — it came along with borrowing the End's generator,
     * and it brought the End's sallow yellow with it, which fought everything else in the palette.
     * This is the same job in the realm's own colour.</p>
     *
     * <p>It carries a light level of two. Nothing is lit from below in a world of floating islands,
     * so their undersides and cliff faces were the darkest surfaces in the dimension; a faint glow
     * in the rock itself means the land reads as alive rather than as silhouette.</p>
     */
    public static final DeferredBlock<Block> SPIRITSTONE = BLOCKS.register("spiritstone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE)
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .lightLevel(state -> 2)));

    /** Cut and coursed, for what the ruins are built from. */
    public static final DeferredBlock<Block> SPIRITSTONE_BRICKS = BLOCKS.register("spiritstone_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE_BRICKS)
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .lightLevel(state -> 2)));

    /**
     * The Open Way's sheet. Placed only by a burning Vigil of the Open Way — it has no block
     * item (see the skip list in {@link #isItemless}) and no loot table.
     */
    public static final DeferredBlock<Block> SPIRIT_PORTAL = BLOCKS.register("spirit_portal",
            () -> new SpiritPortalBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .noLootTable()
                    .noOcclusion()
                    .strength(-1.0f, 3600000.0f)
                    .lightLevel(state -> 11)
                    .pushReaction(PushReaction.BLOCK)));

    // ---- The Willow Fen ----------------------------------------------------
    // Everything below is scenery for the wetlands. None of it is a willow: the fen's weeping
    // trees are dark oak with drooping leaves, so a player still has to coax the first willow
    // sapling out of still water, and willow logs stay behind that rite.

    /**
     * Drops sticks and never a sapling.
     *
     * <p>A {@link ModifiedLeavesBlock} rather than a vanilla one, and that is load-bearing: vanilla
     * leaves die more than six blocks from the nearest log, while a weeping tree's whole silhouette
     * is a curtain trailing further than that. Measured on 2026-09-16, a great weeping tree built
     * from vanilla leaves had 115 of its 388 leaves already at the decay distance the moment it
     * generated — the ends of every strand evaporated a few minutes later. This block reaches
     * fourteen, which is what the willow grown from the rite has always used.</p>
     */
    public static final DeferredBlock<Block> WEEPING_LEAVES = BLOCKS.register("weeping_leaves",
            () -> new ModifiedLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));

    public static final DeferredBlock<Block> FEN_MOSS = BLOCKS.register("fen_moss",
            () -> new FenMossBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .replaceable()
                    .ignitedByLava()
                    .sound(SoundType.MOSS_CARPET)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<Block> WHISPERREED = BLOCKS.register("whisperreed",
            () -> new WhisperreedBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS)
                    .mapColor(MapColor.COLOR_BROWN)));

    public static final DeferredBlock<Block> MOONLILY = BLOCKS.register("moonlily",
            () -> new MoonlilyBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_PAD)
                    .lightLevel(state -> 5)));

    public static final DeferredBlock<Block> WISPCAP = BLOCKS.register("wispcap",
            () -> new WispcapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.FUNGUS)
                    .lightLevel(state -> 7)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<Block> BOGBELL = BLOCKS.register("bogbell",
            () -> new FlowerBlock(MobEffects.WATER_BREATHING, 7.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.BLUE_ORCHID)));

    public static final DeferredBlock<Block> DUSKPETAL = BLOCKS.register("duskpetal",
            () -> new FlowerBlock(MobEffects.NIGHT_VISION, 5.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.ALLIUM)));

    public static final DeferredBlock<Block> POTTED_WISPCAP = BLOCKS.register("potted_wispcap", () -> flowerPot(WISPCAP.get()));
    public static final DeferredBlock<Block> POTTED_BOGBELL = BLOCKS.register("potted_bogbell", () -> flowerPot(BOGBELL.get()));
    public static final DeferredBlock<Block> POTTED_DUSKPETAL = BLOCKS.register("potted_duskpetal", () -> flowerPot(DUSKPETAL.get()));

    /** Waterlogged, half-rotted soil from under the fen pools. Burns, slowly. */
    public static final DeferredBlock<Block> PEAT = BLOCKS.register("peat",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(0.5F)
                    .sound(SoundType.PACKED_MUD)));

    public static final DeferredBlock<Block> FENSTONE = BLOCKS.register("fenstone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.TERRACOTTA_GREEN)));
    public static final DeferredBlock<Block> FENSTONE_BRICKS = BLOCKS.register("fenstone_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.TERRACOTTA_GREEN)));
    public static final DeferredBlock<Block> MOSSY_FENSTONE_BRICKS = BLOCKS.register("mossy_fenstone_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICKS).mapColor(MapColor.TERRACOTTA_GREEN)));
    public static final DeferredBlock<StairBlock> FENSTONE_BRICK_STAIRS = BLOCKS.register("fenstone_brick_stairs",
            () -> new StairBlock(FENSTONE_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_STAIRS)));
    public static final DeferredBlock<SlabBlock> FENSTONE_BRICK_SLAB = BLOCKS.register("fenstone_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_SLAB)));
    public static final DeferredBlock<WallBlock> FENSTONE_BRICK_WALL = BLOCKS.register("fenstone_brick_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_WALL)));

    /** Set willow sap. Glassy, warm, and it keeps a little of the tree's light. */
    public static final DeferredBlock<Block> WILLOW_AMBER = BLOCKS.register("willow_amber",
            () -> new TransparentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                    .mapColor(MapColor.COLOR_ORANGE)
                    .lightLevel(state -> 6)));

    public static final DeferredHolder<Block, LiquidBlock> BOG_WATER_BLOCK = BLOCKS.register("bog_water",
            () -> new LiquidBlock(FluidRegistry.BOG_WATER.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid()
                    .sound(SoundType.EMPTY)));

    public static final DeferredHolder<Block, LiquidBlock> WILLOW_SAP_BLOCK = BLOCKS.register("willow_sap",
            () -> new LiquidBlock(FluidRegistry.WILLOW_SAP.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .lightLevel(state -> 3)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid()
                    .sound(SoundType.EMPTY)));

    /** The bog cypress's needles: darker and bluer than the willow's, and they droop in threads. */
    public static final DeferredBlock<Block> CYPRESS_NEEDLES = BLOCKS.register("cypress_needles",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES)));

    public static final DeferredBlock<Block> MOSSY_FENSTONE = BLOCKS.register("mossy_fenstone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_COBBLESTONE).mapColor(MapColor.TERRACOTTA_GREEN)));

    public static final DeferredBlock<RotatedPillarBlock> FENSTONE_PILLAR = BLOCKS.register("fenstone_pillar",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.TERRACOTTA_GREEN)));

    /** Peat cut into blocks and dried: tidier than the raw stuff, and it burns longer. */
    public static final DeferredBlock<Block> PEAT_BRICKS = BLOCKS.register("peat_bricks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(0.8F)
                    .sound(SoundType.PACKED_MUD)));

    /**
     * Blocks with no item of any kind: liquids, the wall torch, the portal sheet and potted
     * plants. Both the automatic block items and the creative tab skip these.
     */
    public static boolean isItemless(DeferredHolder<Block, ? extends Block> entry) {
        return entry == WILLOW_TORCH_WALL || entry == FLUXIA_FLUID_BLOCK || entry == GATEWAY_PORTAL
                || entry == SPIRIT_PORTAL
                || entry == BOG_WATER_BLOCK || entry == WILLOW_SAP_BLOCK
                || entry == POTTED_WISPCAP || entry == POTTED_BOGBELL || entry == POTTED_DUSKPETAL;
    }

    //Block Tags
    private static Block flowerPot(Block potted) {
        return new FlowerPotBlock(potted, BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY));
    }
    public static final TagKey<Block> WILLOW_LOGS_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "willow_logs")
    );

    public static final TagKey<Block> SOULWOOD_LOGS_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "soulwood_logs")
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);

    }
}
