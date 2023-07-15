package org.teacon.areacontrol.api;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cuboid area defined by dimension id, min. coordinate and max. coordinate.
 */
public final class Area {
	
	public UUID uid = UUID.randomUUID();

    public String name = "Area " + uid.toString().substring(0, 8);

    public String dimension = "minecraft:overworld";
    
    public Set<UUID> owners = new HashSet<>();
    public Set<UUID> builders = new HashSet<>();

    public int minX, minY, minZ, maxX, maxY, maxZ;
    public UUID belongingArea = null;
    public transient Set<UUID> subAreas = new HashSet<>();

    public final Map<String, Object> properties = new ConcurrentHashMap<>();

    public static final class Summary {
        public final UUID uid;
        public final int minX, minY, minZ, maxX, maxY, maxZ;
        public final int midX, midY, midZ;
        public final boolean enclosed;
        public Summary(Area area) {
            this(area.uid, area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ, area.belongingArea != null);
        }
        public Summary(UUID uid, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean enclosed) {
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
            this.enclosed = enclosed;
        }
    }
}