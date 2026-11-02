package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.TagRegistry;
import com.reynn.wispwillow.worldgen.fen.WillowFenBiome;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.Tags;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class BiomeTagProvider extends BiomeTagsProvider {
    public static final ResourceLocation HAS_WILLOW_HUT =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "has_structure/willow_hut");

    /** Everywhere underground in the overworld, which is everywhere the oasis makes sense. */
    public static final TagKey<Biome> HAS_FLUXIA_OASIS = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "has_structure/fluxia_oasis"));

    public BiomeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, ExistingFileHelper existingFileHelper) {
        super(output, provider, WispWillow.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var tagKey = TagKey.create(Registries.BIOME, HAS_WILLOW_HUT);
        tag(tagKey).addTag(BiomeTags.IS_SAVANNA);

        tag(HAS_FLUXIA_OASIS).addTag(BiomeTags.IS_OVERWORLD);

        // The fen is its own biome now; its structures and full plant life belong to it alone.
        tag(TagRegistry.Biomes.IS_WILLOW_FEN).add(WillowFenBiome.WILLOW_FEN);

        // Swamps only carry traces of it. Named outright rather than through #c:is_swamp, because
        // the fen is itself in that tag: pulling it in here would add the fen's own reeds and
        // lilies to the fen a second time, which the game rejects as a feature order cycle.
        tag(TagRegistry.Biomes.HAS_FEN_TRACES)
                .add(Biomes.SWAMP)
                .add(Biomes.MANGROVE_SWAMP);

        // What an overworld biome has to be in to behave like one: the overworld itself (which is
        // also what gives it the mod's own ores), plus the structures and world features that go
        // looking for a wetland.
        tag(BiomeTags.IS_OVERWORLD).add(WillowFenBiome.WILLOW_FEN);
        tag(BiomeTags.HAS_MINESHAFT).add(WillowFenBiome.WILLOW_FEN);
        tag(BiomeTags.HAS_STRONGHOLD).add(WillowFenBiome.WILLOW_FEN);
        tag(BiomeTags.HAS_RUINED_PORTAL_SWAMP).add(WillowFenBiome.WILLOW_FEN);
        tag(BiomeTags.HAS_SWAMP_HUT).add(WillowFenBiome.WILLOW_FEN);
        // Common tags, so other mods treat the fen as the wetland it is.
        tag(Tags.Biomes.IS_SWAMP).add(WillowFenBiome.WILLOW_FEN);
        tag(Tags.Biomes.IS_WET_OVERWORLD).add(WillowFenBiome.WILLOW_FEN);
        tag(Tags.Biomes.IS_DENSE_VEGETATION_OVERWORLD).add(WillowFenBiome.WILLOW_FEN);

    }
}
