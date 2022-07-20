package org.teacon.areacontrol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
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
    private final IdentityHashMap<ResourceKey<Level>, Map<ChunkPos, Set<UUID>>> perWorldAreaCache = new IdentityHashMap<>();
    /**
     * All known instances of {@link Area}, indexed by dimensions only. Used for situations such as querying
     * areas in a larger range.
     */
    private final IdentityHashMap<ResourceKey<Level>, Set<UUID>> areasByWorld = new IdentityHashMap<>();
    /**
     * All instances of "wildness" area, indexed by dimension.
     * They all have owner as {@link Area#GLOBAL_AREA_OWNER}.
     */
    private final IdentityHashMap<ResourceKey<Level>, UUID> wildnessByWorld = new IdentityHashMap<>();

    private void buildCacheFor(Area area, ResourceKey<Level> worldIndex) {
        // not locked since every method invoking this one has been locked
        this.areasById.put(area.uid, area);
        this.areasByName.put(area.name, area);
        this.areasByWorld.compute(worldIndex, (key, areas) -> {
            if (areas == null) {
                areas = new HashSet<>();
            }
            areas.add(area.uid);
            return areas;
        });
        if (Area.GLOBAL_AREA_OWNER.equals(area.owner)) {
            this.wildnessByWorld.put(worldIndex, area.uid);
            return;
        }
        final var areasInDim = this.perWorldAreaCache.computeIfAbsent(worldIndex, id -> new HashMap<>());
        ChunkPos.rangeClosed(new ChunkPos(area.minX >> 4, area.minZ >> 4), new ChunkPos(area.maxX >> 4, area.maxZ >> 4))
                .map(cp -> areasInDim.computeIfAbsent(cp, _cp -> Collections.newSetFromMap(new IdentityHashMap<>())))
                .forEach(list -> list.add(area.uid));
    }

    void init(AreaRepository repository) {
        this.repository = repository;
        this.areasById.clear();
        this.areasByName.clear();
        this.areasByWorld.clear();
        this.perWorldAreaCache.clear();
        this.wildnessByWorld.clear();
    }

    void load() throws Exception {
        var writeLock = this.lock.writeLock();
        try {
            writeLock.lock();
            // Read all areas
            for (Area a : this.repository.load()) {
                var dim = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(a.dimension));
                this.buildCacheFor(a, dim);
            }
            // Check for invalid areas.
            // An area is invalid if:
            //   1. Has exactly the same range as another area
            //   2. Has intersection ("overlap") with one or more area(s), but not being a subset of that area/those areas.
            var invalidArea = new ArrayList<Area>();
            for (Iterator<Area> itr = this.areasById.values().iterator(); itr.hasNext(); ) {
                Area a = itr.next();
                var dim = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(a.dimension));
                var maybeOverlaps = Stream.of(
                        new BlockPos(a.minX, a.minY, a.minZ),
                        new BlockPos(a.minX, a.minY, a.maxZ),
                        new BlockPos(a.minX, a.maxY, a.minZ),
                        new BlockPos(a.minX, a.maxY, a.maxZ),
                        new BlockPos(a.maxX, a.minY, a.minZ),
                        new BlockPos(a.maxX, a.minY, a.maxZ),
                        new BlockPos(a.maxX, a.maxY, a.minZ),
                        new BlockPos(a.maxX, a.maxY, a.maxZ)
                ).map(cornerPos -> findBy(dim, cornerPos)).collect(Collectors.toCollection(() -> Collections.newSetFromMap(new IdentityHashMap<>())));
                Area parent = maybeOverlaps.iterator().next();
                // An area ia invalid if and only if all of its 8 defining vertices fall into 2 or more
                // different areas. To have a valid area, the size of maybeOverlaps must be exactly 1.
                // If the area is the wildness (global area for a certain dimension), then both `parent`
                // and `a` must point to the same reference.
                // Finally, an area is invalid if and only if all 8 vertices are exactly the same as of
                // another area.
                if (maybeOverlaps.size() == 1 && (parent == a || parent.minX != a.minX || parent.minY != a.minY || parent.minZ != a.minZ || parent.maxX != a.maxX || parent.maxY != a.maxY || parent.maxZ != a.maxZ)) {
                    if (a.belongingArea != null) {
                        Area realParent = this.areasById.get(a.belongingArea);
                        if (realParent != null) {
                            realParent.subAreas.add(a.uid);
                        } else {
                            LOGGER.warn("Area with UID {}, which encloses area {} (UID {}), no longer exists. The enclosed area will forget that.", a.belongingArea, a.name, a.uid);
                            a.belongingArea = null;
                        }
                    }
                } else {
                    LOGGER.warn("Area {} (UID {}) has overlap with at least one existing area; it should not happen and will be removed.", a.name, a.uid);
                    LOGGER.warn("Overlapping areas: {}", maybeOverlaps.stream().map(overlap -> "Area " + overlap.name + " (" + overlap.uid + ")").toList());
                    invalidArea.add(a);
                    itr.remove();
                }
            }
            // TODO Make auto-remove optional
            for (var a : invalidArea) {
                this.remove(a, ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(a.dimension)));
            }
        } finally {
            writeLock.unlock();
        }
    }

    void saveDimension(ResourceKey<Level> key) throws Exception {
        var writeLock = this.lock.writeLock();
        try {
            writeLock.lock();
            this.repository.save(this.areasById.values().stream()
                    .filter(a -> a.dimension.equals(key.location().toString())).toList());
        } finally {
            writeLock.unlock();
        }
    }

    void save() throws Exception {
        var writeLock = this.lock.writeLock();
        try {
            writeLock.lock();
            this.repository.save(this.areasById.values());
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * @param area       The Area instance to be recorded
     * @param worldIndex The {@link ResourceKey<Level>} of the {@link Level} to which the area belongs
     * @return true if and only if the area is successfully recorded by this AreaManager; false otherwise.
     */
    public boolean add(Area area, ResourceKey<Level> worldIndex) {
        var writeLock = this.lock.writeLock();
        try {
            writeLock.lock();
            // First we check if at least one vertex of the defining cuboid falls in an existing area
            var maybeOverlaps = Stream.of(
                    new BlockPos(area.minX, area.minY, area.minZ),
                    new BlockPos(area.minX, area.minY, area.maxZ),
                    new BlockPos(area.minX, area.maxY, area.minZ),
                    new BlockPos(area.minX, area.maxY, area.maxZ),
                    new BlockPos(area.maxX, area.minY, area.minZ),
                    new BlockPos(area.maxX, area.minY, area.maxZ),
                    new BlockPos(area.maxX, area.maxY, area.minZ),
                    new BlockPos(area.maxX, area.maxY, area.maxZ)
            ).map(cornerPos -> findBy(worldIndex, cornerPos)).collect(Collectors.toCollection(() -> Collections.newSetFromMap(new IdentityHashMap<>())));
            // We need to make sure that all 8 vertices fall into the same area.
            if (maybeOverlaps.size() != 1) {
                return false;
            }
            // There are two possibilities:
            // 1. theEnclosingArea is the wildness
            // 2. theEnclosingArea is an existing area claimed by someone
            var theEnclosingArea = maybeOverlaps.iterator().next();
            {
                // Then, we check if the defining cuboid is enclosing another area.
                boolean noEnclosing = true;
                for (var uuid : this.areasByWorld.getOrDefault(worldIndex, Collections.emptySet())) {
                    Area a = this.areasById.get(uuid);
                    if (a == null) continue;
                    if (area.minX <= a.minX && a.maxX <= area.maxX) {
                        if (area.minY <= a.minY && a.maxY <= area.maxY) {
                            if (area.minZ <= a.minZ && a.maxZ <= area.maxZ) {
                                noEnclosing = false;
                                break;
                            }
                        }
                    }
                }
                // If not, we consider this to be a success.
                if (noEnclosing) {
                    this.buildCacheFor(area, worldIndex);
                    area.properties.putAll(theEnclosingArea.properties);
                    // Copy default settings over
                    if (!theEnclosingArea.owner.equals(Area.GLOBAL_AREA_OWNER)) {
                        area.belongingArea = theEnclosingArea.uid;
                        theEnclosingArea.subAreas.add(area.uid);
                    }
                    return true;
                }
                return false;
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void remove(Area area, ResourceKey<Level> worldIndex) {
        var writeLock = this.lock.writeLock();
        try {
            writeLock.lock();
            this.areasById.remove(area.uid, area);
            this.areasByName.remove(area.name, area);
            this.perWorldAreaCache.values().forEach(m -> m.values().forEach(l -> l.removeIf(uid -> uid == area.uid)));
            this.areasByWorld.getOrDefault(worldIndex, Collections.emptySet()).remove(area.uid);
            if (area.belongingArea != null) {
                Area enclosing = this.areasById.get(area.belongingArea);
                if (enclosing != null) {
                    enclosing.subAreas.remove(area.uid);
                }
            }
            try {
                this.repository.remove(area);
            } catch (Exception e) {
                LOGGER.error("Failed to remove data for area " + area.uid, e);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Convenient overload of {@link #findBy(ResourceKey, BlockPos)} that unpacks
     * the {@link GlobalPos} instance for you, in case you have one.
     *
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
        if (!DEBUG && AreaControl.singlePlayerServerChecker.test(worldInstance.getServer())) {
            return this.singlePlayerWildness;
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
        if (!DEBUG && AreaControl.singlePlayerServerChecker.test(maybeLevel.getServer())) {
            return this.singlePlayerWildness;
        }
        if (maybeLevel instanceof Level level) {
            return this.findBy(level.dimension(), pos);
        } else if (maybeLevel instanceof ServerLevelAccessor) {
            return this.findBy(((ServerLevelAccessor) maybeLevel).getLevel(), pos);
        } else {
            LOGGER.debug("Use LevelAccessor.dimensionType() to determine dimension id at best effort");
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return findDefaultBy(Level.OVERWORLD);
            }
            RegistryAccess registryAccess = server.registryAccess();
            var maybeDimRegistry = registryAccess.registry(Registry.DIMENSION_TYPE_REGISTRY);
            if (maybeDimRegistry.isPresent()) {
                var dimKey = maybeDimRegistry.get().getKey(maybeLevel.dimensionType());
                if (dimKey != null) {
                    return this.findBy(ResourceKey.create(Registry.DIMENSION_REGISTRY, dimKey), pos);
                }
                LOGGER.warn("Detect unregistered DimensionType; we cannot reliably determine the dimension name. Treat as overworld wildness instead.");
            } else {
                LOGGER.warn("Detect that the DimensionType registry itself is missing. This should be impossible. Treat as overworld wildness instead.");
            }
            return findDefaultBy(Level.OVERWORLD);
        }
    }

    @Nonnull
    private Area findDefaultBy(ResourceKey<Level> key) {
        var writeLock = this.lock.writeLock();
        try {
            writeLock.lock();
            var def = this.areasById.get(this.wildnessByWorld.getOrDefault(key, UUID.randomUUID()));
            return Objects.requireNonNullElseGet(def, () -> {
                var newCreated = AreaFactory.defaultWildness(key);
                buildCacheFor(newCreated, key);
                return newCreated;
            });
        } finally {
            writeLock.unlock();
        }
    }

    @Nonnull
    public Area findBy(ResourceKey<Level> world, BlockPos pos) {
        var readLock = this.lock.readLock();
        try {
            readLock.lock();
            Set<Area> results = Collections.newSetFromMap(new IdentityHashMap<>());
            // Locate all areas that contain the specified position
            for (var uuid : this.perWorldAreaCache.getOrDefault(
                    world, Collections.emptyMap()).getOrDefault(new ChunkPos(pos), Collections.emptySet())) {
                Area area = this.areasById.get(uuid);
                if (area == null) continue;
                if (area.minX <= pos.getX() && pos.getX() <= area.maxX) {
                    if (area.minY <= pos.getY() && pos.getY() <= area.maxY) {
                        if (area.minZ <= pos.getZ() && pos.getZ() <= area.maxZ) {
                            results.add(area);
                        }
                    }
                }
            }
            // If there is only one area, it will be our result.
            if (results.size() == 1) {
                return results.iterator().next();
            }
            // Else, we find the area that does not contain any sub-area.
            // Think about it like a tree (in data structure): leaf node does not have child node.
            // Similarly, area that doesn't contain sub-area should be the deepest one on that location.
            for (Area a : results) {
                if (a.subAreas.isEmpty()) {
                    return a;
                }
            }
        } finally {
            readLock.unlock();
        }
        return findDefaultBy(world);
    }

    public Area findBy(UUID uid) {
        var readLock = this.lock.readLock();
        try {
            readLock.lock();
            return this.areasById.get(uid);
        } finally {
            readLock.unlock();
        }
    }

    public Collection<Area> findByOwner(UUID ownerId) {
        return this.areasById.values().stream().filter(area -> ownerId.equals(area.owner)).toList();
    }

    public Area findBy(String name) {
        var readLock = this.lock.readLock();
        try {
            readLock.lock();
            return this.areasByName.get(name);
        } finally {
            readLock.unlock();
        }
    }

    public List<Area> getAreaSummariesSurround(ResourceKey<Level> dim, BlockPos center, double radius) {
        var readLock = this.lock.readLock();
        try {
            readLock.lock();
            var ret = new ArrayList<Area>();
            for (var uuid : this.areasByWorld.getOrDefault(dim, Collections.emptySet())) {
                Area area = this.areasById.get(uuid);
                if (area == null || Area.GLOBAL_AREA_OWNER.equals(area.owner)) continue;
                int xDiff = (area.maxX - area.minX) / 2 - center.getX(), zDiff = (area.maxZ - area.minZ) / 2 - center.getZ();
                if (Math.abs(xDiff) < radius && Math.abs(zDiff) < radius) {
                    ret.add(area);
                }
            }
            return ret;
        } finally {
            readLock.unlock();
        }
    }
}
