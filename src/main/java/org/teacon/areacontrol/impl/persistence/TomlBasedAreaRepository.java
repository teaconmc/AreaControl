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
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.core.BlockPos;
import org.teacon.areacontrol.api.Area;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
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
            var fileName = file.getFileName().toString();
            if (fileName.startsWith("claim-") && fileName.endsWith(".toml")) {
                try (FileConfig areasData = FileConfig.of(file)) {
                    areasData.load();
                    var converter = new ObjectConverter();
                    var model = converter.toObject(areasData, AreaModel::new);
                    Preconditions.checkArgument("claim-%s.toml".formatted(model.uid).equals(fileName),
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
    public void remove(Area areaToRemove) throws Exception {
        Files.deleteIfExists(this.dataDirRoot.resolve("claim-%s.toml".formatted(areaToRemove.uid)));
    }

    @Override
    public void save(Collection<Area> areas) throws Exception {
        @Nullable var exception = (IOException) null;
        for (Area area : areas) {
            var model = new AreaModel(area);
            try (FileConfig areasData = FileConfig.builder(this.dataDirRoot.resolve("claim-%s.toml".formatted(area.uid))).sync().build()) {
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
        @Conversion(UUIDCollectionConverter.class)
        @SpecNotNull
        public Collection<UUID> owners = new HashSet<>();
        @SpecNotNull
        public List<String> ownerGroups = new ArrayList<>();
        @Conversion(UUIDCollectionConverter.class)
        public Collection<UUID> builders = new HashSet<>();
        @SpecNotNull
        public List<String> builderGroups = new ArrayList<>();
        @Conversion(BlockPosConverter.class)
        @SpecNotNull
        public BlockPos min = new BlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        @Conversion(BlockPosConverter.class)
        @SpecNotNull
        public BlockPos max = new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        @Conversion(NullableUUIDConverter.class)
        public UUID belongingArea;

        public Config properties;

        public AreaModel() {
        }

        public AreaModel(Area realArea) {
            this.uid = realArea.uid;
            this.name = realArea.name;
            this.dimension = realArea.dimension;
            this.owners = realArea.owners;
            this.ownerGroups = new ArrayList<>(realArea.ownerGroups);
            this.builders = realArea.builders;
            this.builderGroups = new ArrayList<>(realArea.builderGroups);
            this.min = new BlockPos(realArea.minX, realArea.minY, realArea.minZ);
            this.max = new BlockPos(realArea.maxX, realArea.maxY, realArea.maxZ);
            this.belongingArea = realArea.belongingArea;
            this.properties = Config.wrap(realArea.properties, InMemoryFormat.defaultInstance());
        }

        public Area toRealArea() {
            var area = new Area();
            area.uid = this.uid;
            area.name = this.name;
            area.dimension = this.dimension;
            area.owners = new ObjectArraySet<>(this.owners);
            area.ownerGroups = new ObjectArraySet<>(this.ownerGroups);
            area.builders = new ObjectArraySet<>(this.builders);
            area.builderGroups = new ObjectArraySet<>(this.builderGroups);
            area.minX = Math.min(this.min.getX(), this.max.getX());
            area.minY = Math.min(this.min.getY(), this.max.getY());
            area.minZ = Math.min(this.min.getZ(), this.max.getZ());
            area.maxX = Math.max(this.min.getX(), this.max.getX());
            area.maxY = Math.max(this.min.getY(), this.max.getY());
            area.maxZ = Math.max(this.min.getZ(), this.max.getZ());
            area.belongingArea = this.belongingArea;
            area.properties.clear();
            area.properties.putAll(this.properties.valueMap());
            return area;
        }
    }

    private static final class BlockPosConverter implements Converter<BlockPos, List<Integer>> {
        @Override
        public BlockPos convertToField(List<Integer> value) {
            Preconditions.checkArgument(value.size() == 3);
            return new BlockPos(value.get(0), value.get(1), value.get(2));
        }

        @Override
        public List<Integer> convertFromField(BlockPos value) {
            return Lists.newArrayList(value.getX(), value.getY(), value.getZ());
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

    private static final class NullableUUIDConverter implements Converter<UUID, String> {

        @Override
        public UUID convertToField(String value) {
            return value == null || "null".equals(value) ? null : UUID.fromString(value);
        }

        @Override
        public String convertFromField(UUID value) {
            return Objects.toString(value);
        }
    }

    private static final class UUIDCollectionConverter implements Converter<Collection<UUID>, List<String>> {

        @Override
        public Collection<UUID> convertToField(List<String> value) {
            return value == null ? new ArrayList<>() : value.stream().map(UUID::fromString).toList();
        }

        @Override
        public List<String> convertFromField(Collection<UUID> value) {
            return value.stream().map(UUID::toString).toList();
        }
    }
}
