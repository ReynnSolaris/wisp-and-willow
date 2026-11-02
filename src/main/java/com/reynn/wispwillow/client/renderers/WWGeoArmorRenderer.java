package com.reynn.wispwillow.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.client.AwakenedGlint;
import com.reynn.wispwillow.items.Awakening;
import com.reynn.wispwillow.items.WWGeoArmorItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import javax.annotation.Nullable;

/**
 * The worn renderer for {@link WWGeoArmorItem}: GeckoLib's armour renderer plus three things it
 * does not do on its own.
 *
 * <p><b>Slim arms.</b> GeckoLib moves each armour bone with the wearer's model part but never
 * resizes it, so a set modelled for the four-wide "wide" arm hangs a unit proud of a three-wide
 * "slim" one on the outside and clips it on the inside. For a slim player the two arm bones are
 * drawn at three quarters across and nudged toward the body, so the sleeve or manica hugs the
 * thinner arm with the same stand-off.</p>
 *
 * <p><b>Awakened plate.</b> An awakenable set has a second sheet, {@code <set>_awakened.png},
 * chosen per piece from the stack in the slot being drawn. The glow layer asks this renderer for
 * its texture, so the awakened sheet's own {@code _glowmask} lights its filigree.</p>
 *
 * <p><b>The gold glint.</b> GeckoLib draws its armour in place of vanilla's
 * {@code renderArmorPiece}, which is where the mod normally announces the piece being drawn, so
 * the renderer announces it itself and an awakened piece glints gold on the wearer too.</p>
 */
public class WWGeoArmorRenderer extends GeoArmorRenderer<WWGeoArmorItem> {

    /** How much narrower a slim arm is than a wide one: 3 px against 4. */
    private static final float SLIM_ARM_SCALE = 0.75f;
    /** Scaling about the pivot (the inner edge) leaves the sleeve a quarter unit proud; this recentres it. Model units. */
    private static final float SLIM_ARM_SHIFT = 0.25f;

    private final boolean awakenable;
    private final ResourceLocation awakenedTexture;

    public WWGeoArmorRenderer(WWGeoArmorItem item) {
        super(new DefaultedGeoModel<WWGeoArmorItem>(ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, item.set())) {
            // One shared model per set ("armor/<set>"), not GeckoLib's default of one per item id.
            @Override
            protected String subtype() {
                return "armor";
            }
        });
        this.awakenable = item.awakenable();
        this.awakenedTexture = ResourceLocation.fromNamespaceAndPath(WispWillow.MODID,
                "textures/armor/" + item.set() + "_awakened.png");
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /** The awakened sheet for a piece that has been through the rite; see the class comment. */
    @Override
    public ResourceLocation getTextureLocation(WWGeoArmorItem animatable) {
        if (this.awakenable && this.currentStack != null && Awakening.isAwakened(this.currentStack)) {
            return this.awakenedTexture;
        }
        return super.getTextureLocation(animatable);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, @Nullable VertexConsumer buffer, int packedLight,
                               int packedOverlay, int colour) {
        AwakenedGlint.push(this.currentStack == null ? ItemStack.EMPTY : this.currentStack);
        try {
            super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, colour);
        } finally {
            AwakenedGlint.pop();
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, WWGeoArmorItem animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, int colour) {
        boolean right = bone == this.rightArm;
        if ((right || bone == this.leftArm) && isSlimChest()) {
            // Applied here, after the animation pass, and put back afterwards: the baked model is
            // shared by every wearer, and the next one may well have wide arms.
            float scaleX = bone.getScaleX();
            float posX = bone.getPosX();
            // Right is -X in the model, so the body is toward +X for the right arm and -X for the left.
            bone.setScaleX(SLIM_ARM_SCALE);
            bone.setPosX(posX + (right ? SLIM_ARM_SHIFT : -SLIM_ARM_SHIFT));
            try {
                super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                        partialTick, packedLight, packedOverlay, colour);
            } finally {
                bone.setScaleX(scaleX);
                bone.setPosX(posX);
            }
            return;
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }

    /** The arms are only drawn with the chest piece, and only a slim player needs them narrowed. */
    private boolean isSlimChest() {
        return this.currentSlot == EquipmentSlot.CHEST
                && this.currentEntity instanceof AbstractClientPlayer player
                && player.getSkin().model() == PlayerSkin.Model.SLIM;
    }
}
