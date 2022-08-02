package org.teacon.areacontrol;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import org.teacon.areacontrol.api.Area;

import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

public final class Util {

    public static final UUID SYSTEM = new UUID(0L, 0L);

    private static final Random RAND = new Random();

    public static Component toGreenText(BlockPos pos) {
        return new TranslatableComponent("area_control.claim.pos", pos.getX(), pos.getY(), pos.getZ())
                .withStyle(ChatFormatting.GREEN);
    }

    public static Component toGreenText(Area area) {
        var min = new TranslatableComponent("area_control.claim.pos", area.minX, area.minY, area.minZ)
                .withStyle(ChatFormatting.GREEN);
        var max = new TranslatableComponent("area_control.claim.pos", area.maxX, area.maxY, area.maxZ)
                .withStyle(ChatFormatting.GREEN);
        return new TranslatableComponent("area_control.claim.range", min, max);
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
        return new TranslatableComponent("area_control.claim.detail",
                new TextComponent(area.name).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent(area.uid.toString())))),
                new TextComponent(area.dimension),
                Util.toGreenText(area),
                new TranslatableComponent("area_control.claim.nearby.detail.go_there").setStyle(
                        Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("chat.coordinates.tooltip")))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/execute in " + area.dimension + " run tp @s " + midX + " " + midY + " " + midZ))
                                .withColor(ChatFormatting.DARK_AQUA)
                ));
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
        final UUID owner = area.owner;
        return owner == null || SYSTEM.equals(owner) ? new TextComponent("System") : getPlayerDisplayName(owner, profileCache, onlinePlayers);
    }

    public static Component getPlayerDisplayName(UUID playerUid, GameProfileCache profileCache, PlayerList onlinePlayers) {
        if (profileCache != null) {
            var maybeProfile = profileCache.get(playerUid);
            if (maybeProfile.isPresent()) {
                return getOwnerName(maybeProfile.get(), onlinePlayers);
            }
        }
        return new TextComponent(playerUid.toString());
    }

    public static Component getOwnerName(GameProfile profile, PlayerList onlinePlayers) {
        final var ownerName = new TextComponent(profile.getName());
        if (onlinePlayers != null) {
            Player p = onlinePlayers.getPlayer(profile.getId());
            if (p != null) {
                ownerName.setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)
                        .withUnderlined(Boolean.TRUE)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("area_control.owner.aka", p.getDisplayName())))
                );
            }
        }
        return ownerName;
    }
}