package org.teacon.areacontrol.compat.luckperm;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import org.teacon.areacontrol.api.GroupProvider;

import java.util.Collection;

public enum LuckPermBackedGroupProvider implements GroupProvider {

    INSTANCE;

    @Override
    public Collection<String> getGroups() {
        var luckPermGroupManager = LuckPermsProvider.get().getGroupManager();
        return luckPermGroupManager.getLoadedGroups().stream().map(Group::getName).toList();
    }

    @Override
    public boolean isValidGroup(String groupIdentifier) {
        var luckPermGroupManager = LuckPermsProvider.get().getGroupManager();
        return luckPermGroupManager.isLoaded(groupIdentifier);
    }
}
