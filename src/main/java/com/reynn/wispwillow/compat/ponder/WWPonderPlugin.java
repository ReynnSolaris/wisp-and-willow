package com.reynn.wispwillow.compat.ponder;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.blocks.RuneType;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import com.reynn.wispwillow.ritual.RitualDefinitions;
import net.createmod.ponder.api.registration.MultiTagBuilder;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Wisp &amp; Willow's scenes, for anyone who has Ponder — which is anyone who has Create.
 *
 * <p>Scenes hang off whatever a player is holding when they wonder how something works: the
 * altar, chalk and runes for casting; each rite's grimoire icon for its circle; the cauldron and
 * the gateway core for what gets built around them.</p>
 */
public final class WWPonderPlugin implements PonderPlugin {

    private static final ResourceLocation RITUALS = PonderText.id(PonderText.TAG_RITUALS);
    private static final ResourceLocation STRUCTURES = PonderText.id(PonderText.TAG_STRUCTURES);

    @Override
    public String getModId() {
        return WispWillow.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemLike> items = helper.withKeyFunction(WWPonderPlugin::key);

        items.forComponents(BlockRegistry.ALTAR.get())
                .addStoryBoard(PonderLayouts.ALTAR_CASTING, AltarScenes::casting, RITUALS)
                .addStoryBoard(PonderLayouts.ALTAR_FOUNDATION, AltarScenes::foundation, RITUALS);
        items.forComponents(chalk())
                .addStoryBoard(PonderLayouts.ALTAR_CASTING, AltarScenes::casting, RITUALS);
        items.forComponents(runes())
                .addStoryBoard(PonderLayouts.ALTAR_FOUNDATION, AltarScenes::foundation, RITUALS);

        // Each rite under the item the grimoire files it by, so hovering what a rite makes
        // shows how to make it.
        for (RitualDefinitions.Def def : RitualDefinitions.all()) {
            items.forComponents(def.icon().getItem())
                    .addStoryBoard(PonderLayouts.ritualId(def), RitualScenes.of(def), RITUALS);
        }

        items.forComponents(BlockRegistry.ALCHEMICAL_CAULDRON.get())
                .addStoryBoard(PonderLayouts.CAULDRON, CauldronScenes::build, STRUCTURES);
        items.forComponents(BlockRegistry.GATEWAY_CORE.get(), ItemRegistry.SEALED_SOUL_PACT.get())
                .addStoryBoard(PonderLayouts.GATEWAY, GatewayScenes::build, STRUCTURES);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(PonderText.TAG_RITUALS)
                .addToIndex()
                .item(BlockRegistry.ALTAR.get(), true, false)
                .title(PonderText.tagTitle(PonderText.TAG_RITUALS))
                .description(PonderText.tagDescription(PonderText.TAG_RITUALS))
                .register();
        helper.registerTag(PonderText.TAG_STRUCTURES)
                .addToIndex()
                .item(BlockRegistry.GATEWAY_CORE.get(), true, false)
                .title(PonderText.tagTitle(PonderText.TAG_STRUCTURES))
                .description(PonderText.tagDescription(PonderText.TAG_STRUCTURES))
                .register();

        PonderTagRegistrationHelper<ItemLike> items = helper.withKeyFunction(WWPonderPlugin::key);

        // A set, because several rites share an icon and a tag lists each item once.
        Set<Item> rituals = new LinkedHashSet<>();
        rituals.add(BlockRegistry.ALTAR.get().asItem());
        rituals.add(ItemRegistry.CHALK.get(DyeColor.WHITE).get());
        rituals.add(BlockRegistry.RUNES.get(RuneType.BLANK).get().asItem());
        for (RitualDefinitions.Def def : RitualDefinitions.all()) rituals.add(def.icon().getItem());
        MultiTagBuilder.Tag<ItemLike> ritualTag = items.addToTag(RITUALS);
        rituals.forEach(ritualTag::add);

        items.addToTag(STRUCTURES)
                .add(BlockRegistry.ALTAR.get())
                .add(BlockRegistry.ALCHEMICAL_CAULDRON.get())
                .add(BlockRegistry.GATEWAY_CORE.get());
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        // Only Ponder's editing mode reads these; everywhere else the language file does.
        PonderText.shared().forEach(helper::registerSharedText);
    }

    private static ResourceLocation key(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem());
    }

    private static List<Item> chalk() {
        return ItemRegistry.CHALK.values().stream().map(DeferredItem::get).toList();
    }

    private static List<Item> runes() {
        return BlockRegistry.RUNES.values().stream().map(b -> b.get().asItem()).toList();
    }
}
