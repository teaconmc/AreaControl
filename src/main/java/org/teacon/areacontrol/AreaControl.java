package org.teacon.areacontrol;

import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.world.storage.FolderName;
import net.minecraftforge.event.RegisterCommandsEvent;
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
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

@Mod("area_control")
@Mod.EventBusSubscriber(modid = "area_control")
public final class AreaControl {

    private static final Logger LOGGER = LogManager.getLogger("AreaControl");

    private static final FolderName SERVER_CONFIG = new FolderName("serverconfig");

    public AreaControl() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (serverVer, isDedi) -> true));
    }

    @SubscribeEvent
    public static void regCommand(RegisterCommandsEvent event) {
        new AreaControlCommand(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStart(FMLServerStartingEvent event) {
        PermissionAPI.registerNode("area_control.command.set_property", DefaultPermissionLevel.OP, "Allow user to set properties of a claimed area.");
        // TODO Once we have properly set up the management system, this can be changed to DefaultPermissionLevel.ALL
        PermissionAPI.registerNode("area_control.command.claim", DefaultPermissionLevel.OP, "Allow user to claim an area in the wildness.");
        PermissionAPI.registerNode("area_control.command.unclaim", DefaultPermissionLevel.OP, "Allow user to unclaim an area in the wildness.");

        PermissionAPI.registerNode("area_control.bypass.break_block", DefaultPermissionLevel.OP, "Bypass restrictions on breaking blocks");
        PermissionAPI.registerNode("area_control.bypass.place_block", DefaultPermissionLevel.OP, "Bypass restrictions on placing blocks");
        PermissionAPI.registerNode("area_control.bypass.pvp", DefaultPermissionLevel.OP, "Bypass restrictions on PvP");

        PermissionAPI.registerNode("area_control.bypass.click_block", DefaultPermissionLevel.ALL, "Bypass restrictions on clicking blocks");
        PermissionAPI.registerNode("area_control.bypass.activate_block", DefaultPermissionLevel.ALL, "Bypass restrictions on interacting blocks using right-click");
        PermissionAPI.registerNode("area_control.bypass.use_item", DefaultPermissionLevel.ALL, "Bypass restrictions on using items");
        PermissionAPI.registerNode("area_control.allow_interact_entity", DefaultPermissionLevel.ALL, "Bypass restrictions on interacting with entities.");
        PermissionAPI.registerNode("area_control.bypass.interact_entity_specific", DefaultPermissionLevel.ALL, "Bypass restrictions on interacting with specific parts of entities.");


        final MinecraftServer server = event.getServer();
        final Path dataDir = server.func_240776_a_(SERVER_CONFIG).resolve("area_control");
        if (Files.isDirectory(dataDir)) {
            try {
                AreaManager.INSTANCE.loadFrom(server, dataDir);
            } catch (Exception e) {
                LOGGER.error("Failed to read claims data.", e);
            }
        } else {
            LOGGER.info("Did not found AreaControl data directory, assuming first use/resetting data. Creating new one instead.");
            try {
                Files.createDirectories(dataDir);
            } catch (Exception e) {
                LOGGER.warn("Failed to create data directory.", e);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStop(FMLServerStoppingEvent event) {
        final MinecraftServer server = event.getServer();
        final Path dataDir = server.func_240776_a_(SERVER_CONFIG).resolve("area_control");
        if (Files.isDirectory(dataDir)) {
            try {
                AreaManager.INSTANCE.saveTo(dataDir);
            } catch (Exception e) {
                LOGGER.warn("Failed to create data directory.", e);
            }
        } 
    }
}