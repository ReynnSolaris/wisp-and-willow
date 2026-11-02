package com.reynn.wispwillow.dev;

import com.reynn.wispwillow.blocks.AltarBlockEntity;
import com.reynn.wispwillow.blocks.alchemy.AlchemicalCauldronBlockEntity;
import com.reynn.wispwillow.blocks.fluxia.GatewayCoreBlockEntity;
import com.reynn.wispwillow.client.goggles.GoggleText;
import com.reynn.wispwillow.fluxia.BottomlessFluxiaStorage;
import com.reynn.wispwillow.fluxia.IFluxiaNode;
import com.reynn.wispwillow.fluxia.IFluxiaStorage;
import com.reynn.wispwillow.fluxia.SimpleFluxiaStorage;
import com.reynn.wispwillow.ritual.RitualRecipe;
import com.reynn.wispwillow.ritual.Vigil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fills, drains and reads any Fluxia node in one click.
 *
 * <p>Right-click a pool, altar, cauldron, gateway, spitter, flower, dynamo or font: its
 * reserve is set to full and its state is printed to chat. Sneak to drain it to nothing
 * instead. The readout is the same data the Arcane Goggles show, plus the parts that only
 * matter when something is wrong: which rite the altar thinks is drawn and how many glyphs
 * it is short, whether the cauldron is lit, whether the gateway is open.</p>
 *
 * <p>A cauldron that is not lit is lit on the way past, since a cold cauldron on full charge
 * is almost never what the tester wanted.</p>
 */
public class FluxiaProbeItem extends DevToolItem {

    public FluxiaProbeItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        if (player == null) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof IFluxiaNode node)) return InteractionResult.PASS;
        if (!gate(player)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = (BlockEntity) node;
        BlockState state = level.getBlockState(pos);
        boolean drain = player.isSecondaryUseActive();

        List<Component> lines = new ArrayList<>();
        lines.add(head("Probe", state.getBlock().getName().copy()
                .append(Component.literal(" at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()))));

        IFluxiaStorage storage = node.getFluxStorage();
        if (storage == null) {
            lines.add(kv("Reserve", "none (relay)"));
        } else {
            if (storage instanceof BottomlessFluxiaStorage) {
                lines.add(kv("Reserve", "bottomless; nothing to " + (drain ? "drain" : "fill")));
            } else if (storage instanceof SimpleFluxiaStorage simple) {
                // setFlux rather than insert/extract: those clamp to the per-tick rate, which is
                // a throughput limit and not what "set it to full" means.
                simple.setFlux(drain ? 0 : simple.capacity());
                lines.add(kv("Reserve", drain ? warn("drained") : good("filled")));
            } else {
                pump(storage, drain);
                lines.add(kv("Reserve", drain ? warn("drained (by rate)") : good("filled (by rate)")));
            }
            lines.add(kv("Stored", GoggleText.num(storage.amount()) + " / " + GoggleText.num(storage.capacity()) + " f"));
            lines.add(kv("Rate", "in " + GoggleText.num(storage.canAcceptPerTick())
                    + " f/t, out " + GoggleText.num(storage.canOutputPerTick()) + " f/t"));
        }
        lines.add(kv("Role", node.getFluxRole().name().toLowerCase(Locale.ROOT)
                + ", " + node.getLinkedNodes().size() + " wand link(s), wants "
                + GoggleText.num(node.getDesiredFluxPerTick()) + " f/t"));

        if (be instanceof AltarBlockEntity altar) describeAltar(altar, lines);
        if (be instanceof AlchemicalCauldronBlockEntity cauldron) describeCauldron(cauldron, drain, lines);
        if (be instanceof GatewayCoreBlockEntity gateway) {
            lines.add(kv("Gateway", gateway.isOpen() ? good("open") : Component.literal("closed")));
        }

        be.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        for (Component line : lines) chat(player, line);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f, drain ? 0.7f : 1.4f);
        return InteractionResult.SUCCESS;
    }

    /** For a storage that is not a SimpleFluxiaStorage: move it through its own rate until it stops. */
    private static void pump(IFluxiaStorage storage, boolean drain) {
        for (int guard = 0; guard < 100_000; guard++) {
            int moved = drain
                    ? storage.extract(storage.amount(), false)
                    : storage.insert(storage.capacity() - storage.amount(), false);
            if (moved <= 0) break;
        }
    }

    private static void describeAltar(AltarBlockEntity altar, List<Component> lines) {
        altar.forceRecompute();
        RitualRecipe rite = altar.getCachedMatch();
        if (rite == null) {
            lines.add(kv("Rite", warn("none recognised")));
        } else {
            lines.add(kv("Rite", Component.translatable(rite.getId().toLanguageKey())
                    .append(Component.literal("  (" + rite.getId() + ")"))));
            int missing = altar.getMissing().size();
            lines.add(kv("Glyphs", missing == 0 ? good("complete") : warn(missing + " missing")));
            lines.add(kv("Tier", "needs " + rite.minTier() + ", foundation is " + altar.getFoundation().tier()
                    + (altar.getFoundation().hasMisplacedRunes() ? " (runes misplaced)" : "")));
            int cost = altar.effectiveCost(rite.fluxiaRequired().orElse(0));
            lines.add(kv("Cost", GoggleText.num(cost) + " f after boons"));
        }
        lines.add(kv("Familiars", altar.familiarsInService() + " serving of " + altar.familiarSlots() + " slot(s), "
                + altar.familiarsBound() + " bound"));
        lines.add(kv("Boons", Math.round(altar.boonEfficiency() * 100) + "% cheaper, "
                + Math.round(altar.boonYield() * 100) + "% bonus yield"));
        if (altar.hasVigil()) {
            lines.add(kv("Vigil", "burning, " + Vigil.formatTicks(altar.vigilRemaining()) + " left"));
        }
    }

    private static void describeCauldron(AlchemicalCauldronBlockEntity cauldron, boolean drain, List<Component> lines) {
        if (!drain && !cauldron.isLit() && cauldron.tryIgnite()) {
            lines.add(kv("Fire", good("lit it")));
        } else {
            lines.add(kv("Fire", cauldron.isLit() ? good("lit") : Component.literal("cold")));
        }
        lines.add(kv("Brewing", cauldron.isCrafting() ? good("yes") : Component.literal("idle")));
        lines.add(kv("Upgrades", "haste " + cauldron.hasteCount() + ", thrift " + cauldron.thriftCount()
                + ", capacity " + cauldron.capacityCount()));
    }
}
