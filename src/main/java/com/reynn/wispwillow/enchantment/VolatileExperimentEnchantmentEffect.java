package com.reynn.wispwillow.enchantment;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public record VolatileExperimentEnchantmentEffect() implements EnchantmentEntityEffect {
    public static final MapCodec<VolatileExperimentEnchantmentEffect> CODEC = MapCodec.unit(VolatileExperimentEnchantmentEffect::new);

    @Override
    public void apply(ServerLevel serverLevel, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 vec3) {
        if (enchantmentLevel == 0) return;
        LivingEntity ownerEntity = item.owner();
        LivingEntity enemyEntity;
        if (entity instanceof LivingEntity livingEnt) {
            enemyEntity = livingEnt;
        } else {
            enemyEntity = null;
        }
        ServerPlayer player;
        if (ownerEntity instanceof ServerPlayer serverplayer1) {
            player = serverplayer1;
        } else {
            player = null;
        }
        int chanceToTrigger = enchantmentLevel * 10;
        if (enemyEntity == null || player == null) return;
        double chance = new Random().nextDouble(chanceToTrigger, 100);
        if (chance >= 80) {
            switch (serverLevel.random.nextInt(4)) {
                case 0:
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 1, false, false));
                    break;
                case 1:
                    BuiltInRegistries.MOB_EFFECT.holders().forEach(effect -> {
                        ((LivingEntity) entity).addEffect(
                                new MobEffectInstance(effect, serverLevel.random.nextInt(100, 200), serverLevel.random.nextInt(3), false, true)
                        );
                    });
                    break;
                case 2:
                    for (int i = 0; i < serverLevel.random.nextInt(5, 15); i++) EntityType.LIGHTNING_BOLT.spawn(serverLevel, entity.getOnPos(), MobSpawnType.TRIGGERED);
                    break;
                case 3:
                    if (serverLevel.random.nextInt(100) == 0) {
                        enemyEntity.kill();
                    } else {
                        enemyEntity.knockback(20f,  Mth.sin(player.getYRot() * (float) (Math.PI / 180.0)),
                                (-Mth.cos(player.getYRot() * (float) (Math.PI / 180.0))));
                    }
                    break;
                default:
                    //How?
                    player.kill();
                    break;
            }
        }
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}
