package org.teacon.areacontrol.mixin;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.LecternMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.AreaProperties;
import org.teacon.areacontrol.impl.Bridge;

@Mixin(LecternMenu.class)
public class LecternMenuMixin {

    @Shadow
    @Final
    private Container lectern;

    @Inject(method = "clickMenuButton", cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;mayBuild()Z"))
    private void checkPerm(Player p, int id, CallbackInfoReturnable<Boolean> cir) {
        var theLectern = Bridge.fromAnonymousContainer(this.lectern);
        if (theLectern == null) {
            return;
        }
        var area = AreaManager.INSTANCE.findBy(theLectern.getLevel(), theLectern.getBlockPos());
        if (!AreaProperties.getBool(area, "area.allow_take_out_item")) {
            p.displayClientMessage(new TextComponent("AreaControl: you cannot take items out of this!"), true);
            cir.setReturnValue(false);
        }
    }
}
