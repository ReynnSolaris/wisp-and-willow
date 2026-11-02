package com.reynn.wispwillow.client.renderers;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Render types for the mod's world-space effects.
 *
 * <p>Everything here is a <em>stock</em> vanilla render type on purpose. Shader packs map
 * vanilla types onto their own gbuffer programs; a bespoke {@code RenderType.create(...)}
 * is not in that mapping, so with Iris enabled it silently draws nothing. That is exactly
 * what happened here: the effects were fine until a shader pack was switched on.</p>
 *
 * <p>Two consequences worth knowing:</p>
 * <ul>
 *   <li>The <em>emissive</em> entity type declares no lightmap state, and outside entity
 *       rendering nothing else binds the lightmap, so it draws nothing at our stage. The
 *       plain translucent entity type plus a full-bright lightmap coordinate looks the
 *       same and actually works.</li>
 *   <li>These types depth-test, so overlays are occluded by terrain rather than drawn
 *       through it — which is how the links were wanted anyway.</li>
 * </ul>
 */
public final class WWRenderTypes {
    private WWRenderTypes() {}

    /** A small fully opaque white square, for flat untextured fills like the gauges. */
    public static final ResourceLocation WHITE_TEX =
            ResourceLocation.fromNamespaceAndPath("wispwillow", "textures/effect/white.png");

    /**
     * Ordinary textured quads, alpha-blended, and able to be fully opaque — the Soul
     * Gateway's surface. Vertices are emitted at full-bright so it reads as self-lit.
     *
     * <p>Beams and links come through here too. An additive type was tried for those and
     * withdrawn: {@code RenderType.eyes} is the only stock additive one, and unlike this it
     * sets no cull state, so back-face culling is on. A ribbon's winding depends on which
     * way the link runs, so half of them were culled away while the camera-facing sprites
     * survived — and Photon dropped it entirely. Glow is built from geometry instead; see
     * {@code OverlayShapes.glowRibbon}.</p>
     */
    public static RenderType surface(ResourceLocation tex) {
        return RenderType.entityTranslucent(tex);
    }

    /** Flat coloured quads: gauge plates, bars and frames. */
    public static RenderType flat() {
        return RenderType.entityTranslucent(WHITE_TEX);
    }
}
