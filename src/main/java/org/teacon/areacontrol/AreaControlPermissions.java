package org.teacon.areacontrol;

import net.minecraftforge.server.permission.nodes.PermissionDynamicContextKey;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

public class AreaControlPermissions {

    static final PermissionNode.PermissionResolver<Boolean> OP_ONLY = (p, uuid, contexts) -> {
        if (p == null) return true;
        var server = p.getServer();
        if (server == null) return false; // There isn't a server?
        if (server.isSingleplayerOwner(p.getGameProfile())) return true; // Bypass single-player
        return p.hasPermissions(2);
    };
    static final PermissionNode.PermissionResolver<Boolean> ANYONE = (p, uuid, contexts) -> true;

    public static final PermissionNode<Boolean> SET_PROPERTY = new PermissionNode<>("area_control", "command.set_property", PermissionTypes.BOOLEAN, OP_ONLY);
    public static final PermissionNode<Boolean> SET_FRIENDS  = new PermissionNode<>("area_control", "command.set_friend", PermissionTypes.BOOLEAN, OP_ONLY);
    public static final PermissionNode<Boolean> CLAIM_AREA   = new PermissionNode<>("area_control", "command.claim", PermissionTypes.BOOLEAN, ANYONE);
    public static final PermissionNode<Boolean> MARK_AREA    = new PermissionNode<>("area_control", "command.mark", PermissionTypes.BOOLEAN, ANYONE);
    public static final PermissionNode<Boolean> UNCLAIM_AREA = new PermissionNode<>("area_control", "command.unclaim", PermissionTypes.BOOLEAN, ANYONE);
    public static final PermissionNode<Boolean> INSPECT      = new PermissionNode<>("area_control", "admin.inspect", PermissionTypes.BOOLEAN, OP_ONLY);

    public static final PermissionNode<Boolean> BYPASS_BREAK_BLOCK = new PermissionNode<>("area_control", "bypass.break_block", PermissionTypes.BOOLEAN, areaSensitive("area.allow_break_block"));
    public static final PermissionNode<Boolean> BYPASS_PLACE_BLOCK = new PermissionNode<>("area_control", "bypass.place_block", PermissionTypes.BOOLEAN, areaSensitive("area.allow_place_block"));
    public static final PermissionNode<Boolean> BYPASS_PVP = new PermissionNode<>("area_control", "bypass.pvp", PermissionTypes.BOOLEAN, areaSensitive("area.allow_pvp"));
    public static final PermissionNode<Boolean> BYPASS_ATTACK = new PermissionNode<>("area_control", "bypass.attack", PermissionTypes.BOOLEAN, areaSensitive("area.allow_attack"));

    public static final PermissionNode<Boolean> BYPASS_CLICK_BLOCK = new PermissionNode<>("area_control", "bypass.click_block", PermissionTypes.BOOLEAN, areaSensitive("area.allow_click_block"));
    public static final PermissionNode<Boolean> BYPASS_ACTIVATE_BLOCK = new PermissionNode<>("area_control", "bypass.activate_block", PermissionTypes.BOOLEAN, areaSensitive("area.allow_activate_block"));
    public static final PermissionNode<Boolean> BYPASS_USE_ITEM = new PermissionNode<>("area_control", "bypass.use_item", PermissionTypes.BOOLEAN, areaSensitive("area.allow_use_item"));
    public static final PermissionNode<Boolean> BYPASS_INTERACT_ENTITY = new PermissionNode<>("area_control", "bypass.interact_entity", PermissionTypes.BOOLEAN, areaSensitive("area.allow_interact_entity"));
    public static final PermissionNode<Boolean> BYPASS_INTERACT_ENTITY_SPECIFIC = new PermissionNode<>("area_control", "bypass.interact_entity_specific", PermissionTypes.BOOLEAN, areaSensitive("area.allow_interact_entity_specific"));

    public static final boolean DEBUG = Boolean.getBoolean("area_control.dev");

    public static final PermissionDynamicContextKey<Area> KEY_AREA = new PermissionDynamicContextKey<>(Area.class, "global_pos", a -> a.uid.toString());

    public static PermissionNode.PermissionResolver<Boolean> areaSensitive(String areaProp) {
        return ((player, playerUUID, contexts) -> {
            // Cannot grant permission if there is no player.
            if (player == null) {
                return Boolean.FALSE;
            }
            var server = player.getServer();
            // Cannot grant permission if there is no server.
            if (server == null) {
                return Boolean.FALSE;
            }
            // Grant permission if it is the sole player in single-player.
            // Skip this check if in debug mode.
            if (!DEBUG && server.isSingleplayerOwner(player.getGameProfile())) {
                return Boolean.TRUE;
            }
            // Extract area info from contexts
            Area area = null;
            for (var context : contexts) {
                if (context.getDynamic().typeToken() == Area.class) {
                    area = (Area) context.getValue();
                    break;
                }
            }
            Boolean result = null;
            // Check the area properties, if not allowed, then check if it is the owner.
            if (area != null) {
                result = AreaProperties.getBool(area, areaProp) || area.owner.equals(playerUUID);
            }
            // Otherwise, check if it is server moderator.
            if (result != Boolean.TRUE) {
                result = player.hasPermissions(2);
            }
            return result;
        });
    }
}
