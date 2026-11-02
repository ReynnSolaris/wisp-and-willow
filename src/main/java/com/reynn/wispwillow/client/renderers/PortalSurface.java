package com.reynn.wispwillow.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reynn.wispwillow.WispWillow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The shared drawing behind both portal sheets: a parallax field of layers, a drift of
 * sparks over it, a slow breathing of the light, and a glowing rim around the opening.
 *
 * <p>One opening is drawn as <em>one</em> surface. The portal blocks that fill a gateway
 * are many, but only the block in the bottom corner of a connected sheet draws anything,
 * and it draws the whole rectangle in a single quad per layer. Drawing it block by block
 * was what put a grid over the gate: every block computed its own parallax from its own
 * centre and its own breathing from its own position, so the field jumped a little at
 * each seam, and the backdrop tile repeated once per block so its motif read as nine
 * small pictures rather than one opening. Now the parallax is measured from the centre of
 * the opening, the breath is one value for the whole sheet, the backdrop tile spans
 * {@value #FIELD_SPAN} blocks, and the rim burns around the outside only.</p>
 *
 * <p>Everything goes through stock {@code entityTranslucent} types at full-bright, for the
 * shader-pack reasons {@link WWRenderTypes} spells out.</p>
 */
public final class PortalSurface {
    private PortalSurface() {}

    /** Bright motes, transparent everywhere else; scrolled against the field's own motion. */
    public static final ResourceLocation SPARKLE =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "textures/effect/portal_sparkle.png");

    /** Blocks one repeat of the backdrop covers: a gateway's 3x3 hole shows the tile exactly once. */
    public static final float FIELD_SPAN = 3f;
    /** Longest side a sheet may have; the walk that finds the sheet stops here. */
    public static final int MAX_SHEET = 32;
    /** How far a portal block's render box reaches, so the corner block's sheet is never culled. */
    public static final double RENDER_REACH = 8.0;

    /** Gap between stacked layers, so each sits in front of the one behind it. */
    private static final float LAYER_STEP = 0.014f;
    /** How far the rim's glow reaches into the opening from an edge, in blocks. */
    private static final float RIM_REACH = 0.2f;
    /** The bright hairline right on the edge. */
    private static final float RIM_LINE = 0.035f;

    /**
     * @param texture   the seamless field tile
     * @param tile      repeats per {@link #FIELD_SPAN} blocks, back layer to front
     * @param parallax  how far each layer slides as the camera moves past
     * @param scroll    each layer's own drift along v per tick (negative rises)
     * @param layerArgb tint per layer; the first should be opaque, it is the backdrop
     * @param sparkArgb tint of the spark drift
     * @param rimArgb   colour of the rim glow at its brightest (alpha is the peak alpha)
     */
    public record Style(ResourceLocation texture, float[] tile, float[] parallax, float[] scroll,
                        int[] layerArgb, int sparkArgb, int rimArgb) {}

    /** The box a portal block's renderer should claim, so the corner block's sheet is never culled. */
    public static AABB renderBox(BlockPos pos) {
        return new AABB(pos).inflate(RENDER_REACH);
    }

    public static void render(Style style, Level level, BlockPos pos, Block self, boolean acrossX,
                              PoseStack ps, MultiBufferSource buf, float partialTick) {
        Direction posAlong = acrossX ? Direction.EAST : Direction.SOUTH;
        Direction negAlong = posAlong.getOpposite();

        // Walk to the bottom, then to the near end of the bottom row. For the rectangles the
        // mod builds (a gateway's 3x3 hole, the Open Way's column) that is the corner, and only
        // the corner block draws; the rest of the sheet stays silent.
        BlockPos corner = pos;
        for (int i = 0; i < MAX_SHEET && level.getBlockState(corner.below()).is(self); i++) corner = corner.below();
        for (int i = 0; i < MAX_SHEET && level.getBlockState(corner.relative(negAlong)).is(self); i++) {
            corner = corner.relative(negAlong);
        }
        if (!corner.equals(pos)) return;

        int width = 1;
        while (width < MAX_SHEET && level.getBlockState(pos.relative(posAlong, width)).is(self)) width++;
        int height = 1;
        while (height < MAX_SHEET && level.getBlockState(pos.above(height)).is(self)) height++;
        float w = width, h = height;

        Minecraft mc = Minecraft.getInstance();
        Matrix4f pose = ps.last().pose();
        float time = level.getGameTime() + partialTick;
        float along0 = acrossX ? pos.getX() : pos.getZ();
        float y0 = pos.getY();

        // How far off-axis the viewer is from the middle of the opening, as a slope rather
        // than a distance: that is what a perspective projection gives, so the field tracks
        // the camera the way a screen-space one would, and the deeper layers lag behind.
        Vec3 centre = new Vec3(
                pos.getX() + (acrossX ? width * 0.5 : 0.5),
                pos.getY() + height * 0.5,
                pos.getZ() + (acrossX ? 0.5 : width * 0.5));
        Vec3 rel = mc.gameRenderer.getMainCamera().getPosition().subtract(centre);
        double away = Math.max(0.5, Math.abs(acrossX ? rel.z : rel.x));
        float viewU = (float) ((acrossX ? rel.x : rel.z) / away);
        float viewV = (float) (rel.y / away);

        // The whole opening breathes as one: a slow swell in the front layers' brightness.
        float breath = 0.85f + 0.15f * Mth.sin(time * 0.045f + along0 * 0.9f + y0 * 0.6f);

        VertexConsumer vc = buf.getBuffer(WWRenderTypes.surface(style.texture()));
        for (int i = 0; i < style.tile().length; i++) {
            float tile = style.tile()[i] / FIELD_SPAN;   // repeats per block
            float slide = style.parallax()[i];

            // Anchored to world position, with v running downward, so the image sits
            // upright and a sheet that grows keeps the field it already had.
            float u0 = along0 * tile + viewU * slide;
            float vTop = -(y0 + h) * tile - viewV * slide - time * style.scroll()[i];

            int argb = i == 0 ? style.layerArgb()[i] : scaleAlpha(style.layerArgb()[i], breath);
            float offset = LAYER_STEP * (i + 1);
            quad(vc, pose, acrossX, 0.5f - offset, true, argb, 0f, w, 0f, h, u0, vTop, u0 + w * tile, vTop + h * tile);
            quad(vc, pose, acrossX, 0.5f + offset, false, argb, 0f, w, 0f, h, u0, vTop, u0 + w * tile, vTop + h * tile);
        }

        // Sparks: a sparse sheet of motes drifting against the field and sliding with the
        // camera more than anything behind it, so it reads as the nearest thing in the gate.
        {
            float tile = 1.3f;
            float drift = -style.scroll()[style.scroll().length - 1] * 0.8f;
            float u0 = along0 * tile + viewU * 0.42f + time * 0.004f;
            float vTop = -(y0 + h) * tile - viewV * 0.42f - time * drift;
            int argb = scaleAlpha(style.sparkArgb(), 0.7f + 0.3f * Mth.sin(time * 0.11f + along0));
            float offset = LAYER_STEP * (style.tile().length + 1);
            VertexConsumer sv = buf.getBuffer(WWRenderTypes.surface(SPARKLE));
            quad(sv, pose, acrossX, 0.5f - offset, true, argb, 0f, w, 0f, h, u0, vTop, u0 + w * tile, vTop + h * tile);
            quad(sv, pose, acrossX, 0.5f + offset, false, argb, 0f, w, 0f, h, u0, vTop, u0 + w * tile, vTop + h * tile);
        }

        // The rim: around the outside of the opening, a band of light fades in from the edge.
        float pulse = 0.8f + 0.2f * Mth.sin(time * 0.12f + along0 * 1.3f);
        int rim = scaleAlpha(style.rimArgb(), pulse);
        int clear = style.rimArgb() & 0x00FFFFFF;
        float rimOffset = LAYER_STEP * (style.tile().length + 2);
        VertexConsumer rv = buf.getBuffer(WWRenderTypes.flat());
        for (boolean front : new boolean[]{true, false}) {
            float d = front ? 0.5f - rimOffset : 0.5f + rimOffset;
            // Two bands per edge: a hairline at the very edge, and a soft fade behind it.
            // One wide band at full alpha read as a painted frame rather than a glow.
            int line = scaleAlpha(rim, 0.9f);
            int soft = scaleAlpha(rim, 0.38f);
            strip(rv, pose, acrossX, d, front, line, clear, 0f, w, 0f, RIM_LINE);
            strip(rv, pose, acrossX, d, front, soft, clear, 0f, w, 0f, RIM_REACH);
            strip(rv, pose, acrossX, d, front, line, clear, 0f, w, h, h - RIM_LINE);
            strip(rv, pose, acrossX, d, front, soft, clear, 0f, w, h, h - RIM_REACH);
            stripAlong(rv, pose, acrossX, d, front, line, clear, 0f, h, 0f, RIM_LINE);
            stripAlong(rv, pose, acrossX, d, front, soft, clear, 0f, h, 0f, RIM_REACH);
            stripAlong(rv, pose, acrossX, d, front, line, clear, 0f, h, w, w - RIM_LINE);
            stripAlong(rv, pose, acrossX, d, front, soft, clear, 0f, h, w, w - RIM_REACH);
        }
    }

    private static int scaleAlpha(int argb, float mul) {
        int a = Mth.clamp((int) (((argb >>> 24) & 0xFF) * mul), 0, 255);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /**
     * One face of the sheet at {@code d} along the block's normal axis, spanning
     * {@code a0..a1} along the plane and {@code y0..y1} up it.
     *
     * <p>{@code front} is the -X or -Z side; the two windings are mirrored so each face is
     * front-facing from its own side, and each side's layers stack toward its own viewer.</p>
     */
    private static void quad(VertexConsumer vc, Matrix4f pose, boolean acrossX, float d, boolean front, int argb,
                             float a0, float a1, float y0, float y1,
                             float u0, float vTop, float u1, float vBottom) {
        float n = front ? -1f : 1f;
        float nx = acrossX ? 0f : n;
        float nz = acrossX ? n : 0f;

        float[][] xyz = corners(acrossX, d, front, a0, a1, y0, y1);

        // UVs follow the corner order, with vTop at the top, so the image sits upright.
        float[][] uv = front
                ? new float[][]{{u0, vBottom}, {u1, vBottom}, {u1, vTop}, {u0, vTop}}
                : new float[][]{{u0, vBottom}, {u0, vTop}, {u1, vTop}, {u1, vBottom}};

        for (int i = 0; i < 4; i++) {
            vc.addVertex(pose, xyz[i][0], xyz[i][1], xyz[i][2])
                    .setColor(argb)
                    .setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(nx, 0f, nz);
        }
    }

    /** A horizontal band from y=edge to y=inner, bright at the edge and clear at inner. */
    private static void strip(VertexConsumer vc, Matrix4f pose, boolean acrossX, float d, boolean front,
                              int argbEdge, int argbInner, float a0, float a1, float edge, float inner) {
        float lo = Math.min(edge, inner), hi = Math.max(edge, inner);
        float[][] xyz = corners(acrossX, d, front, a0, a1, lo, hi);
        int[] col = new int[4];
        for (int i = 0; i < 4; i++) col[i] = xyz[i][1] == edge ? argbEdge : argbInner;
        emit(vc, pose, acrossX, front, xyz, col);
    }

    /** A vertical band from along=edge to along=inner, bright at the edge and clear at inner. */
    private static void stripAlong(VertexConsumer vc, Matrix4f pose, boolean acrossX, float d, boolean front,
                                   int argbEdge, int argbInner, float y0, float y1, float edge, float inner) {
        float lo = Math.min(edge, inner), hi = Math.max(edge, inner);
        float[][] xyz = corners(acrossX, d, front, lo, hi, y0, y1);
        int[] col = new int[4];
        for (int i = 0; i < 4; i++) {
            float along = acrossX ? xyz[i][0] : xyz[i][2];
            col[i] = along == edge ? argbEdge : argbInner;
        }
        emit(vc, pose, acrossX, front, xyz, col);
    }

    private static void emit(VertexConsumer vc, Matrix4f pose, boolean acrossX, boolean front,
                             float[][] xyz, int[] col) {
        float n = front ? -1f : 1f;
        float nx = acrossX ? 0f : n;
        float nz = acrossX ? n : 0f;
        for (int i = 0; i < 4; i++) {
            vc.addVertex(pose, xyz[i][0], xyz[i][1], xyz[i][2])
                    .setColor(col[i])
                    .setUv(0.5f, 0.5f)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(nx, 0f, nz);
        }
    }

    /**
     * Corners of a rectangle on the sheet spanning {@code a0..a1} along the plane and
     * {@code y0..y1} up it, wound to face the viewer on the given side.
     */
    private static float[][] corners(boolean acrossX, float d, boolean front,
                                     float a0, float a1, float y0, float y1) {
        if (acrossX) {
            return front ? new float[][]{{a0, y0, d}, {a1, y0, d}, {a1, y1, d}, {a0, y1, d}}
                         : new float[][]{{a0, y0, d}, {a0, y1, d}, {a1, y1, d}, {a1, y0, d}};
        }
        return front ? new float[][]{{d, y0, a0}, {d, y1, a0}, {d, y1, a1}, {d, y0, a1}}
                     : new float[][]{{d, y0, a0}, {d, y0, a1}, {d, y1, a1}, {d, y1, a0}};
    }
}
