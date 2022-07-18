package org.teacon.areacontrol.api;

import java.util.HashSet;
import java.util.Set;

public final class AreaProperties {

    /** 
     * Set of area properties that are known by the AreaControl mod.
     * Adding a property to this Set is optional; it only enables command auto-completion.
     */
    public static final Set<String> KNOWN_PROPERTIES = new HashSet<>();   

    public static final String SHOW_WELCOME = register("area.display_welcome_message");
    public static final String ALLOW_SPAWN = register("area.allow_spawn");
    public static final String ALLOW_SPECIAL_SPAWN = register("area.allow_special_spawn");
    public static final String ALLOW_PVP = register("area.allow_pvp");
    public static final String ALLOW_PVE = register("area.allow_attack");
    public static final String ALLOW_INTERACT_ENTITY_SP = register("area.allow_interact_entity_specific");
    public static final String ALLOW_INTERACT_ENTITY = register("area.allow_interact_entity");
    public static final String ALLOW_BREAK = register("area.allow_break_block");
    public static final String ALLOW_CLICK = register("area.allow_click_block");
    public static final String ALLOW_ACTIVATE = register("area.allow_activate_block");
    public static final String ALLOW_USE_ITEM = register("area.allow_use_item");
    public static final String ALLOW_PLACE_BLOCK = register("area.allow_place_block");
    public static final String ALLOW_TRAMPLE_FARMLAND = register("area.allow_trample_farmland");
    public static final String ALLOW_EXPLOSION = register("area.allow_explosion");
    public static final String ALLOW_EXPLOSION_AFFECT_BLOCKS = register("area.allow_explosion_affect_blocks");
    public static final String ALLOW_EXPLOSION_AFFECT_ENTITIES = register("area.allow_explosion_affect_entities");

    static String register(String property) {
        KNOWN_PROPERTIES.add(property);
        return property;
    }

    public static boolean keyPresent(Area area, String key) {
        return area.properties.containsKey(key);
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