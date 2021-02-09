package org.teacon.areacontrol;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.commons.lang3.tuple.Pair;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.function.Predicate;

public final class AreaControlCommand {

    public AreaControlCommand(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("ac")
                .redirect(dispatcher.register(Commands.literal("areacontrol")
                        .then(Commands.literal("about").executes(AreaControlCommand::about))
                        .then(Commands.literal("admin").executes(AreaControlCommand::admin))
                        .then(Commands.literal("claim").requires(check("area_control.command.claim")).executes(AreaControlCommand::claim))
                        .then(Commands.literal("current").executes(AreaControlCommand::displayCurrent))
                        .then(Commands.literal("list").executes(AreaControlCommand::list))
                        .then(Commands.literal("mark").requires(check("area_control.command.mark")).then(
                                Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(AreaControlCommand::mark)))
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
            if (source.source instanceof ServerPlayerEntity) {
                return PermissionAPI.hasPermission((ServerPlayerEntity) source.source, permission);
            }
            return source.hasPermissionLevel(2);
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
        final Pair<BlockPos, BlockPos> recordPos = AreaControlClaimHandler.popRecord(src.asPlayer());
        if (recordPos != null) {
            final Area area = Util.createArea("Area " + Util.nextRandomString(), new AxisAlignedBB(recordPos.getLeft(), recordPos.getRight()));
            final RegistryKey<World> worldIndex = src.getWorld().getDimensionKey();
            if (AreaManager.INSTANCE.add(area, worldIndex)) {
                src.sendFeedback(new StringTextComponent(String.format("Claim '%s' has been created ", area.name)).append(Util.toGreenText(area)), true);
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
        final Area area = AreaManager.INSTANCE.findBy(src.getWorld().getDimensionKey(), new BlockPos(src.getPos()));
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
            src.sendFeedback(new StringTextComponent(String.format("  - %s (", a.name)).append(Util.toGreenText(a)).appendString(")"), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int mark(CommandContext<CommandSource> context) throws CommandSyntaxException {
        BlockPos marked = new BlockPos(Vec3Argument.getVec3(context, "pos"));
        AreaControlClaimHandler.pushRecord(context.getSource().asPlayer(), marked);
        context.getSource().sendFeedback(new StringTextComponent("AreaControl: Marked position ").append(Util.toGreenText(marked)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setProperty(CommandContext<CommandSource> context) throws CommandSyntaxException {
        final CommandSource src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getWorld().getDimensionKey(), new BlockPos(src.getPos()));
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
        final RegistryKey<World> worldIndex = src.getWorld().getDimensionKey();
        final Area area = AreaManager.INSTANCE.findBy(worldIndex, new BlockPos(src.getPos()));
        if (area != AreaManager.INSTANCE.wildness) {
            AreaManager.INSTANCE.remove(area, worldIndex);
            src.sendFeedback(new StringTextComponent(String.format("Claim '%s' (internal name %s, ranged ",
                    AreaProperties.getString(area, "area.display_name", area.name), area.name))
                    .append(Util.toGreenText(area)).appendString(") has been abandoned"), true);
            return Command.SINGLE_SUCCESS;
        } else {
            context.getSource().sendErrorMessage(new StringTextComponent("You are in the wildness. Are you returning the wild nature to the nature itself?"));
            return -1;
        }
    }
}