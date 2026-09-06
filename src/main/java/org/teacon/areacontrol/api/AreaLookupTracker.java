package org.teacon.areacontrol.api;

@FunctionalInterface
public interface AreaLookupTracker {

    void track(Area area, String property, Object rawValue);

    static final AreaLookupTracker NO_OP = (area, prop, rawValue) -> {};
}
