package com.reynn.wispwillow.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.awakening.Imbuement;
import com.reynn.wispwillow.blocks.SigilBlock;
import com.reynn.wispwillow.blocks.SigilType;
import com.reynn.wispwillow.registry.RecipeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class RitualRecipe implements Recipe<RecipeInput> {
    // IMPORTANT: In 1.21.x use the factory method, not the 2-arg constructor
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "ritual");
    public static final RecipeType<RitualRecipe> TYPE = RecipeType.simple(ID);

    /** Supported sizes */
    public enum Grid implements StringRepresentable {
        G3(1,"3x3"), G6(3,"6x6"), G9(4,"9x9"), G12(5,"12x12"), G15(6,"15x15"), G18(7,"18x18");
        public final int radius;
        private final String ser;
        Grid(int r, String s){ this.radius = r; this.ser = s; }
        @Override public String getSerializedName(){ return ser; }

        // simpler
        public static final Codec<Grid> CODEC = StringRepresentable.fromEnum(Grid::values);
    }

    /** One cell in the layout */
    public record Cell(int dx, int dz, Optional<SigilType> type, Optional<DyeColor> color, boolean required) {
        public static final MapCodec<Cell> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.fieldOf("dx").forGetter(Cell::dx),
                Codec.INT.fieldOf("dz").forGetter(Cell::dz),
                StringRepresentable.fromEnum(SigilType::values).optionalFieldOf("type").forGetter(Cell::type),
                StringRepresentable.fromEnum(DyeColor::values).optionalFieldOf("color").forGetter(Cell::color),
                Codec.BOOL.fieldOf("required").orElse(true).forGetter(Cell::required)
        ).apply(i, Cell::new));
    }

    // Use .codec() on the MapCodec when you want listOf(); also use accessors, not private fields
    public static final MapCodec<RitualRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("id", ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "ritual/unnamed")).forGetter(RitualRecipe::getId),
            Grid.CODEC.fieldOf("grid").forGetter(RitualRecipe::grid),
            Cell.CODEC.codec().listOf().fieldOf("pattern").forGetter(RitualRecipe::pattern),
            Ingredient.CODEC.listOf().fieldOf("inputs").orElse(List.of()).forGetter(RitualRecipe::inputs),
            ItemStack.CODEC.optionalFieldOf("result_item").forGetter(RitualRecipe::resultItem),
            ResourceLocation.CODEC.optionalFieldOf("result_mob").forGetter(RitualRecipe::resultMobId),
            Codec.INT.fieldOf("result_mob_count").orElse(1).forGetter(RitualRecipe::resultMobCount),
            Codec.INT.optionalFieldOf("fluxia_required").forGetter(RitualRecipe::fluxiaRequired),
            Codec.INT.fieldOf("min_tier").orElse(1).forGetter(RitualRecipe::minTier),
            Vigil.CODEC.optionalFieldOf("vigil").forGetter(RitualRecipe::vigil),
            TagKey.codec(Registries.ITEM).optionalFieldOf("awakens").forGetter(RitualRecipe::awakens),
            Imbuement.CODEC.optionalFieldOf("imbues").forGetter(RitualRecipe::imbues)
    ).apply(i, RitualRecipe::new));

    // -------- instance --------
    private final ResourceLocation id;
    private final Grid grid;
    private final List<Cell> pattern;
    private final List<Ingredient> inputs;
    private final Optional<ItemStack> resultItem;
    private final Optional<ResourceLocation> resultMobId;
    private final int resultMobCount;
    private final Optional<Integer> fluxiaRequired;
    private final int minTier;
    private final Optional<Vigil> vigil;
    private final Optional<TagKey<Item>> awakens;
    private final Optional<Imbuement> imbues;

    public RitualRecipe(ResourceLocation id, Grid grid, List<Cell> pattern, List<Ingredient> inputs,
                        Optional<ItemStack> resultItem, Optional<ResourceLocation> resultMobId, int resultMobCount,
                        Optional<Integer> fluxiaRequired, int minTier, Optional<Vigil> vigil,
                        Optional<TagKey<Item>> awakens, Optional<Imbuement> imbues) {
        this.id = id;
        this.grid = grid;
        this.pattern = pattern;
        this.inputs = inputs;
        this.resultItem = resultItem;
        this.resultMobId = resultMobId;
        this.resultMobCount = resultMobCount;
        this.fluxiaRequired = fluxiaRequired;
        this.minTier = Math.max(1, minTier);
        this.vigil = vigil;
        this.awakens = awakens;
        this.imbues = imbues;
    }

    public ResourceLocation id() { return id; }
    public Grid grid() { return grid; }
    public List<Cell> pattern() { return pattern; }
    public List<Ingredient> inputs() { return inputs; }
    public Optional<ItemStack> resultItem() { return resultItem; }
    public Optional<ResourceLocation> resultMobId() { return resultMobId; }
    public int resultMobCount() { return resultMobCount; }
    public Optional<Integer> fluxiaRequired() { return fluxiaRequired; }
    /** Minimum altar tier required to perform this rite. */
    public int minTier() { return minTier; }

    /** Present if this rite is kept burning rather than completed on the spot. */
    public Optional<Vigil> vigil() { return vigil; }
    public boolean isVigil() { return vigil.isPresent(); }

    /**
     * Present if this rite embues what is already lying in the circle rather than
     * producing something new: the tag naming which of those items it will awaken.
     *
     * <p>These items are <em>not</em> reagents. They are never listed in {@link #inputs()},
     * they are not consumed, and they come back out of the circle as the same stacks they
     * went in as — enchantments, damage and custom name intact — only awakened.</p>
     */
    public Optional<TagKey<Item>> awakens() { return awakens; }
    public boolean isAwakening() { return awakens.isPresent(); }

    /**
     * Present if this rite imbues the awakened pieces lying in the circle. Like an awakening,
     * those pieces are not reagents: they are never consumed and come back as themselves,
     * carrying the new imbuement in place of any old one.
     */
    public Optional<Imbuement> imbues() { return imbues; }
    public boolean isImbuing() { return imbues.isPresent(); }

    /** World check you call from altar/brazier */
    public boolean matchesAt(Level level, BlockPos center) {
        for (Cell c : pattern) {
            if (c.dx()==0 && c.dz()==0) return false; // center is altar; disallow
            BlockPos p = center.offset(c.dx(), 0, c.dz());
            var s = level.getBlockState(p);
            if (!(s.getBlock() instanceof SigilBlock)) {
                if (c.required()) return false;
                else continue;
            }
            if (c.type().isPresent()  && s.getValue(SigilBlock.TYPE)  != c.type().get()) return false;
            if (c.color().isPresent() && s.getValue(SigilBlock.COLOR) != c.color().get()) return false;
        }
        return true;
    }

    // ---- Recipe<RecipeInput> plumbing (not used by vanilla crafting UI) ----
    @Override public boolean matches(RecipeInput input, net.minecraft.world.level.Level level) { return false; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider regs) { return ItemStack.EMPTY; }
    @Override public ItemStack getResultItem(HolderLookup.Provider regs) { return ItemStack.EMPTY; }

    public ResourceLocation getId() {
        return id;
    }

    public int maxRadius() {
        int r = this.grid.radius; // G3=1, G6=3, G9=4
        for (var c : this.pattern) {
            int m = Math.max(Math.abs(c.dx()), Math.abs(c.dz()));
            if (m > r) r = m;
        }
        return r;
    }

    @Override public RecipeSerializer<?> getSerializer() { return RecipeRegistry.RITUAL_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return TYPE; }
    @Override public boolean canCraftInDimensions(int w, int h) { return false; } // required on some mappings
    // Optional:
    // @Override public boolean isSpecial() { return true; }
}