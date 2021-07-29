package org.teacon.areacontrol;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.commons.lang3.tuple.Pair;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.Objects;
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
            return source.hasPermission(2);
        };
    }

    private static int about(CommandContext<CommandSource> context) {
        context.getSource().sendSuccess(new StringTextComponent("AreaControl 0.1.4"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int admin(CommandContext<CommandSource> context) {
        context.getSource().sendSuccess(new StringTextComponent("WIP"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int claim(CommandContext<CommandSource> context) throws CommandSyntaxException {
        final CommandSource src = context.getSource();
        final Pair<BlockPos, BlockPos> recordPos = AreaControlClaimHandler.popRecord(src.getPlayerOrException());
        if (recordPos != null) {
            final Area area = Util.createArea("Area " + Util.nextRandomString(), new AxisAlignedBB(recordPos.getLeft(), recordPos.getRight()));
            final RegistryKey<World> worldIndex = src.getLevel().dimension();
            if (AreaManager.INSTANCE.add(area, worldIndex)) {
                src.sendSuccess(new TranslationTextComponent("area_control.claim.created", area.name, Util.toGreenText(area)), true);
                return Command.SINGLE_SUCCESS;
            } else {
                src.sendFailure(new StringTextComponent("Cannot claim the selected area because it overlaps another claimed area. Perhaps try somewhere else?"));
                return -2;
            }
        } else {
            src.sendFailure(new StringTextComponent("Cannot determine what area you want to claim. Did you forget to select an area?"));
            return -1;
        }
        
    }

    private static int displayCurrent(CommandContext<CommandSource> context) throws CommandSyntaxException {
        final CommandSource src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), new BlockPos(src.getPosition()));
        if (area != AreaManager.INSTANCE.wildness) {
            final String name = AreaProperties.getString(area, "area.display_name", area.name);
            src.sendSuccess(new TranslationTextComponent("area_control.claim.current", name), true);
        } else {
            src.sendSuccess(new TranslationTextComponent("area_control.claim.current.wildness"), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int list(CommandContext<CommandSource> context) throws CommandSyntaxException {
        final CommandSource src = context.getSource();
        src.sendSuccess(new TranslationTextComponent("area_control.claim.list", ObjectArrays.EMPTY_ARRAY), false);
        for (Area a : AreaManager.INSTANCE.getKnownAreas()) {
        	src.sendSuccess(new TranslationTextComponent("area_control.claim.list.element", a.name, Util.toGreenText(a)), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int mark(CommandContext<CommandSource> context) throws CommandSyntaxException {
        BlockPos marked = new BlockPos(Vec3Argument.getVec3(context, "pos"));
        AreaControlClaimHandler.pushRecord(context.getSource().getPlayerOrException(), marked);
        context.getSource().sendSuccess(new TranslationTextComponent("area_control.claim.marked", Util.toGreenText(marked)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setProperty(CommandContext<CommandSource> context) throws CommandSyntaxException {
        final CommandSource src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), new BlockPos(src.getPosition()));
        if (area != null) {
            final String prop = context.getArgument("property", String.class);
            final String value = context.getArgument("value", String.class);
            final Object oldValue = area.properties.put(prop, value);
            context.getSource().sendSuccess(new TranslationTextComponent("area_control.claim.property.update",
            		area.name, prop, value, Objects.toString(oldValue)), false);
            return Command.SINGLE_SUCCESS;
        } else {
            context.getSource().sendFailure(new StringTextComponent("You are not even in an designated area?! This can be a bug; consider reporting it."));
            return -1;
        }
    }

    private static int unclaim(CommandContext<CommandSource> context) throws CommandSyntaxException {
        final CommandSource src = context.getSource();
        final RegistryKey<World> worldIndex = src.getLevel().dimension();
        final Area area = AreaManager.INSTANCE.findBy(worldIndex, new BlockPos(src.getPosition()));
        if (area != AreaManager.INSTANCE.wildness) {
            AreaManager.INSTANCE.remove(area, worldIndex);
            src.sendSuccess(new TranslationTextComponent("area_control.claim.abandoned", 
            		AreaProperties.getString(area, "area.display_name", area.name),
            		area.name, Util.toGreenText(area)), false);
            return Command.SINGLE_SUCCESS;
        } else {
            context.getSource().sendFailure(new StringTextComponent("You are in the wildness. Are you returning the wild nature to the nature itself?"));
            return -1;
        }
    }
}