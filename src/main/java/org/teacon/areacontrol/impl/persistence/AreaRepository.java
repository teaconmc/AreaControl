package org.teacon.areacontrol.impl.persistence;

import org.teacon.areacontrol.api.Area;

import java.nio.file.Path;
import java.util.Collection;

public interface AreaRepository {

    Collection<Area> load() throws Exception;

    void save(Collection<Area> areas) throws Exception;

    interface Factory {
        AreaRepository createFrom(Path dataRootDir);
    }
}
