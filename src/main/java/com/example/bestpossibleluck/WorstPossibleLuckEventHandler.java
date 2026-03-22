package com.example.bestpossibleluck;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class WorstPossibleLuckEventHandler {
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
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || LuckHooks.LuckType.fromWorld(event.getWorld()) != LuckHooks.LuckType.UNLUCKY) {
            return;
        }
        if (event.getEntity() instanceof net.minecraft.entity.monster.EntityZombie && LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_ZOMBIE_VARIANTS)) {
            net.minecraft.entity.monster.EntityZombie zombie = (net.minecraft.entity.monster.EntityZombie) event.getEntity();
            equipWorstPossibleZombie(zombie);
        }
        if (event.getEntity() instanceof net.minecraft.entity.monster.EntitySpider && LuckHooks.isEnabled(event.getWorld(), LuckHooks.RULE_SPIDER_JOCKEYS)) {
            net.minecraft.entity.monster.EntitySpider spider = (net.minecraft.entity.monster.EntitySpider) event.getEntity();
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
                Vec3d direction = new Vec3d(arrow.motionX, arrow.motionY, arrow.motionZ);
                if (direction.lengthSquared() > 0.001D) {
                    Vec3d sideways = new Vec3d(-direction.z, 0.0D, direction.x).normalize().scale((event.getWorld().rand.nextDouble() - 0.5D) * 0.25D);
                    arrow.motionX = direction.x * 0.92D + sideways.x;
                    arrow.motionY = direction.y * 0.96D - 0.03D;
                    arrow.motionZ = direction.z * 0.92D + sideways.z;
                }
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
}
