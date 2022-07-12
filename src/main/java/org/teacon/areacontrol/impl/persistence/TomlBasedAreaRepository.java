package org.teacon.areacontrol.impl.persistence;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.InMemoryFormat;
import com.electronwill.nightconfig.core.conversion.Conversion;
import com.electronwill.nightconfig.core.conversion.Converter;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.conversion.SpecNotNull;
import com.electronwill.nightconfig.core.file.FileConfig;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.teacon.areacontrol.api.Area;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class TomlBasedAreaRepository implements AreaRepository {

    private final Path dataDirRoot;

    public TomlBasedAreaRepository(Path dataDirRoot) {
        this.dataDirRoot = dataDirRoot;
    }

    @Override
    public Collection<Area> load() throws Exception {
        try (FileConfig areasData = FileConfig.of(this.dataDirRoot.resolve("claims.toml"))) {
            areasData.load();
            var converter = new ObjectConverter();
            var model = converter.toObject(areasData, AreaRepoModel::new);
            return model.toRealAreas();
        }
    }

    @Override
    public void save(Collection<Area> areas) throws Exception {
        var model = new AreaRepoModel(areas);
        try (FileConfig areasData = FileConfig.of(this.dataDirRoot.resolve("claims.toml"))) {
            var converter = new ObjectConverter();
            converter.toConfig(model, areasData);
            areasData.save();
        }
    }

    private static final class AreaRepoModel {
        public List<AreaModel> areas;

        public AreaRepoModel() {}

        public AreaRepoModel(Collection<Area> realAreas) {
            this.areas = realAreas.stream().map(AreaModel::new).toList();
        }

        public List<Area> toRealAreas() {
            return this.areas.stream().map(AreaModel::toRealArea).toList();
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

            public AreaModel() {}

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
