package com.reynn.wispwillow.gateway;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class GatewayRecipeSerializer implements RecipeSerializer<GatewayRecipe> {
    public static final GatewayRecipeSerializer INSTANCE = new GatewayRecipeSerializer();

    private static final StreamCodec<RegistryFriendlyByteBuf, GatewayRecipe> NET = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, GatewayRecipe::getId,
            ResourceLocation.STREAM_CODEC, GatewayRecipe::soul,
            ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ENTITY_TYPE)), GatewayRecipe::summons,
            ByteBufCodecs.VAR_INT, GatewayRecipe::count,
            ItemStack.LIST_STREAM_CODEC, GatewayRecipe::results,
            ByteBufCodecs.VAR_INT, GatewayRecipe::fluxia,
            (id, soul, summons, count, results, fluxia) ->
                    new GatewayRecipe(id, soul, summons.orElse(null), count, results, fluxia));

    @Override
    public MapCodec<GatewayRecipe> codec() {
        return GatewayRecipe.MAP_CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, GatewayRecipe> streamCodec() {
        return NET;
    }
}
