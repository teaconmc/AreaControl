package org.teacon.areacontrol.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class AreaProperties {

    /**
     * Set of area properties that are known by the AreaControl mod.
     * Adding a property to this Set is optional; it only enables command auto-completion.
     */
    public static final Set<String> KNOWN_PROPERTIES = new LinkedHashSet<>();
    /**
     * Set of area properties that should be synced to client whenever the area information
     * is about to be sent to client.
     */
    public static final Set<String> SYNCED_PROPERTIES = new HashSet<>();

    public static final String SHOW_WELCOME = register("display_welcome_message");
    public static final String ALLOW_SPAWN = register("spawn");
    public static final String ALLOW_PVP = register("pvp");
    public static final String ALLOW_PVE = register("attack");
    public static final String ALLOW_INTERACT_ENTITY = register("interact_entity");
    public static final String ALLOW_RIDE = register("ride");
    public static final String ALLOW_POSSESS = register("possess");
    public static final String ALLOW_USE_ITEM = register("use_item");
    public static final String ALLOW_BREAK = register("break_block");
    public static final String ALLOW_CLICK = register("click_block");
    public static final String ALLOW_ACTIVATE = register("activate_block");
    public static final String ALLOW_PLACE_BLOCK = register("place_block");
    public static final String ALLOW_TRAMPLE_FARMLAND = register("trample_farmland");
    public static final String ALLOW_EXPLOSION = register("explosion");
    public static final String ALLOW_EXPLOSION_AFFECT_BLOCKS = register("explosion_affect_blocks");
    public static final String ALLOW_EXPLOSION_AFFECT_ENTITIES = register("explosion_affect_entities");
    public static final String ALLOW_FIRE_SPREAD = register("fire_spread");

    public static final String ALLOW_ENTITY_USE_SELECTOR_FROM_CHILD = register("select_from_child_area_by_entity");
    public static final String ALLOW_ENTITY_USE_SELECTOR_FROM_PARENT = register("select_from_parent_area_by_entity");

    public static final String ALLOW_CB_USE_SELECTOR_FROM_CHILD = register("select_from_child_area_by_command_block");
    public static final String ALLOW_CB_USE_SELECTOR_FROM_PARENT = register("select_from_parent_area_by_command_block");

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
                var parent = area.resolveParent();
                return getBool(parent, key, true);
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
                var parent = area.resolveParent();
                return getBoolOptional(parent, key, true);
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

    private AreaProperties() {
    }
}