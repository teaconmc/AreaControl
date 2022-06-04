package org.teacon.areacontrol;

import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

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
    public static final PermissionNode<Boolean> CLAIM_AREA   = new PermissionNode<>("area_control", "command.claim", PermissionTypes.BOOLEAN, ANYONE);
    public static final PermissionNode<Boolean> MARK_AREA    = new PermissionNode<>("area_control", "command.mark", PermissionTypes.BOOLEAN, ANYONE);
    public static final PermissionNode<Boolean> UNCLAIM_AREA = new PermissionNode<>("area_control", "command.unclaim", PermissionTypes.BOOLEAN, ANYONE);
    public static final PermissionNode<Boolean> INSPECT      = new PermissionNode<>("area_control", "admin.inspect", PermissionTypes.BOOLEAN, OP_ONLY);

    public static final PermissionNode<Boolean> BYPASS_BREAK_BLOCK = new PermissionNode<>("area_control", "bypass.break_block", PermissionTypes.BOOLEAN, OP_ONLY);
    public static final PermissionNode<Boolean> BYPASS_PLACE_BLOCK = new PermissionNode<>("area_control", "bypass.place_block", PermissionTypes.BOOLEAN, OP_ONLY);
    public static final PermissionNode<Boolean> BYPASS_PVP = new PermissionNode<>("area_control", "bypass.pvp", PermissionTypes.BOOLEAN, OP_ONLY);
    public static final PermissionNode<Boolean> BYPASS_ATTACK = new PermissionNode<>("area_control", "bypass.attack", PermissionTypes.BOOLEAN, OP_ONLY);

    public static final PermissionNode<Boolean> BYPASS_CLICK_BLOCK = new PermissionNode<>("area_control", "bypass.click_block", PermissionTypes.BOOLEAN, ANYONE);
    public static final PermissionNode<Boolean> BYPASS_ACTIVATE_BLOCK = new PermissionNode<>("area_control", "bypass.activate_block", PermissionTypes.BOOLEAN, ANYONE);
    public static final PermissionNode<Boolean> BYPASS_USE_ITEM = new PermissionNode<>("area_control", "bypass.use_item", PermissionTypes.BOOLEAN, ANYONE);
    public static final PermissionNode<Boolean> BYPASS_INTERACT_ENTITY = new PermissionNode<>("area_control", "bypass.interact_entity", PermissionTypes.BOOLEAN, ANYONE);
    public static final PermissionNode<Boolean> BYPASS_INTERACT_ENTITY_SPECIFIC = new PermissionNode<>("area_control", "bypass.interact_entity_specific", PermissionTypes.BOOLEAN, ANYONE);
}
