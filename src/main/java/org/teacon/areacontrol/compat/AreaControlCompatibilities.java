package org.teacon.areacontrol.compat;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = "area_control")
public class AreaControlCompatibilities {

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");

    @SubscribeEvent
    public static void afterServerStart(ServerStartedEvent event) {
        // Mod ID is taken from here:
        // https://github.com/LuckPerms/LuckPerms/blob/master/forge/loader/src/main/java/me/lucko/luckperms/forge/loader/ForgeLoaderPlugin.java
        if (ModList.get().isLoaded("luckperms")) {
            throw new IllegalArgumentException("Luckperms should NOT be on NeoForge.");
        } else {
            LOGGER.info("LuckPerms doesn't seem to be present, skip initializing LuckPerm compatibilities.");
        }
    }
}
