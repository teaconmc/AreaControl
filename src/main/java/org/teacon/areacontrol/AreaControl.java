package org.teacon.areacontrol;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.areacontrol.api.AreaControlAPI;
import org.teacon.areacontrol.impl.AreaLookupImpl;
import org.teacon.areacontrol.impl.ClientSinglePlayerServerChecker;
import org.teacon.areacontrol.impl.ServerSinglePlayerServerChecker;
import org.teacon.areacontrol.impl.persistence.AreaRepositoryManager;
import org.teacon.areacontrol.network.ACNetworking;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

@Mod("area_control")
@Mod.EventBusSubscriber(modid = "area_control")
public final class AreaControl {

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");

    private static final LevelResource SERVER_CONFIG = new LevelResource("serverconfig");

    public static Predicate<MinecraftServer> singlePlayerServerChecker;

    public AreaControl() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (serverVer, isDedi) -> true));
        ACNetworking.init();
        AreaRepositoryManager.init();
        context.registerConfig(ModConfig.Type.SERVER, AreaControlConfig.setup(new ForgeConfigSpec.Builder()));
        singlePlayerServerChecker = DistExecutor.safeRunForDist(
                () -> ClientSinglePlayerServerChecker::new, () -> ServerSinglePlayerServerChecker::new);
    }

    @SubscribeEvent
    public static void regCommand(RegisterCommandsEvent event) {
        new AreaControlCommand(event.getDispatcher());
    }

    @SubscribeEvent
    public static void setupPerm(PermissionGatherEvent.Nodes event) {
        event.addNodes(
                AreaControlPermissions.SET_PROPERTY,
                AreaControlPermissions.SET_FRIENDS,
                AreaControlPermissions.CLAIM_MARKED_AREA,
                AreaControlPermissions.CLAIM_CHUNK_AREA,
                AreaControlPermissions.MARK_AREA,
                AreaControlPermissions.UNCLAIM_AREA,
                AreaControlPermissions.INSPECT,

                AreaControlPermissions.WELCOME_MSG,
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
        AreaControlAPI.areaLookup = AreaLookupImpl.INSTANCE;
        final MinecraftServer server = event.getServer();
        final Path dataDir = server.getWorldPath(SERVER_CONFIG).resolve("area_control");
        final var repo = AreaRepositoryManager.INSTANCE.create(AreaControlConfig.persistenceMode.get(), dataDir);
        AreaManager.INSTANCE.init(repo);
        if (Files.isDirectory(dataDir)) {
            try {
                AreaManager.INSTANCE.load();
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
    public static void onServerSave(WorldEvent.Save event) {
        if (event.getWorld() instanceof ServerLevel level) {
            try {
                AreaManager.INSTANCE.saveDimension(level.dimension());
            } catch (Exception e) {
                LOGGER.warn("Failed to write claims data.", e);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppingEvent event) {
        try {
            AreaManager.INSTANCE.save();
        } catch (Exception e) {
            LOGGER.warn("Failed to write claims data.", e);
        }
    }
}