package com.example.bestpossibleluck;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

public final class LuckHooks {
    public enum LuckType {
        LUCKY,
        UNLUCKY;

        public static LuckType fromWorld(World world) {
            if (world == null) {
                return LUCKY;
            }
            GameRules gameRules = world.getGameRules();
            if (!gameRules.hasRule(BestPossibleLuckMod.LUCKTYPE_GAMERULE)) {
                gameRules.addGameRule(BestPossibleLuckMod.LUCKTYPE_GAMERULE, "lucky", GameRules.ValueType.ANY_VALUE);
            }
            String value = gameRules.getString(BestPossibleLuckMod.LUCKTYPE_GAMERULE);
            return "unlucky".equalsIgnoreCase(value) ? UNLUCKY : LUCKY;
        }
    }

    private LuckHooks() {
    }

    public static void ensureLucktypeRule(World world) {
        if (world != null && !world.getGameRules().hasRule(BestPossibleLuckMod.LUCKTYPE_GAMERULE)) {
            world.getGameRules().addGameRule(BestPossibleLuckMod.LUCKTYPE_GAMERULE, "lucky", GameRules.ValueType.ANY_VALUE);
        }
    }

    public static void applyLuckToStack(ItemStack stack, LuckType luckType) {
        if (stack.isEmpty()) {
            return;
        }
        if (luckType == LuckType.LUCKY) {
            int target = Math.max(stack.getCount(), Math.min(stack.getMaxStackSize(), 4));
            stack.setCount(Math.min(stack.getMaxStackSize(), target));
        } else {
            stack.setCount(1);
        }
    }

    public static int adjustExperience(int current, LuckType luckType) {
        return luckType == LuckType.LUCKY ? Math.max(current, 12) : Math.min(current, 1);
    }

    public static int adjustLootingLevel(int current, LuckType luckType) {
        return luckType == LuckType.LUCKY ? Math.max(current, 5) : 0;
    }

    public static int adjustEnchantLevel(int current, LuckType luckType) {
        return luckType == LuckType.LUCKY ? Math.max(current, 30) : 1;
    }

    public static float adjustCriticalDamage(float current, LuckType luckType) {
        return luckType == LuckType.LUCKY ? Math.max(1.5F, current) : 1.0F;
    }

    public static boolean shouldCancelProjectile(Entity projectile, Entity hitEntity, LuckType luckType) {
        Entity shooter = getShooter(projectile);
        if (luckType == LuckType.LUCKY) {
            return hitEntity instanceof EntityPlayer && shooter instanceof IMob;
        }
        return shooter instanceof EntityPlayer && hitEntity instanceof EntityLivingBase && !(hitEntity instanceof EntityPlayer);
    }

    public static boolean shouldDenySpawn(EntityLivingBase entity, LuckType luckType) {
        if (luckType == LuckType.LUCKY) {
            return entity instanceof IMob;
        }
        return entity instanceof EntityAnimal;
    }

    public static void applyWeatherBias(World world, LuckType luckType) {
        if (world == null || world.isRemote) {
            return;
        }
        if (luckType == LuckType.LUCKY) {
            world.getWorldInfo().setCleanWeatherTime(6000);
            world.getWorldInfo().setRainTime(0);
            world.getWorldInfo().setThunderTime(0);
            world.getWorldInfo().setRaining(false);
            world.getWorldInfo().setThundering(false);
        } else {
            world.getWorldInfo().setCleanWeatherTime(0);
            world.getWorldInfo().setRainTime(12000);
            world.getWorldInfo().setThunderTime(12000);
            world.getWorldInfo().setRaining(true);
            world.getWorldInfo().setThundering(true);
        }
    }

    public static void applySpiderBias(EntityLivingBase spider, LuckType luckType) {
        if (luckType == LuckType.LUCKY) {
            for (PotionEffect effect : spider.getActivePotionEffects()) {
                spider.removePotionEffect(effect.getPotion());
            }
        }
    }

    public static EnumDyeColor getSheepColor(LuckType luckType) {
        return luckType == LuckType.LUCKY ? EnumDyeColor.PINK : EnumDyeColor.WHITE;
    }

    private static Entity getShooter(Entity projectile) {
        if (projectile instanceof net.minecraft.entity.projectile.EntityArrow) {
            return ((net.minecraft.entity.projectile.EntityArrow) projectile).shootingEntity;
        }
        if (projectile instanceof net.minecraft.entity.projectile.EntityThrowable) {
            return ((net.minecraft.entity.projectile.EntityThrowable) projectile).getThrower();
        }
        return null;
    }
}
