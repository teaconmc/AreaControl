package org.teacon.areacontrol;

import java.util.ArrayDeque;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import org.teacon.areacontrol.api.Area;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;

public final class AreaControlCommand {

    public AreaControlCommand(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("ac")
                .redirect(dispatcher.register(Commands.literal("areacontrol")
                        .then(Commands.literal("about").executes(AreaControlCommand::about))
                        .then(Commands.literal("admin").executes(AreaControlCommand::admin))
                        .then(Commands.literal("claim").executes(AreaControlCommand::claim))
                        .then(Commands.literal("list").executes(AreaControlCommand::list))
                        .then(Commands.literal("set").then(
                            Commands.argument("property", StringArgumentType.string()).then(
                                Commands.argument("value", StringArgumentType.greedyString())
                                    .executes(AreaControlCommand::setProperty)
                            )))
                        )
                )
        );
    }

    private static int about(CommandContext<CommandSource> context) {
        context.getSource().sendFeedback(new StringTextComponent("AreaControl 0.1.0"), false);
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
            final Area area = Util.createArea("Unnamed area " + Util.nextRandomString(), new AxisAlignedBB(recordPos.pop(), recordPos.pop()));
            recordPos.clear();
            if (AreaManager.INSTANCE.add(area)) {
                src.sendFeedback(new StringTextComponent(String.format("Claim '%s' has been created from [%d, %d, %d] to [%d, %d, %d]", area.name, area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ)), true);
                return Command.SINGLE_SUCCESS;
            } else {
                src.sendFeedback(new StringTextComponent("Cannot claim the selected area because it overlaps another claimed area. Perhaps try somewhere else?"), false);
                return -2;
            }
        } else {
            src.sendFeedback(new StringTextComponent("Cannot determine what area you want to claim. Did you forget to select an area?"), false);
            return -1;
        }
        
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
        final Area area = AreaManager.INSTANCE.findBy(new BlockPos(context.getSource().getPos()));
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
}