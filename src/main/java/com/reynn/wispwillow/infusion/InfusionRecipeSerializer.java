package com.reynn.wispwillow.infusion;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.List;

public class InfusionRecipeSerializer implements RecipeSerializer<InfusionRecipe> {
    public static final InfusionRecipeSerializer INSTANCE = new InfusionRecipeSerializer();

    @Override
    public MapCodec<InfusionRecipe> codec() {
        return InfusionRecipe.MAP_CODEC;
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, InfusionRecipe> NET =
            new StreamCodec<>() {
                @Override
                public InfusionRecipe decode(RegistryFriendlyByteBuf buf) {
                    var id = buf.readResourceLocation();
                    int n = buf.readVarInt();
                    List<Ingredient> inputs = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) {
                        inputs.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                    }
                    ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                    int fluxia = buf.readVarInt();
                    return new InfusionRecipe(id, inputs, result, fluxia);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, InfusionRecipe r) {
                    buf.writeResourceLocation(r.getId());
                    buf.writeVarInt(r.inputs().size());
                    for (Ingredient ing : r.inputs()) {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
                    }
                    ItemStack.STREAM_CODEC.encode(buf, r.result());
                    buf.writeVarInt(r.fluxia());
                }
            };

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, InfusionRecipe> streamCodec() {
        return NET;
    }
}
