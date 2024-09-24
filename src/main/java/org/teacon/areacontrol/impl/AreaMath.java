package org.teacon.areacontrol.impl;

import org.jetbrains.annotations.Nullable;
import org.teacon.areacontrol.api.Area;

import java.math.BigInteger;
import java.util.EnumSet;

public class AreaMath {

    public enum SetRelation {
        INDEPENDENT, SUPERSET, INTERSECT, SAME, SUBSET;

        public SetRelation flip() {
            return switch (this) {
                case INDEPENDENT -> INDEPENDENT;
                case SUPERSET -> SUBSET;
                case INTERSECT -> INTERSECT;
                case SAME -> SAME;
                case SUBSET -> SUPERSET;
            };
        }
    }

    /**
     * Sufficiently large number used for calculating area volume.
     */
    public static final BigInteger VERY_BIG_NUMBER = BigInteger.valueOf(Long.MAX_VALUE).pow(3);

    public static boolean isPointIn(@Nullable Area area, double x, double y, double z) {
        return area == null || (area.minX <= x && area.maxX >= x && area.minY <= y && area.maxY >= y && area.minZ <= z && area.maxZ >= z);
    }

    public static double distanceSqBetween(Area area, double x, double y, double z) {
        double dx = Math.max(Math.max(area.minX - x, x - area.maxX), 0.0D);
        double dy = Math.max(Math.max(area.minY - y, y - area.maxY), 0.0D);
        double dz = Math.max(Math.max(area.minZ - z, z - area.maxZ), 0.0D);
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Calculate the shortest distance between a point inside the given area and the bounding
     * planes of the given area.
     * This method does not check if the point is inside the area or not.
     *
     * @return Shortest distance
     */
    public static double distanceFromInteriorToBoundary(Area area, double x, double y, double z) {
        double dx = Math.min(x - area.minX, area.maxX - x);
        double dy = Math.min(y - area.minY, area.maxY - y);
        double dz = Math.min(z - area.minZ, area.maxZ - z);
        return Math.min(dx, Math.min(dy, dz));
    }

    public static boolean isEnclosing(@Nullable Area parent, @Nullable Area maybeChild) {
        // Null area means wildness: wildness is the parent of any area.
        if (parent == null) {
            return true;
        }
        // Parent is non-wildness area, but maybeChild is the wildness.
        // Naturally, parent cannot be the parent of maybeChild in this case.
        if (maybeChild == null) {
            return false;
        }
        return parent.minX <= maybeChild.minX && maybeChild.maxX <= parent.maxX
                && parent.minY <= maybeChild.minY && maybeChild.maxY <= parent.maxY
                && parent.minZ <= maybeChild.minZ && maybeChild.maxZ <= parent.maxZ;
    }

    public static boolean isCoveringSameArea(Area a, Area b) {
        return a.minX == b.minX && a.minY == b.minY && a.minZ == b.minZ && a.maxX == b.maxX && a.maxY == b.maxY && a.maxZ == b.maxZ;
    }

    public static SetRelation relationBetween(int aMin, int aMax, int bMin, int bMax) {
        if (aMax < bMin || bMax < aMin) {
            return SetRelation.INDEPENDENT;
        }
        if (aMin == bMin && aMax == bMax) {
            return SetRelation.SAME;
        }
        if (aMin >= bMin && aMax <= bMax) {
            return SetRelation.SUBSET;
        }
        if (aMin <= bMin && aMax >= bMax) {
            return SetRelation.SUPERSET;
        }
        return SetRelation.INTERSECT;
    }

    public static SetRelation relationBetween(Area left, Area right) {
        return relationBetween(left.minX, left.minY, left.minZ, left.maxX, left.maxY, left.maxZ,
                right.minX, right.minY, right.minZ, right.maxX, right.maxY, right.maxZ);
    }

    public static SetRelation relationBetween(int aMinX, int aMinY, int aMinZ, int aMaxX, int aMaxY, int aMaxZ, Area right) {
        return relationBetween(aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ,
                right.minX, right.minY, right.minZ, right.maxX, right.maxY, right.maxZ);
    }

    public static SetRelation relationBetween(Area left, int bMinX, int bMinY, int bMinZ, int bMaxX, int bMaxY, int bMaxZ) {
        return relationBetween(left.minX, left.minY, left.minZ, left.maxX, left.maxY, left.maxZ,
                bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ);
    }

    /**
     * Determine the relation between area A and area B.
     *
     * @param aMinX Smaller X of area A
     * @param aMinY Smaller Y of area A
     * @param aMinZ Smaller Z of area A
     * @param aMaxX Larger X of area A
     * @param aMaxY Larger Y of area A
     * @param aMaxZ Larger Z of area A
     * @param bMinX Smaller X of area B
     * @param bMinY Smaller Y of area B
     * @param bMinZ Smaller Z of area B
     * @param bMaxX Larger X of area B
     * @param bMaxY Larger Y of area B
     * @param bMaxZ Larger Z of area B
     * @return The relation for area A being related to area B
     */
    public static SetRelation relationBetween(
            int aMinX, int aMinY, int aMinZ, int aMaxX, int aMaxY, int aMaxZ,
            int bMinX, int bMinY, int bMinZ, int bMaxX, int bMaxY, int bMaxZ
    ) {
        EnumSet<SetRelation> axisRelations = EnumSet.of(
                relationBetween(aMinX, aMaxX, bMinX, bMaxX),
                relationBetween(aMinY, aMaxY, bMinY, bMaxY),
                relationBetween(aMinZ, aMaxZ, bMinZ, bMaxZ)
        );
        if (axisRelations.size() == 1) {
            return axisRelations.iterator().next();
        } else {
            if (axisRelations.contains(SetRelation.INDEPENDENT)) {
                return SetRelation.INDEPENDENT;
            }
            return SetRelation.INTERSECT;
        }
        /*
        String builder = "Impossible arrangement found! " +
                "Box A: [" + aMinX + ", " + aMinY + ", " + aMinZ + "] -> [" + aMaxX + ", " + aMaxY + ", " + aMaxZ + "]; " +
                "Box B: [" + bMinX + ", " + bMinY + ", " + bMinZ + "] -> [" + bMaxX + ", " + bMaxY + ", " + bMaxZ + "]; " +
                "axis relations: " + axisRelations;
        throw new IllegalArgumentException(builder);*/
    }
}
