package com.reynn.wispwillow.compat.ponder;

import com.reynn.wispwillow.blocks.fluxia.GatewayCoreBlock;
import com.reynn.wispwillow.blocks.fluxia.GatewayFrame;
import com.reynn.wispwillow.blocks.fluxia.GatewayPortalBlock;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Building the Soul Gateway, opening it, and paying a pact through it. */
final class GatewayScenes {
    private GatewayScenes() {}

    static void build(SceneBuilder scene, SceneBuildingUtil util) {
        BlockPos core = PonderLayouts.gatewayCore();
        Direction facing = PonderLayouts.GATEWAY_FACING;
        List<BlockPos> frame = PonderLayouts.gatewayFrame();
        List<BlockPos> hole = PonderLayouts.gatewayInterior();
        int lintelY = frame.stream().mapToInt(BlockPos::getY).max().orElse(core.getY());
        List<BlockPos> sill = frame.stream().filter(p -> p.getY() == core.getY()).toList();
        List<BlockPos> lintel = frame.stream().filter(p -> p.getY() == lintelY).toList();
        List<BlockPos> jambs = frame.stream().filter(p -> p.getY() != core.getY() && p.getY() != lintelY).toList();

        scene.title(PonderLayouts.GATEWAY, PonderText.header(PonderLayouts.GATEWAY));
        scene.configureBasePlate(0, 0, PonderLayouts.GATEWAY_SIZE);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(core), Direction.DOWN);
        scene.idle(15);
        SceneKit.say(scene, "gateway.core", util.vector().topOf(core));

        scene.world().showSection(SceneKit.select(util, sill), Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(SceneKit.select(util, jambs), Direction.DOWN);
        scene.idle(8);
        // The hole goes up with the lintel while it is still air, so the portal can appear in it.
        scene.world().showSection(SceneKit.select(util, lintel).add(SceneKit.select(util, hole)), Direction.DOWN);
        scene.idle(15);
        SceneKit.say(scene, "gateway.frame", util.vector().centerOf(lintel.get(lintel.size() / 2)));
        SceneKit.say(scene, "gateway.fluxia", util.vector().topOf(core));

        SceneKit.click(scene, util.vector().topOf(core), null, false);
        scene.idle(10);
        BlockState portal = BlockRegistry.GATEWAY_PORTAL.get().defaultBlockState()
                .setValue(GatewayPortalBlock.AXIS, facing.getClockWise().getAxis());
        scene.world().setBlocks(SceneKit.select(util, hole), portal, false);
        scene.world().modifyBlock(core, s -> s.setValue(GatewayCoreBlock.OPEN, true), false);
        Vec3 middle = util.vector().centerOf(hole.get(hole.size() / 2));
        scene.effects().emitParticles(middle,
                scene.effects().simpleParticleEmitter(ParticleTypes.SOUL_FIRE_FLAME, Vec3.ZERO), 4, 15);
        SceneKit.say(scene, "gateway.open", util.vector().topOf(core));
        SceneKit.say(scene, "gateway.collapse", util.vector().centerOf(jambs.get(0)));

        // A pact thrown in from the front and taken by the light. The payout is the soul burst the
        // real gateway makes: a familiar spawned here never shows up in Ponder's level.
        BlockPos exit = GatewayFrame.exit(core, facing);
        Vec3 throwFrom = util.vector().centerOf(exit.relative(facing));
        Vec3 throwAt = Vec3.atLowerCornerOf(facing.getOpposite().getNormal()).scale(0.12).add(0, 0.2, 0);
        ElementLink<EntityElement> pact = scene.world().createItemEntity(throwFrom, throwAt,
                new ItemStack(ItemRegistry.SEALED_SOUL_PACT.get()));
        scene.idle(20);
        scene.world().modifyEntity(pact, Entity::discard);
        scene.effects().emitParticles(util.vector().centerOf(exit),
                scene.effects().simpleParticleEmitter(ParticleTypes.SOUL, new Vec3(0, 0.05, 0)), 5, 10);
        scene.idle(15);
        SceneKit.say(scene, "gateway.cast", util.vector().centerOf(exit));
        SceneKit.say(scene, "gateway.pacts", null);
        scene.markAsFinished();
    }
}
