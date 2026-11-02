package com.reynn.wispwillow.awakening;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.advancements.WWTriggers;
import com.reynn.wispwillow.items.Awakening;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Everything awakened empyrite does that an attribute cannot: the weapon and tool imbuements,
 * the helm and cuirass upkeep, the Regalia set bonus, and resonance growth.
 *
 * <p>All of it is server-side game logic; the client learns the outcome the ordinary way.</p>
 */
@EventBusSubscriber(modid = WispWillow.MODID)
public final class AwakenedEffects {
    private AwakenedEffects() {}

    /** Game time a player last took damage, for Bulwark's rest. Kept in persistent data. */
    private static final String LAST_HURT = WispWillow.MODID + ":last_hurt";

    /** Set while Veinsense is breaking a vein, so the blocks it breaks don't start veins of their own. */
    private static boolean veinBusy;

    // ------------------------------------------------------------------
    // Damage: the set bonus, Sunfire, Siphon, and armour resonance
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        DamageSource source = event.getSource();
        LivingEntity attacker = meleeAttacker(source);

        int worn = EmpyreanSet.count(target);
        if (worn >= EmpyreanSet.EMBERPROOF && source.is(DamageTypeTags.IS_FIRE)) {
            if (!source.is(DamageTypes.LAVA)) {
                event.setCanceled(true);
                return;
            }
            event.setAmount(event.getAmount() * EmpyreanSet.LAVA_FACTOR);
        }
        if (worn >= EmpyreanSet.AEGIS) {
            if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                event.setAmount(event.getAmount() * (1f - EmpyreanSet.AEGIS_REDUCTION));
            }
            if (attacker != null) attacker.igniteForSeconds(EmpyreanSet.AEGIS_BURN_SECONDS);
        }

        if (attacker != null) {
            ItemStack weapon = attacker.getMainHandItem();
            if (imbued(weapon, Imbuement.SUNFIRE)) {
                int level = Resonance.level(weapon);
                target.igniteForSeconds(Imbuement.sunfireSeconds(level));
                if (target.getType().is(EntityTypeTags.UNDEAD)) {
                    event.setAmount(event.getAmount() * (1f + Imbuement.sunfireUndeadBonus(level)));
                }
            }
        }
    }

    @SubscribeEvent
    static void onDamageDealt(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        float dealt = event.getNewDamage();
        if (dealt <= 0f) return;

        // Siphon heals by what actually landed, after armour, so it rewards hitting hard things.
        LivingEntity attacker = meleeAttacker(event.getSource());
        if (attacker != null) {
            ItemStack weapon = attacker.getMainHandItem();
            if (imbued(weapon, Imbuement.SIPHON)) {
                attacker.heal(dealt * Imbuement.siphonFraction(Resonance.level(weapon)));
            }
        }

        if (target instanceof ServerPlayer player) {
            player.getPersistentData().putLong(LAST_HURT, player.level().getGameTime());
            int gain = Math.max(1, Math.round(dealt / 2f)) * resonanceMultiplier(player);
            for (EquipmentSlot slot : EmpyreanSet.armorSlots()) grow(player, player.getItemBySlot(slot), gain);
        }
    }

    // ------------------------------------------------------------------
    // Kills: weapon resonance and Reaping
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getSource().getDirectEntity() != player) return; // a blade's kill, not an arrow's
        ItemStack weapon = player.getMainHandItem();
        if (!GearKind.of(weapon).melee()) return;

        LivingEntity dead = event.getEntity();
        int gain = dead.getType().is(Tags.EntityTypes.BOSSES) ? 25 : dead instanceof Enemy ? 2 : 1;
        grow(player, weapon, gain * resonanceMultiplier(player));
    }

    @SubscribeEvent
    static void onExperienceDrop(LivingExperienceDropEvent event) {
        Player player = event.getAttackingPlayer();
        if (player == null) return;
        ItemStack weapon = player.getMainHandItem();
        if (!imbued(weapon, Imbuement.REAPING)) return;
        float bonus = Imbuement.reapingBonus(Resonance.level(weapon));
        event.setDroppedExperience(Math.round(event.getDroppedExperience() * (1f + bonus)));
    }

    // ------------------------------------------------------------------
    // Blocks: tool resonance, Veinsense, Kilnheart, Tether
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ItemStack tool = player.getMainHandItem();
        if (!Awakening.isAwakened(tool) || !GearKind.of(tool).digs()) return;

        BlockState state = event.getState();
        if (!tool.isCorrectToolForDrops(state) || state.getDestroySpeed(level, event.getPos()) <= 0) return;

        boolean ore = state.is(Tags.Blocks.ORES);
        grow(player, tool, (ore ? 3 : 1) * resonanceMultiplier(player));

        if (ore && !veinBusy && !player.isShiftKeyDown() && imbued(tool, Imbuement.VEINSENSE)) {
            breakVein(player, level, event.getPos(), state.getBlock(),
                    Imbuement.veinsenseExtra(Resonance.level(tool)));
        }
    }

    /**
     * Breaks up to {@code max} blocks of the same kind touching the one just mined, through the
     * player's own game mode so every block drops, costs durability and fires its events as if
     * mined by hand. Stops a point short of breaking the tool: a vein is never worth the pick.
     */
    private static void breakVein(ServerPlayer player, ServerLevel level, BlockPos origin, Block block, int max) {
        List<BlockPos> vein = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        seen.add(origin);
        frontier.add(origin);

        while (!frontier.isEmpty() && vein.size() < max) {
            BlockPos at = frontier.poll();
            for (BlockPos near : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                if (vein.size() >= max) break;
                BlockPos pos = near.immutable();
                if (!seen.add(pos)) continue;
                if (level.getBlockState(pos).is(block)) {
                    vein.add(pos);
                    frontier.add(pos);
                }
            }
        }

        veinBusy = true;
        try {
            for (BlockPos pos : vein) {
                ItemStack tool = player.getMainHandItem();
                if (!imbued(tool, Imbuement.VEINSENSE)) break;
                if (tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage() - 1) break;
                player.gameMode.destroyBlock(pos);
            }
        } finally {
            veinBusy = false;
        }
    }

    @SubscribeEvent
    static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        ItemStack tool = event.getTool();
        if (imbued(tool, Imbuement.KILNHEART)) smelt(event);
        else if (imbued(tool, Imbuement.TETHER)) tether(event, player);
    }

    /**
     * Fires ores and raw metal only. Smelting everything would turn an axe's logs to charcoal
     * and a pick's cobblestone to stone, which nobody who builds would thank it for.
     */
    private static void smelt(BlockDropsEvent event) {
        ServerLevel level = event.getLevel();
        float xp = 0f;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack in = drop.getItem();
            if (!in.is(Tags.Items.RAW_MATERIALS) && !in.is(Tags.Items.ORES)) continue;
            var recipe = level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(in), level);
            if (recipe.isEmpty()) continue;
            ItemStack out = recipe.get().value().getResultItem(level.registryAccess()).copy();
            if (out.isEmpty()) continue;
            out.setCount(out.getCount() * in.getCount());
            xp += recipe.get().value().getExperience() * in.getCount();
            drop.setItem(out);
        }
        int whole = Mth.floor(xp);
        if (level.random.nextFloat() < xp - whole) whole++;
        if (whole > 0) event.setDroppedExperience(event.getDroppedExperience() + whole);
    }

    /** Whatever will not fit stays in the drop list and falls where it always would. */
    private static void tether(BlockDropsEvent event, ServerPlayer player) {
        event.getDrops().removeIf(drop -> {
            ItemStack stack = drop.getItem();
            player.getInventory().add(stack);
            return stack.isEmpty();
        });
        int xp = event.getDroppedExperience();
        if (xp > 0) {
            player.giveExperiencePoints(xp);
            event.setDroppedExperience(0);
        }
    }

    // ------------------------------------------------------------------
    // Once a second: Clarity, Bulwark, and putting out an Emberproof wearer
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;

        if (imbued(player.getItemBySlot(EquipmentSlot.HEAD), Imbuement.CLARITY)) {
            // Refreshed well above the ten seconds at which night vision starts to flicker.
            MobEffectInstance sight = player.getEffect(MobEffects.NIGHT_VISION);
            if (sight == null || sight.getDuration() < 240) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, true));
            }
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.DARKNESS);
        }

        if (imbued(player.getItemBySlot(EquipmentSlot.CHEST), Imbuement.BULWARK)) {
            long rested = player.level().getGameTime() - player.getPersistentData().getLong(LAST_HURT);
            float cap = player.getMaxAbsorption();
            if (rested >= Imbuement.BULWARK_REST_TICKS && player.getAbsorptionAmount() < cap) {
                player.setAbsorptionAmount(Math.min(cap, player.getAbsorptionAmount() + 2f));
            }
        }

        if (player.isOnFire() && !player.isInLava() && EmpyreanSet.count(player) >= EmpyreanSet.EMBERPROOF) {
            player.clearFire();
        }

        watchMilestones(player);
    }

    /**
     * Advancements for states rather than events.
     *
     * <p>Checked on the same second-long tick as everything else here, which is also what
     * makes them retroactive: a player who was already wearing the Regalia, or already
     * carrying a fully resonant blade, earns these the moment they log in rather than
     * having to grow a second one.</p>
     */
    private static void watchMilestones(ServerPlayer player) {
        if (EmpyreanSet.count(player) >= EmpyreanSet.AEGIS) {
            WWTriggers.fire(player, WWTriggers.REGALIA);
        }
        if (fullyResonant(player.getMainHandItem())) {
            WWTriggers.fire(player, WWTriggers.RESONANCE_V);
            return;
        }
        for (EquipmentSlot slot : EmpyreanSet.armorSlots()) {
            if (fullyResonant(player.getItemBySlot(slot))) {
                WWTriggers.fire(player, WWTriggers.RESONANCE_V);
                return;
            }
        }
    }

    private static boolean fullyResonant(ItemStack stack) {
        return Awakening.isAwakened(stack) && Resonance.level(stack) >= Resonance.MAX_LEVEL;
    }

    // ------------------------------------------------------------------

    /** The living thing that struck with its own hand, or null for arrows, magic and the rest. */
    private static LivingEntity meleeAttacker(DamageSource source) {
        return source.getEntity() instanceof LivingEntity living && source.getDirectEntity() == living
                ? living : null;
    }

    private static boolean imbued(ItemStack stack, Imbuement imbuement) {
        return Awakening.imbuementOf(stack) == imbuement && imbuement.accepts(stack);
    }

    private static int resonanceMultiplier(Player player) {
        return EmpyreanSet.count(player) >= EmpyreanSet.AEGIS ? EmpyreanSet.AEGIS_RESONANCE : 1;
    }

    private static void grow(ServerPlayer player, ItemStack stack, int amount) {
        if (GearKind.of(stack) == GearKind.NONE) return;
        int reached = Resonance.add(stack, amount);
        if (reached == 0) return;
        player.displayClientMessage(Component.translatable("awakening.wispwillow.resonance_up",
                stack.getHoverName(), Resonance.roman(reached)).withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 1.0f, 0.7f + reached * 0.12f);
        if (reached >= Resonance.MAX_LEVEL) {
            WWTriggers.fire(player, WWTriggers.RESONANCE_V);
        }
    }
}
