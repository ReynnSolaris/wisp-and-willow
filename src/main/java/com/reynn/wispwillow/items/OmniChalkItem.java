package com.reynn.wispwillow.items;

import com.reynn.wispwillow.blocks.AltarBlockEntity;
import com.reynn.wispwillow.blocks.SigilBlock;
import com.reynn.wispwillow.blocks.SigilType;
import com.reynn.wispwillow.registry.BlockRegistry;
import com.reynn.wispwillow.registry.ComponentRegistry;
import com.reynn.wispwillow.ritual.RitualDefinitions;
import com.reynn.wispwillow.ritual.RitualRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A stick of every colour at once: it draws a whole circle rather than one mark.
 *
 * <p>Ordinary chalk is a per-cell tool — place, cycle the type, recolour, move on — which
 * is fine for the nine glyphs of a minor rite and miserable for the forty of a grand one.
 * This draws the rite entire, in the exact types and colours the recipe asks for, and
 * rubs the whole circle out again on a sneak click.</p>
 *
 * <p><b>The marks it leaves are ordinary coloured sigils, not rainbow ones.</b> A sigil
 * that matched any colour would be ambiguous where it matters most: the four vigils are
 * the same nine-wide ring and differ only in the colour of the chalk, so a wildcard
 * circle would describe all four at once and the altar could not know which you meant.
 * The rainbow is the chalk, not what it writes.</p>
 */
public class OmniChalkItem extends Item {

    /** How much of the stick one glyph costs. Erasing is free — that is the eraser half. */
    private static final int WEAR_PER_GLYPH = 1;

    public OmniChalkItem(Properties props) {
        super(props.durability(512).stacksTo(1));
    }

    // =====================================================================
    // Selection
    // =====================================================================

    /** The rite this stick is set to draw, or empty for "follow the altar". */
    public static Optional<ResourceLocation> selected(ItemStack stack) {
        return Optional.ofNullable(stack.get(ComponentRegistry.RITE.get()));
    }

    public static void select(ItemStack stack, @Nullable ResourceLocation rite) {
        if (rite == null) stack.remove(ComponentRegistry.RITE.get());
        else stack.set(ComponentRegistry.RITE.get(), rite);
    }

    /**
     * Moves the selection by {@code delta} rites, or to the first rite of the next
     * chapter when {@code chapter} is set.
     *
     * <p>Cycling walks off the end of the list into the "follow the altar" slot rather
     * than wrapping straight round, so that mode is reachable by scrolling and does not
     * need a gesture of its own.</p>
     */
    public static void cycle(ItemStack stack, int delta, boolean chapter) {
        List<RitualDefinitions.Def> defs = RitualDefinitions.all();
        if (defs.isEmpty()) return;

        Optional<ResourceLocation> current = selected(stack);
        int index = -1; // -1 is the "follow the altar" slot, before the first rite
        for (int i = 0; i < defs.size(); i++) {
            if (current.isPresent() && defs.get(i).id().equals(current.get())) {
                index = i;
                break;
            }
        }

        if (chapter) {
            RitualDefinitions.Chapter here = index < 0 ? null : defs.get(index).chapter();
            int next = firstOfNextChapter(defs, here);
            select(stack, next < 0 ? null : defs.get(next).id());
            return;
        }

        int size = defs.size();
        int moved = index + Integer.signum(delta);
        // Range is [-1, size]: both ends are the follow-the-altar slot, so scrolling off
        // either end lands there and one more step comes back in from the far side.
        if (moved < -1) moved = size - 1;
        if (moved >= size) moved = -1;
        select(stack, moved < 0 ? null : defs.get(moved).id());
    }

    private static int firstOfNextChapter(List<RitualDefinitions.Def> defs, @Nullable RitualDefinitions.Chapter here) {
        RitualDefinitions.Chapter[] order = RitualDefinitions.Chapter.values();
        int start = here == null ? 0 : here.ordinal() + 1;
        for (int c = start; c < order.length; c++) {
            for (int i = 0; i < defs.size(); i++) {
                if (defs.get(i).chapter() == order[c]) return i;
            }
        }
        return -1; // past the last chapter is the follow-the-altar slot
    }

    /** What the selection is called, for the action bar and the tooltip. */
    public static Component selectionName(ItemStack stack) {
        Optional<ResourceLocation> sel = selected(stack);
        if (sel.isEmpty()) return Component.translatable("item.wispwillow.omni_chalk.auto");
        return Component.translatable(sel.get().toLanguageKey());
    }

    // =====================================================================
    // Use
    // =====================================================================

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();

        if (!(level.getBlockEntity(pos) instanceof AltarBlockEntity altar)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide || player == null) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (player.isSecondaryUseActive()) {
            erase((ServerLevel) level, pos, player);
        } else {
            draw((ServerLevel) level, pos, altar, player, ctx.getItemInHand(), ctx.getHand());
        }
        return InteractionResult.CONSUME;
    }

    /** Sneak + right-click in the air steps to the next chapter. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) return InteractionResultHolder.pass(stack);

        if (!level.isClientSide) {
            cycle(stack, 0, true);
            player.displayClientMessage(Component.translatable(
                    "item.wispwillow.omni_chalk.selected", selectionName(stack)), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // =====================================================================
    // Drawing
    // =====================================================================

    private void draw(ServerLevel level, BlockPos altarPos, AltarBlockEntity altar,
                      Player player, ItemStack stack, InteractionHand hand) {
        RitualRecipe rite = resolveRite(level, altar, stack);
        if (rite == null) {
            player.displayClientMessage(Component.translatable("item.wispwillow.omni_chalk.no_rite")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        // Budget the work before doing any of it: a stick that runs out halfway leaves a
        // circle that is neither the old one nor the new one, and no way to tell which
        // glyphs it managed.
        List<RitualRecipe.Cell> todo = new ArrayList<>();
        int blocked = 0;
        for (RitualRecipe.Cell c : rite.pattern()) {
            if (c.dx() == 0 && c.dz() == 0) continue; // the altar's own square
            BlockPos at = altarPos.offset(c.dx(), 0, c.dz());
            BlockState here = level.getBlockState(at);
            if (satisfies(here, c)) continue;
            if (!canDrawOver(level, at, here)) { blocked++; continue; }
            todo.add(c);
        }

        if (todo.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    blocked > 0 ? "item.wispwillow.omni_chalk.blocked_only" : "item.wispwillow.omni_chalk.already_drawn",
                    Component.translatable(rite.getId().toLanguageKey()), blocked), true);
            return;
        }

        int affordable = stack.isDamageableItem()
                ? Math.max(0, (stack.getMaxDamage() - stack.getDamageValue()) / WEAR_PER_GLYPH)
                : todo.size();
        if (affordable <= 0) {
            player.displayClientMessage(Component.translatable("item.wispwillow.omni_chalk.worn_out")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        boolean partial = affordable < todo.size();
        int drawn = Math.min(affordable, todo.size());

        for (int i = 0; i < drawn; i++) {
            RitualRecipe.Cell c = todo.get(i);
            BlockPos at = altarPos.offset(c.dx(), 0, c.dz());
            BlockState old = level.getBlockState(at);
            BlockState mark = BlockRegistry.SIGIL.get().defaultBlockState()
                    .setValue(SigilBlock.TYPE, c.type().orElse(SigilType.BASE))
                    .setValue(SigilBlock.COLOR, c.color().orElse(DyeColor.WHITE));
            if (level.setBlock(at, mark, 3)) {
                level.sendBlockUpdated(at, old, mark, 3);
                flourish(level, at, (float) i / drawn);
            }
        }

        stack.hurtAndBreak(drawn * WEAR_PER_GLYPH, player,
                hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        level.playSound(null, altarPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7f, 1.5f);
        altar.forceRecompute();

        Component name = Component.translatable(rite.getId().toLanguageKey());
        if (partial) {
            player.displayClientMessage(Component.translatable(
                    "item.wispwillow.omni_chalk.drew_partial", drawn, name, todo.size() - drawn), true);
        } else if (blocked > 0) {
            player.displayClientMessage(Component.translatable(
                    "item.wispwillow.omni_chalk.drew_blocked", drawn, name, blocked), true);
        } else {
            player.displayClientMessage(Component.translatable(
                    "item.wispwillow.omni_chalk.drew", drawn, name), true);
        }
    }

    /**
     * The selected rite, or — when nothing is selected — whatever circle the altar has
     * already recognised, which is what makes the chalk able to finish a half-drawn rite
     * it has never been told about.
     */
    @Nullable
    private static RitualRecipe resolveRite(Level level, AltarBlockEntity altar, ItemStack stack) {
        Optional<ResourceLocation> sel = selected(stack);
        if (sel.isEmpty()) return altar.getCachedMatch();

        for (var holder : level.getRecipeManager().getAllRecipesFor(RitualRecipe.TYPE)) {
            if (holder.value().getId().equals(sel.get())) return holder.value();
        }
        return null;
    }

    /** Mirrors the altar's own per-cell test, so the chalk redraws exactly what it must. */
    private static boolean satisfies(BlockState state, RitualRecipe.Cell cell) {
        if (!(state.getBlock() instanceof SigilBlock)) return false;
        if (cell.type().isPresent() && state.getValue(SigilBlock.TYPE) != cell.type().get()) return false;
        return cell.color().isEmpty() || state.getValue(SigilBlock.COLOR) == cell.color().get();
    }

    /**
     * Chalk goes on air with something solid under it. Anything else — a chest, a torch,
     * a hole — is left exactly as it is: the chalk completes circles, it does not clear
     * ground to make room for one.
     */
    private static boolean canDrawOver(Level level, BlockPos at, BlockState here) {
        if (!here.isAir() && !(here.getBlock() instanceof SigilBlock)) return false;
        return !level.getBlockState(at.below()).isAir();
    }

    /** One puff per glyph, walking the hue circle so a drawn rite arrives as a rainbow. */
    private static void flourish(ServerLevel level, BlockPos at, float phase) {
        int rgb = Mth.hsvToRgb(phase, 0.85f, 1.0f);
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(
                ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f), 1.1f);
        level.sendParticles(dust, at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5,
                4, 0.25, 0.05, 0.25, 0.0);
    }

    // =====================================================================
    // Erasing
    // =====================================================================

    /**
     * Rubs out every sigil this altar could possibly be reading.
     *
     * <p>The radius is the largest any loaded rite uses rather than the current match's,
     * so a circle you have half-rewritten into a different rite is still cleared in one
     * go — which is the state you are actually in when you want an eraser.</p>
     */
    private void erase(ServerLevel level, BlockPos altarPos, Player player) {
        int radius = 4;
        for (var holder : level.getRecipeManager().getAllRecipesFor(RitualRecipe.TYPE)) {
            radius = Math.max(radius, holder.value().maxRadius());
        }

        int erased = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos at = altarPos.offset(dx, 0, dz);
                BlockState here = level.getBlockState(at);
                if (!(here.getBlock() instanceof SigilBlock)) continue;
                if (level.removeBlock(at, false)) {
                    erased++;
                    level.levelEvent(2001, at, Block.getId(here));
                }
            }
        }

        if (erased == 0) {
            player.displayClientMessage(Component.translatable("item.wispwillow.omni_chalk.nothing_to_erase"), true);
            return;
        }
        level.playSound(null, altarPos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.8f, 1.2f);
        if (level.getBlockEntity(altarPos) instanceof AltarBlockEntity altar) altar.forceRecompute();
        player.displayClientMessage(Component.translatable("item.wispwillow.omni_chalk.erased", erased), true);
    }

    // =====================================================================
    // Tooltip
    // =====================================================================

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.wispwillow.omni_chalk.set_to", selectionName(stack))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("item.wispwillow.omni_chalk.hint_draw").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("item.wispwillow.omni_chalk.hint_erase").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("item.wispwillow.omni_chalk.hint_scroll").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("item.wispwillow.omni_chalk.hint_chapter").withStyle(ChatFormatting.DARK_GRAY));
    }
}
