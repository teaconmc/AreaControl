package org.teacon.areacontrol.api;

import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cuboid area defined by dimension id, min. coordinate and max. coordinate.
 */
public final class Area {

    public UUID uid;

    public String name = "无主之地"; // Chinese for "Terra Nullius", or "Nobody's land"

    public String dimension = "minecraft:overworld";

    public Set<UUID> owners = new HashSet<>();
    public Set<String> ownerGroups = new HashSet<>();
    public Set<UUID> builders = new HashSet<>();
    public Set<String> builderGroups = new HashSet<>();

    public int minX, minY, minZ, maxX, maxY, maxZ;
    private UUID belongingArea = null;
    public final Map<String, Object> properties = new ConcurrentHashMap<>();

    public transient Set<UUID> subAreas = new HashSet<>();
    public transient BigInteger volume = BigInteger.ZERO;
    private transient Area parentAreaRef;
    private transient boolean parentAreaResolved = false;

    public @Nullable Area resolveParent() {
        if (!this.parentAreaResolved) {
            this.parentAreaRef = AreaControlAPI.areaLookup.findBy(this.belongingArea);
            this.parentAreaResolved = true;
        }
        return this.parentAreaRef;
    }

    public void clearCache() {
        this.parentAreaResolved = false;
    }

    public @Nullable UUID getBelongingArea() {
        return this.belongingArea;
    }

    public void setBelongingArea(UUID belongingArea) {
        this.belongingArea = belongingArea;
        this.clearCache();
    }

    public static final class Summary {
        public final UUID uid;
        public final int minX, minY, minZ, maxX, maxY, maxZ;
        public final int midX, midY, midZ;
        public final boolean enclosed;

        public Summary(Area area) {
            this(area.uid, area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ, area.getBelongingArea() != null);
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