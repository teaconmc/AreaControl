package org.teacon.areacontrol;

import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;

@Mod.EventBusSubscriber(modid = "area_control")
public final class AreaControlEventHandlers {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        final BlockPos spawnPos = new BlockPos(event.getX(), event.getY(), event.getZ());
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), spawnPos);
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_spawn")) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        final BlockPos spawnPos = new BlockPos(event.getX(), event.getY(), event.getZ());
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), spawnPos);
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_special_spawn")) {
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
        if (targetArea != null) {
            if (target instanceof PlayerEntity) {
                if (!AreaProperties.getBool(targetArea, "area.allow_pvp") && !PermissionAPI.hasPermission(event.getPlayer(), "area_control.bypass.pvp")) {
                    event.setCanceled(true);
                }
            } else {
                if (!AreaProperties.getBool(targetArea, "area.allow_attack") && !PermissionAPI.hasPermission(event.getPlayer(), "area_control.bypass.attack")) {
                    event.setCanceled(true); // TODO Show notice when this action is blocked
                }
            }
        }
    }

    // This one is fired when player is using "indirect" tools, e.g. ranged weapons such as bows
    // and crossbows, to attack something else.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(LivingAttackEvent event) {
        final Entity src = event.getSource().getEntity();
        if (src instanceof PlayerEntity) {
            if (src.level.isClientSide) {
                return;
            }
            // Same above, we use the location of target entity to find the area.
            final Entity target = event.getEntity();
            final Area targetArea = AreaManager.INSTANCE.findBy(target.getCommandSenderWorld().dimension(), target.blockPosition());
            if (targetArea != null) {
                if (target instanceof PlayerEntity) {
                    if (!AreaProperties.getBool(targetArea, "area.allow_pvp") && !PermissionAPI.hasPermission((PlayerEntity) src, "area_control.bypass.pvp")) {
                        event.setCanceled(true);
                    }
                } else {
                    if (!AreaProperties.getBool(targetArea, "area.allow_attack") && !PermissionAPI.hasPermission((PlayerEntity) src, "area_control.bypass.attack")) {
                        event.setCanceled(true);
                    }
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
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_interact_entity_specific")
            && !PermissionAPI.hasPermission(event.getPlayer(), "area_control.bypass.interact_entity_specific")) {
                event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_interact_entity")
            && !PermissionAPI.hasPermission(event.getPlayer(), "area_control.bypass.interact_entity")) {
                event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_break_block")
            && !PermissionAPI.hasPermission(event.getPlayer(), "area_control.bypass.break_block")) {
                event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_click_block")
            && !PermissionAPI.hasPermission(event.getPlayer(), "area_control.bypass.click_block")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onActivateBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_activate_block")
            && !PermissionAPI.hasPermission(event.getPlayer(), "area_control.bypass.activate_block")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_use_item")
            && !PermissionAPI.hasPermission(event.getPlayer(), "area_control.bypass.use_item")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTramplingFarmland(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_trample_farmland")) {
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
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_place_block")) {
            final Entity e = event.getEntity();
            if (e instanceof PlayerEntity && !PermissionAPI.hasPermission((PlayerEntity) e, "area_control.bypass.place_block")) {
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
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_explosion")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void afterExplosion(ExplosionEvent.Detonate event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld().dimension(), new BlockPos(event.getExplosion().getPosition()));
        if (targetArea != null) {
            if (!AreaProperties.getBool(targetArea, "area.allow_explosion_affect_blocks")) {
                event.getAffectedBlocks().clear();
            }
            if (!AreaProperties.getBool(targetArea, "area.allow_explosion_affect_entities")) {
                event.getAffectedEntities().clear();
            }
        }
    }
}