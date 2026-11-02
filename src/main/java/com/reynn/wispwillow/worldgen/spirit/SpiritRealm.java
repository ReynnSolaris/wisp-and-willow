package com.reynn.wispwillow.worldgen.spirit;

import com.reynn.wispwillow.WispWillow;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import java.util.OptionalLong;

/**
 * The Spirit Realm: End islands with the grass still on them.
 *
 * <p>Generation is the mod's own — see {@link SpiritNoise} — so the realm is broken land adrift
 * over nothing, reached by falling as much as by walking. What makes it <em>this</em> place rather
 * than the End is partly what sits on top of that rock ({@link SpiritFeatures#SPIRIT_FLOOR_PLACED}
 * paints the island crowns with soil and pale grass) and partly the shape of the rock itself: no
 * concentric rings, no fixed centre, and islands all the way up the column rather than crowded into
 * the bottom half of it.</p>
 *
 * <p>Two earlier builds got this wrong in opposite directions. The first used the overworld's noise,
 * which gave hills and caves and rivers — a place you could mistake for home. The second used the
 * End's, which gave islands but also gave the End's own ring layout and its 128-block ceiling. The
 * islands are the point, the void under them is what makes an excursion feel like one, and neither
 * of those requires being the End.</p>
 *
 * <p>Nobody lives here permanently and nobody spawns in: getting in means keeping a
 * {@code Vigil.Effect#OPEN_WAY} burning, and when that vigil ends the realm puts you out.</p>
 */
public final class SpiritRealm {
    private SpiritRealm() {}

    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, id());
    public static final ResourceKey<DimensionType> TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, id());
    public static final ResourceKey<LevelStem> STEM = ResourceKey.create(Registries.LEVEL_STEM, id());

    /** The End's build range. Islands want headroom above and void below, not bedrock at -64. */
    public static final int MIN_Y = 0;
    public static final int HEIGHT = 256;

    private static ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "spirit_realm");
    }

    public static void bootstrapType(BootstrapContext<DimensionType> context) {
        context.register(TYPE, new DimensionType(
                // Full daylight, held there. The realm spent two builds at 13000 — the sun just
                // below the horizon — which was a handsome dusk and completely wrong for what this
                // place is: somewhere living mana grows, not somewhere it has gone out. 6000 is
                // noon, and with a fixed time it is noon for ever.
                OptionalLong.of(6000L),
                true,   // sky light: an overworld sky, so the biome's own colours have a dome to tint
                false,  // no ceiling
                false,  // not ultrawarm
                false,  // not natural: no nether portals linking here, no compass sense of home
                1.0,    // same scale as the overworld
                false,  // beds do not work
                false,  // nor respawn anchors -- you cannot make this place your spawn
                MIN_Y, HEIGHT, HEIGHT,
                BlockTags.INFINIBURN_END,
                // An overworld sky over terrain that is nobody's but ours. OVERWORLD_EFFECTS draws
                // a real dome, clouds and horizon that the biome's sky and fog colours actually
                // drive, where END_EFFECTS painted its own fixed starfield and ignored them.
                BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                // Ambient light. Went 0.35 -> 0.5 to lift the realm out of dusk and overshot into
                // washed-out, so this sits between: enough that island undersides and the inside of
                // a grove keep their colour, not so much that nothing has any shading left. Vanilla
                // puts the End at 0.0 and the Nether at 0.1, so this is still far brighter.
                0.4F,
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 7), 0)));
    }

    public static void bootstrapStem(BootstrapContext<LevelStem> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<DimensionType> types = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noise = context.lookup(Registries.NOISE_SETTINGS);

        context.register(STEM, new LevelStem(
                types.getOrThrow(TYPE),
                new NoiseBasedChunkGenerator(
                        // Four regions over one island field, and both halves are the mod's own.
                        // The biome source has to be, because a multi-noise source selects on
                        // climate and this generator leaves the climate slots empty — see
                        // SpiritBiomeSource. Nothing of the End's remains, so the dragon, the
                        // cities and the chorus have no way in.
                        new SpiritBiomeSource(
                                biomes.getOrThrow(SpiritBiome.SPIRIT_MEADOW),
                                biomes.getOrThrow(SpiritBiome.SOULWOOD_GROVE),
                                biomes.getOrThrow(SpiritBiome.HOLLOW_BARRENS),
                                biomes.getOrThrow(SpiritBiome.MISTFALL_SHALLOWS)),
                        noise.getOrThrow(SpiritNoise.SETTINGS))));
    }
}
