package com.reynn.wispwillow.worldgen.structures;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.registry.StructureRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * The Fluxia Oasis: a lit cavern around a pool of Liquid Fluxia, with a sealed vault beneath it.
 *
 * <p>Placed underground and never allowed to break the surface, so finding one is a matter of
 * caving rather than of walking. The whole thing is one {@link FluxiaOasisPiece}; there is no
 * jigsaw, because the cavern and the vault below it are a single fixed shape whose geometry the
 * puzzle depends on — the pool has to sit directly over the vault for phasing down into it to
 * mean anything.</p>
 */
public class FluxiaOasisStructure extends Structure {
    public static final MapCodec<FluxiaOasisStructure> CODEC = simpleCodec(FluxiaOasisStructure::new);

    /** The shallowest and deepest the cavern floor is allowed to sit. */
    private static final int MIN_FLOOR = -38;
    private static final int MAX_FLOOR = 6;

    /** Rock left between the top of the dome and the surface, so no oasis opens into daylight. */
    private static final int HEADROOM = 8;

    public FluxiaOasisStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();

        int surface = context.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());

        // Pick a depth, then push it down as far as the terrain overhead requires. Shallow
        // ground simply has no room for an oasis, and that is a better answer than a dome
        // with a hole in the top of it.
        int floor = context.random().nextIntBetweenInclusive(MIN_FLOOR, MAX_FLOOR);
        floor = Math.min(floor, surface - HEADROOM - FluxiaOasisPiece.DOME_HEIGHT);
        if (floor < MIN_FLOOR) return Optional.empty();

        // The vault hangs below the floor, and it may not hang out of the world.
        if (floor - FluxiaOasisPiece.DEPTH_BELOW < context.heightAccessor().getMinBuildHeight() + 4) {
            return Optional.empty();
        }

        BlockPos origin = new BlockPos(x, floor, z);
        long seed = context.random().nextLong();
        return Optional.of(new Structure.GenerationStub(origin,
                builder -> builder.addPiece(new FluxiaOasisPiece(origin, seed))));
    }

    @Override
    public StructureType<?> type() {
        return StructureRegistry.FLUXIA_OASIS.get();
    }
}
