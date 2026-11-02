package com.reynn.wispwillow;

import com.reynn.wispwillow.client.*;
import com.reynn.wispwillow.client.renderers.AlchemicalCauldronRenderer;
import com.reynn.wispwillow.client.renderers.AltarBER;
import com.reynn.wispwillow.client.renderers.FluxiaSpitterBER;
import com.reynn.wispwillow.client.renderers.GatewayPortalRenderer;
import com.reynn.wispwillow.client.renderers.ManaPoolBER;
import com.reynn.wispwillow.client.screen.AlchemicalCauldronScreen;
import com.reynn.wispwillow.compat.ponder.WWPonder;
import com.reynn.wispwillow.entity.client.AxolotlFamiliarRenderer;
import com.reynn.wispwillow.entity.client.BatFamiliarRenderer;
import com.reynn.wispwillow.entity.client.RavenFamiliarRenderer;
import com.reynn.wispwillow.entity.client.FenHeronRenderer;
import com.reynn.wispwillow.entity.client.GlimmerfawnRenderer;
import com.reynn.wispwillow.entity.client.WillowHoundRenderer;
import com.reynn.wispwillow.entity.client.FrogFamiliarRenderer;
import com.reynn.wispwillow.entity.client.WispFamiliarRenderer;
import com.reynn.wispwillow.entity.client.WispflyRenderer;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.joml.Vector3f;
import com.reynn.wispwillow.particles.FluxSpark;
import com.reynn.wispwillow.registry.*;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = WispWillow.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = WispWillow.MODID, value = Dist.CLIENT)
public class WispWillowClient {
    public WispWillowClient(ModContainer container) {
        // The client half of phasing: the drift and the fog inside stone.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(PhasingClient.class);

        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // Ponder is optional. Every Ponder type lives behind WWPonder, so without Ponder
        // installed this check is the only thing that runs. Ponder collects its plugins at
        // load-complete, so registering from the constructor is early enough.
        if (ModList.get().isLoaded("ponder")) {
            WWPonder.register();
        }
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        BlockEntityRenderers.register(
                BlockEntityRegistry.ALTAR.get(),
                AltarBER::new
        );

        EntityRenderers.register(EntityRegistry.AXOLOTL_FAMILIAR.get(), AxolotlFamiliarRenderer::new);
        EntityRenderers.register(EntityRegistry.OVERLAY_HOST.get(),
                com.reynn.wispwillow.client.goggles.OverlayHostRenderer::new);
        EntityRenderers.register(EntityRegistry.WISP_FAMILIAR.get(), WispFamiliarRenderer::new);
        EntityRenderers.register(EntityRegistry.FROG_FAMILIAR.get(), FrogFamiliarRenderer::new);
        EntityRenderers.register(EntityRegistry.BAT_FAMILIAR.get(), BatFamiliarRenderer::new);
        EntityRenderers.register(EntityRegistry.RAVEN_FAMILIAR.get(), RavenFamiliarRenderer::new);
        EntityRenderers.register(EntityRegistry.WISPFLY.get(), WispflyRenderer::new);
        EntityRenderers.register(EntityRegistry.FEN_HERON.get(), FenHeronRenderer::new);
        EntityRenderers.register(EntityRegistry.WILLOW_HOUND.get(), WillowHoundRenderer::new);
        EntityRenderers.register(EntityRegistry.GLIMMERFAWN.get(), GlimmerfawnRenderer::new);

        BlockEntityRenderers.register(
                BlockEntityRegistry.MANA_POOL.get(), ManaPoolBER::new
        );

        BlockEntityRenderers.register(
                BlockEntityRegistry.FLUXIA_SPITTER.get(), FluxiaSpitterBER::new
        );

        BlockEntityRenderers.register(
                BlockEntityRegistry.GATEWAY_PORTAL.get(), GatewayPortalRenderer::new
        );

        BlockEntityRenderers.register(
                BlockEntityRegistry.SPIRIT_PORTAL.get(),
                com.reynn.wispwillow.client.renderers.SpiritPortalRenderer::new
        );

        BlockEntityRenderers.register(
                BlockEntityRegistry.ALCHEMICAL_CAULDRON.get(), AlchemicalCauldronRenderer::new
        );

        ItemBlockRenderTypes.setRenderLayer(FluidRegistry.FLUXIA_FLUID.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidRegistry.FLOWING_FLUXIA_FLUID.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidRegistry.BOG_WATER.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidRegistry.BOG_WATER_FLOWING.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidRegistry.WILLOW_SAP.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidRegistry.WILLOW_SAP_FLOWING.get(), RenderType.translucent());

        // The basin is hollow, so its inner faces need a cutout layer or the pot renders solid.
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.ALCHEMICAL_CAULDRON.get(), RenderType.cutout());


    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(ParticleRegistry.FLUX_SPARK.get(), FluxSpark.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MainRegistry.ALCHEMICAL_CAULDRON_MENU.get(), AlchemicalCauldronScreen::new);

    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
                new IClientFluidTypeExtensions() {

                    final ResourceLocation FLUXIA_STILL = ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "block/fluxia_fluid_still");
                    final ResourceLocation FLUXIA_FLOW = ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "block/fluxia_fluid_flow");

                    @Override
                    public ResourceLocation getStillTexture() {
                        return FLUXIA_STILL;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return FLUXIA_FLOW;
                    }

                },
                FluidRegistry.FLUXIA_FLUID_TYPE
        );

        // Murky and close under bog water; under sap you can barely see your hand.
        event.registerFluidType(fenFluid("bog_water", 0x2F3B22, 6.0F), FluidRegistry.BOG_WATER_TYPE);
        event.registerFluidType(fenFluid("willow_sap", 0x9C5A15, 2.5F), FluidRegistry.WILLOW_SAP_TYPE);
    }

    /** Sprites named {@code <name>_still} and {@code <name>_flow}, and a short coloured fog. */
    private static IClientFluidTypeExtensions fenFluid(String name, int fogRgb, float fogEnd) {
        ResourceLocation still = ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "block/" + name + "_still");
        ResourceLocation flow = ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "block/" + name + "_flow");
        float r = ((fogRgb >> 16) & 0xFF) / 255.0F;
        float g = ((fogRgb >> 8) & 0xFF) / 255.0F;
        float b = (fogRgb & 0xFF) / 255.0F;

        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flow;
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                           float darkenWorldAmount, Vector3f fluidFogColor) {
                return new Vector3f(r, g, b);
            }

            @Override
            public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick,
                                        float nearDistance, float farDistance, FogShape shape) {
                RenderSystem.setShaderFogStart(0.0F);
                RenderSystem.setShaderFogEnd(fogEnd);
            }
        };
    }

}
