package com.reynn.wispwillow.compat.ponder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** The handful of moves every Wisp &amp; Willow scene makes, so the scenes read as scripts. */
final class SceneKit {
    private SceneKit() {}

    /** Plates wider than this are pulled back so the whole circle stays in frame. */
    private static final int FRAMED = 7;

    /** Summoned creatures turn toward Ponder's default camera. */
    private static final float FACE_VIEWER = -45f;

    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};

    /** One line of text, held long enough to read, and the scene waits for it. */
    static void say(SceneBuilder scene, String key, @Nullable Vec3 at, Object... args) {
        // Ponder holds a line for exactly as long as it is told to, so size the hold to what is
        // actually on screen rather than guessing one number for every sentence.
        int ticks = Mth.clamp(40 + I18n.get(PonderText.sharedLangKey(key), args).length(), 70, 170);
        TextElementBuilder text = scene.overlay().showText(ticks)
                .sharedText(PonderText.id(key), args)
                .attachKeyFrame();
        if (at != null) text.pointAt(at).placeNearTarget();
        scene.idle(ticks + 10);
    }

    static Selection select(SceneBuildingUtil util, Collection<BlockPos> positions) {
        Selection out = null;
        for (BlockPos p : positions) {
            Selection one = util.select().position(p);
            out = out == null ? one : out.add(one);
        }
        if (out == null) throw new IllegalArgumentException("Nothing to select");
        return out;
    }

    static void fit(SceneBuilder scene, int size) {
        if (size > FRAMED) scene.scaleSceneView((float) FRAMED / size);
    }

    /** Just above a flat block such as a glyph, where a pointer or a click should land. */
    static Vec3 onTop(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
    }

    static void click(SceneBuilder scene, Vec3 at, @Nullable ItemStack held, boolean sneaking) {
        InputElementBuilder input = scene.overlay().showControls(at, Pointing.DOWN, 20).rightClick();
        if (held != null) input.withItem(held);
        if (sneaking) input.whileSneaking();
        scene.idle(6);
    }

    /** The i-th of n points evenly around {@code centre}, starting on a diagonal. */
    static Vec3 around(SceneBuildingUtil util, BlockPos centre, int i, int n, double radius) {
        // Diagonals first: the cardinal cells are the ones circles most often chalk.
        double a = Math.PI / 4 + Math.PI * 2 * i / n;
        return util.vector().centerOf(centre).add(Math.cos(a) * radius, 0.5, Math.sin(a) * radius);
    }

    /** Drops stacks in a ring around {@code centre}, returning them so they can be spent later. */
    static List<ElementLink<EntityElement>> scatter(SceneBuilder scene, SceneBuildingUtil util,
                                                    BlockPos centre, List<ItemStack> stacks, double radius) {
        List<ElementLink<EntityElement>> out = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i++) {
            out.add(scene.world().createItemEntity(around(util, centre, i, stacks.size(), radius),
                    Vec3.ZERO, stacks.get(i).copy()));
            scene.idle(3);
        }
        return out;
    }

    /** The click on the altar, the reagents taken, and the flash that says it worked. */
    static void cast(SceneBuilder scene, SceneBuildingUtil util, BlockPos altar,
                     List<ElementLink<EntityElement>> spent) {
        click(scene, util.vector().topOf(altar), null, false);
        scene.idle(10);
        for (ElementLink<EntityElement> link : spent) scene.world().modifyEntity(link, Entity::discard);
        sparkle(scene, util.vector().topOf(altar), 12);
        scene.effects().indicateSuccess(altar);
        scene.idle(15);
    }

    static void sparkle(SceneBuilder scene, Vec3 at, int cycles) {
        scene.effects().emitParticles(at,
                scene.effects().simpleParticleEmitter(ParticleTypes.WITCH, new Vec3(0, 0.05, 0)), 3, cycles);
    }

    /** A result landing on top of the altar. */
    static void drop(SceneBuilder scene, SceneBuildingUtil util, BlockPos altar, ItemStack stack) {
        scene.world().createItemEntity(util.vector().topOf(altar).add(0, 0.4, 0),
                new Vec3(0, 0.15, 0), stack.copy());
    }

    /** A creature standing still where it was called, looking at the viewer. */
    static void spawn(SceneBuilder scene, EntityType<?> type, Vec3 at) {
        scene.world().createEntity(level -> {
            Entity entity = type.create(level);
            // Never expected; a barrier is at least visibly wrong rather than a crash.
            if (entity == null) return new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.BARRIER));
            entity.setPos(at);
            entity.setYRot(FACE_VIEWER);
            if (entity instanceof LivingEntity living) {
                living.yBodyRot = living.yBodyRotO = FACE_VIEWER;
                living.yHeadRot = living.yHeadRotO = FACE_VIEWER;
            }
            if (entity instanceof Mob mob) mob.setNoAi(true);
            return entity;
        });
    }

    static String describe(ItemStack stack) {
        String name = stack.getHoverName().getString();
        return stack.getCount() > 1 ? stack.getCount() + "× " + name : name;
    }

    static String describe(List<ItemStack> stacks) {
        return stacks.stream().map(SceneKit::describe).collect(Collectors.joining(", "));
    }

    static String roman(int n) {
        return n >= 1 && n <= ROMAN.length ? ROMAN[n - 1] : String.valueOf(n);
    }
}
