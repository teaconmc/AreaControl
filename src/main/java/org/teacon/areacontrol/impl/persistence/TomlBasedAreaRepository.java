package org.teacon.areacontrol.impl.persistence;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.serde.DeserializerContext;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import com.electronwill.nightconfig.core.serde.SerializerContext;
import com.electronwill.nightconfig.core.serde.TypeConstraint;
import com.electronwill.nightconfig.core.serde.ValueDeserializer;
import com.electronwill.nightconfig.core.serde.ValueDeserializerProvider;
import com.electronwill.nightconfig.core.serde.ValueSerializer;
import com.electronwill.nightconfig.core.serde.ValueSerializerProvider;
import com.electronwill.nightconfig.core.serde.annotations.SerdeAssert;
import com.electronwill.nightconfig.core.serde.annotations.SerdeSkipDeserializingIf;
import com.electronwill.nightconfig.core.serde.annotations.SerdeSkipSerializingIf;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.base.Preconditions;
import com.google.common.io.MoreFiles;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.areacontrol.api.Area;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TomlBasedAreaRepository implements AreaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(TomlBasedAreaRepository.class);

    private final Path dataDirRoot;
    private final Path globalConfigDirRoot;

    private final ObjectSerializer writer;
    private final ObjectDeserializer reader;

    public TomlBasedAreaRepository(Path dataDirRoot, Path globalConfigDir) {
        this.dataDirRoot = dataDirRoot;
        this.globalConfigDirRoot = globalConfigDir;

        var desBuilder = ObjectDeserializer.builder();
        desBuilder.withDefaultDeserializerProvider(BlockPosSerDeProvider.INSTANCE);
        this.reader = desBuilder.build();

        var seBuilder = ObjectSerializer.builder();
        seBuilder.withSerializerProvider(BlockPosSerDeProvider.INSTANCE);
        this.writer = seBuilder.build();
    }

    @Override
    public Collection<Area> load() throws Exception {
        var areas = new TreeMap<UUID, Area>();
        @Nullable var exception = (IOException) null;
        for (Path file : MoreFiles.listFiles(this.dataDirRoot)) {
            var fileName = file.getFileName().toString();
            if (fileName.startsWith("claim-") && fileName.endsWith(".toml")) {
                try (FileConfig areasData = FileConfig.of(file, TomlFormat.instance())) {
                    areasData.load();
                    var model = reader.deserializeFields(areasData, AreaModel::new);
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
    public Area loadWildness() throws Exception {
        LOGGER.info("Loading wildness permission data...");
        Path wildDef = this.globalConfigDirRoot.resolve("area_control-wildness.toml");
        if (Files.notExists(wildDef)) {
            LOGGER.info("No wildness found, will use default one");
            return null;
        }
        try (FileConfig areasData = FileConfig.of(wildDef, TomlFormat.instance())) {
            areasData.load();
            var model = reader.deserializeFields(areasData, AreaModel::new);
            LOGGER.info("Wildness permission data loaded");
            return model.toRealArea();
        } catch (Exception e) {
            LOGGER.error("Error occurred while loading permission data for the wildness", e);
            throw e;
        }
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
                var saved = this.writer.serializeFields(model, TomlFormat::newConfig);
                areasData.addAll(saved);
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

    @Override
    public void saveWildness(Area wild) throws Exception {
        Path wildDef = this.globalConfigDirRoot.resolve("area_control-wildness.toml");
        try (FileConfig areasData = FileConfig.builder(wildDef).sync().build()) {
            var saved = this.writer.serializeFields(new AreaModel(wild), TomlFormat::newConfig);
            areasData.addAll(saved);
            areasData.save();
        }
    }

    public static final class AreaModel {
        @SerdeAssert(SerdeAssert.AssertThat.NOT_EMPTY)
        public UUID uid;
        @SerdeAssert(SerdeAssert.AssertThat.NOT_EMPTY)
        public String name;
        @SerdeAssert(SerdeAssert.AssertThat.NOT_NULL) // TODO[3TUSK]: Custom check
        public String dimension = "minecraft:overworld";
        @SerdeAssert(SerdeAssert.AssertThat.NOT_NULL)
        public Collection<UUID> owners = new HashSet<>();
        @SerdeAssert(SerdeAssert.AssertThat.NOT_NULL)
        public List<String> ownerGroups = new ArrayList<>();
        @SerdeAssert(SerdeAssert.AssertThat.NOT_NULL)
        public Collection<UUID> builders = new HashSet<>();
        @SerdeAssert(SerdeAssert.AssertThat.NOT_NULL)
        public List<String> builderGroups = new ArrayList<>();
        @SerdeAssert(SerdeAssert.AssertThat.NOT_EMPTY)
        public BlockPos min = new BlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        @SerdeAssert(SerdeAssert.AssertThat.NOT_EMPTY)
        public BlockPos max = new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

        @SerdeSkipDeserializingIf({
                SerdeSkipDeserializingIf.SkipDeIf.IS_MISSING,
                SerdeSkipDeserializingIf.SkipDeIf.IS_EMPTY,
                SerdeSkipDeserializingIf.SkipDeIf.IS_NULL
        })
        @SerdeSkipSerializingIf({
                SerdeSkipSerializingIf.SkipSerIf.IS_EMPTY,
                SerdeSkipSerializingIf.SkipSerIf.IS_NULL
        })
        public UUID belongingArea;

        public Map<String, Object> properties;

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
            this.belongingArea = realArea.getBelongingArea();
            this.properties = realArea.properties;
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
            area.setBelongingArea(this.belongingArea);
            area.properties.clear();
            area.properties.putAll(this.properties);
            return area;
        }
    }

    private enum BlockPosSerDe
            implements ValueDeserializer<List<Integer>, BlockPos>, ValueSerializer<BlockPos, List<Integer>> {

        INSTANCE;

        @Override
        public BlockPos deserialize(List<Integer> value, Optional<TypeConstraint> resultType, DeserializerContext ctx) {
            Preconditions.checkArgument(value.size() == 3);
            return new BlockPos(value.get(0), value.get(1), value.get(2));
        }

        @Override
        public List<Integer> serialize(BlockPos value, SerializerContext ctx) {
            return List.of(value.getX(), value.getY(), value.getZ());
        }
    }

    private enum BlockPosSerDeProvider implements ValueDeserializerProvider<List<Integer>, BlockPos>, ValueSerializerProvider<BlockPos, List<Integer>> {
        INSTANCE;

        @Override
        public ValueDeserializer<List<Integer>, BlockPos> provide(Class<?> valueClass, TypeConstraint resultType) {
            if (List.class.isAssignableFrom(valueClass)) { // TODO[3TUSK]: type check
                return BlockPosSerDe.INSTANCE;
            }
            return null;
        }

        @Override
        public ValueSerializer<BlockPos, List<Integer>> provide(Class<?> valueClass, SerializerContext ctx) {
            if (valueClass == BlockPos.class) {
                return BlockPosSerDe.INSTANCE;
            }
            return null;
        }
    }

}
