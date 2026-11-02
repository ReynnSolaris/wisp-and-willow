package com.reynn.wispwillow.registry;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.entity.AxolotlFamiliarEntity;
import com.reynn.wispwillow.entity.OverlayHostEntity;
import com.reynn.wispwillow.entity.BatFamiliarEntity;
import com.reynn.wispwillow.entity.FrogFamiliarEntity;
import com.reynn.wispwillow.entity.RavenFamiliarEntity;
import com.reynn.wispwillow.entity.WispFamiliarEntity;
import com.reynn.wispwillow.entity.spirit.GlimmerfawnEntity;
import com.reynn.wispwillow.entity.wild.FenHeronEntity;
import com.reynn.wispwillow.entity.wild.WillowHoundEntity;
import com.reynn.wispwillow.entity.wild.WispflyEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, WispWillow.MODID);

    public static final Supplier<EntityType<AxolotlFamiliarEntity>> AXOLOTL_FAMILIAR =
            ENTITY_TYPES.register("axolotl_familiar", () -> EntityType.Builder.of(AxolotlFamiliarEntity::new, MobCategory.CREATURE)
                    .sized(.75f, 0.35f).build("axolotl_familiar"));

    public static final Supplier<EntityType<WispFamiliarEntity>> WISP_FAMILIAR =
            ENTITY_TYPES.register("wisp_familiar", () -> EntityType.Builder.of(WispFamiliarEntity::new, MobCategory.CREATURE)
                    .sized(0.4f, 0.6f).fireImmune().build("wisp_familiar"));

    public static final Supplier<EntityType<FrogFamiliarEntity>> FROG_FAMILIAR =
            ENTITY_TYPES.register("frog_familiar", () -> EntityType.Builder.of(FrogFamiliarEntity::new, MobCategory.CREATURE)
                    .sized(0.5f, 0.5f).build("frog_familiar"));

    public static final Supplier<EntityType<BatFamiliarEntity>> BAT_FAMILIAR =
            ENTITY_TYPES.register("bat_familiar", () -> EntityType.Builder.of(BatFamiliarEntity::new, MobCategory.CREATURE)
                    .sized(0.5f, 0.9f).build("bat_familiar"));

    /**
     * The author's familiar. Registered like any other so an existing one keeps working on
     * any server; what is gated is the act of summoning it, not the entity's existence.
     */
    public static final Supplier<EntityType<RavenFamiliarEntity>> RAVEN_FAMILIAR =
            ENTITY_TYPES.register("raven_familiar", () -> EntityType.Builder.of(RavenFamiliarEntity::new, MobCategory.CREATURE)
                    .sized(0.5f, 0.9f).build("raven_familiar"));

    // ---- Wildlife of the Willow Fen ----

    public static final Supplier<EntityType<WispflyEntity>> WISPFLY =
            ENTITY_TYPES.register("wispfly", () -> EntityType.Builder.of(WispflyEntity::new, MobCategory.AMBIENT)
                    .sized(0.35f, 0.35f).clientTrackingRange(8).build("wispfly"));

    public static final Supplier<EntityType<FenHeronEntity>> FEN_HERON =
            ENTITY_TYPES.register("fen_heron", () -> EntityType.Builder.of(FenHeronEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.4f).clientTrackingRange(10).build("fen_heron"));

    /** The tameable one. Bound to a person rather than to an altar, unlike every familiar above. */
    public static final Supplier<EntityType<WillowHoundEntity>> WILLOW_HOUND =
            ENTITY_TYPES.register("willow_hound", () -> EntityType.Builder.of(WillowHoundEntity::new, MobCategory.CREATURE)
                    .sized(0.7f, 0.85f).clientTrackingRange(10).build("willow_hound"));

    // ---- The Spirit Realm ----

    public static final Supplier<EntityType<GlimmerfawnEntity>> GLIMMERFAWN =
            ENTITY_TYPES.register("glimmerfawn", () -> EntityType.Builder.of(GlimmerfawnEntity::new, MobCategory.CREATURE)
                    .sized(0.7f, 1.3f).clientTrackingRange(10).build("glimmerfawn"));

    /**
     * The client-only carrier for the Fluxia currents and the goggle overlay: one per
     * client, never saved, never summoned. See {@link OverlayHostEntity}.
     */
    public static final Supplier<EntityType<OverlayHostEntity>> OVERLAY_HOST =
            ENTITY_TYPES.register("overlay_host", () -> EntityType.Builder.of(OverlayHostEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f).noSave().noSummon().clientTrackingRange(1).updateInterval(Integer.MAX_VALUE)
                    .build("overlay_host"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
