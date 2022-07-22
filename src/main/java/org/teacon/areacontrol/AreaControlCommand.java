package org.teacon.areacontrol;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;
import org.teacon.areacontrol.impl.AreaPropertyArgument;

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
                        .then(Commands.literal("desel").requires(check(AreaControlPermissions.CLAIM_MARKED_AREA)).executes(AreaControlCommand::clearMarked))
                        .then(Commands.literal("deselect").requires(check(AreaControlPermissions.CLAIM_MARKED_AREA)).executes(AreaControlCommand::clearMarked))
                        .then(Commands.literal("claim")
                                .then(Commands.literal("cancel")
                                        .requires(check(AreaControlPermissions.CLAIM_MARKED_AREA))
                                        .executes(AreaControlCommand::clearMarked))
                                .then(Commands.literal("marked")
                                        .requires(check(AreaControlPermissions.CLAIM_MARKED_AREA))
                                        .executes(AreaControlCommand::claimMarked))
                                .then(Commands.literal("chunk")
                                        .requires(check(AreaControlPermissions.CLAIM_CHUNK_AREA))
                                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .executes(AreaControlCommand::claimChunkWithSize)))
                                        .executes(AreaControlCommand::claimChunk)))
                        .then(Commands.literal("current")
                                .then(Commands.literal("name")
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                                        .executes(AreaControlCommand::setAreaName)))
                                        .executes(AreaControlCommand::displayAreaName))
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
                                                .then(Commands.argument("property", AreaPropertyArgument.areaProperty())
                                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                                .executes(AreaControlCommand::setProperty))))
                                        .then(Commands.literal("unset")
                                                .then(Commands.argument("property", AreaPropertyArgument.areaProperty())
                                                        .executes(AreaControlCommand::unsetProperty)))
                                        .executes(AreaControlCommand::listProperties))
                                .then(Commands.literal("tags")
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("tag", ResourceLocationArgument.id())
                                                        .executes(AreaControlCommand::addTag)))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("tag", ResourceLocationArgument.id())
                                                        .executes(AreaControlCommand::removeTag)))
                                        .executes(AreaControlCommand::listTags))
                                .executes(AreaControlCommand::displayCurrent))
                         .then(Commands.literal("mine")
                                 .executes(AreaControlCommand::displayMine))
                        .then(Commands.literal("mark").requires(check(AreaControlPermissions.MARK_AREA)).then(
                                Commands.argument("pos", Vec3Argument.vec3()).executes(AreaControlCommand::mark)))
                        .then(Commands.literal("unclaim").executes(AreaControlCommand::unclaim))
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
        var markerTool = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(AreaControlConfig.areaClaimTool.get())));
        var markerToolName = markerTool.getDisplayName();
        context.getSource().sendSuccess(new TranslatableComponent("area_control.claim.how_to", markerToolName), false);
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

    private static int claimChunk(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var claimer = src.getPlayerOrException();
        final var chunkPos = new ChunkPos(new BlockPos(src.getPosition()));
        final var chunkStart = chunkPos.getBlockAt(0, Short.MIN_VALUE, 0);
        final var chunkEnd = chunkPos.getBlockAt(15, Short.MAX_VALUE, 15);
        final var range = new AABB(Vec3.atCenterOf(chunkStart), Vec3.atCenterOf(chunkEnd));
        if (!range.expandTowards(0.5, 0.5, 0.5).contains(claimer.position())) {
            src.sendFailure(new TranslatableComponent("area_control.error.outside_selection"));
            return -1;
        }
        final Area area = Util.createArea(chunkStart, chunkEnd);
        final var worldIndex = src.getLevel().dimension();
        final UUID claimerUUID = claimer.getGameProfile().getId();
        if (claimerUUID != null) {
            area.owner = claimerUUID;
        }
        if (AreaManager.INSTANCE.add(area, worldIndex)) {
            src.sendSuccess(new TranslatableComponent("area_control.claim.created", area.name, Util.toGreenText(area)), true);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(new TranslatableComponent("area_control.error.overlap"));
            return -2;
        }
    }

    private static int claimChunkWithSize(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var claimer = src.getPlayerOrException();
        final var corner = new ChunkPos(new BlockPos(src.getPosition()));
        int xOffset = context.getArgument("x", Integer.class), zOffset = context.getArgument("z", Integer.class);
        final BlockPos chunkStart, chunkEnd;
        if (xOffset > 0) {
            if (zOffset > 0) {
                // Corner is at northwest
                chunkStart = corner.getBlockAt(0, Short.MIN_VALUE, 0);
                chunkEnd = new ChunkPos(corner.x + xOffset - 1, corner.z + zOffset - 1).getBlockAt(15, Short.MAX_VALUE, 15);
            } else if (zOffset == 0) {
                // Corner is at northwest
                chunkStart = corner.getBlockAt(0, Short.MIN_VALUE, 0);
                chunkEnd = new ChunkPos(corner.x + xOffset - 1, corner.z).getBlockAt(15, Short.MAX_VALUE, 15);
            } else {
                // Corner is at southwest
                chunkStart = corner.getBlockAt(0, Short.MIN_VALUE, 15);
                chunkEnd = new ChunkPos(corner.x + xOffset - 1, corner.z + zOffset + 1).getBlockAt(15, Short.MAX_VALUE, 0);
            }
        } else if (xOffset == 0) {
            if (zOffset > 0) {
                // Corner is at northwest
                chunkStart = corner.getBlockAt(0, Short.MIN_VALUE, 0);
                chunkEnd = new ChunkPos(corner.x, corner.z + zOffset - 1).getBlockAt(15, Short.MAX_VALUE, 15);
            } else if (zOffset == 0) {
                // Corner is at northwest, just one chunk
                chunkStart = corner.getBlockAt(0, Short.MIN_VALUE, 0);
                chunkEnd = corner.getBlockAt(15, Short.MAX_VALUE, 15);
            } else {
                // Corner is at southwest
                chunkStart = corner.getBlockAt(0, Short.MIN_VALUE, 15);
                chunkEnd = new ChunkPos(corner.x, corner.z + zOffset + 1).getBlockAt(15, Short.MAX_VALUE, 0);
            }
        } else {
            if (zOffset > 0) {
                // Corner is at northeast
                chunkStart = corner.getBlockAt(15, Short.MIN_VALUE, 0);
                chunkEnd = new ChunkPos(corner.x + xOffset + 1, corner.z + zOffset - 1).getBlockAt(0, Short.MAX_VALUE, 15);
            } else if (zOffset == 0) {
                // Corner is at northeast
                chunkStart = corner.getBlockAt(15, Short.MIN_VALUE, 0);
                chunkEnd = new ChunkPos(corner.x + xOffset + 1, corner.z).getBlockAt(0, Short.MAX_VALUE, 15);
            } else {
                // Corner is southeast
                chunkStart = corner.getBlockAt(15, Short.MIN_VALUE, 15);
                chunkEnd = new ChunkPos(corner.x + xOffset + 1, corner.z + zOffset + 1).getBlockAt(0, Short.MAX_VALUE, 0);
            }
        }
        final var range = new AABB(Vec3.atCenterOf(chunkStart), Vec3.atCenterOf(chunkEnd));
        if (!range.expandTowards(0.5, 0.5, 0.5).contains(claimer.position())) {
            src.sendFailure(new TranslatableComponent("area_control.error.outside_selection"));
            return -1;
        }
        final Area area = Util.createArea(chunkStart, chunkEnd);
        final var worldIndex = src.getLevel().dimension();
        final UUID claimerUUID = claimer.getGameProfile().getId();
        if (claimerUUID != null) {
            area.owner = claimerUUID;
        }
        if (AreaManager.INSTANCE.add(area, worldIndex)) {
            src.sendSuccess(new TranslatableComponent("area_control.claim.created", area.name, Util.toGreenText(area)), true);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(new TranslatableComponent("area_control.error.overlap"));
            return -2;
        }
    }

    private static int claimMarked(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var claimer = src.getPlayerOrException();
        final var recordPos = AreaControlClaimHandler.popRecord(claimer);
        AreaControlPlayerTracker.INSTANCE.clearSelectionForClient(claimer);
        if (recordPos != null) {
            final var range = new AABB(Vec3.atCenterOf(recordPos.start()), Vec3.atCenterOf(recordPos.end()));
            if (!range.expandTowards(0.5, 0.5, 0.5).contains(claimer.position())) {
                src.sendFailure(new TranslatableComponent("area_control.error.outside_selection"));
                return -1;
            }
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

    private static int clearMarked(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var claimer = src.getPlayerOrException();
        AreaControlPlayerTracker.INSTANCE.clearSelectionForClient(claimer);
        AreaControlClaimHandler.popRecord(claimer);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayCurrent(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var server = src.getServer();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), new BlockPos(src.getPosition()));
        if (!area.owner.equals(Area.GLOBAL_AREA_OWNER)) {
            final String name = area.name;
            final var ownerName = Util.getOwnerName(area, server.getProfileCache(), server.getPlayerList());
            src.sendSuccess(new TranslatableComponent("area_control.claim.current", name, ownerName), true);
            if (area.belongingArea != null) {
                final var enclosingArea = AreaManager.INSTANCE.findBy(area.belongingArea);
                final var enclosingAreaOwnerName = Util.getOwnerName(enclosingArea, server.getProfileCache(), server.getPlayerList());
                src.sendSuccess(new TranslatableComponent("area_control.claim.current.enclosed", enclosingArea.name, enclosingAreaOwnerName), true);
            }
        } else {
            src.sendSuccess(new TranslatableComponent("area_control.claim.current.wildness"), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int mark(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos marked = new BlockPos(Vec3Argument.getVec3(context, "pos"));
        AreaControlClaimHandler.pushRecord(context.getSource().getPlayerOrException(), marked);
        context.getSource().sendSuccess(new TranslatableComponent("area_control.claim.marked", Util.toGreenText(marked)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayAreaName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var level = src.getLevel();
        final var pos = src.getPosition();
        final var area = AreaManager.INSTANCE.findBy(level, new BlockPos(pos));
        src.sendSuccess(new TextComponent(area.name), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setAreaName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var requester = src.getPlayerOrException();
        final var requesterId = requester.getGameProfile().getId();
        final var level = src.getLevel();
        final var pos = src.getPosition();
        final var area = AreaManager.INSTANCE.findBy(level, new BlockPos(pos));
        if (area.owner.equals(requesterId) || area.friends.contains(requesterId) || PermissionAPI.getPermission(requester, AreaControlPermissions.SET_PROPERTY)) {
            final var newName = context.getArgument("name", String.class);
            final var oldName = area.name;
            area.name = newName;
            src.sendSuccess(new TranslatableComponent("area_control.claim.name.update", oldName, newName), true);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(new TranslatableComponent("area_control.error.cannot_set_property", area.name));
            return -1;
        }
    }

    private static int listTags(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var server = src.getServer();
        final var profileCache = server.getProfileCache();
        final var playerList = server.getPlayerList();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), new BlockPos(src.getPosition()));
        src.sendSuccess(new TextComponent(area.tags + " tag(s): " + String.join(", ", area.tags)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int addTag(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), new BlockPos(src.getPosition()));
        final var player = src.getPlayerOrException();
        if (player.getGameProfile().getId().equals(area.owner) || PermissionAPI.getPermission(player, AreaControlPermissions.SET_FRIENDS)) {
            final var tag = ResourceLocationArgument.getId(context, "tag");
            String message = area.tags.add(tag.toString()) ? "area_control.claim.tag.added" : "area_control.claim.tag.existed";
            src.sendSuccess(new TranslatableComponent(message, area.name, tag), false);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(new TranslatableComponent("area_control.error.cannot_set_tag", area.name));
            return -1;
        }
    }

    private static int removeTag(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), new BlockPos(src.getPosition()));
        final var player = src.getPlayerOrException();
        if (player.getGameProfile().getId().equals(area.owner) || PermissionAPI.getPermission(player, AreaControlPermissions.SET_FRIENDS)) {
            final var tag = ResourceLocationArgument.getId(context, "tag");
            String message = area.tags.remove(tag.toString()) ? "area_control.claim.friend.removed" : "area_control.claim.friend.not_yet";
            src.sendSuccess(new TranslatableComponent(message, area.name, tag), false);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(new TranslatableComponent("area_control.error.cannot_set_tag", area.name));
            return -1;
        }
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

    private static int displayMine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var player = src.getPlayerOrException();
        var areas = AreaManager.INSTANCE.findByOwner(player.getGameProfile().getId());
        src.sendSuccess(new TranslatableComponent("area_control.claim.mine", areas.size()), false);
        for (Area area : areas) {
            src.sendSuccess(Util.describe(area), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int unclaim(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var claimer = src.getPlayerOrException();
        final ResourceKey<Level> worldIndex = src.getLevel().dimension();
        final Area area = AreaManager.INSTANCE.findBy(worldIndex, new BlockPos(src.getPosition()));
        if (!area.owner.equals(Area.GLOBAL_AREA_OWNER)) {
            if (area.owner.equals(claimer.getGameProfile().getId()) || PermissionAPI.getPermission(claimer, AreaControlPermissions.UNCLAIM_AREA)) {
                AreaManager.INSTANCE.remove(area, worldIndex);
                src.sendSuccess(new TranslatableComponent("area_control.claim.abandoned",
                        AreaProperties.getString(area, "area.display_name", area.name),
                        area.name, Util.toGreenText(area)), false);
                return Command.SINGLE_SUCCESS;
            } else {
                src.sendFailure(new TranslatableComponent("area_control.error.unclaim_without_permission",
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