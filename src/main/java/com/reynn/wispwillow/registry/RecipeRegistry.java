package com.reynn.wispwillow.registry;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.alchemy.AlchemyRecipe;
import com.reynn.wispwillow.alchemy.AlchemyRecipeSerializer;
import com.reynn.wispwillow.gateway.GatewayRecipe;
import com.reynn.wispwillow.gateway.GatewayRecipeSerializer;
import com.reynn.wispwillow.infusion.InfusionRecipe;
import com.reynn.wispwillow.infusion.InfusionRecipeSerializer;
import com.reynn.wispwillow.ritual.RitualRecipe;
import com.reynn.wispwillow.ritual.RitualRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RecipeRegistry {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, WispWillow.MODID);

    /**
     * Recipe <em>types</em> need their own registry. Ritual and infusion were building
     * theirs with a bare {@code RecipeType.simple(...)} held in a static field, which
     * happens to work for {@code getRecipeFor} — the manager buckets on object identity —
     * but leaves the type absent from {@code Registries.RECIPE_TYPE}, where recipe-book
     * and third-party lookups go looking for it. The two existing types are registered
     * here by handing over the very same instance, so nothing about their behaviour
     * changes; alchemy gets its type from the registry directly.
     */
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, WispWillow.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RitualRecipeSerializer> RITUAL_SERIALIZER =
            SERIALIZERS.register("ritual", () -> RitualRecipeSerializer.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, InfusionRecipeSerializer> INFUSION_SERIALIZER =
            SERIALIZERS.register("infusion", () -> InfusionRecipeSerializer.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, AlchemyRecipeSerializer> ALCHEMY_SERIALIZER =
            SERIALIZERS.register("alchemy", () -> AlchemyRecipeSerializer.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GatewayRecipe>> GATEWAY_SERIALIZER =
            SERIALIZERS.register("gateway", () -> GatewayRecipeSerializer.INSTANCE);

    public static final DeferredHolder<RecipeType<?>, RecipeType<RitualRecipe>> RITUAL_TYPE =
            TYPES.register("ritual", () -> RitualRecipe.TYPE);

    public static final DeferredHolder<RecipeType<?>, RecipeType<InfusionRecipe>> INFUSION_TYPE =
            TYPES.register("infusion", () -> InfusionRecipe.TYPE);

    public static final DeferredHolder<RecipeType<?>, RecipeType<GatewayRecipe>> GATEWAY_TYPE =
            TYPES.register("gateway", () -> GatewayRecipe.TYPE);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AlchemyRecipe>> ALCHEMY_TYPE =
            TYPES.register("alchemy", () -> RecipeType.simple(
                    ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "alchemy")));

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        SERIALIZERS.register(bus);
        TYPES.register(bus);
    }
}
