package org.teacon.areacontrol;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import org.teacon.areacontrol.api.Area;

import java.util.UUID;
import java.util.stream.Stream;

public final class Util {

    public static Component toGreenText(BlockPos pos) {
        return Component.translatable("area_control.claim.pos", pos.getX(), pos.getY(), pos.getZ())
                .withStyle(ChatFormatting.GREEN);
    }

    public static Component toGreenText(Area area) {
        var min = Component.translatable("area_control.claim.pos", area.minX, area.minY, area.minZ)
                .withStyle(ChatFormatting.GREEN);
        var max = Component.translatable("area_control.claim.pos", area.maxX, area.maxY, area.maxZ)
                .withStyle(ChatFormatting.GREEN);
        return Component.translatable("area_control.claim.range", min, max);
    }

    public static Component describe(Area area) {
        return describe(area, null);
    }
    public static Component describe(Area area, LevelAccessor level) {
        var midX = (area.minX + area.maxX) / 2;
        var midY = (area.minY + area.maxY) / 2;
        var midZ = (area.minZ + area.maxZ) / 2;
        if (level != null) {
            midY = level.getHeight(Heightmap.Types.WORLD_SURFACE, midX, midZ);
        }
        return Component.translatable("area_control.claim.detail",
                Component.literal(area.name).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("area_control.claim.current.copy_name")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, area.name))),
                Component.literal(area.dimension),
                Util.toGreenText(area),
                Component.translatable("area_control.claim.nearby.detail.go_there").setStyle(
                        Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/execute in " + area.dimension + " run tp @s " + midX + " " + midY + " " + midZ))
                                .withColor(ChatFormatting.DARK_AQUA)
                ),
                Component.translatable("area_control.claim.current.uuid")
                        .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, area.uid.toString()))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("area_control.claim.current.copy_uuid")))
                                .withColor(ChatFormatting.DARK_AQUA)));
    }

    public static Stream<BlockPos> verticesOf(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return Stream.of(
                new BlockPos(minX, minY, minZ),
                new BlockPos(minX, minY, maxZ),
                new BlockPos(minX, maxY, minZ),
                new BlockPos(minX, maxY, maxZ),
                new BlockPos(maxX, minY, minZ),
                new BlockPos(maxX, minY, maxZ),
                new BlockPos(maxX, maxY, minZ),
                new BlockPos(maxX, maxY, maxZ)
        );
    }


    public static Area createArea(BlockPos start, BlockPos end) {
        final Area a = new Area();
        a.minX = Math.min(start.getX(), end.getX());
        a.minY = Math.min(start.getY(), end.getY());
        a.minZ = Math.min(start.getZ(), end.getZ());
        a.maxX = Math.max(start.getX(), end.getX());
        a.maxY = Math.max(start.getY(), end.getY());
        a.maxZ = Math.max(start.getZ(), end.getZ());
        return a;
    }

    public static boolean isInsideArea(Area area, int x, int y, int z) {
        return area.minX <= x && x <= area.maxX && area.minY <= y && y <= area.maxY && area.minZ < z && z < area.maxZ;
    }

    public static boolean isInsideArea(Area area, double x, double y, double z) {
        return area.minX <= x && x <= area.maxX && area.minY <= y && y <= area.maxY && area.minZ < z && z < area.maxZ;
    }

    public static Component getOwnerName(Area area, GameProfileCache profileCache, PlayerList onlinePlayers) {
        if (area.owners.isEmpty()) {
            return Component.literal("暂缺"); // FIXME Translatable
        } else {
            final UUID owner = area.owners.iterator().next();
            var oneName = getPlayerDisplayName(owner, profileCache, onlinePlayers);
            return area.owners.size() == 1 ? oneName : Component.translatable("area_control.claim.owner.multiple", oneName);
        }
    }

    public static Component getPlayerDisplayName(UUID playerUid, GameProfileCache profileCache, PlayerList onlinePlayers) {
        if (profileCache != null) {
            var maybeProfile = profileCache.get(playerUid);
            if (maybeProfile.isPresent()) {
                return getOwnerName(maybeProfile.get(), onlinePlayers);
            }
        }
        return Component.literal(playerUid.toString());
    }

    public static Component getOwnerName(GameProfile profile, PlayerList onlinePlayers) {
        final var ownerName = Component.literal(profile.getName());
        if (onlinePlayers != null) {
            Player p = onlinePlayers.getPlayer(profile.getId());
            if (p != null) {
                ownerName.setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)
                        .withUnderlined(Boolean.TRUE)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("area_control.owner.aka", p.getDisplayName())))
                );
            }
        }
        return ownerName;
    }
}