package com.reynn.wispwillow.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reynn.wispwillow.blocks.AltarBlockEntity;
import com.reynn.wispwillow.blocks.SigilType;
import com.reynn.wispwillow.ritual.RitualRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.*;

public class AltarBER implements BlockEntityRenderer<AltarBlockEntity> {
    public AltarBER(BlockEntityRendererProvider.Context ctx) {}
    private static final Map<ResourceLocation, Entity> ENTITY_CACHE = new HashMap<>();

    private static ResourceLocation texFor(SigilType t) {
        return ResourceLocation.fromNamespaceAndPath(
                "wispwillow", "textures/block/sigil/" + t.getSerializedName().toLowerCase() + ".png");
    }
    private static final double RESULT_Y = 1.70;  // same Y you use in renderResultPreview()

    /**
     * One reagent in the orbit: a stack to draw, how many this altar wants, and how many
     * a bare altar would have wanted.
     *
     * <p>{@code baseCount} is what lets the orbit show its own discount. Without it the
     * preview drew the recipe's raw numbers, so an altar with a Frugal familiar serving it
     * asked you for four ingots on screen and then took two off the ground.</p>
     */
    private record InputVisual(ItemStack stack, int count, int baseCount) {}

    /** Flattened ingredient list -> how many of each distinct item it asks for. */
    private static Map<Item, Integer> countByItem(List<Ingredient> inputs) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (Ingredient ing : inputs) {
            ItemStack rep = representativeStack(ing);
            if (rep.isEmpty()) continue;
            counts.merge(rep.getItem(), 1, Integer::sum);
        }
        return counts;
    }

    /**
     * Builds the orbit from the discounted list, carrying the undiscounted counts alongside
     * so each reagent can show what it would otherwise have cost.
     */
    private static List<InputVisual> collapseInputs(List<Ingredient> effective, List<Ingredient> base) {
        Map<Item, Integer> baseCounts = countByItem(base);
        Map<Item, InputVisual> map = new LinkedHashMap<>();

        for (Ingredient ing : effective) {
            ItemStack rep = representativeStack(ing);
            if (rep.isEmpty()) continue;

            var item = rep.getItem();
            var prev = map.get(item);
            if (prev == null) {
                ItemStack display = rep.copy();
                display.setCount(1); // keep it visually as a single item
                map.put(item, new InputVisual(display, 1, baseCounts.getOrDefault(item, 1)));
            } else {
                map.put(item, new InputVisual(prev.stack(), prev.count() + 1, prev.baseCount()));
            }
        }

        return new ArrayList<>(map.values());
    }

    private static Vec3 resultAnchorWorld(AltarBlockEntity be) {
        var bp = be.getBlockPos();
        return new Vec3(bp.getX() + 0.5, bp.getY() + RESULT_Y, bp.getZ() + 0.5);
    }
    /** Widened so the ghost-glyph overlay for a large circle isn't frustum-culled with the altar block. */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(AltarBlockEntity be) {
        var box = be.getRenderBoundingBox();
        // The bolt and the vigil's column reach for the sky; without this they vanish the
        // moment the altar itself leaves the frustum.
        if (be.hasVigil() || (be.strikeAge(0f) >= 0f && be.strikeAge(0f) < STRIKE_TICKS)) {
            box = box.expandTowards(0, SKY_REACH, 0);
        }
        return box;
    }

    @Override
    public boolean shouldRender(AltarBlockEntity be, Vec3 cameraPos) {
        var pos = be.getBlockPos();
        int r = (be.getCachedMatch() != null)
                ? Math.max(4, be.getCachedMatch().grid().radius)
                : 6;

        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        double budget = Math.max(getViewDistance(), (r + 2) * 16);
        return cameraPos.distanceToSqr(cx, cy, cz) <= budget * budget;
    }

    @Override
    public void render(AltarBlockEntity be, float partial, PoseStack ps,
                       MultiBufferSource buf, int packedLight, int packedOverlay) {

        renderSkyfall(be, partial, ps, buf);

        var recipe = be.getCachedMatch();
        if (recipe == null) return;

        // --------- 1) MISSING SIGIL GHOSTS (your existing ground quads) ---------
        var missing = be.getMissing();
        var lvl  = be.getLevel();
        var base = be.getBlockPos();
        if (!missing.isEmpty()) {
            for (var m : missing) {
                var type = m.type().orElse(SigilType.BASE);

                int rgb = m.color().map(net.minecraft.world.item.DyeColor::getTextureDiffuseColor).orElse(0xFFFFFF);
                float r = ((rgb >> 16) & 0xFF) / 255f;
                float g = ((rgb >>  8) & 0xFF) / 255f;
                float b = ( rgb        & 0xFF) / 255f;
                float a = 0.70f;

                double dx = m.wx() - base.getX();
                double dy = m.wy() - base.getY();
                double dz = m.wz() - base.getZ();

                double top = 1.0;
                if (lvl != null) {
                    var tPos = new net.minecraft.core.BlockPos(m.wx(), m.wy(), m.wz());
                    var st = lvl.getBlockState(tPos);
                    var shape = st.getShape(lvl, tPos);
                    if (!shape.isEmpty()) top = shape.max(net.minecraft.core.Direction.Axis.Y);
                }
                double yLocal = dy + top + 1.0e-3;

                ps.pushPose();
                ps.translate(dx + 0.5, yLocal - 1, dz + 0.5);

                var vc = buf.getBuffer(RenderType.entityTranslucent(texFor(type)));
                var pose = ps.last().pose();
                int light   = LightTexture.FULL_BRIGHT;
                int overlay = OverlayTexture.NO_OVERLAY;

                // Quad (double-sided)
                vtx(vc, pose, -0.5f, 0.0f, -0.5f, 0f, 0f, r,g,b,a, light, overlay, 0f, 1f, 0f);
                vtx(vc, pose,  0.5f, 0.0f, -0.5f, 1f, 0f, r,g,b,a, light, overlay, 0f, 1f, 0f);
                vtx(vc, pose,  0.5f, 0.0f,  0.5f, 1f, 1f, r,g,b,a, light, overlay, 0f, 1f, 0f);
                vtx(vc, pose, -0.5f, 0.0f,  0.5f, 0f, 1f, r,g,b,a, light, overlay, 0f, 1f, 0f);

                vtx(vc, pose, -0.5f, -0.01f,  0.5f, 0f, 1f, r,g,b,a, light, overlay, 0f,0.9f, 0f);
                vtx(vc, pose,  0.5f, -0.01f,  0.5f, 1f, 1f, r,g,b,a, light, overlay, 0f,0.9f, 0f);
                vtx(vc, pose,  0.5f, -0.01f, -0.5f, 1f, 0f, r,g,b,a, light, overlay, 0f,0.9f, 0f);
                vtx(vc, pose, -0.5f, -0.01f, -0.5f, 0f, 0f, r,g,b,a, light, overlay, 0f,0.9f, 0f);

                ps.popPose();
            }
        }

        renderOrbitingIngredients(be, recipe, partial, ps, buf, packedLight);
        renderResultPreview(be, recipe, partial, ps, buf, packedLight);
        renderBoons(be, recipe, ps, buf);
        /*
        var mc  = Minecraft.getInstance();
        var cam = mc.gameRenderer.getMainCamera();
        double t = mc.level.getGameTime() + partial;

        Vec3 C = new Vec3(0.5, RESULT_Y, 0.5);
        float prog = 0.5f + 0.5f * Mth.sin((float)t * 0.06f);
        BeamRender.drawDragonBurstLocal(
                ps, buf, cam,
                C,
                0.12f,          // inner radius
                3.75f,          // ray length
                0.22f,          // base ray width
                0xD08BE3FF,     // inner color ARGB
                0x008BE3FF,     // tip color ARGB (often ~0 alpha)
                (float)t, prog
        );
        BeamRender.drawColumnLocal(
                ps, buf, cam,
                C, C.add(0, 3.5 * prog, 0),
                0.24f,          // base width
                0.12f,          // tip width
                0xC08BE3FF,     // base ARGB
                0x208BE3FF,     // tip ARGB
                (float)t
        );
         */
    }

    // =====================================================================
    // The strike and the vigil: light from the sky
    // =====================================================================

    /** How long the bolt that answers a completed rite lasts, in ticks. */
    private static final int STRIKE_TICKS = 48;
    /** How far above the altar the column is drawn. */
    private static final float SKY_REACH = 72f;

    private static final int CORE_ARGB   = 0xFFEFFBFF;
    private static final int CYAN_ARGB   = 0xFF8BE3FF;
    private static final int VIOLET_ARGB = 0xFF9A6CFF;

    /**
     * A completed rite is answered by a bolt from the sky: a white-hot core inside a violet
     * halo that slams down, flares, and thins away, with a ring of light racing out across
     * the circle and rays bursting from the altar. A vigil, being a rite that keeps burning,
     * holds a steadier column with a slow pulse and a halo ring turning about it.
     */
    private void renderSkyfall(AltarBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var cam = mc.gameRenderer.getMainCamera();
        float t = mc.level.getGameTime() + partial;
        Vec3 c = new Vec3(0.5, 1.0, 0.5);

        float age = be.strikeAge(partial);
        if (age >= 0f && age < STRIKE_TICKS) {
            float p = age / STRIKE_TICKS;                    // 0 -> 1 over the strike
            float slam = Mth.clamp(age / 4f, 0f, 1f);        // the column arrives in four ticks
            float fade = 1f - Mth.clamp((p - 0.45f) / 0.55f, 0f, 1f);
            fade = fade * fade;
            float width = (0.20f + 0.70f * (1f - p) * (1f - p)) * slam;

            // Halo, then core: the wide faint one first so the bright one draws over it.
            BeamRender.drawColumnLocal(ps, buf, cam, c, c.add(0, SKY_REACH * slam, 0),
                    width * 2.6f, width * 1.4f,
                    withAlpha(VIOLET_ARGB, 0.32f * fade), withAlpha(VIOLET_ARGB, 0.05f * fade), t);
            BeamRender.drawColumnLocal(ps, buf, cam, c, c.add(0, SKY_REACH * slam, 0),
                    width, width * 0.55f,
                    withAlpha(CORE_ARGB, 0.95f * fade), withAlpha(CYAN_ARGB, 0.35f * fade), t);

            // Rays out of the altar that are longest at the moment of impact.
            BeamRender.drawDragonBurstLocal(ps, buf, cam, c.add(0, 0.25, 0),
                    0.15f, 3.2f, 0.22f,
                    withAlpha(CYAN_ARGB, 0.85f * fade), withAlpha(CYAN_ARGB, 0f),
                    t, 1f - p * 0.6f);

            // The shockwave: a ring on the ground racing out to the edge of the circle.
            int radius = be.getCachedMatch() != null ? be.getCachedMatch().maxRadius() : 4;
            float reach = (radius + 1.2f) * Mth.clamp(age / 18f, 0f, 1f);
            float ringFade = 1f - Mth.clamp(age / 22f, 0f, 1f);
            groundRing(ps, buf, 0.5f, 0.06f, 0.5f, reach, 0.45f,
                    withAlpha(CORE_ARGB, 0.85f * ringFade), withAlpha(CYAN_ARGB, 0f));
        }

        if (be.hasVigil()) {
            float pulse = 0.5f + 0.5f * Mth.sin(t * 0.07f);
            float width = 0.22f + 0.08f * pulse;
            BeamRender.drawColumnLocal(ps, buf, cam, c, c.add(0, SKY_REACH, 0),
                    width * 2.4f, width * 1.2f,
                    withAlpha(VIOLET_ARGB, 0.22f), withAlpha(VIOLET_ARGB, 0.03f), t);
            BeamRender.drawColumnLocal(ps, buf, cam, c, c.add(0, SKY_REACH, 0),
                    width, width * 0.5f,
                    withAlpha(CORE_ARGB, 0.7f + 0.2f * pulse), withAlpha(CYAN_ARGB, 0.25f), t);
            BeamRender.drawDragonBurstLocal(ps, buf, cam, c.add(0, 0.25, 0),
                    0.12f, 2.2f, 0.16f,
                    withAlpha(CYAN_ARGB, 0.6f), withAlpha(CYAN_ARGB, 0f),
                    t, 0.55f + 0.45f * pulse);
            // A halo ring that breathes in and out about the column.
            float r = 1.1f + 0.25f * Mth.sin(t * 0.05f);
            groundRing(ps, buf, 0.5f, 1.35f + 0.1f * Mth.sin(t * 0.09f), 0.5f, r, 0.18f,
                    withAlpha(CYAN_ARGB, 0.55f), withAlpha(VIOLET_ARGB, 0.15f));
        }
    }

    /**
     * A flat ring of light lying in the XZ plane, drawn both ways up: {@code argbIn} on the
     * inner edge and {@code argbOut} on the outer, so it can fade to nothing outward.
     */
    private static void groundRing(PoseStack ps, MultiBufferSource buf, float cx, float y, float cz,
                                   float radius, float width, int argbIn, int argbOut) {
        if (radius <= 0.01f) return;
        var vc = buf.getBuffer(WWRenderTypes.flat());
        var pose = ps.last().pose();
        int light = LightTexture.FULL_BRIGHT, overlay = OverlayTexture.NO_OVERLAY;
        final int segs = 40;
        float rIn = Math.max(0f, radius - width * 0.5f), rOut = radius + width * 0.5f;
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (Math.PI * 2 * i / segs), a1 = (float) (Math.PI * 2 * (i + 1) / segs);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float[][] q = {
                    {cx + c0 * rIn, y, cz + s0 * rIn}, {cx + c1 * rIn, y, cz + s1 * rIn},
                    {cx + c1 * rOut, y, cz + s1 * rOut}, {cx + c0 * rOut, y, cz + s0 * rOut}};
            int[] col = {argbIn, argbIn, argbOut, argbOut};
            for (int k = 0; k < 4; k++)
                vc.addVertex(pose, q[k][0], q[k][1], q[k][2]).setColor(col[k]).setUv(0.5f, 0.5f)
                        .setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            for (int k = 3; k >= 0; k--)
                vc.addVertex(pose, q[k][0], q[k][1] - 0.002f, q[k][2]).setColor(col[k]).setUv(0.5f, 0.5f)
                        .setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        }
    }

    private static int withAlpha(int argb, float a) {
        int al = Mth.clamp((int) (255 * a), 0, 255);
        return (al << 24) | (argb & 0x00FFFFFF);
    }

    private void renderOrbitingIngredients(AltarBlockEntity be, RitualRecipe recipe,
                                           float partial, PoseStack ps, MultiBufferSource buf,
                                           int packedLight) {
        java.util.List<Ingredient> base = recipe.inputs();
        if (base.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // What this altar will actually take off the ground, boons included. The boon state
        // rides the altar's normal sync, so binding or banishing a familiar changes the orbit
        // on the next altar update rather than only on the next relog.
        var visuals = collapseInputs(be.effectiveInputs(recipe), base);
        if (visuals.isEmpty()) return;

        double t = mc.level.getGameTime() + partial;

        int n = visuals.size();
        double baseSpeed = 0.08;          // radians/tick
        double baseRadius = 0.65;         // starting radius
        double radius = baseRadius + Math.max(0, n - 6) * 0.05; // tiny expansion for lots of distinct items
        double bobAmp = 0.12;
        double yBase  = 1.45;             // below result Y
        float  itemScale = 0.35f;

        var font = mc.font;
        var disp = mc.getEntityRenderDispatcher();

        int i = 0;
        for (InputVisual visual : visuals) {
            ItemStack stack = visual.stack();
            int count       = visual.count();
            if (stack.isEmpty()) {
                i++;
                continue;
            }

            double ang = t * baseSpeed + (Mth.TWO_PI * i / n);
            double ox  = Math.cos(ang) * radius;
            double oz  = Math.sin(ang) * radius;
            double oy  = bobAmp * Math.sin(t * 0.15 + i * 0.9);

            ps.pushPose();
            // Local: center around altar
            ps.translate(0.5 + ox, yBase + oy, 0.5 + oz);
            Quaternionf rot = Axis.YP.rotationDegrees((float)((t * 4.0 + i * 30.0) % 360));
            ps.mulPose(rot);
            ps.scale(itemScale, itemScale, itemScale);

            // Render the item
            mc.getItemRenderer().renderStatic(
                    stack, ItemDisplayContext.FIXED,
                    packedLight, OverlayTexture.NO_OVERLAY,
                    ps, buf, mc.level, 0
            );

            // Draw the count whenever it is worth reading: more than one is needed, or the
            // altar has talked the price down and that ought to be visible.
            int baseCount = visual.baseCount();
            boolean discounted = count < baseCount;
            if (count > 1 || discounted) {
                ps.pushPose();
                // move up a bit above the item
                ps.translate(0.0, 0.70, 0.0);

                // face the camera (same trick as name tags)
                //ps.mulPose(disp.cameraOrientation());
                ps.mulPose(rot.invert());
                // scale: bigger and not microscopic
                float textScale = 0.035f; // vanilla name tags use 0.025
                ps.scale(-textScale, -textScale, textScale);

                String label = discounted ? "x" + count + " (was " + baseCount + ")" : "x" + count;
                int rgb = discounted ? DISCOUNT_RGB : 0xFFFFFF;
                float width = font.width(label) / 2.0f;

                font.drawInBatch(
                        label,
                        -width, 0.0f,
                        rgb,
                        false,
                        ps.last().pose(),
                        buf,
                        Font.DisplayMode.NORMAL,
                        0,
                        0xFFFFFF
                );
                ps.mulPose(Axis.YP.rotationDegrees(180.0F));

                font.drawInBatch(
                        label,
                        -width, 0.0f,
                        rgb,
                        false,
                        ps.last().pose(),
                        buf,
                        Font.DisplayMode.NORMAL,
                        0,
                        0xFFFFFF
                );
                ps.popPose();
            }

            ps.popPose();
            i++;
        }
    }

    private static ItemStack representativeStack(Ingredient ing) {
        // Pick the first displayable stack
        ItemStack[] choices = ing.getItems();
        if (choices != null && choices.length > 0) {
            for (ItemStack s : choices) if (!s.isEmpty()) return s;
        }
        return ItemStack.EMPTY;
    }

    private void renderResultPreview(AltarBlockEntity be, RitualRecipe r,
                                     float partial, PoseStack ps, MultiBufferSource buf,
                                     int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var font = mc.font;
        final float  SPIN = 2.0f;       // deg/tick
        final float  yawDeg = (float)((mc.level.getGameTime() + partial) * SPIN % 360f);

        var mobId = r.resultMobId();
        if (mobId.isPresent()) {
            Entity e = getOrCreatePreviewEntity(mc, mobId.get());
            if (e == null) return;

            // Freeze all rotations to avoid interpolation tug-of-war

            if (e instanceof net.minecraft.world.entity.LivingEntity le) {
                le.setNoGravity(true);
                le.setOnGround(false);
                tryStopWalkAnim(le);
            }

            // scale to fit nicely
            float maxDim = Math.max(e.getBbHeight(), e.getBbWidth() * 1.5f);
            float scale = Mth.clamp(0.9f / Math.max(0.3f, maxDim), 0.15f, 0.9f);

            ps.pushPose();
            ps.translate(0.5, RESULT_Y, 0.5);                     // pivot at altar center
            ps.mulPose(Axis.YP.rotationDegrees(yawDeg));
            ps.scale(scale, scale, scale);

            var disp = mc.getEntityRenderDispatcher();
            disp.setRenderShadow(false);
            // Pass 0 here; living renderers use entity fields, not this yaw
            disp.render(e, 0.0, 0.0, 0.0, 0.0f, partial, ps, buf, packedLight);
            disp.setRenderShadow(true);   // global flag (vanilla's default): leave it off and every entity loses its shadow
            if (r.resultMobCount() > 1) {
                float textScale = 0.035f; // vanilla name tags use 0.025
                ps.scale(-textScale, -textScale, textScale);
                String label = "x" + r.resultMobCount();
                float width = font.width(label) / 2.0f;
                font.drawInBatch(
                        label,
                        -width, 0.0f,
                        0xFFFFFF,
                        false,
                        ps.last().pose(),
                        buf,
                        Font.DisplayMode.NORMAL,
                        0,
                        0xFFFFFF
                );

                ps.mulPose(Axis.YP.rotationDegrees(180.0F));

                font.drawInBatch(
                        label,
                        -width, 0.0f,
                        0xFFFFFF,
                        false,
                        ps.last().pose(),
                        buf,
                        Font.DisplayMode.NORMAL,
                        0,
                        0xFFFFFF
                );
            }
            ps.popPose();
            return;
        }

        // Item result: centered & spinning
        r.resultItem().ifPresent(stack -> {
            ps.pushPose();
            ps.translate(0.5, RESULT_Y, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(yawDeg));
            ps.scale(.8f, .8f, .8f);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack, ItemDisplayContext.FIXED,
                    packedLight, OverlayTexture.NO_OVERLAY,
                    ps, buf, mc.level, 0
            );

            if (stack.getCount() > 1) {
                ps.pushPose();
                // move up a bit above the item
                ps.translate(0.0, 0.70, 0.0);

                float textScale = 0.035f; // vanilla name tags use 0.025
                ps.scale(-textScale, -textScale, textScale);

                String label = "x" + stack.getCount();
                float width = font.width(label) / 2.0f;

                font.drawInBatch(
                        label,
                        -width, 0.0f,
                        0xFFFFFF,
                        false,
                        ps.last().pose(),
                        buf,
                        Font.DisplayMode.NORMAL,
                        0,
                        0xFFFFFF
                );

                ps.mulPose(Axis.YP.rotationDegrees(180.0F));

                font.drawInBatch(
                        label,
                        -width, 0.0f,
                        0xFFFFFF,
                        false,
                        ps.last().pose(),
                        buf,
                        Font.DisplayMode.NORMAL,
                        0,
                        0xFFFFFF
                );
                ps.popPose();
            }

            ps.popPose();
        });
    }


    /** Optional: stop idle walk animation drift (helps bees/zombies). */
    private static void tryStopWalkAnim(net.minecraft.world.entity.LivingEntity le) {
        try {
            // 1.21 exposes a WalkAnimationState; keep it still if present
            var f = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("walkAnimation");
            f.setAccessible(true);
            Object state = f.get(le);
            if (state != null) {
                // state.setSpeed(0) in Mojang mappings
                var m = state.getClass().getMethod("setSpeed", float.class);
                m.invoke(state, 0f);
            }
        } catch (Throwable ignored) {
            // Not available on all mappings/entities.
        }
    }

    private static Entity getOrCreatePreviewEntity(Minecraft mc, ResourceLocation id) {
        Entity cached = ENTITY_CACHE.get(id);
        if (cached != null && cached.level() == mc.level) return cached;

        EntityType<?> type = EntityType.byString(id.toString()).orElse(null);
        if (type == null) return null;
        Entity e = type.create(mc.level);
        if (e == null) return null;

        e.setYRot(0f);
        e.setXRot(0f);
        e.setYHeadRot(0f);
        e.setYBodyRot(0f);

        ENTITY_CACHE.put(id, e);
        return e;
    }


    private static void vtx(com.mojang.blaze3d.vertex.VertexConsumer vc, org.joml.Matrix4f pose,
                            float x, float y, float z, float u, float v,
                            float r, float g, float b, float a,
                            int light, int overlay,
                            float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }

    /**
     * What the rite will actually cost here, and what the altar is doing to it.
     *
     * <p>The orbit showed the recipe's raw numbers, so an altar with three familiars serving
     * it looked exactly like a bare one — the discount was real but invisible unless you were
     * wearing the goggles. This reads from the block entity's synced boon state, so binding or
     * banishing a familiar changes it on the next altar update.</p>
     */
    private void renderBoons(AltarBlockEntity be, RitualRecipe recipe, PoseStack ps, MultiBufferSource buf) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int base = recipe.fluxiaRequired().orElse(0);
        int cost = be.effectiveCost(base);
        float eff = be.boonEfficiency();
        float yield = be.boonYield();

        // Cost first, tinted when something is paying part of it.
        if (base > 0) {
            String text = String.format(java.util.Locale.ROOT, "%,d", cost) + " \u0192";
            if (cost < base) text += "  (was " + String.format(java.util.Locale.ROOT, "%,d", base) + ")";
            billboardLabel(mc, ps, buf, text, BOON_Y, cost < base ? DISCOUNT_RGB : 0xDCCBFF);
        }

        // Then what the runes and familiars are contributing, if anything. Efficiency buys
        // reagents as well as Fluxia now, so say so rather than leaving "cost" to be read
        // as the reserve alone.
        if (eff > 0f || yield > 0f) {
            StringBuilder sb = new StringBuilder();
            if (eff > 0f) {
                sb.append("-").append(Math.round(eff * 100))
                        .append(recipe.inputs().isEmpty() ? "% cost" : "% cost & reagents");
            }
            if (yield > 0f) {
                if (sb.length() > 0) sb.append("   ");
                sb.append("+").append(Math.round(yield * 100)).append("% bonus");
            }
            billboardLabel(mc, ps, buf, sb.toString(), BOON_Y - 0.20, DISCOUNT_RGB);
        }

        int serving = be.familiarsInService();
        if (be.familiarsBound() > 0) {
            String text = serving + " / " + be.familiarSlots() + " familiars";
            billboardLabel(mc, ps, buf, text, BOON_Y - 0.38,
                    be.familiarsBound() > be.familiarSlots() ? 0xFF8A7A : 0x9E8CC8);
        }
    }

    /** Small centred text above the altar, turned to face the camera. */
    private void billboardLabel(Minecraft mc, PoseStack ps, MultiBufferSource buf,
                                String text, double y, int rgb) {
        var font = mc.font;
        ps.pushPose();
        ps.translate(0.5, y, 0.5);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        // The font draws +Y down and the camera quaternion mirrors X, so negate both.
        ps.scale(-LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

        float width = font.width(text) / 2.0f;
        font.drawInBatch(text, -width, 0.0f, rgb, false, ps.last().pose(), buf,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        ps.popPose();
    }

    /** Height of the cost readout, above the orbiting reagents. */
    private static final double BOON_Y = 2.35;
    private static final float LABEL_SCALE = 0.022f;
    /** Green, used everywhere the altar is showing a price it has brought down. */
    private static final int DISCOUNT_RGB = 0x7CFFA8;
}
