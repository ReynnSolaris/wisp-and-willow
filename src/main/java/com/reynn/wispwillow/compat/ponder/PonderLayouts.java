package com.reynn.wispwillow.compat.ponder;

import com.reynn.wispwillow.altar.AltarTier;
import com.reynn.wispwillow.blocks.RuneType;
import com.reynn.wispwillow.blocks.SigilBlock;
import com.reynn.wispwillow.blocks.SigilType;
import com.reynn.wispwillow.blocks.alchemy.CauldronPattern;
import com.reynn.wispwillow.blocks.fluxia.GatewayCoreBlock;
import com.reynn.wispwillow.blocks.fluxia.GatewayFrame;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.ritual.RitualDefinitions;
import com.reynn.wispwillow.ritual.RitualRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The ground every Ponder scene stands on, derived from the same tables the blocks check
 * against — so a scene cannot show a circle the altar would reject, or a ring the cauldron
 * would call broken.
 *
 * <p>Deliberately free of Ponder imports. Datagen writes these out as the scenes' schematic
 * files, and datagen has to run whether or not Ponder is installed.</p>
 *
 * <p>Every plate is square, with its floor at y = 0 and whatever stands in the middle of it at
 * {@link #centre}.</p>
 */
public final class PonderLayouts {
    private PonderLayouts() {}

    public static final String ALTAR_CASTING = "altar_casting";
    public static final String ALTAR_FOUNDATION = "altar_foundation";
    public static final String CAULDRON = "alchemical_cauldron";
    public static final String GATEWAY = "soul_gateway";

    /** The rite the casting scene walks through: the first circle every apprentice draws. */
    private static final String INTRO_RITE = "minor_focus";

    public static final int FOUNDATION_SIZE = AltarTier.ringRadius(AltarTier.MAX_TIER - 1) * 2 + 1;
    public static final int CAULDRON_SIZE = CauldronPattern.RADIUS * 2 + 1;
    public static final int GATEWAY_SIZE = 5;

    /** Toward the default camera, so the wall is seen face-on rather than edge-on. */
    public static final Direction GATEWAY_FACING = Direction.SOUTH;

    /** One schematic: a plate {@code size} across and {@code height} tall. Unlisted cells are air. */
    public record Layout(String id, int size, int height, Map<BlockPos, BlockState> blocks) {}

    public static List<Layout> all() {
        List<Layout> out = new ArrayList<>();
        out.add(casting());
        out.add(foundation());
        for (RitualDefinitions.Def def : RitualDefinitions.all()) out.add(ritual(def));
        out.add(cauldron());
        out.add(gateway());
        return out;
    }

    // ------------------------------------------------------------------
    // Shared pieces
    // ------------------------------------------------------------------

    /** A quiet two-tone floor, mid-grey so pale and dark chalk both read against it. */
    public static BlockState floor(int x, int z) {
        return (((x + z) & 1) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.ANDESITE).defaultBlockState();
    }

    /** Where the centrepiece stands on a plate of the given size. */
    public static BlockPos centre(int size) {
        return new BlockPos(size / 2, 1, size / 2);
    }

    /** Every cell on the square ring of radius {@code r} around {@code centre}, at its height. */
    public static List<BlockPos> ring(BlockPos centre, int r) {
        List<BlockPos> out = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) == r) out.add(centre.offset(dx, 0, dz));
            }
        }
        return out;
    }

    public static BlockState rune(RuneType type) {
        return BlockRegistry.RUNES.get(type).get().defaultBlockState();
    }

    private static Map<BlockPos, BlockState> plate(int size) {
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) out.put(new BlockPos(x, 0, z), floor(x, z));
        }
        return out;
    }

    // ------------------------------------------------------------------
    // The altar and its rites
    // ------------------------------------------------------------------

    public static RitualDefinitions.Def introRite() {
        RitualDefinitions.Def def = RitualDefinitions.byPath(INTRO_RITE);
        return def != null ? def : RitualDefinitions.all().get(0);
    }

    public static String ritualId(RitualDefinitions.Def def) {
        return "ritual_" + def.path();
    }

    /** Room for the circle and the foundation under it, plus a block of margin all round. */
    public static int ritualSize(RitualDefinitions.Def def) {
        int r = Math.max(def.radius(), foundationRadius(def.minTier()));
        return (r + 1) * 2 + 1;
    }

    /** Radius of the outermost rune ring a tier needs; nothing for a bare altar. */
    public static int foundationRadius(int tier) {
        return tier <= 1 ? 0 : AltarTier.ringRadius(tier - 1);
    }

    /**
     * The glyphs a rite demands, placed around {@code altar}. Optional guide marks are left
     * out, as the grimoire's own diagram leaves them out: the altar never looks at them.
     */
    public static Map<BlockPos, BlockState> glyphs(RitualDefinitions.Def def, BlockPos altar) {
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        for (RitualRecipe.Cell c : def.pattern()) {
            if (!c.required() || (c.dx() == 0 && c.dz() == 0)) continue;
            out.put(altar.offset(c.dx(), 0, c.dz()), BlockRegistry.SIGIL.get().defaultBlockState()
                    .setValue(SigilBlock.TYPE, c.type().orElse(SigilType.BASE))
                    .setValue(SigilBlock.COLOR, c.color().orElse(DyeColor.WHITE)));
        }
        return out;
    }

    private static Layout casting() {
        RitualDefinitions.Def def = introRite();
        int size = ritualSize(def);
        BlockPos altar = centre(size);
        Map<BlockPos, BlockState> m = plate(size);
        m.put(altar, BlockRegistry.ALTAR.get().defaultBlockState());
        m.putAll(glyphs(def, altar));
        return new Layout(ALTAR_CASTING, size, 2, m);
    }

    /** A bare altar on a plate wide enough for every ring. The scene lays the runes itself. */
    private static Layout foundation() {
        Map<BlockPos, BlockState> m = plate(FOUNDATION_SIZE);
        m.put(centre(FOUNDATION_SIZE), BlockRegistry.ALTAR.get().defaultBlockState());
        return new Layout(ALTAR_FOUNDATION, FOUNDATION_SIZE, 2, m);
    }

    /** The finished circle on the foundation its tier needs, laid in blank runes: any rune counts. */
    public static Layout ritual(RitualDefinitions.Def def) {
        int size = ritualSize(def);
        BlockPos altar = centre(size);
        Map<BlockPos, BlockState> m = plate(size);
        for (int r = 1; r < def.minTier(); r++) {
            for (BlockPos p : ring(altar.below(), AltarTier.ringRadius(r))) m.put(p, rune(RuneType.BLANK));
        }
        m.put(altar, BlockRegistry.ALTAR.get().defaultBlockState());
        m.putAll(glyphs(def, altar));
        return new Layout(ritualId(def), size, 2, m);
    }

    // ------------------------------------------------------------------
    // The Alchemical Cauldron
    // ------------------------------------------------------------------

    public static BlockPos cauldronCore() {
        return centre(CAULDRON_SIZE);
    }

    /** The ring around the pot, each slot filled with the building block it accepts. */
    public static Map<BlockPos, BlockState> cauldronRing() {
        BlockPos core = cauldronCore();
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        for (CauldronPattern.Slot slot : CauldronPattern.slots()) {
            BlockState state = satisfying(slot);
            if (state != null) out.put(core.offset(slot.offset()), state);
        }
        return out;
    }

    /**
     * The block a slot wants, found by asking the slot's own test rather than keeping a second
     * list that could disagree with it. Slots that must stay open accept air and get nothing.
     */
    private static BlockState satisfying(CauldronPattern.Slot slot) {
        if (slot.test().test(Blocks.AIR.defaultBlockState())) return null;
        for (Block block : List.of(BlockRegistry.FLUXITE.get(), BlockRegistry.WILLOW_LOG.get(),
                BlockRegistry.ASTRALITE_BLOCK.get())) {
            if (slot.test().test(block.defaultBlockState())) return block.defaultBlockState();
        }
        throw new IllegalStateException("No Ponder block satisfies cauldron slot: " + slot.describe());
    }

    private static Layout cauldron() {
        Map<BlockPos, BlockState> m = plate(CAULDRON_SIZE);
        m.put(cauldronCore(), BlockRegistry.ALCHEMICAL_CAULDRON.get().defaultBlockState());
        m.putAll(cauldronRing());
        int height = 1;
        for (BlockPos p : m.keySet()) height = Math.max(height, p.getY() + 1);
        return new Layout(CAULDRON, CAULDRON_SIZE, height, m);
    }

    // ------------------------------------------------------------------
    // The Soul Gateway
    // ------------------------------------------------------------------

    public static BlockPos gatewayCore() {
        return centre(GATEWAY_SIZE);
    }

    public static List<BlockPos> gatewayFrame() {
        return GatewayFrame.frame(gatewayCore(), GATEWAY_FACING);
    }

    public static List<BlockPos> gatewayInterior() {
        return GatewayFrame.interior(gatewayCore(), GATEWAY_FACING);
    }

    private static Layout gateway() {
        BlockPos core = gatewayCore();
        Map<BlockPos, BlockState> m = plate(GATEWAY_SIZE);
        m.put(core, BlockRegistry.GATEWAY_CORE.get().defaultBlockState()
                .setValue(GatewayCoreBlock.FACING, GATEWAY_FACING));

        int lintel = core.getY();
        for (BlockPos p : gatewayFrame()) lintel = Math.max(lintel, p.getY());

        // Uprights stand on end; the sill and the lintel lie along the wall.
        Direction.Axis along = GATEWAY_FACING.getClockWise().getAxis();
        for (BlockPos p : gatewayFrame()) {
            boolean flat = p.getY() == core.getY() || p.getY() == lintel;
            m.put(p, BlockRegistry.WILLOW_LOG.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, flat ? along : Direction.Axis.Y));
        }
        return new Layout(GATEWAY, GATEWAY_SIZE, lintel + 1, m);
    }
}
