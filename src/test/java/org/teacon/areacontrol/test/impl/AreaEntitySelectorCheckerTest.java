package org.teacon.areacontrol.test.impl;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.teacon.areacontrol.AreaControlConfig;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaControlAPI;
import org.teacon.areacontrol.api.AreaProperties;
import org.teacon.areacontrol.impl.AreaEntitySelectorChecker;
import org.teacon.areacontrol.impl.AreaLookupImpl;
import org.teacon.areacontrol.test.InMemoryAreaRepository;

import java.util.ArrayList;

@ExtendWith(MockitoExtension.class)
public class AreaEntitySelectorCheckerTest {

    @Mock
    private MinecraftServer mockServer;
    @Mock
    private ServerLevel mockLevel;
    @Mock
    private ServerPlayer mockPlayer;
    @Mock
    private BaseCommandBlock mockCmdBlock;
    @Mock
    private Entity mockEntity;

    @BeforeAll
    public static void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        var spec = AreaControlConfig.setup(new ForgeConfigSpec.Builder());
        spec.acceptConfig(CommentedConfig.inMemory());

        initAreaManager();
    }

    public static void initAreaManager() {
        Area a, b, c, d, e, f, g, h, i, j;
        var allAreas = new ArrayList<Area>();
        allAreas.add(a = createArea("A", "minecraft:overworld", -10, -10, -10, 10, 10, 10));
        allAreas.add(b = createArea("B", "minecraft:overworld", -9, -9, -9, -8, -8, -8));
        allAreas.add(c = createArea("C", "minecraft:overworld", -5, -5, -5, -1, -1, -1));
        allAreas.add(d = createArea("D", "minecraft:overworld", -3, -3, -3, -2, -2, -2));
        allAreas.add(e = createArea("E", "minecraft:overworld", 1, 1, 1, 9, 9, 9));
        allAreas.add(f = createArea("F", "minecraft:overworld", 2, 2, 2, 8, 8, 8));
        allAreas.add(g = createArea("G", "minecraft:overworld", 3, 3, 3, 4, 4, 4));
        allAreas.add(h = createArea("H", "minecraft:overworld", 5, 5, 5, 6, 6, 6));
        allAreas.add(i = createArea("I", "minecraft:overworld", 41, 41, 41, 43, 43, 43));
        allAreas.add(j = createArea("J", "minecraft:overworld", 42, 42, 42, 43, 43, 43));

        j.belongingArea = i.uid;
        g.belongingArea = f.uid;
        h.belongingArea = f.uid;
        f.belongingArea = e.uid;
        e.belongingArea = a.uid;
        d.belongingArea = c.uid;
        c.belongingArea = a.uid;
        b.belongingArea = a.uid;

        f.properties.put(AreaProperties.ALLOW_ENTITY_USE_SELECTOR_FROM_CHILD, true);
        f.properties.put(AreaProperties.ALLOW_CB_USE_SELECTOR_FROM_CHILD, true);
        e.properties.put(AreaProperties.ALLOW_ENTITY_USE_SELECTOR_FROM_CHILD, true);
        e.properties.put(AreaProperties.ALLOW_CB_USE_SELECTOR_FROM_CHILD, true);
        a.properties.put(AreaProperties.ALLOW_ENTITY_USE_SELECTOR_FROM_CHILD, true);
        a.properties.put(AreaProperties.ALLOW_CB_USE_SELECTOR_FROM_CHILD, true);
        c.properties.put(AreaProperties.ALLOW_ENTITY_USE_SELECTOR_FROM_PARENT, true);
        c.properties.put(AreaProperties.ALLOW_CB_USE_SELECTOR_FROM_PARENT, true);
        d.properties.put(AreaProperties.ALLOW_ENTITY_USE_SELECTOR_FROM_PARENT, true);
        d.properties.put(AreaProperties.ALLOW_CB_USE_SELECTOR_FROM_PARENT, true);

        var areaRepo = new InMemoryAreaRepository(allAreas);
        try {
            AreaManager.INSTANCE.init(areaRepo);
            AreaManager.INSTANCE.load();
            AreaControlAPI.areaLookup = AreaLookupImpl.INSTANCE;
        } catch (Exception ex) {
            Assertions.fail("Failed to initialize AreaManager, which should not happen!");
        }
    }

    @Test
    public void testSelectingWithinSameAreaByPlayer() {
        Mockito.when(this.mockLevel.dimension()).thenReturn(Level.OVERWORLD);
        var commandSrc = new CommandSourceStack(this.mockEntity, Vec3.ZERO, Vec2.ZERO, this.mockLevel, 4, "Mockito", Component.literal("Mockito"), this.mockServer, null);
        // Position mock entity to [0.5, 0.5, 0.5] of overworld (in area A)
        Mockito.when(this.mockEntity.level()).thenReturn(this.mockLevel);
        this.mockEntity.xo = 0.0;
        this.mockEntity.yo = 5.0;
        this.mockEntity.zo = 0.0;
        // Verify that the command source can use entity selector to select the mock entity
        Assertions.assertTrue(AreaEntitySelectorChecker.check(commandSrc, this.mockEntity));
    }

    @Test
    public void testSelectingWithinSameAreaByCommandBlock() {
        Mockito.when(this.mockLevel.dimension()).thenReturn(Level.OVERWORLD);
        var commandSrc = new CommandSourceStack(this.mockCmdBlock, Vec3.ZERO, Vec2.ZERO, this.mockLevel, 4, "Mockito", Component.literal("Mockito"), this.mockServer, null);
        // Position mock entity to [0.0, 5.0, 5.0] of overworld (in area A)
        Mockito.when(this.mockEntity.level()).thenReturn(this.mockLevel);
        this.mockEntity.xo = 5.0;
        this.mockEntity.yo = 0.0;
        this.mockEntity.zo = 5.0;
        // Verify that the command source can use entity selector to select the mock entity
        Assertions.assertTrue(AreaEntitySelectorChecker.check(commandSrc, this.mockEntity));
    }

    @Test
    public void testSelectingByNeitherPlayerNorCommandBlock() {
        // Create a CommandSourceStack with true source being a server.
        // In real world, this means the server console command-line interface.
        var commandSrc = new CommandSourceStack(this.mockServer, Vec3.ZERO, Vec2.ZERO, this.mockLevel, 4, "Mockito", Component.literal("Mockito"), this.mockServer, null);
        // Position mock entity to [0.0, 5.0, 5.0] of overworld (in area A)
        //Mockito.when(this.mockEntity.level()).thenReturn(this.mockLevel);
        this.mockEntity.xo = 5.0;
        this.mockEntity.yo = 0.0;
        this.mockEntity.zo = 5.0;
        // Verify that the command source can use entity selector to select the mock entity
        Assertions.assertTrue(AreaEntitySelectorChecker.check(commandSrc, this.mockEntity));
    }

    @Test
    public void testSelectingByPlayerFromMultiLayerNestedAreas() {
        // Create a CommandSourceStack with true source being a mock Player, location is [3.5, 3.5, 3.5] (in area G).
        var commandSrc = new CommandSourceStack(this.mockPlayer, new Vec3(3.5, 3.5, 3.5), Vec2.ZERO, this.mockLevel, 4, "Mockito", Component.literal("Mockito"), mockServer, null);
        // Position mock entity to [-2.5, -2.5, -2.5] of overworld (in area D)
        Mockito.when(this.mockLevel.dimension()).thenReturn(Level.OVERWORLD);
        this.mockEntity.xo = -2.5;
        this.mockEntity.yo = -2.5;
        this.mockEntity.zo = -2.5;
        Mockito.when(this.mockEntity.level()).thenReturn(this.mockLevel);
        // Verify that the command source can use entity selector to select the mock entity
        Assertions.assertTrue(AreaEntitySelectorChecker.check(commandSrc, this.mockEntity));
    }

    @Test
    public void testSelectingByCommandBlockFromMultiLayerNestedAreas() {
        // Create a CommandSourceStack with true source being a mock command block, location is [3, 3, 3] (in area G).
        var commandSrc = new CommandSourceStack(this.mockCmdBlock, new Vec3(3, 3, 3), Vec2.ZERO, this.mockLevel, 4, "Mockito", Component.literal("Mockito"), mockServer, null);
        // Position mock entity to [-2.5, -2.5, -2.5] of overworld (in area D)
        Mockito.when(this.mockLevel.dimension()).thenReturn(Level.OVERWORLD);
        this.mockEntity.xo = -2.5;
        this.mockEntity.yo = -2.5;
        this.mockEntity.zo = -2.5;
        Mockito.when(this.mockEntity.level()).thenReturn(this.mockLevel);
        // Verify that the command source can use entity selector to select the mock entity
        Assertions.assertTrue(AreaEntitySelectorChecker.check(commandSrc, this.mockEntity));
    }

    public static Area createArea(String name, String dim, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        var area = new Area();
        area.name = name;
        area.dimension = dim;
        area.minX = minX;
        area.minY = minY;
        area.minZ = minZ;
        area.maxX = maxX;
        area.maxY = maxY;
        area.maxZ = maxZ;
        return area;
    }
}
