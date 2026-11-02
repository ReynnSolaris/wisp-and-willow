package com.reynn.wispwillow.worldgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reynn.wispwillow.registry.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * Refuses a spot that already has one of something standing too close to it.
 *
 * <p>Vanilla has no minimum-spacing modifier, because vanilla does not need one: an oak trunk is
 * one block wide and {@code TreeFeature} already refuses to start inside another tree, so a dense
 * forest overlaps only at the leaves and reads as a forest. The soulwood breaks both assumptions.
 * Its crown floats clear of the trunk and is far wider than it, so the space check that guards the
 * bole says nothing at all about where the canopy will end up.</p>
 *
 * <p><b>Why the vertical range is configurable, and why it matters more than the radius.</b> The
 * first version scanned a fixed five-block band around the candidate and did nothing useful,
 * because it was looking in the wrong place. {@code PlacementUtils.HEIGHTMAP} resolves to
 * {@code MOTION_BLOCKING}, which <em>counts leaves</em> — so once one soulwood exists, a candidate
 * a few blocks away is handed a Y at the top of that tree's canopy rather than on the ground. The
 * filter then searched for logs around a point eight or more blocks <em>above</em> where the logs
 * actually were, found nothing, and cheerfully approved a tree that grew on top of the first. Four
 * deep, in the groves.</p>
 *
 * <p>Two things fix it together: the placement switching to a heightmap that ignores leaves, and
 * this scanning deep enough that a candidate perched on a canopy still sees the trunk underneath
 * it. A caller that only needs a shallow look — ruins, which do not stack on each other's roofs —
 * passes a small height and pays for a small scan.</p>
 */
public class SpacedFilter extends PlacementFilter {
    public static final MapCodec<SpacedFilter> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.intRange(1, 16).fieldOf("radius").forGetter(f -> f.radius),
                    Codec.intRange(1, 32).fieldOf("height").forGetter(f -> f.height),
                    BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(f -> f.block)
            ).apply(instance, SpacedFilter::new));

    private final int radius;
    private final int height;
    private final Block block;

    public SpacedFilter(int radius, int height, Block block) {
        this.radius = radius;
        this.height = height;
        this.block = block;
    }

    /**
     * @param radius how far out to look, in blocks
     * @param height how far above and below the candidate to look; must comfortably exceed the
     *               height of whatever is being spaced, or a candidate standing on top of one
     *               will not see it
     */
    public static SpacedFilter of(int radius, int height, Block block) {
        return new SpacedFilter(radius, height, block);
    }

    @Override
    public PlacementModifierType<?> type() {
        return MainRegistry.SPACED.get();
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        WorldGenLevel level = context.getLevel();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int min = Math.max(level.getMinBuildHeight(), pos.getY() - height);
        int max = Math.min(level.getMaxBuildHeight() - 1, pos.getY() + height);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int y = min; y <= max; y++) {
                    cursor.set(pos.getX() + dx, y, pos.getZ() + dz);
                    if (level.getBlockState(cursor).is(block)) return false;
                }
            }
        }
        return true;
    }
}
