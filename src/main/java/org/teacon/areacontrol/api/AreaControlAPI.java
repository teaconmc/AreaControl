package org.teacon.areacontrol.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

public class AreaControlAPI {

    public static AreaLookup areaLookup = new AreaLookup() {

        @Override
        public Area findBy(UUID areaUid) {
            throw new IllegalStateException("Not initialized yet!");
        }

        @Override
        public Area findBy(String dimKey, double x, double y, double z) {
            throw new IllegalStateException("Not initialized yet!");
        }

        @Override
        public Area findBy(String dimKey, int x, int y, int z) {
            throw new IllegalStateException("Not initialized yet!");
        }
    };

    public static GroupProvider groupProvider = new GroupProvider() {

        @Override
        public @NotNull Collection<String> getGroups() {
            throw new IllegalStateException("Not initialized yet!");
        }

        @Override
        public boolean isValidGroup(@Nullable String groupIdentifier) {
            throw new IllegalStateException("Not initialized yet!");
        }

        @Override
        public @Nullable String getGroupFor(@NotNull UUID playerId) {
            throw new IllegalStateException("Not initialized yet!");
        }
    };
}
