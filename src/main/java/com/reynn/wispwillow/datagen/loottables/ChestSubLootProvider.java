package com.reynn.wispwillow.datagen.loottables;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

public class ChestSubLootProvider implements LootTableSubProvider {
    // The parameter is provided by the lambda (see below). It can be stored and used to lookup other registry entries.
    public ChestSubLootProvider(HolderLookup.Provider lookupProvider) {
        // Store the lookupProvider in a field
    }


    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> biConsumer) {
        // LootTable.lootTable() returns a loot table builder we can add loot tables to.
        biConsumer.accept(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "willow_hut_chest")), LootTable.lootTable()
                                .withPool(LootPool.lootPool()
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                                        .setRolls(UniformGenerator.between(0.0F, 1.0F))
                                        .add(LootItem.lootTableItem(ItemRegistry.FLUXIA_TABLET))
                                        .when(LootItemRandomChanceCondition.randomChance(0.35f))
                                )
                // Add a loot table-level loot function. This example uses a number provider (see below).
                // Add a loot pool.
                .withPool(LootPool.lootPool()
                        // Add a loot pool-level function, similar to above.
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2,6), true))
                        // Set the amount of rolls and bonus rolls, respectively.
                        // Both of these methods utilize a number provider.
                        .setRolls(UniformGenerator.between(4, 6))
                        .setBonusRolls(ConstantValue.exactly(1))
                        // Add a loot entry. This example returns an item loot entry. See below for more loot entries.
                        .add(LootItem.lootTableItem(BlockRegistry.WILLOW_LOG).setWeight(4))
                        .add(LootItem.lootTableItem(Items.COBWEB).setWeight(2))
                        .add(LootItem.lootTableItem(Items.BREAD).setWeight(3))
                        .add(LootItem.lootTableItem(Items.WHEAT).setWeight(2))
                        .add(LootItem.lootTableItem(BlockRegistry.STRIPPED_WILLOW_LOG).setWeight(4))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2,6), true))

                )

        );

        // The Fluxia Oasis vault. Both chests roll the same table: what the puzzle pays out is
        // a running start on Fluxia, not one jackpot item sitting in the left-hand chest.
        biConsumer.accept(ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "fluxia_oasis_cache")),
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .setRolls(UniformGenerator.between(3, 5))
                                .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(8)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                                .add(LootItem.lootTableItem(BlockRegistry.FLUXITE).setWeight(6)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                                .add(LootItem.lootTableItem(ItemRegistry.AMETHYST_COAL).setWeight(5)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5))))
                                .add(LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE).setWeight(5)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                                .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(4)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5))))
                                .add(LootItem.lootTableItem(ItemRegistry.DUSTCONVERT).setWeight(4)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                                .add(LootItem.lootTableItem(ItemRegistry.ASTRALITE_INGOT).setWeight(4)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))))
                                .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(3)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                        )
                        .withPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1))
                                .add(LootItem.lootTableItem(ItemRegistry.FLUXIA_TABLET))
                                .when(LootItemRandomChanceCondition.randomChance(0.30f))
                        )
                        .withPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1))
                                .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE))
                                .when(LootItemRandomChanceCondition.randomChance(0.08f))
                        )
        );

        // A sunken fen ruin: the things a marsh keeps, and now and then a tablet someone lost.
        biConsumer.accept(key("fen_ruin_cache"),
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .setRolls(UniformGenerator.between(4, 7))
                                .add(LootItem.lootTableItem(ItemRegistry.WILLOW_SAP_BOTTLE).setWeight(6)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                                .add(LootItem.lootTableItem(BlockRegistry.PEAT).setWeight(6)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                                .add(LootItem.lootTableItem(BlockRegistry.WISPCAP).setWeight(5)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                                .add(LootItem.lootTableItem(BlockRegistry.MOONLILY).setWeight(4)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                                .add(LootItem.lootTableItem(BlockRegistry.MOSSY_FENSTONE_BRICKS).setWeight(4)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5))))
                                .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(4)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                                .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(4)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 9))))
                                .add(LootItem.lootTableItem(ItemRegistry.AMETHYST_COAL).setWeight(3)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                                .add(LootItem.lootTableItem(Items.EMERALD).setWeight(2)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                                .add(LootItem.lootTableItem(Items.NAME_TAG).setWeight(1))
                        )
                        .withPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1))
                                .add(LootItem.lootTableItem(ItemRegistry.FLUXIA_TABLET))
                                .when(LootItemRandomChanceCondition.randomChance(0.15f))
                        )
        );

        // What was left on a fen shrine: candles, flowers, a little sap and amber.
        biConsumer.accept(key("fen_shrine_offering"),
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2, 4))
                                .add(LootItem.lootTableItem(Items.CANDLE).setWeight(4)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                                .add(LootItem.lootTableItem(ItemRegistry.WILLOW_SAP_BOTTLE).setWeight(4)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                                .add(LootItem.lootTableItem(BlockRegistry.DUSKPETAL).setWeight(3)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                                .add(LootItem.lootTableItem(BlockRegistry.BOGBELL).setWeight(3)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                                .add(LootItem.lootTableItem(Items.GLOW_BERRIES).setWeight(3)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5))))
                                .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(3)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                                .add(LootItem.lootTableItem(BlockRegistry.WILLOW_AMBER).setWeight(2)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                                .add(LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE).setWeight(2)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        )
        );
    }

    private static ResourceKey<LootTable> key(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, name));
    }
}