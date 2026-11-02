package com.reynn.wispwillow.items;

import com.reynn.wispwillow.client.renderers.WWGeoArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * A piece of armour drawn in 3D by GeckoLib when worn: Empyrite plate or Glimmerweave cloth.
 *
 * <p>The item itself is an ordinary {@link ArmorItem} (material, slot and durability are as
 * before); this class adds the worn renderer and an animation. All four pieces of a set share
 * one model, {@code geo/armor/<set>.geo.json}, whose bones follow GeckoLib's humanoid template
 * ({@code armorHead}, {@code armorBody}, the arms, legs and boots): the renderer shows the bones
 * that belong to the slot being drawn and moves each with the matching part of the wearer's own
 * model. The texture is {@code textures/armor/<set>.png} with a {@code _glowmask} beside it for
 * the parts that burn in the dark; a set that {@link #awakenable awakens} also has
 * {@code <set>_awakened.png}. The flat {@code textures/models/armor/<set>_layer_*.png} sheets
 * are only ever seen if GeckoLib is bypassed.</p>
 *
 * <p>Both the geometry and the sheets come from {@code tools/gen_armor_model.py} and
 * {@code tools/gen_cloth_model.py}; edit the designs there, not here.</p>
 */
public class WWGeoArmorItem extends ArmorItem implements GeoItem {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final String set;
    private final boolean awakenable;
    private final RawAnimation idle;
    private final RawAnimation awakened;

    /**
     * @param set         model/texture name under {@code armor/}
     * @param awakenable  true for Empyrite: dormant plate holds still and shows the plain sheet,
     *                    awakened plate plays {@code animation.<set>.awakened} on the awakened sheet.
     *                    False for a set that always plays {@code animation.<set>.idle}.
     */
    public WWGeoArmorItem(Holder<ArmorMaterial> material, Type type, Properties props,
                          String set, boolean awakenable) {
        super(material, type, props);
        this.set = set;
        this.awakenable = awakenable;
        this.idle = RawAnimation.begin().thenLoop("animation." + set + ".idle");
        this.awakened = RawAnimation.begin().thenLoop("animation." + set + ".awakened");
    }

    public String set() {
        return set;
    }

    public boolean awakenable() {
        return awakenable;
    }

    /**
     * Dormant plate holds still; awakened plate breathes; cloth is never quite still. The armour
     * renderer hands the controller the very stack it is drawing, so each piece decides for itself.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 10, state -> {
            if (!awakenable) return state.setAndContinue(idle);
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            if (stack != null && Awakening.isAwakened(stack)) {
                return state.setAndContinue(awakened);
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /**
     * Hands GeckoLib the armour renderer. Only the anonymous class names client classes; GeckoLib
     * calls this from the client bootstrap alone, so a dedicated server never loads them.
     */
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private WWGeoArmorRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack,
                                                                                @Nullable EquipmentSlot equipmentSlot,
                                                                                @Nullable HumanoidModel<T> original) {
                if (this.renderer == null) {
                    this.renderer = new WWGeoArmorRenderer(WWGeoArmorItem.this);
                }
                return this.renderer;
            }
        });
    }
}
