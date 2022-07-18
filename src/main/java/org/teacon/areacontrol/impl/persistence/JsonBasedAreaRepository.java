package org.teacon.areacontrol.impl.persistence;

import com.google.common.base.Preconditions;
import com.google.common.io.MoreFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.teacon.areacontrol.api.Area;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeMap;
import java.util.UUID;

public class JsonBasedAreaRepository implements AreaRepository {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataDirRoot;

    public JsonBasedAreaRepository(Path dataDirRoot) {
        this.dataDirRoot = dataDirRoot;
    }

    @Override
    public Collection<Area> load() throws Exception {
        var areas = new TreeMap<UUID, Area>();
        @Nullable var exception = (IOException) null;
        // noinspection UnstableApiUsage
        for (Path file : MoreFiles.listFiles(this.dataDirRoot)) {
            var fileName = file.getFileName().toString();
            if (fileName.startsWith("claim-") && fileName.endsWith(".json")) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    var model = GSON.fromJson(reader, Area.class);
                    Preconditions.checkArgument("claim-%s.json".formatted(model.uid).equals(fileName),
                            "The name of the claim file (" + fileName + ") does not match the claim uid: " + model.uid);
                    areas.put(model.uid, model);
                } catch (Exception e) {
                    if (exception == null) {
                        exception = new IOException("Failed to load one or more claim file(s)");
                    }
                    exception.addSuppressed(e);
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
        var ret = new ArrayList<>(areas.values());
        // Check for presence of legacy data for auto-migration
        Path legacyAreas = dataDirRoot.resolve("claims.json");
        if (Files.isRegularFile(legacyAreas)) {
            try (Reader reader = Files.newBufferedReader(legacyAreas)) {
                ret.addAll(Arrays.asList(GSON.fromJson(reader, Area[].class)));
            }
        }
        Path oldWildness = dataDirRoot.resolve("wildness.json");
        if (Files.isRegularFile(oldWildness)) {
            boolean success = false;
            try (Reader reader = Files.newBufferedReader(oldWildness)) {
                Area wildness = GSON.fromJson(reader, Area.class);
                wildness.uid = UUID.randomUUID();
                wildness.owner = Area.GLOBAL_AREA_OWNER;
                success = ret.add(wildness);
            }
            if (success) {
                Files.deleteIfExists(oldWildness);
            }
        }
        return ret;
    }

    @Override
    public void remove(Area areaToRemove) throws Exception {
        Files.deleteIfExists(this.dataDirRoot.resolve("claim-%s.json".formatted(areaToRemove.uid)));
    }

    @Override
    public void save(Collection<Area> areas) throws Exception {
        @Nullable var exception = (IOException) null;
        for (Area area : areas) {
            try {
                Files.writeString(this.dataDirRoot.resolve("claim-%s.json".formatted(area.uid)), GSON.toJson(area));
            } catch (Exception e) {
                if (exception == null) {
                    exception = new IOException("Failed to save one or more claim file(s)");
                }
                exception.addSuppressed(e);
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

}
