package org.teacon.areacontrol;

import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

public class AreaControlPermissions {

    private static final boolean DEBUG = Boolean.getBoolean("area_control.dev");

    static final PermissionNode.PermissionResolver<Boolean> OP_ONLY = (p, uuid, contexts) -> {
        // Cannot grant permission if there is no player.
        if (p == null) {
            return true;
        }
        var server = p.level().getServer();
        // Cannot grant permission if there is no server.
        if (server == null) {
            return false;
        }
        // Grant permission if it is the sole player in single-player.
        // Skip this check if in debug mode.
        if (!DEBUG && server.isSingleplayerOwner(new NameAndId(p.getGameProfile()))) {
            return true; // Bypass single-player
        }
        return p.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
    };

    static final PermissionNode.PermissionResolver<Boolean> ANYONE = (p, uuid, contexts) -> true;

    public static final PermissionNode<Boolean> AC_ADMIN = new PermissionNode<>("area_control", "command.admin", PermissionTypes.BOOLEAN, OP_ONLY);
    public static final PermissionNode<Boolean> AC_CLAIMER = new PermissionNode<>("area_control", "command.claim", PermissionTypes.BOOLEAN, ANYONE);
    public static final PermissionNode<Boolean> AC_BUILDER = new PermissionNode<>("area_control", "command.build", PermissionTypes.BOOLEAN, ANYONE);

}
