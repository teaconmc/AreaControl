package org.teacon.areacontrol.impl;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

public final class AreaFactory {

    public static Area defaultWildness(ResourceKey<Level> levelId) {
        var wildness = new Area();
        wildness.owner = Area.GLOBAL_AREA_OWNER;
        wildness.name = "Wildness of " + levelId.location();
        wildness.dimension = levelId.location().toString();
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
        wildness.properties.put(AreaProperties.ALLOW_SPAWN, Boolean.TRUE);
        return wildness;
    }

    public static Area singlePlayerWildness() {
        // TODO Too brutal, need a clever way
        var singlePlayerWildness = new Area();
        singlePlayerWildness.uid = Area.GLOBAL_AREA_OWNER;
        singlePlayerWildness.name = "Single-player Wildness";
        singlePlayerWildness.dimension = "minecraft:overworld";
        singlePlayerWildness.minX = Integer.MIN_VALUE;
        singlePlayerWildness.minY = Integer.MIN_VALUE;
        singlePlayerWildness.minZ = Integer.MIN_VALUE;
        singlePlayerWildness.maxX = Integer.MAX_VALUE;
        singlePlayerWildness.maxY = Integer.MAX_VALUE;
        singlePlayerWildness.maxZ = Integer.MAX_VALUE;
        singlePlayerWildness.properties.put("area.allow_spawn", Boolean.TRUE);
        singlePlayerWildness.properties.put("area.allow_special_spawn", Boolean.TRUE);
        singlePlayerWildness.properties.put("area.allow_pvp", Boolean.TRUE);
        singlePlayerWildness.properties.put("area.allow_attack", Boolean.TRUE);
        singlePlayerWildness.properties.put("area.allow_click_block", Boolean.TRUE);
        singlePlayerWildness.properties.put("area.allow_break_block", Boolean.TRUE);
        singlePlayerWildness.properties.put("area.allow_activate_block", Boolean.TRUE);
        singlePlayerWildness.properties.put("area.allow_use_item", Boolean.TRUE);
        singlePlayerWildness.properties.put("area.allow_trample_farmland", Boolean.TRUE);
        singlePlayerWildness.properties.put("area.allow_place_block", Boolean.TRUE);
        singlePlayerWildness.properties.put("area.allow_interact_entity", Boolean.TRUE);
        singlePlayerWildness.properties.put("area.allow_interact_entity_specific", Boolean.TRUE);
        singlePlayerWildness.properties.put("area.allow_explosion", Boolean.TRUE);
        singlePlayerWildness.properties.put("area.allow_explosion_affect_blocks", Boolean.TRUE);
        singlePlayerWildness.properties.put("area.allow_explosion_affect_entities", Boolean.TRUE);
        return singlePlayerWildness;
    }
}
