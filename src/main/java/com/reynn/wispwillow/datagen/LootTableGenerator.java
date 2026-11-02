package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.blocks.RuneType;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;

public class LootTableGenerator extends BlockLootSubProvider {

    public LootTableGenerator(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, provider);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return BlockRegistry.BLOCKS.getEntries()
                .stream()
                .map(e -> (Block) e.value())
                .toList();
    }
    private static final float[] NORMAL_LEAVES_CHANCES = new float[]{0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F};

    /**
     * Shears or silk touch for the leaves themselves, otherwise sticks — and never a sapling.
     *
     * <p>The fen's trees grow wild. Willow saplings are meant to come from the infusion rite and
     * nowhere else, so these leaves deliberately have no sapling pool at all.</p>
     */
    private void leavesWithSticksOnly(Block leaves) {
        add(leaves, createSilkTouchOrShearsDispatchTable(leaves,
                applyExplosionDecay(leaves, LootItem.lootTableItem(Items.STICK)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .when(BonusLevelTableCondition.bonusLevelFlatChance(
                                registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),
                                0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F))));
    }

    @Override
    protected void generate() {
        // Foundation runes are expensive to infuse; they always come back.
        for (RuneType rune : RuneType.values()) dropSelf(BlockRegistry.RUNES.get(rune).get());
        dropSelf(BlockRegistry.VERDANT_BLOOM.get());
        dropSelf(BlockRegistry.GATHERING_BLOOM.get());
        dropSelf(BlockRegistry.WARDING_BLOOM.get());

        dropSelf(BlockRegistry.WILLOW_BUTTON.get());
        dropSelf(BlockRegistry.WILLOW_SAPLING.get());
        dropSelf(BlockRegistry.WILLOW_PLANKS.get());
        dropSelf(BlockRegistry.WILLOW_DOOR.get());
        dropSelf(BlockRegistry.WILLOW_TRAPDOOR.get());
        dropSelf(BlockRegistry.WILLOW_LOG.get());
        dropSelf(BlockRegistry.WILLOW_FENCE_GATE.get());
        dropSelf(BlockRegistry.WILLOW_SLAB.get());
        dropSelf(BlockRegistry.WILLOW_FENCE.get());
        dropSelf(BlockRegistry.WILLOW_PRESSURE_PLATE.get());
        dropSelf(BlockRegistry.WILLOW_STAIRS.get());
        dropSelf(BlockRegistry.STRIPPED_WILLOW_LOG.get());
        dropSelf(BlockRegistry.AMETHYST_COAL_BLOCK.get());
        dropSelf(BlockRegistry.FLUXITE.get());
        dropSelf(BlockRegistry.WARDED_SLATE.get());
        dropSelf(BlockRegistry.CHUNK_LOADER.get());
        dropSelf(BlockRegistry.ASTRALITE_BLOCK.get());
        dropPottedContents(BlockRegistry.POTTED_LUXINIAS_FLOWER.get());
        add(BlockRegistry.SIGIL.get(), noDrop());
        dropSelf(BlockRegistry.FLUXIA_SPITTER.get());
        dropSelf(BlockRegistry.FLUXIA_DYNAMO.get());
        dropSelf(BlockRegistry.ASTRAL_FONT.get());
        dropSelf(BlockRegistry.GATEWAY_CORE.get());
        dropSelf(BlockRegistry.ALCHEMICAL_CAULDRON.get());
        dropSelf(BlockRegistry.EMPYRITE_BLOCK.get());
        // No entry for GATEWAY_PORTAL on purpose: it declares noLootTable(), so its table id
        // is minecraft:empty and the provider skips it. Registering one anyway leaves an
        // orphan in the map and the generator fails with "loot tables for non-blocks".
        dropSelf(BlockRegistry.SNAD.get());
        dropOther(BlockRegistry.AMETHYST_FARMLAND.get(), Blocks.DIRT);
        dropWhenSilkTouch(BlockRegistry.AMETHYST_FARMLAND.get());
        dropSelf(BlockRegistry.AMETHYST_SUGAR_CANE.get());
        dropSelf(BlockRegistry.WILLOW_TORCH.get());
        dropSelf(BlockRegistry.WILLOW_TORCH_WALL.get());

        dropSelf(BlockRegistry.WILLOW_LANTERN.get());

        dropSelf(BlockRegistry.LUXINIAS_FLOWER.get());
        dropSelf(BlockRegistry.SOLROSA_FLOWER.get());

        dropSelf(BlockRegistry.ALTAR.get());
        dropSelf(BlockRegistry.FLUXIA_LAMP.get());
        dropSelf(BlockRegistry.MANA_POOL.get());
        dropSelf(BlockRegistry.CREATIVE_MANA_POOL.get());
        createLeavesDrops(BlockRegistry.WILLOW_LEAVES.get(), BlockRegistry.WILLOW_SAPLING.get());
        createLeavesDrops(BlockRegistry.FLOWERING_WILLOW_LEAVES.get(), BlockRegistry.WILLOW_SAPLING.get());
        dropWhenSilkTouch(BlockRegistry.WILLOW_VINE.get());
        dropWhenSilkTouch(BlockRegistry.WILLOW_LEAVES.get());
        dropWhenSilkTouch(BlockRegistry.FLOWERING_WILLOW_LEAVES.get());

        // ---- The Willow Fen ----
        // Weeping leaves are the one leaf in the mod that must never drop a sapling: they grow
        // wild, and the willow sapling is only ever meant to come from the infusion rite.
        leavesWithSticksOnly(BlockRegistry.WEEPING_LEAVES.get());
        leavesWithSticksOnly(BlockRegistry.CYPRESS_NEEDLES.get());
        // Soulwood has no sapling either: the realm is visited, not farmed.
        leavesWithSticksOnly(BlockRegistry.SOULWOOD_LEAVES.get());
        dropSelf(BlockRegistry.SOULWOOD_LOG.get());
        dropSelf(BlockRegistry.STRIPPED_SOULWOOD_LOG.get());
        dropSelf(BlockRegistry.SOULWOOD_PLANKS.get());
        dropSelf(BlockRegistry.SOULWOOD_STAIRS.get());
        dropSelf(BlockRegistry.SOULWOOD_FENCE.get());
        dropSelf(BlockRegistry.SOULWOOD_FENCE_GATE.get());
        dropSelf(BlockRegistry.SOULWOOD_TRAPDOOR.get());
        dropSelf(BlockRegistry.SOULWOOD_BUTTON.get());
        dropSelf(BlockRegistry.SOULWOOD_PRESSURE_PLATE.get());
        // Not dropSelf for these two. A door is two blocks and would drop twice over; a double
        // slab is one block holding two, and would drop half of what it cost.
        add(BlockRegistry.SOULWOOD_DOOR.get(), createDoorTable(BlockRegistry.SOULWOOD_DOOR.get()));
        add(BlockRegistry.SOULWOOD_SLAB.get(), createSlabItemTable(BlockRegistry.SOULWOOD_SLAB.get()));
        dropSelf(BlockRegistry.GLIMMERBLOOM.get());
        dropSelf(BlockRegistry.VEILCAP.get());
        dropSelf(BlockRegistry.MANABLOOM.get());
        dropSelf(BlockRegistry.EMBERBLOOM.get());
        dropSelf(BlockRegistry.DUSKBLOOM.get());
        dropSelf(BlockRegistry.SPIRITSTONE.get());
        dropSelf(BlockRegistry.SPIRITSTONE_BRICKS.get());
        add(BlockRegistry.FEN_MOSS.get(), createShearsOnlyDrop(BlockRegistry.FEN_MOSS.get()));
        add(BlockRegistry.WHISPERREED.get(), createSinglePropConditionTable(BlockRegistry.WHISPERREED.get(),
                DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        dropSelf(BlockRegistry.MOONLILY.get());
        dropSelf(BlockRegistry.WISPCAP.get());
        dropSelf(BlockRegistry.BOGBELL.get());
        dropSelf(BlockRegistry.DUSKPETAL.get());
        dropPottedContents(BlockRegistry.POTTED_WISPCAP.get());
        dropPottedContents(BlockRegistry.POTTED_BOGBELL.get());
        dropPottedContents(BlockRegistry.POTTED_DUSKPETAL.get());
        dropSelf(BlockRegistry.PEAT.get());
        dropSelf(BlockRegistry.FENSTONE.get());
        dropSelf(BlockRegistry.FENSTONE_BRICKS.get());
        dropSelf(BlockRegistry.MOSSY_FENSTONE_BRICKS.get());
        dropSelf(BlockRegistry.FENSTONE_BRICK_STAIRS.get());
        dropSelf(BlockRegistry.FENSTONE_BRICK_WALL.get());
        add(BlockRegistry.FENSTONE_BRICK_SLAB.get(), this::createSlabItemTable);
        dropSelf(BlockRegistry.WILLOW_AMBER.get());
        dropSelf(BlockRegistry.MOSSY_FENSTONE.get());
        dropSelf(BlockRegistry.FENSTONE_PILLAR.get());
        dropSelf(BlockRegistry.PEAT_BRICKS.get());

        add(BlockRegistry.DEEPSLATE_ASTRALITE_ORE.get(), createOreDrop(BlockRegistry.DEEPSLATE_ASTRALITE_ORE.get(), ItemRegistry.RAW_ASTRALITE.get()));
        add(BlockRegistry.ASTRALITE_ORE.get(), createOreDrop(BlockRegistry.ASTRALITE_ORE.get(), ItemRegistry.RAW_ASTRALITE.get()));
    }
}
