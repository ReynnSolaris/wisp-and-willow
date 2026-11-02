package com.reynn.wispwillow.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SigilBlock extends Block {
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    public static final EnumProperty<SigilType> TYPE = EnumProperty.create("type", SigilType.class);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public SigilBlock(Properties props) {
        super(props.noCollission().noOcclusion().instabreak());
        registerDefaultState(
                this.stateDefinition.any()
                .setValue(COLOR, DyeColor.WHITE)
                .setValue(TYPE, SigilType.BASE)
        );
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b){ b.add(COLOR, TYPE); }
    @Override public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c){ return SHAPE; }
}