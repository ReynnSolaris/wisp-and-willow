package com.reynn.wispwillow.worldgen.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

public class WillowTreeVineDecorator extends TreeDecorator {
    public static final MapCodec<WillowTreeVineDecorator> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("probability").xmap(WillowTreeVineDecorator::new, (p_226037_) -> p_226037_.probability);
    private final float probability;

    protected TreeDecoratorType<?> type() {
        return MainRegistry.WILLOW_VINES.get();
    }

    public WillowTreeVineDecorator(float probability) {
        this.probability = probability;
    }

    public void place(TreeDecorator.Context context) {
        RandomSource randomsource = context.random();
        context.leaves().forEach((p_226035_) -> {
            if (randomsource.nextFloat() < this.probability) {
                BlockPos blockpos = p_226035_.west();
                if (context.isAir(blockpos)) {
                    addHangingVine(blockpos, VineBlock.EAST, context);
                }
            }

            if (randomsource.nextFloat() < this.probability) {
                BlockPos blockpos1 = p_226035_.east();
                if (context.isAir(blockpos1)) {
                    addHangingVine(blockpos1, VineBlock.WEST, context);
                }
            }

            if (randomsource.nextFloat() < this.probability) {
                BlockPos blockpos2 = p_226035_.north();
                if (context.isAir(blockpos2)) {
                    addHangingVine(blockpos2, VineBlock.SOUTH, context);
                }
            }

            if (randomsource.nextFloat() < this.probability) {
                BlockPos blockpos3 = p_226035_.south();
                if (context.isAir(blockpos3)) {
                    addHangingVine(blockpos3, VineBlock.NORTH, context);
                }
            }

        });
    }

    public static void placeVine(TreeDecorator.Context context, BlockPos pos, BooleanProperty sideProperty) {
        context.setBlock(pos, BlockRegistry.WILLOW_VINE.get().defaultBlockState().setValue(sideProperty, true));
    }

    private static void addHangingVine(BlockPos pos, BooleanProperty sideProperty, TreeDecorator.Context context) {
        placeVine(context, pos, sideProperty);
        int i = 4;

        for(BlockPos blockpos = pos.below(); context.isAir(blockpos) && i > 0; --i) {
            placeVine(context, blockpos, sideProperty);
            blockpos = blockpos.below();
        }

    }
}