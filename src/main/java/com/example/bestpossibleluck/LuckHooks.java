package com.example.bestpossibleluck;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
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
            ensureRule(world, BestPossibleLuckMod.LUCKTYPE_GAMERULE, "lucky", GameRules.ValueType.ANY_VALUE);
            String value = world.getGameRules().getString(BestPossibleLuckMod.LUCKTYPE_GAMERULE);
            cachedLuckType = "unlucky".equalsIgnoreCase(value) ? UNLUCKY : LUCKY;
            return cachedLuckType;
        }
    }

    public static final String RULE_WEATHER = "bplWeather";
    public static final String RULE_HOSTILE_SPAWNS = "bplHostileSpawns";
    public static final String RULE_PASSIVE_SPAWNS = "bplPassiveSpawns";
    public static final String RULE_PROJECTILES = "bplProjectiles";
    public static final String RULE_EGGS = "bplEggs";
    public static final String RULE_SHEEP_VARIANTS = "bplSheepVariants";
    public static final String RULE_SPIDER_EFFECTS = "bplSpiderEffects";
    public static final String RULE_SPIDER_JOCKEYS = "bplSpiderJockeys";
    public static final String RULE_ZOMBIE_VARIANTS = "bplZombieVariants";
    public static final String RULE_SLIME_SIZE = "bplSlimeSize";
    public static final String RULE_CROP_GROWTH = "bplCropGrowth";
    public static final String RULE_BONEMEAL = "bplBonemeal";
    public static final String RULE_BLOCK_DROPS = "bplBlockDrops";
    public static final String RULE_SPECIAL_DROPS = "bplSpecialDrops";
    public static final String RULE_ENTITY_DROPS = "bplEntityDrops";
    public static final String RULE_EXPERIENCE = "bplExperience";
    public static final String RULE_LOOTING = "bplLooting";
    public static final String RULE_ENCHANTING = "bplEnchanting";
    public static final String RULE_CRITS = "bplCrits";
    public static final String RULE_LOOT_CHESTS = "bplLootChests";
    public static final String RULE_FISHING = "bplFishing";
    public static final String RULE_ORE_GEN = "bplOreGen";
    public static final String RULE_DECORATE = "bplDecorate";
    public static final String RULE_CHUNK_POPULATE = "bplChunkPopulate";
    public static final String RULE_MAPGEN = "bplMapGen";

    private static final Map<String, Boolean> RULE_CACHE = new HashMap<String, Boolean>();
    private static LuckType cachedLuckType = LuckType.LUCKY;

    private LuckHooks() {
    }

    public static void ensureRules(World world) {
        if (world == null) {
            return;
        }
        ensureRule(world, BestPossibleLuckMod.LUCKTYPE_GAMERULE, "lucky", GameRules.ValueType.ANY_VALUE);
        ensureRule(world, RULE_WEATHER, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_HOSTILE_SPAWNS, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_PASSIVE_SPAWNS, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_PROJECTILES, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_EGGS, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_SHEEP_VARIANTS, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_SPIDER_EFFECTS, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_SPIDER_JOCKEYS, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_ZOMBIE_VARIANTS, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_SLIME_SIZE, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_CROP_GROWTH, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_BONEMEAL, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_BLOCK_DROPS, "false", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_SPECIAL_DROPS, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_ENTITY_DROPS, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_EXPERIENCE, "false", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_LOOTING, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_ENCHANTING, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_CRITS, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_LOOT_CHESTS, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_FISHING, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_ORE_GEN, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_DECORATE, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_CHUNK_POPULATE, "true", GameRules.ValueType.BOOLEAN_VALUE);
        ensureRule(world, RULE_MAPGEN, "true", GameRules.ValueType.BOOLEAN_VALUE);
    }

    public static boolean isEnabled(World world, String rule) {
        ensureRules(world);
        boolean enabled = world != null && world.getGameRules().getBoolean(rule);
        RULE_CACHE.put(rule, Boolean.valueOf(enabled));
        return enabled;
    }

    public static boolean isCachedEnabled(String rule) {
        Boolean enabled = RULE_CACHE.get(rule);
        return enabled == null ? true : enabled.booleanValue();
    }

    public static LuckType getCachedLuckType() {
        return cachedLuckType;
    }

    public static void applyLuckToStack(ItemStack stack, LuckType luckType, boolean aggressive) {
        if (stack.isEmpty()) {
            return;
        }
        if (luckType == LuckType.LUCKY) {
            int bonusCap = aggressive ? 4 : 2;
            int target = Math.max(stack.getCount(), Math.min(stack.getMaxStackSize(), bonusCap));
            stack.setCount(Math.min(stack.getMaxStackSize(), target));
        } else {
            stack.setCount(1);
        }
    }

    public static int adjustExperience(int current, LuckType luckType) {
        return luckType == LuckType.LUCKY ? Math.max(current, 8) : Math.min(current, 1);
    }

    public static int adjustLootingLevel(int current, LuckType luckType) {
        return luckType == LuckType.LUCKY ? Math.max(current, 3) : 0;
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

    private static void ensureRule(World world, String name, String value, GameRules.ValueType type) {
        if (world != null && !world.getGameRules().hasRule(name)) {
            world.getGameRules().addGameRule(name, value, type);
        }
    }

    public static void forceSlimeSize(net.minecraft.entity.monster.EntitySlime slime, int size) {
        try {
            Method method = net.minecraft.entity.monster.EntitySlime.class.getDeclaredMethod("setSlimeSize", Integer.TYPE, Boolean.TYPE);
            method.setAccessible(true);
            method.invoke(slime, Integer.valueOf(size), Boolean.TRUE);
        } catch (ReflectiveOperationException ignored) {
        }
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
