package org.teacon.areacontrol;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

@Mod.EventBusSubscriber(modid = "area_control")
public final class AreaControlEventHandlers {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        final BlockPos spawnPos = new BlockPos(event.getX(), event.getY(), event.getZ());
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), spawnPos);
        if (!AreaProperties.getBool(targetArea, "area.allow_spawn")) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        final BlockPos spawnPos = new BlockPos(event.getX(), event.getY(), event.getZ());
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), spawnPos);
        if (!AreaProperties.getBool(targetArea, "area.allow_special_spawn")) {
            event.setCanceled(true);
        }
    }

    // This one is fired when player directly attacks something else
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getPlayer().level.isClientSide) {
            return;
        }
        // We use the location of target entity to find the area.
        final Entity target = event.getTarget();
        final Area targetArea = AreaManager.INSTANCE.findBy(target.getCommandSenderWorld().dimension(), target.blockPosition());
        if (target instanceof Player) {
            if (!AreaProperties.getBool(targetArea, "area.allow_pvp") && !PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_PVP)) {
                event.getPlayer().displayClientMessage(new TranslatableComponent("area_control.notice.pvp_disabled", ObjectArrays.EMPTY_ARRAY), true);
                event.setCanceled(true);
            }
        } else {
            if (!AreaProperties.getBool(targetArea, "area.allow_attack") && !PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_ATTACK)) {
                event.getPlayer().displayClientMessage(new TranslatableComponent("area_control.notice.pve_disabled", ObjectArrays.EMPTY_ARRAY), true);
                event.setCanceled(true); // TODO Show notice when this action is blocked
            }
        }
    }

    // This one is fired when player is using "indirect" tools, e.g. ranged weapons such as bows
    // and crossbows, to attack something else.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(LivingAttackEvent event) {
        final Entity src = event.getSource().getEntity();
        if (src instanceof Player) {
            if (src.level.isClientSide) {
                return;
            }
            // Same above, we use the location of target entity to find the area.
            final Entity target = event.getEntity();
            final Area targetArea = AreaManager.INSTANCE.findBy(target.getCommandSenderWorld().dimension(), target.blockPosition());
            if (target instanceof Player) {
                if (!AreaProperties.getBool(targetArea, "area.allow_pvp") && !PermissionAPI.getPermission((ServerPlayer) src, AreaControlPermissions.BYPASS_PVP)) {
                    ((Player) src).displayClientMessage(new TranslatableComponent("area_control.notice.pvp_disabled", ObjectArrays.EMPTY_ARRAY), true);
                    event.setCanceled(true);
                }
            } else {
                if (!AreaProperties.getBool(targetArea, "area.allow_attack") && !PermissionAPI.getPermission((ServerPlayer) src, AreaControlPermissions.BYPASS_ATTACK)) {
                    ((Player) src).displayClientMessage(new TranslatableComponent("area_control.notice.pve_disabled", ObjectArrays.EMPTY_ARRAY), true);
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getPlayer().level.isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_interact_entity_specific")
                && !PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_INTERACT_ENTITY_SPECIFIC)) {
                event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_interact_entity")
                && !PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_INTERACT_ENTITY)) {
                event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final var p = event.getPlayer();
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_break_block")
           && !PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_BREAK_BLOCK)) {
                p.displayClientMessage(new TranslatableComponent("area_control.notice.break_block_disabled", ObjectArrays.EMPTY_ARRAY), true);
                event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        final var  p = event.getPlayer();
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_click_block") && !PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_CLICK_BLOCK)) {
            p.displayClientMessage(new TranslatableComponent("area_control.notice.click_block_disabled", ObjectArrays.EMPTY_ARRAY), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onActivateBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_activate_block")
                && !PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_ACTIVATE_BLOCK)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        final var p = event.getPlayer();
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_use_item")
                && !PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_USE_ITEM)) {
            p.displayClientMessage(new TranslatableComponent("area_control.notice.use_item_disabled", ObjectArrays.EMPTY_ARRAY), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTramplingFarmland(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_trample_farmland")) {
            // TODO area_control.bypass.trample_farmland?
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_place_block")) {
            final Entity e = event.getEntity();
            if (e instanceof ServerPlayer && !PermissionAPI.getPermission((ServerPlayer) e, AreaControlPermissions.BYPASS_PLACE_BLOCK)) {
                // TODO This one does consume the item. Ideas?
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeExplosion(ExplosionEvent.Start event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld().dimension(), new BlockPos(event.getExplosion().getPosition()));
        if (!AreaProperties.getBool(targetArea, "area.allow_explosion")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void afterExplosion(ExplosionEvent.Detonate event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld().dimension(), new BlockPos(event.getExplosion().getPosition()));
        if (!AreaProperties.getBool(targetArea, "area.allow_explosion_affect_blocks")) {
            event.getAffectedBlocks().clear();
        }
        if (!AreaProperties.getBool(targetArea, "area.allow_explosion_affect_entities")) {
            event.getAffectedEntities().clear();
        }
    }
}