package org.teacon.areacontrol.api;

import java.util.UUID;

public class AreaControlAPI {

    public static AreaLookup areaLookup = new AreaLookup() {

        @Override
        public Area findWildnessOf(String dimKey) {
            throw new IllegalStateException("Not initialized yet!");
        }

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
}
