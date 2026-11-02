package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import com.reynn.wispwillow.registry.TagRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ItemTagProvider extends ItemTagsProvider {

    public ItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, WispWillow.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ItemTags.FLOWERS)
                .add(BlockRegistry.SOLROSA_FLOWER.get().asItem())
                .add(BlockRegistry.LUXINIAS_FLOWER.get().asItem());
        tag(ItemTags.PLANKS)
                .add(BlockRegistry.WILLOW_PLANKS.get().asItem());
        tag(ItemRegistry.WILLOW_LOGS_TAG)
                .add(BlockRegistry.STRIPPED_WILLOW_LOG.get().asItem())
                .add(BlockRegistry.WILLOW_LOG.get().asItem());

        tag(TagRegistry.Items.BOTANY_POT_SOIL)
                .add(BlockRegistry.SNAD.get().asItem())
                .add(BlockRegistry.AMETHYST_FARMLAND.get().asItem());

        // Empyrite gear has to be in the vanilla tool and armour tags or it is barely
        // enchantable and cannot be trimmed at all. Every 1.21 enchantment declares the
        // items it supports as a tag, and those all resolve back to these nine:
        // #enchantable/sword -> #swords, #enchantable/mining -> #pickaxes|#axes|#shovels|
        // #hoes, #enchantable/armor and #enchantable/durability -> the four armour tags.
        // #minecraft:trimmable_armor is likewise just the four armour tags, so smithing
        // trims start applying off the back of the same change. NeoForge builds its own
        // c:tools and c:armors from these too, so nothing else needs listing here.
        tag(ItemTags.SWORDS).add(ItemRegistry.EMPYRITE_SWORD.get());
        tag(ItemTags.PICKAXES).add(ItemRegistry.EMPYRITE_PICKAXE.get());
        tag(ItemTags.AXES).add(ItemRegistry.EMPYRITE_AXE.get());
        tag(ItemTags.SHOVELS).add(ItemRegistry.EMPYRITE_SHOVEL.get());
        tag(ItemTags.HOES).add(ItemRegistry.EMPYRITE_HOE.get());
        tag(ItemTags.HEAD_ARMOR).add(ItemRegistry.EMPYRITE_HELMET.get());
        tag(ItemTags.CHEST_ARMOR).add(ItemRegistry.EMPYRITE_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR).add(ItemRegistry.EMPYRITE_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(ItemRegistry.EMPYRITE_BOOTS.get());
        tag(ItemTags.HEAD_ARMOR).add(ItemRegistry.GLIMMERWEAVE_HOOD.get());
        tag(ItemTags.CHEST_ARMOR).add(ItemRegistry.GLIMMERWEAVE_ROBE.get());
        tag(ItemTags.LEG_ARMOR).add(ItemRegistry.GLIMMERWEAVE_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(ItemRegistry.GLIMMERWEAVE_SLIPPERS.get());

        // The cloth set, for the set bonus. (The bolt that mends it is the material's repair
        // ingredient, see ArmorMaterialRegistry.GLIMMERWEAVE.)
        tag(TagRegistry.Items.GLIMMERWEAVE_ARMOR)
                .add(ItemRegistry.GLIMMERWEAVE_HOOD.get())
                .add(ItemRegistry.GLIMMERWEAVE_ROBE.get())
                .add(ItemRegistry.GLIMMERWEAVE_LEGGINGS.get())
                .add(ItemRegistry.GLIMMERWEAVE_SLIPPERS.get());

        // Everything the Awakening Empyrite rite will take. Kept as a tag so a pack can
        // widen it without editing the rite.
        tag(TagRegistry.Items.EMPYRITE)
                .add(ItemRegistry.EMPYRITE_INGOT.get())
                .add(ItemRegistry.EMPYRITE_NUGGET.get())
                .add(ItemRegistry.EMPYRITE_SWORD.get())
                .add(ItemRegistry.EMPYRITE_PICKAXE.get())
                .add(ItemRegistry.EMPYRITE_AXE.get())
                .add(ItemRegistry.EMPYRITE_SHOVEL.get())
                .add(ItemRegistry.EMPYRITE_HOE.get())
                .add(ItemRegistry.EMPYRITE_HELMET.get())
                .add(ItemRegistry.EMPYRITE_CHESTPLATE.get())
                .add(ItemRegistry.EMPYRITE_LEGGINGS.get())
                .add(ItemRegistry.EMPYRITE_BOOTS.get())
                .add(BlockRegistry.EMPYRITE_BLOCK.get().asItem());

        // ---- The Willow Fen ----
        tag(ItemTags.SMALL_FLOWERS)
                .add(BlockRegistry.BOGBELL.get().asItem())
                .add(BlockRegistry.DUSKPETAL.get().asItem());
        tag(ItemTags.LEAVES)
                .add(BlockRegistry.WEEPING_LEAVES.get().asItem())
                .add(BlockRegistry.CYPRESS_NEEDLES.get().asItem())
                .add(BlockRegistry.SOULWOOD_LEAVES.get().asItem());
        tag(ItemTags.LOGS)
                .add(BlockRegistry.SOULWOOD_LOG.get().asItem())
                .add(BlockRegistry.STRIPPED_SOULWOOD_LOG.get().asItem());
        tag(ItemTags.LOGS_THAT_BURN)
                .add(BlockRegistry.SOULWOOD_LOG.get().asItem())
                .add(BlockRegistry.STRIPPED_SOULWOOD_LOG.get().asItem());
        // The recipe ingredient: planksFromLog resolves against this, so both the log and its
        // stripped form give planks.
        tag(ItemRegistry.SOULWOOD_LOGS_TAG)
                .add(BlockRegistry.SOULWOOD_LOG.get().asItem())
                .add(BlockRegistry.STRIPPED_SOULWOOD_LOG.get().asItem());

        tag(ItemTags.PLANKS).add(BlockRegistry.SOULWOOD_PLANKS.get().asItem());
        tag(ItemTags.WOODEN_STAIRS).add(BlockRegistry.SOULWOOD_STAIRS.get().asItem());
        tag(ItemTags.WOODEN_SLABS).add(BlockRegistry.SOULWOOD_SLAB.get().asItem());
        tag(ItemTags.WOODEN_FENCES).add(BlockRegistry.SOULWOOD_FENCE.get().asItem());
        tag(ItemTags.WOODEN_DOORS).add(BlockRegistry.SOULWOOD_DOOR.get().asItem());
        tag(ItemTags.WOODEN_TRAPDOORS).add(BlockRegistry.SOULWOOD_TRAPDOOR.get().asItem());
        tag(ItemTags.WOODEN_BUTTONS).add(BlockRegistry.SOULWOOD_BUTTON.get().asItem());
        tag(ItemTags.WOODEN_PRESSURE_PLATES).add(BlockRegistry.SOULWOOD_PRESSURE_PLATE.get().asItem());

        tag(ItemTags.SMALL_FLOWERS)
                .add(BlockRegistry.GLIMMERBLOOM.get().asItem())
                .add(BlockRegistry.VEILCAP.get().asItem())
                .add(BlockRegistry.MANABLOOM.get().asItem())
                .add(BlockRegistry.EMBERBLOOM.get().asItem())
                .add(BlockRegistry.DUSKBLOOM.get().asItem());
        tag(ItemTags.STAIRS).add(BlockRegistry.FENSTONE_BRICK_STAIRS.get().asItem());
        tag(ItemTags.SLABS).add(BlockRegistry.FENSTONE_BRICK_SLAB.get().asItem());
        tag(ItemTags.WALLS).add(BlockRegistry.FENSTONE_BRICK_WALL.get().asItem());
    }
}
