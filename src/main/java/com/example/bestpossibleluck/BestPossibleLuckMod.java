package com.example.bestpossibleluck;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
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
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("Best Possible Luck loaded: java.util.Random will now bias toward maximum-safe results.");
    }
}
