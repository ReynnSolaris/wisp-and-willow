package com.reynn.wispwillow.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class BeamRender {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("wispwillow", "textures/effect/fluxia_beam.png");

    private record Basis(Vec3 x, Vec3 y, Vec3 z) {}

    private static Basis makeBasis(Vec3 dir) {
        Vec3 z = dir.normalize();
        Vec3 a = (Math.abs(z.y) > 0.9) ? new Vec3(1,0,0) : new Vec3(0,1,0);
        Vec3 x = z.cross(a).normalize();
        if (x.lengthSqr() < 1e-8) x = new Vec3(1,0,0);
        Vec3 y = z.cross(x).normalize();
        return new Basis(x,y,z);
    }

    private static float[] argbToRGBA(int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>>  8) & 0xFF) / 255f;
        float b = ( argb         & 0xFF) / 255f;
        return new float[]{r,g,b,a};
    }

    private static void put(VertexConsumer vc, Matrix4f m, Vec3 p,
                            float u, float v, float[] rgba,
                            int light, int ov) {
        vc.addVertex(m, (float)p.x, (float)p.y, (float)p.z)
                .setColor(rgba[0], rgba[1], rgba[2], rgba[3])
                .setUv(u, v)
                .setOverlay(ov)
                .setLight(light)
                .setNormal(0, 1, 0);
    }

    /* ---------- Public API (LOCAL/model space) ---------- */

    /** Single billboarded strip between local points A->B, gradient from near->far. */
    public static void drawStripLocal(PoseStack ps, MultiBufferSource buf, Camera cam,
                                      Vec3 fromLocal, Vec3 toLocal, float width,
                                      int argbNear, int argbFar,
                                      float scroll, float planePhase) {

        Vec3 A = fromLocal;
        Vec3 B = toLocal;
        Vec3 dir = B.subtract(A);
        double len = dir.length();
        if (len < 1e-4) return;
        dir = dir.scale(1.0/len);

        Basis basis = makeBasis(dir);
        Vec3 axis = basis.x.scale(Math.cos(planePhase)).add(basis.y.scale(Math.sin(planePhase)));

        float v0 = scroll;
        float v1 = scroll + (float)(len * 0.25);

        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(TEX));
        Matrix4f m = ps.last().pose();
        int light = LightTexture.FULL_BRIGHT, ov = OverlayTexture.NO_OVERLAY;

        Vec3 off = axis.scale(width * 0.5f);
        Vec3 a0 = A.add(off), a1 = A.subtract(off), b0 = B.add(off), b1 = B.subtract(off);

        float[] cNear = argbToRGBA(argbNear);
        float[] cFar  = argbToRGBA(argbFar);

        put(vc, m, a0, 0f, v0, cNear, light, ov);
        put(vc, m, a1, 1f, v0, cNear, light, ov);
        put(vc, m, b1, 1f, v1, cFar,  light, ov);
        put(vc, m, b0, 0f, v1, cFar,  light, ov);
    }

    /** Two crossed strips between local points for a “rounder” column. */
    public static void drawColumnLocal(PoseStack ps, MultiBufferSource buf, Camera cam,
                                       Vec3 fromLocal, Vec3 toLocal,
                                       float baseWidth, float tipWidth,
                                       int argbBase, int argbTip,
                                       float time) {
        drawGradientStripLocal(ps, buf, cam, fromLocal, toLocal, baseWidth, tipWidth, argbBase, argbTip, 0f);
        drawGradientStripLocal(ps, buf, cam, fromLocal, toLocal, baseWidth, tipWidth, argbBase, argbTip, (float)Math.PI * 0.5f);
    }

    private static void drawGradientStripLocal(PoseStack ps, MultiBufferSource buf, Camera cam,
                                               Vec3 fromLocal, Vec3 toLocal,
                                               float widthNear, float widthFar,
                                               int argbNear, int argbFar,
                                               float planePhase) {

        Vec3 A = fromLocal;
        Vec3 B = toLocal;
        Vec3 dir = B.subtract(A);
        double len = dir.length();
        if (len < 1e-4) return;
        dir = dir.scale(1.0/len);

        Basis basis = makeBasis(dir);
        Vec3 axis = basis.x.scale(Math.cos(planePhase)).add(basis.y.scale(Math.sin(planePhase)));

        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(TEX));
        Matrix4f m = ps.last().pose();
        int light = LightTexture.FULL_BRIGHT, ov = OverlayTexture.NO_OVERLAY;

        Vec3 offNear = axis.scale(widthNear * 0.5f);
        Vec3 offFar  = axis.scale(widthFar  * 0.5f);

        Vec3 a0 = A.add(offNear);
        Vec3 a1 = A.subtract(offNear);
        Vec3 b0 = B.add(offFar);
        Vec3 b1 = B.subtract(offFar);

        float[] cNear = argbToRGBA(argbNear);
        float[] cFar  = argbToRGBA(argbFar);

        put(vc, m, a0, 0f, 0f, cNear, light, ov);
        put(vc, m, a1, 1f, 0f, cNear, light, ov);
        put(vc, m, b1, 1f, 1f, cFar,  light, ov);
        put(vc, m, b0, 0f, 1f, cFar,  light, ov);
    }

    /** Dragon-style burst in a billboard plane that faces the camera. Center is LOCAL. */
    public static void drawDragonBurstLocal(PoseStack ps, MultiBufferSource buf, Camera cam,
                                            Vec3 centerLocal,
                                            float innerRadius, float maxLen, float baseWidth,
                                            int argbInner, int argbTip,
                                            float time, float progress01) {

        // Vector pointing FROM center towards camera (so the plane faces the player)
        Vec3 camDir = Vec3.directionFromRotation(cam.getXRot(), cam.getYRot()).scale(-1.0).normalize();
        Basis basis = makeBasis(camDir);

        final int rays = 18;
        final float spin = time * 0.08f;
        final float grow = Math.max(0f, Math.min(1f, progress01));

        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(TEX));
        Matrix4f m = ps.last().pose();

        float[] cNear = argbToRGBA(argbInner);
        float[] cFar  = argbToRGBA(argbTip);
        int light = LightTexture.FULL_BRIGHT, ov = OverlayTexture.NO_OVERLAY;

        for (int i = 0; i < rays; i++) {
            float ang = spin + i * (float)(Math.PI * 2.0 / rays);

            Vec3 d = basis.x.scale(Math.cos(ang)).add(basis.y.scale(Math.sin(ang)));

            float len = (0.35f + 0.65f * (0.5f * (1f + (float)Math.sin(time * 0.9f + i * 0.7f))))
                    * maxLen * grow;
            float w0 = baseWidth * 0.30f;
            float w1 = baseWidth * 1.30f;

            Vec3 start = centerLocal.add(d.scale(innerRadius));
            Vec3 tip   = centerLocal.add(d.scale(innerRadius + len));
            Vec3 perp  = d.cross(camDir).normalize();

            Vec3 s0 = start.add(perp.scale(w0));
            Vec3 s1 = start.subtract(perp.scale(w0));
            Vec3 t0 = tip.add(perp.scale(w1));
            Vec3 t1 = tip.subtract(perp.scale(w1));

            put(vc, m, s0, 0f, 0f, cNear, light, ov);
            put(vc, m, s1, 1f, 0f, cNear, light, ov);
            put(vc, m, t1, 1f, 1f, cFar,  light, ov);
            put(vc, m, t0, 0f, 1f, cFar,  light, ov);
        }
    }
}
