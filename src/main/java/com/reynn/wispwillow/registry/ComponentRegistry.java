package com.reynn.wispwillow.registry;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.awakening.Imbuement;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Unit;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item data components this mod defines.
 *
 * <p>The linking wand stores its state in {@code CUSTOM_DATA} because it predates this
 * class; new state goes here instead, so it round-trips through a codec, syncs to the
 * client for free, and makes two stacks with different contents refuse to stack.</p>
 */
public final class ComponentRegistry {
    private ComponentRegistry() {}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, WispWillow.MODID);

    /**
     * The creature a soul pact holds, as its entity type id. Present on a bound or sealed
     * pact and absent on an empty or primed one, which is what tells them apart.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SOUL =
            COMPONENTS.register("soul", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    /**
     * The rite an Omni Chalk is set to draw, as its recipe id. Absent means the chalk
     * follows whatever circle the altar has already recognised, which is also the only
     * mode that can complete a rite this mod did not author.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> RITE =
            COMPONENTS.register("rite", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    /**
     * Present on an item the Awakening rite has embued; absent on one it has not.
     *
     * <p>A marker rather than a flag, so there is no {@code awakened = false} state
     * that can drift out of step with itself — presence is the whole question, and
     * it is the only one the rite and the glint ever ask.</p>
     *
     * <p>Deliberately a component and not an enchantment. An enchantment would sit
     * in the item's enchantment list, where a grindstone strips it, an anvil can
     * trade it away and the tooltip files it among spells the player chose. Awakening
     * is a property the metal now has, so it rides on the stack itself and survives
     * all three.</p>
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> AWAKENED =
            COMPONENTS.register("awakened", () -> DataComponentType.<Unit>builder()
                    .persistent(Unit.CODEC)
                    .networkSynchronized(StreamCodec.unit(Unit.INSTANCE))
                    .build());

    /**
     * How much resonance an awakened piece has built up through use. Absent reads as none.
     * See {@link com.reynn.wispwillow.awakening.Resonance}.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RESONANCE =
            COMPONENTS.register("resonance", () -> DataComponentType.<Integer>builder()
                    .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /**
     * The one imbuement an awakened piece carries, if any. A separate component from
     * {@link #AWAKENED} so that pieces awakened before imbuements existed load unchanged.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Imbuement>> IMBUEMENT =
            COMPONENTS.register("imbuement", () -> DataComponentType.<Imbuement>builder()
                    .persistent(Imbuement.CODEC)
                    .networkSynchronized(Imbuement.STREAM_CODEC)
                    .build());

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
