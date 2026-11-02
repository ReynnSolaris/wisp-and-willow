package com.reynn.wispwillow.compat.ponder;

import com.reynn.wispwillow.blocks.alchemy.AlchemicalCauldronBlock;
import com.reynn.wispwillow.registry.BlockRegistry;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Building the Alchemical Cauldron, from the ring in the floor to the first light. */
final class CauldronScenes {
    private CauldronScenes() {}

    static void build(SceneBuilder scene, SceneBuildingUtil util) {
        BlockPos core = PonderLayouts.cauldronCore();
        List<BlockPos> circle = new ArrayList<>();
        List<BlockPos> pillars = new ArrayList<>();
        List<BlockPos> caps = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> e : PonderLayouts.cauldronRing().entrySet()) {
            BlockState s = e.getValue();
            if (s.is(BlockRegistry.FLUXITE.get())) circle.add(e.getKey());
            else if (s.is(BlockRegistry.ASTRALITE_BLOCK.get())) caps.add(e.getKey());
            else pillars.add(e.getKey());
        }

        scene.title(PonderLayouts.CAULDRON, PonderText.header(PonderLayouts.CAULDRON));
        scene.configureBasePlate(0, 0, PonderLayouts.CAULDRON_SIZE);
        scene.showBasePlate();
        scene.idle(10);
        SceneKit.say(scene, "cauldron.intro", null);

        scene.overlay().showOutline(PonderPalette.GREEN, "circle", SceneKit.select(util, circle), 100);
        SceneKit.say(scene, "cauldron.circle", util.vector().topOf(circle.get(0)));

        scene.world().showSection(util.select().position(core), Direction.DOWN);
        scene.idle(15);
        SceneKit.say(scene, "cauldron.core", util.vector().topOf(core));

        scene.world().showSection(SceneKit.select(util, pillars), Direction.DOWN);
        scene.idle(15);
        SceneKit.say(scene, "cauldron.pillars", util.vector().centerOf(pillars.get(0)));

        scene.world().showSection(SceneKit.select(util, caps), Direction.DOWN);
        scene.idle(15);
        SceneKit.say(scene, "cauldron.caps", util.vector().topOf(caps.get(0)));

        Selection mouth = util.select().position(core.above()).add(util.select().position(core.above(2)));
        scene.overlay().showOutline(PonderPalette.RED, "mouth", mouth, 100);
        SceneKit.say(scene, "cauldron.open", util.vector().centerOf(core.above()));

        scene.world().modifyBlock(core, s -> s.setValue(AlchemicalCauldronBlock.FORMED, true), false);
        scene.effects().indicateSuccess(core);
        SceneKit.say(scene, "cauldron.formed", util.vector().topOf(core));

        SceneKit.click(scene, util.vector().topOf(core), null, false);
        scene.idle(10);
        scene.world().modifyBlock(core, s -> s.setValue(AlchemicalCauldronBlock.LIT, true), false);
        SceneKit.sparkle(scene, util.vector().topOf(core), 20);
        SceneKit.say(scene, "cauldron.light", util.vector().topOf(core));
        SceneKit.say(scene, "cauldron.use", util.vector().topOf(core));
        SceneKit.say(scene, "cauldron.feed", util.vector().blockSurface(core, Direction.WEST));
        scene.markAsFinished();
    }
}
