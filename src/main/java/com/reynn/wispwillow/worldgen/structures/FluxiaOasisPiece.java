package com.reynn.wispwillow.worldgen.structures;

import com.reynn.wispwillow.WispWillow;
import com.mojang.serialization.DynamicOps;
import com.reynn.wispwillow.blocks.ModifiedLeavesBlock;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.StructureRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The whole oasis: the cavern, the pool, the wards, and the vault sealed underneath.
 *
 * <p><b>The puzzle.</b> The pool's basin is lined with
 * {@link BlockRegistry#WARDED_SLATE Warded Slate}, so going under and phasing drops you onto the
 * bottom rather than through it. Set into that floor are four small squares of crying obsidian —
 * four weeping stones. Three are backed by more ward a block below and go nowhere. The fourth,
 * the one on the same side as the willow, opens onto four blocks of ordinary deepslate and then
 * the vault, which is otherwise sealed on all six sides. The sign by the pool is the clue, and
 * the way back out is the ladder shaft, which is capped by a single block of moss you break from
 * underneath.</p>
 *
 * <p><b>Determinism.</b> {@code postProcess} runs once per chunk the piece touches, each time
 * with a different {@code random}, so nothing here may draw from it — two chunks would disagree
 * about the same block. Every choice is a hash of the world position and the piece's own seed
 * instead, which gives the same answer no matter which chunk asks.</p>
 *
 * <p><b>Sealing.</b> Every solid block facing the cavern comes from this piece's own palette
 * rather than being left as stone. That is what keeps the oasis dry: an aquifer cut open by the
 * dome would otherwise pour in, and springs place only against vanilla stone.</p>
 */
public class FluxiaOasisPiece extends StructurePiece {

    /** The chest loot in the vault. Also what the "Beneath the Veil" advancement watches for. */
    public static final ResourceKey<LootTable> CACHE = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "fluxia_oasis_cache"));

    // Geometry, relative to the origin. The floor's top block is at dy = -1, so dy = 0 is the
    // air a player stands in, and everything below -3 is under the pool.
    public static final int CAVERN_R = 13;
    public static final int DOME_HEIGHT = 9;
    private static final int SHELL = 2;

    private static final int POOL_R = 6;
    private static final double BASIN_R = POOL_R + 1.6;
    private static final int POOL_DEPTH = 3;
    private static final int WARD_Y = -4;

    private static final int VAULT_CEILING = -8;
    private static final int VAULT_AIR_TOP = -9;
    private static final int VAULT_AIR_FLOOR = -13;
    private static final int VAULT_FLOOR = -14;
    private static final int VAULT_R = 3;
    private static final int VAULT_WALL = 4;

    private static final int SHAFT_X = 8;

    /** How far below the origin the piece reaches, so the structure can refuse to hang out of the world. */
    public static final int DEPTH_BELOW = -VAULT_FLOOR + 1;

    private BlockPos origin;
    private long seed;

    public FluxiaOasisPiece(BlockPos origin, long seed) {
        super(StructureRegistry.FLUXIA_OASIS_PIECE.get(), 0, boxAround(origin));
        this.origin = origin;
        this.seed = seed;
        this.setOrientation(null);
    }

    public FluxiaOasisPiece(CompoundTag tag) {
        super(StructureRegistry.FLUXIA_OASIS_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getInt("OX"), tag.getInt("OY"), tag.getInt("OZ"));
        this.seed = tag.getLong("Seed");
    }

    private static BoundingBox boxAround(BlockPos origin) {
        int reach = CAVERN_R + SHELL;
        return new BoundingBox(
                origin.getX() - reach, origin.getY() - DEPTH_BELOW, origin.getZ() - reach,
                origin.getX() + reach, origin.getY() + DOME_HEIGHT + SHELL, origin.getZ() + reach);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OX", origin.getX());
        tag.putInt("OY", origin.getY());
        tag.putInt("OZ", origin.getZ());
        tag.putLong("Seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        Direction shade = Direction.from2DDataValue((int) (hash(0, 0, 0, 17) * 4.0f) & 3);

        carveCavern(level, box);
        layFloor(level, box);
        buildVault(level, box);
        fillPool(level, box);
        weepingStones(level, box, shade);
        decorate(level, box);
        raiseWillow(level, box, shade);
        placeSign(level, box, shade);
        // The exit goes in last. The lamp ring around the pool wants the same block as the
        // shaft's cap, and the cap has to win: a lamp set in the cavern floor is exactly the
        // sort of thing a player notices, and it would hand them the vault for nothing.
        cutExit(level, box);
        stockVault(level, box, random);
    }

    // ------------------------------------------------------------------
    // The cavern
    // ------------------------------------------------------------------

    /** Half an ellipsoid of air, wrapped in two blocks of the oasis's own stone. */
    private void carveCavern(WorldGenLevel level, BoundingBox box) {
        int reach = CAVERN_R + SHELL;
        for (int dy = 0; dy <= DOME_HEIGHT + SHELL; dy++) {
            double radius = domeRadius(dy);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (dy <= DOME_HEIGHT && d <= radius) {
                        place(level, box, dx, dy, dz, Blocks.CAVE_AIR.defaultBlockState());
                    } else if (d <= radius + SHELL) {
                        place(level, box, dx, dy, dz, shellState(dx, dy, dz));
                    }
                }
            }
        }
    }

    private static double domeRadius(int dy) {
        double t = Math.min(1.0, (double) dy / DOME_HEIGHT);
        return CAVERN_R * Math.sqrt(Math.max(0.0, 1.0 - t * t));
    }

    private BlockState shellState(int dx, int dy, int dz) {
        float roll = hash(dx, dy, dz, 3);
        if (roll < 0.05f) return BlockRegistry.FLUXITE.get().defaultBlockState();
        if (roll < 0.13f) return Blocks.AMETHYST_BLOCK.defaultBlockState();
        if (roll < 0.50f) return Blocks.CALCITE.defaultBlockState();
        if (roll < 0.85f) return Blocks.SMOOTH_BASALT.defaultBlockState();
        return Blocks.TUFF.defaultBlockState();
    }

    /** Three blocks of ground under the whole cavern, mossy on top. */
    private void layFloor(WorldGenLevel level, BoundingBox box) {
        int reach = CAVERN_R + SHELL;
        for (int dy = -3; dy <= -1; dy++) {
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    if (Math.sqrt(dx * dx + dz * dz) > CAVERN_R + SHELL) continue;
                    BlockState state;
                    if (dy == -1) {
                        float roll = hash(dx, dy, dz, 5);
                        state = roll < 0.62f ? Blocks.MOSS_BLOCK.defaultBlockState()
                                : roll < 0.82f ? Blocks.ROOTED_DIRT.defaultBlockState()
                                : Blocks.TUFF.defaultBlockState();
                    } else {
                        state = Blocks.DEEPSLATE.defaultBlockState();
                    }
                    place(level, box, dx, dy, dz, state);
                }
            }
        }
    }

    /** The pool itself, and the warded basin that holds it. */
    private void fillPool(WorldGenLevel level, BoundingBox box) {
        BlockState fluxia = BlockRegistry.FLUXIA_FLUID_BLOCK.get().defaultBlockState();
        BlockState ward = BlockRegistry.WARDED_SLATE.get().defaultBlockState();

        for (int dy = -POOL_DEPTH; dy <= -1; dy++) {
            for (int dx = -POOL_R - 2; dx <= POOL_R + 2; dx++) {
                for (int dz = -POOL_R - 2; dz <= POOL_R + 2; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d <= POOL_R) {
                        place(level, box, dx, dy, dz, fluxia);
                    } else if (d <= BASIN_R) {
                        place(level, box, dx, dy, dz, ward);
                    }
                }
            }
        }

        for (int dx = -POOL_R - 2; dx <= POOL_R + 2; dx++) {
            for (int dz = -POOL_R - 2; dz <= POOL_R + 2; dz++) {
                if (Math.sqrt(dx * dx + dz * dz) <= BASIN_R) {
                    place(level, box, dx, WARD_Y, dz, ward);
                }
            }
        }
    }

    /**
     * Four weeping stones set into the warded floor, one per compass point.
     *
     * <p>Only the one on the willow's side is thin. The other three are ward a block down: you
     * sink in up to your knees, stop, and have to come back up and think again.</p>
     */
    private void weepingStones(WorldGenLevel level, BoundingBox box, Direction shade) {
        BlockState weeping = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState ward = BlockRegistry.WARDED_SLATE.get().defaultBlockState();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();

        for (Direction dir : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            boolean thin = dir == shade;
            for (int i = 0; i <= 1; i++) {
                for (int j = 0; j <= 1; j++) {
                    int dx = dir.getStepX() * 2 + i * dir.getStepX() + j * Math.abs(dir.getStepZ());
                    int dz = dir.getStepZ() * 2 + i * dir.getStepZ() + j * Math.abs(dir.getStepX());

                    place(level, box, dx, WARD_Y, dz, weeping);
                    if (thin) {
                        // Straight down to the vault, ceiling included: this is the way in.
                        for (int dy = WARD_Y - 1; dy >= VAULT_CEILING; dy--) {
                            place(level, box, dx, dy, dz, deepslate);
                        }
                    } else {
                        place(level, box, dx, WARD_Y - 1, dz, ward);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // The vault
    // ------------------------------------------------------------------

    private void buildVault(WorldGenLevel level, BoundingBox box) {
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState ward = BlockRegistry.WARDED_SLATE.get().defaultBlockState();

        // Bed the whole vault into solid rock first, so it never hangs open in a cave.
        for (int dy = VAULT_FLOOR - 1; dy <= WARD_Y - 1; dy++) {
            for (int dx = -VAULT_WALL - 1; dx <= VAULT_WALL + 1; dx++) {
                for (int dz = -VAULT_WALL - 1; dz <= VAULT_WALL + 1; dz++) {
                    place(level, box, dx, dy, dz, deepslate);
                }
            }
        }

        // Six warded faces.
        for (int dy = VAULT_FLOOR; dy <= VAULT_CEILING; dy++) {
            for (int dx = -VAULT_WALL; dx <= VAULT_WALL; dx++) {
                for (int dz = -VAULT_WALL; dz <= VAULT_WALL; dz++) {
                    boolean face = dy == VAULT_FLOOR || dy == VAULT_CEILING
                            || Math.abs(dx) == VAULT_WALL || Math.abs(dz) == VAULT_WALL;
                    if (face) place(level, box, dx, dy, dz, ward);
                }
            }
        }

        // And the room inside them.
        for (int dy = VAULT_AIR_FLOOR; dy <= VAULT_AIR_TOP; dy++) {
            for (int dx = -VAULT_R; dx <= VAULT_R; dx++) {
                for (int dz = -VAULT_R; dz <= VAULT_R; dz++) {
                    place(level, box, dx, dy, dz, Blocks.CAVE_AIR.defaultBlockState());
                }
            }
        }
    }

    /**
     * The way out: a corridor east out of the vault and a ladder back up to the cavern floor,
     * capped with one block of moss. Nothing gives it away from above, and nothing about it
     * helps you get in — the corridor pierces the ward from the inside.
     */
    private void cutExit(WorldGenLevel level, BoundingBox box) {
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);

        // Sleeve the corridor and the shaft, then hollow them out.
        for (int dx = VAULT_R; dx <= SHAFT_X + 1; dx++) {
            for (int dy = VAULT_AIR_FLOOR - 1; dy <= VAULT_AIR_FLOOR + 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    place(level, box, dx, dy, dz, deepslate);
                }
            }
        }
        for (int dy = VAULT_AIR_FLOOR; dy <= -2; dy++) {
            for (int dx = SHAFT_X - 1; dx <= SHAFT_X + 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    place(level, box, dx, dy, dz, deepslate);
                }
            }
        }

        for (int dx = VAULT_WALL; dx <= SHAFT_X; dx++) {
            place(level, box, dx, VAULT_AIR_FLOOR, 0, air);
            place(level, box, dx, VAULT_AIR_FLOOR + 1, 0, air);
        }
        // Ladders the whole way, rather than air with a ladder on one wall: a one-block shaft
        // is easier to climb out of when every block of it is climbable.
        for (int dy = VAULT_AIR_FLOOR; dy <= -2; dy++) {
            place(level, box, SHAFT_X, dy, 0, ladder);
        }
        place(level, box, SHAFT_X, -1, 0, Blocks.MOSS_BLOCK.defaultBlockState());
    }

    private void stockVault(WorldGenLevel level, BoundingBox box, RandomSource random) {
        BlockState lamp = BlockRegistry.FLUXIA_LAMP.get().defaultBlockState();
        for (int dx = -VAULT_R; dx <= VAULT_R; dx += VAULT_R * 2) {
            for (int dz = -VAULT_R; dz <= VAULT_R; dz += VAULT_R * 2) {
                place(level, box, dx, VAULT_AIR_FLOOR, dz, lamp);
            }
        }
        place(level, box, 0, VAULT_AIR_TOP, 0,
                Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN));
        place(level, box, 0, VAULT_AIR_FLOOR, 0, BlockRegistry.ASTRALITE_BLOCK.get().defaultBlockState());

        createChest(level, box, random, world(-2, VAULT_AIR_FLOOR, 0), CACHE,
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));
        createChest(level, box, random, world(2, VAULT_AIR_FLOOR, 0), CACHE,
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
    }

    // ------------------------------------------------------------------
    // Dressing
    // ------------------------------------------------------------------

    private void decorate(WorldGenLevel level, BoundingBox box) {
        for (int dx = -CAVERN_R; dx <= CAVERN_R; dx++) {
            for (int dz = -CAVERN_R; dz <= CAVERN_R; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > CAVERN_R) continue;

                // Ceiling: whatever hangs from the highest air block over this column.
                int ceiling = ceilingAt(d);
                if (ceiling >= 2) {
                    float roll = hash(dx, ceiling, dz, 11);
                    if (roll < 0.05f) {
                        place(level, box, dx, ceiling, dz, Blocks.AMETHYST_CLUSTER.defaultBlockState()
                                .setValue(AmethystClusterBlock.FACING, Direction.DOWN));
                    } else if (roll < 0.10f) {
                        place(level, box, dx, ceiling, dz, Blocks.CAVE_VINES_PLANT.defaultBlockState()
                                .setValue(CaveVines.BERRIES, true));
                        place(level, box, dx, ceiling - 1, dz, Blocks.CAVE_VINES.defaultBlockState()
                                .setValue(CaveVines.BERRIES, true));
                    } else if (roll < 0.16f) {
                        place(level, box, dx, ceiling, dz, Blocks.GLOW_LICHEN.defaultBlockState()
                                .setValue(MultifaceBlock.getFaceProperty(Direction.UP), true));
                    }
                }

                // Floor: moss, the odd flower, and a ring of lamps around the pool.
                if (d <= POOL_R + 0.5) continue;
                float roll = hash(dx, 0, dz, 13);
                if (d > POOL_R + 1.5 && d < POOL_R + 2.5 && (dx == 0 || dz == 0)) {
                    place(level, box, dx, -1, dz, BlockRegistry.FLUXIA_LAMP.get().defaultBlockState());
                } else if (roll < 0.14f) {
                    place(level, box, dx, 0, dz, Blocks.MOSS_CARPET.defaultBlockState());
                } else if (roll < 0.17f) {
                    place(level, box, dx, 0, dz, BlockRegistry.LUXINIAS_FLOWER.get().defaultBlockState());
                } else if (roll < 0.19f) {
                    place(level, box, dx, 0, dz, BlockRegistry.SOLROSA_FLOWER.get().defaultBlockState());
                }
            }
        }
    }

    /** The highest block of air over a column this far from the middle, or -1 if there is none. */
    private static int ceilingAt(double d) {
        for (int dy = DOME_HEIGHT; dy >= 0; dy--) {
            if (d <= domeRadius(dy)) return dy;
        }
        return -1;
    }

    /** One willow, on the side of the pool the clue points to. */
    private void raiseWillow(WorldGenLevel level, BoundingBox box, Direction shade) {
        int tx = shade.getStepX() * 9;
        int tz = shade.getStepZ() * 9;
        BlockState log = BlockRegistry.WILLOW_LOG.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        // Willow leaves are this mod's own block, not a vanilla LeavesBlock: their distance
        // property has a different range entirely, and setting vanilla's throws. Persistent is
        // the only value that matters here — it is what keeps the tree from decaying away.
        BlockState leaves = BlockRegistry.WILLOW_LEAVES.get().defaultBlockState()
                .setValue(ModifiedLeavesBlock.PERSISTENT, true);

        for (int dy = 0; dy <= 4; dy++) {
            place(level, box, tx, dy, tz, log);
        }
        for (int dy = 3; dy <= 6; dy++) {
            int r = dy >= 6 ? 1 : (dy == 5 ? 3 : 2);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r + 1) continue;
                    if (dx == 0 && dz == 0 && dy <= 4) continue;
                    place(level, box, tx + dx, dy, tz + dz, leaves);
                }
            }
        }
        // The droop, which is the whole reason the tree reads as a willow.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz < 5 || dx * dx + dz * dz > 10) continue;
                if (hash(tx + dx, 0, tz + dz, 19) > 0.55f) continue;
                for (int dy = 4; dy >= 2; dy--) {
                    place(level, box, tx + dx, dy, tz + dz, leaves);
                }
            }
        }
    }

    private void placeSign(WorldGenLevel level, BoundingBox box, Direction shade) {
        int sx = -shade.getStepX() * 8;
        int sz = -shade.getStepZ() * 8;
        int rotation = Mth.floor(shade.toYRot() * 16.0 / 360.0 + 0.5) & 15;

        place(level, box, sx, -1, sz, Blocks.MOSS_BLOCK.defaultBlockState());
        place(level, box, sx, 0, sz,
                Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation));

        BlockPos pos = world(sx, 0, sz);
        if (!box.isInside(pos)) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SignBlockEntity sign) {
            SignText text = new SignText();
            for (int line = 0; line < 4; line++) {
                text = text.setMessage(line, Component.translatable(
                        "sign." + WispWillow.MODID + ".oasis." + (line + 1)));
            }
            writeSignText(level, sign, text);
        }
    }

    /**
     * Writes a sign's text the way generation has to.
     *
     * <p>{@code SignBlockEntity#setText} ends in {@code markUpdated()}, which dereferences the
     * block entity's level to broadcast the change. While a chunk is being generated the sign
     * lives in a ProtoChunk and has no level at all, so that call takes the whole chunk down
     * with a {@code NullPointerException} — and it cannot be caught by placing this piece into
     * a world that is already running, because there the block entity does have a level.</p>
     *
     * <p>Vanilla's own template placement never uses the setter either: it loads the data
     * straight into the block entity, which is what this does.</p>
     */
    private static void writeSignText(WorldGenLevel level, SignBlockEntity sign, SignText text) {
        HolderLookup.Provider registries = level.registryAccess();
        DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        SignText.DIRECT_CODEC.encodeStart(ops, text)
                .resultOrPartial(error -> WispWillow.LOGGER.error("Fluxia Oasis sign: {}", error))
                .ifPresent(encoded -> {
                    CompoundTag tag = new CompoundTag();
                    tag.put("front_text", encoded);
                    sign.loadWithComponents(tag, registries);
                });
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private BlockPos world(int dx, int dy, int dz) {
        return new BlockPos(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
    }

    private void place(WorldGenLevel level, BoundingBox box, int dx, int dy, int dz, BlockState state) {
        BlockPos pos = world(dx, dy, dz);
        if (!box.isInside(pos)) return;
        // Flag 2 and no neighbour updates: a source block placed at a chunk edge must not start
        // flowing before the chunk that closes the basin has had its turn.
        level.setBlock(pos, state, 2);
    }

    /**
     * A stable roll for one block of this oasis.
     *
     * <p>Seeded by position and by the piece, never by the chunk's random, so the answer does
     * not depend on which chunk was generated first.</p>
     */
    private float hash(int dx, int dy, int dz, int salt) {
        long h = seed + Mth.getSeed(dx, dy, dz) + salt * 0x9E3779B97F4A7C15L;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (float) ((h >>> 11) * 0x1.0p-53);
    }
}
