package org.teacon.areacontrol.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaLookup;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public enum AreaLookupImpl implements AreaLookup {

    INSTANCE;

    private final Map<String, ResourceKey<Level>> cache = new HashMap<>();

    private ResourceKey<Level> getOrCreate(String dimKey) {
        return this.cache.computeIfAbsent(dimKey, k -> ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(k)));
    }

    @Override
    public Area findWildnessOf(String dimKey) {
        var dimResKey = this.getOrCreate(dimKey);
        return AreaManager.INSTANCE.findDefaultBy(dimResKey);
    }

    @Override
    public Area findBy(UUID areaUid) {
        return AreaManager.INSTANCE.findBy(areaUid);
    }

    @Override
    public Area findBy(String dimKey, double x, double y, double z) {
        var dimResKey = this.getOrCreate(dimKey);
        return AreaManager.INSTANCE.findBy(dimResKey, new BlockPos(x, y, z));
    }

    @Override
    public Area findBy(String dimKey, int x, int y, int z) {
        var dimResKey = this.getOrCreate(dimKey);
        return AreaManager.INSTANCE.findBy(dimResKey, new BlockPos(x, y, z));
    }
}
