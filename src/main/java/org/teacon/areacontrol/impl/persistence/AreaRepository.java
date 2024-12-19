package org.teacon.areacontrol.impl.persistence;

import org.teacon.areacontrol.api.Area;

import java.nio.file.Path;
import java.util.Collection;

public interface AreaRepository {

    Collection<Area> load() throws Exception;

    Area loadWildness() throws Exception;

    void remove(Area areaToRemove) throws Exception;

    void save(Collection<Area> areas) throws Exception;

    void saveWildness(Area area) throws Exception;

    interface Factory {
        AreaRepository createFrom(Path dataRootDir, Path globalConfigDir);
    }
}
