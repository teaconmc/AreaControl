package org.teacon.areacontrol.api;

import java.util.HashMap;
import java.util.Map;

/**
 * A cuboid area defined by min. coordinate and max. coordinate.
 */
public final class Area {

    public String name;

    public int minX, minY, minZ, maxX, maxY, maxZ;

    public final Map<String, Object> properties = new HashMap<>();
}