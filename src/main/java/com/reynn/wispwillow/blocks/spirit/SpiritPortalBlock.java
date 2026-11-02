package com.reynn.wispwillow.blocks.spirit;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.world.SpiritTravel;
import com.reynn.wispwillow.worldgen.spirit.SpiritRealm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * The way into the {@link com.reynn.wispwillow.worldgen.spirit.SpiritRealm}, held open by a
 * Vigil of the Open Way.
 *
 * <p>Placed and removed by the vigil, never by a player — it has no item and no loot. Before this
 * existed the vigil simply teleported anyone standing on the altar, which worked and was invisible:
 * there was nothing to walk into and nothing to tell you the way was open. Now the rite hangs a
 * sheet above its own altar and <em>that</em> is what takes you, so standing on the altar still
 * works and you can finally see why.</p>
 *
 * <p>No collision, like a nether portal: you walk through it rather than into it, and anything
 * thrown falls through onto the altar instead of bouncing off.</p>
 */
public class SpiritPortalBlock extends Block implements EntityBlock {

    public static final MapCodec<SpiritPortalBlock> CODEC = simpleCodec(SpiritPortalBlock::new);

    /** Which way the sheet lies, so the renderer draws across the opening. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public SpiritPortalBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AXIS);
    }

    /** Nothing to bake: {@code SpiritPortalRenderer} draws it. */
    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpiritPortalBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    /**
     * Anyone who steps into the sheet goes through.
     *
     * <p>Only players, and only ones the realm is not already holding — {@code isTravelling} is
     * what stops someone who walks back out of the sheet on the far side from being sent again and
     * having their way home overwritten with a position inside the realm.</p>
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer player)) return;
        // Freshly arrived, or freshly sent. Without this a traveller set down on a gate is taken by
        // it again on the very next tick.
        if (player.isOnPortalCooldown()) return;

        MinecraftServer server = level.getServer();
        if (server == null) return;

        // A gate standing in the realm is the way back, not another way in. Without this the
        // return gate would try to send an arriving traveller onward into the realm they are
        // already standing in, and their way home would be overwritten with a position inside it.
        if (level.dimension().equals(SpiritRealm.LEVEL)) {
            if (!SpiritTravel.isTravelling(player)) return;
            server.execute(() -> {
                if (player.isRemoved() || !SpiritTravel.isTravelling(player)) return;
                SpiritTravel.returnHome(player, server, true);
            });
            return;
        }

        // Someone the realm just set down here is not asking to go back in — they are standing where
        // it left them, which is beside a gate that is still burning.
        if (SpiritTravel.standingWhereTheyLanded(player)) return;

        // Standing in the overworld with a way home recorded means that way home is stale — the trip
        // it belongs to is over. Refusing them on the strength of it is what made the gate silently
        // do nothing; forget it and let them through.
        if (SpiritTravel.isTravelling(player)) SpiritTravel.forgetReturn(player);

        // Deferred to the end of the tick rather than done here. entityInside runs in the middle of
        // Entity.move(), while the collision pass still holds the entity's position: teleporting from
        // inside it changed the dimension but the movement code then wrote the old coordinates back,
        // so a traveller arrived in the realm at exactly the y they had been standing at in the
        // overworld — out in the void, well below the island field. Vanilla never teleports from here
        // either; a nether portal only marks the entity and moves it once the tick is over.
        server.execute(() -> {
            if (player.isRemoved() || SpiritTravel.isTravelling(player)) return;
            SpiritTravel.send(player, server);
        });
    }

    /** Never drops, and cannot be picked. The vigil owns these. */
    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,
                                       net.minecraft.world.level.LevelReader level, BlockPos pos,
                                       net.minecraft.world.entity.player.Player player) {
        return ItemStack.EMPTY;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) != 0) return;
        level.addParticle(ParticleTypes.REVERSE_PORTAL,
                pos.getX() + random.nextDouble(),
                pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(),
                (random.nextDouble() - 0.5) * 0.03, 0.03 + random.nextDouble() * 0.04, (random.nextDouble() - 0.5) * 0.03);
    }
}
