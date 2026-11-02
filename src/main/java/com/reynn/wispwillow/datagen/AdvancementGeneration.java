package com.reynn.wispwillow.datagen;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.advancements.MilestoneTrigger;
import com.reynn.wispwillow.advancements.WWTriggers;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.EffectRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import com.reynn.wispwillow.worldgen.WWStructures;
import com.reynn.wispwillow.worldgen.structures.FluxiaOasisPiece;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.EffectsChangedTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.LootTableTrigger;
import net.minecraft.advancements.critereon.MobEffectsPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AdvancementGeneration extends AdvancementProvider {
    public AdvancementGeneration(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper) {
        super(output, registries, existingFileHelper, List.of(new MyAdvancementGenerator()));
    }

    private static final class MyAdvancementGenerator implements AdvancementProvider.AdvancementGenerator {

        @Override
        public void generate(HolderLookup.Provider provider, Consumer<AdvancementHolder> consumer, ExistingFileHelper existingFileHelper) {
            Advancement.Builder rootBuilder = Advancement.Builder.advancement();
            ItemStack patchouli_book = PatchouliAPI.get().getBookStack(ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "wispwillow_grimoire"));
            rootBuilder.display(
                    patchouli_book,
                    Component.translatable("advancements."+ WispWillow.MODID +".root.title"),
                    Component.translatable("advancements."+ WispWillow.MODID +".root.description"),
                    ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "textures/gui/advancements/backgrounds/magical_stone.png"),
                    AdvancementType.TASK,
                    true,
                    true,
                    false
            );

            rootBuilder.addCriterion("spawn_in", PlayerTrigger.TriggerInstance.tick());
            rootBuilder.requirements(AdvancementRequirements.allOf(List.of("spawn_in")));

            rootBuilder.save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "root"), existingFileHelper);

            // All methods follow the builder pattern, meaning that chaining is possible and encouraged.
            // For better readability of the explanations, chaining will not be done here.
            // Create an advancement builder using the static #advancement() method.
            // Using #advancement() automatically enables telemetry events. If you do not want this,
            // #recipeAdvancement() can be used instead, there are no other functional differences.
            Advancement.Builder builder = Advancement.Builder.advancement();

            // Sets the parent of the advancement. You can use another advancement you have already generated,
            // or create a placeholder advancement using the static AdvancementSubProvider#createPlaceholder method.
           // builder.parent(AdvancementSubProvider.createPlaceholder("wispwillow:story/root"));
            builder.parent(AdvancementSubProvider.createPlaceholder("wispwillow:root"));
            // Sets the display properties of the advancement. This can either be a DisplayInfo object,
            // or pass in the values directly. If values are passed in directly, a DisplayInfo object will be created for you.
            builder.display(
                    // The advancement icon. Can be an ItemStack or an ItemLike.
                    new ItemStack(BlockRegistry.WILLOW_SAPLING),
                    // The advancement title and description. Don't forget to add translations for these!
                    Component.translatable("advancements."+ WispWillow.MODID +".first_steps.title"),
                    Component.translatable("advancements."+ WispWillow.MODID +".first_steps.description"),
                    // The background texture. Use null if you don't want a background texture (for non-root advancements).
                    null,
                    // The frame type. Valid values are AdvancementType.TASK, CHALLENGE, or GOAL.
                    AdvancementType.GOAL,
                    // Whether to show the advancement toast or not.
                    true,
                    // Whether to announce the advancement into chat or not.
                    true,
                    // Whether the advancement should be hidden or not.
                    false
            );
            // An advancement reward builder. Can be created with any of the four reward types, and further rewards
            // can be added using the methods prefixed with add. This can also be built beforehand,
            // and the resulting AdvancementRewards can then be reused across multiple advancement builders.
            builder.rewards(
                    // Alternatively, use addExperience() to add to an existing builder.
                    AdvancementRewards.Builder.experience(100)
                            // Alternatively, use loot() to create a new builder.
                            //.addLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("minecraft", "chests/igloo")))
                            // Alternatively, use recipe() to create a new builder.
                            //.addRecipe(ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot"))
                            // Alternatively, use function() to create a new builder.
                            //.runs(ResourceLocation.fromNamespaceAndPath("examplemod", "example_function"))
            );
            // Adds a criterion with the given name to the advancement. Use the corresponding trigger instance's static method.
            builder.addCriterion("pickup_sapling", InventoryChangeTrigger.TriggerInstance.hasItems(BlockRegistry.WILLOW_SAPLING.get()));
            // Adds a requirements handler. Minecraft natively provides allOf() and anyOf(), more complex requirements
            // must be implemented manually. Only has an effect with two or more criteria.
            builder.requirements(AdvancementRequirements.allOf(List.of("pickup_sapling")));
            // Save the advancement to disk, using the given resource location. This returns an AdvancementHolder,
            // which may be stored in a variable and used as a parent by other advancement builders.
            builder.save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "first_steps"), existingFileHelper);

            Advancement.Builder  builder2 = Advancement.Builder.advancement();
            builder2.parent(AdvancementSubProvider.createPlaceholder("wispwillow:first_steps"));
            builder2.display(
                    new ItemStack(BlockRegistry.ALTAR.get()),
                    Component.translatable("advancements."+ WispWillow.MODID +".altar.title"),
                    Component.translatable("advancements."+ WispWillow.MODID +".altar.description"),
                    null,
                    AdvancementType.GOAL,
                    true,
                    true,
                    false
            );
            builder2.rewards(AdvancementRewards.Builder.experience(100));
            builder2.addCriterion("craft_altar", InventoryChangeTrigger.TriggerInstance.hasItems(BlockRegistry.ALTAR.get()));
            builder2.requirements(AdvancementRequirements.allOf(List.of("craft_altar")));
            builder2.save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "altar"), existingFileHelper);

            Advancement.Builder  builder3 = Advancement.Builder.advancement();
            builder3.parent(AdvancementSubProvider.createPlaceholder("wispwillow:altar"));
            builder3.display(
                    new ItemStack(ItemRegistry.CHALK.get(DyeColor.WHITE).get()),
                    Component.translatable("advancements."+ WispWillow.MODID +".chalk.title"),
                    Component.translatable("advancements."+ WispWillow.MODID +".chalk.description"),
                    null,
                    AdvancementType.GOAL,
                    true,
                    true,
                    false
            );
            builder3.rewards(AdvancementRewards.Builder.experience(100));
            builder3.addCriterion("craft_chalk", InventoryChangeTrigger.TriggerInstance.hasItems(ItemRegistry.CHALK.get(DyeColor.WHITE).get()));
            builder3.requirements(AdvancementRequirements.allOf(List.of("craft_chalk")));
            builder3.save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "chalk"), existingFileHelper);

            Advancement.Builder  builder4 = Advancement.Builder.advancement();
            builder4.parent(AdvancementSubProvider.createPlaceholder("wispwillow:altar"));
            builder4.display(
                    new ItemStack(ItemRegistry.FLUXIA_TABLET.get()),
                    Component.translatable("advancements."+ WispWillow.MODID +".tablet.title"),
                    Component.translatable("advancements."+ WispWillow.MODID +".tablet.description"),
                    null,
                    AdvancementType.GOAL,
                    true,
                    true,
                    false
            );
            builder4.rewards(AdvancementRewards.Builder.experience(100));
            builder4.addCriterion("craft_tablet", InventoryChangeTrigger.TriggerInstance.hasItems(ItemRegistry.FLUXIA_TABLET.get()));
            builder4.requirements(AdvancementRequirements.allOf(List.of("craft_tablet")));
            builder4.save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "tablet"), existingFileHelper);

            Advancement.Builder  builder5 = Advancement.Builder.advancement();
            builder5.parent(AdvancementSubProvider.createPlaceholder("wispwillow:chalk"));
            builder5.display(
                    new ItemStack(ItemRegistry.AMETHYST_COAL.get()),
                    Component.translatable("advancements."+ WispWillow.MODID +".m_coal.title"),
                    Component.translatable("advancements."+ WispWillow.MODID +".m_coal.description"),
                    null,
                    AdvancementType.GOAL,
                    true,
                    true,
                    false
            );
            builder5.rewards(AdvancementRewards.Builder.experience(100));
            builder5.addCriterion("ritual_coal", InventoryChangeTrigger.TriggerInstance.hasItems(ItemRegistry.AMETHYST_COAL.get()));
            builder5.requirements(AdvancementRequirements.allOf(List.of("ritual_coal")));
            builder5.save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "m_coal"), existingFileHelper);

            Advancement.Builder  builder6 = Advancement.Builder.advancement();
            builder6.parent(AdvancementSubProvider.createPlaceholder("wispwillow:chalk"));
            builder6.display(
                    new ItemStack(BlockRegistry.AMETHYST_FARMLAND.get()),
                    Component.translatable("advancements."+ WispWillow.MODID +".farm.title"),
                    Component.translatable("advancements."+ WispWillow.MODID +".farm.description"),
                    null,
                    AdvancementType.GOAL,
                    true,
                    true,
                    false
            );
            builder6.rewards(AdvancementRewards.Builder.experience(100));
            builder6.addCriterion("ritual_farm", InventoryChangeTrigger.TriggerInstance.hasItems(BlockRegistry.AMETHYST_FARMLAND.get()));
            builder6.requirements(AdvancementRequirements.allOf(List.of("ritual_farm")));
            builder6.save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "farm"), existingFileHelper);

            // Hangs off the tablet rather than the chalk: the cauldron is a Fluxia machine,
            // and a player who has not yet moved current around has nothing to light it with.
            Advancement.Builder builder7 = Advancement.Builder.advancement();
            builder7.parent(AdvancementSubProvider.createPlaceholder("wispwillow:tablet"));
            builder7.display(
                    new ItemStack(BlockRegistry.ALCHEMICAL_CAULDRON.get()),
                    Component.translatable("advancements."+ WispWillow.MODID +".cauldron.title"),
                    Component.translatable("advancements."+ WispWillow.MODID +".cauldron.description"),
                    null,
                    AdvancementType.GOAL,
                    true,
                    true,
                    false
            );
            builder7.rewards(AdvancementRewards.Builder.experience(100));
            builder7.addCriterion("has_cauldron",
                    InventoryChangeTrigger.TriggerInstance.hasItems(BlockRegistry.ALCHEMICAL_CAULDRON.get()));
            builder7.requirements(AdvancementRequirements.allOf(List.of("has_cauldron")));
            builder7.save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "cauldron"), existingFileHelper);

            empyreanArts(consumer, existingFileHelper);
            theOasis(consumer, provider, existingFileHelper);
        }

        // ==============================================================
        // The Empyrean Arts: the top tier, and what it grows into
        // ==============================================================

        private static void empyreanArts(Consumer<AdvancementHolder> consumer, ExistingFileHelper efh) {
            // Hangs off the cauldron because the cauldron is the only place empyrite comes from.
            AdvancementHolder empyrite = milestone("empyrite", "cauldron",
                    new ItemStack(ItemRegistry.EMPYRITE_INGOT.get()), AdvancementType.GOAL, 100, false)
                    .addCriterion("has_empyrite",
                            InventoryChangeTrigger.TriggerInstance.hasItems(ItemRegistry.EMPYRITE_INGOT.get()))
                    .save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "empyrite"), efh);

            AdvancementHolder awakening = milestone("awakening", "empyrite",
                    new ItemStack(ItemRegistry.EMPYRITE_SWORD.get()), AdvancementType.GOAL, 200, false)
                    .addCriterion("rite", MilestoneTrigger.TriggerInstance.reached(WWTriggers.AWAKENED))
                    .save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "awakening"), efh);

            milestone("imbuing", "awakening",
                    new ItemStack(ItemRegistry.EMPYRITE_PICKAXE.get()), AdvancementType.TASK, 200, false)
                    .addCriterion("rite", MilestoneTrigger.TriggerInstance.reached(WWTriggers.IMBUED))
                    .save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "imbuing"), efh);

            // Both challenges: one is a long haul, the other is a full set of the rite.
            milestone("resonance", "awakening",
                    new ItemStack(ItemRegistry.EMPYRITE_AXE.get()), AdvancementType.CHALLENGE, 500, false)
                    .addCriterion("resonant", MilestoneTrigger.TriggerInstance.reached(WWTriggers.RESONANCE_V))
                    .save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "resonance"), efh);

            milestone("regalia", "awakening",
                    new ItemStack(ItemRegistry.EMPYRITE_CHESTPLATE.get()), AdvancementType.CHALLENGE, 500, false)
                    .addCriterion("worn", MilestoneTrigger.TriggerInstance.reached(WWTriggers.REGALIA))
                    .save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "regalia"), efh);

            // Silence the unused warning in a way that also documents the shape of the tree.
            if (empyrite == null || awakening == null) throw new IllegalStateException();
        }

        // ==============================================================
        // The Oasis: phasing, finding one, and what is under the pool
        // ==============================================================

        private static void theOasis(Consumer<AdvancementHolder> consumer, HolderLookup.Provider provider,
                                     ExistingFileHelper efh) {
            milestone("phasing", "tablet",
                    new ItemStack(ItemRegistry.FLUXIA_FLUID_BUCKET.get()), AdvancementType.TASK, 100, false)
                    .addCriterion("unmoored", EffectsChangedTrigger.TriggerInstance.hasEffects(
                            MobEffectsPredicate.Builder.effects().and(EffectRegistry.PHASING)))
                    .save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "phasing"), efh);

            Holder<Structure> oasis = provider.lookupOrThrow(Registries.STRUCTURE)
                    .getOrThrow(WWStructures.FLUXIA_OASIS);

            milestone("oasis", "phasing",
                    new ItemStack(BlockRegistry.WARDED_SLATE.get()), AdvancementType.GOAL, 200, false)
                    .addCriterion("found", PlayerTrigger.TriggerInstance.located(
                            LocationPredicate.Builder.inStructure(oasis)))
                    .save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "oasis"), efh);

            // Hidden: the whole point of the vault is that nothing tells you it is down there.
            milestone("oasis_cache", "oasis",
                    new ItemStack(Items.CHEST), AdvancementType.CHALLENGE, 500, true)
                    .addCriterion("opened", LootTableTrigger.TriggerInstance.lootTableUsed(FluxiaOasisPiece.CACHE))
                    .save(consumer, ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "oasis_cache"), efh);
        }

        /** The boilerplate every advancement in these two branches shares. */
        private static Advancement.Builder milestone(String name, String parent, ItemStack icon,
                                                     AdvancementType type, int experience, boolean hidden) {
            Advancement.Builder builder = Advancement.Builder.advancement();
            builder.parent(AdvancementSubProvider.createPlaceholder(WispWillow.MODID + ":" + parent));
            builder.display(
                    icon,
                    Component.translatable("advancements." + WispWillow.MODID + "." + name + ".title"),
                    Component.translatable("advancements." + WispWillow.MODID + "." + name + ".description"),
                    null,
                    type,
                    true,
                    true,
                    hidden
            );
            builder.rewards(AdvancementRewards.Builder.experience(experience));
            return builder;
        }
    }
}
