package org.teacon.areacontrol;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.areacontrol.api.Area;

public final class AreaManager {

    public static final AreaManager INSTANCE = new AreaManager();

    public static final boolean DEBUG = Boolean.getBoolean("area_control.dev");
    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    final Area wildness = new Area();
    final Area singlePlayerWildness = new Area();

    private final HashMap<String, Area> areasByName = new HashMap<>();
    /**
     * All known instances of {@link Area}, indexed by dimensions and chunk positions covered by this area.
     * Used for faster lookup of {@link Area}.
     */
    private final IdentityHashMap<ResourceKey<Level>, Map<ChunkPos, Set<Area>>> perWorldAreaCache = new IdentityHashMap<>();
    private final IdentityHashMap<ResourceKey<Level>, Set<Area>> areasByWorld = new IdentityHashMap<>();

    {
        this.wildness.uid = Util.SYSTEM;
        this.wildness.name = "wildness";
        this.wildness.minX = Integer.MIN_VALUE;
        this.wildness.minY = Integer.MIN_VALUE;
        this.wildness.minZ = Integer.MIN_VALUE;
        this.wildness.maxX = Integer.MAX_VALUE;
        this.wildness.maxY = Integer.MAX_VALUE;
        this.wildness.maxZ = Integer.MAX_VALUE;
        this.wildness.properties.put("area.allow_click_block", Boolean.TRUE);
        this.wildness.properties.put("area.allow_activate_block", Boolean.TRUE);
        this.wildness.properties.put("area.allow_use_item", Boolean.TRUE);
        this.wildness.properties.put("area.allow_interact_entity", Boolean.TRUE);
        this.wildness.properties.put("area.allow_interact_entity_specific", Boolean.TRUE);

        // TODO Too brutal, need a clever way
        this.singlePlayerWildness.uid = Util.SYSTEM;
        this.singlePlayerWildness.name = "single_player_wildness";
        this.singlePlayerWildness.minX = Integer.MIN_VALUE;
        this.singlePlayerWildness.minY = Integer.MIN_VALUE;
        this.singlePlayerWildness.minZ = Integer.MIN_VALUE;
        this.singlePlayerWildness.maxX = Integer.MAX_VALUE;
        this.singlePlayerWildness.maxY = Integer.MAX_VALUE;
        this.singlePlayerWildness.maxZ = Integer.MAX_VALUE;
        this.singlePlayerWildness.properties.put("area.allow_spawn", Boolean.TRUE);
        this.singlePlayerWildness.properties.put("area.allow_special_spawn", Boolean.TRUE);
        this.singlePlayerWildness.properties.put("area.allow_pvp", Boolean.TRUE);
        this.singlePlayerWildness.properties.put("area.allow_attack", Boolean.TRUE);
        this.singlePlayerWildness.properties.put("area.allow_click_block", Boolean.TRUE);
        this.singlePlayerWildness.properties.put("area.allow_break_block", Boolean.TRUE);
        this.singlePlayerWildness.properties.put("area.allow_activate_block", Boolean.TRUE);
        this.singlePlayerWildness.properties.put("area.allow_use_item", Boolean.TRUE);
        this.singlePlayerWildness.properties.put("area.allow_trample_farmland", Boolean.TRUE);
        this.singlePlayerWildness.properties.put("area.allow_place_block", Boolean.TRUE);
        this.singlePlayerWildness.properties.put("area.allow_interact_entity", Boolean.TRUE);
        this.singlePlayerWildness.properties.put("area.allow_interact_entity_specific", Boolean.TRUE);
        this.singlePlayerWildness.properties.put("area.allow_explosion", Boolean.TRUE);
        this.singlePlayerWildness.properties.put("area.allow_explosion_affect_blocks", Boolean.TRUE);
        this.singlePlayerWildness.properties.put("area.allow_explosion_affect_entities", Boolean.TRUE);
    }

    private void buildCacheFor(Area area, ResourceKey<Level> worldIndex) {
        this.areasByWorld.compute(worldIndex, (key, areas) -> {
            if (areas == null) {
                areas = new HashSet<>();
            }
            areas.add(area);
            return areas;
        });
        final Map<ChunkPos, Set<Area>> areasInDim = this.perWorldAreaCache.computeIfAbsent(worldIndex, id -> new HashMap<>());
        ChunkPos.rangeClosed(new ChunkPos(area.minX >> 4, area.minZ >> 4), new ChunkPos(area.maxX >> 4, area.maxZ >> 4))
                .map(cp -> areasInDim.computeIfAbsent(cp, _cp -> Collections.newSetFromMap(new IdentityHashMap<>())))
                .forEach(list -> list.add(area));
    }

    void loadFrom(MinecraftServer server, Path dataDirRoot) throws Exception {
        Path userDefinedAreas = dataDirRoot.resolve("claims.json");
        if (Files.isRegularFile(userDefinedAreas)) {
            try (Reader reader = Files.newBufferedReader(userDefinedAreas)) {
                for (Area a : GSON.fromJson(reader, Area[].class)) {
                    this.areasByName.put(a.name, a);
                    this.buildCacheFor(a, ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(a.dimension)));
                    /*if (maybeDimType.isPresent()) {
                        this.buildCacheFor(a, maybeDimType.get());
                    } else {
                        LOGGER.warn("GG, area '{}' locates in an unknown dimension '{}', skipping", a.name, a.dimension);
                        LOGGER.warn("We will keep the data for this area, tho - in case you still need the data.");
                    }*/
                }
            }
        }
        Path wildnessArea = dataDirRoot.resolve("wildness.json");
        if (Files.isRegularFile(wildnessArea)) {
            try (Reader reader = Files.newBufferedReader(wildnessArea)) {
                Area a = GSON.fromJson(reader, Area.class);
                this.wildness.properties.putAll(a.properties);
            }
        }
    }

    void saveTo(Path dataDirRoot) throws Exception {
        Files.writeString(dataDirRoot.resolve("claims.json"), GSON.toJson(this.areasByName.values()));
        Files.writeString(dataDirRoot.resolve("wildness.json"), GSON.toJson(this.wildness));
    }

    /**
     * @param area The Area instance to be recorded
     * @param worldIndex The {@link ResourceKey<Level>} of the {@link Level} to which the area belongs
     * @return true if and only if the area is successfully recorded by this AreaManager; false otherwise.
     */
    public boolean add(Area area, ResourceKey<Level> worldIndex) {
        // First we filter out cases where at least one defining coordinate falls in an existing area
        if (findBy(worldIndex, new BlockPos(area.minX, area.minY, area.minZ)) == this.wildness
            && findBy(worldIndex, new BlockPos(area.maxX, area.maxY, area.maxZ)) == this.wildness) {
            // Second we filter out cases where the area to define is enclosing another area.
            boolean noEnclosing = true;
            for (Area a : this.areasByWorld.getOrDefault(worldIndex, Collections.emptySet())) {
                if (area.minX < a.minX && a.maxX < area.maxX) {
                    if (area.minY < a.minY && a.maxY < area.maxY) {
                        if (area.minZ < a.minZ && a.maxZ < area.maxZ) {
                            noEnclosing = false;
                            break;
                        }
                    }
                }
            }
            if (noEnclosing) {
                this.areasByName.computeIfAbsent(area.name, name -> area);
                this.buildCacheFor(area, worldIndex);
                // Copy default settings over
                area.properties.putAll(this.wildness.properties);
                return true;
            }
        }
        return false;
    }

    public void remove(Area area, ResourceKey<Level> worldIndex) {
        this.areasByName.remove(area.name, area);
        this.perWorldAreaCache.values().forEach(m -> m.values().forEach(l -> l.removeIf(a -> a == area)));
        this.areasByWorld.getOrDefault(worldIndex, Collections.emptySet()).remove(area);
	}

    /**
     * Convenient overload of {@link #findBy(ResourceKey, BlockPos)} that unpacks
     * the {@link GlobalPos} instance for you, in case you have one.
     * @param pos The globally qualified coordinate
     * @return The area instance
     * @see #findBy(ResourceKey, BlockPos)
     */
    @Nonnull
    public Area findBy(GlobalPos pos) {
        return this.findBy(pos.dimension(), pos.pos());
    }

    @Nonnull
    public Area findBy(Level worldInstance, BlockPos pos) {
        // Remember that neither Dimension nor DimensionType are for
        // distinguishing a world - they are information for world
        // generation. Mojang is most likely to allow duplicated
        // overworld in unforeseeable future and at that time neither
        // of them would ever be able to fully qualify a specific
        // world/dimension.
        // The only reliable information is the World.getDimensionKey
        // (func_234923_W_). Yes, this downcast is cursed, but
        // there is no other ways around.
        // We will see how Mojang proceeds. Specifically, the exact
        // meaning of Dimension objects. For now, they seems to be
        // able to fully qualify a world/dimension.
        return this.findBy(worldInstance.dimension(), pos);
    }

    public @Nonnull Area findBy(LevelAccessor maybeLevel, BlockPos pos) {
        if (!DEBUG && maybeLevel.getServer() instanceof IntegratedServer lanServer) {
            if (!lanServer.isPublished()) {
                return this.singlePlayerWildness;
            }
        }
        if (maybeLevel instanceof Level level) {
            return this.findBy(level.dimension(), pos);
        } else if (maybeLevel instanceof ServerLevelAccessor) {
            return this.findBy(((ServerLevelAccessor) maybeLevel).getLevel(), pos);
        } else {
            LOGGER.debug("Use LevelAccessor.dimensionType() to determine dimension id at best effort");
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return this.wildness;
            }
            RegistryAccess registryAccess = server.registryAccess();
            var maybeDimRegistry = registryAccess.registry(Registry.DIMENSION_TYPE_REGISTRY);
            if (maybeDimRegistry.isPresent()) {
                var dimKey = maybeDimRegistry.get().getKey(maybeLevel.dimensionType());
                if (dimKey == null) {
                    LOGGER.warn("Detect unregistered DimensionType; we cannot reliably determine the dimension name. Treat as wildness instead.");
                    return this.wildness;
                }
                return this.findBy(ResourceKey.create(Registry.DIMENSION_REGISTRY, dimKey), pos);
            } else {
                LOGGER.warn("Detect that the DimensionType registry itself is missing. This should be impossible. Treat as wildness instead.");
                return this.wildness;
            }
        }
    }

    @Nonnull
    public Area findBy(ResourceKey<Level> world, BlockPos pos) {
        for (Area area : this.perWorldAreaCache.getOrDefault(world, Collections.emptyMap()).getOrDefault(new ChunkPos(pos), Collections.emptySet())) {
            if (area.minX <= pos.getX() && pos.getX() <= area.maxX) {
                if (area.minY <= pos.getY() && pos.getY() <= area.maxY) {
                    if (area.minZ <= pos.getZ() && pos.getZ() <= area.maxZ) {
                        return area;
                    }
                }
            }
        }
        return AreaManager.INSTANCE.wildness;
    }

    public Area findBy(String name) {
        return this.areasByName.get(name);
    }

    public Collection<Area> getKnownAreas() {
		return Collections.unmodifiableCollection(this.areasByName.values());
	}

    public List<Area.Summary> getAreaSummariesSurround(ResourceKey<Level> dim, BlockPos center, double radius) {
        var ret = new ArrayList<Area.Summary>();
        for (Area area : this.areasByWorld.getOrDefault(dim, Collections.emptySet())) {
            if (center.closerThan(new Vec3i((area.maxX - area.minX) / 2, (area.maxY - area.minY) / 2, (area.maxZ - area.minZ) / 2), radius)) {
                ret.add(new Area.Summary(area));
            }
        }
        return ret;
    }
}
