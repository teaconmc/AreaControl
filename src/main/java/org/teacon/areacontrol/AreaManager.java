package org.teacon.areacontrol;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.teacon.areacontrol.api.Area;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class AreaManager {

    public static final AreaManager INSTANCE = new AreaManager();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    final Area wildness = new Area();

    private final HashMap<String, Area> areasByName = new HashMap<>();
    /**
     * All known instances of {@link Area}, indexed by chunk positions covered by this area. 
     * Used for faster lookup of {@link Area}.
     */
    private final HashMap<ChunkPos, List<Area>> areasByChunk = new HashMap<>();

    {
        wildness.name = "wildness";
        wildness.minX = Integer.MIN_VALUE;
        wildness.minY = Integer.MIN_VALUE;
        wildness.minZ = Integer.MIN_VALUE;
        wildness.maxX = Integer.MAX_VALUE;
        wildness.maxY = Integer.MAX_VALUE;
        wildness.maxZ = Integer.MAX_VALUE;
        wildness.properties.put("area.allow_click_block", true);
        wildness.properties.put("area.allow_activate_block", true);
        wildness.properties.put("area.allow_use_item", true);
    }

    private void buildCacheFor(Area area) {
        ChunkPos.getAllInBox(new ChunkPos(area.minX >> 4, area.minZ >> 4), new ChunkPos(area.maxX >> 4, area.maxZ >> 4))
                .map(cp -> this.areasByChunk.computeIfAbsent(cp, _cp -> new ArrayList<>()))
                .forEach(list -> list.add(area));
    }

    void loadFrom(Path dataDirRoot) throws Exception {
        Path userDefinedAreas = dataDirRoot.resolve("claims.json");
        if (Files.isRegularFile(userDefinedAreas)) {
            try (Reader reader = Files.newBufferedReader(userDefinedAreas)) {
                for (Area a : GSON.fromJson(reader, Area[].class)) {
                    areasByName.put(a.name, a);
                }
            }
        }
        Path wildnessArea = dataDirRoot.resolve("wildness.json");
        if (Files.isRegularFile(wildnessArea)) {
            try (Reader reader = Files.newBufferedReader(userDefinedAreas)) {
                Area a = GSON.fromJson(reader, Area.class);
                this.wildness.properties.putAll(a.properties);
            }
        }
    }

    void saveTo(Path dataDirRoot) throws Exception {
        Path userDefinedAreas = dataDirRoot.resolve("claims.json");
        Files.write(userDefinedAreas, GSON.toJson(this.areasByName.values()).getBytes(StandardCharsets.UTF_8));
        Path wildnessArea = dataDirRoot.resolve("wildness.json");
        Files.write(wildnessArea, GSON.toJson(this.wildness).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param area The Area instance to be recorded
     * @return true if and only if the area is successfully recorded by this AreaManager; false otherwise.
     */
    public boolean add(Area area) {
        // First we filter out cases where at least one defining coordinate falls in an existing area
        if (findBy(new BlockPos(area.minX, area.minY, area.minZ)) == this.wildness && findBy(new BlockPos(area.maxX, area.maxY, area.maxZ)) == this.wildness) {
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
                this.buildCacheFor(area);
                // Copy default settings over
                area.properties.putAll(wildness.properties);
                return true;
            }
        }
        return false;
    }

    public Area findBy(BlockPos pos) {
        for (Area area : this.areasByChunk.getOrDefault(new ChunkPos(pos), Collections.emptyList())) {
            if (area.minX <= pos.getX() && pos.getX() <= area.maxX) {
                if (area.minY <= pos.getY() && pos.getY() <= area.maxY) {
                    if (area.minZ <= pos.getZ() && pos.getZ() <= area.maxZ) {
                        return area;
                    }
                }
            }
        }
        return wildness;
    }

    public Area findBy(String name) {
        return areasByName.get(name);
    }

    public Collection<Area> getKnownAreas() {
		return Collections.unmodifiableCollection(this.areasByName.values());
	}
}