package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.datagen.book.PatchouliBookProvider;
import com.reynn.wispwillow.datagen.loottables.ChestSubLootProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Collections;
import java.util.List;

@EventBusSubscriber(modid = WispWillow.MODID)
public class WWDataGen {
    @SubscribeEvent
    public static void gather(GatherDataEvent e) {
        var gen = e.getGenerator();
        var efh = e.getExistingFileHelper();
        var output = gen.getPackOutput();
        var lookupProvider = e.getLookupProvider();
        var includeServer = e.includeServer();
        // Blockstates & models
        gen.addProvider(includeServer, new BlockModelGenerator(output, efh));
        gen.addProvider(e.includeClient(), new WWBlockStateProvider(output, efh));
        // The datapack provider comes first and its lookup is what the advancements are built
        // against: the mod's own structures live in it, and "find a Fluxia Oasis" needs a
        // holder for one. Everything else is happy with the plain vanilla lookup.
        var datapackProvider = new DatapackProvider(output, lookupProvider);
        gen.addProvider(includeServer, datapackProvider);
        gen.addProvider(includeServer, new AdvancementGeneration(output, datapackProvider.getRegistryProvider(), efh));
        gen.addProvider(e.includeClient(), new WWItemModelProvider(output, efh));
        gen.addProvider(includeServer, new ParticleProvider(output, efh));
        // Built against the datapack provider's lookup, not the plain one: the fen biome the
        // biome tags name lives in the mod's own registry patch, and a tag provider refuses to
        // define a tag whose entries its lookup has never heard of.
        gen.addProvider(includeServer, new BiomeTagProvider(output, datapackProvider.getRegistryProvider(), efh));
        gen.addProvider(includeServer, new WWSoundProvider(output, efh));
        // Loot & recipes
       // gen.addProvider(true, new WWBlockLootProvider(output));
       // gen.addProvider(true, new WWRecipeProvider(output));
        gen.addProvider(includeServer, new DataMapGenerator(output, lookupProvider));
        gen.addProvider(includeServer , new LootTableProvider(output, Collections.emptySet(),
                List.of(
                        new LootTableProvider.SubProviderEntry(
                                ChestSubLootProvider::new,
                                LootContextParamSets.CHEST
                        ),
                        new LootTableProvider.SubProviderEntry(LootTableGenerator::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(com.reynn.wispwillow.datagen.loottables.WWEntityLoot::new, LootContextParamSets.ENTITY)
                ), lookupProvider));
        BlockTagsProvider blockTagsProvider = gen.addProvider(includeServer, new BlockTagProvider(output, lookupProvider, efh));
        gen.addProvider(includeServer, new ItemTagProvider(output, lookupProvider, blockTagsProvider.contentsGetter(), efh));

        gen.addProvider(includeServer, new WWRecipeProvider(output, lookupProvider));

        // The Patchouli grimoire: book.json is data, the pages are assets, and the ritual
        // entries are derived from the same table the recipes come from.
        gen.addProvider(includeServer || e.includeClient(), new PatchouliBookProvider(output));

        // The structures the Ponder scenes stand on, from the same tables the blocks check.
        gen.addProvider(e.includeClient(), new PonderSchematicProvider(output));

        // Lang
        gen.addProvider(includeServer, new WWLangEnUS(output));

    }
}