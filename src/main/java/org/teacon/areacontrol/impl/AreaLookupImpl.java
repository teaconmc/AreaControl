package org.teacon.areacontrol.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaLookup;

import java.util.UUID;

public enum AreaLookupImpl implements AreaLookup {

    INSTANCE;

    @Override
    public Area findWildnessOf(String dimKey) {
        var dimResKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(dimKey));
        return AreaManager.INSTANCE.findDefaultBy(dimResKey);
    }

    @Override
    public Area findBy(UUID areaUid) {
        return AreaManager.INSTANCE.findBy(areaUid);
    }

    @Override
    public Area findBy(String dimKey, double x, double y, double z) {
        var dimResKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(dimKey));
        return AreaManager.INSTANCE.findBy(dimResKey, new BlockPos(x, y, z));
    }

    @Override
    public Area findBy(String dimKey, int x, int y, int z) {
        var dimResKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(dimKey));
        return AreaManager.INSTANCE.findBy(dimResKey, new BlockPos(x, y, z));
    }
}
