package com.reynn.wispwillow.blocks.alchemy;

import com.reynn.wispwillow.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The Alchemical Cauldron multiblock: a pot standing in a circle, under four pillars.
 *
 * <pre>
 *   y = +2     A . . . A        A = Astralite Block   (the caps)
 *              . . . . .        W = Willow Log        (the pillars, two tall)
 *              . . o . .        F = Fluxite           (the circle)
 *              . . . . .        C = Alchemical Cauldron, the core and the origin
 *              A . . . A        o = must be open
 *                               . = unconstrained
 *   y = +1     W . . . W
 *              . . . . .
 *              . . o . .
 *              . . . . .
 *              W . . . W
 *
 *   y =  0     W . . . W
 *              . . . . .
 *              . . C . .
 *              . . . . .
 *              W . . . W
 *
 *   y = -1     . . F . .
 *              . F F F .
 *              F F _ F F        _ = deliberately unconstrained
 *              . F F F .
 *              . . F . .
 * </pre>
 *
 * <p>Deliberately rotationally symmetric: there is no FACING on the core, so there is no
 * way for the pattern check and the rendered model to disagree about which way is front.
 * (The gateway core has exactly that bug — FACING plus a cubeAll model.)</p>
 *
 * <p>The footprint is five across rather than three. A three-wide ring put the structural
 * blocks hard against the pot on every side, which read as a crate around a cauldron rather
 * than a place where something is done: there was nowhere to stand, nothing to walk between,
 * and the pillars were too short to be pillars. Pushing the corners out to ±2 and standing
 * them two blocks tall costs eight more blocks and buys a courtyard.</p>
 *
 * <p>Two cells are unconstrained on purpose, and both for the same reason — the block entity
 * hands out items and fluid through its faces, so the pattern must not seal the faces it
 * relies on:</p>
 * <ul>
 *   <li>the centre of the circle, directly under the pot, where the outputs come out. Lay
 *       fluxite there for a solid-looking floor, or a hopper to drain it; both are correct.</li>
 *   <li>everything at the pot's own level inside the pillars, so a pipe, an importer or a
 *       chest can sit against its side without the ring reporting itself broken.</li>
 * </ul>
 *
 * <p>The centre column above the cauldron is required to be open. That is not decoration:
 * the renderer throws its light and particles straight up, and a roofed cauldron would
 * swallow the only feedback the player gets that the thing is running.</p>
 */
public final class CauldronPattern {

    private CauldronPattern() {}

    /** How far the ring reaches from the core, in blocks. Used for render bounds. */
    public static final int RADIUS = 2;

    /** Total structural blocks, excluding the core itself. Used by the book page. */
    public static final int BLOCK_COUNT = 24;

    /** One position in the pattern, relative to the core, and what is allowed to be there. */
    public record Slot(BlockPos offset, Predicate<BlockState> test, String describe) {}

    private static Predicate<BlockState> is(java.util.function.Supplier<? extends Block> block) {
        return state -> state.is(block.get());
    }

    private static final Predicate<BlockState> OPEN =
            state -> state.isAir() || state.canBeReplaced();

    private static final List<Slot> SLOTS = build();

    private static List<Slot> build() {
        List<Slot> out = new ArrayList<>();

        // y = -1 : a fluxite circle, cut as a diamond so it reads as drawn rather than
        // poured. The centre is left out; see the class comment.
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                if (x == 0 && z == 0) continue;
                if (Math.abs(x) + Math.abs(z) > RADIUS) continue;
                out.add(new Slot(new BlockPos(x, -1, z),
                        is(BlockRegistry.FLUXITE), "Fluxite"));
            }
        }

        // y = 0 and y = +1 : four willow pillars, two blocks tall, at the corners.
        for (int y = 0; y <= 1; y++) {
            for (int x = -RADIUS; x <= RADIUS; x += RADIUS * 2) {
                for (int z = -RADIUS; z <= RADIUS; z += RADIUS * 2) {
                    out.add(new Slot(new BlockPos(x, y, z),
                            is(BlockRegistry.WILLOW_LOG), "Willow Log"));
                }
            }
        }

        // y = +2 : astralite caps on top of the pillars.
        for (int x = -RADIUS; x <= RADIUS; x += RADIUS * 2) {
            for (int z = -RADIUS; z <= RADIUS; z += RADIUS * 2) {
                out.add(new Slot(new BlockPos(x, 2, z),
                        is(BlockRegistry.ASTRALITE_BLOCK), "Astralite Block"));
            }
        }

        // The mouth stays clear so the light and the runes have somewhere to go.
        out.add(new Slot(new BlockPos(0, 1, 0), OPEN, "open sky above the pot"));
        out.add(new Slot(new BlockPos(0, 2, 0), OPEN, "open sky above the pot"));

        return List.copyOf(out);
    }

    public static List<Slot> slots() {
        return SLOTS;
    }

    /** True when every slot around {@code core} is satisfied. */
    public static boolean matches(LevelReader level, BlockPos core) {
        return firstFailure(level, core) == null;
    }

    /**
     * The first slot that does not match, or null if the structure is complete.
     * Returning the offending slot rather than a boolean is what lets the block tell the
     * player <em>what</em> is wrong instead of just refusing to light.
     */
    public static Slot firstFailure(LevelReader level, BlockPos core) {
        for (Slot slot : SLOTS) {
            BlockPos at = core.offset(slot.offset());
            if (!level.hasChunkAt(at)) return slot;
            if (!slot.test().test(level.getBlockState(at))) return slot;
        }
        return null;
    }

    /**
     * Re-checks the structure and writes the result onto the core's blockstate.
     * Safe to call from neighbour updates; it does nothing if the answer has not changed.
     */
    public static void revalidate(Level level, BlockPos core, BlockState state) {
        if (level.isClientSide) return;
        if (!state.hasProperty(AlchemicalCauldronBlock.FORMED)) return;

        boolean now = matches(level, core);
        if (state.getValue(AlchemicalCauldronBlock.FORMED) == now) return;

        level.setBlock(core, state.setValue(AlchemicalCauldronBlock.FORMED, now), Block.UPDATE_ALL);

        // Breaking the ring while it is lit puts the fire out. The fluxia already spent
        // on lighting it is not refunded — same contract as the gateway.
        if (!now && state.getValue(AlchemicalCauldronBlock.LIT)) {
            level.setBlock(core, level.getBlockState(core)
                    .setValue(AlchemicalCauldronBlock.LIT, false), Block.UPDATE_ALL);
        }
    }

    /** The four pillar caps, in world space. The renderer draws its glyphs streaming in from these. */
    public static BlockPos[] pillarCaps(BlockPos core) {
        return new BlockPos[] {
                core.offset(-RADIUS, 2, -RADIUS),
                core.offset(RADIUS, 2, -RADIUS),
                core.offset(-RADIUS, 2, RADIUS),
                core.offset(RADIUS, 2, RADIUS),
        };
    }
}
