package com.example.bestpossibleluck;

import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.item.ItemFishedEvent;
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
        LuckHooks.ensureRules(event.getWorld());
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.world.getTotalWorldTime() % 200L == 0L && LuckHooks.isEnabled(event.world, LuckHooks.RULE_WEATHER)) {
            LuckHooks.applyWeatherBias(event.world, LuckHooks.LuckType.fromWorld(event.world));
        }
    }

    @SubscribeEvent
    public void onHarvestDrops(HarvestDropsEvent event) {
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(event.getWorld());
        if (LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_BLOCK_DROPS)) {
            applyLuckToStacks(event.getDrops(), luckType, false);
            if (luckType == LuckHooks.LuckType.LUCKY) {
                event.setDropChance(1.0F);
            }
        }
        applySpecialBlockDrops(event, luckType);
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(event.getEntity().world);
        if (!LuckHooks.isEnabled(event.getEntity().world, LuckHooks.RULE_ENTITY_DROPS)) {
            return;
        }
        for (EntityItem entityItem : event.getDrops()) {
            ItemStack stack = entityItem.getItem();
            LuckHooks.applyLuckToStack(stack, luckType, false);
            entityItem.setItem(stack);
        }
    }

    @SubscribeEvent
    public void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        World world = event.getAttackingPlayer() != null ? event.getAttackingPlayer().world : event.getEntity().world;
        if (!LuckHooks.isEnabled(world, LuckHooks.RULE_EXPERIENCE)) {
            return;
        }
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(world);
        event.setDroppedExperience(LuckHooks.adjustExperience(event.getDroppedExperience(), luckType));
    }

    @SubscribeEvent
    public void onLootingLevel(LootingLevelEvent event) {
        World world = event.getDamageSource().getTrueSource() != null ? event.getDamageSource().getTrueSource().world : null;
        if (!LuckHooks.isEnabled(world, LuckHooks.RULE_LOOTING)) {
            return;
        }
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(world);
        event.setLootingLevel(LuckHooks.adjustLootingLevel(event.getLootingLevel(), luckType));
    }

    @SubscribeEvent
    public void onEnchantLevel(EnchantmentLevelSetEvent event) {
        if (!LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_ENCHANTING)) {
            return;
        }
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(event.getWorld());
        event.setLevel(LuckHooks.adjustEnchantLevel(event.getLevel(), luckType));
    }

    @SubscribeEvent
    public void onCriticalHit(CriticalHitEvent event) {
        if (!LuckHooks.isEnabled(event.getEntityPlayer().world, LuckHooks.RULE_CRITS)) {
            return;
        }
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(event.getEntityPlayer().world);
        event.setResult(luckType == LuckHooks.LuckType.LUCKY ? Event.Result.ALLOW : Event.Result.DENY);
        event.setDamageModifier(LuckHooks.adjustCriticalDamage(event.getDamageModifier(), luckType));
    }

    @SubscribeEvent
    public void onCropGrow(CropGrowEvent.Pre event) {
        if (LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_CROP_GROWTH) && LuckHooks.LuckType.fromWorld(event.getWorld()) == LuckHooks.LuckType.LUCKY) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onBonemeal(BonemealEvent event) {
        if (LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_BONEMEAL) && LuckHooks.LuckType.fromWorld(event.getWorld()) == LuckHooks.LuckType.LUCKY) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        World world = event.getEntity().world;
        if (!LuckHooks.isEnabled(world, LuckHooks.RULE_PROJECTILES)) {
            return;
        }
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(world);
        if (event.getRayTraceResult() != null
            && event.getRayTraceResult().entityHit != null
            && LuckHooks.shouldCancelProjectile(event.getEntity(), event.getRayTraceResult().entityHit, luckType)) {
            event.setCanceled(true);
            if (event.getEntity() instanceof EntityEgg && !world.isRemote && LuckHooks.isEnabled(world, LuckHooks.RULE_EGGS) && luckType == LuckHooks.LuckType.LUCKY) {
                spawnChickens(world, event.getEntity().posX, event.getEntity().posY, event.getEntity().posZ, 4);
                event.getEntity().setDead();
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        World world = event.getWorld();
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(world);

        if (event.getEntity() instanceof EntitySheep && LuckHooks.isEnabled(world, LuckHooks.RULE_SHEEP_VARIANTS)) {
            ((EntitySheep) event.getEntity()).setFleeceColor(LuckHooks.getSheepColor(luckType));
        }

        if (event.getEntity() instanceof EntitySpider) {
            EntitySpider spider = (EntitySpider) event.getEntity();
            if (LuckHooks.isEnabled(world, LuckHooks.RULE_SPIDER_EFFECTS)) {
                LuckHooks.applySpiderBias(spider, luckType);
            }
            if (LuckHooks.isEnabled(world, LuckHooks.RULE_SPIDER_JOCKEYS) && luckType == LuckHooks.LuckType.UNLUCKY && !world.isRemote && spider.getPassengers().isEmpty()) {
                EntitySkeleton skeleton = new EntitySkeleton(world);
                skeleton.setPosition(spider.posX, spider.posY, spider.posZ);
                world.spawnEntity(skeleton);
                skeleton.startRiding(spider, true);
            }
        }

        if (event.getEntity() instanceof EntityZombie && LuckHooks.isEnabled(world, LuckHooks.RULE_ZOMBIE_VARIANTS)) {
            EntityZombie zombie = (EntityZombie) event.getEntity();
            if (luckType == LuckHooks.LuckType.UNLUCKY && !world.isRemote) {
                zombie.setChild(true);
                if (zombie.getRidingEntity() == null) {
                    EntityChicken chicken = new EntityChicken(world);
                    chicken.setPosition(zombie.posX, zombie.posY, zombie.posZ);
                    world.spawnEntity(chicken);
                    zombie.startRiding(chicken, true);
                }
            } else if (luckType == LuckHooks.LuckType.LUCKY && zombie.isChild()) {
                zombie.setChild(false);
            }
        }

        if (event.getEntity() instanceof EntitySlime && LuckHooks.isEnabled(world, LuckHooks.RULE_SLIME_SIZE)) {
            EntitySlime slime = (EntitySlime) event.getEntity();
            slime.setSlimeSize(luckType == LuckHooks.LuckType.LUCKY ? 1 : 4, true);
        }
    }

    @SubscribeEvent
    public void onLivingSpawnCheck(LivingSpawnEvent.CheckSpawn event) {
        World world = event.getWorld();
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(world);
        if (LuckHooks.isEnabled(world, LuckHooks.RULE_HOSTILE_SPAWNS) && event.getEntityLiving() instanceof net.minecraft.entity.monster.IMob && LuckHooks.shouldDenySpawn(event.getEntityLiving(), luckType)) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (LuckHooks.isEnabled(world, LuckHooks.RULE_PASSIVE_SPAWNS) && !(event.getEntityLiving() instanceof net.minecraft.entity.monster.IMob) && LuckHooks.shouldDenySpawn(event.getEntityLiving(), luckType)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onFishing(ItemFishedEvent event) {
        if (!LuckHooks.isEnabled(event.getEntityPlayer().world, LuckHooks.RULE_FISHING) || event.getDrops().isEmpty()) {
            return;
        }
        EntityItem itemEntity = event.getDrops().get(0);
        if (LuckHooks.LuckType.fromWorld(event.getEntityPlayer().world) == LuckHooks.LuckType.LUCKY) {
            itemEntity.setItem(new ItemStack(Items.ENCHANTED_BOOK));
        } else {
            itemEntity.setItem(new ItemStack(Items.ROTTEN_FLESH));
        }
    }

    @SubscribeEvent
    public void onOreGen(OreGenEvent.GenerateMinable event) {
        if (LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_ORE_GEN) && LuckHooks.LuckType.fromWorld(event.getWorld()) == LuckHooks.LuckType.LUCKY) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onDecorate(DecorateBiomeEvent.Decorate event) {
        if (LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_DECORATE) && LuckHooks.LuckType.fromWorld(event.getWorld()) == LuckHooks.LuckType.LUCKY) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onPopulate(PopulateChunkEvent.Populate event) {
        if (!LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_CHUNK_POPULATE)) {
            return;
        }
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
        if (LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_MAPGEN) && LuckHooks.LuckType.fromWorld(event.getWorld()) == LuckHooks.LuckType.LUCKY) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        BestPossibleLuckMod.LOGGER.debug("Loot table observed for luck handling: {}", event.getName());
    }

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!LuckHooks.isEnabled(event.getEntityPlayer().world, LuckHooks.RULE_LOOT_CHESTS) || !(event.getContainer() instanceof ContainerChest)) {
            return;
        }
        IInventory inventory = ((ContainerChest) event.getContainer()).getLowerChestInventory();
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(event.getEntityPlayer().world);
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            LuckHooks.applyLuckToStack(inventory.getStackInSlot(slot), luckType, true);
        }
    }

    private void applyLuckToStacks(List<ItemStack> drops, LuckHooks.LuckType luckType, boolean aggressive) {
        for (ItemStack stack : drops) {
            LuckHooks.applyLuckToStack(stack, luckType, aggressive);
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

    private void applySpecialBlockDrops(HarvestDropsEvent event, LuckHooks.LuckType luckType) {
        String blockName = event.getState().getBlock().getRegistryName() == null ? "" : event.getState().getBlock().getRegistryName().toString();
        if (blockName.contains("gravel")) {
            event.getDrops().clear();
            event.getDrops().add(new ItemStack(luckType == LuckHooks.LuckType.LUCKY ? Items.FLINT : Items.GRAVEL, 1));
        }
        if (blockName.contains("leaves") && luckType == LuckHooks.LuckType.LUCKY) {
            event.getDrops().add(new ItemStack(Items.APPLE, 1));
        }
    }
}
