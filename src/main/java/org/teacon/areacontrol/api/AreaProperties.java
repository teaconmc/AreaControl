package org.teacon.areacontrol.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class AreaProperties {

    /**
     * Set of area properties that are known by the AreaControl mod.
     * Adding a property to this Set is optional; it only enables command auto-completion.
     */
    public static final Set<String> KNOWN_PROPERTIES = new HashSet<>();
    /**
     * Set of area properties that should be synced to client whenever the area information
     * is about to be sent to client.
     */
    public static final Set<String> SYNCED_PROPERTIES = new HashSet<>();

    public static final String SHOW_WELCOME = register("area.display_welcome_message");
    public static final String ALLOW_SPAWN = register("area.allow_spawn");
    @Deprecated(forRemoval = true)
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
    public static final String ALLOW_FIRE_SPREAD = register("area.allow_fire_spread");
    public static final String ALLOW_POSSESS = register("area.allow_possess");
    public static final String ALLOW_RIDE = register("area.allow_ride");

    public static final String ALLOW_ENTITY_USE_SELECTOR_FROM_CHILD = register("area.allow_select_from_child_area_by_entity");
    public static final String ALLOW_ENTITY_USE_SELECTOR_FROM_PARENT = register("area.allow_select_from_parent_area_by_entity");

    public static final String ALLOW_CB_USE_SELECTOR_FROM_CHILD = register("area.allow_select_from_child_area_by_command_block");
    public static final String ALLOW_CB_USE_SELECTOR_FROM_PARENT  = register("area.allow_select_from_parent_area_by_command_block");

    static String register(String property) {
        KNOWN_PROPERTIES.add(property);
        return property;
    }

    public static boolean keyPresent(Area area, String key) {
        return area.properties.containsKey(key);
    }

    public static boolean getBool(@Nullable Area area, String key) {
        return getBool(area, key, true);
    }

    public static boolean getBool(@Nullable Area area, String key, boolean recursive) {
        if (area == null) return false;
        Object o = area.properties.get(key);
        if (o == null || "null".equals(o)) {
            if (recursive) {
                if (area.belongingArea != null) {
                    var parent = AreaControlAPI.areaLookup.findBy(area.belongingArea);
                    return getBool(parent, key, true);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return getBool(o);
        }
    }

    /**
     * @return null if the property is not specified by AC, so you can seek it in gameRule.
     */
    public static Optional<Boolean> getBoolOptional(@Nullable Area area, String key) {
        return getBoolOptional(area, key, true);
    }

    public static Optional<Boolean> getBoolOptional(@Nullable Area area, String key, boolean recursive) {
        if (area == null) return Optional.empty();
        Object o = area.properties.get(key);
        if (o == null || "null".equals(o)) {
            if (recursive) {
                if (area.belongingArea != null) {
                    var parent = AreaControlAPI.areaLookup.findBy(area.belongingArea);
                    return getBoolOptional(parent, key, true);
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.of(getBool(o));
        }
    }

    private static boolean getBool(@NotNull Object o) {
        if (o instanceof Boolean) {
            return (Boolean) o;
        } else if (o instanceof Number) {
            return ((Number) o).intValue() != 0;
        } else {
            return "true".equals(o) || "t".equals(o) || Character.valueOf('t').equals(o);
        }
    }
    
    private AreaProperties() {}
}