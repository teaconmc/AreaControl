package org.teacon.areacontrol.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teacon.areacontrol.AreaControlPermissions;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaControlAPI;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.List;

public class AreaChecks {

    public static boolean isACtrlAdmin(ServerPlayer p) {
        return PermissionAPI.getPermission(p, AreaControlPermissions.AC_ADMIN);
    }

    public static boolean isACtrlAreaOwner(@NotNull ServerPlayer p, @Nullable Area area) {
        if (area != null) {
            var uid = p.getGameProfile().getId();
            var group = AreaControlAPI.groupProvider.getGroupFor(uid);
            // 1. Check if player is one of owners
            if (area.owners.contains(uid) || area.ownerGroups.contains(group)) {
                return true;
            }
            // 2. If area has parent area, check if it owns parent
            var parent = AreaManager.INSTANCE.findBy(area.belongingArea);
            if (parent != null) {
                return parent.owners.contains(uid) || parent.ownerGroups.contains(group);
            }
        }
        // 3. Check if player is admin.
        return isACtrlAdmin(p);
    }

    public static boolean isACtrlAreaBuilder(@NotNull ServerPlayer p, @Nullable Area area) {
        if (area != null) {
            var uid = p.getGameProfile().getId();
            var group = AreaControlAPI.groupProvider.getGroupFor(uid);
            // 1. Check if player is one of builders
            if (area.builders.contains(uid) || area.builderGroups.contains(group)) {
                return true;
            }
            // 2. If area has parent area, check if it owns parent
            var parent = AreaManager.INSTANCE.findBy(area.belongingArea);
            if (parent != null) {
                return parent.builders.contains(uid) || parent.builderGroups.contains(group);
            }
        }
        // 3. Check if player is admin.
        return isACtrlAdmin(p);
    }

    public static boolean allow(Player p, @Nullable Area area, PermissionNode<Boolean> perm) {
        var uuid = p.getGameProfile().getId();
        boolean isFriend = area != null && (area.owners.contains(uuid) || area.builders.contains(uuid));
        if (!isFriend && p instanceof ServerPlayer sp) {
            isFriend = PermissionAPI.getPermission(sp, perm);
        }
        return isFriend;
    }

    public static boolean allow(ServerPlayer p, @Nullable Area area, PermissionNode<Boolean> perm) {
        var uuid = p.getGameProfile().getId();
        return (area != null && (area.owners.contains(uuid) || area.builders.contains(uuid))) || PermissionAPI.getPermission(p, perm);
    }

    public static void checkInv(List<ItemStack> inv, @Nullable Area currentArea, Player player) {
        var invSize = inv.size();
        for (int i = 0; i < invSize; i++) {
            var item = inv.get(i);
            if (!item.isEmpty() && !checkPossess(currentArea, item.getItem())) {
                inv.set(i, ItemStack.EMPTY);
                player.displayClientMessage(Component.translatable("area_control.notice.possess_disabled_item", item.getHoverName()), true);
            }
        }
    }

    // This is a separate method because area.allow_possess currently has a different logic
    private static boolean checkPossess(Area area, Item item) {
        var targetId = ForgeRegistries.ITEMS.getKey(item);
        if (targetId != null) {
            var objSpecific = AreaProperties.getBoolOptional(area, AreaProperties.ALLOW_POSSESS + "." + targetId);
            if (objSpecific.isPresent()) {
                return objSpecific.get();
            } else {
                var modSpecific = AreaProperties.getBoolOptional(area, AreaProperties.ALLOW_POSSESS + "." + targetId.getNamespace());
                if (modSpecific.isPresent()) {
                    return modSpecific.get();
                }
            }
        }
        // FIXME Use global fallback instead
        return true;
    }

    public static boolean checkProp(Area area, String prop, ResourceLocation targetId) {
        if (targetId != null) {
            var objSpecific = AreaProperties.getBoolOptional(area, prop + "." + targetId);
            if (objSpecific.isPresent()) {
                return objSpecific.get();
            } else {
                var modSpecific = AreaProperties.getBoolOptional(area, prop + "." + targetId.getNamespace());
                if (modSpecific.isPresent()) {
                    return modSpecific.get();
                }
            }
        }
        return AreaProperties.getBool(area, prop);
    }

}
