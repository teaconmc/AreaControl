package org.teacon.areacontrol;

import java.util.Random;

import org.teacon.areacontrol.api.Area;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public final class Util {

    private static final Random RAND = new Random();

    public static String nextRandomString() {
        // https://stackoverflow.com/questions/14622622/generating-a-random-hex-string-of-length-50-in-java-me-j2me#comment100639373_14623245
        // tldr: formatting to 8 digits of hexadecimal number, padding zeros at beginning
        return String.format("%08x", RAND.nextInt());
    }

    public static boolean isOpInServer(PlayerEntity player, MinecraftServer server) {
        return server.getPlayerList().getOps().get(player.getGameProfile()) != null;
    }

    public static IFormattableTextComponent toGreenText(BlockPos pos) {
        return new TranslationTextComponent("area_control.claim.pos", pos.getX(), pos.getY(), pos.getZ())
        		.withStyle(TextFormatting.GREEN);
    }

    public static IFormattableTextComponent toGreenText(Area area) {
        ITextComponent min = new TranslationTextComponent("area_control.claim.pos", area.minX, area.minY, area.minZ).withStyle(TextFormatting.GREEN);
        ITextComponent max = new TranslationTextComponent("area_control.claim.pos", area.maxX, area.maxY, area.maxZ).withStyle(TextFormatting.GREEN);
        return new TranslationTextComponent("area_control.claim.range", min, max);
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