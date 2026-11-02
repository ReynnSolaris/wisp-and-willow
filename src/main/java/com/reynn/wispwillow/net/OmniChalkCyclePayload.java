package com.reynn.wispwillow.net;

import com.reynn.wispwillow.WispWillow;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * "The player scrolled the chalk", not "the chalk is now set to X".
 *
 * <p>Sending the intent rather than the result keeps the list of rites and the order they
 * cycle in on the server, where the stack actually lives — a client that disagreed about
 * the order (a different mod version, a datapack it has not seen) can only ever ask for
 * the next one, never for something the server would not have chosen itself.</p>
 *
 * @param delta   which way the wheel went; ignored when {@code chapter} is set
 * @param chapter true to jump to the first rite of the next chapter instead
 */
public record OmniChalkCyclePayload(int delta, boolean chapter) implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "omni_chalk_cycle");
    public static final Type<OmniChalkCyclePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, OmniChalkCyclePayload> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> {
                buf.writeVarInt(msg.delta);
                buf.writeBoolean(msg.chapter);
            }, buf -> new OmniChalkCyclePayload(buf.readVarInt(), buf.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
