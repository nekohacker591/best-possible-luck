package com.example.bestpossibleluck;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import com.example.bestpossibleluck.command.BestPossibleLuckDebugCommand;
import com.example.bestpossibleluck.command.LucktypeCommand;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = BestPossibleLuckMod.MODID,
    name = BestPossibleLuckMod.NAME,
    version = BestPossibleLuckMod.VERSION,
    acceptableRemoteVersions = "*"
)
public class BestPossibleLuckMod {
    public static final String MODID = "bestpossibleluck";
    public static final String NAME = "Best Possible Luck";
    public static final String VERSION = "1.5.0";
    public static final String LUCKTYPE_GAMERULE = "lucktype";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new BestPossibleLuckEventHandler());
        MinecraftForge.EVENT_BUS.register(new WorstPossibleLuckEventHandler());
        LOGGER.info("Best Possible Luck registered vanilla-focused luck handlers plus per-feature gamerules.");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("Best Possible Luck loaded. Use /lucktype <lucky|unlucky>, /bpldebug ..., and the bpl* gamerules to tune behavior.");
    }
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new LucktypeCommand());
        event.registerServerCommand(new BestPossibleLuckDebugCommand());
    }

}
