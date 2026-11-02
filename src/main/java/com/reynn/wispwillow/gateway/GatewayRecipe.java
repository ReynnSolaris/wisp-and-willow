package com.reynn.wispwillow.gateway;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.blocks.fluxia.GatewayCoreBlockEntity;
import com.reynn.wispwillow.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * A Soul Gateway casting: a sealed pact holding one creature's soul, thrown into an open
 * gateway, becomes something — a creature that steps out, items that fall out, or both.
 *
 * <p>Until 1.4.1 this table lived only in code ({@code SoulPacts}); since then it has been a
 * recipe type, {@code wispwillow:gateway}, so a datapack or a KubeJS script can add a bond
 * or retune one. The built-in bonds are written out at datagen as the default set.</p>
 *
 * <p>Since 2.1.0 a casting is no longer limited to familiars. {@code summons} may be any
 * entity type, {@code count} of them, and {@code results} is a list of item stacks dropped
 * in front of the core. At least one of the two must be present. {@code familiar} is still
 * read as an alias of {@code summons} so older datapacks keep working, and {@code id} may be
 * omitted (it defaults to {@code wispwillow:gateway/<soul path>}) so scripted recipes do not
 * have to invent one.</p>
 *
 * <p>Matching is by the soul's id, which is what a sealed pact carries. The pact itself asks
 * this table whether a creature can be taken at all, so adding a casting for a creature is
 * also what makes a primed pact accept it.</p>
 */
public class GatewayRecipe implements Recipe<RecipeInput> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "gateway");
    public static final RecipeType<GatewayRecipe> TYPE = RecipeType.simple(ID);

    private static final Codec<EntityType<?>> ENTITY = BuiltInRegistries.ENTITY_TYPE.byNameCodec();

    private static final MapCodec<GatewayRecipe> RAW_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("id").forGetter(r -> Optional.of(r.id)),
            ResourceLocation.CODEC.fieldOf("soul").forGetter(GatewayRecipe::soul),
            ENTITY.optionalFieldOf("summons").forGetter(GatewayRecipe::summons),
            // Pre-2.1.0 name for the same field. Read, never written.
            ENTITY.optionalFieldOf("familiar").forGetter(r -> Optional.<EntityType<?>>empty()),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(GatewayRecipe::count),
            ItemStack.CODEC.listOf().optionalFieldOf("results", List.of()).forGetter(GatewayRecipe::results),
            Codec.INT.optionalFieldOf("fluxia", GatewayCoreBlockEntity.CAST_COST).forGetter(GatewayRecipe::fluxia)
    ).apply(i, GatewayRecipe::fromFields));

    public static final MapCodec<GatewayRecipe> MAP_CODEC = RAW_CODEC.validate(r ->
            r.summons().isEmpty() && r.results().isEmpty()
                    ? DataResult.error(() -> "A gateway casting needs 'summons' (an entity type), 'results' (item stacks), or both")
                    : DataResult.success(r));

    private final ResourceLocation id;
    private final ResourceLocation soul;
    @Nullable private final EntityType<?> summons;
    private final int count;
    private final List<ItemStack> results;
    private final int fluxia;

    public GatewayRecipe(ResourceLocation id, ResourceLocation soul, @Nullable EntityType<?> summons, int count,
                         List<ItemStack> results, int fluxia) {
        this.id = id;
        this.soul = soul;
        this.summons = summons;
        this.count = Math.max(1, count);
        this.results = List.copyOf(results);
        this.fluxia = fluxia;
    }

    /** The old four-argument shape: one familiar, no items. */
    public GatewayRecipe(ResourceLocation id, ResourceLocation soul, EntityType<?> summons, int fluxia) {
        this(id, soul, summons, 1, List.of(), fluxia);
    }

    private static GatewayRecipe fromFields(Optional<ResourceLocation> id, ResourceLocation soul,
                                            Optional<EntityType<?>> summons, Optional<EntityType<?>> familiar,
                                            int count, List<ItemStack> results, int fluxia) {
        return new GatewayRecipe(id.orElseGet(() -> defaultId(soul)), soul,
                summons.or(() -> familiar).orElse(null), count, results, fluxia);
    }

    /** Where a casting without an explicit id lives: {@code wispwillow:gateway/<soul path>}. */
    public static ResourceLocation defaultId(ResourceLocation soul) {
        return ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "gateway/" + soul.getPath());
    }

    public ResourceLocation getId() { return id; }
    /** The creature whose soul the pact must hold, as its entity type id. */
    public ResourceLocation soul() { return soul; }
    /** What steps out of the gateway, if anything. Any entity type; familiars are the usual case. */
    public Optional<EntityType<?>> summons() { return Optional.ofNullable(summons); }
    /** How many of {@link #summons()} step out. */
    public int count() { return count; }
    /** What falls out of the gateway, if anything. */
    public List<ItemStack> results() { return results; }
    public int fluxia() { return fluxia; }

    /** The creature behind {@link #soul()}, for naming it. */
    @Nullable
    public EntityType<?> sourceType() {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(soul).orElse(null);
    }

    /** The recipe for a soul, or null if nothing bonds it. The list is short; a scan is fine. */
    @Nullable
    public static GatewayRecipe find(ServerLevel level, @Nullable ResourceLocation soul) {
        if (soul == null) return null;
        for (RecipeHolder<GatewayRecipe> holder : level.getRecipeManager().getAllRecipesFor(TYPE)) {
            if (holder.value().soul().equals(soul)) return holder.value();
        }
        return null;
    }

    /** Will a primed pact take this creature? Yes if any casting in the world names its soul. */
    public static boolean hasCastingFor(ServerLevel level, EntityType<?> type) {
        return find(level, BuiltInRegistries.ENTITY_TYPE.getKey(type)) != null;
    }

    // ---- Recipe plumbing; the gateway drives this, not the vanilla crafting UI ----
    @Override public boolean matches(RecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider regs) { return ItemStack.EMPTY; }
    @Override public ItemStack getResultItem(HolderLookup.Provider regs) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int w, int h) { return false; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return RecipeRegistry.GATEWAY_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return TYPE; }
}
