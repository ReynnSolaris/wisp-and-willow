package com.reynn.wispwillow.dev;

import com.reynn.wispwillow.awakening.Resonance;
import com.reynn.wispwillow.entity.familiar.SoulPacts;
import com.reynn.wispwillow.fluxia.SimpleFluxiaStorage;
import com.reynn.wispwillow.items.FluxiaTabletItem;
import com.reynn.wispwillow.items.SoulPactItem;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ComponentRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * What the developer tab holds, and which items belong to it and nowhere else.
 *
 * <p>The tab is the kit's only source: nothing in it has a recipe, and the main tab's loops
 * skip anything {@link #isDevOnly} says yes to. Besides the tools it carries ready-made test
 * subjects that are tedious to produce by hand: a pact for every creature the pact table
 * knows, in both the bound and the sealed stage, and the whole Empyrite set already awake at
 * full resonance.</p>
 */
public final class DevKit {
    private DevKit() {}

    /** An item that must stay out of the player-facing tab. */
    public static boolean isDevOnly(Item item) {
        return item instanceof DevToolItem
                || item instanceof CreativeFluxiaTabletItem
                || (item instanceof BlockItem block && block.getBlock() instanceof CreativeManaPoolBlock);
    }

    public static void fillTab(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output out) {
        // The tools
        out.accept(ItemRegistry.FLUXIA_PROBE.get());
        out.accept(ItemRegistry.RITE_PRIMER.get());
        out.accept(ItemRegistry.RESONANCE_TUNER.get());
        out.accept(ItemRegistry.HOURGLASS.get());
        out.accept(ItemRegistry.SPIRIT_KEY.get());
        out.accept(ItemRegistry.CREATIVE_FLUXIA_TABLET.get());
        out.accept(BlockRegistry.CREATIVE_MANA_POOL.get());

        // The ordinary things a tester reaches for next, so they are one tab away.
        out.accept(ItemRegistry.OMNI_CHALK.get());
        out.accept(ItemRegistry.ARCANE_GOGGLES.get());
        out.accept(ItemRegistry.FLUX_LINKING_WAND.get());
        out.accept(ItemRegistry.FLUXIA_FLUID_BUCKET.get());
        out.accept(ItemRegistry.SOUL_WISP.get());
        for (Item tablet : List.of(ItemRegistry.FLUXIA_TABLET.get(), ItemRegistry.GREATER_TABLET.get(), ItemRegistry.GRAND_TABLET.get())) {
            ItemStack full = new ItemStack(tablet);
            SimpleFluxiaStorage flux = FluxiaTabletItem.readStorage(full);
            flux.set(flux.capacity());
            FluxiaTabletItem.writeStorage(full, flux);
            out.accept(full);
        }

        // One pact per bonded creature, bound and sealed: the gateway's whole input space.
        for (ResourceLocation soul : SoulPacts.sources()) {
            ItemStack bound = new ItemStack(ItemRegistry.BOUND_SOUL_PACT.get());
            SoulPactItem.setSoul(bound, soul);
            out.accept(bound);
            ItemStack sealed = new ItemStack(ItemRegistry.SEALED_SOUL_PACT.get());
            SoulPactItem.setSoul(sealed, soul);
            out.accept(sealed);
        }

        // The Empyrite set, awake and fully resonant: imbue with the tuner from here.
        for (Item piece : List.of(
                ItemRegistry.EMPYRITE_SWORD.get(), ItemRegistry.EMPYRITE_PICKAXE.get(), ItemRegistry.EMPYRITE_AXE.get(),
                ItemRegistry.EMPYRITE_SHOVEL.get(), ItemRegistry.EMPYRITE_HOE.get(),
                ItemRegistry.EMPYRITE_HELMET.get(), ItemRegistry.EMPYRITE_CHESTPLATE.get(),
                ItemRegistry.EMPYRITE_LEGGINGS.get(), ItemRegistry.EMPYRITE_BOOTS.get())) {
            ItemStack awake = new ItemStack(piece);
            // Components set directly rather than through Awakening.awaken: this runs while
            // tabs are being built, and it should not depend on tag state at that moment.
            awake.set(ComponentRegistry.AWAKENED.get(), Unit.INSTANCE);
            awake.set(ComponentRegistry.RESONANCE.get(), Resonance.threshold(Resonance.MAX_LEVEL));
            out.accept(awake);
        }
    }
}
