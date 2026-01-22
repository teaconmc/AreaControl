package org.teacon.areacontrol.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaControlAPI;
import org.teacon.areacontrol.api.AreaLookup;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public enum AreaLookupImpl implements AreaLookup {

    INSTANCE;

    private final Map<String, ResourceKey<Level>> cache = new HashMap<>();

    private ResourceKey<Level> getOrCreate(String dimKey) {
        return this.cache.computeIfAbsent(dimKey, k -> ResourceKey.create(Registries.DIMENSION, Identifier.parse(k)));
    }

    @Override
    public Area findBy(UUID areaUid) {
        return AreaManager.INSTANCE.findBy(areaUid);
    }

    @Override
    public Area findBy(String dimKey, double x, double y, double z) {
        return AreaManager.INSTANCE.findBy(this.getOrCreate(dimKey), new BlockPos((int) x, (int) y, (int) z));
    }

    @Override
    public Area findBy(String dimKey, int x, int y, int z) {
        return AreaManager.INSTANCE.findBy(this.getOrCreate(dimKey), new BlockPos(x, y, z));
    }

    @Override
    public @NotNull Area findWildness() {
        return findBy(AreaControlAPI.WILDNESS);
    }
}
