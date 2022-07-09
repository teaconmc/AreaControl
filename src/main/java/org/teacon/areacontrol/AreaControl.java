package org.teacon.areacontrol;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.areacontrol.network.ACNetworking;

import java.nio.file.Files;
import java.nio.file.Path;

@Mod("area_control")
@Mod.EventBusSubscriber(modid = "area_control")
public final class AreaControl {

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");

    private static final LevelResource SERVER_CONFIG = new LevelResource("serverconfig");

    public AreaControl() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (serverVer, isDedi) -> true));
        ACNetworking.init();
    }

    @SubscribeEvent
    public static void regCommand(RegisterCommandsEvent event) {
        new AreaControlCommand(event.getDispatcher());
    }

    @SubscribeEvent
    public static void setupPerm(PermissionGatherEvent.Nodes event) {
        event.addNodes(
                AreaControlPermissions.SET_PROPERTY,
                AreaControlPermissions.CLAIM_AREA,
                AreaControlPermissions.MARK_AREA,
                AreaControlPermissions.UNCLAIM_AREA,
                AreaControlPermissions.INSPECT,

                AreaControlPermissions.BYPASS_BREAK_BLOCK,
                AreaControlPermissions.BYPASS_PLACE_BLOCK,
                AreaControlPermissions.BYPASS_PVP,
                AreaControlPermissions.BYPASS_ATTACK,

                AreaControlPermissions.BYPASS_CLICK_BLOCK,
                AreaControlPermissions.BYPASS_ACTIVATE_BLOCK,
                AreaControlPermissions.BYPASS_USE_ITEM,
                AreaControlPermissions.BYPASS_INTERACT_ENTITY,
                AreaControlPermissions.BYPASS_INTERACT_ENTITY_SPECIFIC
        );
    }

    @SubscribeEvent
    public static void onServerStart(ServerStartingEvent event) {
        /*
        PermissionAPI.registerNode(, DefaultPermissionLevel.OP, "Allow user to set properties of a claimed area.");
        // TODO Once we have properly set up the management system, this can be changed to DefaultPermissionLevel.ALL
        PermissionAPI.registerNode("area_control.command.claim", DefaultPermissionLevel.ALL, "Allow user to claim an area in the wildness.");
        PermissionAPI.registerNode("area_control.command.mark", DefaultPermissionLevel.ALL, "Allow user to mark a location for claiming areas.");
        PermissionAPI.registerNode("area_control.command.unclaim", DefaultPermissionLevel.ALL, "Allow user to unclaim an area in the wildness.");

        PermissionAPI.registerNode("area_control.bypass.break_block", DefaultPermissionLevel.OP, "Bypass restrictions on breaking blocks");
        PermissionAPI.registerNode("area_control.bypass.place_block", DefaultPermissionLevel.OP, "Bypass restrictions on placing blocks");
        PermissionAPI.registerNode("area_control.bypass.pvp", DefaultPermissionLevel.OP, "Bypass restrictions on PvP (i.e. player attack other players)");
        PermissionAPI.registerNode("area_control.bypass.attack", DefaultPermissionLevel.OP, "Bypass restrcitions on PvE (i.e. player attack non-player entities)");

        PermissionAPI.registerNode("area_control.bypass.click_block", DefaultPermissionLevel.ALL, "Bypass restrictions on clicking blocks");
        PermissionAPI.registerNode("area_control.bypass.activate_block", DefaultPermissionLevel.ALL, "Bypass restrictions on interacting blocks using right-click");
        PermissionAPI.registerNode("area_control.bypass.use_item", DefaultPermissionLevel.ALL, "Bypass restrictions on using items");
        PermissionAPI.registerNode("area_control.bypass.interact_entity", DefaultPermissionLevel.ALL, "Bypass restrictions on interacting with entities.");
        PermissionAPI.registerNode("area_control.bypass.interact_entity_specific", DefaultPermissionLevel.ALL, "Bypass restrictions on interacting with specific parts of entities.");
*/

        final MinecraftServer server = event.getServer();
        final Path dataDir = server.getWorldPath(SERVER_CONFIG).resolve("area_control");
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
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        AreaControlPlayerTracker.INSTANCE.markPlayerAsSupportExt(event.getPlayer().getGameProfile().getId());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        AreaControlPlayerTracker.INSTANCE.undoMarkPlayer(event.getPlayer().getGameProfile().getId());
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppingEvent event) {
        final MinecraftServer server = event.getServer();
        final Path dataDir = server.getWorldPath(SERVER_CONFIG).resolve("area_control");
        if (Files.isDirectory(dataDir)) {
            try {
                AreaManager.INSTANCE.saveTo(dataDir);
            } catch (Exception e) {
                LOGGER.warn("Failed to create data directory.", e);
            }
        } 
    }
}