package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.blocks.RuneType;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.TagRegistry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class BlockTagProvider extends BlockTagsProvider {
    public BlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, WispWillow.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Runes are stone-like: a pickaxe mines them, anything else drops nothing.
        for (RuneType rune : RuneType.values()) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(BlockRegistry.RUNES.get(rune).get());
            tag(BlockTags.NEEDS_STONE_TOOL).add(BlockRegistry.RUNES.get(rune).get());
        }
        {
            tag(BlockTags.NEEDS_DIAMOND_TOOL).add(BlockRegistry.EMPYRITE_BLOCK.get());
            tag(BlockTags.NEEDS_DIAMOND_TOOL).add(BlockRegistry.WARDED_SLATE.get());
        }

        // What Fluxia will not carry you through. Bedrock and the rest are here so that
        // phasing can never take a player out of the bottom of the world or into a vault
        // the game considers closed.
        tag(TagRegistry.Blocks.PHASE_PROOF)
                .add(BlockRegistry.WARDED_SLATE.get())
                .add(Blocks.BEDROCK)
                .add(Blocks.BARRIER)
                .add(Blocks.REINFORCED_DEEPSLATE)
                .add(Blocks.END_PORTAL_FRAME)
                .add(Blocks.END_GATEWAY)
                .add(Blocks.END_PORTAL);

        tag(BlockTags.INFINIBURN_OVERWORLD)
                .add(BlockRegistry.AMETHYST_COAL_BLOCK.get());
        tag(BlockTags.INFINIBURN_NETHER)
                .add(BlockRegistry.AMETHYST_COAL_BLOCK.get());
        tag(BlockTags.INFINIBURN_END)
                .add(BlockRegistry.AMETHYST_COAL_BLOCK.get());

        tag(BlockTags.BUTTONS)
                .add(BlockRegistry.WILLOW_BUTTON.get());
        tag(BlockTags.PRESSURE_PLATES)
                .add(BlockRegistry.WILLOW_PRESSURE_PLATE.get());
        tag(BlockTags.SLABS)
                .add(BlockRegistry.WILLOW_SLAB.get());

        tag(BlockTags.FLOWER_POTS)
                .add(BlockRegistry.POTTED_LUXINIAS_FLOWER.get());

        tag(BlockTags.SWORD_EFFICIENT)
                .add(BlockRegistry.AMETHYST_SUGAR_CANE.get());

        tag(BlockRegistry.WILLOW_LOGS_TAG)
                .add(BlockRegistry.WILLOW_LOG.get())
                .add(BlockRegistry.STRIPPED_WILLOW_LOG.get());
        tag(BlockTags.SAND)
                .add(BlockRegistry.SNAD.get());
        tag(BlockTags.LOGS)
                .add(BlockRegistry.WILLOW_LOG.get())
                .add(BlockRegistry.STRIPPED_WILLOW_LOG.get());
        tag(BlockTags.LEAVES)
                .add(BlockRegistry.FLOWERING_WILLOW_LEAVES.get())
                .add(BlockRegistry.WILLOW_LEAVES.get());
        tag(BlockTags.LOGS_THAT_BURN)
                .add(BlockRegistry.WILLOW_LOG.get())
                .add(BlockRegistry.STRIPPED_WILLOW_LOG.get());
        tag(BlockTags.PLANKS)
                .add(BlockRegistry.WILLOW_PLANKS.get());
        tag(BlockTags.FLOWERS)
                .add(BlockRegistry.SOLROSA_FLOWER.get())
                .add(BlockRegistry.LUXINIAS_FLOWER.get());
        tag(BlockTags.FENCES)
                .add(BlockRegistry.WILLOW_FENCE.get());
        tag(BlockTags.FENCE_GATES)
                .add(BlockRegistry.WILLOW_FENCE_GATE.get());

        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(BlockRegistry.WILLOW_PLANKS.get())
                .add(BlockRegistry.WILLOW_STAIRS.get())
                .add(BlockRegistry.WILLOW_FENCE.get())
                .add(BlockRegistry.WILLOW_PRESSURE_PLATE.get())
                .add(BlockRegistry.WILLOW_BUTTON.get())
                .add(BlockRegistry.WILLOW_LOG.get())
                .add(BlockRegistry.WILLOW_SLAB.get())
                .add(BlockRegistry.STRIPPED_WILLOW_LOG.get())
                .add(BlockRegistry.WILLOW_FENCE_GATE.get())
                .add(BlockRegistry.WILLOW_TRAPDOOR.get())
                .add(BlockRegistry.WILLOW_DOOR.get());

        // ---- The Spirit Realm ----
        // BlockTags.LOGS is load-bearing beyond tools: ModifiedLeavesBlock measures a leaf's decay
        // distance from the nearest block in this tag, so the soulwood's hidden crown core only
        // keeps its canopy alive because the log is listed here.
        tag(BlockTags.LOGS)
                .add(BlockRegistry.SOULWOOD_LOG.get())
                .add(BlockRegistry.STRIPPED_SOULWOOD_LOG.get());
        tag(BlockTags.LOGS_THAT_BURN)
                .add(BlockRegistry.SOULWOOD_LOG.get())
                .add(BlockRegistry.STRIPPED_SOULWOOD_LOG.get());
        tag(BlockTags.LEAVES).add(BlockRegistry.SOULWOOD_LEAVES.get());
        tag(BlockTags.MINEABLE_WITH_HOE).add(BlockRegistry.SOULWOOD_LEAVES.get());
        tag(BlockRegistry.SOULWOOD_LOGS_TAG)
                .add(BlockRegistry.SOULWOOD_LOG.get())
                .add(BlockRegistry.STRIPPED_SOULWOOD_LOG.get());

        tag(BlockTags.PLANKS).add(BlockRegistry.SOULWOOD_PLANKS.get());
        tag(BlockTags.FENCES).add(BlockRegistry.SOULWOOD_FENCE.get());
        tag(BlockTags.FENCE_GATES).add(BlockRegistry.SOULWOOD_FENCE_GATE.get());

        // The wooden-* tags are what make the set behave like wood rather than merely look like it:
        // doors and trapdoors a villager can path through, buttons an arrow can trip, pressure
        // plates that answer to dropped items.
        tag(BlockTags.WOODEN_STAIRS).add(BlockRegistry.SOULWOOD_STAIRS.get());
        tag(BlockTags.WOODEN_SLABS).add(BlockRegistry.SOULWOOD_SLAB.get());
        tag(BlockTags.WOODEN_FENCES).add(BlockRegistry.SOULWOOD_FENCE.get());
        tag(BlockTags.WOODEN_DOORS).add(BlockRegistry.SOULWOOD_DOOR.get());
        tag(BlockTags.WOODEN_TRAPDOORS).add(BlockRegistry.SOULWOOD_TRAPDOOR.get());
        tag(BlockTags.WOODEN_BUTTONS).add(BlockRegistry.SOULWOOD_BUTTON.get());
        tag(BlockTags.WOODEN_PRESSURE_PLATES).add(BlockRegistry.SOULWOOD_PRESSURE_PLATE.get());

        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(BlockRegistry.SOULWOOD_LOG.get())
                .add(BlockRegistry.STRIPPED_SOULWOOD_LOG.get())
                .add(BlockRegistry.SOULWOOD_PLANKS.get())
                .add(BlockRegistry.SOULWOOD_STAIRS.get())
                .add(BlockRegistry.SOULWOOD_SLAB.get())
                .add(BlockRegistry.SOULWOOD_FENCE.get())
                .add(BlockRegistry.SOULWOOD_FENCE_GATE.get())
                .add(BlockRegistry.SOULWOOD_DOOR.get())
                .add(BlockRegistry.SOULWOOD_TRAPDOOR.get())
                .add(BlockRegistry.SOULWOOD_BUTTON.get())
                .add(BlockRegistry.SOULWOOD_PRESSURE_PLATE.get());

        // SMALL_FLOWERS rather than FLOWERS directly: vanilla folds the former into the latter, so
        // one entry puts them in both, and it is SMALL_FLOWERS that bees and flower pots read.
        tag(BlockTags.SMALL_FLOWERS)
                .add(BlockRegistry.GLIMMERBLOOM.get())
                .add(BlockRegistry.VEILCAP.get())
                .add(BlockRegistry.MANABLOOM.get())
                .add(BlockRegistry.EMBERBLOOM.get())
                .add(BlockRegistry.DUSKBLOOM.get());

        // Without this the realm's own bedrock is unmineable at any speed and drops nothing to a
        // pickaxe, which is a miserable way to find out a tag was forgotten.
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(BlockRegistry.SPIRITSTONE.get())
                .add(BlockRegistry.SPIRITSTONE_BRICKS.get());

        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(BlockRegistry.ALCHEMICAL_CAULDRON.get())
                .add(BlockRegistry.EMPYRITE_BLOCK.get())
                .add(BlockRegistry.ASTRALITE_BLOCK.get())
                .add(BlockRegistry.MANA_POOL.get())
                .add(BlockRegistry.CREATIVE_MANA_POOL.get())
                .add(BlockRegistry.FLUXIA_LAMP.get())
                .add(BlockRegistry.AMETHYST_COAL_BLOCK.get())
                .add(BlockRegistry.ALTAR.get())
                .add(BlockRegistry.FLUXIA_SPITTER.get())
                .add(BlockRegistry.ASTRALITE_ORE.get())
                .add(BlockRegistry.DEEPSLATE_ASTRALITE_ORE.get())
                .add(BlockRegistry.WARDED_SLATE.get())
                .add(BlockRegistry.FLUXITE.get());

        tag(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)
                .add(BlockRegistry.ASTRALITE_ORE.get())
                .add(BlockRegistry.DEEPSLATE_ASTRALITE_ORE.get());

        // ---- The Willow Fen ----
        tag(BlockTags.LEAVES)
                .add(BlockRegistry.WEEPING_LEAVES.get())
                .add(BlockRegistry.CYPRESS_NEEDLES.get());
        tag(BlockTags.MINEABLE_WITH_HOE)
                .add(BlockRegistry.WEEPING_LEAVES.get())
                .add(BlockRegistry.CYPRESS_NEEDLES.get())
                .add(BlockRegistry.FEN_MOSS.get());
        tag(BlockTags.MINEABLE_WITH_SHOVEL)
                .add(BlockRegistry.PEAT.get())
                .add(BlockRegistry.PEAT_BRICKS.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(BlockRegistry.MOSSY_FENSTONE.get())
                .add(BlockRegistry.FENSTONE_PILLAR.get())
                .add(BlockRegistry.FENSTONE.get())
                .add(BlockRegistry.FENSTONE_BRICKS.get())
                .add(BlockRegistry.MOSSY_FENSTONE_BRICKS.get())
                .add(BlockRegistry.FENSTONE_BRICK_STAIRS.get())
                .add(BlockRegistry.FENSTONE_BRICK_SLAB.get())
                .add(BlockRegistry.FENSTONE_BRICK_WALL.get());
        // Peat is soil: reeds, flowers, saplings and wispcaps all take root in it.
        tag(BlockTags.DIRT).add(BlockRegistry.PEAT.get());
        tag(BlockTags.STAIRS).add(BlockRegistry.FENSTONE_BRICK_STAIRS.get());
        tag(BlockTags.SLABS).add(BlockRegistry.FENSTONE_BRICK_SLAB.get());
        tag(BlockTags.WALLS).add(BlockRegistry.FENSTONE_BRICK_WALL.get());
        tag(BlockTags.SMALL_FLOWERS)
                .add(BlockRegistry.BOGBELL.get())
                .add(BlockRegistry.DUSKPETAL.get());
        tag(BlockTags.FLOWER_POTS)
                .add(BlockRegistry.POTTED_WISPCAP.get())
                .add(BlockRegistry.POTTED_BOGBELL.get())
                .add(BlockRegistry.POTTED_DUSKPETAL.get());
        // Undergrowth a growing tree may push through, the same as grass and vines.
        tag(BlockTags.REPLACEABLE_BY_TREES)
                .add(BlockRegistry.WHISPERREED.get())
                .add(BlockRegistry.FEN_MOSS.get())
                .add(BlockRegistry.BOGBELL.get())
                .add(BlockRegistry.DUSKPETAL.get())
                .add(BlockRegistry.WISPCAP.get());
        tag(BlockTags.SWORD_EFFICIENT)
                .add(BlockRegistry.WHISPERREED.get())
                .add(BlockRegistry.FEN_MOSS.get());
        tag(BlockTags.CLIMBABLE).add(BlockRegistry.FEN_MOSS.get());
    }
}
