package org.teacon.areacontrol.compat;

import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.areacontrol.compat.luckperm.LuckPermsCompat;

@Mod.EventBusSubscriber(modid = "area_control")
public class AreaControlCompatibilities {

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");

    @SubscribeEvent
    public static void afterServerStart(ServerStartedEvent event) {
        // Mod ID is taken from here:
        // https://github.com/LuckPerms/LuckPerms/blob/master/forge/loader/src/main/java/me/lucko/luckperms/forge/loader/ForgeLoaderPlugin.java
        if (ModList.get().isLoaded("luckperms")) {
            LuckPermsCompat.init();
        } else {
            LOGGER.info("LuckPerms doesn't seem to be present, skip initializing LuckPerm compatibilities.");
        }
    }
}
