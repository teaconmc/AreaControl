package org.teacon.areacontrol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.impl.AreaFactory;
import org.teacon.areacontrol.impl.persistence.AreaRepository;

public final class AreaManager {

    public static final AreaManager INSTANCE = new AreaManager();

    public static final boolean DEBUG = Boolean.getBoolean("area_control.dev");
    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");

    private final Area singlePlayerWildness = AreaFactory.singlePlayerWildness();

    private AreaRepository repository = null;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final HashMap<UUID, Area> areasById = new HashMap<>();
    private final HashMap<String, Area> areasByName = new HashMap<>();
    /**
     * All known instances of {@link Area}, indexed by dimensions and chunk positions covered by this area.
     * Used for faster lookup of {@link Area} when the position is known.
     */
    private final IdentityHashMap<ResourceKey<Level>, Map<ChunkPos, Set<Area>>> perWorldAreaCache = new IdentityHashMap<>();
    /**
     * All known instances of {@link Area}, indexed by dimensions only. Used for situations such as querying
     * areas in a larger range.
     */
    private final IdentityHashMap<ResourceKey<Level>, Set<Area>> areasByWorld = new IdentityHashMap<>();
    /**
     * All instances of "wildness" area, indexed by dimension.
     * They all have owner as {@link Area#GLOBAL_AREA_OWNER}.
     */
    private final IdentityHashMap<ResourceKey<Level>, Area> wildnessByWorld = new IdentityHashMap<>();

    private void buildCacheFor(Area area, ResourceKey<Level> worldIndex) {
        // not locked since every method invoking this one has been locked
        this.areasById.put(area.uid, area);
        this.areasByName.put(area.name, area);
        this.areasByWorld.compute(worldIndex, (key, areas) -> {
            if (areas == null) {
                areas = new HashSet<>();
            }
            areas.add(area);
            return areas;
        });
        if (Area.GLOBAL_AREA_OWNER.equals(area.owner)) {
            this.wildnessByWorld.put(worldIndex, area);
            return;
        }
        final Map<ChunkPos, Set<Area>> areasInDim = this.perWorldAreaCache.computeIfAbsent(worldIndex, id -> new HashMap<>());
        ChunkPos.rangeClosed(new ChunkPos(area.minX >> 4, area.minZ >> 4), new ChunkPos(area.maxX >> 4, area.maxZ >> 4))
                .map(cp -> areasInDim.computeIfAbsent(cp, _cp -> Collections.newSetFromMap(new IdentityHashMap<>())))
                .forEach(list -> list.add(area));
    }

    void init(AreaRepository repository) {
        this.repository = repository;
    }

    void load() throws Exception {
        var writeLock = this.lock.writeLock();
        try {
            for (Area a : this.repository.load()) {
                this.buildCacheFor(a, ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(a.dimension)));
            }
        } finally {
            writeLock.unlock();
        }
    }

    void save() throws Exception {
        var writeLock = this.lock.writeLock();
        try {
            this.repository.save(this.areasById.values());
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * @param area The Area instance to be recorded
     * @param worldIndex The {@link ResourceKey<Level>} of the {@link Level} to which the area belongs
     * @return true if and only if the area is successfully recorded by this AreaManager; false otherwise.
     */
    public boolean add(Area area, ResourceKey<Level> worldIndex) {
        var writeLock = this.lock.writeLock();
        try {
            // First we filter out cases where at least one defining coordinate falls in an existing area
            if (findBy(worldIndex, new BlockPos(area.minX, area.minY, area.minZ)).owner.equals(Area.GLOBAL_AREA_OWNER)
                && findBy(worldIndex, new BlockPos(area.maxX, area.maxY, area.maxZ)).owner.equals(Area.GLOBAL_AREA_OWNER)) {
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
                    this.buildCacheFor(area, worldIndex);
                    // Copy default settings over
                    area.properties.putAll(this.wildnessByWorld.computeIfAbsent(worldIndex, AreaFactory::defaultWildness).properties);
                    return true;
                }
            }
            return false;
        } finally {
            writeLock.unlock();
        }
    }

    public void remove(Area area, ResourceKey<Level> worldIndex) {
        var writeLock = this.lock.writeLock();
        try {
            this.areasByName.remove(area.name, area);
            this.perWorldAreaCache.values().forEach(m -> m.values().forEach(l -> l.removeIf(a -> a == area)));
            this.areasByWorld.getOrDefault(worldIndex, Collections.emptySet()).remove(area);
        } finally {
            writeLock.unlock();
        }
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
        if (!DEBUG && worldInstance.getServer() instanceof IntegratedServer lanServer) {
            if (!lanServer.isPublished()) {
                return this.singlePlayerWildness;
            }
        }
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
                var readLock = this.lock.readLock();
                try {
                    return this.wildnessByWorld.computeIfAbsent(Level.OVERWORLD, AreaFactory::defaultWildness);
                } finally {
                    readLock.unlock();
                }
            }
            RegistryAccess registryAccess = server.registryAccess();
            var maybeDimRegistry = registryAccess.registry(Registry.DIMENSION_TYPE_REGISTRY);
            if (maybeDimRegistry.isPresent()) {
                var dimKey = maybeDimRegistry.get().getKey(maybeLevel.dimensionType());
                if (dimKey != null) {
                    var readLock = this.lock.readLock();
                    try {
                        return this.findBy(ResourceKey.create(Registry.DIMENSION_REGISTRY, dimKey), pos);
                    } finally {
                        readLock.unlock();
                    }
                }
                LOGGER.warn("Detect unregistered DimensionType; we cannot reliably determine the dimension name. Treat as overworld wildness instead.");
            } else {
                LOGGER.warn("Detect that the DimensionType registry itself is missing. This should be impossible. Treat as overworld wildness instead.");
            }
            var readLock = this.lock.readLock();
            try {
                return this.wildnessByWorld.computeIfAbsent(Level.OVERWORLD, AreaFactory::defaultWildness);
            } finally {
                readLock.unlock();
            }
        }
    }

    @Nonnull
    public Area findBy(ResourceKey<Level> world, BlockPos pos) {
        var readLock = this.lock.readLock();
        try {
            for (Area area : this.perWorldAreaCache.getOrDefault(world, Collections.emptyMap()).getOrDefault(new ChunkPos(pos), Collections.emptySet())) {
                if (area.minX <= pos.getX() && pos.getX() <= area.maxX) {
                    if (area.minY <= pos.getY() && pos.getY() <= area.maxY) {
                        if (area.minZ <= pos.getZ() && pos.getZ() <= area.maxZ) {
                            return area;
                        }
                    }
                }
            }
            return this.wildnessByWorld.computeIfAbsent(world, AreaFactory::defaultWildness);
        } finally {
            readLock.unlock();
        }
    }

    public Area findBy(String name) {
        var readLock = this.lock.readLock();
        try {
            return this.areasByName.get(name);
        } finally {
            readLock.unlock();
        }
    }

    public List<Area.Summary> getAreaSummariesSurround(ResourceKey<Level> dim, BlockPos center, double radius) {
        var readLock = this.lock.readLock();
        try {
            var ret = new ArrayList<Area.Summary>();
            for (Area area : this.areasByWorld.getOrDefault(dim, Collections.emptySet())) {
                if (center.closerThan(new Vec3i((area.maxX - area.minX) / 2, (area.maxY - area.minY) / 2, (area.maxZ - area.minZ) / 2), radius)) {
                    ret.add(new Area.Summary(area));
                }
            }
            return ret;
        } finally {
            readLock.unlock();
        }
    }
}
