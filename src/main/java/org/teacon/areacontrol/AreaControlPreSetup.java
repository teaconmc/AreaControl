package org.teacon.areacontrol;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.teacon.areacontrol.impl.command.arguments.AreaPropertyArgument;
import org.teacon.areacontrol.impl.command.arguments.DirectionArgument;
import org.teacon.areacontrol.impl.command.selector.AreaSelectorOption;

@Mod.EventBusSubscriber(modid = "area_control", bus = Mod.EventBusSubscriber.Bus.MOD)
public class AreaControlPreSetup {

    static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARG_TYPES = DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, "area_control");

    static final RegistryObject<ArgumentTypeInfo<AreaPropertyArgument, ?>> AREA_PROPERTY_ARG_TYPE = ARG_TYPES.register("area_property", () -> SingletonArgumentInfo.contextFree(AreaPropertyArgument::areaProperty));
    static final RegistryObject<ArgumentTypeInfo<DirectionArgument, ?>> DIRECTION_ARG_TYPE = ARG_TYPES.register("direction", () -> SingletonArgumentInfo.contextFree(DirectionArgument::direction));

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        ArgumentTypeInfos.registerByClass(AreaPropertyArgument.class, AREA_PROPERTY_ARG_TYPE.get());
        ArgumentTypeInfos.registerByClass(DirectionArgument.class, DIRECTION_ARG_TYPE.get());

        AreaSelectorOption.register();
    }
}
