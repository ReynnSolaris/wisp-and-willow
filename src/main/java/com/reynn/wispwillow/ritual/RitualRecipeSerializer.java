package com.reynn.wispwillow.ritual;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.awakening.Imbuement;
import com.reynn.wispwillow.blocks.SigilType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RitualRecipeSerializer implements RecipeSerializer<RitualRecipe> {
    public static final RitualRecipeSerializer INSTANCE = new RitualRecipeSerializer();

    // JSON codec
    @Override
    public MapCodec<RitualRecipe> codec() {
        return RitualRecipe.MAP_CODEC;
    }

    // ---------- Network codecs ----------

    private static final StreamCodec<RegistryFriendlyByteBuf, RitualRecipe.Grid> GRID_STREAM =
            new StreamCodec<>() {
                @Override public RitualRecipe.Grid decode(RegistryFriendlyByteBuf buf) {
                    return RitualRecipe.Grid.values()[buf.readVarInt()];
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, RitualRecipe.Grid value) {
                    buf.writeVarInt(value.ordinal());
                }
            };

    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<SigilType>> OPT_SIGIL_TYPE_STREAM =
            new StreamCodec<>() {
                @Override public Optional<SigilType> decode(RegistryFriendlyByteBuf buf) {
                    if (!buf.readBoolean()) return Optional.empty();
                    return Optional.of(SigilType.values()[buf.readVarInt()]);
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, Optional<SigilType> val) {
                    buf.writeBoolean(val.isPresent());
                    if (val.isPresent()) buf.writeVarInt(val.get().ordinal());
                }
            };

    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<DyeColor>> OPT_DYE_STREAM =
            new StreamCodec<>() {
                @Override public Optional<DyeColor> decode(RegistryFriendlyByteBuf buf) {
                    if (!buf.readBoolean()) return Optional.empty();
                    return Optional.of(DyeColor.values()[buf.readVarInt()]);
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, Optional<DyeColor> val) {
                    buf.writeBoolean(val.isPresent());
                    if (val.isPresent()) buf.writeVarInt(val.get().ordinal());
                }
            };

    private static final StreamCodec<RegistryFriendlyByteBuf, RitualRecipe.Cell> CELL_STREAM =
            new StreamCodec<>() {
                @Override public RitualRecipe.Cell decode(RegistryFriendlyByteBuf buf) {
                    int dx = buf.readVarInt();
                    int dz = buf.readVarInt();
                    var type = OPT_SIGIL_TYPE_STREAM.decode(buf);
                    var col  = OPT_DYE_STREAM.decode(buf);
                    boolean req = buf.readBoolean();
                    return new RitualRecipe.Cell(dx, dz, type, col, req);
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, RitualRecipe.Cell c) {
                    buf.writeVarInt(c.dx());
                    buf.writeVarInt(c.dz());
                    OPT_SIGIL_TYPE_STREAM.encode(buf, c.type());
                    OPT_DYE_STREAM.encode(buf, c.color());
                    buf.writeBoolean(c.required());
                }
            };

    private static <T> List<T> readList(RegistryFriendlyByteBuf buf, StreamCodec<RegistryFriendlyByteBuf, T> c) {
        int n = buf.readVarInt();
        List<T> out = new ArrayList<>(n);
        for (int i=0;i<n;i++) out.add(c.decode(buf));
        return out;
    }
    private static <T> void writeList(RegistryFriendlyByteBuf buf, List<T> list, StreamCodec<RegistryFriendlyByteBuf, T> c) {
        buf.writeVarInt(list.size());
        for (T t : list) c.encode(buf, t);
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, List<RitualRecipe.Cell>> CELL_LIST_STREAM =
            new StreamCodec<>() {
                @Override public List<RitualRecipe.Cell> decode(RegistryFriendlyByteBuf buf) {
                    return readList(buf, CELL_STREAM);
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, List<RitualRecipe.Cell> value) {
                    writeList(buf, value, CELL_STREAM);
                }
            };

    // Ingredient & ItemStack already provide registry-aware codecs in 1.21.1
    private static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> INGREDIENT_STREAM = Ingredient.CONTENTS_STREAM_CODEC;

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Ingredient>> INGREDIENT_LIST_STREAM =
            new StreamCodec<>() {
                @Override public List<Ingredient> decode(RegistryFriendlyByteBuf buf) {
                    return readList(buf, INGREDIENT_STREAM);
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, List<Ingredient> value) {
                    writeList(buf, value, INGREDIENT_STREAM);
                }
            };

    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<ItemStack>> OPT_STACK_STREAM =
            new StreamCodec<>() {
                @Override public Optional<ItemStack> decode(RegistryFriendlyByteBuf buf) {
                    if (!buf.readBoolean()) return Optional.empty();
                    return Optional.of(ItemStack.STREAM_CODEC.decode(buf));
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, Optional<ItemStack> val) {
                    buf.writeBoolean(val.isPresent());
                    if (val.isPresent()) ItemStack.STREAM_CODEC.encode(buf, val.get());
                }
            };

    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<Integer>> OPT_FLUXIA_STREAM =
            new StreamCodec<>() {
                @Override public Optional<Integer> decode(RegistryFriendlyByteBuf buf) {
                    if (!buf.readBoolean()) return Optional.empty();
                    return Optional.of(buf.readVarInt());
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, Optional<Integer> val) {
                    buf.writeBoolean(val.isPresent());
                    if (val.isPresent()) buf.writeVarInt(val.get());
                }
            };
    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<ResourceLocation>> OPT_RL_STREAM =
            new StreamCodec<>() {
                @Override public Optional<ResourceLocation> decode(RegistryFriendlyByteBuf buf) {
                    if (!buf.readBoolean()) return Optional.empty();
                    return Optional.of(buf.readResourceLocation());
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, Optional<ResourceLocation> val) {
                    buf.writeBoolean(val.isPresent());
                    if (val.isPresent()) buf.writeResourceLocation(val.get());
                }
            };

    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<TagKey<Item>>> OPT_ITEM_TAG_STREAM =
            new StreamCodec<>() {
                @Override public Optional<TagKey<Item>> decode(RegistryFriendlyByteBuf buf) {
                    if (!buf.readBoolean()) return Optional.empty();
                    return Optional.of(TagKey.create(Registries.ITEM, buf.readResourceLocation()));
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, Optional<TagKey<Item>> val) {
                    buf.writeBoolean(val.isPresent());
                    if (val.isPresent()) buf.writeResourceLocation(val.get().location());
                }
            };

    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<Vigil>> OPT_VIGIL_STREAM =
            new StreamCodec<>() {
                @Override public Optional<Vigil> decode(RegistryFriendlyByteBuf buf) {
                    if (!buf.readBoolean()) return Optional.empty();
                    Vigil.Effect effect = Vigil.Effect.values()[buf.readVarInt()];
                    return Optional.of(new Vigil(effect, buf.readVarInt(), buf.readVarInt()));
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, Optional<Vigil> val) {
                    buf.writeBoolean(val.isPresent());
                    if (val.isPresent()) {
                        Vigil v = val.get();
                        buf.writeVarInt(v.effect().ordinal());
                        buf.writeVarInt(v.durationTicks());
                        buf.writeVarInt(v.upkeepPerSecond());
                    }
                }
            };

    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<Imbuement>> OPT_IMBUEMENT_STREAM =
            ByteBufCodecs.optional(Imbuement.STREAM_CODEC).cast();

    private static final StreamCodec<RegistryFriendlyByteBuf, RitualRecipe> NET_CODEC =
            new StreamCodec<>() {
                @Override public RitualRecipe decode(RegistryFriendlyByteBuf buf) {
                    ResourceLocation id = buf.readResourceLocation();
                    var grid           = GRID_STREAM.decode(buf);
                    var pattern        = CELL_LIST_STREAM.decode(buf);
                    var inputs         = INGREDIENT_LIST_STREAM.decode(buf);
                    var resultItem     = OPT_STACK_STREAM.decode(buf);
                    var resultMob      = OPT_RL_STREAM.decode(buf);
                    int mobCount       = buf.readVarInt();
                    Optional<Integer> fluxiaRequired = OPT_FLUXIA_STREAM.decode(buf);
                    int minTier = buf.readVarInt();
                    Optional<Vigil> vigil = OPT_VIGIL_STREAM.decode(buf);
                    Optional<TagKey<Item>> awakens = OPT_ITEM_TAG_STREAM.decode(buf);
                    Optional<Imbuement> imbues = OPT_IMBUEMENT_STREAM.decode(buf);
                    return new RitualRecipe(id, grid, pattern, inputs, resultItem, resultMob, mobCount, fluxiaRequired, minTier, vigil, awakens, imbues);
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, RitualRecipe r) {
                    buf.writeResourceLocation(r.getId());
                    GRID_STREAM.encode(buf, r.grid());
                    CELL_LIST_STREAM.encode(buf, r.pattern());
                    INGREDIENT_LIST_STREAM.encode(buf, r.inputs());
                    OPT_STACK_STREAM.encode(buf, r.resultItem());
                    OPT_RL_STREAM.encode(buf, r.resultMobId());
                    buf.writeVarInt(r.resultMobCount());
                    OPT_FLUXIA_STREAM.encode(buf, r.fluxiaRequired());
                    buf.writeVarInt(r.minTier());
                    OPT_VIGIL_STREAM.encode(buf, r.vigil());
                    OPT_ITEM_TAG_STREAM.encode(buf, r.awakens());
                    OPT_IMBUEMENT_STREAM.encode(buf, r.imbues());
                }
            };

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, RitualRecipe> streamCodec() {
        return NET_CODEC;
    }
}