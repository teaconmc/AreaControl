package org.teacon.areacontrol;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;
import org.teacon.areacontrol.impl.AreaChecks;

@EventBusSubscriber(modid = "area_control")
public final class AreaControlEventHandlers {

    /*
     * This is probably one of the most universal ways to prevent edge cases such as
     * preventing an arrow from unauthorized player to shoot down vanilla item frame.
     * Vanilla Item Frames are not LivingEntity so LivingAttackEvent cannot catch this
     * case.
     * When arrows hit vanilla Item Frames, the "damage" does not list Player as
     * direct source (lists as indirect source, a.k.a. "true source"), so
     * AttackEntityEvent cannot catch this case, either.
     * Use Entity.hurt as an injecting target has an issue: anyone who extends from
     * Entity can override that method and make our injection in vain, and thus requires
     * us to special-casing every single edge case.
     * All of these leave isInvulnerableTo a most-probable choice. If your entity can
     * subject to some form of attack, you most likely will need to call this method.
     * This is the case for vanilla entities.
     *
     * NeoForge update: NeoForge has an event at this spot, so we no longer need to mix-in
     * on our own. - 3TUSK
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void vulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        DamageSource src = event.getSource();
        Entity attackTarget = event.getEntity();
        Level level = attackTarget.level();
        boolean originalInvulnerability = event.getOriginalInvulnerability();
        if (!src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !originalInvulnerability && !level.isClientSide()) {
            var area = AreaManager.INSTANCE.findBy(level, attackTarget.blockPosition());
            boolean allow;
            Component deniedFeedback;
            var damageSrc = src.getEntity();
            if (attackTarget instanceof Player && damageSrc instanceof Player) {
                allow = AreaChecks.checkPropFor(area, damageSrc, AreaProperties.ALLOW_PVP, null, AreaControlConfig.allowPvP);
                deniedFeedback = Component.translatable("area_control.notice.pvp_disabled", ObjectArrays.EMPTY_ARRAY);
            } else {
                var entityTypeRegName = BuiltInRegistries.ENTITY_TYPE.getKey(attackTarget.getType());
                allow = AreaChecks.checkPropFor(area, damageSrc, AreaProperties.ALLOW_PVE, entityTypeRegName, AreaControlConfig.allowPvE);
                deniedFeedback = Component.translatable("area_control.notice.pve_disabled", ObjectArrays.EMPTY_ARRAY);
            }
            if (!allow) {
                if (src.getEntity() instanceof ServerPlayer srcPlayer) {
                    srcPlayer.displayClientMessage(deniedFeedback, true);
                }
                event.setInvulnerable(true);
            }
        }
    }

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
            final var entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityInQuestion.getType());
            if (!AreaChecks.checkPropFor(targetArea, null, AreaProperties.ALLOW_SPAWN, entityId, AreaControlConfig.allowSpawnEntity)) {
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
        final var targetTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(targetType);
        if (!AreaChecks.checkPropFor(targetArea, event.getEntity(), AreaProperties.ALLOW_INTERACT_ENTITY, targetTypeId, AreaControlConfig.allowInteractEntity)) {
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
        final var targetTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(targetType);
        if (!AreaChecks.checkPropFor(targetArea, event.getEntity(), AreaProperties.ALLOW_INTERACT_ENTITY, targetTypeId, AreaControlConfig.allowInteractEntity)) {
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
        final var blockId = BuiltInRegistries.BLOCK.getKey(block.getBlock());
        var allowed = AreaChecks.checkPropFor(targetArea, p, AreaProperties.ALLOW_BREAK, blockId, AreaControlConfig.allowBreakBlock);
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
        final var blockId = BuiltInRegistries.BLOCK.getKey(block.getBlock());
        var allowed = AreaChecks.checkPropFor(targetArea, p, AreaProperties.ALLOW_CLICK, blockId, AreaControlConfig.allowClickBlock);
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
        final var blockId = BuiltInRegistries.BLOCK.getKey(block.getBlock());
        var allowed = AreaChecks.checkPropFor(targetArea, player, AreaProperties.ALLOW_ACTIVATE, blockId, AreaControlConfig.allowActivateBlock);
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
        final var itemId = BuiltInRegistries.ITEM.getKey(theItem);
        var allowed = AreaChecks.checkPropFor(targetArea, p, AreaProperties.ALLOW_USE_ITEM, itemId, AreaControlConfig.allowUseItem);
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
        if (!AreaChecks.checkPropFor(targetArea, event.getEntity(), AreaProperties.ALLOW_TRAMPLE_FARMLAND, null, null)) {
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
        final var blockId = BuiltInRegistries.BLOCK.getKey(block.getBlock());
        final var placer = event.getEntity();
        var allowed = AreaChecks.checkPropFor(targetArea, placer, AreaProperties.ALLOW_PLACE_BLOCK, blockId, AreaControlConfig.allowPlaceBlock);
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
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getExplosion().center());
        if (!AreaChecks.checkPropFor(targetArea, null, AreaProperties.ALLOW_EXPLOSION, null, null)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void afterExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getExplosion().center());
        if (!AreaChecks.checkPropFor(targetArea, null, AreaProperties.ALLOW_EXPLOSION_AFFECT_BLOCKS, null, null)) {
            event.getAffectedBlocks().clear();
        } else {
            for (var itr = event.getAffectedBlocks().iterator(); itr.hasNext(); ) {
                BlockPos affected = itr.next();
                final Area a = AreaManager.INSTANCE.findBy(event.getLevel(), affected);
                if (!AreaChecks.checkPropFor(a, null, AreaProperties.ALLOW_EXPLOSION_AFFECT_ENTITIES, null, null)) {
                    itr.remove();
                }
            }
        }
        if (!AreaChecks.checkPropFor(targetArea, null, AreaProperties.ALLOW_EXPLOSION_AFFECT_ENTITIES, null, null)) {
            event.getAffectedEntities().clear();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tryRide(EntityMountEvent event) {
        if (event.isMounting() && !event.getLevel().isClientSide) {
            var vehicle = event.getEntityBeingMounted();
            var entityId = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
            var area = AreaManager.INSTANCE.findBy(event.getLevel(), vehicle.blockPosition());
            var rider = event.getEntityMounting();
            if (!AreaChecks.checkPropFor(area, rider, AreaProperties.ALLOW_RIDE, entityId, AreaControlConfig.allowRideEntity)) {
                if (rider instanceof Player p) {
                    p.displayClientMessage(Component.translatable("area_control.notice.ride_disabled", vehicle.getDisplayName()), true);
                }
                event.setCanceled(true);
            }
        }
    }
}