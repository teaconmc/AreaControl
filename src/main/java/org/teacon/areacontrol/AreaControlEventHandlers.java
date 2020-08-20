package org.teacon.areacontrol;

import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "area_control")
public final class AreaControlEventHandlers {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        final BlockPos spawnPos = new BlockPos(event.getX(), event.getY(), event.getZ());
        final Area targetArea = AreaManager.INSTANCE.findBy(spawnPos);
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_spawn")) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        final BlockPos spawnPos = new BlockPos(event.getX(), event.getY(), event.getZ());
        final Area targetArea = AreaManager.INSTANCE.findBy(spawnPos);
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_special_spawn")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getPos());
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_break_block")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getPos());
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_click_block")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onActivateBlock(PlayerInteractEvent.RightClickBlock event) {
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getPos());
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_activate_block")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getPos());
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_use_item")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getPos());
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_place_block")) {
            // TODO Does this consume the item?
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeExplosion(ExplosionEvent.Start event) {
        final Area targetArea = AreaManager.INSTANCE.findBy(new BlockPos(event.getExplosion().getPosition()));
        if (targetArea != null && !AreaProperties.getBool(targetArea, "area.allow_explosion")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void afterExplosion(ExplosionEvent.Detonate event) {
        final Area targetArea = AreaManager.INSTANCE.findBy(new BlockPos(event.getExplosion().getPosition()));
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