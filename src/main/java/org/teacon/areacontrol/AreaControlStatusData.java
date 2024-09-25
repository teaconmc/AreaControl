package org.teacon.areacontrol;

import org.teacon.areacontrol.api.Area;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AreaControlStatusData {

    public boolean clientExtensionEnabled = false;

    public boolean globalBypassMode = false;

    public boolean wildnessBypassMode = false;

    public transient Area currentArea = null;

    public Set<UUID> areaIdsWithBypassModeOn = new HashSet<>();
}
