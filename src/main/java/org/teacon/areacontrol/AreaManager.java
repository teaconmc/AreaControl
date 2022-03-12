package org.teacon.areacontrol;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.areacontrol.api.Area;

public final class AreaManager {

    public static final AreaManager INSTANCE = new AreaManager();

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    final Area wildness = new Area();

    private final HashMap<String, Area> areasByName = new HashMap<>();
    /**
     * All known instances of {@link Area}, indexed by dimensions and chunk positions covered by this area.
     * Used for faster lookup of {@link Area}.
     */
    @Deprecated
    private final IdentityHashMap<DimensionType, Map<ChunkPos, Set<Area>>> areasByChunk = new IdentityHashMap<>();
    private final IdentityHashMap<ResourceKey<Level>, Map<ChunkPos, Set<Area>>> perWorldAreaCache = new IdentityHashMap<>();

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
    }

    private void buildCacheFor(Area area, ResourceKey<Level> worldIndex) {
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
            for (Area a : this.areasByName.values()) {
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
        if (maybeLevel instanceof Level) {
            // TODO Is maybeLevel.dimensionType() actually reliable?
            return this.findBy((Level) maybeLevel, pos);
        } else if (maybeLevel instanceof ServerLevelAccessor) {
            return this.findBy(((ServerLevelAccessor) maybeLevel).getLevel(), pos);
        } else {
            LOGGER.warn("Unrecognized world instance of LevelAccessor passed in for area querying");
            return AreaManager.INSTANCE.wildness;
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

    /**
     * @deprecated It is no longer reliable to get a {@link Level} instance from just a
     * {@link DimensionType} instance alone.
     * Consider use {@link #findBy(ResourceKey, BlockPos)} instead.
     * @param dimType The dimension type object
     * @param pos The position
     * @return The area instance
     */
    @Deprecated
    public Area findBy(DimensionType dimType, BlockPos pos) {
        return this.wildness;
    }

    public Area findBy(String name) {
        return this.areasByName.get(name);
    }

    public Collection<Area> getKnownAreas() {
		return Collections.unmodifiableCollection(this.areasByName.values());
	}
}
