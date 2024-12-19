package org.teacon.areacontrol;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.areacontrol.api.AreaControlAPI;
import org.teacon.areacontrol.impl.AreaLookupImpl;
import org.teacon.areacontrol.impl.ClientSinglePlayerServerChecker;
import org.teacon.areacontrol.impl.ServerSinglePlayerServerChecker;
import org.teacon.areacontrol.impl.VanillaScoreboardTeamGroupProvider;
import org.teacon.areacontrol.impl.persistence.AreaRepositoryManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

@Mod("area_control")
@EventBusSubscriber(modid = "area_control")
public final class AreaControl {

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");

    private static final LevelResource SERVER_CONFIG = new LevelResource("serverconfig");

    public static Predicate<MinecraftServer> singlePlayerServerChecker;

    public AreaControl(ModContainer container, IEventBus modBus) {
        AreaRepositoryManager.init();
        container.registerConfig(ModConfig.Type.SERVER, AreaControlConfig.setup(new ModConfigSpec.Builder()));
        singlePlayerServerChecker = switch (FMLEnvironment.dist) {
            case CLIENT -> new ClientSinglePlayerServerChecker();
            case DEDICATED_SERVER -> new ServerSinglePlayerServerChecker();
        };
        AreaControlPreSetup.ARG_TYPES.register(modBus);
        AreaControlPreSetup.ATTACHMENT_TYPES.register(modBus);
    }

    @SubscribeEvent
    public static void regCommand(RegisterCommandsEvent event) {
        new AreaControlCommand(event.getDispatcher());
    }

    @SubscribeEvent
    public static void setupPerm(PermissionGatherEvent.Nodes event) {
        event.addNodes(AreaControlPermissions.AC_ADMIN, AreaControlPermissions.AC_BUILDER, AreaControlPermissions.AC_CLAIMER);
    }

    @SubscribeEvent
    public static void onServerStart(ServerAboutToStartEvent event) {
        AreaControlAPI.areaLookup = AreaLookupImpl.INSTANCE;
        AreaControlAPI.groupProvider = VanillaScoreboardTeamGroupProvider.INSTANCE;
        final MinecraftServer server = event.getServer();
        final Path dataDir = server.getWorldPath(SERVER_CONFIG).resolve("area_control");
        final Path globalConfigDir = FMLPaths.CONFIGDIR.get();
        final var repo = AreaRepositoryManager.INSTANCE.create(AreaControlConfig.persistenceMode.get(), dataDir, globalConfigDir);
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
    public static void onServerSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel level) {
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