package org.teacon.areacontrol.impl.seizer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

@Mod("area_control") // Yes, NeoForge allows multiple @Mod annotation for the same mod!
public class AreaControlBorderControl {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES
            = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "area_control");

    public static final DeferredHolder<AttachmentType<?>, @NotNull AttachmentType<ConfiscationInv>> CONFISCATION_INV
            = ATTACHMENT_TYPES.register("confiscation_inv", () -> AttachmentType.serializable(ConfiscationInv::new).copyOnDeath().build());

    public AreaControlBorderControl(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
