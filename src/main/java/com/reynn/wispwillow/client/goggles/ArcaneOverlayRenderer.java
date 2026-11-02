package com.reynn.wispwillow.client.goggles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.client.renderers.FluxArcRenderer;
import com.reynn.wispwillow.client.renderers.OverlayShapes;
import com.reynn.wispwillow.client.renderers.WWRenderTypes;
import com.reynn.wispwillow.entity.OverlayHostEntity;
import com.reynn.wispwillow.fluxia.FluxLinks;
import com.reynn.wispwillow.fluxia.IFluxiaNode;
import com.reynn.wispwillow.fluxia.IFluxiaStorage;
import com.reynn.wispwillow.registry.EntityRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything that lives in the world rather than on the HUD.
 *
 * <ul>
 *   <li>Flux currents — always visible, the mod's main feedback that a network is doing
 *       something.</li>
 *   <li>Node gauges and the flowing links between them — only for a wearer of the
 *       {@link com.reynn.wispwillow.items.ArcaneGogglesItem Arcane Goggles}.</li>
 * </ul>
 *
 * <p>None of it is drawn from a level-render stage any more. This class keeps one
 * {@link OverlayHostEntity} alive on the client, parked on the player, and the entity's
 * renderer ({@link OverlayHostRenderer}) is what draws. Drawing at a render stage looked
 * fine in vanilla and vanished under a shader pack, which composites those stages its own
 * way; an entity renderer is drawn by every pack, which is why the portals — block entity
 * renderers using the same stock render types — never had the problem.</p>
 *
 * <p>The gather half ({@link #extract}) reads the level and returns plain records; the draw
 * half ({@link #submit}) touches nothing live.</p>
 */
@EventBusSubscriber(modid = WispWillow.MODID, value = Dist.CLIENT)
public final class ArcaneOverlayRenderer {
    private ArcaneOverlayRenderer() {}

    /** Links longer than this are a mistake not worth drawing across the whole world. */
    private static final double MAX_LINK_DRAW = 64.0;

    /** Gauges start fading out this far away and are gone by the overlay range. */
    private static final double FADE_START = 16.0;

    /** Violet bloom around the link. */
    private static final int LINK_BODY = 0xFFB98BFF;
    /** Near-white centre line, bright enough for a shader pack's bloom to catch it. */
    private static final int LINK_CORE = 0xFFEBDCFF;
    private static final int LINK_PULSE = 0xFFFFFAFF;

    private static OverlayHostEntity host;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        FluxArcRenderer.tick();
        keepHost(Minecraft.getInstance());
    }

    /** Arcs are positions in a specific world; dropping the world drops them too. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FluxArcRenderer.clear();
        host = null;
    }

    /**
     * Keeps the carrier entity in the current level and on the player.
     *
     * <p>It is re-made whenever the level changes (a dimension hop drops every client entity
     * with the old level) and moved every tick so it always sits in a loaded section. Its
     * position does not matter to what it draws; it only has to exist and be in view range,
     * and its renderer opts out of culling.</p>
     */
    private static void keepHost(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            host = null;
            return;
        }
        if (host == null || host.level() != mc.level || host.isRemoved()) {
            host = new OverlayHostEntity(EntityRegistry.OVERLAY_HOST.get(), mc.level);
            Vec3 at = mc.player.position();
            host.setPos(at.x, at.y, at.z);
            host.setOldPosAndRot();
            mc.level.addEntity(host);
            return;
        }
        Vec3 at = mc.player.position();
        host.setPos(at.x, at.y, at.z);
        host.setOldPosAndRot();
    }

    // ------------------------------------------------------------------
    // Extract
    // ------------------------------------------------------------------

    /**
     * One node as the overlay draws it: where it is, where its links go, how full it is.
     *
     * @param fill     0..1, or negative for a node with no reserve to gauge
     */
    public record NodeView(Vec3 origin, List<Vec3> links, float alpha, float fill, int amount, int capacity) {}

    public static List<NodeView> extract(Minecraft mc, Level level, Vec3 camPos) {
        List<BlockPos> nodes = nearbyNodes(level, camPos);
        if (nodes.isEmpty()) return List.of();

        List<NodeView> out = new ArrayList<>(nodes.size());
        for (BlockPos pos : nodes) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof IFluxiaNode node)) continue;

            double dist = Math.sqrt(camPos.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            float alpha = distanceFade(dist);
            if (alpha <= 0.02f) continue;

            Vec3 origin = new Vec3(pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5);

            List<Vec3> links = new ArrayList<>();
            for (BlockPos target : node.getLinkedNodes()) {
                // A link whose far end is no longer a node is dead, and drawing it makes a
                // broken network look wired. The server prunes these, but its update has to
                // reach us first — and a link is only dead if we can actually see that the
                // target is gone, not merely because its chunk is out of view.
                if (FluxLinks.isLoaded(level, target) && !FluxLinks.isNode(level, target)) continue;
                Vec3 to = new Vec3(target.getX() + 0.5, target.getY() + 0.6, target.getZ() + 0.5);
                if (origin.distanceTo(to) > MAX_LINK_DRAW) continue;
                links.add(to);
            }

            IFluxiaStorage storage = node.getFluxStorage();
            float fill = -1f;
            int amount = 0, capacity = 0;
            if (storage != null && storage.capacity() > 0) {
                amount = storage.amount();
                capacity = storage.capacity();
                fill = Mth.clamp((float) amount / capacity, 0f, 1f);
            }
            out.add(new NodeView(origin, links, alpha, fill, amount, capacity));
        }
        return out;
    }

    /** 1 up close, easing to 0 at the edge of goggle range. */
    private static float distanceFade(double dist) {
        if (dist <= FADE_START) return 1f;
        double t = (dist - FADE_START) / (ArcaneVision.OVERLAY_RANGE - FADE_START);
        return (float) Mth.clamp(1.0 - t, 0.0, 1.0);
    }

    /**
     * Block entities are not indexed by type, so walk the loaded chunks around the camera
     * and pick out the Fluxia nodes. The range is small and this only runs while the
     * goggles are on.
     */
    private static List<BlockPos> nearbyNodes(Level level, Vec3 camPos) {
        List<BlockPos> out = new ArrayList<>();
        int r = ArcaneVision.OVERLAY_RANGE;
        int cx = Mth.floor(camPos.x) >> 4;
        int cz = Mth.floor(camPos.z) >> 4;
        int chunkR = (r >> 4) + 1;

        double maxSqr = (double) r * r;

        for (int dx = -chunkR; dx <= chunkR; dx++) {
            for (int dz = -chunkR; dz <= chunkR; dz++) {
                var chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) continue;

                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    if (camPos.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > maxSqr) {
                        continue;
                    }
                    if (chunk.getBlockEntity(pos) instanceof IFluxiaNode) out.add(pos);
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Draw
    // ------------------------------------------------------------------

    /** @param ps pose with (0,0,0) at the world origin, camera-relative */
    public static void submit(List<NodeView> nodes, PoseStack ps, MultiBufferSource buf,
                              Camera cam, float time, Font font) {
        for (NodeView node : nodes) {
            for (Vec3 to : node.links()) drawLink(ps, buf, cam, node.origin(), to, time, node.alpha());
            if (node.fill() >= 0f) {
                drawGauge(ps, buf, cam, node.origin().add(0, 0.55, 0), node.fill(), node.alpha(), time,
                        node.amount(), node.capacity(), font);
            }
        }
    }

    /**
     * A wand link, drawn the way a current is: a thin violet rod that looks the same from
     * every side, a soft halo, and pulses running from source to target so the one-way
     * direction of the link is legible at a glance.
     */
    private static void drawLink(PoseStack ps, MultiBufferSource buf, Camera cam,
                                 Vec3 from, Vec3 to, float time, float alpha) {
        var current = WWRenderTypes.surface(OverlayShapes.CURRENT_TEX);
        var soft = WWRenderTypes.surface(OverlayShapes.BEAM_TEX);
        var glow = WWRenderTypes.surface(OverlayShapes.GLOW_TEX);

        OverlayShapes.ribbon(ps, buf, cam, from, to, 0.26f,
                OverlayShapes.withAlpha(LINK_BODY, alpha * 0.18f),
                OverlayShapes.withAlpha(LINK_BODY, alpha * 0.18f), 0f, 0.5f, soft);
        // Negative scroll makes the texture travel from -> to.
        OverlayShapes.prism(ps, buf, cam, from, to, 0.07f,
                OverlayShapes.withAlpha(LINK_CORE, alpha * 0.8f),
                OverlayShapes.withAlpha(LINK_CORE, alpha * 0.8f), -time * 0.03f, 0.6f, current);

        // Pulses spaced evenly along the link, so direction of flow is obvious.
        double len = from.distanceTo(to);
        int pulses = Mth.clamp((int) (len / 3.0), 1, 4);
        for (int i = 0; i < pulses; i++) {
            float phase = ((time * 0.018f) + i / (float) pulses) % 1.0f;
            Vec3 head = from.lerp(to, phase);
            Vec3 tail = from.lerp(to, Math.max(0f, phase - 0.12f));

            // Fade each pulse in and out so it does not pop at the ends of the link.
            float bright = Mth.sin(phase * Mth.PI);
            int hot = OverlayShapes.withAlpha(LINK_PULSE, alpha * bright * 0.95f);
            int warm = OverlayShapes.withAlpha(LINK_BODY, alpha * bright * 0.35f);
            int cold = OverlayShapes.withAlpha(LINK_BODY, 0f);

            OverlayShapes.ribbon(ps, buf, cam, tail, head, 0.13f, cold, warm, 0f, 0.5f, soft);
            OverlayShapes.glowSprite(ps, buf, cam, head, 0.11f * bright, warm, hot, glow);
        }
    }

    /**
     * The node gauge: a frame, a dark well, a gradient fill, and a bright leading edge.
     * Everything is drawn in back-to-front order with a small lift per layer, so no two
     * pieces can ever fight over which is in front — which is what made it flicker.
     */
    private static void drawGauge(PoseStack ps, MultiBufferSource buf, Camera cam,
                                  Vec3 centre, float fill, float alpha, float time,
                                  int amount, int capacity, Font font) {
        final float w = 0.62f, h = 0.13f;
        final float half = w * 0.5f, halfH = h * 0.5f;
        final float border = 0.012f;
        final float shoulder = 0.02f;

        // Each layer sits a hair closer to the camera than the one behind it. These
        // render types write depth, so co-planar layers would fight and flicker.
        final float step = 0.002f;

        OverlayShapes.billboard(cam, centre, plane -> {
            // Frame, with the corners nipped off so it reads as a capsule.
            int frame = OverlayShapes.withAlpha(0xFF2A1B3D, alpha * 0.95f);
            OverlayShapes.rect(ps, buf, cam, plane, -half - border, -halfH, half + border, halfH, 0f, frame);
            OverlayShapes.rect(ps, buf, cam, plane, -half, -halfH - border, half, halfH + border, 0f, frame);

            // Inner well.
            OverlayShapes.rect(ps, buf, cam, plane, -half, -halfH, half, halfH, step,
                    OverlayShapes.withAlpha(0xFF0B0713, alpha * 0.95f));

            if (fill > 0.001f) {
                float right = -half + w * fill;
                int top = gaugeColour(fill);
                int bottom = OverlayShapes.lerpColor(top, 0xFF000000, 0.45f);

                OverlayShapes.rectGradient(ps, buf, cam, plane, -half, -halfH, right, halfH, step * 2,
                        OverlayShapes.withAlpha(bottom, alpha),
                        OverlayShapes.withAlpha(top, alpha));

                // Bright leading edge, so a slowly filling pool reads as moving.
                if (fill < 0.999f) {
                    OverlayShapes.rect(ps, buf, cam, plane, right - Math.min(shoulder, w * fill), -halfH,
                            right, halfH, step * 3, OverlayShapes.withAlpha(0xFFFFFFFF, alpha * 0.65f));
                }
            }

            // A full node shimmers so it stands out in a bank of pools.
            if (fill > 0.995f) {
                float shimmer = 0.30f + 0.30f * Mth.sin(time * 0.15f);
                OverlayShapes.rect(ps, buf, cam, plane, -half, halfH - 0.015f, half, halfH, step * 4,
                        OverlayShapes.withAlpha(0xFFFFFFFF, alpha * shimmer));
            }
        });

        // The number itself, only once you are close enough for it to be legible.
        if (alpha > 0.75f) {
            OverlayShapes.label(ps, buf, cam, font,
                    compact(amount) + " / " + compact(capacity),
                    centre.add(OverlayShapes.camUp(cam).scale(0.12))
                            .add(OverlayShapes.towardCamera(cam, centre).scale(0.02)),
                    0.006f, OverlayShapes.withAlpha(0xFFDCCBFF, alpha));
        }
    }

    /** 12,340 becomes "12.3k" so it fits above a bar without crowding it. */
    private static String compact(int value) {
        if (value < 1000) return Integer.toString(value);
        if (value < 100_000) return String.format(java.util.Locale.ROOT, "%.1fk", value / 1000f);
        if (value < 1_000_000) return (value / 1000) + "k";
        return String.format(java.util.Locale.ROOT, "%.1fM", value / 1_000_000f);
    }

    /** Empty reads red, half reads violet, full reads cyan. */
    private static int gaugeColour(float fill) {
        if (fill < 0.5f) {
            return OverlayShapes.lerpColor(0xFFE04050, 0xFFB28BFF, fill / 0.5f);
        }
        return OverlayShapes.lerpColor(0xFFB28BFF, 0xFF7FE8FF, (fill - 0.5f) / 0.5f);
    }
}
