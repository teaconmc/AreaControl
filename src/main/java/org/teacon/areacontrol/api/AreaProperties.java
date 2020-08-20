package org.teacon.areacontrol.api;

public final class AreaProperties {
    private AreaProperties() {}

    public static boolean getBool(Area area, String key) {
        Object o = area.properties.get(key);
        if (o == null) {
            return false;
        } else if (o instanceof Boolean) {
            return (Boolean) o;
        } else if (o instanceof Number) {
            return ((Number) o).intValue() != 0;
        } else {
            return "true".equals(o) || "t".equals(o) || Character.valueOf('t').equals(o);
        }
    }

    public static int getInt(Area area, String key) {
        Object o = area.properties.get(key);
        if (o == null) {
            return 0;
        } else if (o instanceof Number) {
            return ((Number) o).intValue();
        } else {
            return o instanceof Boolean ? (Boolean) o ? 1 : 0 : 0;
        }
    }

    public static double getDouble(Area area, String key) {
        Object o = area.properties.get(key);
        if (o == null) {
            return 0.0;
        } else if (o instanceof Number) {
            return ((Number) o).doubleValue();
        } else {
            return o instanceof Boolean ? (Boolean) o ? 1.0 : 0.0 : 0.0;
        }
    }
    
    public static String getString(Area area, String key) {
        return area.properties.getOrDefault(key, "").toString();
    }
}