package com.reynn.wispwillow.client.renderers;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.reynn.wispwillow.WispWillow;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;

/**
 * The gold enchantment glint worn by awakened empyrite.
 *
 * <p>Each type here is vanilla's matching glint type with exactly one thing changed: the
 * texture. That is not laziness, it is the whole trick. The glint fragment shader is
 * {@code texture(Sampler0, texCoord0) * ColorModulator} and never reads vertex colour, so
 * a per-item tint is simply not expressible; recolouring the streak texture is the only
 * way to colour one item's glint without repainting every glint in the game.</p>
 *
 * <p>Every other state shard is shared with vanilla <em>by reference</em>, the shader shard
 * most of all. {@link RenderStateShard#RENDERTYPE_GLINT_SHADER} wraps
 * {@code GameRenderer::getRendertypeGlintShader}, which is the very hook Iris replaces when
 * a shader pack is loaded, so these types resolve to the pack's glint program the same way
 * vanilla's do. This is the sharp edge {@link WWRenderTypes} documents from the other
 * direction: a bespoke type with a bespoke shader is not in a pack's mapping and silently
 * draws nothing. Borrowing the shard instead of declaring one is what keeps these safe, so
 * do not "tidy" them into a shader of their own.</p>
 *
 * <p>They are also registered as fixed render buffers below. A type missing from that map
 * shares one scratch buffer with every other stray type and is flushed the moment anything
 * else claims it, which for a glint means being drawn before the geometry it is meant to
 * sit on; and since glints depth-test with {@code EQUAL_DEPTH_TEST}, drawn early is drawn
 * invisible.</p>
 */
@EventBusSubscriber(modid = WispWillow.MODID, value = Dist.CLIENT)
public final class WWGlintRenderTypes {
    private WWGlintRenderTypes() {}

    public static final ResourceLocation AWAKENED_GLINT_ITEM =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "textures/misc/awakened_glint_item.png");
    public static final ResourceLocation AWAKENED_GLINT_ENTITY =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "textures/misc/awakened_glint_entity.png");

    /** Counterpart of {@link RenderType#glint()}: inventory, hand and item frames. */
    public static final RenderType GLINT = RenderType.create(
            "wispwillow_awakened_glint",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_GLINT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(AWAKENED_GLINT_ITEM, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setTexturingState(RenderStateShard.GLINT_TEXTURING)
                    .createCompositeState(false));

    /** Counterpart of {@link RenderType#glintTranslucent()}: items under fabulous graphics. */
    public static final RenderType GLINT_TRANSLUCENT = RenderType.create(
            "wispwillow_awakened_glint_translucent",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_GLINT_TRANSLUCENT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(AWAKENED_GLINT_ITEM, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setTexturingState(RenderStateShard.GLINT_TEXTURING)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .createCompositeState(false));

    /** Counterpart of {@link RenderType#entityGlint()}: stacks lying on the ground. */
    public static final RenderType ENTITY_GLINT = RenderType.create(
            "wispwillow_awakened_entity_glint",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_GLINT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(AWAKENED_GLINT_ENTITY, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .setTexturingState(RenderStateShard.ENTITY_GLINT_TEXTURING)
                    .createCompositeState(false));

    /** Counterpart of {@link RenderType#entityGlintDirect()}. */
    public static final RenderType ENTITY_GLINT_DIRECT = RenderType.create(
            "wispwillow_awakened_entity_glint_direct",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(AWAKENED_GLINT_ENTITY, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setTexturingState(RenderStateShard.ENTITY_GLINT_TEXTURING)
                    .createCompositeState(false));

    /** Counterpart of {@link RenderType#armorEntityGlint()}: armour worn on a body. */
    public static final RenderType ARMOR_ENTITY_GLINT = RenderType.create(
            "wispwillow_awakened_armor_entity_glint",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(AWAKENED_GLINT_ENTITY, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setTexturingState(RenderStateShard.ENTITY_GLINT_TEXTURING)
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .createCompositeState(false));

    @SubscribeEvent
    static void onRegisterRenderBuffers(RegisterRenderBuffersEvent event) {
        event.registerRenderBuffer(GLINT);
        event.registerRenderBuffer(GLINT_TRANSLUCENT);
        event.registerRenderBuffer(ENTITY_GLINT);
        event.registerRenderBuffer(ENTITY_GLINT_DIRECT);
        event.registerRenderBuffer(ARMOR_ENTITY_GLINT);
    }
}
