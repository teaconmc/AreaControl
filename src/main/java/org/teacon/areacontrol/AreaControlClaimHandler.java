package org.teacon.areacontrol;

import java.util.ArrayDeque;
import java.util.WeakHashMap;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;

@Mod.EventBusSubscriber(modid = "area_control")
public final class AreaControlClaimHandler {

    static Item userClaimTool = Items.STICK;
    static Item adminTool = Items.TRIDENT;

    static WeakHashMap<PlayerEntity, ArrayDeque<BlockPos>> recordPos = new WeakHashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() == LogicalSide.SERVER) {
            final PlayerEntity player = event.getPlayer();
            if (event.getItemStack().getItem() == adminTool && PermissionAPI.hasPermission(player, "area_control.admin.inspect")) {
                player.sendStatusMessage(new StringTextComponent("AreaControl: Welcome back, administrator"), true);
            } else if (event.getItemStack().getItem() == userClaimTool) {
                final BlockPos clicked;
                recordPos.computeIfAbsent(player, p -> new ArrayDeque<>()).offerLast(clicked = event.getPos().toImmutable());
                player.sendStatusMessage(new StringTextComponent(String.format("AreaControl: Marked position x=%d, y=%d, z=%d ", clicked.getX(), clicked.getY(), clicked.getZ())), true);
            }
        }
    }
}