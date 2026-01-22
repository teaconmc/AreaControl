package org.teacon.areacontrol;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import org.teacon.areacontrol.impl.AreaChecks;

import javax.annotation.Nonnull;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = "area_control")
public final class AreaControlClaimHandler {

    private static final WeakHashMap<Player, RectangleRegion> records = new WeakHashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() == LogicalSide.SERVER) {
            final var player = (ServerPlayer) event.getEntity();
            final var areaClaimTool = BuiltInRegistries.ITEM.get(Identifier.parse(AreaControlConfig.areaClaimTool.get()));
            // TODO [3TUSK]: 这对吗？感觉不对劲啊？
            if (areaClaimTool.isPresent() && event.getItemStack().getItem() == areaClaimTool.get().value()) {
                var currentArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getPos());
                if (AreaChecks.isACtrlAreaBuilder(player, currentArea) || PermissionAPI.getPermission(player, AreaControlPermissions.AC_CLAIMER)) {
                    final BlockPos clicked = event.getPos();
                    pushRecord(player, event.getLevel().dimension(), clicked.immutable());
                    player.displayClientMessage(Component.translatable("area_control.claim.marked", Util.toGreenText(clicked)), true);
                }
            }
        }
    }

    static RectangleRegion popRecord(@Nonnull ServerPlayer player) {
        return records.containsKey(player) && records.get(player).start != null ? records.remove(player) : null;
    }

    static void pushRecord(@Nonnull ServerPlayer player, ResourceKey<Level> dimension, @Nonnull BlockPos clicked) {
        var selection = records.compute(player, (p, old) -> new RectangleRegion(old == null ? null : old.end, GlobalPos.of(dimension, clicked)));
        AreaControlPlayerTracker.INSTANCE.sendCurrentSelectionToClient(player, selection);
    }

    public record RectangleRegion(GlobalPos start, GlobalPos end) {}
}