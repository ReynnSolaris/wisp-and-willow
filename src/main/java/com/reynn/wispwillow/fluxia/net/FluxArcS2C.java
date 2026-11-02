package com.reynn.wispwillow.fluxia.net;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.client.renderers.FluxArcRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * "Fluxia just moved from here to there." Sent once per transfer to everyone tracking the
 * source chunk; the client turns it into a travelling mote of light.
 *
 * <p>This replaces the old approach of blasting up to 65 individual particle packets down
 * the wire for every single beam.</p>
 */
public record FluxArcS2C(BlockPos from, BlockPos to, int amount, int argb) implements CustomPacketPayload {

    public static final Type<FluxArcS2C> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "flux_arc"));

    /** Default Fluxia colour: the same cyan the book and HUD use. */
    public static final int FLUXIA_ARGB = 0xFF7FE8FF;

    public static final StreamCodec<RegistryFriendlyByteBuf, FluxArcS2C> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> {
                        buf.writeBlockPos(msg.from);
                        buf.writeBlockPos(msg.to);
                        buf.writeVarInt(msg.amount);
                        buf.writeInt(msg.argb);
                    },
                    buf -> new FluxArcS2C(buf.readBlockPos(), buf.readBlockPos(), buf.readVarInt(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Server-side helper: announce a transfer to nearby players. */
    public static void send(ServerLevel level, BlockPos from, BlockPos to, int amount, int argb) {
        if (amount <= 0 || from.equals(to)) return;
        // Long links can span chunks; tell both ends so neither side misses the arc.
        // Sending to both endpoint chunks delivered the arc twice to anyone tracking
        // both, which stacked two copies of every beam. One send, generous radius.
        FluxArcS2C msg = new FluxArcS2C(from, to, amount, argb);
        double cx = (from.getX() + to.getX()) * 0.5;
        double cy = (from.getY() + to.getY()) * 0.5;
        double cz = (from.getZ() + to.getZ()) * 0.5;
        double reach = Math.sqrt(from.distSqr(to)) * 0.5 + 64.0;
        PacketDistributor.sendToPlayersNear(level, null, cx, cy, cz, reach, msg);
    }

    public static void send(ServerLevel level, BlockPos from, BlockPos to, int amount) {
        send(level, from, to, amount, FLUXIA_ARGB);
    }

    public static void handle(FluxArcS2C msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> FluxArcRenderer.accept(msg));
    }
}
