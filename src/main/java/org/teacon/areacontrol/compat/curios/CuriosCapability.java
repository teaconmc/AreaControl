package org.teacon.areacontrol.compat.curios;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.items.IItemHandler;

public final class CuriosCapability {
    public static final EntityCapability<IItemHandler, Void> CURIO_INV = EntityCapability.createVoid(ResourceLocation.fromNamespaceAndPath("curios", "item_handler"), IItemHandler.class);
}
