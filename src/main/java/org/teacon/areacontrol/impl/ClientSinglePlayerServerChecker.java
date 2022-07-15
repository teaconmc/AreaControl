package org.teacon.areacontrol.impl;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;

import java.util.function.Predicate;

public class ClientSinglePlayerServerChecker implements Predicate<MinecraftServer> {
    @Override
    public boolean test(MinecraftServer s) {
        return s instanceof IntegratedServer intS && !intS.isPublished();
    }
}
