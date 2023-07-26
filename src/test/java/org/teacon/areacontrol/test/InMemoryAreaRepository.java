package org.teacon.areacontrol.test;

import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.impl.persistence.AreaRepository;

import java.util.Collection;
import java.util.List;

public class InMemoryAreaRepository implements AreaRepository {

    public final List<Area> areas;

    public InMemoryAreaRepository(List<Area> areas) {
        this.areas = areas;
    }

    @Override
    public Collection<Area> load() {
        return List.copyOf(this.areas);
    }

    @Override
    public void remove(Area areaToRemove) {
        this.areas.remove(areaToRemove);
    }

    @Override
    public void save(Collection<Area> areas) {
        // No-op, this is in-memory
    }
}
