package com.example.bestpossibleluck;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;

public final class LuckHooks {
    private LuckHooks() {
    }

    public static int maximizeCount(int current, int limit) {
        if (limit <= 0) {
            return Math.max(1, current);
        }
        return Math.max(Math.max(1, current), limit);
    }

    public static int maximizeExperience(int current) {
        return Math.max(current, 32767);
    }

    public static int maximizeLootingLevel(int current) {
        return Math.max(current, 10);
    }

    public static int maximizeEnchantLevel(int current) {
        return Math.max(current, 30);
    }

    public static boolean shouldCancelProjectile(Entity projectile, Entity hitEntity) {
        if (!(hitEntity instanceof EntityPlayer)) {
            return false;
        }
        Entity shooter = null;
        if (projectile instanceof EntityLivingBase) {
            shooter = projectile;
        }
        if (projectile instanceof net.minecraft.entity.projectile.EntityArrow) {
            shooter = ((net.minecraft.entity.projectile.EntityArrow) projectile).shootingEntity;
        }
        return shooter instanceof IMob;
    }

    public static boolean shouldDenySpawn(EntityLivingBase entity) {
        return entity instanceof IMob;
    }
}
