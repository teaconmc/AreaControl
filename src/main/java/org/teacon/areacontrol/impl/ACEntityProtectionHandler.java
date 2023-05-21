package org.teacon.areacontrol.impl;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.PermissionAPI;
import org.teacon.areacontrol.AreaControlPermissions;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.AreaProperties;

/**
 * Event handlers that protect entities from being hurt.
 * <p>
 * Currently, these handlers are unused because they failed to capture
 * edge cases such as "player shooting arrows at item frame/armor stand".
 * Once we are able to handle these kinds of edge cases, we will
 * re-consider these handles. They are here for reference only.
 * </p>
 */
public class ACEntityProtectionHandler {

    // This one is fired when player directly attacks something else
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level.isClientSide) {
            return;
        }
        // We use the location of target entity to find the area.
        final var target = event.getTarget();
        final var targetArea = AreaManager.INSTANCE.findBy(target.getCommandSenderWorld().dimension(), target.blockPosition());
        if (target instanceof Player) {
            if (!AreaProperties.getBool(targetArea, "area.allow_pvp") && !PermissionAPI.getPermission((ServerPlayer) event.getEntity(), AreaControlPermissions.BYPASS_PVP)) {
                event.getEntity().displayClientMessage(Component.translatable("area_control.notice.pvp_disabled", ObjectArrays.EMPTY_ARRAY), true);
                event.setCanceled(true);
            }
        } else {
            if (!AreaProperties.getBool(targetArea, "area.allow_attack") && !PermissionAPI.getPermission((ServerPlayer) event.getEntity(), AreaControlPermissions.BYPASS_ATTACK)) {
                event.getEntity().displayClientMessage(Component.translatable("area_control.notice.pve_disabled", ObjectArrays.EMPTY_ARRAY), true);
                event.setCanceled(true);
            }
        }
    }

    // This one is fired when player is using "indirect" tools, e.g. ranged weapons such as bows
    // and crossbows, to attack something else.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(LivingAttackEvent event) {
        final var src = event.getSource().getEntity();
        if (src instanceof Player) {
            if (src.level.isClientSide) {
                return;
            }
            // Same above, we use the location of target entity to find the area.
            final var target = event.getEntity();
            final var targetArea = AreaManager.INSTANCE.findBy(target.getCommandSenderWorld().dimension(), target.blockPosition());
            if (target instanceof Player) {
                if (!AreaProperties.getBool(targetArea, "area.allow_pvp") && !PermissionAPI.getPermission((ServerPlayer) src, AreaControlPermissions.BYPASS_PVP)) {
                    ((Player) src).displayClientMessage(Component.translatable("area_control.notice.pvp_disabled", ObjectArrays.EMPTY_ARRAY), true);
                    event.setCanceled(true);
                }
            } else {
                if (!AreaProperties.getBool(targetArea, "area.allow_attack") && !PermissionAPI.getPermission((ServerPlayer) src, AreaControlPermissions.BYPASS_ATTACK)) {
                    ((Player) src).displayClientMessage(Component.translatable("area_control.notice.pve_disabled", ObjectArrays.EMPTY_ARRAY), true);
                    event.setCanceled(true);
                }
            }
        }
    }
}
