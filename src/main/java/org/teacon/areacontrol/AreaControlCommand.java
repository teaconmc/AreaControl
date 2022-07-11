package org.teacon.areacontrol;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

public final class AreaControlCommand {

    public AreaControlCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ac")
                .redirect(dispatcher.register(Commands.literal("areacontrol")
                        .then(Commands.literal("about").executes(AreaControlCommand::about))
                        .then(Commands.literal("help").executes(AreaControlCommand::help))
                        .then(Commands.literal("admin").executes(AreaControlCommand::admin))
                        .then(Commands.literal("nearby").executes(AreaControlCommand::nearby))
                        .then(Commands.literal("claim").requires(check(AreaControlPermissions.CLAIM_AREA)).executes(AreaControlCommand::claim))
                        .then(Commands.literal("current")
                                .then(Commands.literal("friends")
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("friend", GameProfileArgument.gameProfile())
                                                        .executes(AreaControlCommand::addFriend)))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("friend", GameProfileArgument.gameProfile())
                                                        .executes(AreaControlCommand::removeFriend)))
                                        .executes(AreaControlCommand::listFriends))
                                .then(Commands.literal("properties")
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("property", StringArgumentType.string())
                                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                                .executes(AreaControlCommand::setProperty))))
                                        .then(Commands.literal("unset")
                                                .then(Commands.argument("property", StringArgumentType.string())
                                                        .executes(AreaControlCommand::unsetProperty)))
                                        .executes(AreaControlCommand::listProperties))
                                .executes(AreaControlCommand::displayCurrent))
                        .then(Commands.literal("list").executes(AreaControlCommand::list))
                        .then(Commands.literal("mark").requires(check(AreaControlPermissions.MARK_AREA)).then(
                                Commands.argument("pos", Vec3Argument.vec3()).executes(AreaControlCommand::mark)))
                        .then(Commands.literal("unclaim").requires(check(AreaControlPermissions.UNCLAIM_AREA)).executes(AreaControlCommand::unclaim))
                        )
                )
        );
    }

    private static Predicate<CommandSourceStack> check(PermissionNode<Boolean> permission) {
        return source -> {
            if (source.getEntity() instanceof ServerPlayer) {
                return PermissionAPI.getPermission((ServerPlayer) source.getEntity(), permission);
            }
            return source.hasPermission(2);
        };
    }

    private static int about(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(new TextComponent("AreaControl 0.1.4"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        var markerTool = new ItemStack(AreaControlClaimHandler.userClaimTool).getDisplayName();
        context.getSource().sendSuccess(new TranslatableComponent("area_control.claim.how_to", markerTool), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int admin(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(new TextComponent("WIP"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int nearby(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var server = src.getServer();
        final var sender = src.getPlayerOrException();
        final var dim = src.getLevel().dimension();
        AreaControlPlayerTracker.INSTANCE.sendNearbyAreasToClient(dim, sender, server.getPlayerList().getViewDistance() * 16);
        return Command.SINGLE_SUCCESS;
    }

    private static int claim(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var claimer = src.getPlayerOrException();
        final var recordPos = AreaControlClaimHandler.popRecord(claimer);
        if (recordPos != null) {
            final Area area = Util.createArea(recordPos.start(), recordPos.end());
            final UUID claimerUUID = claimer.getGameProfile().getId();
            if (claimerUUID != null) {
            	area.owner = claimerUUID;
            }
            final var worldIndex = src.getLevel().dimension();
            if (AreaManager.INSTANCE.add(area, worldIndex)) {
                src.sendSuccess(new TranslatableComponent("area_control.claim.created", area.name, Util.toGreenText(area)), true);
                return Command.SINGLE_SUCCESS;
            } else {
                src.sendFailure(new TranslatableComponent("area_control.error.overlap"));
                return -2;
            }
        } else {
            src.sendFailure(new TranslatableComponent("area_control.error.no_selection"));
            return -1;
        }
        
    }

    private static int displayCurrent(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var server = src.getServer();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), new BlockPos(src.getPosition()));
        if (area != AreaManager.INSTANCE.wildness) {
            final String name = AreaProperties.getString(area, "area.display_name", area.name);
            final var ownerName = Util.getOwnerName(area, server.getProfileCache(), server.getPlayerList());
            src.sendSuccess(new TranslatableComponent("area_control.claim.current", name, ownerName), true);
        } else {
            src.sendSuccess(new TranslatableComponent("area_control.claim.current.wildness"), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        src.sendSuccess(new TranslatableComponent("area_control.claim.list", ObjectArrays.EMPTY_ARRAY), false);
        for (Area a : AreaManager.INSTANCE.getKnownAreas()) {
        	src.sendSuccess(new TranslatableComponent("area_control.claim.list.element", a.name, Util.toGreenText(a)), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int mark(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos marked = new BlockPos(Vec3Argument.getVec3(context, "pos"));
        AreaControlClaimHandler.pushRecord(context.getSource().getPlayerOrException(), marked);
        context.getSource().sendSuccess(new TranslatableComponent("area_control.claim.marked", Util.toGreenText(marked)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int listFriends(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var server = src.getServer();
        final var profileCache = server.getProfileCache();
        final var playerList = server.getPlayerList();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), new BlockPos(src.getPosition()));
        // TODO Check if it is wildness
        src.sendSuccess(new TranslatableComponent("area_control.claim.friend.list.header", area.name), false);
        final var friends = area.friends;
        for (var friend :friends) {
            src.sendSuccess(new TranslatableComponent("area_control.claim.friend.list.entry", Util.getPlayerDisplayName(friend, profileCache, playerList)), false);
        }
        src.sendSuccess(new TranslatableComponent("area_control.claim.friend.list.footer", friends.size()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int addFriend(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), new BlockPos(src.getPosition()));
        final var player = src.getPlayerOrException();
        if (player.getGameProfile().getId().equals(area.owner) || PermissionAPI.getPermission(player, AreaControlPermissions.SET_FRIENDS)) {
            final var friendProfile = context.getArgument("friend", GameProfile.class);
            String message = area.friends.add(friendProfile.getId()) ? "area_control.claim.friend.added" : "area_control.claim.friend.existed";
            src.sendSuccess(new TranslatableComponent(message, area.name, Util.getOwnerName(friendProfile, src.getServer().getPlayerList())), false);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(new TranslatableComponent("area_control.error.cannot_set_friend", area.name));
            return -1;
        }
    }

    private static int removeFriend(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), new BlockPos(src.getPosition()));
        final var player = src.getPlayerOrException();
        if (player.getGameProfile().getId().equals(area.owner) || PermissionAPI.getPermission(player, AreaControlPermissions.SET_FRIENDS)) {
            final var friendProfile = context.getArgument("friend", GameProfile.class);
            String message = area.friends.remove(friendProfile.getId()) ? "area_control.claim.friend.removed" : "area_control.claim.friend.not_yet";
            src.sendSuccess(new TranslatableComponent(message, area.name, Util.getOwnerName(friendProfile, src.getServer().getPlayerList())), false);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(new TranslatableComponent("area_control.error.cannot_set_friend", area.name));
            return -1;
        }
    }

    private static int listProperties(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), new BlockPos(src.getPosition()));
        final var properties = area.properties;
        src.sendSuccess(new TranslatableComponent("area_control.claim.property.list.header", area.name), false);
        for (var prop : properties.entrySet()) {
            src.sendSuccess(new TranslatableComponent("area_control.claim.property.list.entry", prop.getKey(), prop.getValue()), false);
        }
        src.sendSuccess(new TranslatableComponent("area_control.claim.property.list.footer", properties.size()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setProperty(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), new BlockPos(src.getPosition()));
        final var player = src.getPlayerOrException();
        if (player.getGameProfile().getId().equals(area.owner) || PermissionAPI.getPermission(player, AreaControlPermissions.SET_PROPERTY)) {
            final String prop = context.getArgument("property", String.class);
            final String value = context.getArgument("value", String.class);
            final Object oldValue = area.properties.put(prop, value);
            src.sendSuccess(new TranslatableComponent("area_control.claim.property.update",
                    area.name, prop, value, Objects.toString(oldValue)), false);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(new TranslatableComponent("area_control.error.cannot_set_property", area.name));
            return -1;
        }
    }

    private static int unsetProperty(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), new BlockPos(src.getPosition()));
        final var player = src.getPlayerOrException();
        if (player.getGameProfile().getId().equals(area.owner) || PermissionAPI.getPermission(player, AreaControlPermissions.SET_PROPERTY)) {
            final String prop = context.getArgument("property", String.class);
            final Object oldValue = area.properties.remove(prop);
            src.sendSuccess(new TranslatableComponent("area_control.claim.property.unset",
                    area.name, prop, Objects.toString(oldValue)), false);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(new TranslatableComponent("area_control.error.cannot_set_property", area.name));
            return -1;
        }
    }

    private static int unclaim(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var claimer = src.getPlayerOrException();
        final ResourceKey<Level> worldIndex = src.getLevel().dimension();
        final Area area = AreaManager.INSTANCE.findBy(worldIndex, new BlockPos(src.getPosition()));
        if (area != AreaManager.INSTANCE.wildness) {
            if (area.owner.equals(claimer.getGameProfile().getId())) {
                AreaManager.INSTANCE.remove(area, worldIndex);
                src.sendSuccess(new TranslatableComponent("area_control.claim.abandoned", 
                        AreaProperties.getString(area, "area.display_name", area.name),
                        area.name, Util.toGreenText(area)), false);
                return Command.SINGLE_SUCCESS;
            } else {
                src.sendFailure(new TranslatableComponent("area_cnotrol.error.unclaim_without_permimisson",
                        AreaProperties.getString(area, "area.display_name", area.name),
                        area.name, Util.toGreenText(area)));
                return -1;
            }
        } else {
            context.getSource().sendFailure(new TranslatableComponent("area_control.error.unclaim_wildness"));
            return -1;
        }
    }
}