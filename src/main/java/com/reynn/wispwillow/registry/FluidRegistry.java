package com.reynn.wispwillow.registry;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.fluid.FluxiaFluid;
import com.reynn.wispwillow.fluid.fluidtypes.FluxiaFluidType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class FluidRegistry {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, WispWillow.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, WispWillow.MODID);

    public static final DeferredHolder<Fluid, FluxiaFluid.Flowing> FLOWING_FLUXIA_FLUID = FLUIDS.register(
            "fluxia_fluid_flowing",
            FluxiaFluid.Flowing::new
    );
    public static final DeferredHolder<Fluid, FluxiaFluid.Source> FLUXIA_FLUID = FLUIDS.register(
            "fluxia_fluid",
            FluxiaFluid.Source::new
    );

    public static final DeferredHolder<FluidType, FluxiaFluidType> FLUXIA_FLUID_TYPE = FLUID_TYPES.register(
            "fluxia_fluid",
            FluxiaFluidType::new
    );

    // ---- Bog Water: the Willow Fen's still, murky pools ----

    public static final DeferredHolder<FluidType, FluidType> BOG_WATER_TYPE = FLUID_TYPES.register("bog_water",
            () -> new FluidType(FluidType.Properties.create()
                    // Thicker than water: you wade rather than swim.
                    .motionScale(0.009)
                    .canSwim(true)
                    .canDrown(true)
                    .canExtinguish(true)
                    .canConvertToSource(true)
                    .supportsBoating(true)
                    .canHydrate(true)
                    .fallDistanceModifier(0F)
                    .density(1100)
                    .viscosity(1600)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> BOG_WATER =
            FLUIDS.register("bog_water", () -> new BaseFlowingFluid.Source(bogWaterProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> BOG_WATER_FLOWING =
            FLUIDS.register("bog_water_flowing", () -> new BaseFlowingFluid.Flowing(bogWaterProperties()));

    private static BaseFlowingFluid.Properties bogWaterProperties() {
        return new BaseFlowingFluid.Properties(BOG_WATER_TYPE, BOG_WATER, BOG_WATER_FLOWING)
                .bucket(ItemRegistry.BOG_WATER_BUCKET)
                .block(BlockRegistry.BOG_WATER_BLOCK)
                .slopeFindDistance(3)
                .levelDecreasePerBlock(2)
                .tickRate(10)
                .explosionResistance(100F);
    }

    // ---- Willow Sap: tapped from willow logs, slow and sticky ----

    public static final DeferredHolder<FluidType, FluidType> WILLOW_SAP_TYPE = FLUID_TYPES.register("willow_sap",
            () -> new FluidType(FluidType.Properties.create()
                    // Sap holds you. There is no swimming in it and very little walking.
                    .motionScale(0.0035)
                    .canSwim(false)
                    .canDrown(true)
                    .canPushEntity(false)
                    .canExtinguish(true)
                    .canConvertToSource(false)
                    .supportsBoating(false)
                    .canHydrate(false)
                    .fallDistanceModifier(0.2F)
                    .lightLevel(3)
                    .density(1400)
                    .viscosity(8000)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> WILLOW_SAP =
            FLUIDS.register("willow_sap", () -> new BaseFlowingFluid.Source(willowSapProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> WILLOW_SAP_FLOWING =
            FLUIDS.register("willow_sap_flowing", () -> new BaseFlowingFluid.Flowing(willowSapProperties()));

    private static BaseFlowingFluid.Properties willowSapProperties() {
        return new BaseFlowingFluid.Properties(WILLOW_SAP_TYPE, WILLOW_SAP, WILLOW_SAP_FLOWING)
                .bucket(ItemRegistry.WILLOW_SAP_BUCKET)
                .block(BlockRegistry.WILLOW_SAP_BLOCK)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(30)
                .explosionResistance(100F);
    }

    public static void register(IEventBus bus) {
        FLUIDS.register(bus);
        FLUID_TYPES.register(bus);
    }
}
