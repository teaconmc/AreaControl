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
import java.util.Optional;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.teacon.areacontrol.api.Area;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;

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
    private final IdentityHashMap<DimensionType, Map<ChunkPos, Set<Area>>> areasByChunk = new IdentityHashMap<>();

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

    private void buildCacheFor(Area area, DimensionType dimType) {
        final Map<ChunkPos, Set<Area>> areasInDim = areasByChunk.computeIfAbsent(dimType, id -> new HashMap<>());
        ChunkPos.getAllInBox(new ChunkPos(area.minX >> 4, area.minZ >> 4), new ChunkPos(area.maxX >> 4, area.maxZ >> 4))
                .map(cp -> areasInDim.computeIfAbsent(cp, _cp -> Collections.newSetFromMap(new IdentityHashMap<>())))
                .forEach(list -> list.add(area));
    }

    void loadFrom(Path dataDirRoot) throws Exception {
        Path userDefinedAreas = dataDirRoot.resolve("claims.json");
        if (Files.isRegularFile(userDefinedAreas)) {
            try (Reader reader = Files.newBufferedReader(userDefinedAreas)) {
                for (Area a : GSON.fromJson(reader, Area[].class)) {
                    this.areasByName.put(a.name, a);
                    // We have to use this registry because dimension may be no longer here
                    // which DimensionType.byName cannot tell us.
                    final Optional<DimensionType> maybeDimType = Registry.DIMENSION_TYPE.getValue(new ResourceLocation(a.dimension));
                    if (maybeDimType.isPresent()) {
                        final DimensionType dimType = maybeDimType.get();
                        //a.dimId = dimType.getId();
                        this.buildCacheFor(a, dimType);
                    } else {
                        LOGGER.warn("GG, area '{}' locates in an unknown dimension '{}', skipping", a.name, a.dimension);
                        LOGGER.warn("We will keep the data for this area, tho - in case you still need the data.");
                    }
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
     * @param dimType The {@link DimensionType} to which the area belongs
     * @return true if and only if the area is successfully recorded by this AreaManager; false otherwise.
     */
    public boolean add(Area area, DimensionType dimType) {
        // First we filter out cases where at least one defining coordinate falls in an existing area
        if (findBy(dimType, new BlockPos(area.minX, area.minY, area.minZ)) == this.wildness 
            && findBy(dimType, new BlockPos(area.maxX, area.maxY, area.maxZ)) == this.wildness) {
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
                this.buildCacheFor(area, dimType);
                // Copy default settings over
                area.properties.putAll(wildness.properties);
                return true;
            }
        }
        return false;
    }

    public void remove(Area area, DimensionType dimType) {
        areasByName.remove(area.name, area);
        areasByChunk.values().forEach(m -> m.values().forEach(l -> l.removeIf(a -> a == area)));
	}

    /**
     * Convenient overload of {@link #findBy(DimensionType, BlockPos)} that unpacks 
     * the {@link GlobalPos} instance for you, in case you have one.
     * @param pos The globally qualified coordiante
     * @return The area instance
     * @see #findBy(DimensionType, BlockPos)
     */
    public Area findBy(GlobalPos pos) {
        return findBy(pos.getDimension(), pos.getPos());
    }

    /**
     * @deprecated Using integral id is NOT reliable, use {@link #findBy(DimensionType, BlockPos)} instead.
     *             This method is subject to removal at any time point without notification.
     * 
     * @param dimId The integral id of the dimension
     * @param pos The 3D coordinate
     * @return The area instance
     */
    @Deprecated
    public Area findBy(int dimId, BlockPos pos) {
        final DimensionType dimType = Registry.DIMENSION_TYPE.getByValue(dimId);
        return dimType == null ? this.wildness : findBy(dimType, pos);
    }

    public Area findBy(DimensionType dimType, BlockPos pos) {
        for (Area area : this.areasByChunk.getOrDefault(dimType, Collections.emptyMap()).getOrDefault(new ChunkPos(pos), Collections.emptySet())) {
            if (area.minX <= pos.getX() && pos.getX() <= area.maxX) {
                if (area.minY <= pos.getY() && pos.getY() <= area.maxY) {
                    if (area.minZ <= pos.getZ() && pos.getZ() <= area.maxZ) {
                        return area;
                    }
                }
            }
        }
        return this.wildness;
    }

    public Area findBy(String name) {
        return areasByName.get(name);
    }

    public Collection<Area> getKnownAreas() {
		return Collections.unmodifiableCollection(this.areasByName.values());
	}
}
