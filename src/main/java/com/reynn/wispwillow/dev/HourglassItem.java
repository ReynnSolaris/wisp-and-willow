package com.reynn.wispwillow.dev;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Random ticks on demand, for anything that grows, spreads or ripens on its own time.
 *
 * <p>Right-click a block: it gets {@value #TICKS_SINGLE} random ticks at once. Sneak: every
 * block within {@value #AREA_RADIUS} gets {@value #TICKS_AREA} each, for a bed of reeds or a
 * patch of fen moss. Bone meal only works on what vanilla decided it should; this works on
 * whatever random-ticks, which is where most of this mod's plants live.</p>
 */
public class HourglassItem extends DevToolItem {

    public static final int TICKS_SINGLE = 64;
    public static final int TICKS_AREA = 8;
    public static final int AREA_RADIUS = 2;

    public HourglassItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        if (player == null) return InteractionResult.PASS;
        if (!gate(player)) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        RandomSource random = server.getRandom();
        int blocks = 0, ticks = 0;
        if (player.isSecondaryUseActive()) {
            for (BlockPos each : BlockPos.betweenClosed(pos.offset(-AREA_RADIUS, -AREA_RADIUS, -AREA_RADIUS),
                    pos.offset(AREA_RADIUS, AREA_RADIUS, AREA_RADIUS))) {
                // Immutable: a block that schedules a tick keeps the position it was handed.
                int done = tick(server, each.immutable(), TICKS_AREA, random);
                if (done > 0) blocks++;
                ticks += done;
            }
        } else {
            ticks = tick(server, pos, TICKS_SINGLE, random);
            blocks = ticks > 0 ? 1 : 0;
        }

        if (blocks == 0) {
            overlay(player, Component.translatable("item.wispwillow.hourglass.nothing"));
            return InteractionResult.FAIL;
        }
        overlay(player, Component.translatable("item.wispwillow.hourglass.ticked", ticks, blocks));
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12 + 4 * blocks,
                player.isSecondaryUseActive() ? AREA_RADIUS : 0.4, 0.4, player.isSecondaryUseActive() ? AREA_RADIUS : 0.4, 0.0);
        server.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.5f, 1.6f);
        return InteractionResult.SUCCESS;
    }

    /** Random-ticks one position up to {@code times}, re-reading the state each time in case it changed. */
    private static int tick(ServerLevel level, BlockPos pos, int times, RandomSource random) {
        int done = 0;
        for (int i = 0; i < times; i++) {
            BlockState state = level.getBlockState(pos);
            if (!state.isRandomlyTicking()) break;
            state.randomTick(level, pos, random);
            done++;
        }
        return done;
    }
}
