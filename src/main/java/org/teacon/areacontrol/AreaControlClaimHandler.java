package org.teacon.areacontrol;

import javax.annotation.Nonnull;
import java.util.WeakHashMap;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

@Mod.EventBusSubscriber(modid = "area_control")
public final class AreaControlClaimHandler {

    // TODO Configurable
    static Item userClaimTool = Items.STICK;
    static Item adminTool = Items.TRIDENT;

    private static final WeakHashMap<PlayerEntity, Pair<BlockPos, BlockPos>> records = new WeakHashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() == LogicalSide.SERVER) {
            final PlayerEntity player = event.getPlayer();
            if (event.getItemStack().getItem() == adminTool && PermissionAPI.hasPermission(player, "area_control.admin.inspect")) {
                player.sendStatusMessage(new StringTextComponent("AreaControl: Welcome back, administrator"), true);
            } else if (event.getItemStack().getItem() == userClaimTool && PermissionAPI.hasPermission(player, "area_control.command.mark")) {
                final BlockPos clicked  = event.getPos();
                pushRecord(player, clicked.toImmutable());
                player.sendStatusMessage(new StringTextComponent(String.format("AreaControl: Marked position [%s]", clicked.getCoordinatesAsString())), true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() == userClaimTool) {
            final PlayerEntity player = event.getPlayer();
            if (player != null && PermissionAPI.hasPermission(player, "area_control.command.mark")) {
                final Pair<BlockPos, BlockPos> record = records.getOrDefault(player, ImmutablePair.nullPair());
                final BlockPos left = record.getLeft(), right = record.getRight();
                final String leftString = left == null ? "undefined" : String.format("[%s]", left.getCoordinatesAsString());
                final String rightString = right == null ? "undefined" : String.format("[%s]", right.getCoordinatesAsString());
                event.getToolTip().add(new StringTextComponent("AreaControl: Marked positions"));
                event.getToolTip().add(new StringTextComponent(String.format("From: %s, To: %s", leftString, rightString)));
            }
        }
    }

    static Pair<BlockPos, BlockPos> popRecord(@Nonnull PlayerEntity player) {
        return records.containsKey(player) && records.get(player).getLeft() != null ? records.remove(player) : null;
    }

    static void pushRecord(@Nonnull PlayerEntity player, @Nonnull BlockPos clicked) {
        records.compute(player, (p, old) -> Pair.of(old == null ? null : old.getRight(), clicked));
    }
}