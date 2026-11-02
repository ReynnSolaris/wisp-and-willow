package com.reynn.wispwillow.fluxia.net;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.fluxia.SimpleFluxiaStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FluxiaSyncPayload(int flux, int max, int regen) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "fluxia_sync");
    public static final Type<FluxiaSyncPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, FluxiaSyncPayload> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> {
                buf.writeVarInt(msg.flux);
                buf.writeVarInt(msg.max);
                buf.writeVarInt(msg.regen);
            }, buf -> new FluxiaSyncPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static FluxiaSyncPayload from(SimpleFluxiaStorage fx) {
        return new FluxiaSyncPayload(fx.amount(), fx.capacity(), fx.regen());
    }
}
