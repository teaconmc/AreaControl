package org.teacon.areacontrol.impl.persistence;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.InMemoryFormat;
import com.electronwill.nightconfig.core.conversion.Conversion;
import com.electronwill.nightconfig.core.conversion.Converter;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.conversion.SpecNotNull;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.MoreFiles;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.core.BlockPos;
import org.teacon.areacontrol.api.Area;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class TomlBasedAreaRepository implements AreaRepository {

    private final Path dataDirRoot;

    public TomlBasedAreaRepository(Path dataDirRoot) {
        this.dataDirRoot = dataDirRoot;
    }

    @Override
    public Collection<Area> load() throws Exception {
        var areas = new TreeMap<UUID, Area>();
        @Nullable var exception = (IOException) null;
        // noinspection UnstableApiUsage
        for (Path file : MoreFiles.listFiles(this.dataDirRoot)) {
            var fileName = file.getFileName();
            if (fileName.startsWith("claim-") && fileName.endsWith(".toml")) {
                try (FileConfig areasData = FileConfig.of(file)) {
                    areasData.load();
                    var converter = new ObjectConverter();
                    var model = converter.toObject(areasData, AreaModel::new);
                    Preconditions.checkArgument(Path.of("claim-%s.toml".formatted(model.uid)).equals(fileName),
                            "The name of the claim file (" + fileName + ") does not match the claim uid: " + model.uid);
                    areas.put(model.uid, model.toRealArea());
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
        return Collections.unmodifiableCollection(areas.values());
    }

    @Override
    public void save(Collection<Area> areas) throws Exception {
        @Nullable var exception = (IOException) null;
        for (Area area : areas) {
            var model = new AreaModel(area);
            try (FileConfig areasData = FileConfig.of(this.dataDirRoot.resolve("claims-%s.toml".formatted(area.uid)))) {
                var converter = new ObjectConverter();
                converter.toConfig(model, areasData);
                areasData.save();
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

    public static final class AreaModel {
        @Conversion(UUIDConverter.class)
        @SpecNotNull
        public UUID uid;
        @SpecNotNull
        public String name;
        @SpecNotNull
        public String dimension = "minecraft:overworld";
        @Conversion(UUIDConverter.class)
        @SpecNotNull
        public UUID owner = new UUID(0L, 0L);
        @Conversion(UUIDCollectionConverter.class)
        public Collection<UUID> friends = new HashSet<>();

        public int minX, minY, minZ, maxX, maxY, maxZ;

        public Config properties;

        public AreaModel() {
        }

        public AreaModel(Area realArea) {
            this.uid = realArea.uid;
            this.name = realArea.name;
            this.dimension = realArea.dimension;
            this.owner = realArea.owner;
            this.friends = realArea.friends;
            this.minX = realArea.minX;
            this.minY = realArea.minY;
            this.minZ = realArea.minZ;
            this.maxX = realArea.maxX;
            this.maxY = realArea.maxY;
            this.maxZ = realArea.maxZ;
            this.properties = Config.wrap(realArea.properties, InMemoryFormat.defaultInstance());
        }

        public Area toRealArea() {
            var area = new Area();
            area.uid = this.uid;
            area.name = this.name;
            area.dimension = this.dimension;
            area.owner = this.owner;
            area.friends = new ObjectArraySet<>(this.friends);
            area.minX = this.minX;
            area.minY = this.minY;
            area.minZ = this.minZ;
            area.maxX = this.maxX;
            area.maxY = this.maxY;
            area.maxZ = this.maxZ;
            area.properties.clear();
            area.properties.putAll(this.properties.valueMap());
            return area;
        }
    }

    private static final class UUIDConverter implements Converter<UUID, String> {

        @Override
        public UUID convertToField(String value) {
            return UUID.fromString(value);
        }

        @Override
        public String convertFromField(UUID value) {
            return value.toString();
        }
    }

    private static final class UUIDCollectionConverter implements Converter<Collection<UUID>, List<String>> {

        @Override
        public Collection<UUID> convertToField(List<String> value) {
            return value.stream().map(UUID::fromString).toList();
        }

        @Override
        public List<String> convertFromField(Collection<UUID> value) {
            return value.stream().map(UUID::toString).toList();
        }
    }
}
