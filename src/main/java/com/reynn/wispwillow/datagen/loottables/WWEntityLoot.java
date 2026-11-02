package com.reynn.wispwillow.datagen.loottables;

import com.reynn.wispwillow.registry.EntityRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.stream.Stream;

/**
 * Drops for the wild creatures.
 *
 * <p>Familiars are not listed: a familiar is bound, not hunted, and has no table. The provider
 * only checks the entity types named in {@link #getKnownEntityTypes()}, which is what lets
 * them go without one.</p>
 */
public class WWEntityLoot extends EntityLootSubProvider {

    public WWEntityLoot(HolderLookup.Provider registries) {
        super(FeatureFlags.DEFAULT_FLAGS, registries);
    }

    @Override
    public void generate() {
        add(EntityRegistry.FEN_HERON.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.FEATHER)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0.0F, 1.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.4F))
                        .add(LootItem.lootTableItem(Items.COD)
                                .apply(SmeltItemFunction.smelted().when(shouldSmeltLoot())))));

        add(EntityRegistry.WISPFLY.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.GLOWSTONE_DUST)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0.0F, 1.0F))))));

        // A hound comes apart into what it was woven from. Deliberately poor: killing your own
        // companion for two sticks should never look like a sensible trade.
        add(EntityRegistry.WILLOW_HOUND.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.STICK)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0.0F, 1.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.25F))
                        .add(LootItem.lootTableItem(ItemRegistry.WILLOW_SAP_BOTTLE.get()))));

        // Less down than shearing gives, and the fawn does not come back. Shears are the living way.
        add(EntityRegistry.GLIMMERFAWN.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ItemRegistry.SPIRIT_DOWN.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F)))
                                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0.0F, 1.0F))))));
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return Stream.of(
                EntityRegistry.WISPFLY.get(),
                EntityRegistry.FEN_HERON.get(),
                EntityRegistry.WILLOW_HOUND.get(),
                EntityRegistry.GLIMMERFAWN.get());
    }
}
