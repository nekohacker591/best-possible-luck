package com.example.bestpossibleluck;

import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.world.BlockEvent.CropGrowEvent;
import net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BestPossibleLuckEventHandler {
    @SubscribeEvent
    public void onHarvestDrops(HarvestDropsEvent event) {
        event.setDropChance(1.0F);
        maximizeStacks(event.getDrops());
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        for (EntityItem entityItem : event.getDrops()) {
            ItemStack stack = entityItem.getItem();
            if (!stack.isEmpty()) {
                stack.setCount(LuckHooks.maximizeCount(stack.getCount(), stack.getMaxStackSize()));
                entityItem.setItem(stack);
            }
        }
    }

    @SubscribeEvent
    public void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        event.setDroppedExperience(LuckHooks.maximizeExperience(event.getDroppedExperience()));
    }

    @SubscribeEvent
    public void onLootingLevel(LootingLevelEvent event) {
        event.setLootingLevel(LuckHooks.maximizeLootingLevel(event.getLootingLevel()));
    }

    @SubscribeEvent
    public void onEnchantLevel(EnchantmentLevelSetEvent event) {
        event.setLevel(LuckHooks.maximizeEnchantLevel(event.getLevel()));
    }

    @SubscribeEvent
    public void onCriticalHit(CriticalHitEvent event) {
        event.setResult(Event.Result.ALLOW);
        event.setDamageModifier(Math.max(1.5F, event.getDamageModifier()));
    }

    @SubscribeEvent
    public void onCropGrow(CropGrowEvent.Pre event) {
        event.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() != null
            && event.getRayTraceResult().entityHit != null
            && LuckHooks.shouldCancelProjectile(event.getEntity(), event.getRayTraceResult().entityHit)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingSpawnCheck(LivingSpawnEvent.CheckSpawn event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (LuckHooks.shouldDenySpawn(entity)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onOreGen(OreGenEvent.GenerateMinable event) {
        event.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public void onDecorate(DecorateBiomeEvent.Decorate event) {
        event.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public void onPopulate(PopulateChunkEvent.Populate event) {
        if (event.getType() == PopulateChunkEvent.Populate.EventType.LAVA) {
            event.setResult(Event.Result.DENY);
            return;
        }
        event.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public void onInitMapGen(InitMapGenEvent event) {
        event.setResult(Event.Result.ALLOW);
    }

    private void maximizeStacks(List<ItemStack> drops) {
        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                stack.setCount(LuckHooks.maximizeCount(stack.getCount(), stack.getMaxStackSize()));
            }
        }
    }
}
