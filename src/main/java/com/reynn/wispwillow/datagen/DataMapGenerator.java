package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.concurrent.CompletableFuture;

public class DataMapGenerator extends DataMapProvider {
    protected DataMapGenerator(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        this.builder(NeoForgeDataMaps.FURNACE_FUELS)
                .add(ItemRegistry.AMETHYST_COAL.getId(), new FurnaceFuel(3200), false)
                .add(BlockRegistry.AMETHYST_COAL_BLOCK.getId(), new FurnaceFuel(32000), false)
                // Peat smoulders: four items to a block.
                .add(BlockRegistry.PEAT.getId(), new FurnaceFuel(800), false)
                // Cut and dried, it burns twice as long as the raw sod.
                .add(BlockRegistry.PEAT_BRICKS.getId(), new FurnaceFuel(1600), false);
    }
}
