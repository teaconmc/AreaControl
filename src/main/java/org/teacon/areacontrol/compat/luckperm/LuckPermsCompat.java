package org.teacon.areacontrol.compat.luckperm;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuckPermsCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");

    public static void init() {
        try {
            LuckPerms theApi = LuckPermsProvider.get();
            theApi.getContextManager().registerCalculator(AreaControlContextCalculator.INSTANCE);
        } catch (IllegalStateException e) {
            LOGGER.warn("Failed to initialize LuckPerms compatibilities. Details: ", e);
        }
    }
}
