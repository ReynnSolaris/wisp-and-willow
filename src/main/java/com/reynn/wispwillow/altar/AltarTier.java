package com.reynn.wispwillow.altar;

import com.reynn.wispwillow.blocks.RuneBlock;
import com.reynn.wispwillow.blocks.RuneType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;

/**
 * The altar's foundation: complete rings of {@link RuneBlock} laid one block below the
 * altar, each finished ring raising it a tier.
 *
 * <p>The ring below the altar is the floor you are already standing on, so a foundation
 * is built by replacing that floor rather than by stacking anything into the chalk circle.
 * Rings must be <em>complete</em> — a ring with a single gap does not count, which keeps
 * the rule easy to state and easy to debug in-world.</p>
 */
public final class AltarTier {
    private AltarTier() {}

    /** Highest reachable tier. Tier 1 is a bare altar with no foundation at all. */
    public static final int MAX_TIER = 5;

    /** Runes needed for each successive ring: radius 1 through 4. */
    private static final int[] RING_RADII = {1, 2, 3, 4};

    /** Ceilings, so a colossal foundation cannot trivialise the whole mod. */
    private static final float MAX_EFFICIENCY = 0.50f;
    private static final float MAX_YIELD_CHANCE = 0.50f;
    private static final int BASE_CAPACITY = 20000;

    /**
     * A snapshot of what the foundation grants.
     *
     * @param tier            1..{@link #MAX_TIER}
     * @param runes           how many runes were found in complete rings
     * @param bonusCapacity   extra Fluxia reserve, on top of the altar's base
     * @param efficiency      0..0.5, fraction removed from every rite's cost
     * @param yieldChance     0..0.5, chance of a bonus copy of a rite's output
     */
    public record Foundation(int tier, int runes, int bonusCapacity, float efficiency,
                             float yieldChance, Map<RuneType, Integer> counts,
                             int misplaced) {

        public static final Foundation BARE =
                new Foundation(1, 0, 0, 0f, 0f, Map.of(), 0);

        /** Runes found on the altar's own layer, where they do nothing. */
        public boolean hasMisplacedRunes() { return misplaced > 0; }

        public int totalCapacity() {
            return BASE_CAPACITY + bonusCapacity;
        }

        /** Cost of a rite after the foundation's discount. */
        public int applyEfficiency(int baseCost) {
            return Math.max(0, Math.round(baseCost * (1f - efficiency)));
        }
    }

    /**
     * Reads the foundation beneath {@code altarPos}.
     *
     * <p>Rings are checked outward and stop at the first incomplete one, so partial
     * progress on ring three never counts while ring two has a hole in it.</p>
     */
    public static Foundation scan(BlockGetter level, BlockPos altarPos) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int y = altarPos.getY() - 1;

        Map<RuneType, Integer> counts = new EnumMap<>(RuneType.class);
        int tier = 1;
        int total = 0;

        for (int radius : RING_RADII) {
            Map<RuneType, Integer> ring = new EnumMap<>(RuneType.class);
            int found = 0;
            int expected = 0;
            boolean complete = true;

            for (int dx = -radius; dx <= radius && complete; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Perimeter only; the interior belongs to the smaller rings.
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    expected++;

                    m.set(altarPos.getX() + dx, y, altarPos.getZ() + dz);
                    BlockState state = level.getBlockState(m);
                    if (state.getBlock() instanceof RuneBlock rune) {
                        ring.merge(rune.runeType(), 1, Integer::sum);
                        found++;
                    } else {
                        complete = false;
                        break;
                    }
                }
            }

            if (!complete || found != expected) break;

            tier++;
            total += found;
            ring.forEach((k, v) -> counts.merge(k, v, Integer::sum));
        }

        if (tier == 1) return new Foundation(1, 0, 0, 0f, 0f, Map.of(), countMisplaced(level, altarPos));

        int capacity = 0;
        float efficiency = 0f;
        float yield = 0f;
        for (var e : counts.entrySet()) {
            capacity += e.getKey().capacityBonus() * e.getValue();
            efficiency += e.getKey().efficiencyBonus() * e.getValue();
            yield += e.getKey().yieldBonus() * e.getValue();
        }

        return new Foundation(
                Math.min(tier, MAX_TIER),
                total,
                capacity,
                Mth.clamp(efficiency, 0f, MAX_EFFICIENCY),
                Mth.clamp(yield, 0f, MAX_YIELD_CHANCE),
                Map.copyOf(counts),
                countMisplaced(level, altarPos));
    }

    /**
     * Counts runes sitting on the altar's <em>own</em> layer.
     *
     * <p>That is the intuitive place to put them and it does nothing, so rather than
     * reporting a bare altar and leaving you to wonder, the altar can point at them.
     * They cannot be accepted there: at radius 1 they would occupy exactly the cells
     * that Minor Focus and Minor Arcana need for their chalk.</p>
     */
    private static int countMisplaced(BlockGetter level, BlockPos altarPos) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int count = 0;
        int r = RING_RADII[RING_RADII.length - 1];
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) continue;
                m.set(altarPos.getX() + dx, altarPos.getY(), altarPos.getZ() + dz);
                if (level.getBlockState(m).getBlock() instanceof RuneBlock) count++;
            }
        }
        return count;
    }

    /** How many runes a given ring needs, for the grimoire and the goggles. */
    public static int runesForRing(int ring) {
        if (ring < 1 || ring > RING_RADII.length) return 0;
        int r = RING_RADII[ring - 1];
        return (r * 2 + 1) * (r * 2 + 1) - (r * 2 - 1) * (r * 2 - 1);
    }

    /** How far out from the altar a ring lies, for anything that has to draw one. */
    public static int ringRadius(int ring) {
        return RING_RADII[Mth.clamp(ring, 1, RING_RADII.length) - 1];
    }

    /** Cumulative rune count needed to reach a tier. */
    public static int runesForTier(int tier) {
        int total = 0;
        for (int ring = 1; ring <= tier - 1; ring++) total += runesForRing(ring);
        return total;
    }
}
