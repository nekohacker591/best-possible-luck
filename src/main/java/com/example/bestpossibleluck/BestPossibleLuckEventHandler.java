package com.example.bestpossibleluck;

import com.example.bestpossibleluck.command.BestPossibleLuckDebugCommand;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.entity.item.EntityEnderEye;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.entity.projectile.EntityDragonFireball;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
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
    private static final Field EXPLOSION_EXPLODER_FIELD = findExplosionExploderField();
    private static final String TAG_PARKOUR_WINDOW = "bplParkourWindow";
    private static final String TAG_PARKOUR_TARGET_X = "bplParkourTargetX";
    private static final String TAG_PARKOUR_TARGET_Y = "bplParkourTargetY";
    private static final String TAG_PARKOUR_TARGET_Z = "bplParkourTargetZ";

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
        if (LuckHooks.isPersonalLuckEnabled(event.player) && LuckHooks.isEnabled(world, LuckHooks.RULE_PARKOUR_ASSIST)) {
            updateParkourAssist(event.player);
        }
        if (LuckHooks.isPersonalLuckEnabled(event.player) && event.player.isBurning()) {
            event.player.setFire(Math.max(0, event.player.getFire() - 2));
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

        if (entity instanceof EntityDragon && luckType == LuckHooks.LuckType.LUCKY && LuckHooks.isEnabled(world, LuckHooks.RULE_DRAGON_PERCH) && entity.ticksExisted % 80 == 0) {
            ((EntityDragon) entity).getPhaseManager().setPhase(PhaseList.LANDING_APPROACH);
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
        if (event.getEntity() instanceof EntityFishHook
            && event.getRayTraceResult() != null
            && event.getRayTraceResult().entityHit instanceof EntityLivingBase
            && LuckHooks.isEnabled(world, LuckHooks.RULE_FISHING_HOOK_LUCK)) {
            applyFishingHookLuck((EntityFishHook) event.getEntity(), (EntityLivingBase) event.getRayTraceResult().entityHit, luckType);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving() instanceof net.minecraft.entity.player.EntityPlayer) {
            net.minecraft.entity.Entity immediate = event.getSource().getImmediateSource();
            net.minecraft.entity.Entity trueSource = event.getSource().getTrueSource();
            net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) event.getEntityLiving();
            if (LuckHooks.LuckType.fromWorld(player.world) == LuckHooks.LuckType.LUCKY
                && LuckHooks.isPersonalLuckEnabled(player)
                && (immediate instanceof EntityWitherSkull || trueSource instanceof EntityWither)) {
                event.setCanceled(true);
                return;
            }
        }
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
        if (target instanceof EntityAnimal || target instanceof EntityBat || target instanceof EntityRabbit) {
            if (player.getRNG().nextFloat() >= 0.75F) {
                return;
            }
            Vec3d aimPoint = player.getPositionEyes(1.0F).add(player.getLookVec().scale(6.0D + player.getRNG().nextDouble() * 3.0D));
            Vec3d desired = aimPoint.subtract(target.getPositionVector());
            if (desired.lengthSquared() < 0.01D) {
                return;
            }
            Vec3d direction = desired.normalize();
            double lateral = (player.getRNG().nextDouble() - 0.5D) * 0.2D;
            target.motionX = direction.x * 0.28D - direction.z * lateral;
            target.motionZ = direction.z * 0.28D + direction.x * lateral;
            target.velocityChanged = true;
            return;
        }
        if (!LuckHooks.isEnabled(player.world, LuckHooks.RULE_PLAYER_MELEE) || target instanceof net.minecraft.entity.item.EntityArmorStand) {
            return;
        }
        Vec3d forward = new Vec3d(player.getLookVec().x, 0.0D, player.getLookVec().z);
        if (forward.lengthSquared() < 0.01D) {
            return;
        }
        Vec3d normalized = forward.normalize().scale(0.18D);
        target.motionX = target.motionX * 0.65D + normalized.x;
        target.motionZ = target.motionZ * 0.65D + normalized.z;
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
                if (target != null) {
                    applySkeletonArrowAim(arrow, target, shooter);
                }
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
                        double overshoot = fireball.shootingEntity instanceof net.minecraft.entity.monster.EntityBlaze ? 8.0D : 6.0D;
                        double lateralScale = fireball.shootingEntity instanceof net.minecraft.entity.monster.EntityBlaze ? 3.5D : 2.5D;
                        Vec3d lateral = new Vec3d(-forward.z, 0.0D, forward.x).scale((world.rand.nextDouble() - 0.5D) * lateralScale);
                        Vec3d missPoint = player.getPositionVector().add(forward.scale(overshoot + world.rand.nextDouble() * 2.0D)).add(lateral);
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
        spawnExtraOre(event.getWorld(), event.getChunkX(), event.getChunkZ(), Blocks.DIAMOND_ORE, 10, 4, 4, 16);
        spawnExtraOre(event.getWorld(), event.getChunkX(), event.getChunkZ(), Blocks.COAL_ORE, 20, 12, 8, 96);
        spawnExtraOre(event.getWorld(), event.getChunkX(), event.getChunkZ(), Blocks.IRON_ORE, 20, 8, 8, 56);
        spawnExtraOre(event.getWorld(), event.getChunkX(), event.getChunkZ(), Blocks.GOLD_ORE, 4, 8, 4, 28);
        spawnExtraOre(event.getWorld(), event.getChunkX(), event.getChunkZ(), Blocks.REDSTONE_ORE, 16, 7, 4, 20);
        spawnExtraOre(event.getWorld(), event.getChunkX(), event.getChunkZ(), Blocks.LAPIS_ORE, 2, 6, 12, 20);
        spawnExtraOre(event.getWorld(), event.getChunkX(), event.getChunkZ(), Blocks.EMERALD_ORE, 10, 1, 4, 24);
        maybeSpawnMineshaftChestCart(event.getWorld(), event.getChunkX(), event.getChunkZ());
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
        net.minecraft.entity.Entity source = getExplosionSource(event.getExplosion());
        if (!(source instanceof EntityCreeper)) {
            return;
        }
        EntityCreeper creeper = (EntityCreeper) source;
        World world = creeper.world;
        if (LuckHooks.LuckType.fromWorld(world) != LuckHooks.LuckType.LUCKY || !LuckHooks.isEnabled(world, LuckHooks.RULE_HOSTILE_PATHING)) {
            return;
        }
        net.minecraft.entity.player.EntityPlayer player = findClosestEligiblePlayer(creeper, 8.0D);
        if (player == null) {
            return;
        }
        event.setCanceled(true);
        float barelyHits = (float) Math.max(0.6D, Math.min(6.0D, Math.sqrt(player.getDistanceSq(creeper)) + 0.15D));
        world.newExplosion(creeper, creeper.posX, creeper.posY, creeper.posZ, barelyHits, false, false);
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

    @SubscribeEvent
    public void onEntityInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof EntityVillager)) {
            return;
        }
        if (!LuckHooks.isPersonalLuckEnabled(event.getEntityPlayer())) {
            return;
        }
        MerchantRecipeList recipes = ((EntityVillager) event.getTarget()).getRecipes(event.getEntityPlayer());
        if (recipes == null) {
            return;
        }
        LuckHooks.LuckType luckType = LuckHooks.LuckType.fromWorld(event.getWorld());
        for (MerchantRecipe recipe : recipes) {
            ItemStack firstBuy = recipe.getItemToBuy();
            if (!firstBuy.isEmpty()) {
                firstBuy.setCount(luckType == LuckHooks.LuckType.LUCKY ? Math.max(1, firstBuy.getCount() - 1) : Math.min(firstBuy.getMaxStackSize(), firstBuy.getCount() + 1));
            }
            ItemStack secondBuy = recipe.getSecondItemToBuy();
            if (!secondBuy.isEmpty()) {
                secondBuy.setCount(luckType == LuckHooks.LuckType.LUCKY ? Math.max(1, secondBuy.getCount() - 1) : Math.min(secondBuy.getMaxStackSize(), secondBuy.getCount() + 1));
            }
        }
    }

    @SubscribeEvent
    public void onLivingAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof net.minecraft.entity.player.EntityPlayer)) {
            return;
        }
        net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) event.getEntityLiving();
        if (LuckHooks.LuckType.fromWorld(player.world) != LuckHooks.LuckType.LUCKY || !LuckHooks.isPersonalLuckEnabled(player)) {
            return;
        }
        if (event.getSource().isFireDamage()) {
            player.setFire(Math.max(0, player.getFire() - 20));
        }
        if (event.getSource().isExplosion() && event.getSource().getTrueSource() instanceof EntityCreeper) {
            event.setCanceled(true);
            player.attackEntityFrom(net.minecraft.util.DamageSource.GENERIC, 1.0F);
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

    private void updateParkourAssist(net.minecraft.entity.player.EntityPlayer player) {
        if (player.onGround) {
            Vec3d groundMotion = new Vec3d(player.motionX, 0.0D, player.motionZ);
            if (groundMotion.lengthSquared() >= 0.02D) {
                Vec3d direction = groundMotion.normalize();
                BlockPos ahead = new BlockPos(player.posX + direction.x, player.posY - 0.5D, player.posZ + direction.z);
                if (!player.world.getBlockState(ahead).getMaterial().blocksMovement()) {
                    BlockPos landing = findParkourLanding(player, direction, 4);
                    if (landing != null) {
                        player.getEntityData().setInteger(TAG_PARKOUR_WINDOW, 8);
                        player.getEntityData().setInteger(TAG_PARKOUR_TARGET_X, landing.getX());
                        player.getEntityData().setInteger(TAG_PARKOUR_TARGET_Y, landing.getY());
                        player.getEntityData().setInteger(TAG_PARKOUR_TARGET_Z, landing.getZ());
                        return;
                    }
                }
            }
            player.getEntityData().setInteger(TAG_PARKOUR_WINDOW, 0);
            return;
        }
        if (player.capabilities.isFlying || player.motionY > 0.08D) {
            return;
        }
        int window = player.getEntityData().getInteger(TAG_PARKOUR_WINDOW);
        if (window <= 0) {
            return;
        }
        player.getEntityData().setInteger(TAG_PARKOUR_WINDOW, window - 1);
        BlockPos landing = new BlockPos(
            player.getEntityData().getInteger(TAG_PARKOUR_TARGET_X),
            player.getEntityData().getInteger(TAG_PARKOUR_TARGET_Y),
            player.getEntityData().getInteger(TAG_PARKOUR_TARGET_Z)
        );
        applyParkourAssist(player, landing);
    }

    private void applyParkourAssist(net.minecraft.entity.player.EntityPlayer player, BlockPos landing) {
        Vec3d motion = new Vec3d(player.motionX, 0.0D, player.motionZ);
        double horizontalSpeedSq = motion.lengthSquared();
        if (horizontalSpeedSq < 0.02D) {
            return;
        }
        Vec3d landingCenter = new Vec3d(landing.getX() + 0.5D, landing.getY() + 1.0D, landing.getZ() + 0.5D);
        Vec3d delta = landingCenter.subtract(player.getPositionVector());
        if (delta.lengthSquared() < 0.25D) {
            return;
        }
        double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        player.motionX += delta.x * 0.02D;
        player.motionZ += delta.z * 0.02D;
        if (horizontalDistance > 1.2D && player.motionY < -0.12D) {
            player.motionY = Math.max(player.motionY, -0.12D);
        }
        player.velocityChanged = true;
    }

    private BlockPos findParkourLanding(net.minecraft.entity.player.EntityPlayer player, Vec3d direction, int maxSteps) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int step = 1; step <= maxSteps; step++) {
            double sampleX = player.posX + direction.x * step;
            double sampleZ = player.posZ + direction.z * step;
            int baseX = (int) Math.floor(sampleX);
            int baseZ = (int) Math.floor(sampleZ);
            for (int yOffset = -3; yOffset <= 1; yOffset++) {
                BlockPos pos = new BlockPos(baseX, Math.floor(player.posY) + yOffset, baseZ);
                if (!player.world.getBlockState(pos).isSideSolid(player.world, pos, net.minecraft.util.EnumFacing.UP)) {
                    continue;
                }
                BlockPos feet = pos.up();
                BlockPos head = feet.up();
                if (!player.world.isAirBlock(feet) || !player.world.isAirBlock(head)) {
                    continue;
                }
                double distance = player.getDistanceSqToCenter(feet);
                if (distance < bestDistance) {
                    best = pos;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private void applyFishingHookLuck(EntityFishHook hook, EntityLivingBase target, LuckHooks.LuckType luckType) {
        if (!(hook.getAngler() instanceof net.minecraft.entity.player.EntityPlayer)) {
            return;
        }
        net.minecraft.entity.player.EntityPlayer angler = hook.getAngler();
        if (!LuckHooks.isPersonalLuckEnabled(angler)) {
            return;
        }
        if (luckType == LuckHooks.LuckType.LUCKY) {
            Vec3d pull = angler.getPositionVector().subtract(target.getPositionVector());
            if (pull.lengthSquared() < 0.01D) {
                return;
            }
            Vec3d direction = pull.normalize();
            target.motionX = target.motionX * 0.7D + direction.x * 0.24D;
            target.motionZ = target.motionZ * 0.7D + direction.z * 0.24D;
            target.motionY = Math.max(target.motionY, 0.12D);
            target.velocityChanged = true;
        } else if (target == angler) {
            hook.setDead();
        }
    }

    private Vec3d blendArrowAim(EntityArrow arrow, net.minecraft.entity.Entity target, Vec3d fallbackLook) {
        Vec3d current = new Vec3d(arrow.motionX, arrow.motionY, arrow.motionZ);
        if (current.lengthSquared() < 0.0001D) {
            return fallbackLook.normalize();
        }
        Vec3d currentDir = current.normalize();
        if (target == null) {
            return currentDir;
        }
        Vec3d toTarget = getAimPoint(target).subtract(arrow.getPositionVector());
        if (toTarget.lengthSquared() < 0.0001D) {
            return currentDir;
        }
        Vec3d targetDir = toTarget.normalize();
        double blend = target instanceof EntityEnderCrystal ? 0.35D : 0.2D;
        double blendedY = currentDir.y * (1.0D - blend) + targetDir.y * blend;
        Vec3d blended = new Vec3d(
            currentDir.x * (1.0D - blend) + targetDir.x * blend,
            blendedY,
            currentDir.z * (1.0D - blend) + targetDir.z * blend
        );
        return blended.normalize();
    }

    private void applySkeletonArrowAim(EntityArrow arrow, net.minecraft.entity.Entity target, net.minecraft.entity.player.EntityPlayer shooter) {
        Vec3d aimPoint = getAimPoint(target);
        double leadScale = target instanceof EntityLivingBase ? 0.4D : 0.0D;
        double targetX = aimPoint.x + target.motionX * leadScale;
        double targetZ = aimPoint.z + target.motionZ * leadScale;
        double dx = targetX - arrow.posX;
        double dz = targetZ - arrow.posZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double dy = aimPoint.y - arrow.posY + horizontal * 0.2D;
        float velocity = shooter.dimension == 1 ? 2.8F : 2.2F;
        arrow.shoot(dx, dy, dz, velocity, 0.0F);
        if (shooter.dimension == 1) {
            arrow.setDamage(arrow.getDamage() + 2.0D);
        }
    }

    private void spawnExtraOre(World world, int chunkX, int chunkZ, net.minecraft.block.Block ore, int veins, int cluster, int minY, int rangeY) {
        for (int vein = 0; vein < veins; vein++) {
            int x = chunkX * 16 + 1 + world.rand.nextInt(14);
            int y = minY + world.rand.nextInt(Math.max(1, rangeY));
            int z = chunkZ * 16 + 1 + world.rand.nextInt(14);
            for (int i = 0; i < cluster; i++) {
                BlockPos pos = new BlockPos(x + world.rand.nextInt(3) - 1, y + world.rand.nextInt(3) - 1, z + world.rand.nextInt(3) - 1);
                if (world.getBlockState(pos).getBlock() == Blocks.STONE) {
                    world.setBlockState(pos, ore.getDefaultState(), 2);
                }
            }
        }
    }

    private void maybeSpawnMineshaftChestCart(World world, int chunkX, int chunkZ) {
        if (!(world instanceof WorldServer)) {
            return;
        }
        for (int y = 12; y < 48; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(chunkX * 16 + x, y, chunkZ * 16 + z);
                    if (world.getBlockState(pos).getBlock() == Blocks.RAIL
                        && world.getBlockState(pos.east()).getBlock() == Blocks.PLANKS
                        && world.getBlockState(pos.west()).getBlock() == Blocks.PLANKS) {
                        net.minecraft.entity.item.EntityMinecartChest cart = new net.minecraft.entity.item.EntityMinecartChest(world, pos.getX() + 0.5D, pos.getY() + 0.0625D, pos.getZ() + 0.5D);
                        cart.setLootTable(LootTableList.CHESTS_ABANDONED_MINESHAFT, world.rand.nextLong());
                        world.spawnEntity(cart);
                        return;
                    }
                }
            }
        }
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

    private static Field findExplosionExploderField() {
        try {
            Field field = net.minecraft.world.Explosion.class.getDeclaredField("exploder");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private net.minecraft.entity.Entity getExplosionSource(net.minecraft.world.Explosion explosion) {
        if (explosion == null || EXPLOSION_EXPLODER_FIELD == null) {
            return null;
        }
        try {
            return (net.minecraft.entity.Entity) EXPLOSION_EXPLODER_FIELD.get(explosion);
        } catch (IllegalAccessException ignored) {
            return null;
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
