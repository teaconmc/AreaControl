package org.teacon.areacontrol.test.impl.persistence;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.teacon.areacontrol.Util;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.impl.persistence.JsonBasedAreaRepository;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class JsonBasedAreaRepositoryTest {

    private final FileSystem fsRoot = Jimfs.newFileSystem(Configuration.unix());

    @Mock
    private ServerPlayer mockPlayer;

    @BeforeEach
    public void setup() {
        Path claimStoreRoot = this.fsRoot.getPath("/area-control");
        try {
            Files.createDirectories(claimStoreRoot);
        } catch (IOException e) {
            Assertions.fail(e);
        }

        GameProfile dummyProfile = new GameProfile(UUID.randomUUID(), "Test Player Please Ignore");
        Mockito.when(this.mockPlayer.getGameProfile()).thenReturn(dummyProfile);
    }

    @Test
    public void testLoad() {
        Path claimStoreRoot = this.fsRoot.getPath("/area-control");
        String claimData = """
                {
                    "uid": "f6b2a791-d2cd-4992-b4d6-4b0ecd242f92",
                    "name": "Test Area",
                    "dimension": "area_control:test",
                    "owners": [],
                    "ownerGroups": [ "test_group" ],
                    "builders": [],
                    "builderGroups": [],
                    "minX": -1,
                    "minY": -1,
                    "minZ": -1,
                    "maxX": 1,
                    "maxY": 1,
                    "maxZ": 1,
                    "belongingArea": "f67749a2-f85c-4891-8cb2-b61af46c7ec2",
                    "properties": {
                        "foo": "bar"
                    }
                }
                """;
        try {
            Files.writeString(claimStoreRoot.resolve("claim-f6b2a791-d2cd-4992-b4d6-4b0ecd242f92.json"), claimData);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            Assertions.fail(e);
        }

        JsonBasedAreaRepository repo = new JsonBasedAreaRepository(claimStoreRoot);
        Collection<Area> areas = null;
        try {
            areas = repo.load();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            Assertions.fail(e);
        }

        Assertions.assertNotNull(areas);
        Assertions.assertFalse(areas.isEmpty());
        Assertions.assertEquals(1, areas.size());
        Area theArea = areas.iterator().next();

        Assertions.assertEquals(UUID.fromString("f6b2a791-d2cd-4992-b4d6-4b0ecd242f92"), theArea.uid);
        Assertions.assertEquals(UUID.fromString("f67749a2-f85c-4891-8cb2-b61af46c7ec2"), theArea.getBelongingArea());
        Assertions.assertEquals("Test Area", theArea.name);
        Assertions.assertEquals("area_control:test", theArea.dimension);
        Assertions.assertTrue(theArea.owners.isEmpty());
        Assertions.assertFalse(theArea.ownerGroups.isEmpty());
        Assertions.assertEquals(-1, theArea.minX);
        Assertions.assertEquals(-1, theArea.minY);
        Assertions.assertEquals(-1, theArea.minZ);
        Assertions.assertEquals(1, theArea.maxX);
        Assertions.assertEquals(1, theArea.maxY);
        Assertions.assertEquals(1, theArea.maxZ);

        var props = theArea.properties;
        Assertions.assertFalse(props.isEmpty());
        Assertions.assertEquals("bar", props.get("foo"));
    }

    @Test
    public void testSave() {
        Path claimStoreRoot = this.fsRoot.getPath("/area-control");
        JsonBasedAreaRepository repo = new JsonBasedAreaRepository(claimStoreRoot);

        Area area = Util.createArea(new BlockPos(-1, -1, -1), new BlockPos(1, 1,1 ), this.mockPlayer);
        area.uid = UUID.fromString("5d9e9b0d-f438-4e5a-826e-7d42ab76385a");

        try {
            repo.save(List.of(area));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            Assertions.fail(e);
        }

        String claimFileName = "claim-5d9e9b0d-f438-4e5a-826e-7d42ab76385a.json";

        Assertions.assertTrue(Files.exists(claimStoreRoot.resolve(claimFileName)));
    }
}
