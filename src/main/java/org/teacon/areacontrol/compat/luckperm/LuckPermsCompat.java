package org.teacon.areacontrol.compat.luckperm;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.areacontrol.AreaControlConfig;
import org.teacon.areacontrol.api.AreaControlAPI;

public class LuckPermsCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");

    public static void init() {
        try {
            LuckPerms theApi = LuckPermsProvider.get();
            theApi.getContextManager().registerCalculator(AreaControlContextCalculator.INSTANCE);
            var groupProviderConfig = AreaControlConfig.groupProvider.get();
            if ("luckperms".equalsIgnoreCase(groupProviderConfig) || "luckperm".equalsIgnoreCase(groupProviderConfig)) {
                AreaControlAPI.groupProvider = LuckPermBackedGroupProvider.INSTANCE;
            }
        } catch (IllegalStateException e) {
            LOGGER.warn("Failed to initialize LuckPerms compatibilities. Details: ", e);
        }
    }
}
