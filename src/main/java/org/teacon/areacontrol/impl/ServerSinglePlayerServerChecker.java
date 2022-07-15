package org.teacon.areacontrol.impl;

import net.minecraft.server.MinecraftServer;

import java.util.function.Predicate;

public class ServerSinglePlayerServerChecker implements Predicate<MinecraftServer> {
    @Override
    public boolean test(MinecraftServer minecraftServer) {
        // There is no way that a dedicated server can be a single-player instance on physical client.
        return false;
    }
}
