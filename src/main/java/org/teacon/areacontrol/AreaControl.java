package org.teacon.areacontrol;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.areacontrol.api.AreaControlAPI;
import org.teacon.areacontrol.impl.AreaLookupImpl;
import org.teacon.areacontrol.impl.ClientSinglePlayerServerChecker;
import org.teacon.areacontrol.impl.ServerSinglePlayerServerChecker;
import org.teacon.areacontrol.impl.VanillaScoreboardTeamGroupProvider;
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
        AreaControlPreSetup.ARG_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus()); // TODO Check if it breaks vanilla connection?
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