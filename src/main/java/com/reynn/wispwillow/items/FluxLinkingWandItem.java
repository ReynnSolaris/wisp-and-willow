package com.reynn.wispwillow.items;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.blocks.fluxia.FluxiaSpitterBlockEntity;
import com.reynn.wispwillow.fluxia.IFluxiaNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class FluxLinkingWandItem extends Item implements GeoItem {

    // =========================
    // GeckoLib
    // =========================

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** The staff holds still; there is nothing to animate, so no controllers. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /**
     * The renderer: {@code geo/item/flux_linking_wand.geo.json} over
     * {@code textures/item/flux_linking_wand.png}, with the {@code _glowmask} texture drawn
     * fullbright on top by the glowing layer.
     *
     * <p>Only the lambda body names client classes. GeckoLib calls this from the client
     * bootstrap alone, so a dedicated server never loads them.</p>
     */
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<FluxLinkingWandItem> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GeoItemRenderer<>(new DefaultedItemGeoModel<>(
                            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "flux_linking_wand")));
                    this.renderer.addRenderLayer(new AutoGlowingGeoLayer<>(this.renderer));
                }
                return this.renderer;
            }
        });
    }

    // Root tag inside CUSTOM_DATA
    private static final String ROOT_KEY = "fluxia_link";
    // Fields under ROOT_KEY
    private static final String KEY_DIM = "dim";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_Z = "z";

    /**
     * True if this stack is a linking wand.
     *
     * <p>Blocks that override {@code useWithoutItem} must check this and get out of the
     * way. ServerPlayerGameMode runs the block's own interaction <em>before</em>
     * {@code ItemStack#useOn}, and it does so even when the player is holding something —
     * so a pool that adds charge on right-click, or a gateway that toggles, swallows the
     * click and the wand never fires. Sneaking is not a workaround: that reaches the wand
     * but triggers its clear-links branch instead.</p>
     */
    public static boolean isWand(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof FluxLinkingWandItem;
    }

    public FluxLinkingWandItem(Properties props) {
        super(props);
    }

    // =========================
    // Data helpers
    // =========================

    private static CompoundTag getRoot(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        // copyTag() gives us a mutable tag; EMPTY returns a new tag
        return data.copyTag();
    }

    private static void saveRoot(ItemStack stack, CompoundTag root) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static void clearStoredSource(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        root.remove(ROOT_KEY);
        saveRoot(stack, root);
    }

    private record StoredSource(ResourceLocation dim, BlockPos pos) {}

    private static StoredSource readStoredSource(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        if (!root.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag tag = root.getCompound(ROOT_KEY);

        if (!tag.contains(KEY_DIM, Tag.TAG_STRING) ||
                !tag.contains(KEY_X, Tag.TAG_INT) ||
                !tag.contains(KEY_Y, Tag.TAG_INT) ||
                !tag.contains(KEY_Z, Tag.TAG_INT)) {
            return null;
        }

        String dimStr = tag.getString(KEY_DIM);
        ResourceLocation dimId;
        try {
            dimId = ResourceLocation.parse(dimStr);
        } catch (Exception e) {
            return null;
        }

        BlockPos pos = new BlockPos(tag.getInt(KEY_X), tag.getInt(KEY_Y), tag.getInt(KEY_Z));
        return new StoredSource(dimId, pos);
    }

    private static void storeSource(ItemStack stack, Level level, BlockPos pos) {
        CompoundTag root = getRoot(stack);
        CompoundTag tag = root.contains(ROOT_KEY, Tag.TAG_COMPOUND)
                ? root.getCompound(ROOT_KEY)
                : new CompoundTag();

        tag.putString(KEY_DIM, level.dimension().location().toString());
        tag.putInt(KEY_X, pos.getX());
        tag.putInt(KEY_Y, pos.getY());
        tag.putInt(KEY_Z, pos.getZ());

        root.put(ROOT_KEY, tag);
        saveRoot(stack, root);
    }

    // =========================
    // Interaction
    // =========================

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        BlockPos clickedPos = ctx.getClickedPos();

        if (player == null) {
            return InteractionResult.PASS;
        }

        // Only do logic server-side; client will get messages via displayClientMessage
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Shift-right-click: clear stored source
        if (player.isShiftKeyDown()) {
            clearStoredSource(stack);

            BlockEntity be = level.getBlockEntity(clickedPos);
            if (be instanceof IFluxiaNode node) {
                // Copy to avoid concurrent modification while removing
                var toRemove = List.copyOf(node.getLinkedNodes());
                for (BlockPos other : toRemove) {
                    node.removeLinkedNode(other);
                }

                // If it's a spitter, also make sure its internal cached links are reset (if you later add helpers)
                if (be instanceof FluxiaSpitterBlockEntity spitter) {
                    spitter.clearOutgoingLinks();
                }

                be.setChanged();
                level.sendBlockUpdated(clickedPos, be.getBlockState(), be.getBlockState(), 3);

                player.displayClientMessage(
                        Component.literal("Cleared flux links for ")
                                .append(posToText(level, clickedPos))
                                .withStyle(style -> style.withColor(0xA0A0FF)),
                        true
                );
            } else {
                // Fallback: just clear wand memory, same as before
                player.displayClientMessage(
                        Component.translatable("item.wispwillow.flux_linking_wand.cleared")
                                .withStyle(style -> style.withColor(0xA0A0FF)),
                        true
                );
            }

            return InteractionResult.CONSUME;
        }

        StoredSource stored = readStoredSource(stack);

        if (stored == null) {
            // First click: set the source
            storeSource(stack, level, clickedPos);
            player.displayClientMessage(
                    Component.translatable(
                            "item.wispwillow.flux_linking_wand.set_source",
                            posToText(level, clickedPos)
                    ).withStyle(style -> style.withColor(0xC0FFE0)),
                    true
            );
        } else {
            // Second click: attempt to establish link
            ResourceLocation currentDim = level.dimension().location();

            if (!stored.dim.equals(currentDim)) {
                player.displayClientMessage(
                        Component.translatable(
                                "item.wispwillow.flux_linking_wand.dim_mismatch",
                                stored.dim.toString(),
                                currentDim.toString()
                        ).withStyle(style -> style.withColor(0xFF8080)),
                        true
                );
            } else {
                boolean success = onLinkEstablished(level, player, stored.pos, clickedPos);

                if (success) {
                    player.displayClientMessage(
                            Component.translatable(
                                    "item.wispwillow.flux_linking_wand.link_success",
                                    posToText(level, stored.pos),
                                    posToText(level, clickedPos)
                            ).withStyle(style -> style.withColor(0x80FF80)),
                            true
                    );
                } else {
                    player.displayClientMessage(
                            Component.translatable(
                                    "item.wispwillow.flux_linking_wand.link_failed",
                                    posToText(level, stored.pos),
                                    posToText(level, clickedPos)
                            ).withStyle(style -> style.withColor(0xFF8080)),
                            true
                    );
                }
            }

            // Either way, clear source after attempting to link
            clearStoredSource(stack);
        }

        return InteractionResult.CONSUME;
    }

    /**
     * Wire into Fluxia network:
     * - Only allow links if BOTH are IFluxiaNode
     * - Actually stores the link on both ends
     */
    protected boolean onLinkEstablished(Level level, Player player, BlockPos source, BlockPos target) {
        if (level.isClientSide()) {
            return false;
        }

        // Prevent self-link
        if (source.equals(target)) {
            player.displayClientMessage(
                    Component.literal("Cannot link a block to itself.")
                            .withStyle(style -> style.withColor(0xFF8080)),
                    true
            );
            return false;
        }

        Vec3 from = Vec3.atCenterOf(source);
        Vec3 to   = Vec3.atCenterOf(target);

        double distance = from.distanceTo(to);

        if (distance > 16) {
            player.displayClientMessage(
                    Component.literal("Cannot link longer than 16 blocks.")
                            .withStyle(style -> style.withColor(0xFF8080)),
                    true
            );
            return false;
        }

        BlockEntity beA = level.getBlockEntity(source);
        BlockEntity beB = level.getBlockEntity(target);

        if (!(beA instanceof IFluxiaNode nodeA)) {
            player.displayClientMessage(
                    Component.literal("Source is not a Fluxia node: ")
                            .append(posToText(level, source)),
                    true
            );
            return false;
        }

        if (!(beB instanceof IFluxiaNode nodeB)) {
            player.displayClientMessage(
                    Component.literal("Target is not a Fluxia node: ")
                            .append(posToText(level, target)),
                    true
            );
            return false;
        }

        // ============================
        // Directional link: SOURCE → TARGET
        // ============================

        if (beA instanceof FluxiaSpitterBlockEntity spitter) {
            // Spitter = relay/gun: it should only ever have ONE outgoing link.

            // 1) Clear any previous targets on this spitter
            spitter.clearOutgoingLinks();

            // 2) Add the new target
            nodeA.addLinkedNode(target);

            // 3) Rotate spitter to face the new target
            Direction facing = Direction.getNearest(
                    target.getX() - source.getX(),
                    target.getY() - source.getY(),
                    target.getZ() - source.getZ()
            );
            spitter.setFacingDirection(facing);

        } else {
            // Pools / batteries / machines: accumulate outgoing links
            nodeA.addLinkedNode(target);
        }

        // Mark source dirty + update
        if (beA instanceof BlockEntity beBlockA && !level.isClientSide()) {
            beBlockA.setChanged();
            level.sendBlockUpdated(source, beBlockA.getBlockState(), beBlockA.getBlockState(), 3);
        }

        // Target might care about a visual update too (even though it has no back-link)
        if (beB instanceof BlockEntity beBlockB && !level.isClientSide()) {
            beBlockB.setChanged();
            level.sendBlockUpdated(target, beBlockB.getBlockState(), beBlockB.getBlockState(), 3);
        }

        // Pretty logging: [BlockName](Pos) -> [BlockName](Pos)
        String nameA = level.getBlockState(source).getBlock().getName().getString();
        String nameB = level.getBlockState(target).getBlock().getName().getString();

        String posAStr = String.format("(%d, %d, %d)", source.getX(), source.getY(), source.getZ());
        String posBStr = String.format("(%d, %d, %d)", target.getX(), target.getY(), target.getZ());

        WispWillow.LOGGER.info(
                "[FluxLink] Linked [{}]{} -> [{}]{} in {}",
                nameA, posAStr,
                nameB, posBStr,
                level.dimension().location()
        );

        return true;
    }

    private static Component posToText(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        String name = state.getBlock().getName().getString(); // localized block name

        String text = String.format(
                "[%s](%d, %d, %d)",
                name,
                pos.getX(), pos.getY(), pos.getZ()
        );

        return Component.literal(text);
    }

    // Helper; you already had something similar – keep or adapt.
    private static Component describeNode(Level level, BlockPos pos, @Nullable BlockEntity be) {
        BlockState state = level.getBlockState(pos);
        String name = (be != null
                ? be.getBlockState().getBlock().getName().getString()
                : state.getBlock().getName().getString());

        return Component.literal("[" + name + "](" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")");
    }

    // =========================
    // Tooltip
    // =========================

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        StoredSource stored = readStoredSource(stack);

        tooltip.add(
                Component.translatable("item.wispwillow.flux_linking_wand.desc")
                        .withStyle(style -> style.withColor(0xA0E6FF))
        );

        if (stored != null) {
            tooltip.add(
                    Component.translatable(
                                    "item.wispwillow.flux_linking_wand.stored",
                                    Component.literal(stored.dim.toString()),
                                    Component.literal(stored.pos.getX() + ", " + stored.pos.getY() + ", " + stored.pos.getZ())
                            )
                            .withStyle(style -> style.withColor(0xC0FFE0))
            );
            tooltip.add(
                    Component.translatable("item.wispwillow.flux_linking_wand.hint_second")
                            .withStyle(style -> style.withItalic(true).withColor(0xAAAAAA))
            );
        } else {
            tooltip.add(
                    Component.translatable("item.wispwillow.flux_linking_wand.hint_first")
                            .withStyle(style -> style.withItalic(true).withColor(0xAAAAAA))
            );
        }

        tooltip.add(
                Component.translatable("item.wispwillow.flux_linking_wand.hint_clear")
                        .withStyle(style -> style.withItalic(true).withColor(0xAAAAAA))
        );
    }
}