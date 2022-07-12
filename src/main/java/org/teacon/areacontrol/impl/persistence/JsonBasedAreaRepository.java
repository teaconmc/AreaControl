package org.teacon.areacontrol.impl.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.teacon.areacontrol.api.Area;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

public class JsonBasedAreaRepository implements AreaRepository {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataDirRoot;

    public JsonBasedAreaRepository(Path dataDirRoot) {
        this.dataDirRoot = dataDirRoot;
    }

    @Override
    public Collection<Area> load() throws Exception {
        var areas = new ArrayList<Area>();
        Path userDefinedAreas = dataDirRoot.resolve("claims.json");
        if (Files.isRegularFile(userDefinedAreas)) {
            try (Reader reader = Files.newBufferedReader(userDefinedAreas)) {
                areas.addAll(Arrays.asList(GSON.fromJson(reader, Area[].class)));
            }
        }
        // Check for presence of legacy data for auto-migration
        Path oldWildness = dataDirRoot.resolve("wildness.json");
        if (Files.isRegularFile(oldWildness)) {
            boolean success = false;
            try (Reader reader = Files.newBufferedReader(oldWildness)) {
                Area wildness = GSON.fromJson(reader, Area.class);
                wildness.uid = UUID.randomUUID();
                wildness.owner = Area.GLOBAL_AREA_OWNER;
                success = areas.add(wildness);
            }
            if (success) {
                Files.deleteIfExists(oldWildness);
            }
        }
        return areas;
    }

    @Override
    public void save(Collection<Area> areas) throws Exception {
        Files.writeString(dataDirRoot.resolve("claims.json"), GSON.toJson(areas));
    }

}
