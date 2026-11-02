package com.reynn.wispwillow.worldgen.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reynn.wispwillow.blocks.fen.FenMossBlock;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

/**
 * Hangs drapes of fen moss from the underside of a tree's canopy.
 *
 * <p>Only leaves with open air directly beneath them are considered, so drapes come off the
 * bottom of the crown and its hanging tassels rather than being buried inside it.</p>
 */
public class FenDrapeDecorator extends TreeDecorator {
    public static final MapCodec<FenDrapeDecorator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(d -> d.probability),
            IntProvider.codec(1, 8).fieldOf("length").forGetter(d -> d.length)
    ).apply(instance, FenDrapeDecorator::new));

    private final float probability;
    private final IntProvider length;

    public FenDrapeDecorator(float probability, IntProvider length) {
        this.probability = probability;
        this.length = length;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return MainRegistry.FEN_DRAPE.get();
    }

    @Override
    public void place(Context context) {
        RandomSource random = context.random();
        for (BlockPos leaf : context.leaves()) {
            BlockPos start = leaf.below();
            if (!context.isAir(start) || random.nextFloat() >= probability) continue;

            int wanted = length.sample(random);
            int placed = 0;
            while (placed < wanted && context.isAir(start.below(placed))) placed++;

            for (int i = 0; i < placed; i++) {
                context.setBlock(start.below(i), BlockRegistry.FEN_MOSS.get().defaultBlockState()
                        .setValue(FenMossBlock.TIP, i == placed - 1));
            }
        }
    }
}
