package org.teacon.areacontrol;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
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
    public static void onCheckSpawn(EntityJoinWorldEvent event) {
        // Note from 3TUSK:
        // This event may be fired off-thread, and thus this handler may cause deadlock.
        // However, theoretically that could not happen:
        //   1. We avoid doing anything when the entity is loaded from disk, which should
        //      be the #1 source of off-thread spawning.
        //   2. No operations here is modifying underlying states of any objects, unless
        //      someone is overriding Entity.blockPosition (m_142538_).
        // Further, AreaManager.findBy is synchronized.
        if (!event.loadedFromDisk()) {
            final var entityRegId = event.getEntity().getType().getRegistryName();
            final var entitySpecificPerm = AreaProperties.ALLOW_SPAWN + "." + entityRegId;
            final var modSpecificPerm = AreaProperties.ALLOW_SPAWN + "." + (entityRegId == null ? "null" : entityRegId.getNamespace());
            final var pos = event.getEntity().blockPosition();
            final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld().dimension(), pos);
            var allow = true;
            if (AreaProperties.keyPresent(targetArea, entitySpecificPerm)) {
                allow = AreaProperties.getBool(targetArea, entitySpecificPerm);
            } else if (AreaProperties.keyPresent(targetArea, modSpecificPerm)) {
                allow = AreaProperties.getBool(targetArea, modSpecificPerm);
            } else {
                allow = AreaProperties.getBool(targetArea, AreaProperties.ALLOW_SPAWN);
            }
            if (!allow) {
                event.setCanceled(true);
            }
        }
    }

    //@SubscribeEvent(priority = EventPriority.HIGHEST) // TODO (3TUSK): Re-evaluate
    public static void onSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        final BlockPos spawnPos = new BlockPos(event.getX(), event.getY(), event.getZ());
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), spawnPos);
        if (!AreaProperties.getBool(targetArea, "area.allow_special_spawn")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getPlayer().level.isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_interact_entity_specific")
                && !AreaChecks.allow(event.getPlayer(), targetArea, AreaControlPermissions.BYPASS_INTERACT_ENTITY_SPECIFIC)) {
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
                && !AreaChecks.allow(event.getPlayer(), targetArea, AreaControlPermissions.BYPASS_INTERACT_ENTITY)) {
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
        final var block = event.getWorld().getBlockState(event.getPos());
        final var blockName = ForgeRegistries.BLOCKS.getKey(block.getBlock());
        final var blockSpecificPerm = blockName != null ? "area.allow_break_block." + blockName : null;
        final var modSpecificPerm = blockName != null ? "area.allow_break_block." + blockName.getNamespace() : null;
        var allowed = true;
        if (AreaProperties.keyPresent(targetArea, blockSpecificPerm)) {
            allowed = AreaProperties.getBool(targetArea, blockSpecificPerm);
        } else if (AreaProperties.keyPresent(targetArea, modSpecificPerm)) {
            allowed = AreaProperties.getBool(targetArea, modSpecificPerm);
        } else {
            allowed = AreaProperties.getBool(targetArea, "area.allow_break_block");
        }
        allowed |= AreaChecks.allow(p, targetArea, AreaControlPermissions.BYPASS_BREAK_BLOCK);
        if (!allowed) {
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
        final var block = event.getWorld().getBlockState(event.getPos());
        final var blockName = ForgeRegistries.BLOCKS.getKey(block.getBlock());
        final var blockSpecificPerm = blockName != null ? "area.allow_click_block." + blockName : null;
        final var modSpecificPerm = blockName != null ? "area.allow_click_block." + blockName.getNamespace() : null;
        var allowed = true;
        if (AreaProperties.keyPresent(targetArea, blockSpecificPerm)) {
            allowed = AreaProperties.getBool(targetArea, blockSpecificPerm);
        } else if (AreaProperties.keyPresent(targetArea, modSpecificPerm)) {
            allowed = AreaProperties.getBool(targetArea, modSpecificPerm);
        } else {
            allowed = AreaProperties.getBool(targetArea, "area.allow_click_block");
        }
        allowed |= AreaChecks.allow(p, targetArea, AreaControlPermissions.BYPASS_BREAK_BLOCK);
        if (!allowed) {
            p.displayClientMessage(new TranslatableComponent("area_control.notice.click_block_disabled", ObjectArrays.EMPTY_ARRAY), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onActivateBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        final var player = event.getPlayer();
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        final var block = event.getWorld().getBlockState(event.getPos());
        final var blockName = ForgeRegistries.BLOCKS.getKey(block.getBlock());
        final var blockSpecificPerm = blockName != null ? "area.allow_activate_block." + blockName : null;
        final var modSpecificPerm = blockName != null ? "area.allow_activate_block." + blockName.getNamespace() : null;
        var allowed = true;
        if (AreaProperties.keyPresent(targetArea, blockSpecificPerm)) {
            allowed = AreaProperties.getBool(targetArea, blockSpecificPerm);
        } else if (AreaProperties.keyPresent(targetArea, modSpecificPerm)) {
            allowed = AreaProperties.getBool(targetArea, modSpecificPerm);
        } else {
            allowed = AreaProperties.getBool(targetArea, "area.allow_activate_block");
        }
        allowed |= AreaChecks.allow(player, targetArea, AreaControlPermissions.BYPASS_BREAK_BLOCK);
        if (!allowed) {
            player.displayClientMessage(new TranslatableComponent("area_control.notice.activate_block_disabled", ObjectArrays.EMPTY_ARRAY), true);
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
        final var itemName = event.getItemStack().getItem().getRegistryName();
        final var itemSpecificPerm = itemName != null ? "area.allow_use_item." + itemName : null;
        final var modSpecificPerm = itemName != null ? "area.allow_use_item." + itemName.getNamespace() : null;
        var allow = true;
        if (AreaProperties.keyPresent(targetArea, itemSpecificPerm)) {
            allow = AreaProperties.getBool(targetArea, itemSpecificPerm);
        } else if (AreaProperties.keyPresent(targetArea, modSpecificPerm)) {
            allow = AreaProperties.getBool(targetArea, modSpecificPerm);
        } else {
            allow = AreaProperties.getBool(targetArea, "area.allow_use_item");
        }
        allow |= AreaChecks.allow(p, targetArea, AreaControlPermissions.BYPASS_USE_ITEM);
        if (!allow) {
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
        final var block = event.getWorld().getBlockState(event.getPos());
        final var blockName = ForgeRegistries.BLOCKS.getKey(block.getBlock());
        final var blockSpecificPerm = blockName != null ? "area.allow_place_block." + blockName : null;
        final var modSpecificPerm = blockName != null ? "area.allow_place_block." + blockName.getNamespace() : null;
        var allowed = true;
        if (AreaProperties.keyPresent(targetArea, blockSpecificPerm)) {
            allowed = AreaProperties.getBool(targetArea, blockSpecificPerm);
        } else if (AreaProperties.keyPresent(targetArea, modSpecificPerm)) {
            allowed = AreaProperties.getBool(targetArea, modSpecificPerm);
        } else {
            allowed = AreaProperties.getBool(targetArea, "area.allow_place_block");
        }
        final var placer = event.getEntity();
        if (placer instanceof ServerPlayer p) {
            allowed |= AreaChecks.allow(p, targetArea, AreaControlPermissions.BYPASS_BREAK_BLOCK);
        }
        if (!allowed) {
            // TODO Client will falsely report item being consumed; however it will return to normal if you click again in inventory GUI
            event.setCanceled(true);
            if (placer instanceof ServerPlayer p) {
                p.displayClientMessage(new TranslatableComponent("area_control.notice.place_block_disabled", ObjectArrays.EMPTY_ARRAY), true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeExplosion(ExplosionEvent.Start event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), new BlockPos(event.getExplosion().getPosition()));
        if (!AreaProperties.getBool(targetArea, "area.allow_explosion")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void afterExplosion(ExplosionEvent.Detonate event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), new BlockPos(event.getExplosion().getPosition()));
        if (!AreaProperties.getBool(targetArea, "area.allow_explosion_affect_blocks")) {
            event.getAffectedBlocks().clear();
        } else {
            for (var itr = event.getAffectedBlocks().iterator(); itr.hasNext();) {
                BlockPos affected = itr.next();
                final Area a = AreaManager.INSTANCE.findBy(event.getWorld(), affected);
                if (!AreaProperties.getBool(a, "area.allow_explosion_affect_blocks")) {
                    itr.remove();
                }
            }
        }
        if (!AreaProperties.getBool(targetArea, "area.allow_explosion_affect_entities")) {
            event.getAffectedEntities().clear();
        }
    }
}