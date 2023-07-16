package org.teacon.areacontrol.impl;

import net.minecraftforge.server.ServerLifecycleHooks;
import org.teacon.areacontrol.api.GroupProvider;

import java.util.Collection;
import java.util.Collections;

public enum VanillaScoreboardTeamGroupProvider implements GroupProvider {

    INSTANCE;

    @Override
    public Collection<String> getGroups() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.getScoreboard().getTeamNames();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isValidGroup(String groupIdentifier) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.getScoreboard().getTeamNames().contains(groupIdentifier);
        } else {
            return false;
        }
    }
}
