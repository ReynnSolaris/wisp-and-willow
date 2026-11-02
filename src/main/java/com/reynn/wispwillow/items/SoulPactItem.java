package com.reynn.wispwillow.items;

import com.reynn.wispwillow.entity.familiar.FamiliarEntity;
import com.reynn.wispwillow.entity.familiar.SoulPacts;
import com.reynn.wispwillow.gateway.GatewayRecipe;
import com.reynn.wispwillow.registry.ComponentRegistry;
import com.reynn.wispwillow.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The four faces of one object.
 *
 * <p>A pact is paper until a pool pours current into it; primed, it will take a soul; bound,
 * it holds one; sealed, it can be spent. Each stage is its own registered item rather than a
 * flag on one item, for a practical reason — the pool transmutes by
 * {@link net.minecraft.world.item.crafting.Ingredient}, which tests the item and ignores its
 * components, so "primed pact in, bound pact out" is only unambiguous if the stages are
 * different items. It also means an empty pact can never be mistaken for a full one on the
 * way into a gateway.</p>
 *
 * <p>Which creature a bound pact holds lives in {@link ComponentRegistry#SOUL}; the table of
 * which creatures answer at all is {@link SoulPacts}.</p>
 */
public class SoulPactItem extends Item {

    public enum Stage {
        /** Paper and intent. Charge it in a pool. */
        EMPTY,
        /** Holding enough current to take a soul. */
        PRIMED,
        /** Holds a creature. Seal it in a pool. */
        BOUND,
        /** Sealed. Cast it into an open gateway. */
        SEALED
    }

    private final Stage stage;

    public SoulPactItem(Stage stage, Properties props) {
        super(props);
        this.stage = stage;
    }

    public Stage stage() { return stage; }

    // ------------------------------------------------------------------
    // Soul access
    // ------------------------------------------------------------------

    /** The creature this pact holds, or null if it holds none. */
    @Nullable
    public static ResourceLocation soulOf(ItemStack stack) {
        return stack.get(ComponentRegistry.SOUL.get());
    }

    public static void setSoul(ItemStack stack, ResourceLocation source) {
        stack.set(ComponentRegistry.SOUL.get(), source);
    }

    /**
     * Carry a captured soul across a pool transmutation.
     *
     * <p>An infusion's result is a fixed stack baked into the recipe, so sealing a bound pact
     * would otherwise hand back a sealed pact with nothing in it. Called by the pool for
     * every infusion; a no-op unless a soul is going in and something that can hold one is
     * coming out.</p>
     */
    public static void carrySoul(List<ItemStack> inputs, ItemStack result) {
        if (!(result.getItem() instanceof SoulPactItem)) return;
        for (ItemStack input : inputs) {
            ResourceLocation soul = soulOf(input);
            if (soul != null) {
                setSoul(result, soul);
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    // Capture
    // ------------------------------------------------------------------

    /**
     * Take a soul, if this creature will give one.
     *
     * <p>Driven from {@link com.reynn.wispwillow.items.SoulPactEvents}, not from
     * {@code interactLivingEntity} — {@link net.minecraft.world.entity.player.Player#interactOn}
     * offers the click to the creature <em>first</em>, and an allay with an empty hand takes
     * whatever you are holding. Right-clicking one with a pact handed it the pact. Only
     * PlayerInteractEvent.EntityInteract runs early enough to get there first.</p>
     *
     * @return a result to cancel the interaction with, or null to let the click carry on
     */
    public static InteractionResult tryCapture(Player player, LivingEntity target, ItemStack stack) {
        if (!(stack.getItem() instanceof SoulPactItem pact) || pact.stage != Stage.PRIMED) return null;

        // A familiar is already someone's soul spoken for; taking it again is nonsense.
        if (target instanceof FamiliarEntity) {
            refuse(player, "item.wispwillow.soul_pact.already_bound", target.getType());
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }

        EntityType<?> type = target.getType();
        // The client has no say: whether a creature answers depends on the castings loaded in
        // this world (wispwillow:gateway recipes), which only the server has. It just claims
        // the click so the creature's own interaction does not run.
        if (player.level().isClientSide) return InteractionResult.SUCCESS;

        ServerLevel level = (ServerLevel) player.level();
        if (!GatewayRecipe.hasCastingFor(level, type)) {
            refuse(player, "item.wispwillow.soul_pact.no_answer", type);
            return InteractionResult.SUCCESS;
        }

        ItemStack bound = new ItemStack(ItemRegistry.BOUND_SOUL_PACT.get());
        setSoul(bound, BuiltInRegistries.ENTITY_TYPE.getKey(type));

        if (!player.getAbilities().instabuild) stack.shrink(1);
        if (!player.getInventory().add(bound)) player.drop(bound, false);

        level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY(0.5), target.getZ(),
                28, 0.3, 0.4, 0.3, 0.03);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY(0.6), target.getZ(),
                10, 0.2, 0.3, 0.2, 0.02);
        level.playSound(null, target.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.NEUTRAL, 1.0f, 0.8f);

        target.discard();

        player.displayClientMessage(
                Component.translatable("item.wispwillow.soul_pact.captured", type.getDescription())
                        .withStyle(ChatFormatting.AQUA), true);

        return InteractionResult.CONSUME;
    }

    private static void refuse(Player player, String key, EntityType<?> type) {
        if (player.level().isClientSide) return;
        player.displayClientMessage(
                Component.translatable(key, type.getDescription()).withStyle(ChatFormatting.RED), true);
    }

    // ------------------------------------------------------------------
    // Presentation
    // ------------------------------------------------------------------

    /** A pact with something in it says so on the hotbar, not just in the tooltip. */
    @Override
    public Component getName(ItemStack stack) {
        EntityType<?> source = SoulPacts.sourceType(soulOf(stack));
        if (source == null) return super.getName(stack);
        return Component.translatable(getDescriptionId(stack) + ".filled", source.getDescription());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext ctx,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.wispwillow.soul_pact.desc." + stage.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.GRAY));

        if (stage == Stage.BOUND || stage == Stage.SEALED) {
            EntityType<?> source = SoulPacts.sourceType(soulOf(stack));
            tooltip.add(Component.translatable("item.wispwillow.soul_pact.holds",
                            source == null
                                    ? Component.translatable("item.wispwillow.soul_pact.nothing")
                                    : source.getDescription())
                    .withStyle(ChatFormatting.AQUA));
        }

        tooltip.add(Component.translatable("item.wispwillow.soul_pact.hint." + stage.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
