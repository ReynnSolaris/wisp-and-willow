package com.reynn.wispwillow.worldgen.fen;

import com.mojang.datafixers.util.Pair;
import com.reynn.wispwillow.WispWillow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.neoforge.data.loading.DatagenModLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Where the Willow Fen sits in the overworld's climate space.
 *
 * <p>The overworld picks a biome by scoring every entry in a list of climate boxes and taking the
 * lowest: {@code Climate.ParameterPoint#fitness} is the summed squared distance to each box, so a
 * point that falls <em>inside</em> a box scores zero. That is why simply appending a box does not
 * work. Vanilla's swamp entry spans every humidity with otherwise identical ranges, so any box
 * placed inside it ties at zero everywhere and never wins — the biome would exist, be registered,
 * and never generate anywhere.</p>
 *
 * <p>So the fen takes its ground instead of asking for it: each vanilla swamp entry is split in
 * two along humidity. The swamp keeps everything drier than {@link #WET_START}, and the fen gets
 * the wettest band. The total space covered is unchanged — no climate is left without a biome, and
 * no other biome is touched — but the wettest swamps in the world are now fens.</p>
 */
public final class WillowFenClimate {
    private WillowFenClimate() {}

    /** Humidity at which a swamp becomes a fen. Vanilla's own wettest band starts at 0.3. */
    private static final float WET_START = 0.3F;

    /**
     * Splits vanilla's swamp entries with the fen, and leaves everything else alone.
     *
     * @param valueGetter the preset's own mapping from biome key to value, which is the only way
     *                    to recognise the swamp entries without knowing what {@code T} is
     */
    public static <T> Climate.ParameterList<T> claimWettestSwamps(Climate.ParameterList<T> vanilla,
                                                                  Function<ResourceKey<Biome>, T> valueGetter) {
        T swamp = valueGetter.apply(Biomes.SWAMP);
        T fen = valueGetter.apply(WillowFenBiome.WILLOW_FEN);

        List<Pair<Climate.ParameterPoint, T>> out = new ArrayList<>(vanilla.values().size() + 8);
        int claimed = 0;
        for (Pair<Climate.ParameterPoint, T> entry : vanilla.values()) {
            Climate.ParameterPoint point = entry.getFirst();
            if (!entry.getSecond().equals(swamp) || !reachesWet(point.humidity())) {
                out.add(entry);
                continue;
            }
            out.add(Pair.of(withHumidity(point, Climate.Parameter.span(-1.0F, WET_START)), swamp));
            out.add(Pair.of(withHumidity(point, Climate.Parameter.span(WET_START, 1.0F)), fen));
            claimed++;
        }

        WispWillow.LOGGER.info("Willow Fen claimed the wettest half of {} swamp climate entries", claimed);
        return claimed == 0 ? vanilla : new Climate.ParameterList<>(out);
    }

    /** True if this entry's humidity band actually extends into the wet end worth splitting. */
    private static boolean reachesWet(Climate.Parameter humidity) {
        return humidity.max() > Climate.quantizeCoord(WET_START);
    }

    private static Climate.ParameterPoint withHumidity(Climate.ParameterPoint point, Climate.Parameter humidity) {
        return new Climate.ParameterPoint(point.temperature(), humidity, point.continentalness(),
                point.erosion(), point.depth(), point.weirdness(), point.offset());
    }

    /**
     * Whether this call is vanilla assembling its own registries rather than a world loading.
     *
     * <p>Vanilla builds this list twice over: once for real, when a world loads, and once inside
     * {@code VanillaRegistries.createLookup()} against a registry set that holds only vanilla
     * biomes. Handing the fen to that second one fails the whole build with "Unreferenced key" —
     * it broke data generation first, then crashed the dev client through {@code Bootstrap.validate}.
     * Nothing is lost by sitting those out: the overworld's parameter list is never baked into
     * JSON, so the list is rebuilt, with the fen in it, every time a world actually loads.</p>
     */
    public static boolean buildingVanillaRegistries() {
        if (DatagenModLoader.isRunningDataGen()) return true;
        return StackWalker.getInstance().walk(frames ->
                frames.anyMatch(f -> f.getClassName().equals("net.minecraft.data.registries.VanillaRegistries")));
    }
}
