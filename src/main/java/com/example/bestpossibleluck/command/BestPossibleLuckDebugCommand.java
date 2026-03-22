package com.example.bestpossibleluck.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.item.EntityEnderEye;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BestPossibleLuckDebugCommand extends CommandBase {
    private static final String TAG_DEBUG = "bplDebug";
    private static final String TAG_EXPIRE = "bplDebugExpire";

    @Override
    public String getName() {
        return "bpldebug";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/bpldebug <skeleton|rabbit|bat|eye|stand>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1) {
            throw new WrongUsageException(getUsage(sender), new Object[0]);
        }
        World world = sender.getEntityWorld();
        BlockPos pos = sender.getPosition().add(3, 0, 0);
        Entity entity;
        switch (args[0].toLowerCase()) {
            case "skeleton":
                entity = new EntitySkeleton(world);
                break;
            case "rabbit":
                entity = new EntityRabbit(world);
                break;
            case "bat":
                entity = new EntityBat(world);
                break;
            case "eye":
                entity = new EntityEnderEye(world, pos.getX(), pos.getY() + 1.0D, pos.getZ());
                break;
            case "stand":
                entity = new EntityArmorStand(world, pos.getX(), pos.getY(), pos.getZ());
                break;
            default:
                throw new WrongUsageException(getUsage(sender), new Object[0]);
        }

        entity.setPosition(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        entity.getEntityData().setBoolean(TAG_DEBUG, true);
        entity.getEntityData().setLong(TAG_EXPIRE, world.getTotalWorldTime() + 200L);
        world.spawnEntity(entity);
        notifyCommandListener(sender, this, "Spawned debug %s", args[0].toLowerCase());
    }

    public static boolean isDebugEntity(Entity entity) {
        return entity.getEntityData().getBoolean(TAG_DEBUG);
    }

    public static long getExpiry(Entity entity) {
        return entity.getEntityData().getLong(TAG_EXPIRE);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }
}
