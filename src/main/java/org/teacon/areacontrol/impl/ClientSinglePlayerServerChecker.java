package org.teacon.areacontrol.impl;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import org.teacon.areacontrol.AreaControlConfig;

import java.util.function.Predicate;

public class ClientSinglePlayerServerChecker implements Predicate<MinecraftServer> {
    @Override
    public boolean test(MinecraftServer s) {
        return AreaControlConfig.disableInSinglePlayer.get() && s instanceof IntegratedServer intS && !intS.isPublished();
    }
}
