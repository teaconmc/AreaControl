package org.teacon.areacontrol;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import org.teacon.areacontrol.impl.AreaMath;
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
            for (Area a : this.repository.load()) {
                this.buildCacheFor(a, ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(a.dimension)));
            }
            var danglingAreas = new ArrayList<Area>();
            for (Area a : this.areasById.values()) {
                if (a.belongingArea != null) {
                    Area parent = this.areasById.get(a.belongingArea);
                    if (parent != null) {
                        parent.subAreas.add(a.uid);
                    } else {
                        LOGGER.warn("Found a dangling area " + a.uid + ", will try to auto-fix this...");
                        danglingAreas.add(a);
                    }
                }
            }
            // Try fixing dangling areas.
            // The assumption is that the correct parent would have the smallest volume.
            // The algorithm here is to find all areas that are superset of the dangling area,
            // then find the area with the smallest volume.
            // If no parent area found, assuming the parent is the wildness, i.e. parent UID is null.
            for (Area dangling : danglingAreas) {
                List<Area> possibleParents = new ArrayList<>();
                var dimKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(dangling.dimension));
                for (var maybeParentUid : this.areasByWorld.getOrDefault(dimKey, Collections.emptySet())) {
                    var maybeParent = this.areasById.get(maybeParentUid);
                    if (maybeParent != null && AreaMath.isEnclosing(maybeParent, dangling) && !AreaMath.isCoveringSameArea(maybeParent, dangling)) {
                        possibleParents.add(maybeParent);
                    }
                }
                var minVolume = BigInteger.valueOf(1L << 32).pow(3);
                UUID theParent = null;
                if (!possibleParents.isEmpty()) {
                    for (var maybeParent : possibleParents) {
                        var vol = BigInteger.valueOf(maybeParent.maxX).subtract(BigInteger.valueOf(maybeParent.minX))
                                .multiply(BigInteger.valueOf(maybeParent.maxY).subtract(BigInteger.valueOf(maybeParent.minY)))
                                .multiply(BigInteger.valueOf(maybeParent.maxZ).subtract(BigInteger.valueOf(maybeParent.minZ)));
                        if (vol.compareTo(minVolume) < 0) {
                            minVolume = vol;
                            theParent = maybeParent.uid;
                        }
                    }
                }
                dangling.belongingArea = theParent;
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
            var maybeOverlaps = Util.verticesOf(area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ)
                    .map(cornerPos -> findBy(worldIndex, cornerPos))
                    .collect(Collectors.toCollection(() -> Collections.newSetFromMap(new IdentityHashMap<>())));
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
                enclosing.subAreas.remove(area.uid);
            }
            for (var subAreaUid : area.subAreas) {
                var subArea = this.findBy(subAreaUid);
                subArea.belongingArea = area.belongingArea;
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

    public void rename(Area area, String newName) {
        this.areasByName.remove(area.name);
        area.name = newName;
        this.areasByName.put(newName, area);
    }

    public boolean changeRangeForArea(ResourceKey<Level> dim, Area area, Direction direction, int amount) {
        if (amount == 0) { // Who would do that?!
            return true;
        }
        if (direction == null || Area.GLOBAL_AREA_OWNER.equals(area.owner)) {
            return false;
        }
        // Calculate the range after change
        int minX = area.minX, minY = area.minY, minZ = area.minZ, maxX = area.maxX, maxY = area.maxY, maxZ = area.maxZ;
        switch (direction) {
            case DOWN -> {
                if (minY == Integer.MIN_VALUE) {
                    return false;
                }
                minY -= amount;
                break;
            }
            case UP -> {
                if (maxY == Integer.MAX_VALUE) {
                    return false;
                }
                maxY += amount;
                break;
            }
            case NORTH -> {
                if (minZ == Integer.MIN_VALUE) {
                    return false;
                }
                minZ -= amount;
                break;
            }
            case SOUTH -> {
                if (maxZ == Integer.MAX_VALUE) {
                    return false;
                }
                maxZ += amount;
                break;
            }
            case WEST -> {
                if (minX == Integer.MIN_VALUE) {
                    return false;
                }
                minX -= amount;
                break;
            }
            case EAST -> {
                if (maxX == Integer.MAX_VALUE) {
                    return false;
                }
                maxX += amount;
                break;
            }
        }
        // Check if the change amount is positive/negative
        if (amount > 0) {
            // Positive means expansion.
            // No existing areas should thus have overlap with this area, or being swallowed after expansion.
            // TODO Allow automatic area "swallowing", this would require heavy calculation...
            var areaByChunkPos = this.perWorldAreaCache.getOrDefault(dim, Collections.emptyMap());
            var maybeOverlaps = ChunkPos.rangeClosed(new ChunkPos(minX >> 4, minZ >> 4), new ChunkPos(maxX >> 4, maxZ >> 4))
                    .flatMap(cp -> areaByChunkPos.getOrDefault(cp, Collections.emptySet()).stream())
                    .collect(Collectors.toSet());
            for (var maybeOverlapId : maybeOverlaps) {
                var maybeOverlap = this.findBy(maybeOverlapId);
                if (maybeOverlap != area) {
                    var relation = AreaMath.relationBetween(minX, minY, minZ, maxX, maxY, maxZ, maybeOverlap);
                    if (relation == AreaMath.SetRelation.INTERSECT || relation == AreaMath.SetRelation.SAME) {
                        // No overlap or being the same.
                        return false;
                    } else if (relation == AreaMath.SetRelation.SUPERSET) {
                        // No swallowing.
                        // If maybeOverlap is sub-area of current area, it must be known.
                        if (!this.isSub(area, maybeOverlap)) {
                            return false;
                        }
                    }
                }
            }
        } else {
            // Negative means contraction.
            // No existing sub-areas should thus have overlap with this area, or become dangling.
            // TODO Allow automatic dangling, this would require heavy calculation...
            for (var subId : area.subAreas) {
                var sub = this.findBy(subId);
                if (minX > sub.minX || sub.maxX > maxX || minY > sub.minY || sub.maxY > maxY || minZ > sub.minZ || sub.maxZ > maxZ) {
                    return false;
                }
            }
        }
        area.minX = minX;
        area.minY = minY;
        area.minZ = minZ;
        area.maxX = maxX;
        area.maxY = maxY;
        area.maxZ = maxZ;
        this.perWorldAreaCache.values().forEach(m -> m.values().forEach(l -> l.removeIf(uid -> uid == area.uid)));
        final var areasInDim = this.perWorldAreaCache.computeIfAbsent(dim, id -> new HashMap<>());
        ChunkPos.rangeClosed(new ChunkPos(area.minX >> 4, area.minZ >> 4), new ChunkPos(area.maxX >> 4, area.maxZ >> 4))
                .map(cp -> areasInDim.computeIfAbsent(cp, _cp -> Collections.newSetFromMap(new IdentityHashMap<>())))
                .forEach(list -> list.add(area.uid));
        return true;
    }

    public boolean isSub(Area parent, Area sub) {
        return parent.minX <= sub.minX && sub.maxX <= parent.maxX
                && parent.minY <= sub.minY && sub.maxY <= parent.maxY
                && parent.minZ <= sub.minZ && sub.maxZ <= parent.maxZ;
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
    public Area findDefaultBy(ResourceKey<Level> key) {
        var writeLock = this.lock.writeLock();
        try {
            writeLock.lock();
            var areaUid = this.wildnessByWorld.get(key);
            if (areaUid != null) {
                var area = this.areasById.get(areaUid);
                if (area != null) {
                    return area;
                }
            }
            var newCreated = AreaFactory.defaultWildness(key);
            buildCacheFor(newCreated, key);
            return newCreated;
        } finally {
            writeLock.unlock();
        }
    }

    @Nonnull
    public Area findBy(ResourceKey<Level> world, BlockPos pos) {
        return findWithExclusion(world, pos, null);
    }

    @Nonnull
    public Area findWithExclusion(ResourceKey<Level> world, BlockPos pos, Area excluded) {
        var readLock = this.lock.readLock();
        try {
            readLock.lock();
            var results = new ArrayList<Area>();
            // Locate all areas that contain the specified position
            for (var uuid : this.perWorldAreaCache.getOrDefault(
                    world, Collections.emptyMap()).getOrDefault(new ChunkPos(pos), Collections.emptySet())) {
                Area area = this.areasById.get(uuid);
                if (area == null || area == excluded) {
                    continue;
                }
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
            // Else, we find the deepest area in our selected area.
            // Such area must have no child area in the set of matched areas.
            // Example: two areas, A and B, are matched. B is sub-area of A.
            // Since one of A's sub-area is in the set of matches area,
            // it would not be the deepest area. Regardless whether B has
            // sub-areas or not, none of the areas other than B are sub-area
            // of B, so B is the deepest area in our set.
            /*outer: for (Area a : results) {
                for (Area maybeChild : results) {
                    if (a != maybeChild && a.subAreas.contains(maybeChild.uid)) {
                        continue outer;
                    }
                }
                return a;
            }*/
            // TODO I still don't believe this is correct, but I am bugged everyday for this.
            //   Need to find some time to verify.
            Area result = null;
            long minVolume = Long.MAX_VALUE;
            for (var a : results) {
                long x = Math.min(a.maxX, 3000_0000) - Math.max(a.minX, -3000_0000);
                long y = Math.min(a.maxY, 320) - Math.max(a.minY, -64);
                long z = Math.min(a.maxZ, 3000_0000) - Math.max(a.minZ, -3000_0000);
                long vol = x * y * z;
                if (vol < minVolume) {
                    result = a;
                    minVolume = vol;
                } else if (vol == minVolume && result != null) {
                    if (result.minX <= a.minX && a.maxX <= result.maxX
                            && result.minY <= a.minY && a.maxY <= result.maxY
                            && result.minZ <= a.minZ && a.maxZ <= result.maxZ) {
                        result = a;
                    }
                }
            }
            if (result != null) {
                return result;
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
                int xDiff = area.minX + (area.maxX - area.minX) / 2 - center.getX(), zDiff = area.minZ + (area.maxZ - area.minZ) / 2 - center.getZ();
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
