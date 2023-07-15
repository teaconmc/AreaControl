package org.teacon.areacontrol.api;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface AreaLookup {

    @Nullable Area findBy(UUID areaUid);

    @Nullable Area findBy(String dimKey, double x, double y, double z);

    @Nullable Area findBy(String dimKey, int x, int y, int z);
}
