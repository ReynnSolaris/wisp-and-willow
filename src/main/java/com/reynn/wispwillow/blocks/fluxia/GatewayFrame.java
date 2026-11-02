package com.reynn.wispwillow.blocks.fluxia;

import com.reynn.wispwillow.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The shape a Soul Gateway has to be built in.
 *
 * <p>A five-by-five wall of willow logs with a three-by-three hole, the Gateway Core sitting
 * in the middle of the bottom edge. The wall stands in the plane the core faces across, so
 * you walk up to a placed core and build outward and upward from it:</p>
 *
 * <pre>
 *   L L L L L      v=4   L = willow log (any block in wispwillow:willow_logs)
 *   L . . . L      v=3   . = interior, filled with gateway portal when open
 *   L . . . L      v=2   C = the Gateway Core
 *   L . . . L      v=1
 *   L L C L L      v=0
 *  u=-2 .. +2
 * </pre>
 *
 * <p>Fifteen logs and the core. Geometry lives here rather than in the block entity so the
 * validator, the portal filler and the item-catching box cannot drift apart.</p>
 *
 * <p>The core does not insist on its own facing: {@link #findFacing} looks in both planes,
 * so a wall built "sideways" to the way the core happened to be placed still counts, and
 * the core turns to match it. Likewise the opening only has to be free of anything a portal
 * cannot push aside — grass, flowers, snow and water in the hole are fine (see
 * {@link #isClear}). Before 2.0.1 both of those read as "the willow frame is unfinished",
 * which is the complaint that prompted them.</p>
 */
public final class GatewayFrame {
    private GatewayFrame() {}

    /** Half-width of the interior, in blocks. */
    private static final int INNER_HALF = 1;
    /** Interior height, in blocks, measured from one above the core. */
    private static final int INNER_HEIGHT = 3;
    /** How many logs a finished wall has. */
    public static final int LOG_COUNT = 15;

    /** Where the portal blocks go, bottom row first. */
    public static List<BlockPos> interior(BlockPos core, Direction facing) {
        Direction right = facing.getClockWise();
        List<BlockPos> out = new ArrayList<>(9);
        for (int v = 1; v <= INNER_HEIGHT; v++) {
            for (int u = -INNER_HALF; u <= INNER_HALF; u++) {
                out.add(core.relative(right, u).above(v));
            }
        }
        return out;
    }

    /** Where the willow logs go. Excludes the core's own position. */
    public static List<BlockPos> frame(BlockPos core, Direction facing) {
        Direction right = facing.getClockWise();
        int edge = INNER_HALF + 1;
        int top = INNER_HEIGHT + 1;
        List<BlockPos> out = new ArrayList<>(LOG_COUNT);

        for (int u = -edge; u <= edge; u++) {
            if (u != 0) out.add(core.relative(right, u));   // bottom row, around the core
            out.add(core.relative(right, u).above(top));    // top row
        }
        for (int v = 1; v <= INNER_HEIGHT; v++) {
            out.add(core.relative(right, -edge).above(v));
            out.add(core.relative(right, edge).above(v));
        }
        return out;
    }

    /**
     * Can a cell of the opening be lit? Air, an already-placed portal, or anything the
     * portal is allowed to push aside: short grass and flowers, a snow layer, vines, water.
     * A player who builds the gateway in a meadow or a wet fen should not have to weed it.
     */
    public static boolean isClear(BlockState state) {
        return state.isAir() || state.is(BlockRegistry.GATEWAY_PORTAL.get()) || state.canBeReplaced();
    }

    /** How many of the fifteen log positions are not willow logs. */
    public static int missingLogs(LevelReader level, BlockPos core, Direction facing) {
        int missing = 0;
        for (BlockPos pos : frame(core, facing)) {
            if (!level.getBlockState(pos).is(BlockRegistry.WILLOW_LOGS_TAG)) missing++;
        }
        return missing;
    }

    /** How many cells of the opening hold something the portal cannot replace. */
    public static int blockedCells(LevelReader level, BlockPos core, Direction facing) {
        int blocked = 0;
        for (BlockPos pos : interior(core, facing)) {
            if (!isClear(level.getBlockState(pos))) blocked++;
        }
        return blocked;
    }

    /**
     * Is the wall built and the hole clear, in the plane {@code facing} looks across?
     *
     * <p>Interior cells may be air or already-placed portal, so this stays true while the
     * gateway is open and can be used to notice a log being knocked out.</p>
     */
    public static boolean isComplete(LevelReader level, BlockPos core, Direction facing) {
        return missingLogs(level, core, facing) == 0 && blockedCells(level, core, facing) == 0;
    }

    /**
     * Which way a finished wall around this core stands, or null if there is none.
     *
     * <p>Both planes are tried: the one the core faces across and the one at right angles
     * to it. The result is the facing of that plane whose front is nearest {@code from} —
     * where the player opening the gateway is standing — so familiars step out towards
     * them. With no {@code from}, the core's own facing wins ties.</p>
     */
    @Nullable
    public static Direction findFacing(LevelReader level, BlockPos core, Direction facing, @Nullable Vec3 from) {
        if (isComplete(level, core, facing)) return towards(core, facing, from);
        Direction side = facing.getClockWise();
        if (isComplete(level, core, side)) return towards(core, side, from);
        return null;
    }

    /**
     * The plane that comes closest to a finished gateway, for telling the player what is
     * wrong: fewer missing logs wins, the core's own facing on a tie.
     */
    public static Direction nearestFacing(LevelReader level, BlockPos core, Direction facing) {
        Direction side = facing.getClockWise();
        return missingLogs(level, core, side) < missingLogs(level, core, facing) ? side : facing;
    }

    /** Of {@code d} and its opposite, the one that points from the core towards {@code from}. */
    private static Direction towards(BlockPos core, Direction d, @Nullable Vec3 from) {
        if (from == null) return d;
        double along = (from.x - (core.getX() + 0.5)) * d.getStepX()
                + (from.z - (core.getZ() + 0.5)) * d.getStepZ();
        return along < 0 ? d.getOpposite() : d;
    }

    /** The volume an item has to be inside to fall through the gateway. */
    public static AABB catchBox(BlockPos core, Direction facing) {
        List<BlockPos> interior = interior(core, facing);
        AABB box = new AABB(interior.get(0));
        for (BlockPos pos : interior) box = box.minmax(new AABB(pos));
        // A hair of slack, so an item resting against the inside of the frame still counts.
        return box.inflate(0.15);
    }

    /** Where a familiar steps out. The middle of the hole, on the ground in front. */
    public static BlockPos exit(BlockPos core, Direction facing) {
        return core.above().relative(facing);
    }

    /** Fills the hole, pushing aside anything {@link #isClear} allows. Assumes {@link #isComplete} already passed. */
    public static void openPortal(Level level, BlockPos core, Direction facing) {
        var portal = BlockRegistry.GATEWAY_PORTAL.get().defaultBlockState()
                .setValue(GatewayPortalBlock.AXIS, facing.getClockWise().getAxis());
        for (BlockPos pos : interior(core, facing)) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockRegistry.GATEWAY_PORTAL.get()) && isClear(state)) level.setBlock(pos, portal, 3);
        }
    }

    /** Clears the hole of portal blocks, leaving anything else alone. */
    public static void closePortal(Level level, BlockPos core, Direction facing) {
        for (BlockPos pos : interior(core, facing)) {
            if (level.getBlockState(pos).is(BlockRegistry.GATEWAY_PORTAL.get())) {
                level.removeBlock(pos, false);
            }
        }
    }
}
