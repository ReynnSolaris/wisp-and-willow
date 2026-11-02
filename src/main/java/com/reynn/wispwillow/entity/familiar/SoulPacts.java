package com.reynn.wispwillow.entity.familiar;

import com.reynn.wispwillow.registry.EntityRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Which creatures a soul pact can take, and what each one becomes.
 *
 * <p>This is the whole extension point. Teaching the pact a new creature is one
 * {@link #bond} line here — the pact item, the gateway, the tooltips and the failure
 * messages all read this table, so none of them can disagree about what is capturable.</p>
 *
 * <p>Deliberately a closed table rather than "any mob becomes a generic familiar": a pact
 * that refuses a cow with a clear message is more legible than one that quietly produces
 * something nondescript, and it keeps every familiar in the game a deliberate design.</p>
 *
 * <p>Since 2.1.0 this table is only the <em>default</em> set. In a running world the pact and
 * the gateway consult the {@code wispwillow:gateway} recipes ({@link com.reynn.wispwillow.gateway.GatewayRecipe}),
 * which datagen writes from this table and which a datapack or KubeJS script may add to, retune
 * or remove — including castings that summon ordinary creatures or drop items. This class is
 * still what datagen, the grimoire and the developer tab read.</p>
 */
public final class SoulPacts {
    private SoulPacts() {}

    /** Source creature id -> the familiar its soul becomes. Insertion-ordered for the book. */
    private static final Map<ResourceLocation, Supplier<? extends EntityType<? extends FamiliarEntity>>> BONDS =
            new LinkedHashMap<>();

    private static void bond(EntityType<?> source,
                             Supplier<? extends EntityType<? extends FamiliarEntity>> familiar) {
        BONDS.put(BuiltInRegistries.ENTITY_TYPE.getKey(source), familiar);
    }

    static {
        bond(EntityType.ALLAY, EntityRegistry.WISP_FAMILIAR);
        bond(EntityType.AXOLOTL, EntityRegistry.AXOLOTL_FAMILIAR);
        bond(EntityType.FROG, EntityRegistry.FROG_FAMILIAR);
        bond(EntityType.BAT, EntityRegistry.BAT_FAMILIAR);
    }

    /**
     * The whole table, source creature to familiar type, in book order. Datagen writes one
     * {@code wispwillow:gateway} recipe per entry; the gateway itself reads those recipes.
     */
    public static Map<ResourceLocation, Supplier<? extends EntityType<? extends FamiliarEntity>>> bonds() {
        return Collections.unmodifiableMap(BONDS);
    }

    /** Every creature a pact will accept, in book order. */
    public static Iterable<ResourceLocation> sources() {
        return BONDS.keySet();
    }

    public static boolean isEligible(EntityType<?> source) {
        return BONDS.containsKey(BuiltInRegistries.ENTITY_TYPE.getKey(source));
    }

    /** The familiar a captured {@code source} yields, or null if it was never bonded. */
    @Nullable
    public static EntityType<? extends FamiliarEntity> familiarFor(@Nullable ResourceLocation source) {
        if (source == null) return null;
        var supplier = BONDS.get(source);
        return supplier == null ? null : supplier.get();
    }

    /** The captured creature's own type, for naming it in tooltips. */
    @Nullable
    public static EntityType<?> sourceType(@Nullable ResourceLocation source) {
        if (source == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(source).orElse(null);
    }
}
