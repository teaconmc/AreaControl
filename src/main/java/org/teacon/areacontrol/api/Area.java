package org.teacon.areacontrol.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.teacon.areacontrol.Util;

/**
 * A cuboid area defined by dimension id, min. coordinate and max. coordinate.
 */
public final class Area {
	
	public UUID uid = UUID.randomUUID();

    public String name = "Area " + Util.nextRandomString();

    public String dimension = "minecraft:overworld";
    
    public UUID owner = new UUID(0L, 0L);

    public int minX, minY, minZ, maxX, maxY, maxZ;

    public final Map<String, Object> properties = new HashMap<>();
}