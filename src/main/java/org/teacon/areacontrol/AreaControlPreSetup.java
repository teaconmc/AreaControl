package org.teacon.areacontrol;

import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.teacon.areacontrol.impl.AreaPropertyArgument;
import org.teacon.areacontrol.impl.DirectionArgument;
import org.teacon.areacontrol.impl.command.selector.AreaSelectorOption;

@Mod.EventBusSubscriber(modid = "area_control", bus = Mod.EventBusSubscriber.Bus.MOD)
public class AreaControlPreSetup {

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        ArgumentTypes.register("area_control:area_property", AreaPropertyArgument.class, AreaPropertyArgument.SERIALIZER);
        ArgumentTypes.register("area_control:direction", DirectionArgument.class, DirectionArgument.SERIALIZER);

        AreaSelectorOption.register();
    }
}
