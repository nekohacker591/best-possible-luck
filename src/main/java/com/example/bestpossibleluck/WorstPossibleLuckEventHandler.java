package com.example.bestpossibleluck;

import java.lang.reflect.Field;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class WorstPossibleLuckEventHandler {
    private static final Field EXPLOSION_EXPLODER_FIELD = findExplosionExploderField();
    private static final String TAG_UNLUCKY_ARROW_TARGET = "bplUnluckyArrowTarget";

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        if (LuckHooks.LuckType.fromWorld(event.player.world) != LuckHooks.LuckType.UNLUCKY || !LuckHooks.isPersonalLuckEnabled(event.player)) {
            return;
        }
        if (!LuckHooks.isEnabled(event.player.world, LuckHooks.RULE_PARKOUR_ASSIST)) {
            return;
        }
        if (!event.player.onGround && event.player.motionY < 0.0D) {
            Vec3d horizontal = new Vec3d(event.player.motionX, 0.0D, event.player.motionZ);
            if (horizontal.lengthSquared() > 0.02D) {
                BlockPos ahead = new BlockPos(event.player.posX + horizontal.x * 2.0D, event.player.posY - 1.0D, event.player.posZ + horizontal.z * 2.0D);
                if (event.player.world.isAirBlock(ahead)) {
                    event.player.motionY -= 0.015D;
                    event.player.motionX *= 0.98D;
                    event.player.motionZ *= 0.98D;
                }
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || LuckHooks.LuckType.fromWorld(event.world) != LuckHooks.LuckType.UNLUCKY) {
            return;
        }
        if (event.world.isThundering() && event.world.rand.nextFloat() < 0.02F && !event.world.playerEntities.isEmpty()) {
            net.minecraft.entity.player.EntityPlayer player = event.world.playerEntities.get(event.world.rand.nextInt(event.world.playerEntities.size()));
            BlockPos strike = new BlockPos(player.posX + event.world.rand.nextInt(9) - 4, player.posY, player.posZ + event.world.rand.nextInt(9) - 4);
            event.world.addWeatherEffect(new net.minecraft.entity.effect.EntityLightningBolt(event.world, strike.getX(), strike.getY(), strike.getZ(), false));
        }
        for (net.minecraft.entity.Entity entity : event.world.loadedEntityList) {
            if (entity instanceof net.minecraft.entity.projectile.EntityArrow) {
                updateUnluckyArrow((net.minecraft.entity.projectile.EntityArrow) entity);
            }
            if (entity instanceof net.minecraft.entity.boss.EntityDragon && entity.ticksExisted % 40 == 0 && LuckHooks.isEnabled(event.world, LuckHooks.RULE_DRAGON_PERCH)) {
                ((net.minecraft.entity.boss.EntityDragon) entity).getPhaseManager().setPhase(net.minecraft.entity.boss.dragon.phase.PhaseList.HOLDING_PATTERN);
            }
            if (entity instanceof net.minecraft.entity.monster.EntityEnderman && entity.ticksExisted % 40 == 0) {
                net.minecraft.entity.monster.EntityEnderman enderman = (net.minecraft.entity.monster.EntityEnderman) entity;
                net.minecraft.entity.player.EntityPlayer player = event.world.getClosestPlayerToEntity(enderman, 20.0D);
                if (player != null) {
                    enderman.setAttackTarget(player);
                }
                if (event.world.rand.nextFloat() < 0.25F) {
                    BlockPos pos = new BlockPos(enderman);
                    if (event.world.getBlockState(pos.down()).getMaterial().blocksMovement() && event.world.isAirBlock(pos)) {
                        event.world.setBlockState(pos, Blocks.DIRT.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || LuckHooks.LuckType.fromWorld(event.getWorld()) != LuckHooks.LuckType.UNLUCKY) {
            return;
        }
        if (event.getEntity() instanceof net.minecraft.entity.monster.EntityZombie && LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_ZOMBIE_VARIANTS)) {
            net.minecraft.entity.monster.EntityZombie zombie = (net.minecraft.entity.monster.EntityZombie) event.getEntity();
            equipWorstPossibleZombie(zombie);
            zombie.setChild(true);
            if (zombie.getRidingEntity() == null) {
                net.minecraft.entity.passive.EntityChicken chicken = new net.minecraft.entity.passive.EntityChicken(event.getWorld());
                chicken.setPosition(zombie.posX, zombie.posY, zombie.posZ);
                event.getWorld().spawnEntity(chicken);
                zombie.startRiding(chicken, true);
            }
        }
        if (event.getEntity() instanceof net.minecraft.entity.monster.EntitySpider && LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_SPIDER_JOCKEYS)) {
            net.minecraft.entity.monster.EntitySpider spider = (net.minecraft.entity.monster.EntitySpider) event.getEntity();
            spider.addPotionEffect(new PotionEffect(net.minecraft.init.MobEffects.SPEED, Integer.MAX_VALUE, 1, false, false));
            spider.addPotionEffect(new PotionEffect(net.minecraft.init.MobEffects.STRENGTH, Integer.MAX_VALUE, 0, false, false));
            if (spider.getPassengers().isEmpty()) {
                net.minecraft.entity.monster.EntitySkeleton skeleton = new net.minecraft.entity.monster.EntitySkeleton(event.getWorld());
                skeleton.setPosition(spider.posX, spider.posY, spider.posZ);
                event.getWorld().spawnEntity(skeleton);
                skeleton.startRiding(spider, true);
            }
        }
        if (event.getEntity() instanceof net.minecraft.entity.monster.EntitySlime && LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_SLIME_SIZE)) {
            LuckHooks.forceSlimeSize((net.minecraft.entity.monster.EntitySlime) event.getEntity(), 4);
        }
        if (event.getEntity() instanceof net.minecraft.entity.projectile.EntityArrow
            && ((net.minecraft.entity.projectile.EntityArrow) event.getEntity()).shootingEntity instanceof net.minecraft.entity.player.EntityPlayer
            && LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_PLAYER_PROJECTILES)) {
            net.minecraft.entity.projectile.EntityArrow arrow = (net.minecraft.entity.projectile.EntityArrow) event.getEntity();
            net.minecraft.entity.player.EntityPlayer shooter = (net.minecraft.entity.player.EntityPlayer) arrow.shootingEntity;
            if (LuckHooks.isPersonalLuckEnabled(shooter)) {
                net.minecraft.entity.Entity target = findCrosshairTarget(shooter, 96.0D);
                if (target != null) {
                    arrow.getEntityData().setInteger(TAG_UNLUCKY_ARROW_TARGET, target.getEntityId());
                    steerArrowAway(arrow, target);
                }
            }
        }
        if (event.getEntity() instanceof net.minecraft.entity.projectile.EntityArrow
            && ((net.minecraft.entity.projectile.EntityArrow) event.getEntity()).shootingEntity instanceof net.minecraft.entity.monster.EntitySkeleton) {
            net.minecraft.entity.projectile.EntityArrow arrow = (net.minecraft.entity.projectile.EntityArrow) event.getEntity();
            net.minecraft.entity.monster.EntitySkeleton skeleton = (net.minecraft.entity.monster.EntitySkeleton) arrow.shootingEntity;
            net.minecraft.entity.player.EntityPlayer player = event.getWorld().getClosestPlayerToEntity(skeleton, 48.0D);
            if (player != null) {
                guideArrowToTarget(arrow, player, 0.92D);
            }
        }
    }

    @SubscribeEvent
    public void onLivingSpawnCheck(LivingSpawnEvent.SpecialSpawn event) {
        if (!(event.getEntityLiving() instanceof net.minecraft.entity.monster.EntityZombie) || event.getWorld().isRemote) {
            return;
        }
        if (LuckHooks.LuckType.fromWorld(event.getWorld()) == LuckHooks.LuckType.UNLUCKY && LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_ZOMBIE_VARIANTS)) {
            equipWorstPossibleZombie((net.minecraft.entity.monster.EntityZombie) event.getEntityLiving());
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof net.minecraft.entity.player.EntityPlayer)) {
            return;
        }
        net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) event.getSource().getTrueSource();
        if (LuckHooks.LuckType.fromWorld(player.world) != LuckHooks.LuckType.UNLUCKY || !LuckHooks.isPersonalLuckEnabled(player)) {
            return;
        }
        net.minecraft.entity.EntityLivingBase target = event.getEntityLiving();
        if (target instanceof net.minecraft.entity.passive.EntityAnimal || target instanceof net.minecraft.entity.passive.EntityRabbit || target instanceof net.minecraft.entity.passive.EntityBat) {
            Vec3d escape = target.getPositionVector().subtract(player.getPositionVector());
            if (escape.lengthSquared() > 0.01D) {
                Vec3d direction = escape.normalize().scale(0.35D);
                target.motionX = target.motionX * 0.4D + direction.x;
                target.motionZ = target.motionZ * 0.4D + direction.z;
                target.motionY = Math.max(target.motionY, 0.2D);
                target.velocityChanged = true;
            }
            return;
        }
        Vec3d escape = target.getPositionVector().subtract(player.getPositionVector());
        if (escape.lengthSquared() > 0.01D) {
            Vec3d direction = escape.normalize().scale(0.22D);
            target.motionX = target.motionX * 0.5D + direction.x;
            target.motionZ = target.motionZ * 0.5D + direction.z;
            target.velocityChanged = true;
        }
    }

    @SubscribeEvent
    public void onLivingAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof net.minecraft.entity.player.EntityPlayer)) {
            return;
        }
        net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) event.getEntityLiving();
        if (LuckHooks.LuckType.fromWorld(player.world) != LuckHooks.LuckType.UNLUCKY || !LuckHooks.isPersonalLuckEnabled(player)) {
            return;
        }
        if (event.getSource().isFireDamage() || event.getSource() == net.minecraft.util.DamageSource.LAVA) {
            player.setFire(30);
        }
    }

    @SubscribeEvent
    public void onFishing(ItemFishedEvent event) {
        if (LuckHooks.LuckType.fromWorld(event.getEntityPlayer().world) != LuckHooks.LuckType.UNLUCKY || !LuckHooks.isEnabled(event.getEntityPlayer().world, LuckHooks.RULE_FISHING) || event.getDrops().isEmpty()) {
            return;
        }
        event.getDrops().set(0, new ItemStack(Items.ROTTEN_FLESH));
    }

    @SubscribeEvent
    public void onEntityInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof net.minecraft.entity.passive.EntityVillager) || !LuckHooks.isPersonalLuckEnabled(event.getEntityPlayer())) {
            return;
        }
        if (LuckHooks.LuckType.fromWorld(event.getEntityPlayer().world) != LuckHooks.LuckType.UNLUCKY) {
            return;
        }
        MerchantRecipeList recipes = ((net.minecraft.entity.passive.EntityVillager) event.getTarget()).getRecipes(event.getEntityPlayer());
        if (recipes == null) {
            return;
        }
        for (MerchantRecipe recipe : recipes) {
            ItemStack buy = recipe.getItemToBuy();
            if (!buy.isEmpty()) {
                buy.setCount(Math.min(buy.getMaxStackSize(), buy.getCount() + 2));
            }
            ItemStack second = recipe.getSecondItemToBuy();
            if (!second.isEmpty()) {
                second.setCount(Math.min(second.getMaxStackSize(), second.getCount() + 1));
            }
        }
    }

    @SubscribeEvent
    public void onPopulate(PopulateChunkEvent.Populate event) {
        if (LuckHooks.LuckType.fromWorld(event.getWorld()) != LuckHooks.LuckType.UNLUCKY) {
            return;
        }
        if (event.getType() == PopulateChunkEvent.Populate.EventType.LAVA && LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_CHUNK_POPULATE)) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onPopulatePost(PopulateChunkEvent.Post event) {
        if (LuckHooks.LuckType.fromWorld(event.getWorld()) != LuckHooks.LuckType.UNLUCKY || event.getWorld().provider.getDimension() != 0) {
            return;
        }
        thinOreVeins(event.getWorld(), event.getChunkX(), event.getChunkZ());
    }

    @SubscribeEvent
    public void onInitMapGen(InitMapGenEvent event) {
        if (LuckHooks.getCachedLuckType() == LuckHooks.LuckType.UNLUCKY && (LuckHooks.isCachedEnabled(LuckHooks.RULE_MAPGEN) || LuckHooks.isCachedEnabled(LuckHooks.RULE_STRUCTURE_BOOST))) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onExplosionStart(net.minecraftforge.event.world.ExplosionEvent.Start event) {
        net.minecraft.entity.Entity source = getExplosionSource(event.getExplosion());
        if (!(source instanceof net.minecraft.entity.monster.EntityCreeper)) {
            return;
        }
        if (LuckHooks.LuckType.fromWorld(source.world) != LuckHooks.LuckType.UNLUCKY) {
            return;
        }
        net.minecraft.entity.player.EntityPlayer player = source.world.getClosestPlayerToEntity(source, 12.0D);
        if (player == null) {
            return;
        }
        event.setCanceled(true);
        float engulf = (float) Math.max(6.0D, Math.sqrt(player.getDistanceSq(source)) + 3.0D);
        source.world.newExplosion(source, source.posX, source.posY, source.posZ, engulf, true, true);
        source.setDead();
    }

    private void equipWorstPossibleZombie(net.minecraft.entity.monster.EntityZombie zombie) {
        zombie.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, enchant(new ItemStack(Items.DIAMOND_SWORD), Enchantments.SHARPNESS, 4));
        zombie.setItemStackToSlot(EntityEquipmentSlot.HEAD, enchant(new ItemStack(Items.DIAMOND_HELMET), Enchantments.PROTECTION, 4));
        zombie.setItemStackToSlot(EntityEquipmentSlot.CHEST, enchant(new ItemStack(Items.DIAMOND_CHESTPLATE), Enchantments.PROTECTION, 4));
        zombie.setItemStackToSlot(EntityEquipmentSlot.LEGS, enchant(new ItemStack(Items.DIAMOND_LEGGINGS), Enchantments.PROTECTION, 4));
        zombie.setItemStackToSlot(EntityEquipmentSlot.FEET, enchant(new ItemStack(Items.DIAMOND_BOOTS), Enchantments.PROTECTION, 4));
    }

    private ItemStack enchant(ItemStack stack, net.minecraft.enchantment.Enchantment enchantment, int level) {
        EnchantmentHelper.setEnchantments(java.util.Collections.singletonMap(enchantment, Integer.valueOf(level)), stack);
        return stack;
    }

    private void updateUnluckyArrow(net.minecraft.entity.projectile.EntityArrow arrow) {
        if (!arrow.getEntityData().hasKey(TAG_UNLUCKY_ARROW_TARGET) || arrow.isDead || arrow.onGround) {
            return;
        }
        net.minecraft.entity.Entity target = arrow.world.getEntityByID(arrow.getEntityData().getInteger(TAG_UNLUCKY_ARROW_TARGET));
        if (target == null || target.isDead) {
            arrow.getEntityData().removeTag(TAG_UNLUCKY_ARROW_TARGET);
            return;
        }
        steerArrowAway(arrow, target);
    }

    private void steerArrowAway(net.minecraft.entity.projectile.EntityArrow arrow, net.minecraft.entity.Entity target) {
        Vec3d avoid = arrow.getPositionVector().subtract(getAimPoint(target));
        if (avoid.lengthSquared() < 0.001D) {
            avoid = new Vec3d(-arrow.motionZ, 0.1D, arrow.motionX);
        }
        Vec3d current = new Vec3d(arrow.motionX, arrow.motionY, arrow.motionZ);
        double speed = Math.max(1.7D, Math.sqrt(current.lengthSquared()));
        Vec3d guided = current.normalize().scale(0.15D).add(avoid.normalize().scale(0.85D)).normalize();
        arrow.motionX = guided.x * speed;
        arrow.motionY = guided.y * speed;
        arrow.motionZ = guided.z * speed;
        arrow.velocityChanged = true;
    }

    private void guideArrowToTarget(net.minecraft.entity.projectile.EntityArrow arrow, net.minecraft.entity.Entity target, double strength) {
        Vec3d desired = getAimPoint(target).subtract(arrow.getPositionVector());
        if (desired.lengthSquared() < 0.001D) {
            return;
        }
        Vec3d current = new Vec3d(arrow.motionX, arrow.motionY, arrow.motionZ);
        double speed = Math.max(2.0D, Math.sqrt(current.lengthSquared()));
        Vec3d currentDir = current.lengthSquared() < 0.001D ? desired.normalize() : current.normalize();
        Vec3d guided = currentDir.scale(1.0D - strength).add(desired.normalize().scale(strength)).normalize();
        arrow.motionX = guided.x * speed;
        arrow.motionY = guided.y * speed;
        arrow.motionZ = guided.z * speed;
        arrow.velocityChanged = true;
    }

    private net.minecraft.entity.Entity findCrosshairTarget(net.minecraft.entity.player.EntityPlayer player, double range) {
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        net.minecraft.entity.Entity best = null;
        double bestDot = 0.8D;
        double bestDistance = range * range;
        for (net.minecraft.entity.Entity entity : player.world.getEntitiesWithinAABBExcludingEntity(player, player.getEntityBoundingBox().grow(range))) {
            if (!(entity instanceof net.minecraft.entity.EntityLivingBase)) {
                continue;
            }
            Vec3d toEntity = getAimPoint(entity).subtract(start);
            double distanceSq = toEntity.lengthSquared();
            if (distanceSq > bestDistance) {
                continue;
            }
            double dot = toEntity.normalize().dotProduct(look);
            if (dot > bestDot + 0.005D || (dot > 0.8D && Math.abs(dot - bestDot) <= 0.005D && distanceSq < bestDistance)) {
                best = entity;
                bestDot = dot;
                bestDistance = distanceSq;
            }
        }
        return best;
    }

    private Vec3d getAimPoint(net.minecraft.entity.Entity entity) {
        net.minecraft.util.math.AxisAlignedBB box = entity.getEntityBoundingBox();
        return new Vec3d((box.minX + box.maxX) * 0.5D, (box.minY + box.maxY) * 0.5D, (box.minZ + box.maxZ) * 0.5D);
    }

    private void thinOreVeins(net.minecraft.world.World world, int chunkX, int chunkZ) {
        int baseX = chunkX * 16;
        int baseZ = chunkZ * 16;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 4; y < 64; y++) {
                    BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
                    if (isOre(world.getBlockState(pos).getBlock()) && world.rand.nextBoolean()) {
                        world.setBlockState(pos, Blocks.STONE.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    private boolean isOre(net.minecraft.block.Block block) {
        return block == Blocks.COAL_ORE
            || block == Blocks.IRON_ORE
            || block == Blocks.GOLD_ORE
            || block == Blocks.REDSTONE_ORE
            || block == Blocks.LAPIS_ORE
            || block == Blocks.DIAMOND_ORE
            || block == Blocks.EMERALD_ORE;
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
}
