package com.reynn.wispwillow.alchemy;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class AlchemyRecipeSerializer implements RecipeSerializer<AlchemyRecipe> {

    public static final AlchemyRecipeSerializer INSTANCE = new AlchemyRecipeSerializer();

    private static final MapCodec<AlchemyRecipe> CODEC =
            RecordCodecBuilder.mapCodec(inst -> inst.group(
                    SizedIngredient.FLAT_CODEC.listOf()
                            .optionalFieldOf("ingredients", java.util.List.of()).forGetter(AlchemyRecipe::ingredients),
                    FluidStack.OPTIONAL_CODEC
                            .optionalFieldOf("fluid_input", FluidStack.EMPTY).forGetter(AlchemyRecipe::fluidInput),
                    com.mojang.serialization.Codec.INT
                            .optionalFieldOf("fluxia", 0).forGetter(AlchemyRecipe::fluxia),
                    com.mojang.serialization.Codec.INT
                            .optionalFieldOf("ticks", 100).forGetter(AlchemyRecipe::ticks),
                    ItemStack.OPTIONAL_CODEC
                            .optionalFieldOf("result", ItemStack.EMPTY).forGetter(AlchemyRecipe::result),
                    FluidStack.OPTIONAL_CODEC
                            .optionalFieldOf("fluid_result", FluidStack.EMPTY).forGetter(AlchemyRecipe::fluidResult)
            ).apply(inst, AlchemyRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, AlchemyRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), AlchemyRecipe::ingredients,
                    FluidStack.OPTIONAL_STREAM_CODEC, AlchemyRecipe::fluidInput,
                    ByteBufCodecs.VAR_INT, AlchemyRecipe::fluxia,
                    ByteBufCodecs.VAR_INT, AlchemyRecipe::ticks,
                    ItemStack.OPTIONAL_STREAM_CODEC, AlchemyRecipe::result,
                    FluidStack.OPTIONAL_STREAM_CODEC, AlchemyRecipe::fluidResult,
                    AlchemyRecipe::new);

    @Override public MapCodec<AlchemyRecipe> codec() { return CODEC; }
    @Override public StreamCodec<RegistryFriendlyByteBuf, AlchemyRecipe> streamCodec() { return STREAM_CODEC; }
}
