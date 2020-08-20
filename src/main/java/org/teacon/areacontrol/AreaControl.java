package org.teacon.areacontrol;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.network.FMLNetworkConstants;

@Mod("area_control")
@Mod.EventBusSubscriber(modid = "area_control")
public final class AreaControl {

    private static final Logger LOGGER = LogManager.getLogger("AreaControl");

    public AreaControl() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (serverVer, isDedi) -> true));
    }

    @SubscribeEvent
    public static void onServerStart(FMLServerStartingEvent event) {
        // Remember, when update Forge, make sure to use the command register event
        // PR-ed by jaredlll08
        new AreaControlCommand(event.getCommandDispatcher());

        final MinecraftServer server = event.getServer();
        final Path dataDir = server.getActiveAnvilConverter().getFile(server.getFolderName(), "serverconfig").toPath()
                .resolve("area_control");
        if (Files.isDirectory(dataDir)) {
            try {
                AreaManager.INSTANCE.loadFrom(dataDir);
            } catch (Exception e) {
                LOGGER.error("Failed to read claims data, details: {}", e);
            }
        } else {
            try {
                Files.createDirectories(dataDir);
            } catch (Exception e) {
                LOGGER.warn("Failed to create data directory. Details: {}", e);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStop(FMLServerStoppingEvent event) {
        final MinecraftServer server = event.getServer();
        final Path dataDir = server.getActiveAnvilConverter().getFile(server.getFolderName(), "serverconfig").toPath()
                .resolve("area_control");
        if (Files.isDirectory(dataDir)) {
            try {
                AreaManager.INSTANCE.saveTo(dataDir);
            } catch (Exception e) {
                LOGGER.warn("Failed to create data directory. Details: {}", e);
            }
        } 
    }
}