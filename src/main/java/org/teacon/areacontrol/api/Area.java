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

    public static final class Summary {
        public final UUID uid;
        public final int minX, minY, minZ, maxX, maxY, maxZ;
        public final int midX, midY, midZ;
        public Summary(Area area) {
            this(area.uid, area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ);
        }
        public Summary(UUID uid, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.uid = uid;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.midX = (maxX + minX) / 2;
            this.midY = (maxY + minY) / 2;
            this.midZ = (maxZ + minZ) / 2;
        }
    }
}