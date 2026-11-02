package com.reynn.wispwillow.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reynn.wispwillow.fluxia.net.FluxArcS2C;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side store and renderer for Fluxia transfers.
 *
 * <p>Each {@link FluxArcS2C} says "power just moved from here to there". A pool reports
 * that once a second for every node it feeds, so rather than drawing each report as its own
 * dart of light — which made a steady feed look like something being thrown, over and over
 * — the reports are folded into one <em>current</em> per pair of nodes. A current lights
 * up when its first transfer arrives, stays lit while transfers keep coming, and fades out
 * shortly after they stop. What is drawn is a steady beam: a thin square rod of light that
 * looks the same from every angle (a flat ribbon turned with the camera and flashed as
 * you walked past it), a wide soft halo round it, and a few motes running down it at a
 * constant speed so the direction of flow is always legible.</p>
 *
 * <p>Nothing here touches client-only types until {@link #extract} is called, so the
 * packet handler is safe to reference from common code.</p>
 */
public final class FluxArcRenderer {
    private FluxArcRenderer() {}

    /** Ticks a current stays lit after its last transfer. Pools push every 20. */
    private static final int HOLD = 32;
    /** Ticks a new current takes to reach full brightness. */
    private static final int FADE_IN = 6;
    /** Ticks a starved current takes to go out. */
    private static final int FADE_OUT = 14;
    /** Beyond this many live currents we drop the oldest, so a huge network can't flood us. */
    private static final int MAX_FLOWS = 192;
    /** Blocks per tick the motes travel. */
    private static final float MOTE_SPEED = 0.16f;

    private record Key(long from, long to) {}

    private static final class Flow {
        final BlockPos from, to;
        final int argb;
        final long born;
        long lastFed;
        /** Smoothed transfer size, which sets the beam's width. */
        float amount;
        /** Where the beam starts and ends, resolved from the blocks once a level is to hand. */
        Vec3 a, b;

        Flow(BlockPos from, BlockPos to, int argb, long now, int amount) {
            this.from = from;
            this.to = to;
            this.argb = argb;
            this.born = now;
            this.lastFed = now;
            this.amount = amount;
        }
    }

    private static final Map<Key, Flow> FLOWS = new LinkedHashMap<>();

    /** Advanced once per client tick so currents age without needing the level. */
    private static long clientTick;

    public static void tick() {
        clientTick++;
        synchronized (FLOWS) {
            Iterator<Flow> it = FLOWS.values().iterator();
            while (it.hasNext()) {
                if (clientTick - it.next().lastFed > HOLD + FADE_OUT) it.remove();
            }
        }
    }

    public static void clear() {
        synchronized (FLOWS) { FLOWS.clear(); }
    }

    /** Packet entry point. Deliberately free of any client-only types. */
    public static void accept(FluxArcS2C msg) {
        Key key = new Key(msg.from().asLong(), msg.to().asLong());
        synchronized (FLOWS) {
            Flow flow = FLOWS.get(key);
            if (flow == null) {
                if (FLOWS.size() >= MAX_FLOWS) {
                    Iterator<Flow> it = FLOWS.values().iterator();
                    it.next();
                    it.remove();
                }
                FLOWS.put(key, new Flow(msg.from(), msg.to(), msg.argb(), clientTick, msg.amount()));
            } else {
                flow.lastFed = clientTick;
                flow.amount = flow.amount * 0.7f + msg.amount() * 0.3f;
            }
        }
    }

    public static boolean isEmpty() {
        synchronized (FLOWS) { return FLOWS.isEmpty(); }
    }

    // ------------------------------------------------------------------
    // Extract: the level is read here and nowhere else
    // ------------------------------------------------------------------

    /** One current as the renderer draws it; no live references, so it can be drawn later. */
    public record FlowView(Vec3 from, Vec3 to, int argb, float width, float alpha, float age) {}

    /** Snapshots every live current, resolving beam ends against the blocks they join. */
    public static List<FlowView> extract(Level level, float partial) {
        List<Flow> snapshot;
        synchronized (FLOWS) {
            if (FLOWS.isEmpty()) return List.of();
            snapshot = new ArrayList<>(FLOWS.values());
        }
        double now = clientTick + partial;
        List<FlowView> out = new ArrayList<>(snapshot.size());
        for (Flow flow : snapshot) {
            if (flow.a == null) {
                flow.a = anchor(level, flow.from);
                flow.b = anchor(level, flow.to);
            }
            float age = (float) (now - flow.born);
            float sinceFed = (float) (now - flow.lastFed);
            float in = Mth.clamp(age / FADE_IN, 0f, 1f);
            float outFade = sinceFed <= HOLD ? 1f : 1f - Mth.clamp((sinceFed - HOLD) / FADE_OUT, 0f, 1f);
            float alpha = in * outFade;
            if (alpha <= 0.01f) continue;
            out.add(new FlowView(flow.a, flow.b, flow.argb, beamWidth(flow.amount), alpha, age));
        }
        return out;
    }

    /**
     * Where a beam meets a block: its centre, at a height inside the block's own shape, so
     * a pool is tapped at its water line and an altar at its bowl rather than both at one
     * fixed height that suited neither.
     */
    private static Vec3 anchor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        double top = 1.0;
        try {
            VoxelShape shape = state.getShape(level, pos);
            if (!shape.isEmpty()) {
                AABB bounds = shape.bounds();
                top = bounds.maxY;
            }
        } catch (RuntimeException ignored) {
            // A shape that wants more context than we have; the block's full height is fine.
        }
        double y = Mth.clamp(top * 0.75, 0.3, 0.9);
        return new Vec3(pos.getX() + 0.5, pos.getY() + y, pos.getZ() + 0.5);
    }

    /**
     * Bigger transfers get fatter beams, on a log scale so 3000 is not 100x wider than 30.
     */
    private static float beamWidth(float amount) {
        float f = (float) Math.log10(Math.max(1f, amount) + 1.0);
        return Mth.clamp(0.04f + f * 0.035f, 0.04f, 0.17f);
    }

    // ------------------------------------------------------------------
    // Draw
    // ------------------------------------------------------------------

    /**
     * @param ps   pose with (0,0,0) at the world origin, camera-relative
     * @param time game time plus partial tick, for the animation
     */
    public static void submit(List<FlowView> flows, PoseStack ps, MultiBufferSource out, Camera cam, float time) {
        if (flows.isEmpty()) return;
        var beam = WWRenderTypes.surface(OverlayShapes.CURRENT_TEX);
        var soft = WWRenderTypes.surface(OverlayShapes.BEAM_TEX);
        var glow = WWRenderTypes.surface(OverlayShapes.GLOW_TEX);

        for (FlowView flow : flows) {
            Vec3 from = flow.from(), to = flow.to();
            double len = from.distanceTo(to);
            if (len < 0.05) continue;
            float a = flow.alpha();
            float w = flow.width();

            int core = withAlpha(0xFFFFFFFF, a * 0.95f);
            int body = withAlpha(flow.argb(), a * 0.85f);
            int halo = withAlpha(flow.argb(), a * 0.22f);

            // The halo: a wide, dim ribbon turned to the camera, which is the only billboarded
            // piece and reads as glow rather than as a surface.
            OverlayShapes.ribbon(ps, out, cam, from, to, w * 3.2f, halo, halo, 0f, 0.5f, soft);

            // The current itself: a square rod, textured so the light visibly runs toward
            // the target, with a thinner white rod inside it for the hot centre.
            float scroll = -time * 0.045f;
            OverlayShapes.prism(ps, out, cam, from, to, w, body, body, scroll, 0.6f, beam);
            OverlayShapes.prism(ps, out, cam, from, to, w * 0.42f, core, core, scroll * 1.5f, 0.6f, beam);

            // Motes: a few bright points running down the beam at a steady pace, with a short
            // tail each, so the direction of flow is obvious from any distance.
            int motes = Mth.clamp((int) (len / 2.5) + 1, 1, 6);
            float cycle = (float) (len / MOTE_SPEED);
            for (int i = 0; i < motes; i++) {
                float phase = ((time / cycle) + i / (float) motes) % 1.0f;
                if (phase < 0) phase += 1f;
                float bright = Mth.sin(phase * Mth.PI);            // fades in at the source, out at the target
                Vec3 head = from.lerp(to, phase);
                Vec3 tail = from.lerp(to, Math.max(0f, phase - (float) (0.45 / len)));
                int hot = withAlpha(0xFFFFFFFF, a * bright);
                int warm = withAlpha(flow.argb(), a * bright * 0.6f);
                int cold = withAlpha(flow.argb(), 0f);
                OverlayShapes.ribbon(ps, out, cam, tail, head, w * 1.4f, cold, warm, 0f, 0.5f, soft);
                OverlayShapes.glowSprite(ps, out, cam, head, w * 1.1f * (0.6f + 0.4f * bright), warm, hot, glow);
            }

            // Caps: a glow where the current leaves its source and a brighter, pulsing one
            // where it lands, so arrival reads even when the beam is seen end-on.
            float pulse = 0.85f + 0.15f * Mth.sin(time * 0.35f);
            OverlayShapes.glowSprite(ps, out, cam, from, w * 1.3f, halo, body, glow);
            OverlayShapes.glowSprite(ps, out, cam, to, w * 1.8f * pulse, halo, core, glow);
        }
    }

    private static int withAlpha(int argb, float mul) {
        int a = (int) (((argb >>> 24) & 0xFF) * Mth.clamp(mul, 0f, 1f));
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
