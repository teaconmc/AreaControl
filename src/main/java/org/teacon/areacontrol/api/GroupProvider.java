package org.teacon.areacontrol.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.UUID;

public interface GroupProvider {

    /**
     * Fetch a read-only collection of strings, in which each string represents a "group".
     * A "group" is lossly defined as a collection of players.
     * @return Collection of group names
     */
    @NotNull @Unmodifiable Collection<String> getGroups();

    /**
     * Check whether a given group name corresponds to a valid group or not.
     * @param groupIdentifier Group name to check
     * @return true if the group name points to a valid group; false otherwise.
     */
    boolean isValidGroup(@Nullable String groupIdentifier);

    @Nullable String getGroupFor(@NotNull UUID playerId);
}
