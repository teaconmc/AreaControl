package org.teacon.areacontrol.impl;

import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teacon.areacontrol.api.GroupProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public enum VanillaScoreboardTeamGroupProvider implements GroupProvider {

    INSTANCE;

    @Override
    public @NotNull Collection<String> getGroups() {
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

    @Override
    public @Nullable String getGroupFor(@NotNull UUID playerId) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            var p = server.getPlayerList().getPlayer(playerId);
            if (p != null) {
                var team = p.getTeam();
                return team != null ? team.getName() : null;
            }
        }
        return null;
    }

}
