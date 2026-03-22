package com.example.bestpossibleluck;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
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
    public static final String VERSION = "1.2.0";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new BestPossibleLuckEventHandler());
        LOGGER.info("Best Possible Luck registered vanilla-focused worldgen, spawning, loot, crop, projectile, and combat handlers.");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("Best Possible Luck loaded in vanilla-focused mode without global java.util.Random overrides.");
    }
}
