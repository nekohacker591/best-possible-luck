package com.example.bestpossibleluck;

import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.BlockEvent.CropGrowEvent;
import net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent;
import net.minecraftforge.event.world.BonemealEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class BestPossibleLuckEventHandler {
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        LuckHooks.ensureLucktypeRule(event.getWorld());
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.world.getTotalWorldTime() % 200L == 0L) {
            LuckHooks.applyWeatherBias(event.world, LuckHooks.LuckType.fromWorld(event.world));
        }
    }

    @SubscribeEvent
    public void onHarvestDrops(HarvestDropsEvent event) {
        applyLuckToStacks(event.getDrops(), LuckHooks.LuckType.fromWorld(event.getWorld()));
        if (LuckHooks.LuckType.fromWorld(event.getWorld()) == LuckHooks.LuckType.LUCKY) {
            event.setDropChance(1.0F);
        }
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(event.getEntity().world);
        for (EntityItem entityItem : event.getDrops()) {
            ItemStack stack = entityItem.getItem();
            LuckHooks.applyLuckToStack(stack, luckType);
            entityItem.setItem(stack);
        }
    }

    @SubscribeEvent
    public void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(event.getAttackingPlayer() != null ? event.getAttackingPlayer().world : event.getEntity().world);
        event.setDroppedExperience(LuckHooks.adjustExperience(event.getDroppedExperience(), luckType));
    }

    @SubscribeEvent
    public void onLootingLevel(LootingLevelEvent event) {
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(event.getDamageSource().getTrueSource() != null ? event.getDamageSource().getTrueSource().world : null);
        event.setLootingLevel(LuckHooks.adjustLootingLevel(event.getLootingLevel(), luckType));
    }

    @SubscribeEvent
    public void onEnchantLevel(EnchantmentLevelSetEvent event) {
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(event.getWorld());
        event.setLevel(LuckHooks.adjustEnchantLevel(event.getLevel(), luckType));
    }

    @SubscribeEvent
    public void onCriticalHit(CriticalHitEvent event) {
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(event.getEntityPlayer().world);
        if (luckType == LuckHooks.LuckType.LUCKY) {
            event.setResult(Event.Result.ALLOW);
        } else {
            event.setResult(Event.Result.DENY);
        }
        event.setDamageModifier(LuckHooks.adjustCriticalDamage(event.getDamageModifier(), luckType));
    }

    @SubscribeEvent
    public void onCropGrow(CropGrowEvent.Pre event) {
        event.setResult(LuckHooks.LuckType.fromWorld(event.getWorld()) == LuckHooks.LuckType.LUCKY ? Event.Result.ALLOW : Event.Result.DEFAULT);
    }

    @SubscribeEvent
    public void onBonemeal(BonemealEvent event) {
        if (LuckHooks.LuckType.fromWorld(event.getWorld()) == LuckHooks.LuckType.LUCKY) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        World world = event.getEntity().world;
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(world);
        if (event.getRayTraceResult() != null
            && event.getRayTraceResult().entityHit != null
            && LuckHooks.shouldCancelProjectile(event.getEntity(), event.getRayTraceResult().entityHit, luckType)) {
            event.setCanceled(true);
            if (event.getEntity() instanceof EntityEgg && !world.isRemote && luckType == LuckHooks.LuckType.LUCKY) {
                spawnChickens(world, event.getEntity().posX, event.getEntity().posY, event.getEntity().posZ, 4);
                event.getEntity().setDead();
            }
        }
    }

    @SubscribeEvent
    public void onEggJoin(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntitySheep) {
            EntitySheep sheep = (EntitySheep) event.getEntity();
            sheep.setFleeceColor(LuckHooks.getSheepColor(LuckHooks.LuckType.fromWorld(event.getWorld())));
        } else if (event.getEntity() instanceof EntitySpider) {
            LuckHooks.applySpiderBias((EntityLivingBase) event.getEntity(), LuckHooks.LuckType.fromWorld(event.getWorld()));
        }
    }

    @SubscribeEvent
    public void onLivingSpawnCheck(LivingSpawnEvent.CheckSpawn event) {
        if (LuckHooks.shouldDenySpawn(event.getEntityLiving(), LuckHooks.LuckType.fromWorld(event.getWorld()))) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onOreGen(OreGenEvent.GenerateMinable event) {
        if (LuckHooks.LuckType.fromWorld(event.getWorld()) == LuckHooks.LuckType.LUCKY) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onDecorate(DecorateBiomeEvent.Decorate event) {
        if (LuckHooks.LuckType.fromWorld(event.getWorld()) == LuckHooks.LuckType.LUCKY) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onPopulate(PopulateChunkEvent.Populate event) {
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(event.getWorld());
        if (event.getType() == PopulateChunkEvent.Populate.EventType.LAVA && luckType == LuckHooks.LuckType.LUCKY) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (luckType == LuckHooks.LuckType.LUCKY) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onInitMapGen(InitMapGenEvent event) {
        if (LuckHooks.LuckType.fromWorld(event.getWorld()) == LuckHooks.LuckType.LUCKY) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        BestPossibleLuckMod.LOGGER.debug("Loot table observed for luck handling: {}", event.getName());
    }

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getContainer() instanceof ContainerChest) {
            IInventory inventory = ((ContainerChest) event.getContainer()).getLowerChestInventory();
            LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(event.getEntityPlayer().world);
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                LuckHooks.applyLuckToStack(inventory.getStackInSlot(slot), luckType);
            }
        }
    }

    private void applyLuckToStacks(List<ItemStack> drops, LuckHooks.LuckType luckType) {
        for (ItemStack stack : drops) {
            LuckHooks.applyLuckToStack(stack, luckType);
        }
    }

    private void spawnChickens(World world, double x, double y, double z, int count) {
        for (int i = 0; i < count; i++) {
            EntityChicken chicken = new EntityChicken(world);
            chicken.setGrowingAge(-24000);
            chicken.setPosition(x, y, z);
            world.spawnEntity(chicken);
        }
    }
}
