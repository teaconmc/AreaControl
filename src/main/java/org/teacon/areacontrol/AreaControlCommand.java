package org.teacon.areacontrol;

import java.util.ArrayDeque;
import java.util.function.Predicate;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.server.permission.PermissionAPI;

public final class AreaControlCommand {

    public AreaControlCommand(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("ac")
                .redirect(dispatcher.register(Commands.literal("areacontrol")
                        .then(Commands.literal("about").executes(AreaControlCommand::about))
                        .then(Commands.literal("admin").executes(AreaControlCommand::admin))
                        .then(Commands.literal("claim").requires(check("area_control.command.claim")).executes(AreaControlCommand::claim))
                        .then(Commands.literal("current").executes(AreaControlCommand::displayCurrent))
                        .then(Commands.literal("list").executes(AreaControlCommand::list))
                        .then(Commands.literal("set").requires(check("area_control.command.set_property")).then(
                            Commands.argument("property", StringArgumentType.string()).then(
                                Commands.argument("value", StringArgumentType.greedyString())
                                    .executes(AreaControlCommand::setProperty)
                            )))
                        .then(Commands.literal("unclaim").requires(check("area_control.command.unclaim")).executes(AreaControlCommand::unclaim))
                        )
                )
        );
    }

    private Predicate<CommandSource> check(String permission) {
        return source -> {
            try {
                return PermissionAPI.hasPermission(source.asPlayer(), permission);
            } catch (CommandSyntaxException e) {
                return false;
            }
        };
    }

    private static int about(CommandContext<CommandSource> context) {
        context.getSource().sendFeedback(new StringTextComponent("AreaControl 0.1.4"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int admin(CommandContext<CommandSource> context) {
        context.getSource().sendFeedback(new StringTextComponent("WIP"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int claim(CommandContext<CommandSource> context) throws CommandSyntaxException {
        final CommandSource src = context.getSource();
        final ArrayDeque<BlockPos> recordPos = AreaControlClaimHandler.recordPos.get(src.asPlayer());
        if (recordPos != null && recordPos.size() >= 2) {
            final Area area = Util.createArea("Area " + Util.nextRandomString(), new AxisAlignedBB(recordPos.pop(), recordPos.pop()));
            final DimensionType dimType;
            area.dimension = (dimType = src.getWorld().getDimension().getType()).getRegistryName().toString();
            recordPos.clear();
            if (AreaManager.INSTANCE.add(area, dimType)) {
                src.sendFeedback(new StringTextComponent(String.format("Claim '%s' has been created from [%d, %d, %d] to [%d, %d, %d]", area.name, area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ)), true);
                return Command.SINGLE_SUCCESS;
            } else {
                src.sendErrorMessage(new StringTextComponent("Cannot claim the selected area because it overlaps another claimed area. Perhaps try somewhere else?"));
                return -2;
            }
        } else {
            src.sendErrorMessage(new StringTextComponent("Cannot determine what area you want to claim. Did you forget to select an area?"));
            return -1;
        }
        
    }

    private static int displayCurrent(CommandContext<CommandSource> context) throws CommandSyntaxException {
        final CommandSource src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getWorld().getDimension().getType(), new BlockPos(src.getPos()));
        if (area != AreaManager.INSTANCE.wildness) {
            final String name = AreaProperties.getString(area, "area.display_name", area.name);
            src.sendFeedback(new StringTextComponent(String.format("You are in an area named '%s'", name)), false);
        } else {
            src.sendFeedback(new StringTextComponent("You are in the wildness."), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int list(CommandContext<CommandSource> context) throws CommandSyntaxException {
        final CommandSource src = context.getSource();
        src.sendFeedback(new StringTextComponent("Currently known areas: "), false);
        for (Area a : AreaManager.INSTANCE.getKnownAreas()) {
            src.sendFeedback(new StringTextComponent(String.format("  - %s (from [%d, %d, %d] to [%d, %d, %d])", a.name, a.minX, a.minY, a.minZ, a.maxX, a.maxY, a.maxZ)), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int setProperty(CommandContext<CommandSource> context) throws CommandSyntaxException {
        final CommandSource src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getWorld().getDimension().getType(), new BlockPos(src.getPos()));
        if (area != AreaManager.INSTANCE.wildness) {
            final String prop = context.getArgument("property", String.class);
            final String value = context.getArgument("value", String.class);
            area.properties.put(prop, value);
            context.getSource().sendFeedback(new StringTextComponent(String.format("Area '%s''s property '%s' has been updated to '%s'", area.name, prop, value)), true);
            return Command.SINGLE_SUCCESS;
        } else {
            context.getSource().sendErrorMessage(new StringTextComponent("You are in the wildness. What were you thinking?"));
            return -1;
        }
    }

    private static int unclaim(CommandContext<CommandSource> context) throws CommandSyntaxException {
        final CommandSource src = context.getSource();
        final DimensionType dimType = src.getWorld().getDimension().getType();
        final Area area = AreaManager.INSTANCE.findBy(dimType, new BlockPos(src.getPos()));
        if (area != AreaManager.INSTANCE.wildness) {
            AreaManager.INSTANCE.remove(area, dimType);
            src.sendFeedback(new StringTextComponent(String.format("Claim '%s' (internal name %s, ranged from [%d, %d, %d] to [%d, %d, %d]) has been abandoned", AreaProperties.getString(area, "area.display_name", area.name), area.name, area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ)), true);
            return Command.SINGLE_SUCCESS;
        } else {
            context.getSource().sendErrorMessage(new StringTextComponent("You are in the wildness. Are you returning the wild nature to the nature itself?"));
            return -1;
        }
    }
}