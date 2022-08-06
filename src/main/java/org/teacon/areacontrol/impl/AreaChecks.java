package org.teacon.areacontrol.impl;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.List;

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

    public static boolean allowPossession(ItemStack item, Area currentArea) {
        var itemRegName = item.getItem().getRegistryName();
        var itemSpecificPerm = "area.allow_possess." + itemRegName;
        var modSpecificPerm = "area.allow_possess." + (itemRegName == null ? "null" : itemRegName.getNamespace());
        if (AreaProperties.keyPresent(currentArea, itemSpecificPerm) && !AreaProperties.getBool(currentArea, itemSpecificPerm)) {
            return false;
        }
        return !AreaProperties.keyPresent(currentArea, modSpecificPerm) || AreaProperties.getBool(currentArea, modSpecificPerm);
    }

    public static int checkInv(List<ItemStack> inv, Area currentArea, Player player) {
        var invSize = inv.size();
        int seized = 0;
        for (int i = 0; i < invSize; i++) {
            var item = inv.get(i);
            if (!item.isEmpty() && !allowPossession(item, currentArea)) {
                inv.set(i, ItemStack.EMPTY);
                player.displayClientMessage(new TranslatableComponent("area_control.notice.possess_disabled_item", item.getHoverName()), true);
                seized += item.getCount();
            }
        }
        return seized;
    }

    public static boolean checkProp(Area area, String prop, IForgeRegistryEntry<?> target) {
        return checkProp(area, prop, target.getRegistryName());
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
