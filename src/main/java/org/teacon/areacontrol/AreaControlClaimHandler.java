package org.teacon.areacontrol;

import javax.annotation.Nonnull;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.permission.PermissionAPI;
import org.teacon.areacontrol.impl.AreaChecks;

@Mod.EventBusSubscriber(modid = "area_control")
public final class AreaControlClaimHandler {

    private static final WeakHashMap<Player, RectangleRegion> records = new WeakHashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() == LogicalSide.SERVER) {
            final var player = (ServerPlayer) event.getEntity();
            final var areaClaimTool = ForgeRegistries.ITEMS.getValue(new ResourceLocation(AreaControlConfig.areaClaimTool.get()));
            if (areaClaimTool != Items.AIR && event.getItemStack().getItem() == areaClaimTool) {
                var currentArea = AreaManager.INSTANCE.findBy(event.getLevel(), event.getPos());
                if (AreaChecks.isACtrlAreaBuilder(player, currentArea) || PermissionAPI.getPermission(player, AreaControlPermissions.AC_CLAIMER)) {
                    final BlockPos clicked = event.getPos();
                    pushRecord(player, clicked.immutable());
                    player.displayClientMessage(Component.translatable("area_control.claim.marked", Util.toGreenText(clicked)), true);
                }
            }
        }
    }

    static RectangleRegion popRecord(@Nonnull ServerPlayer player) {
        return records.containsKey(player) && records.get(player).start != null ? records.remove(player) : null;
    }

    static void pushRecord(@Nonnull ServerPlayer player, @Nonnull BlockPos clicked) {
        var selection = records.compute(player, (p, old) -> new RectangleRegion(old == null ? null : old.end, clicked));
        AreaControlPlayerTracker.INSTANCE.sendCurrentSelectionToClient(player, selection);
    }

    record RectangleRegion(BlockPos start, BlockPos end) {}
}