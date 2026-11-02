package com.reynn.wispwillow.dev;

import com.reynn.wispwillow.world.SpiritTravel;
import com.reynn.wispwillow.worldgen.spirit.SpiritRealm;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Spirit Realm without the vigil: use it to go through, use it again to come home.
 *
 * <p>Goes through the same {@link SpiritTravel} calls the Open Way makes, so the return
 * point, the escort of pets, the landing grace and the gate that opens behind you are all
 * exercised exactly as a player would exercise them.</p>
 */
public class SpiritKeyItem extends DevToolItem {

    public SpiritKeyItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack key = player.getItemInHand(hand);
        if (!gate(player)) return InteractionResultHolder.fail(key);
        if (level.isClientSide) return InteractionResultHolder.success(key);
        if (!(player instanceof ServerPlayer traveller)) return InteractionResultHolder.pass(key);

        MinecraftServer server = traveller.serverLevel().getServer();
        if (traveller.serverLevel().dimension().equals(SpiritRealm.LEVEL)) {
            SpiritTravel.returnHome(traveller, server, true);
        } else {
            SpiritTravel.send(traveller, server);
        }
        // The far end has its own gate underfoot; a second use the same second would just bounce.
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.consume(key);
    }
}
