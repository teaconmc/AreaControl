package org.teacon.areacontrol;

import java.util.Random;

import org.teacon.areacontrol.api.Area;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;

public final class Util {

    private static final Random RAND = new Random();

    public static String nextRandomString() {
        // https://stackoverflow.com/questions/14622622/generating-a-random-hex-string-of-length-50-in-java-me-j2me#comment100639373_14623245
        // tldr: formatting to 8 digits of hexadecimal number, padding zeros at beginning
        return String.format("%08x", RAND.nextInt());
    }

    public static boolean isOpInServer(PlayerEntity player, MinecraftServer server) {
        return server.getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null;
    }

	public static Area createArea(String name, AxisAlignedBB box) {
        final Area a = new Area();
        a.name = name;
        a.minX = (int) box.minX;
        a.minY = (int) box.minY;
        a.minZ = (int) box.minZ;
        a.maxX = (int) box.maxX;
        a.maxY = (int) box.maxY;
        a.maxZ = (int) box.maxZ;
		return a;
	}
}