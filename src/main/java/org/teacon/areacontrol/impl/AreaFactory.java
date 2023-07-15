package org.teacon.areacontrol.impl;

import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

public final class AreaFactory {

    public static Area singlePlayerWildness() {
        // TODO Too brutal, need a clever way
        var singlePlayerWildness = new Area();
        singlePlayerWildness.uid = null;
        singlePlayerWildness.name = "Single-player Wildness";
        singlePlayerWildness.dimension = "minecraft:overworld";
        singlePlayerWildness.minX = Integer.MIN_VALUE;
        singlePlayerWildness.minY = Integer.MIN_VALUE;
        singlePlayerWildness.minZ = Integer.MIN_VALUE;
        singlePlayerWildness.maxX = Integer.MAX_VALUE;
        singlePlayerWildness.maxY = Integer.MAX_VALUE;
        singlePlayerWildness.maxZ = Integer.MAX_VALUE;
        singlePlayerWildness.properties.put("area.allow_spawn", Boolean.TRUE);
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
        singlePlayerWildness.properties.put(AreaProperties.ALLOW_FIRE_SPREAD, Boolean.TRUE);
        singlePlayerWildness.properties.put(AreaProperties.ALLOW_RIDE, Boolean.TRUE);
        return singlePlayerWildness;
    }
}
