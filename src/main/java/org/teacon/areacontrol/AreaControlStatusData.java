package org.teacon.areacontrol;

import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AreaControlStatusData {

    public boolean clientExtensionEnabled = false;

    public boolean globalBypassMode = false;

    public boolean wildnessBypassMode = false;

    public boolean verbose = false;

    public transient Area currentArea = null;

    public Set<UUID> areaIdsWithBypassModeOn = new HashSet<>();

    public Set<String> noTrackingPrefix = new HashSet<>(Set.of(AreaProperties.ALLOW_POSSESS, AreaProperties.ALLOW_ACTIVE_EFFECT, "move_in"));
}
