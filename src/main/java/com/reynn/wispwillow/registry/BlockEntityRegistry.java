package com.reynn.wispwillow.registry;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.blocks.AltarBlockEntity;
import com.reynn.wispwillow.blocks.ManaPoolBlockEntity;
import com.reynn.wispwillow.blocks.alchemy.AlchemicalCauldronBlockEntity;
import com.reynn.wispwillow.blocks.fluxia.AstralFontEntity;
import com.reynn.wispwillow.blocks.fluxia.BloomEntities;
import com.reynn.wispwillow.blocks.fluxia.FluxiaDynamoEntity;
import com.reynn.wispwillow.blocks.fluxia.GatewayCoreBlockEntity;
import com.reynn.wispwillow.blocks.fluxia.GatewayPortalBlockEntity;
import com.reynn.wispwillow.blocks.spirit.SpiritPortalBlockEntity;
import com.reynn.wispwillow.blocks.fluxia.ChunkLoaderBlockEntity;
import com.reynn.wispwillow.blocks.fluxia.FunctionalBloomEntity;
import com.reynn.wispwillow.blocks.fluxia.FluxiaSpitterBlockEntity;
import com.reynn.wispwillow.blocks.fluxia.LuxiniasFlowerEntity;
import com.reynn.wispwillow.blocks.fluxia.SolrosaFlowerEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WispWillow.MODID);

    public static final Supplier<BlockEntityType<AltarBlockEntity>> ALTAR =
            BLOCK_ENTITIES.register("altar",
                    () -> BlockEntityType.Builder.of(AltarBlockEntity::new, BlockRegistry.ALTAR.get()).build(null));

    public static final Supplier<BlockEntityType<FluxiaSpitterBlockEntity>> FLUXIA_SPITTER =
            BLOCK_ENTITIES.register("fluxia_spitter",
                    () -> BlockEntityType.Builder.of(FluxiaSpitterBlockEntity::new, BlockRegistry.FLUXIA_SPITTER.get())
                            .build(null));

    // NOTE: this was bound to LuxiniasFlowerEntity::new while ChunkLoaderBlock creates a
    // ChunkLoaderBlockEntity. Reloading a world with a placed chunk loader therefore
    // rebuilt it as a flower and the ticker's cast blew up.
    public static final Supplier<BlockEntityType<ChunkLoaderBlockEntity>> CHUNK_LOADER_BE =
            BLOCK_ENTITIES.register("chunk_loader",
                    () -> BlockEntityType.Builder.of(ChunkLoaderBlockEntity::new, BlockRegistry.CHUNK_LOADER.get())
                            .build(null));

    public static final Supplier<BlockEntityType<LuxiniasFlowerEntity>> LUXINIAS_FLOWER =
            BLOCK_ENTITIES.register("luxinias_flower",
                    () -> BlockEntityType.Builder.of(LuxiniasFlowerEntity::new, BlockRegistry.LUXINIAS_FLOWER.get())
                            .build(null));

    public static final Supplier<BlockEntityType<SolrosaFlowerEntity>> SOLROSA_FLOWER =
            BLOCK_ENTITIES.register("solrosa_flower",
                    () -> BlockEntityType.Builder.of(SolrosaFlowerEntity::new, BlockRegistry.SOLROSA_FLOWER.get())
                            .build(null));

    public static final Supplier<BlockEntityType<ManaPoolBlockEntity>> MANA_POOL =
            BLOCK_ENTITIES.register("mana_pool", () ->
                    // The creative pool is the same block entity with a bottomless tank; see its constructor.
                    BlockEntityType.Builder.of(ManaPoolBlockEntity::new,
                                    BlockRegistry.MANA_POOL.get(), BlockRegistry.CREATIVE_MANA_POOL.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FunctionalBloomEntity>> VERDANT_BLOOM =
            BLOCK_ENTITIES.register("verdant_bloom",
                    () -> BlockEntityType.Builder.<FunctionalBloomEntity>of(
                            BloomEntities.Verdant::new, BlockRegistry.VERDANT_BLOOM.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FunctionalBloomEntity>> GATHERING_BLOOM =
            BLOCK_ENTITIES.register("gathering_bloom",
                    () -> BlockEntityType.Builder.<FunctionalBloomEntity>of(
                            BloomEntities.Gathering::new, BlockRegistry.GATHERING_BLOOM.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FunctionalBloomEntity>> WARDING_BLOOM =
            BLOCK_ENTITIES.register("warding_bloom",
                    () -> BlockEntityType.Builder.<FunctionalBloomEntity>of(
                            BloomEntities.Warding::new, BlockRegistry.WARDING_BLOOM.get()).build(null));

    public static final Supplier<BlockEntityType<FluxiaDynamoEntity>> FLUXIA_DYNAMO =
            BLOCK_ENTITIES.register("fluxia_dynamo",
                    () -> BlockEntityType.Builder.of(FluxiaDynamoEntity::new, BlockRegistry.FLUXIA_DYNAMO.get())
                            .build(null));

    public static final Supplier<BlockEntityType<AstralFontEntity>> ASTRAL_FONT =
            BLOCK_ENTITIES.register("astral_font",
                    () -> BlockEntityType.Builder.of(AstralFontEntity::new, BlockRegistry.ASTRAL_FONT.get())
                            .build(null));

    // The chunk-loader note above applies here with a vengeance: this one ticks, casts in
    // its ticker, and is the core of a multiblock, so a supplier/block mismatch would only
    // surface on the first world reload with a cauldron already built.
    public static final Supplier<BlockEntityType<AlchemicalCauldronBlockEntity>> ALCHEMICAL_CAULDRON =
            BLOCK_ENTITIES.register("alchemical_cauldron",
                    () -> BlockEntityType.Builder.of(AlchemicalCauldronBlockEntity::new,
                            BlockRegistry.ALCHEMICAL_CAULDRON.get()).build(null));

    public static final Supplier<BlockEntityType<GatewayCoreBlockEntity>> GATEWAY_CORE =
            BLOCK_ENTITIES.register("gateway_core",
                    () -> BlockEntityType.Builder.of(GatewayCoreBlockEntity::new, BlockRegistry.GATEWAY_CORE.get())
                            .build(null));

    /** Stateless. It exists purely to give the portal sheet a renderer. */
    public static final Supplier<BlockEntityType<GatewayPortalBlockEntity>> GATEWAY_PORTAL =
            BLOCK_ENTITIES.register("gateway_portal",
                    () -> BlockEntityType.Builder.of(GatewayPortalBlockEntity::new, BlockRegistry.GATEWAY_PORTAL.get())
                            .build(null));

    /** Stateless, like the gateway's. It exists purely to give the Open Way's sheet a renderer. */
    public static final Supplier<BlockEntityType<SpiritPortalBlockEntity>> SPIRIT_PORTAL =
            BLOCK_ENTITIES.register("spirit_portal",
                    () -> BlockEntityType.Builder.of(SpiritPortalBlockEntity::new, BlockRegistry.SPIRIT_PORTAL.get())
                            .build(null));

    public static void register(net.neoforged.bus.api.IEventBus bus) { BLOCK_ENTITIES.register(bus); }
}
