package com.reynn.wispwillow.blocks.fluxia;

import com.reynn.wispwillow.client.goggles.GoggleText;
import com.reynn.wispwillow.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drinks starlight. Passive, fuel-free, and better at night.
 *
 * <p>The counterweight to the {@link FluxiaDynamoEntity dynamo}: it asks for no fuel at all,
 * only an open sky, and pays best in the small hours. A field of fonts will hold a Soul
 * Gateway open through the night and struggle through a rainy afternoon.</p>
 */
public class AstralFontEntity extends AbstractFluxiaGenerator {

    /** Under an open sky by day. */
    public static final int DAY_RATE = 75;
    /** Under an open sky at night, when the stars are actually out. */
    public static final int NIGHT_RATE = 150;

    private static final int CAPACITY = 30_000;
    private static final int THROUGHPUT = 500;

    public AstralFontEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ASTRAL_FONT.get(), pos, state, CAPACITY, THROUGHPUT, THROUGHPUT);
    }

    @Override
    public boolean isRunning() {
        return level != null && seesSky(level);
    }

    private boolean seesSky(net.minecraft.world.level.LevelReader level) {
        return level.canSeeSky(worldPosition.above());
    }

    @Override
    protected int generatePerSecond(ServerLevel level, BlockPos pos, BlockState state) {
        return rate(level);
    }

    @Override
    protected int currentRate() {
        return level == null ? 0 : rate(level);
    }

    /** Takes a plain Level, not a ServerLevel: the goggles ask for this on the client. */
    private int rate(net.minecraft.world.level.Level level) {
        if (!seesSky(level)) return 0;
        int base = level.isNight() ? NIGHT_RATE : DAY_RATE;
        // Cloud cover between the font and the sky it is reading.
        if (level.isRaining()) base /= 2;
        if (level.isThundering()) base /= 2;
        return base;
    }

    @Override
    protected void addExtraGoggleLines(java.util.List<Component> lines) {
        lines.add(GoggleText.line("goggles.wispwillow.sky",
                level != null && seesSky(level)
                        ? GoggleText.good("open")
                        : GoggleText.warn("blocked")));
    }
}
