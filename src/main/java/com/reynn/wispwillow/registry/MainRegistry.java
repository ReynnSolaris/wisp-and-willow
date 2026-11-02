package com.reynn.wispwillow.registry;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.client.menu.AlchemicalCauldronMenu;
import com.reynn.wispwillow.dev.DevKit;
import com.reynn.wispwillow.fluxia.SimpleFluxiaStorage;
import com.reynn.wispwillow.items.FluxiaTabletItem;
import com.reynn.wispwillow.worldgen.placement.SpacedFilter;
import com.reynn.wispwillow.worldgen.spirit.SpiritBiomeSource;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import com.reynn.wispwillow.worldgen.tree.FenDrapeDecorator;
import com.reynn.wispwillow.worldgen.tree.SoulwoodFoliagePlacer;
import com.reynn.wispwillow.worldgen.tree.SoulwoodTrunkPlacer;
import com.reynn.wispwillow.worldgen.tree.WeepingFoliagePlacer;
import com.reynn.wispwillow.worldgen.tree.WeepingWillowTrunkPlacer;
import com.reynn.wispwillow.worldgen.tree.WillowTreeVineDecorator;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class MainRegistry {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WispWillow.MODID);
    public static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATIONS = DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, WispWillow.MODID);
    public static final DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<WillowTreeVineDecorator>> WILLOW_VINES = register("willow_vines", WillowTreeVineDecorator.CODEC);
    public static final DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<FenDrapeDecorator>> FEN_DRAPE = register("fen_drape", FenDrapeDecorator.CODEC);
    // Tree shapes vanilla has no placer for: the willow's arching limbs and trailing curtain.
    public static final DeferredRegister<TrunkPlacerType<?>> TRUNK_PLACERS =
            DeferredRegister.create(Registries.TRUNK_PLACER_TYPE, WispWillow.MODID);
    public static final DeferredHolder<TrunkPlacerType<?>, TrunkPlacerType<WeepingWillowTrunkPlacer>> WEEPING_WILLOW_TRUNK =
            TRUNK_PLACERS.register("weeping_willow_trunk", () -> new TrunkPlacerType<>(WeepingWillowTrunkPlacer.CODEC));

    public static final DeferredRegister<FoliagePlacerType<?>> FOLIAGE_PLACERS =
            DeferredRegister.create(Registries.FOLIAGE_PLACER_TYPE, WispWillow.MODID);
    public static final DeferredHolder<FoliagePlacerType<?>, FoliagePlacerType<WeepingFoliagePlacer>> WEEPING_FOLIAGE =
            FOLIAGE_PLACERS.register("weeping_foliage", () -> new FoliagePlacerType<>(WeepingFoliagePlacer.CODEC));

    /** The soulwood's severed bole and the crown that hangs unsupported above it. */
    public static final DeferredHolder<TrunkPlacerType<?>, TrunkPlacerType<SoulwoodTrunkPlacer>> SOULWOOD_TRUNK =
            TRUNK_PLACERS.register("soulwood_trunk", () -> new TrunkPlacerType<>(SoulwoodTrunkPlacer.CODEC));
    public static final DeferredHolder<FoliagePlacerType<?>, FoliagePlacerType<SoulwoodFoliagePlacer>> SOULWOOD_FOLIAGE =
            FOLIAGE_PLACERS.register("soulwood_foliage", () -> new FoliagePlacerType<>(SoulwoodFoliagePlacer.CODEC));

    /**
     * The Spirit Realm's own biome source.
     *
     * <p>Registered as a type because a dimension's biome source is serialised into the saved
     * level stem: without a codec under a known id the realm cannot be read back after a restart.</p>
     */
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, WispWillow.MODID);
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<SpiritBiomeSource>> SPIRIT_BIOME_SOURCE =
            BIOME_SOURCES.register("spirit", () -> SpiritBiomeSource.CODEC);

    /** Minimum spacing between features, which vanilla has no modifier for. */
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, WispWillow.MODID);
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<SpacedFilter>> SPACED =
            PLACEMENT_MODIFIERS.register("spaced", () -> () -> SpacedFilter.CODEC);

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, WispWillow.MODID);
    private static <P extends TreeDecorator> DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<P>> register(String name, MapCodec<P> codec) {
        return TREE_DECORATIONS.register(name, () -> new TreeDecoratorType<>(codec));
    }


    public static final DeferredHolder<MenuType<?>, MenuType<AlchemicalCauldronMenu>> ALCHEMICAL_CAULDRON_MENU =
            MENUS.register("alchemical_cauldron", () -> IMenuTypeExtension.create(AlchemicalCauldronMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB =
            CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wispwillow.example_tab"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(ItemRegistry.AMETHYST_COAL.get()::getDefaultInstance)
                    .displayItems((params, output) -> {
                        List<Item> stacks = List.of(ItemRegistry.FLUXIA_TABLET.get(), ItemRegistry.GRAND_TABLET.get(), ItemRegistry.GREATER_TABLET.get());
                        for (Item stack : stacks) {
                            ItemStack full = new ItemStack(stack);
                            SimpleFluxiaStorage flux = FluxiaTabletItem.readStorage(full);
                            flux.set(flux.capacity());
                            FluxiaTabletItem.writeStorage(full, flux);
                            output.accept(stack);
                            output.accept(full);
                        }

                        for (DeferredHolder<Item, ? extends Item> entry : ItemRegistry.ITEMS.getEntries()) {
                            if (ItemRegistry.CHALK.containsValue(entry) || entry.is(ItemRegistry.FLUXIA_TABLET)
                                    || DevKit.isDevOnly(entry.get()))
                                continue;
                            output.accept(entry.get());
                        }
                        //Add Chalk
                        ItemRegistry.CHALK.forEach((dye, item) -> output.accept(item.get()));

                        for (DeferredHolder<Block, ? extends Block> entry : BlockRegistry.BLOCKS.getEntries()) {
                            if (BlockRegistry.isItemless(entry) || entry == BlockRegistry.WILLOW_TORCH
                                    || entry == BlockRegistry.CREATIVE_MANA_POOL)
                                continue;
                            output.accept(entry.get());
                        }
                    })
                    .build());

    /** The developer kit, one tab along from the main one. Contents: {@link DevKit#fillTab}. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DEV_TAB =
            CREATIVE_MODE_TABS.register("dev_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wispwillow.dev_tab"))
                    .withTabsBefore(EXAMPLE_TAB.getId())
                    .icon(ItemRegistry.FLUXIA_PROBE.get()::getDefaultInstance)
                    .displayItems(DevKit::fillTab)
                    .build());


    public static void register(IEventBus event) {
        CREATIVE_MODE_TABS.register(event); TREE_DECORATIONS.register(event); MENUS.register(event);
        TRUNK_PLACERS.register(event); FOLIAGE_PLACERS.register(event);
        BIOME_SOURCES.register(event); PLACEMENT_MODIFIERS.register(event);
    }
}
