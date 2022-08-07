package org.teacon.areacontrol.api;

import java.util.UUID;

public interface AreaLookup {

    Area findWildnessOf(String dimKey);

    Area findBy(UUID areaUid);

    Area findBy(String dimKey, double x, double y, double z);

    Area findBy(String dimKey, int x, int y, int z);
}
