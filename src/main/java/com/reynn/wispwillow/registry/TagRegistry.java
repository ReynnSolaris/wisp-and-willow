package com.reynn.wispwillow.registry;

import com.reynn.wispwillow.WispWillow;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TagRegistry {

    public static class Blocks {

        /**
         * Blocks a phasing player cannot pass through.
         *
         * <p>A tag rather than a block list so a pack can ward its own stone — and so the
         * Fluxia Oasis can line its pool with something the puzzle depends on.</p>
         */
        public static final TagKey<Block> PHASE_PROOF = createTag("phase_proof");

        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, name));
        }
    }

    public static class Biomes {

        /**
         * The Willow Fen itself. A tag rather than the biome key so a pack can add its own
         * wetland and have the fen's structures and creatures treat it as the real thing.
         */
        public static final TagKey<Biome> IS_WILLOW_FEN = TagKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "is_willow_fen"));

        /**
         * Biomes that carry a hint of the fen without being one: vanilla and modded swamps,
         * which get reeds and lilies so the fen's plants are not a total surprise.
         */
        public static final TagKey<Biome> HAS_FEN_TRACES = TagKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "has_fen_traces"));
    }

    public static class Items {

        public static final TagKey<Item> BOTANY_POT_SOIL = createTag("soil");

        /**
         * Everything made of empyrite, and so everything the Awakening rite will take.
         *
         * <p>A tag rather than a list in code so that an addon — or a pack — can hand
         * the rite something new to awaken without touching the ritual definition.</p>
         */
        public static final TagKey<Item> EMPYRITE = createTag("empyrite");

        /** The four pieces of Glimmerweave; the set bonus counts membership here, not item identity. */
        public static final TagKey<Item> GLIMMERWEAVE_ARMOR = createTag("glimmerweave_armor");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, name));
        }
    }
}
