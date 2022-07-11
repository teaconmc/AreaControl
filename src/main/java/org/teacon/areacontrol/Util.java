package org.teacon.areacontrol;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.teacon.areacontrol.api.Area;

import java.util.Random;
import java.util.UUID;

public final class Util {

    public static final UUID SYSTEM = new UUID(0L, 0L);

    private static final Random RAND = new Random();

    public static String nextRandomString() {
        // https://stackoverflow.com/questions/14622622/generating-a-random-hex-string-of-length-50-in-java-me-j2me#comment100639373_14623245
        // tldr: formatting to 8 digits of hexadecimal number, padding zeros at
        // beginning
        return String.format("%08x", RAND.nextInt());
    }

    public static boolean isOpInServer(Player player, MinecraftServer server) {
        return server.getPlayerList().getOps().get(player.getGameProfile()) != null;
    }

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
                        .setUnderlined(Boolean.TRUE)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("area_control.owner.aka", p.getDisplayName())))
                );
            }
        }
        return ownerName;
    }
}