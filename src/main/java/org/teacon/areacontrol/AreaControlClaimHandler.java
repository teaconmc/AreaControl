package org.teacon.areacontrol;

import javax.annotation.Nonnull;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;

@Mod.EventBusSubscriber(modid = "area_control")
public final class AreaControlClaimHandler {

    // TODO Configurable
    static Item userClaimTool = Items.STICK;
    static Item adminTool = Items.TRIDENT;

    private static final WeakHashMap<Player, RectangleRegion> records = new WeakHashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() == LogicalSide.SERVER) {
            final var player = (ServerPlayer) event.getPlayer();
            if (event.getItemStack().getItem() == adminTool && PermissionAPI.getPermission(player, AreaControlPermissions.INSPECT)) {
                player.displayClientMessage(new TranslatableComponent("area_control.admin.welcome", player.getDisplayName()), true);
            } else if (event.getItemStack().getItem() == userClaimTool && PermissionAPI.getPermission(player, AreaControlPermissions.MARK_AREA)) {
                final BlockPos clicked  = event.getPos();
                pushRecord(player, clicked.immutable());
                player.displayClientMessage(new TranslatableComponent("area_control.claim.marked", Util.toGreenText(clicked)), true);
            }
        }
    }

    static RectangleRegion popRecord(@Nonnull Player player) {
        return records.containsKey(player) && records.get(player).start != null ? records.remove(player) : null;
    }

    static void pushRecord(@Nonnull Player player, @Nonnull BlockPos clicked) {
        records.compute(player, (p, old) -> new RectangleRegion(old == null ? null : old.end, clicked));
    }

    record RectangleRegion(BlockPos start, BlockPos end) {}
}