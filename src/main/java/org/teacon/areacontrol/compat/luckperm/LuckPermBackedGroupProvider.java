package org.teacon.areacontrol.compat.luckperm;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teacon.areacontrol.api.GroupProvider;

import java.util.Collection;
import java.util.UUID;

public enum LuckPermBackedGroupProvider implements GroupProvider {

    INSTANCE;

    @Override
    public @NotNull Collection<String> getGroups() {
        var luckPermGroupManager = LuckPermsProvider.get().getGroupManager();
        return luckPermGroupManager.getLoadedGroups().stream().map(Group::getName).toList();
    }

    @Override
    public boolean isValidGroup(@Nullable String groupIdentifier) {
        var luckPermGroupManager = LuckPermsProvider.get().getGroupManager();
        return groupIdentifier != null && luckPermGroupManager.isLoaded(groupIdentifier);
    }

    @Override
    public @Nullable String getGroupFor(@NotNull UUID playerId) {
        var luckPerm = LuckPermsProvider.get();
        var luckUser = luckPerm.getUserManager().getUser(playerId);
        return luckUser == null ? null : luckUser.getPrimaryGroup();
    }
}
