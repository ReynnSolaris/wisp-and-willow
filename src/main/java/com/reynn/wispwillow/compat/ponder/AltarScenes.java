package com.reynn.wispwillow.compat.ponder;

import com.reynn.wispwillow.altar.AltarTier;
import com.reynn.wispwillow.blocks.RuneType;
import com.reynn.wispwillow.blocks.SigilBlock;
import com.reynn.wispwillow.blocks.SigilType;
import com.reynn.wispwillow.registry.ItemRegistry;
import com.reynn.wispwillow.ritual.RitualDefinitions;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

/** The altar itself: how a rite is cast, and how the altar is raised to cast bigger ones. */
final class AltarScenes {
    private AltarScenes() {}

    static void casting(SceneBuilder scene, SceneBuildingUtil util) {
        RitualDefinitions.Def def = PonderLayouts.introRite();
        int size = PonderLayouts.ritualSize(def);
        BlockPos altar = PonderLayouts.centre(size);
        Map<BlockPos, BlockState> glyphs = PonderLayouts.glyphs(def, altar);

        scene.title(PonderLayouts.ALTAR_CASTING, PonderText.header(PonderLayouts.ALTAR_CASTING));
        scene.configureBasePlate(0, 0, size);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(altar), Direction.DOWN);
        scene.idle(15);
        SceneKit.say(scene, "altar.intro", util.vector().topOf(altar));

        // Chalk each glyph in the way a player would: one click on the floor per mark.
        for (Map.Entry<BlockPos, BlockState> glyph : glyphs.entrySet()) {
            BlockPos pos = glyph.getKey();
            SceneKit.click(scene, util.vector().topOf(pos.below()), chalkFor(glyph.getValue()), false);
            scene.world().showSection(util.select().position(pos), Direction.DOWN);
            scene.idle(8);
        }
        BlockPos first = glyphs.keySet().iterator().next();
        SceneKit.say(scene, "altar.chalk", SceneKit.onTop(first));

        // Turn the first glyph a few shapes forward, then sneak it back to where it started.
        ItemStack chalk = chalkFor(glyphs.get(first));
        SigilType home = glyphs.get(first).getValue(SigilBlock.TYPE);
        for (int step = 1; step <= 3; step++) reshape(scene, first, chalk, shift(home, step), false);
        SceneKit.say(scene, "altar.cycle", SceneKit.onTop(first));
        for (int step = 2; step >= 0; step--) reshape(scene, first, chalk, shift(home, step), true);
        scene.idle(10);

        scene.overlay().showOutline(PonderPalette.GREEN, "circle", SceneKit.select(util, glyphs.keySet()), 100);
        SceneKit.say(scene, "altar.pattern", null, Component.translatable(def.langKey()).getString());

        List<ElementLink<EntityElement>> reagents = SceneKit.scatter(scene, util, altar, reagents(def), 1.0);
        SceneKit.say(scene, "altar.reagents", null);
        SceneKit.say(scene, "altar.fluxia", util.vector().topOf(altar));
        SceneKit.say(scene, "altar.cast", util.vector().topOf(altar));
        SceneKit.cast(scene, util, altar, reagents);
        def.resultItem().ifPresent(stack -> SceneKit.drop(scene, util, altar, stack));
        scene.idle(30);
        SceneKit.say(scene, "altar.more", null);
        scene.markAsFinished();
    }

    static void foundation(SceneBuilder scene, SceneBuildingUtil util) {
        int size = PonderLayouts.FOUNDATION_SIZE;
        BlockPos altar = PonderLayouts.centre(size);
        BlockPos floor = altar.below();

        scene.title(PonderLayouts.ALTAR_FOUNDATION, PonderText.header(PonderLayouts.ALTAR_FOUNDATION));
        scene.configureBasePlate(0, 0, size);
        SceneKit.fit(scene, size);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(altar), Direction.DOWN);
        scene.idle(15);
        SceneKit.say(scene, "foundation.intro", util.vector().topOf(altar));
        SceneKit.say(scene, "foundation.below", util.vector().topOf(floor.east()));

        // One rune type per ring, so each ring can be pointed at when the types are explained.
        int rings = AltarTier.MAX_TIER - 1;
        for (int ring = 1; ring <= rings; ring++) {
            int radius = AltarTier.ringRadius(ring);
            scene.world().setBlocks(SceneKit.select(util, PonderLayouts.ring(floor, radius)),
                    PonderLayouts.rune(runeFor(ring)), true);
            scene.idle(10);
            SceneKit.say(scene, "foundation.tier", util.vector().topOf(floor.east(radius)),
                    SceneKit.roman(ring + 1), radius, AltarTier.runesForRing(ring));
        }

        // Break the second ring: one gap, and everything from it outward stops counting.
        BlockPos gap = floor.east(AltarTier.ringRadius(2));
        scene.world().setBlock(gap, PonderLayouts.floor(gap.getX(), gap.getZ()), true);
        scene.overlay().showOutline(PonderPalette.RED, "gap", util.select().position(gap), 100);
        SceneKit.say(scene, "foundation.gap", util.vector().topOf(gap));
        scene.world().setBlock(gap, PonderLayouts.rune(runeFor(2)), true);
        scene.idle(15);

        for (int ring = 1; ring <= rings; ring++) {
            RuneType type = runeFor(ring);
            int radius = AltarTier.ringRadius(ring);
            scene.overlay().showOutline(paletteFor(type), "ring" + ring,
                    SceneKit.select(util, PonderLayouts.ring(floor, radius)), 120);
            SceneKit.say(scene, "foundation." + type.getSerializedName(), util.vector().topOf(floor.east(radius)));
        }
        SceneKit.say(scene, "foundation.caps", null);

        // The obvious mistake: runes laid around the altar instead of beneath it.
        BlockPos wrong = altar.east();
        scene.world().setBlock(wrong, PonderLayouts.rune(RuneType.BLANK), false);
        scene.world().showSection(util.select().position(wrong), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.RED, "wrong", util.select().position(wrong), 100);
        SceneKit.say(scene, "foundation.misplaced", util.vector().topOf(wrong));
        scene.world().hideSection(util.select().position(wrong), Direction.UP);
        scene.idle(15);
        scene.markAsFinished();
    }

    private static void reshape(SceneBuilder scene, BlockPos pos, ItemStack chalk, SigilType type, boolean sneaking) {
        SceneKit.click(scene, SceneKit.onTop(pos), chalk, sneaking);
        scene.world().modifyBlock(pos, s -> s.setValue(SigilBlock.TYPE, type), false);
        scene.idle(10);
    }

    private static SigilType shift(SigilType from, int steps) {
        SigilType[] all = SigilType.values();
        return all[Math.floorMod(from.ordinal() + steps, all.length)];
    }

    private static RuneType runeFor(int ring) {
        RuneType[] all = RuneType.values();
        return all[(ring - 1) % all.length];
    }

    private static PonderPalette paletteFor(RuneType type) {
        return switch (type) {
            case BLANK -> PonderPalette.WHITE;
            case WATER -> PonderPalette.BLUE;
            case EARTH -> PonderPalette.GREEN;
            case BLOOD -> PonderPalette.RED;
        };
    }

    static ItemStack chalkFor(BlockState sigil) {
        return new ItemStack(ItemRegistry.CHALK.get(sigil.getValue(SigilBlock.COLOR)).get());
    }

    static List<ItemStack> reagents(RitualDefinitions.Def def) {
        return def.reagents().stream().map(r -> r.stack().copy()).toList();
    }
}
