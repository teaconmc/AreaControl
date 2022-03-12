package org.teacon.areacontrol.api;

import java.util.HashSet;
import java.util.Set;

public final class AreaProperties {

    /** 
     * Set of area properties that are known by the AreaControl mod.
     * Adding a property to this Set is optional; it only enables command auto-completion.
     */
    public static final Set<String> KNOWN_PROPERTIES = new HashSet<>();   
    
    public static final String ALLOW_SPAWN = register("area.allow_spawn");
    public static final String ALLOW_SPECIAL_SPAWN = register("area.allow_special_spawn");
    public static final String ALLOW_PVE = register("area.allow_attack");
    public static final String ALLOW_PVP = register("area.allow_pvp");
     
    static String register(String property) {
        KNOWN_PROPERTIES.add(property);
        return property;
    }

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
        return getString(area, key, "");
    }

    public static String getString(Area area, String key, String fallback) {
        return area.properties.getOrDefault(key, fallback).toString();
    }
    
    private AreaProperties() {}
}