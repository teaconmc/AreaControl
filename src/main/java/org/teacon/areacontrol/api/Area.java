package org.teacon.areacontrol.api;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.teacon.areacontrol.Util;

/**
 * A cuboid area defined by dimension id, min. coordinate and max. coordinate.
 */
public final class Area {

    /**
     * A special owner UUID that denotes an area as "global".
     * Areas with this owner is also considered as "wildness".
     */
    public static final UUID GLOBAL_AREA_OWNER = new UUID(0L, 0L);
	
	public UUID uid = UUID.randomUUID();

    public String name = "Area " + uid.toString().substring(0, 8);
    public Set<String> tags = new HashSet<>();

    public String dimension = "minecraft:overworld";
    
    public UUID owner = GLOBAL_AREA_OWNER;
    public Set<UUID> friends = new HashSet<>();

    public int minX, minY, minZ, maxX, maxY, maxZ;
    public UUID belongingArea = null;
    public Set<UUID> subAreas = new HashSet<>();

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