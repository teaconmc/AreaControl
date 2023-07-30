package org.teacon.areacontrol.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.teacon.areacontrol.AreaControlConfig;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.AreaProperties;
import org.teacon.areacontrol.impl.AreaChecks;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    private Level level;

    @Shadow
    public abstract BlockPos blockPosition();

    @Shadow
    public abstract EntityType<?> getType();

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
     */
    @Inject(method = "isInvulnerableTo", at = @At("TAIL"), cancellable = true)
    private void damageSrcCheck(DamageSource src, CallbackInfoReturnable<Boolean> cir) {
        if (!src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !cir.getReturnValueZ() && !this.level.isClientSide()) {
            var area = AreaManager.INSTANCE.findBy(this.level, this.blockPosition());
            boolean allow;
            Component deniedFeedback;
            var damageSrc = src.getEntity();
            if (Player.class.isInstance(this) && damageSrc instanceof Player) {
                allow = AreaChecks.checkPropFor(area, damageSrc, AreaProperties.ALLOW_PVP, null, AreaControlConfig.allowPvP);
                deniedFeedback = Component.translatable("area_control.notice.pvp_disabled", ObjectArrays.EMPTY_ARRAY);
            } else {
                var entityTypeRegName = ForgeRegistries.ENTITY_TYPES.getKey(this.getType());
                allow = AreaChecks.checkPropFor(area, damageSrc, AreaProperties.ALLOW_PVE, entityTypeRegName, AreaControlConfig.allowPvE);
                deniedFeedback = Component.translatable("area_control.notice.pve_disabled", ObjectArrays.EMPTY_ARRAY);
            }
            if (!allow) {
                if (src.getEntity() instanceof ServerPlayer srcPlayer) {
                    srcPlayer.displayClientMessage(deniedFeedback, true);
                }
                cir.setReturnValue(true);
            }
        }
    }
}
