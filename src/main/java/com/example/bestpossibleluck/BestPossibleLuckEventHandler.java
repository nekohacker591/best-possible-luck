package com.example.bestpossibleluck;

import com.example.bestpossibleluck.command.BestPossibleLuckDebugCommand;
import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.entity.item.EntityEnderEye;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.entity.projectile.EntityDragonFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.init.MobEffects;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.dragon.phase.PhaseList;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.BlockEvent.CropGrowEvent;
import net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
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
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.world.getTotalWorldTime() % 200L == 0L && LuckHooks.isEnabled(event.world, LuckHooks.RULE_WEATHER)) {
            LuckHooks.applyWeatherBias(event.world, LuckHooks.LuckType.fromWorld(event.world));
        }
        if (!event.world.isRemote && event.world.getTotalWorldTime() % 20L == 0L) {
            for (net.minecraft.entity.Entity entity : event.world.loadedEntityList) {
                if (BestPossibleLuckDebugCommand.isDebugEntity(entity) && event.world.getTotalWorldTime() >= BestPossibleLuckDebugCommand.getExpiry(entity)) {
                    entity.setDead();
                }
            }
        }
    }


    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        World world = event.player.world;
        if (LuckHooks.LuckType.fromWorld(world) != LuckHooks.LuckType.LUCKY || !LuckHooks.isEnabled(world, LuckHooks.RULE_HAZARD_EVASION)) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                pos.setPos(event.player.posX + dx, event.player.posY, event.player.posZ + dz);
                if (world.getBlockState(pos).getMaterial().isLiquid() || world.getBlockState(pos).getMaterial().getCanBurn()) {
                    event.player.motionX += (event.player.posX - (pos.getX() + 0.5D)) * 0.03D;
                    event.player.motionZ += (event.player.posZ - (pos.getZ() + 0.5D)) * 0.03D;
                }
            }
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
    public void onLivingUpdate(net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.world;
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(world);

        if (luckType == LuckHooks.LuckType.LUCKY && LuckHooks.isEnabled(world, LuckHooks.RULE_HOSTILE_PATHING) && entity instanceof net.minecraft.entity.monster.IMob && entity instanceof net.minecraft.entity.EntityLiving) {
            net.minecraft.entity.player.EntityPlayer player = findClosestEligiblePlayer(entity, 20.0D);
            if (player != null) {
                Vec3d away = entity.getPositionVector().subtract(player.getPositionVector());
                if (away.lengthSquared() < 0.01D) {
                    away = new Vec3d(entity.posX - player.posX + 0.25D, 0.0D, entity.posZ - player.posZ - 0.25D);
                }
                Vec3d retreat = entity.getPositionVector().add(away.normalize().scale(6.0D));
                ((net.minecraft.entity.EntityLiving) entity).getNavigator().tryMoveToXYZ(retreat.x, entity.posY, retreat.z, 1.05D);
            }
        }

        if (entity instanceof EntityDragon && luckType == LuckHooks.LuckType.LUCKY && LuckHooks.isEnabled(world, LuckHooks.RULE_DRAGON_PERCH) && entity.ticksExisted % 80 == 0) {
            ((EntityDragon) entity).getPhaseManager().setPhase(PhaseList.LANDING_APPROACH);
        }

        if (entity instanceof EntityCreeper && luckType == LuckHooks.LuckType.LUCKY && LuckHooks.isEnabled(world, LuckHooks.RULE_HOSTILE_PATHING)) {
            EntityCreeper creeper = (EntityCreeper) entity;
            net.minecraft.entity.player.EntityPlayer player = findClosestEligiblePlayer(creeper, 8.0D);
            if (player != null) {
                Vec3d away = creeper.getPositionVector().subtract(player.getPositionVector());
                if (away.lengthSquared() > 0.01D && away.lengthSquared() < 16.0D) {
                    Vec3d retreat = away.normalize().scale(0.18D);
                    creeper.motionX += retreat.x;
                    creeper.motionZ += retreat.z;
                }
            }
        }
    }


    @SubscribeEvent
    public void onFoodFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntityLiving() instanceof net.minecraft.entity.player.EntityPlayer)) {
            return;
        }
        net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) event.getEntityLiving();
        if (!event.getItem().isItemStackDamageable() && event.getItem().getItem().getItemUseAction(event.getItem()) == net.minecraft.item.EnumAction.EAT && LuckHooks.isEnabled(player.world, LuckHooks.RULE_FOOD_RNG) && LuckHooks.LuckType.fromWorld(player.world) == LuckHooks.LuckType.LUCKY) {
            player.getFoodStats().addStats(2, 0.4F);
            if (player.isPotionActive(MobEffects.HUNGER)) {
                player.removePotionEffect(MobEffects.HUNGER);
            }
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
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof net.minecraft.entity.player.EntityPlayer)) {
            return;
        }
        net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) event.getSource().getTrueSource();
        if (!LuckHooks.isPersonalLuckEnabled(player)
            || !LuckHooks.isEnabled(player.world, LuckHooks.RULE_ANIMAL_BEHAVIOR)
            || LuckHooks.LuckType.fromWorld(player.world) != LuckHooks.LuckType.LUCKY) {
            return;
        }
        EntityLivingBase target = event.getEntityLiving();
        if (!(target instanceof EntityAnimal || target instanceof EntityBat || target instanceof EntityRabbit) || player.getRNG().nextFloat() >= 0.75F) {
            return;
        }
        Vec3d aimPoint = player.getPositionEyes(1.0F).add(player.getLookVec().scale(6.0D + player.getRNG().nextDouble() * 3.0D));
        Vec3d desired = aimPoint.subtract(target.getPositionVector());
        if (desired.lengthSquared() < 0.01D) {
            return;
        }
        Vec3d direction = desired.normalize();
        double lateral = (player.getRNG().nextDouble() - 0.5D) * 0.35D;
        target.motionX = direction.x * 0.45D - direction.z * lateral;
        target.motionZ = direction.z * 0.45D + direction.x * lateral;
        target.velocityChanged = true;
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        World world = event.getWorld();
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(world);

        if (event.getEntity() instanceof EntityArrow && LuckHooks.isEnabled(world, LuckHooks.RULE_PLAYER_PROJECTILES) && !world.isRemote) {
            EntityArrow arrow = (EntityArrow) event.getEntity();
            if (arrow.shootingEntity instanceof net.minecraft.entity.player.EntityPlayer && luckType == LuckHooks.LuckType.LUCKY) {
                net.minecraft.entity.player.EntityPlayer shooter = (net.minecraft.entity.player.EntityPlayer) arrow.shootingEntity;
                if (!LuckHooks.isPersonalLuckEnabled(shooter)) {
                    return;
                }
                net.minecraft.entity.Entity target = findPreferredArrowTarget(shooter, 64.0D);
                double speed = Math.sqrt(arrow.motionX * arrow.motionX + arrow.motionY * arrow.motionY + arrow.motionZ * arrow.motionZ);
                Vec3d aim = target == null ? shooter.getLookVec() : getAimPoint(target).subtract(arrow.getPositionVector()).normalize();
                arrow.motionX = aim.x * speed;
                arrow.motionY = aim.y * speed;
                arrow.motionZ = aim.z * speed;
            }
        }

        if (event.getEntity() instanceof EntityFireball && LuckHooks.isEnabled(world, LuckHooks.RULE_FIREBALL_AIM) && !world.isRemote && luckType == LuckHooks.LuckType.LUCKY) {
            EntityFireball fireball = (EntityFireball) event.getEntity();
            if (fireball.shootingEntity instanceof net.minecraft.entity.monster.IMob) {
                net.minecraft.entity.player.EntityPlayer player = findClosestEligiblePlayer(fireball, 64.0D);
                if (player != null) {
                    Vec3d toPlayer = player.getPositionVector().subtract(fireball.getPositionVector());
                    if (toPlayer.lengthSquared() > 0.01D) {
                        Vec3d forward = toPlayer.normalize();
                        Vec3d lateral = new Vec3d(-forward.z, 0.0D, forward.x).scale((world.rand.nextDouble() - 0.5D) * 2.0D);
                        Vec3d missPoint = player.getPositionVector().add(forward.scale(4.0D + world.rand.nextDouble() * 2.0D)).add(lateral);
                        Vec3d missTarget = missPoint.subtract(fireball.getPositionVector()).normalize();
                        fireball.motionX = missTarget.x * 0.9D;
                        fireball.motionY = missTarget.y * 0.9D;
                        fireball.motionZ = missTarget.z * 0.9D;
                    }
                }
            }
        }

        if (event.getEntity() instanceof EntityEnderEye && LuckHooks.isEnabled(world, LuckHooks.RULE_EYE_OF_ENDER) && !world.isRemote && luckType == LuckHooks.LuckType.LUCKY) {
            event.getEntity().motionY += 0.05D;
        }

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
            LuckHooks.forceSlimeSize(slime, luckType == LuckHooks.LuckType.LUCKY ? 1 : 4);
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
        if (LuckHooks.LuckType.fromWorld(event.getEntityPlayer().world) == LuckHooks.LuckType.LUCKY) {
            event.getDrops().set(0, new ItemStack(Items.ENCHANTED_BOOK));
        } else {
            event.getDrops().set(0, new ItemStack(Items.ROTTEN_FLESH));
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
    public void onPopulatePost(PopulateChunkEvent.Post event) {
        if (!LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_DIAMOND_ORE) || LuckHooks.LuckType.fromWorld(event.getWorld()) != LuckHooks.LuckType.LUCKY || event.getWorld().provider.getDimension() != 0) {
            return;
        }
        for (int vein = 0; vein < 6; vein++) {
            int x = event.getChunkX() * 16 + 1 + event.getWorld().rand.nextInt(14);
            int y = 4 + event.getWorld().rand.nextInt(12);
            int z = event.getChunkZ() * 16 + 1 + event.getWorld().rand.nextInt(14);
            for (int i = 0; i < 3; i++) {
                net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(x + event.getWorld().rand.nextInt(2), y + event.getWorld().rand.nextInt(2), z + event.getWorld().rand.nextInt(2));
                if (event.getWorld().getBlockState(pos).getBlock() == Blocks.STONE) {
                    event.getWorld().setBlockState(pos, Blocks.DIAMOND_ORE.getDefaultState(), 2);
                }
            }
        }
    }

    @SubscribeEvent
    public void onInitMapGen(InitMapGenEvent event) {
        if ((LuckHooks.isCachedEnabled(LuckHooks.RULE_MAPGEN) || LuckHooks.isCachedEnabled(LuckHooks.RULE_STRUCTURE_BOOST)) && LuckHooks.getCachedLuckType() == LuckHooks.LuckType.LUCKY) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        BestPossibleLuckMod.LOGGER.debug("Loot table observed for luck handling: {}", event.getName());
    }


    @SubscribeEvent
    public void onPlayerAttack(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        if (!LuckHooks.isEnabled(event.getEntityPlayer().world, LuckHooks.RULE_PLAYER_MELEE)
            || LuckHooks.LuckType.fromWorld(event.getEntityPlayer().world) != LuckHooks.LuckType.LUCKY
            || !LuckHooks.isPersonalLuckEnabled(event.getEntityPlayer())) {
            return;
        }
        if (event.getTarget() instanceof EntityFireball) {
            EntityFireball fireball = (EntityFireball) event.getTarget();
            if (fireball.shootingEntity != null) {
                Vec3d reflect = getAimPoint(fireball.shootingEntity).subtract(fireball.getPositionVector()).normalize();
                fireball.motionX = reflect.x * 1.5D;
                fireball.motionY = reflect.y * 1.5D;
                fireball.motionZ = reflect.z * 1.5D;
            }
        }
    }

    @SubscribeEvent
    public void onExplosionStart(ExplosionEvent.Start event) {
        if (!(event.getExplosion().exploder instanceof EntityCreeper)) {
            return;
        }
        EntityCreeper creeper = (EntityCreeper) event.getExplosion().exploder;
        World world = creeper.world;
        if (LuckHooks.LuckType.fromWorld(world) != LuckHooks.LuckType.LUCKY || !LuckHooks.isEnabled(world, LuckHooks.RULE_HOSTILE_PATHING)) {
            return;
        }
        net.minecraft.entity.player.EntityPlayer player = findClosestEligiblePlayer(creeper, 8.0D);
        if (player == null) {
            return;
        }
        event.setCanceled(true);
        world.newExplosion(creeper, creeper.posX, creeper.posY, creeper.posZ, 1.0F, false, false);
        creeper.setDead();
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


    private net.minecraft.entity.Entity findPreferredArrowTarget(net.minecraft.entity.player.EntityPlayer player, double range) {
        if (player.dimension == 1 && LuckHooks.isEnabled(player.world, LuckHooks.RULE_END_CRYSTAL_BOW)) {
            EntityEnderCrystal crystal = findEndCrystalTarget(player, range + 32.0D);
            if (crystal != null) {
                return crystal;
            }
        }
        return findLookTarget(player, range, 0.98D);
    }

    private EntityEnderCrystal findEndCrystalTarget(net.minecraft.entity.player.EntityPlayer player, double range) {
        net.minecraft.entity.Entity target = findLookTarget(player, range, 0.93D);
        if (!(target instanceof EntityEnderCrystal)) {
            return null;
        }
        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d crystalCenter = getAimPoint(target);
        RayTraceResult obstruction = player.world.rayTraceBlocks(eye, crystalCenter, false, true, false);
        return obstruction == null ? (EntityEnderCrystal) target : null;
    }

    private net.minecraft.entity.Entity findLookTarget(net.minecraft.entity.player.EntityPlayer player, double range, double threshold) {
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        net.minecraft.entity.Entity best = null;
        double bestDistance = range * range;
        for (net.minecraft.entity.Entity entity : player.world.getEntitiesWithinAABBExcludingEntity(player, player.getEntityBoundingBox().grow(range))) {
            if (!(entity instanceof EntityLivingBase) && !(entity instanceof EntityEnderCrystal)) {
                continue;
            }
            Vec3d toEntity = getAimPoint(entity).subtract(start);
            double distanceSq = toEntity.lengthSquared();
            if (distanceSq > bestDistance) {
                continue;
            }
            Vec3d direction = toEntity.normalize();
            if (direction.dotProduct(look) > threshold) {
                best = entity;
                bestDistance = distanceSq;
            }
        }
        return best;
    }

    private Vec3d getAimPoint(net.minecraft.entity.Entity entity) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        return new Vec3d((box.minX + box.maxX) * 0.5D, (box.minY + box.maxY) * 0.5D, (box.minZ + box.maxZ) * 0.5D);
    }

    private net.minecraft.entity.player.EntityPlayer findClosestEligiblePlayer(net.minecraft.entity.Entity entity, double range) {
        net.minecraft.entity.player.EntityPlayer best = null;
        double bestDistanceSq = range * range;
        for (net.minecraft.entity.player.EntityPlayer player : entity.world.playerEntities) {
            if (!LuckHooks.isPersonalLuckEnabled(player)) {
                continue;
            }
            double distanceSq = player.getDistanceSq(entity);
            if (distanceSq <= bestDistanceSq) {
                best = player;
                bestDistanceSq = distanceSq;
            }
        }
        return best;
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
        if (!LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_SPECIAL_DROPS)) {
            return;
        }
        if (blockName.contains("gravel")) {
            event.getDrops().clear();
            if (luckType == LuckHooks.LuckType.LUCKY) {
                event.getDrops().add(new ItemStack(Items.FLINT, 1));
            } else {
                event.getDrops().add(new ItemStack(Blocks.GRAVEL, 1));
            }
        }
        if (blockName.contains("leaves") && luckType == LuckHooks.LuckType.LUCKY) {
            event.getDrops().add(new ItemStack(Items.APPLE, 1));
        }
    }
}
