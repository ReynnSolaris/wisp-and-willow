package com.reynn.wispwillow.worldgen.spirit;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.BlockRegistry;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;

/**
 * The Spirit Realm's own terrain: islands adrift, and no rings.
 *
 * <p>The realm used to generate on {@code NoiseGeneratorSettings.END}, which was wrong in two ways
 * that were easy to miss and hard to un-see once noticed.</p>
 *
 * <p><b>The rings.</b> The End puts {@code end_islands} in its router's <em>erosion</em> slot, and
 * that function is not a noise field at all — it is a deliberate arrangement of a central island
 * with concentric rings of smaller ones marching outward from the origin. Every End looks the same
 * from far enough up, and so did this. Dropping erosion to zero and driving the terrain from the
 * 3D noise alone is the single change that makes the realm stop being a copy of the End's layout.</p>
 *
 * <p><b>The lid.</b> {@code end.json} declares a noise height of 128 while this dimension declares
 * 256, so terrain could only ever occupy the bottom half of the world — everything generated in one
 * narrow band with a great deal of nothing above it. That is most of why the place read as flat.
 * The settings here use the full column, and the upper fade is pushed out to 440 so islands are
 * still being made at the top of it rather than thinning out halfway.</p>
 *
 * <p>The whimsy is the drift term: a slow two-dimensional field added to the noise so island density
 * varies by region. Some stretches come out as dense archipelagos you can island-hop across without
 * ever looking down, others as one great isle alone in a lot of sky. Without it every part of the
 * realm has the same amount of ground in it, which is the other half of feeling flat.</p>
 *
 * <p>Everything else in the router is zero. There are no caves, ores, aquifers or veins here, and
 * the biome is chosen by {@link SpiritBiomeSource} rather than by climate, so the climate slots have
 * no reader and are left empty.</p>
 */
public final class SpiritNoise {
    private SpiritNoise() {}

    public static final ResourceKey<NoiseGeneratorSettings> SETTINGS = ResourceKey.create(
            Registries.NOISE_SETTINGS,
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "spirit_realm"));

    /**
     * Vanilla's own 3D noise for End-like terrain.
     *
     * <p>Named by hand because {@code NoiseRouterData} keeps its copy of this key private. The id is
     * stable and the function is registered by vanilla, so building the key here reaches exactly the
     * same object without needing an access transformer.</p>
     */
    private static final ResourceKey<DensityFunction> BASE_3D_NOISE_END = ResourceKey.create(
            Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace("end/base_3d_noise"));

    private static final ResourceKey<NormalNoise.NoiseParameters> DRIFT = ResourceKey.create(
            Registries.NOISE, ResourceLocation.withDefaultNamespace("continentalness"));

    // ---- Tuning. These five numbers are the whole shape of the realm. --------
    /** Below this band there is nothing: the void floor everything hangs over. */
    private static final int FADE_IN_BOTTOM = 4;
    private static final int FADE_IN_TOP = 32;
    /** Islands thin out toward this, which is deliberately far above the build height. */
    private static final int FADE_OUT_BOTTOM = 184;
    private static final int FADE_OUT_TOP = 440;
    /** How strongly island density varies from region to region. Raise for more contrast. */
    private static final double DRIFT_STRENGTH = 0.55;
    /** How wide those regions are; smaller stretches the field further. */
    private static final double DRIFT_SCALE = 0.35;
    /**
     * A flat positive bias on the terrain noise, which is what makes the land <em>thick</em>.
     *
     * <p>The 3D noise on its own produces islands that are wide but shallow — a lot of surface with
     * very little under it, so from the side the realm looked like a scatter of plates rather than
     * of land. Adding a constant pushes more of every column over the solid threshold, and because
     * it applies at all heights it deepens islands rather than merely widening them.</p>
     *
     * <p>This is the most dangerous number here: too much and the islands fuse into a ceiling with
     * no sky between them, which is the opposite failure and much less pleasant to be inside.</p>
     */
    private static final double THICKNESS = 0.30;

    private static final double BIAS = -0.234375;
    private static final double SLIDE = 23.4375;
    private static final double SCALE = 0.64;

    public static void bootstrap(BootstrapContext<NoiseGeneratorSettings> context) {
        HolderGetter<DensityFunction> functions = context.lookup(Registries.DENSITY_FUNCTION);
        HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);

        context.register(SETTINGS, new NoiseGeneratorSettings(
                new NoiseSettings(SpiritRealm.MIN_Y, SpiritRealm.HEIGHT, 2, 1),
                // Spiritstone rather than end stone. The End's rock came along with borrowing the
                // End's generator and brought its sallow yellow with it, which fought every other
                // colour in the dimension.
                //
                // Load-bearing rather than decorative: SpiritFloorFeature only paints soil over
                // columns whose top block matches, so this block and the one that feature tests for
                // have to stay the same or every island silently comes out bare.
                BlockRegistry.SPIRITSTONE.get().defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                router(functions, noises),
                SurfaceRules.state(BlockRegistry.SPIRITSTONE.get().defaultBlockState()),
                List.of(),
                0,      // sea level: there is no sea, only what the pools hold
                false,  // mobs generate normally
                false,  // no aquifers
                false,  // no ore veins
                true));
    }

    private static NoiseRouter router(HolderGetter<DensityFunction> functions,
                                      HolderGetter<NormalNoise.NoiseParameters> noises) {
        // Wrapped in a HolderHolder rather than unwrapped with value(), exactly as vanilla does:
        // a reference fetched during bootstrap is not necessarily bound yet, and resolving it
        // eagerly here would throw on a registry that has not finished loading.
        DensityFunction base = new DensityFunctions.HolderHolder(functions.getOrThrow(BASE_3D_NOISE_END));

        // Flat-cached because it is a two-dimensional field: this makes it sampled once per column
        // instead of once per cell, which is both cheaper and what keeps it from smearing vertically.
        DensityFunction drift = DensityFunctions.mul(
                DensityFunctions.constant(DRIFT_STRENGTH),
                DensityFunctions.flatCache(
                        DensityFunctions.noise(noises.getOrThrow(DRIFT), DRIFT_SCALE, 0.0)));

        DensityFunction shaped = DensityFunctions.add(base,
                DensityFunctions.add(drift, DensityFunctions.constant(THICKNESS)));

        // The slide: fade the density in above the void and back out toward the roof, so islands
        // have empty air both under and over them instead of fusing into a floor or a ceiling.
        DensityFunction slid = DensityFunctions.add(
                DensityFunctions.constant(BIAS),
                DensityFunctions.mul(
                        DensityFunctions.yClampedGradient(FADE_IN_BOTTOM, FADE_IN_TOP, 0.0, 1.0),
                        DensityFunctions.add(
                                DensityFunctions.constant(-BIAS),
                                DensityFunctions.add(
                                        DensityFunctions.constant(-SLIDE),
                                        DensityFunctions.mul(
                                                DensityFunctions.yClampedGradient(FADE_OUT_BOTTOM, FADE_OUT_TOP, 1.0, 0.0),
                                                DensityFunctions.add(DensityFunctions.constant(SLIDE), shaped))))));

        DensityFunction finalDensity = DensityFunctions.mul(
                DensityFunctions.constant(SCALE),
                DensityFunctions.interpolated(DensityFunctions.blendDensity(slid))).squeeze();

        return new NoiseRouter(
                DensityFunctions.zero(),   // barrier
                DensityFunctions.zero(),   // fluid level floodedness
                DensityFunctions.zero(),   // fluid level spread
                DensityFunctions.zero(),   // lava
                DensityFunctions.zero(),   // temperature
                DensityFunctions.zero(),   // vegetation
                DensityFunctions.zero(),   // continents
                DensityFunctions.zero(),   // erosion -- the End kept its rings here
                DensityFunctions.zero(),   // depth
                DensityFunctions.zero(),   // ridges
                DensityFunctions.zero(),   // initial density without jaggedness
                finalDensity,
                DensityFunctions.zero(),   // vein toggle
                DensityFunctions.zero(),   // vein ridged
                DensityFunctions.zero());  // vein gap
    }
}
