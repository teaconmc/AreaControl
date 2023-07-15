package org.teacon.areacontrol;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
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
import org.teacon.areacontrol.impl.AreaChecks;
import org.teacon.areacontrol.impl.command.arguments.AreaPropertyArgument;
import org.teacon.areacontrol.impl.command.arguments.DirectionArgument;
import org.teacon.areacontrol.mixin.CommandSourceStackAccessor;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

public final class AreaControlCommand {

    private static final Component ERROR_WILD = Component.translatable("area_control.claim.current.wildness");

    public AreaControlCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ac")
                .redirect(dispatcher.register(Commands.literal("areacontrol")
                        .then(Commands.literal("about").executes(AreaControlCommand::about))
                        .then(Commands.literal("help").executes(AreaControlCommand::help))
                        .then(Commands.literal("admin").executes(AreaControlCommand::admin))
                        .then(Commands.literal("nearby")
                                .then(Commands.literal("on").executes(context -> AreaControlCommand.nearby(context, true)))
                                .then(Commands.literal("off").executes(AreaControlCommand::nearbyClear))
                                .executes(context -> AreaControlCommand.nearby(context, false)))
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
                                .then(Commands.literal("range")
                                        .then(Commands.literal("expand")
                                                .then(Commands.argument("direction", DirectionArgument.direction())
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                                .executes(AreaControlCommand::changeAreaRange)))))
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
                                                                .executes(AreaControlCommand::setProperty))
                                                        .executes(AreaControlCommand::displayProperty)))
                                        .then(Commands.literal("unset")
                                                .then(Commands.argument("property", AreaPropertyArgument.areaProperty())
                                                        .executes(AreaControlCommand::unsetProperty)))
                                        .executes(AreaControlCommand::listProperties))
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
            // /execute as will change the "on-behalf-of" source, so we need to extract the true source.
            if (((CommandSourceStackAccessor) source).getSource() instanceof ServerPlayer sp) {
                return PermissionAPI.getPermission(sp, permission);
            }
            return false;
        };
    }

    private static int about(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(Component.literal("AreaControl 0.1.4"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        var markerTool = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(AreaControlConfig.areaClaimTool.get())));
        var markerToolName = markerTool.getDisplayName();
        var displayName = markerToolName.copy()
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.BLUE)
                        .withUnderlined(Boolean.TRUE)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, markerTool.getHoverName().copy()
                                .append(Component.translatable("area_control.claim.how_to.give_item").withStyle(ChatFormatting.GRAY))))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/give @s " + AreaControlConfig.areaClaimTool.get())));
        context.getSource().sendSuccess(Component.translatable("area_control.claim.how_to", displayName), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int admin(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(Component.literal("WIP"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int nearby(CommandContext<CommandSourceStack> context, boolean permanent) throws CommandSyntaxException {
        final var src = context.getSource();
        final var server = src.getServer();
        final var sender = src.getPlayerOrException();
        final var dim = src.getLevel().dimension();
        AreaControlPlayerTracker.INSTANCE.sendNearbyAreasToClient(dim, sender, server.getPlayerList().getViewDistance() * 16, permanent);
        return Command.SINGLE_SUCCESS;
    }

    private static int nearbyClear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var sender = src.getPlayerOrException();
        AreaControlPlayerTracker.INSTANCE.clearNearbyAreasForClient(sender);
        return Command.SINGLE_SUCCESS;
    }

    private static int claimChunk(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var claimer = src.getPlayerOrException();
        final var pos = src.getPosition();
        final var chunkPos = new ChunkPos(SectionPos.blockToSectionCoord(pos.x), SectionPos.blockToSectionCoord(pos.z));
        final var chunkStart = chunkPos.getBlockAt(0, Short.MIN_VALUE, 0);
        final var chunkEnd = chunkPos.getBlockAt(15, Short.MAX_VALUE, 15);
        final var range = new AABB(Vec3.atCenterOf(chunkStart), Vec3.atCenterOf(chunkEnd));
        if (!range.expandTowards(0.5, 0.5, 0.5).contains(claimer.position())) {
            src.sendFailure(Component.translatable("area_control.error.outside_selection"));
            return -1;
        }
        final Area area = Util.createArea(chunkStart, chunkEnd);
        final var worldIndex = src.getLevel().dimension();
        final UUID claimerUUID = claimer.getGameProfile().getId();
        if (claimerUUID != null) {
            area.owners.add(claimerUUID);
        }
        if (AreaManager.INSTANCE.add(area, worldIndex)) {
            src.sendSuccess(Component.translatable("area_control.claim.created", area.name, Util.toGreenText(area)), true);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(Component.translatable("area_control.error.overlap"));
            return -2;
        }
    }

    private static int claimChunkWithSize(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var claimer = src.getPlayerOrException();
        final var pos = src.getPosition();
        final var corner = new ChunkPos(SectionPos.blockToSectionCoord(pos.x), SectionPos.blockToSectionCoord(pos.z));
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
            src.sendFailure(Component.translatable("area_control.error.outside_selection"));
            return -1;
        }
        final Area area = Util.createArea(chunkStart, chunkEnd);
        final var worldIndex = src.getLevel().dimension();
        final UUID claimerUUID = claimer.getGameProfile().getId();
        if (claimerUUID != null) {
            area.owners.add(claimerUUID);
        }
        if (AreaManager.INSTANCE.add(area, worldIndex)) {
            src.sendSuccess(Component.translatable("area_control.claim.created", area.name, Util.toGreenText(area)), true);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(Component.translatable("area_control.error.overlap"));
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
                src.sendFailure(Component.translatable("area_control.error.outside_selection"));
                return -1;
            }
            final Area area = Util.createArea(recordPos.start(), recordPos.end());
            final UUID claimerUUID = claimer.getGameProfile().getId();
            if (claimerUUID != null) {
            	area.owners.add(claimerUUID);
            }
            final var worldIndex = src.getLevel().dimension();
            if (AreaManager.INSTANCE.add(area, worldIndex)) {
                src.sendSuccess(Component.translatable("area_control.claim.created", area.name, Util.toGreenText(area)), true);
                return Command.SINGLE_SUCCESS;
            } else {
                src.sendFailure(Component.translatable("area_control.error.overlap"));
                return -2;
            }
        } else {
            src.sendFailure(Component.translatable("area_control.error.no_selection"));
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
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), src.getPosition());
        if (area != null) {
            final var areaUUID = Component.translatable("area_control.claim.current.uuid")
                    .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, area.uid.toString()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("area_control.claim.current.copy_uuid")))
                            .withColor(ChatFormatting.DARK_AQUA));
            final String name = area.name;
            final var areaName = Component.literal(name)
                    .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, name))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("area_control.claim.current.copy_name")))
                            .withColor(ChatFormatting.DARK_AQUA));
            final var ownerName = Util.getOwnerName(area, server.getProfileCache(), server.getPlayerList());
            src.sendSuccess(Component.translatable("area_control.claim.current", areaName, ownerName, areaUUID), true);
            if (area.belongingArea != null) {
                final var enclosingArea = AreaManager.INSTANCE.findBy(area.belongingArea);
                final var enclosingAreaOwnerName = Util.getOwnerName(enclosingArea, server.getProfileCache(), server.getPlayerList());
                src.sendSuccess(Component.translatable("area_control.claim.current.enclosed", enclosingArea.name, enclosingAreaOwnerName), true);
            }
        } else {
            src.sendSuccess(ERROR_WILD, true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int mark(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var rawPos = Vec3Argument.getVec3(context, "pos");
        BlockPos marked = new BlockPos((int) rawPos.x, (int) rawPos.y, (int) rawPos.z);
        AreaControlClaimHandler.pushRecord(context.getSource().getPlayerOrException(), marked);
        context.getSource().sendSuccess(Component.translatable("area_control.claim.marked", Util.toGreenText(marked)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayAreaName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var level = src.getLevel();
        final var pos = src.getPosition();
        final var area = AreaManager.INSTANCE.findBy(level, pos);
        src.sendSuccess(Component.literal(area.name), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setAreaName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var requester = src.getPlayerOrException();
        final var level = src.getLevel();
        final var pos = src.getPosition();
        final var area = AreaManager.INSTANCE.findBy(level, pos);
        if (AreaChecks.allow(requester, area, AreaControlPermissions.SET_PROPERTY)) {
            final var newName = context.getArgument("name", String.class);
            if (AreaManager.INSTANCE.findBy(newName) == null) {
                final var oldName = area.name;
                AreaManager.INSTANCE.rename(area, newName);
                src.sendSuccess(Component.translatable("area_control.claim.name.update", oldName, newName), true);
                return Command.SINGLE_SUCCESS;
            } else {
                src.sendSuccess(Component.translatable("area_control.error.name_clash", newName), true);
                return -1;
            }
        } else {
            src.sendFailure(Component.translatable("area_control.error.cannot_set_property", area.name));
            return -1;
        }
    }

    private static int changeAreaRange(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var requester = src.getPlayerOrException();
        final var requesterId = requester.getGameProfile().getId();
        final var level = src.getLevel();
        final var pos = src.getPosition();
        final var area = AreaManager.INSTANCE.findBy(level, pos);
        if (area.owners.contains(requesterId) || area.builders.contains(requesterId) || PermissionAPI.getPermission(requester, AreaControlPermissions.SET_PROPERTY)) {
            final var direction = context.getArgument("direction", Direction.class);
            final var amount = context.getArgument("amount", Integer.class);
            if (AreaManager.INSTANCE.changeRangeForArea(level.dimension(), area, direction, amount)) {
                src.sendSuccess(Component.translatable("area_control.claim.range.update.success",
                        Util.toGreenText(new BlockPos(area.minX, area.minY, area.minZ)),
                        Util.toGreenText(new BlockPos(area.maxX, area.maxY, area.maxZ))), true);
                return Command.SINGLE_SUCCESS;
            } else {
                src.sendFailure(Component.translatable("area_control.error.cannot_change_range", area.name));
                return -1;
            }
        } else {
            src.sendFailure(Component.translatable("area_control.error.cannot_set_property", area.name));
            return -1;
        }
    }

    private static int listFriends(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var server = src.getServer();
        final var profileCache = server.getProfileCache();
        final var playerList = server.getPlayerList();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), src.getPosition());
        if (area == null) {
            src.sendSuccess(ERROR_WILD, true);
            return 0;
        }
        src.sendSuccess(Component.translatable("area_control.claim.builder.list.header", area.name), false);
        final var builders = area.builders;
        for (var builder :builders) {
            src.sendSuccess(Component.translatable("area_control.claim.builder.list.entry", Util.getPlayerDisplayName(builder, profileCache, playerList)), false);
        }
        src.sendSuccess(Component.translatable("area_control.claim.builder.list.footer", builders.size()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int addFriend(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), src.getPosition());
        final var player = src.getPlayerOrException();
        if (area == null) {
            src.sendSuccess(ERROR_WILD, true);
            return 0;
        } else if (area.owners.contains(player.getGameProfile().getId()) || PermissionAPI.getPermission(player, AreaControlPermissions.SET_FRIENDS)) {
            final var friendProfiles = GameProfileArgument.getGameProfiles(context, "friend");
            int count = 0;
            for (var friendProfile : friendProfiles) {
                var uid = friendProfile.getId();
                String message = !area.owners.contains(uid) && area.builders.add(uid) ? "area_control.claim.friend.added" : "area_control.claim.friend.existed";
                src.sendSuccess(Component.translatable(message, area.name, Util.getOwnerName(friendProfile, src.getServer().getPlayerList())), false);
            }
            return count;
        } else {
            src.sendFailure(Component.translatable("area_control.error.cannot_set_friend", area.name));
            return -1;
        }
    }

    private static int removeFriend(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), src.getPosition());
        final var player = src.getPlayerOrException();
        if (area == null) {
            src.sendSuccess(ERROR_WILD, true);
            return 0;
        } else if (area.owners.contains(player.getGameProfile().getId()) || PermissionAPI.getPermission(player, AreaControlPermissions.SET_FRIENDS)) {
            final var friendProfiles = GameProfileArgument.getGameProfiles(context, "friend");
            int count = 0;
            for (var friendProfile : friendProfiles) {
                String message = area.builders.remove(friendProfile.getId()) ? "area_control.claim.friend.removed" : "area_control.claim.friend.not_yet";
                src.sendSuccess(Component.translatable(message, area.name, Util.getOwnerName(friendProfile, src.getServer().getPlayerList())), false);
            }
            return count;
        } else {
            src.sendFailure(Component.translatable("area_control.error.cannot_set_friend", area.name));
            return -1;
        }
    }

    private static int listProperties(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), src.getPosition());
        if (area == null) {
            src.sendSuccess(ERROR_WILD, true);
            return 0;
        }
        final var properties = area.properties;
        src.sendSuccess(Component.translatable("area_control.claim.property.list.header", area.name), false);
        for (var prop : properties.entrySet()) {
            src.sendSuccess(Component.translatable("area_control.claim.property.list.entry", prop.getKey(), prop.getValue()), false);
        }
        src.sendSuccess(Component.translatable("area_control.claim.property.list.footer", properties.size()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayProperty(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), src.getPosition());
        final var player = src.getPlayerOrException();
        if (area == null) {
            src.sendSuccess(ERROR_WILD, true);
            return 0;
        } else if (AreaChecks.allow(player, area, AreaControlPermissions.SET_PROPERTY)) {
            final String prop = context.getArgument("property", String.class);
            final Object value = area.properties.get(prop);
            Component msg;
            if (value == null) {
                msg = Component.translatable("area_control.claim.property.single.unset", area.name, prop);
            } else {
                msg = Component.translatable("area_control.claim.property.single", area.name, prop, value);
            }
            src.sendSuccess(msg, false);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(Component.translatable("area_control.error.cannot_set_property", area.name));
            return -1;
        }
    }

    private static int setProperty(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), src.getPosition());
        final var player = src.getPlayerOrException();
        if (area == null) {
            src.sendSuccess(ERROR_WILD, true);
            return 0;
        } else if (AreaChecks.allow(player, area, AreaControlPermissions.SET_PROPERTY)) {
            final String prop = context.getArgument("property", String.class);
            final String value = context.getArgument("value", String.class);
            final Object oldValue = area.properties.put(prop, value);
            src.sendSuccess(Component.translatable("area_control.claim.property.update",
                    area.name, prop, value, Objects.toString(oldValue)), false);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(Component.translatable("area_control.error.cannot_set_property", area.name));
            return -1;
        }
    }

    private static int unsetProperty(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final Area area = AreaManager.INSTANCE.findBy(src.getLevel().dimension(), src.getPosition());
        final var player = src.getPlayerOrException();
        if (area == null) {
            src.sendSuccess(ERROR_WILD, true);
            return 0;
        } else if (AreaChecks.allow(player, area, AreaControlPermissions.SET_PROPERTY)) {
            final String prop = context.getArgument("property", String.class);
            final Object oldValue = area.properties.remove(prop);
            src.sendSuccess(Component.translatable("area_control.claim.property.unset",
                    area.name, prop, Objects.toString(oldValue)), false);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(Component.translatable("area_control.error.cannot_set_property", area.name));
            return -1;
        }
    }

    private static int displayMine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var player = src.getPlayerOrException();
        var areas = AreaManager.INSTANCE.findByOwner(player.getGameProfile().getId());
        src.sendSuccess(Component.translatable("area_control.claim.mine", areas.size()), false);
        for (Area area : areas) {
            src.sendSuccess(Util.describe(area), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int unclaim(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var src = context.getSource();
        final var claimer = src.getPlayerOrException();
        final ResourceKey<Level> worldIndex = src.getLevel().dimension();
        final Area area = AreaManager.INSTANCE.findBy(worldIndex, src.getPosition());
        if (area != null) {
            if (area.owners.contains(claimer.getGameProfile().getId()) || PermissionAPI.getPermission(claimer, AreaControlPermissions.UNCLAIM_AREA)) {
                AreaManager.INSTANCE.remove(area, worldIndex);
                src.sendSuccess(Component.translatable("area_control.claim.abandoned",
                        area.name, Util.toGreenText(area)), false);
                return Command.SINGLE_SUCCESS;
            } else {
                src.sendFailure(Component.translatable("area_control.error.unclaim_without_permission",
                        area.name, Util.toGreenText(area)));
                return -1;
            }
        } else {
            context.getSource().sendFailure(Component.translatable("area_control.error.unclaim_wildness"));
            return -1;
        }
    }
}