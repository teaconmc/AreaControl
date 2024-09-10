package org.teacon.areacontrol.impl;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teacon.areacontrol.AreaControl;
import org.teacon.areacontrol.AreaControlConfig;
import org.teacon.areacontrol.AreaControlPermissions;
import org.teacon.areacontrol.AreaControlPlayerTracker;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaControlAPI;
import org.teacon.areacontrol.api.AreaProperties;
import org.teacon.areacontrol.impl.seizer.AreaControlBorderControl;
import org.teacon.areacontrol.impl.seizer.ConfiscationInv;

import java.util.List;
import java.util.function.Supplier;

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
            var parent = area.resolveParent();
            if (parent != null && (parent.owners.contains(uid) || parent.ownerGroups.contains(group))) {
                return true;
            }
        }
        // 3. Check if player is admin.
        return isACtrlAdmin(p);
    }

    public static boolean isACtrlAreaBuilder(@NotNull ServerPlayer p, @Nullable Area area) {
        return isACtrlAreaBuilder(p, area, true);
    }

    public static boolean isACtrlAreaBuilder(@NotNull ServerPlayer p, @Nullable Area area, boolean includeParent) {
        if (area != null) {
            var uid = p.getGameProfile().getId();
            var group = AreaControlAPI.groupProvider.getGroupFor(uid);
            // 1. Check if player is one of builders
            if (area.owners.contains(uid) || area.builders.contains(uid) || area.ownerGroups.contains(group) || area.builderGroups.contains(group)) {
                return true;
            }
            // 2. If area has parent area, check if it owns parent
            if (includeParent) {
                var parent = area.resolveParent();
                if (parent != null && (parent.owners.contains(uid) || parent.builders.contains(uid) || parent.ownerGroups.contains(group) || parent.builderGroups.contains(group))) {
                    return true;
                }
            }
        }
        // 3. Check if player is admin.
        return isACtrlAdmin(p);
    }

    public static void checkInv(List<ItemStack> inv, @Nullable Area currentArea, Player player) {
        // If bypass mode is on, then this check can be skipped.
        if (AreaControlPlayerTracker.INSTANCE.hasBypassModeOnForArea(player, currentArea)) {
            return;
        }
        ConfiscationInv seizedInv = player.getData(AreaControlBorderControl.CONFISCATION_INV);
        var invSize = inv.size();
        for (int i = 0; i < invSize; i++) {
            var item = inv.get(i);
            if (!item.isEmpty() && !checkPossess(currentArea, item.getItem())) {
                ItemStack seized = inv.set(i, ItemStack.EMPTY);
                seizedInv.add(seized);
                player.displayClientMessage(Component.translatable("area_control.notice.possess_disabled_item", item.getHoverName()), true);
            }
        }
    }

    // This is a separate method because area.allow_possess currently has a different logic
    public static boolean checkPossess(Area area, Item item) {
        var targetId = BuiltInRegistries.ITEM.getKey(item);
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
        return AreaControlConfig.allowPossessItem.get();
    }

    /**
     * Recursively checks if an action is allowed in given area
     *
     * @param area     Area to check
     * @param actor    The entity that carries out the action
     * @param prop     The action, represented by a string property
     * @param targetId The object on which the action is being carried out.
     * @return true if such action is allowed; false otherwise.
     */
    public static boolean checkPropFor(final @Nullable Area area, final @Nullable Entity actor,
                                       final @NotNull String prop, final @Nullable ResourceLocation targetId,
                                       final @Nullable Supplier<@NotNull Boolean> defaultValue) {
        if (actor != null) {
            // If actor is in single-player (without being published to LAN), then skip all checks.
            if (AreaControl.singlePlayerServerChecker.test(actor.getServer())) {
                return true;
            }
            // If bypass mode is activated, then skip all checks.
            if (AreaControlPlayerTracker.INSTANCE.hasBypassModeOnForArea(actor, area)) {
                return true;
            }
        }
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
        if (defaultValue != null) {
            return AreaProperties.getBoolOptional(area, prop).orElseGet(defaultValue);
        } else {
            return AreaProperties.getBool(area, prop);
        }
    }

    public static void sendNotificationTo(@NotNull Player player, String translationKey, Object... args) {
        if (player instanceof ServerPlayer sp) {
            if (!sp.connection.getConnectionType().isNeoForge()) {
                String clientLangType = sp.getLanguage();
                // TODO
            }
        }
        player.displayClientMessage(Component.translatable(translationKey, args), true);
    }

}
