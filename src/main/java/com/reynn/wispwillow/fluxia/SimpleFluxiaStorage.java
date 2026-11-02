package com.reynn.wispwillow.fluxia;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class SimpleFluxiaStorage implements IFluxiaStorage {
    protected int flux, max, inRate, outRate, regen;

    public SimpleFluxiaStorage(int capacity, int perTickIn, int perTickOut, int regen) {
        this.max = Math.max(1, capacity);
        this.inRate = Math.max(1, perTickIn);
        this.outRate = Math.max(1, perTickOut);
        // Zero is a meaningful regen rate ("this thing does not refill itself"); clamping
        // it up to 1 silently gave every pool, spitter and altar free Fluxia.
        this.regen = Math.max(0, regen);
    }

    // ---- IFluxiaStorage ----
    @Override public int insert(int amount, boolean sim) {
        if (amount <= 0) return 0;
        int can = Math.min(inRate, Math.min(amount, max - flux));
        if (!sim) flux += can;
        return can;
    }


    @Override public int extract(int amount, boolean sim) {
        if (amount <= 0) return 0;
        int can = Math.min(outRate, Math.min(amount, flux));
        if (!sim) flux -= can;
        return can;
    }


    @Override public int amount() { return flux; }
    @Override public int capacity() { return max; }
    @Override public int canAcceptPerTick() { return inRate; }
    @Override public int canOutputPerTick() { return outRate; }

    // ---- extra helpers ----
    public void set(int v) { flux = Math.max(0, Math.min(v, max)); }
    public int inRate() { return inRate; }
    public int outRate() { return outRate; }
    public int regen() { return regen; }

    public void setRegen(int regen) {
        this.regen = regen;
    }

    public void setFlux(int flux) {
        this.flux = flux;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public CompoundTag save(CompoundTag tag){
        tag.putInt("flux", flux);
        tag.putInt("cap", max);
        tag.putInt("in", inRate);
        tag.putInt("out", outRate);
        tag.putInt("regen", regen);
        return tag;
    }
    public void load(CompoundTag tag){
        if (tag.contains("cap")) max = Math.max(1, tag.getInt("cap"));
        if (tag.contains("in")) inRate = Math.max(1, tag.getInt("in"));
        if (tag.contains("out")) outRate = Math.max(1, tag.getInt("out"));
        if (tag.contains("regen")) regen = Math.max(0, tag.getInt("regen"));
        flux = Math.max(0, Math.min(tag.getInt("flux"), max));
    }

    // ---- codecs for attachments/persistence/networking ----
    public static final Codec<SimpleFluxiaStorage> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("flux").forGetter(SimpleFluxiaStorage::amount),
            Codec.INT.fieldOf("cap").forGetter(SimpleFluxiaStorage::capacity),
            Codec.INT.fieldOf("in").forGetter(SimpleFluxiaStorage::inRate),
            Codec.INT.fieldOf("out").forGetter(SimpleFluxiaStorage::outRate),
            Codec.INT.fieldOf("regen").forGetter(SimpleFluxiaStorage::regen)
    ).apply(i, (flux, cap, in, out, regen) -> {
        var s = new SimpleFluxiaStorage(cap, in, out, regen);
        s.set(flux);
        return s;
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, SimpleFluxiaStorage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SimpleFluxiaStorage::amount,
                    ByteBufCodecs.VAR_INT, SimpleFluxiaStorage::capacity,
                    ByteBufCodecs.VAR_INT, SimpleFluxiaStorage::inRate,
                    ByteBufCodecs.VAR_INT, SimpleFluxiaStorage::outRate,
                    ByteBufCodecs.VAR_INT, SimpleFluxiaStorage::regen,
                    (flux, cap, in, out, regen) -> { var s = new SimpleFluxiaStorage(cap, in, out, regen); s.set(flux); return s; }
            );
}