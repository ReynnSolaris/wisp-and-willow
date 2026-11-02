package com.reynn.wispwillow.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reynn.wispwillow.WispWillow;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Drawing primitives for the goggle overlays and flux effects.
 *
 * <p>Billboarding is done from the camera's own basis vectors rather than by pushing
 * {@code cam.rotation()} onto the pose — a basis derived from a look vector flips when the
 * camera crosses near-vertical, which made bars and links snap and flash while turning.</p>
 *
 * <p>Everything emits the stock {@code NEW_ENTITY} vertex format at full-bright, because
 * that is the one path a shader pack reliably draws.</p>
 */
public final class OverlayShapes {
    private OverlayShapes() {}

    public static final ResourceLocation BEAM_TEX =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "textures/effect/fluxia_beam.png");
    /** Like the beam, but with pulses along its length, so scrolling it shows the flow. */
    /** A soft round dot, for the sprites: the beam texture is a stripe and drew them as slabs. */
    public static final ResourceLocation GLOW_TEX =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "textures/effect/fluxia_glow.png");
    public static final ResourceLocation CURRENT_TEX =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "textures/effect/fluxia_current.png");

    private static final int FULL_LIGHT = LightTexture.FULL_BRIGHT;
    private static final int NO_OVERLAY = OverlayTexture.NO_OVERLAY;

    /** Middle of the white texture, so flat fills never catch a filtering edge. */
    private static final float WHITE_U = 0.5f;
    private static final float WHITE_V = 0.5f;

    // ==================================================================
    // Camera basis
    // ==================================================================

    public static Vec3 camRight(Camera cam) {
        return new Vec3(cam.getLeftVector()).scale(-1.0);
    }

    public static Vec3 camUp(Camera cam) {
        return new Vec3(cam.getUpVector());
    }

    /** Unit vector from a point back toward the viewer. */
    public static Vec3 towardCamera(Camera cam, Vec3 from) {
        Vec3 d = cam.getPosition().subtract(from);
        return d.lengthSqr() < 1.0e-8 ? new Vec3(0, 0, 1) : d.normalize();
    }

    /**
     * Maps a point in a camera-facing 2D frame to world space.
     *
     * <p>{@code lift} nudges the point toward the viewer. These render types write depth,
     * so stacked pieces of a gauge would otherwise sit at identical depth and fight each
     * other frame to frame — that was the original flicker.</p>
     */
    @FunctionalInterface
    public interface Plane {
        Vec3 at(double x, double y, double lift);

        default Vec3 at(double x, double y) {
            return at(x, y, 0);
        }
    }

    @FunctionalInterface
    public interface Frame {
        void draw(Plane plane);
    }

    /** Builds a camera-facing drawing frame anchored at {@code centre}. */
    public static void billboard(Camera cam, Vec3 centre, Frame body) {
        Vec3 right = camRight(cam);
        Vec3 up = camUp(cam);
        Vec3 toward = towardCamera(cam, centre);
        body.draw((x, y, lift) -> centre
                .add(right.scale(x))
                .add(up.scale(y))
                .add(toward.scale(lift)));
    }

    // ==================================================================
    // Flat quads (gauges)
    // ==================================================================

    /** An axis-aligned rectangle inside a {@link #billboard} frame. */
    public static void rect(PoseStack ps, MultiBufferSource buf, Camera cam, Plane plane,
                            float x0, float y0, float x1, float y1, float lift, int argb) {
        rectGradient(ps, buf, cam, plane, x0, y0, x1, y1, lift, argb, argb);
    }

    /** A rectangle whose colour blends vertically, giving the bar a little depth. */
    public static void rectGradient(PoseStack ps, MultiBufferSource buf, Camera cam, Plane plane,
                                    float x0, float y0, float x1, float y1, float lift,
                                    int argbBottom, int argbTop) {
        VertexConsumer vc = buf.getBuffer(WWRenderTypes.flat());
        Matrix4f m = ps.last().pose();

        Vec3 p00 = plane.at(x0, y0, lift);
        Vec3 n = towardCamera(cam, p00);
        float nx = (float) n.x, ny = (float) n.y, nz = (float) n.z;

        put(vc, m, p00, WHITE_U, WHITE_V, argbBottom, nx, ny, nz);
        put(vc, m, plane.at(x0, y1, lift), WHITE_U, WHITE_V, argbTop, nx, ny, nz);
        put(vc, m, plane.at(x1, y1, lift), WHITE_U, WHITE_V, argbTop, nx, ny, nz);
        put(vc, m, plane.at(x1, y0, lift), WHITE_U, WHITE_V, argbBottom, nx, ny, nz);
    }

    // ==================================================================
    // Text
    // ==================================================================

    /**
     * Small centred text facing the camera. The font draws +Y down and the camera
     * quaternion mirrors X, so both axes are negated to get upright, readable text.
     */
    public static void label(PoseStack ps, MultiBufferSource buf, Camera cam, Font font,
                             String text, Vec3 centre, float scale, int argb) {
        ps.pushPose();
        ps.translate(centre.x, centre.y, centre.z);
        ps.mulPose(cam.rotation());
        ps.scale(-scale, -scale, scale);
        float w = font.width(text) / 2.0f;
        font.drawInBatch(text, -w, 0f, argb, false, ps.last().pose(), buf,
                Font.DisplayMode.NORMAL, 0, FULL_LIGHT);
        ps.popPose();
    }

    // ==================================================================
    // Ribbons and sprites
    // ==================================================================

    /**
     * A flat ribbon from A to B that always turns its face toward the camera.
     *
     * @param scrollV offset on the texture's V axis — animate it to make the ribbon flow
     * @param tile    texture repeats per block of length
     */
    public static void ribbon(PoseStack ps, MultiBufferSource buf, Camera cam,
                              Vec3 from, Vec3 to, float width,
                              int argbFrom, int argbTo,
                              float scrollV, float tile, RenderType type) {

        Vec3 axis = to.subtract(from);
        double len = axis.length();
        if (len < 1.0e-4 || width <= 0f) return;
        axis = axis.scale(1.0 / len);

        Vec3 mid = from.add(to).scale(0.5);
        Vec3 view = cam.getPosition().subtract(mid);
        Vec3 side = axis.cross(view);
        if (side.lengthSqr() < 1.0e-8) side = axis.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0e-8) side = new Vec3(1, 0, 0);
        side = side.normalize().scale(width * 0.5);

        Vec3 a0 = from.add(side), a1 = from.subtract(side);
        Vec3 b0 = to.add(side), b1 = to.subtract(side);

        float v0 = scrollV;
        float v1 = scrollV + (float) len * tile;

        VertexConsumer vc = buf.getBuffer(type);
        Matrix4f m = ps.last().pose();

        // Normal toward the viewer: these types apply diffuse lighting, and a fixed
        // up-normal shaded the ribbons almost to nothing.
        Vec3 n = view.lengthSqr() < 1.0e-8 ? new Vec3(0, 0, 1) : view.normalize();
        float nx = (float) n.x, ny = (float) n.y, nz = (float) n.z;

        put(vc, m, a0, 0f, v0, argbFrom, nx, ny, nz);
        put(vc, m, a1, 1f, v0, argbFrom, nx, ny, nz);
        put(vc, m, b1, 1f, v1, argbTo, nx, ny, nz);
        put(vc, m, b0, 0f, v1, argbTo, nx, ny, nz);
    }

    /**
     * A thin square rod from A to B: four faces around the axis, so it has the same width
     * from every direction and nothing turns with the camera.
     *
     * <p>This is what the currents are made of. A single billboarded ribbon is the cheapest
     * beam, but it is a flat strip that spins to face you: walk past one and it visibly
     * pivots and flashes as its side vector swings through the view direction. Four fixed
     * faces cost three more quads and hold still. The render types used here cull nothing,
     * so the winding of the far faces does not matter, and every face takes the normal
     * toward the viewer so none of them is shaded darker than the rest.</p>
     *
     * @param scrollV offset on the texture's V axis; animate it to make the light run
     * @param tile    texture repeats per block of length
     */
    public static void prism(PoseStack ps, MultiBufferSource buf, Camera cam,
                             Vec3 from, Vec3 to, float width,
                             int argbFrom, int argbTo,
                             float scrollV, float tile, RenderType type) {
        Vec3 axis = to.subtract(from);
        double len = axis.length();
        if (len < 1.0e-4 || width <= 0f) return;
        axis = axis.scale(1.0 / len);

        Vec3 ref = Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 bx = axis.cross(ref).normalize();
        Vec3 by = axis.cross(bx).normalize();
        double h = width * 0.5;

        // The four corners of the rod's cross-section, 45 degrees off the basis so a rod
        // seen from along its basis still shows a face rather than an edge.
        Vec3[] off = new Vec3[4];
        for (int k = 0; k < 4; k++) {
            double ang = Math.PI * 0.25 + k * Math.PI * 0.5;
            off[k] = bx.scale(Math.cos(ang) * h).add(by.scale(Math.sin(ang) * h));
        }

        float v0 = scrollV;
        float v1 = scrollV + (float) len * tile;

        Vec3 n = towardCamera(cam, from.add(to).scale(0.5));
        float nx = (float) n.x, ny = (float) n.y, nz = (float) n.z;

        VertexConsumer vc = buf.getBuffer(type);
        Matrix4f m = ps.last().pose();
        for (int k = 0; k < 4; k++) {
            Vec3 o0 = off[k], o1 = off[(k + 1) % 4];
            put(vc, m, from.add(o0), 0f, v0, argbFrom, nx, ny, nz);
            put(vc, m, from.add(o1), 1f, v0, argbFrom, nx, ny, nz);
            put(vc, m, to.add(o1), 1f, v1, argbTo, nx, ny, nz);
            put(vc, m, to.add(o0), 0f, v1, argbTo, nx, ny, nz);
        }
    }

    /**
     * A ribbon that reads as light: a wide dim halo with a narrow bright core down the middle.
     *
     * <p>Built out of two passes rather than an additive render type. The only stock additive
     * type is {@code RenderType.eyes}, and it leaves back-face culling on — a ribbon's winding
     * depends on which way the link runs, so half of them disappeared — while Photon dropped it
     * at this render stage entirely. Two alpha-blended passes cost one extra quad, cannot be
     * culled, and stay depth-tested, so a link behind a wall is still behind the wall.</p>
     *
     * <p>The core is nudged toward the viewer because these render types write depth; drawn
     * co-planar with the halo it would z-fight instead of sitting on top of it.</p>
     *
     * @param halo  colour of the outer bloom, alpha included
     * @param core  colour of the bright centre line, alpha included
     */
    public static void glowRibbon(PoseStack ps, MultiBufferSource buf, Camera cam,
                                  Vec3 from, Vec3 to, float width,
                                  int halo, int core,
                                  float scrollV, float tile, RenderType type) {
        ribbon(ps, buf, cam, from, to, width * HALO_SPREAD, halo, halo, scrollV, tile, type);

        Vec3 lift = towardCamera(cam, from.lerp(to, 0.5)).scale(CORE_LIFT);
        ribbon(ps, buf, cam, from.add(lift), to.add(lift), width * CORE_WIDTH,
                core, core, scrollV, tile, type);
    }

    /** The same trick for a point: a soft bloom with a hot centre. */
    public static void glowSprite(PoseStack ps, MultiBufferSource buf, Camera cam,
                                  Vec3 centre, float size, int halo, int core, RenderType type) {
        sprite(ps, buf, cam, centre, size * HALO_SPREAD, halo, type);
        sprite(ps, buf, cam, centre.add(towardCamera(cam, centre).scale(CORE_LIFT)),
                size * CORE_WIDTH, core, type);
    }

    /** How much wider the halo is than the nominal width. */
    private static final float HALO_SPREAD = 2.6f;
    /**
     * How wide the bright core is relative to the caller's width. Near 1, so the line keeps
     * the weight it had before the halo was added rather than thinning to a shimmer at range.
     */
    private static final float CORE_WIDTH = 0.9f;
    /** How far the core sits toward the viewer, to win the depth test against its own halo. */
    private static final double CORE_LIFT = 0.012;

    public static void sprite(PoseStack ps, MultiBufferSource buf, Camera cam,
                              Vec3 centre, float size, int argb, RenderType type) {
        if (size <= 0f) return;

        Vec3 right = camRight(cam).scale(size * 0.5);
        Vec3 up = camUp(cam).scale(size * 0.5);
        Vec3 n = towardCamera(cam, centre);
        float nx = (float) n.x, ny = (float) n.y, nz = (float) n.z;

        VertexConsumer vc = buf.getBuffer(type);
        Matrix4f m = ps.last().pose();

        put(vc, m, centre.subtract(right).subtract(up), 0f, 0f, argb, nx, ny, nz);
        put(vc, m, centre.add(right).subtract(up), 1f, 0f, argb, nx, ny, nz);
        put(vc, m, centre.add(right).add(up), 1f, 1f, argb, nx, ny, nz);
        put(vc, m, centre.subtract(right).add(up), 0f, 1f, argb, nx, ny, nz);
    }

    // ==================================================================

    private static void put(VertexConsumer vc, Matrix4f m, Vec3 p, float u, float v, int argb,
                            float nx, float ny, float nz) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(NO_OVERLAY)
                .setLight(FULL_LIGHT)
                .setNormal(nx, ny, nz);
    }

    // ==================================================================
    // Colour helpers
    // ==================================================================

    public static int withAlpha(int argb, float mul) {
        int a = Mth.clamp(Math.round(((argb >>> 24) & 0xFF) * mul), 0, 255);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static int lerpColor(int from, int to, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int a = (int) Mth.lerp(t, (from >>> 24) & 0xFF, (to >>> 24) & 0xFF);
        int r = (int) Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, from & 0xFF, to & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
