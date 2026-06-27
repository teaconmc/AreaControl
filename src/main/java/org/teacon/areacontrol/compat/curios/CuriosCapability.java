package org.teacon.areacontrol.compat.curios;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.NonNull;

public final class CuriosCapability {
    public static final EntityCapability<@NonNull ResourceHandler<@NonNull ItemResource>, Void> CURIO_INV = EntityCapability.createVoid(Identifier.fromNamespaceAndPath("curios", "item_handler"), ResourceHandler.asClass());
}
