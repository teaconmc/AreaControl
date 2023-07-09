package org.teacon.areacontrol;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;
import org.teacon.areacontrol.impl.AreaChecks;

@Mod.EventBusSubscriber(modid = "area_control")
public final class AreaControlEventHandlers {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCheckSpawn(EntityJoinLevelEvent event) {
        // Note from 3TUSK:
        // This event may be fired off-thread, and thus this handler may cause deadlock.
        // However, theoretically that could not happen:
        //   1. We avoid doing anything when the entity is loaded from disk, which should
        //      be the #1 source of off-thread spawning.
        //   2. No operations here is modifying underlying states of any objects, unless
        //      someone is overriding Entity.blockPosition (m_142538_).
        // Further, AreaManager.findBy is synchronized.
        if (!event.loadedFromDisk() && !event.getLevel().isClientSide()) {
            final var entityInQuestion = event.getEntity();
            if (entityInQuestion instanceof Player) {
                return;
            }
            final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel().dimension(), event.getEntity().blockPosition());
            final var entityId = ForgeRegistries.ENTITY_TYPES.getKey(entityInQuestion.getType());
            if (!AreaChecks.checkProp(targetArea, AreaProperties.ALLOW_SPAWN, entityId)) {
                event.setCanceled(true);
            }
        }
    }

    //@SubscribeEvent(priority = EventPriority.HIGHEST) // TODO (3TUSK): Re-evaluate
    public static void onSpecialSpawn(MobSpawnEvent event) {
        final BlockPos spawnPos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), spawnPos);
        if (!AreaProperties.getBool(targetArea, "area.allow_special_spawn")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity().level.isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_interact_entity_specific")
                && !AreaChecks.allow(event.getEntity(), targetArea, AreaControlPermissions.BYPASS_INTERACT_ENTITY_SPECIFIC)) {
                event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_interact_entity")
                && !AreaChecks.allow(event.getEntity(), targetArea, AreaControlPermissions.BYPASS_INTERACT_ENTITY)) {
                event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        final var p = event.getPlayer();
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getPos());
        final var block = event.getLevel().getBlockState(event.getPos());
        final var blockId = ForgeRegistries.BLOCKS.getKey(block.getBlock());
        var allowed = AreaChecks.checkProp(targetArea, AreaProperties.ALLOW_BREAK, blockId);
        allowed |= AreaChecks.allow(p, targetArea, AreaControlPermissions.BYPASS_BREAK_BLOCK);
        if (!allowed) {
            p.displayClientMessage(Component.translatable("area_control.notice.break_block_disabled", ObjectArrays.EMPTY_ARRAY), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        final var p = event.getEntity();
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getPos());
        final var block = event.getLevel().getBlockState(event.getPos());
        final var blockId = ForgeRegistries.BLOCKS.getKey(block.getBlock());
        var allowed = AreaChecks.checkProp(targetArea, AreaProperties.ALLOW_CLICK, blockId);
        allowed |= AreaChecks.allow(p, targetArea, AreaControlPermissions.BYPASS_CLICK_BLOCK);
        if (!allowed) {
            p.displayClientMessage(Component.translatable("area_control.notice.click_block_disabled", ObjectArrays.EMPTY_ARRAY), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onActivateBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        final var player = event.getEntity();
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getPos());
        final var block = event.getLevel().getBlockState(event.getPos());
        final var blockId = ForgeRegistries.BLOCKS.getKey(block.getBlock());
        var allowed = AreaChecks.checkProp(targetArea, AreaProperties.ALLOW_ACTIVATE, blockId);
        allowed |= AreaChecks.allow(player, targetArea, AreaControlPermissions.BYPASS_ACTIVATE_BLOCK);
        if (!allowed) {
            player.displayClientMessage(Component.translatable("area_control.notice.activate_block_disabled", ObjectArrays.EMPTY_ARRAY), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        final var p = event.getEntity();
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getPos());
        final var theItem = event.getItemStack().getItem();
        final var itemId = ForgeRegistries.ITEMS.getKey(theItem);
        var allowed = AreaChecks.checkProp(targetArea, AreaProperties.ALLOW_USE_ITEM, itemId);
        allowed |= AreaChecks.allow(p, targetArea, AreaControlPermissions.BYPASS_USE_ITEM);
        if (!allowed) {
            p.displayClientMessage(Component.translatable("area_control.notice.use_item_disabled", ObjectArrays.EMPTY_ARRAY), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTramplingFarmland(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_trample_farmland")) {
            // TODO area_control.bypass.trample_farmland?
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getPos());
        final var block = event.getLevel().getBlockState(event.getPos());
        final var blockId = ForgeRegistries.BLOCKS.getKey(block.getBlock());
        var allowed = AreaChecks.checkProp(targetArea, AreaProperties.ALLOW_PLACE_BLOCK, blockId);
        final var placer = event.getEntity();
        if (placer instanceof ServerPlayer p) {
            allowed |= AreaChecks.allow(p, targetArea, AreaControlPermissions.BYPASS_PLACE_BLOCK);
        }
        if (!allowed) {
            // TODO Client will falsely report item being consumed; however it will return to normal if you click again in inventory GUI
            event.setCanceled(true);
            if (placer instanceof ServerPlayer p) {
                p.displayClientMessage(Component.translatable("area_control.notice.place_block_disabled", ObjectArrays.EMPTY_ARRAY), true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeExplosion(ExplosionEvent.Start event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getExplosion().getPosition());
        if (!AreaProperties.getBool(targetArea, "area.allow_explosion")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void afterExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getExplosion().getPosition());
        if (!AreaProperties.getBool(targetArea, "area.allow_explosion_affect_blocks")) {
            event.getAffectedBlocks().clear();
        } else {
            for (var itr = event.getAffectedBlocks().iterator(); itr.hasNext();) {
                BlockPos affected = itr.next();
                final Area a = AreaManager.INSTANCE.findBy(event.getLevel(), affected);
                if (!AreaProperties.getBool(a, "area.allow_explosion_affect_blocks")) {
                    itr.remove();
                }
            }
        }
        if (!AreaProperties.getBool(targetArea, "area.allow_explosion_affect_entities")) {
            event.getAffectedEntities().clear();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tryRide(EntityMountEvent event) {
        if (event.isMounting() && !event.getLevel().isClientSide) {
            var vehicle = event.getEntityBeingMounted();
            var entityId = ForgeRegistries.ENTITY_TYPES.getKey(vehicle.getType());
            var area = AreaManager.INSTANCE.findBy(event.getLevel(), vehicle.blockPosition());
            if (!AreaChecks.checkProp(area, AreaProperties.ALLOW_RIDE, entityId)) {
                var rider = event.getEntityMounting();
                if (rider instanceof Player p) {
                    p.displayClientMessage(Component.translatable("area_control.notice.ride_disabled", vehicle.getDisplayName()), true);
                }
                event.setCanceled(true);
            }
        }
    }
}