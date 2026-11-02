package com.reynn.wispwillow.fluid.fluidtypes;

import com.reynn.wispwillow.WispWillow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector3f;

public class FluxiaFluidType extends FluidType {

    // Textures: make sure the PNGs exist at these paths
    public static final ResourceLocation STILL  =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "block/fluxia_fluid_still");
    private static final ResourceLocation FLOWING =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "block/fluxia_fluid_flow");
    // Optional overlay (underwater / in-fluid effect) – you can replace with your own later
    private static final ResourceLocation OVERLAY =
            ResourceLocation.withDefaultNamespace("misc/underwater");

    public static final int TINT = 0xA03FDFFF; // semi-transparent cyan-ish
    private static final Vector3f FOG_COLOR = new Vector3f(
            0x3F / 255f, 0xDF / 255f, 0xFF / 255f
    );

    public FluxiaFluidType() {
        super(Properties.create()
                // Taken from net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE
                .fallDistanceModifier(0F)
                .canExtinguish(true)
                .canConvertToSource(true)
                .supportsBoating(true)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                .canHydrate(true)
        );
    }
}
