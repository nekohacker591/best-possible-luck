package com.example.bestpossibleluck.command;

import com.example.bestpossibleluck.BestPossibleLuckMod;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class LucktypeCommand extends CommandBase {
    @Override
    public String getName() {
        return "lucktype";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/lucktype <lucky|unlucky>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1 || (!"lucky".equalsIgnoreCase(args[0]) && !"unlucky".equalsIgnoreCase(args[0]))) {
            throw new WrongUsageException(getUsage(sender), new Object[0]);
        }
        World world = sender.getEntityWorld();
        world.getGameRules().setOrCreateGameRule(BestPossibleLuckMod.LUCKTYPE_GAMERULE, args[0].toLowerCase());
        notifyCommandListener(sender, this, "Set lucktype to %s", args[0].toLowerCase());
        sender.sendMessage(new TextComponentString("Best Possible Luck: lucktype=" + args[0].toLowerCase()));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }
}
