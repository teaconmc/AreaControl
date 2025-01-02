package org.teacon.areacontrol.impl.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public enum AreaRepositoryManager {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");

    private final Map<String, AreaRepository.Factory> repoFactories = new HashMap<>();

    public void registerFactory(String type, AreaRepository.Factory factory) {
        var old = this.repoFactories.put(type, factory);
        if (old != null) {
            LOGGER.error("Duplicated AreaRepository.Factory type for '{}', was {} (type {}), now is {} (type {})",
                    type, old, old.getClass(), factory, factory.getClass());
            throw new IllegalArgumentException("Duplicated AreaRepository.Factory type for '" + type + "'");
        }
    }

    public AreaRepository create(String type, Path dataRootDir) {
        return this.repoFactories.get(type).createFrom(dataRootDir);
    }

    public static void init() {
        INSTANCE.registerFactory("json", JsonBasedAreaRepository::new);
        INSTANCE.registerFactory("toml", TomlBasedAreaRepository::new);
    }
}
