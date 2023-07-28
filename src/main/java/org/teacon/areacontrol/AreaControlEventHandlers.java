package org.teacon.areacontrol;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
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
            if (!AreaChecks.checkPropFor(targetArea, null, AreaProperties.ALLOW_SPAWN, entityId)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getPos());
        final var targetType = event.getTarget().getType();
        final var targetTypeId = ForgeRegistries.ENTITY_TYPES.getKey(targetType);
        if (!AreaChecks.checkPropFor(targetArea, event.getEntity(), AreaProperties.ALLOW_INTERACT_ENTITY, targetTypeId)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getPos());
        final var targetType = event.getTarget().getType();
        final var targetTypeId = ForgeRegistries.ENTITY_TYPES.getKey(targetType);
        if (!AreaChecks.checkPropFor(targetArea, event.getEntity(), AreaProperties.ALLOW_INTERACT_ENTITY, targetTypeId)) {
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
        var allowed = AreaChecks.checkPropFor(targetArea, p, AreaProperties.ALLOW_BREAK, blockId);
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
        var allowed = AreaChecks.checkPropFor(targetArea, p, AreaProperties.ALLOW_CLICK, blockId);
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
        var allowed = AreaChecks.checkPropFor(targetArea, player, AreaProperties.ALLOW_ACTIVATE, blockId);
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
        var allowed = AreaChecks.checkPropFor(targetArea, p, AreaProperties.ALLOW_USE_ITEM, itemId);
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
        if (!AreaChecks.checkPropFor(targetArea, event.getEntity(), AreaProperties.ALLOW_TRAMPLE_FARMLAND, null)) {
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
        final var placer = event.getEntity();
        var allowed = AreaChecks.checkPropFor(targetArea, placer, AreaProperties.ALLOW_PLACE_BLOCK, blockId);
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
        if (!AreaChecks.checkPropFor(targetArea, null, AreaProperties.ALLOW_EXPLOSION, null)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void afterExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getExplosion().getPosition());
        if (!AreaChecks.checkPropFor(targetArea, null, AreaProperties.ALLOW_EXPLOSION_AFFECT_BLOCKS, null)) {
            event.getAffectedBlocks().clear();
        } else {
            for (var itr = event.getAffectedBlocks().iterator(); itr.hasNext();) {
                BlockPos affected = itr.next();
                final Area a = AreaManager.INSTANCE.findBy(event.getLevel(), affected);
                if (!AreaChecks.checkPropFor(a, null, AreaProperties.ALLOW_EXPLOSION_AFFECT_ENTITIES, null)) {
                    itr.remove();
                }
            }
        }
        if (!AreaChecks.checkPropFor(targetArea, null, AreaProperties.ALLOW_EXPLOSION_AFFECT_ENTITIES, null)) {
            event.getAffectedEntities().clear();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tryRide(EntityMountEvent event) {
        if (event.isMounting() && !event.getLevel().isClientSide) {
            var vehicle = event.getEntityBeingMounted();
            var entityId = ForgeRegistries.ENTITY_TYPES.getKey(vehicle.getType());
            var area = AreaManager.INSTANCE.findBy(event.getLevel(), vehicle.blockPosition());
            var rider = event.getEntityMounting();
            if (!AreaChecks.checkPropFor(area, rider, AreaProperties.ALLOW_RIDE, entityId)) {
                if (rider instanceof Player p) {
                    p.displayClientMessage(Component.translatable("area_control.notice.ride_disabled", vehicle.getDisplayName()), true);
                }
                event.setCanceled(true);
            }
        }
    }
}