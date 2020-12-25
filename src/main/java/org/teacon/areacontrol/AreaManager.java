package org.teacon.areacontrol;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.teacon.areacontrol.api.Area;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public final class AreaManager {

    public static final AreaManager INSTANCE = new AreaManager();

    private static final Logger LOGGER = LogManager.getLogger("AreaControl");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    final Area wildness = new Area();

    private final HashMap<String, Area> areasByName = new HashMap<>();
    /**
     * All known instances of {@link Area}, indexed by dimensions and chunk positions covered by this area.
     * Used for faster lookup of {@link Area}.
     */
    @Deprecated
    private final IdentityHashMap<DimensionType, Map<ChunkPos, Set<Area>>> areasByChunk = new IdentityHashMap<>();
    private final IdentityHashMap<RegistryKey<World>, Map<ChunkPos, Set<Area>>> perWorldAreaCache = new IdentityHashMap<>();

    {
        wildness.name = "wildness";
        wildness.minX = Integer.MIN_VALUE;
        wildness.minY = Integer.MIN_VALUE;
        wildness.minZ = Integer.MIN_VALUE;
        wildness.maxX = Integer.MAX_VALUE;
        wildness.maxY = Integer.MAX_VALUE;
        wildness.maxZ = Integer.MAX_VALUE;
        wildness.properties.put("area.allow_click_block", Boolean.TRUE);
        wildness.properties.put("area.allow_activate_block", Boolean.TRUE);
        wildness.properties.put("area.allow_use_item", Boolean.TRUE);
        wildness.properties.put("area.allow_interact_entity", Boolean.TRUE);
        wildness.properties.put("area.allow_interact_entity_specific", Boolean.TRUE);
    }

    private void buildCacheFor(Area area, RegistryKey<World> worldIndex) {
        final Map<ChunkPos, Set<Area>> areasInDim = perWorldAreaCache.computeIfAbsent(worldIndex, id -> new HashMap<>());
        ChunkPos.getAllInBox(new ChunkPos(area.minX >> 4, area.minZ >> 4), new ChunkPos(area.maxX >> 4, area.maxZ >> 4))
                .map(cp -> areasInDim.computeIfAbsent(cp, _cp -> Collections.newSetFromMap(new IdentityHashMap<>())))
                .forEach(list -> list.add(area));
    }

    void loadFrom(MinecraftServer server, Path dataDirRoot) throws Exception {
        Path userDefinedAreas = dataDirRoot.resolve("claims.json");
        if (Files.isRegularFile(userDefinedAreas)) {
            try (Reader reader = Files.newBufferedReader(userDefinedAreas)) {
                for (Area a : GSON.fromJson(reader, Area[].class)) {
                    this.areasByName.put(a.name, a);
                    this.buildCacheFor(a, RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(a.dimension)));
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
        Files.write(dataDirRoot.resolve("claims.json"), GSON.toJson(this.areasByName.values()).getBytes(StandardCharsets.UTF_8));
        Files.write(dataDirRoot.resolve("wildness.json"), GSON.toJson(this.wildness).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param area The Area instance to be recorded
     * @param worldIndex The {@link RegistryKey<World>} of the {@link World} to which the area belongs
     * @return true if and only if the area is successfully recorded by this AreaManager; false otherwise.
     */
    public boolean add(Area area, RegistryKey<World> worldIndex) {
        // First we filter out cases where at least one defining coordinate falls in an existing area
        if (findBy(worldIndex, new BlockPos(area.minX, area.minY, area.minZ)) == this.wildness
            && findBy(worldIndex, new BlockPos(area.maxX, area.maxY, area.maxZ)) == this.wildness) {
            // Second we filter out cases where the area to define is enclosing another area.
            boolean noEnclosing = true;
            for (Area a : areasByName.values()) {
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
                areasByName.computeIfAbsent(area.name, name -> area);
                this.buildCacheFor(area, worldIndex);
                // Copy default settings over
                area.properties.putAll(wildness.properties);
                return true;
            }
        }
        return false;
    }

    public void remove(Area area, RegistryKey<World> worldIndex) {
        areasByName.remove(area.name, area);
        perWorldAreaCache.values().forEach(m -> m.values().forEach(l -> l.removeIf(a -> a == area)));
	}

    /**
     * Convenient overload of {@link #findBy(RegistryKey, BlockPos)} that unpacks
     * the {@link GlobalPos} instance for you, in case you have one.
     * @param pos The globally qualified coordinate
     * @return The area instance
     * @see #findBy(RegistryKey, BlockPos)
     */
    public Area findBy(GlobalPos pos) {
        return this.findBy(pos.getDimension(), pos.getPos());
    }

    public Area findBy(IWorld worldInstance, BlockPos pos) {
        try {
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
            return this.findBy(((World)worldInstance).getDimensionKey(), pos);
        } catch (Exception e) {
            LOGGER.warn("Non-world instances of IWorld passed in for area querying", e);
            return AreaManager.INSTANCE.wildness;
        }
    }

    public Area findBy(RegistryKey<World> world, BlockPos pos) {
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
     * @deprecated It is no longer reliable to get a {@link IWorld} instance from just a
     * {@link DimensionType} instance alone.
     * Consider use {@link #findBy(RegistryKey, BlockPos)} instead.
     * @param dimType The dimension type object
     * @param pos The position
     * @return The area instance
     */
    @Deprecated
    public Area findBy(DimensionType dimType, BlockPos pos) {
        return this.wildness;
    }

    public Area findBy(String name) {
        return areasByName.get(name);
    }

    public Collection<Area> getKnownAreas() {
		return Collections.unmodifiableCollection(this.areasByName.values());
	}
}
