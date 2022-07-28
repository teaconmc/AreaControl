package org.teacon.areacontrol.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import org.teacon.areacontrol.api.Area;

public class AreaChecks {

    public static boolean allow(Player p, Area area, PermissionNode<Boolean> perm) {
        var uuid = p.getGameProfile().getId();
        boolean isFriend = area.owner.equals(uuid) || area.friends.contains(uuid);
        if (!isFriend && p instanceof ServerPlayer sp) {
            isFriend = PermissionAPI.getPermission(sp, perm);
        }
        return isFriend;
    }

    public static boolean allow(ServerPlayer p, Area area, PermissionNode<Boolean> perm) {
        var uuid = p.getGameProfile().getId();
        return area.owner.equals(uuid) || area.friends.contains(uuid) || PermissionAPI.getPermission(p, perm);
    }

}
