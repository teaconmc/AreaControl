package org.teacon.areacontrol.test;

import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.impl.persistence.AreaRepository;

import java.util.Collection;
import java.util.List;

public class InMemoryAreaRepository implements AreaRepository {

    public final List<Area> areas;

    public final Area virtualWild;

    public InMemoryAreaRepository(List<Area> areas, Area virtualWild) {
        this.areas = areas;
        this.virtualWild = virtualWild;
    }

    @Override
    public Collection<Area> load() {
        return List.copyOf(this.areas);
    }

    @Override
    public Area loadWildness() throws Exception {
        return this.virtualWild;
    }

    @Override
    public void remove(Area areaToRemove) {
        this.areas.remove(areaToRemove);
    }

    @Override
    public void save(Collection<Area> areas) {
        // No-op, this is in-memory
    }

    @Override
    public void saveWildness(Area rea) throws Exception {

    }
}
