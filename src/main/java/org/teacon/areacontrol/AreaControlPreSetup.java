package org.teacon.areacontrol;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.teacon.areacontrol.impl.command.arguments.AreaPropertyArgument;
import org.teacon.areacontrol.impl.command.arguments.DirectionArgument;
import org.teacon.areacontrol.impl.command.arguments.GroupArgument;
import org.teacon.areacontrol.impl.command.selector.AreaSelectorOption;

@EventBusSubscriber(modid = "area_control", bus = EventBusSubscriber.Bus.MOD)
public class AreaControlPreSetup {

    static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARG_TYPES = DeferredRegister.create(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, "area_control");

    static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<AreaPropertyArgument>> AREA_PROPERTY_ARG_TYPE = ARG_TYPES.register("area_property", () -> SingletonArgumentInfo.contextFree(AreaPropertyArgument::areaProperty));
    static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<DirectionArgument>> DIRECTION_ARG_TYPE = ARG_TYPES.register("direction", () -> SingletonArgumentInfo.contextFree(DirectionArgument::direction));

    static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<GroupArgument>> GROUP_ARG_TYPE = ARG_TYPES.register("group", () -> SingletonArgumentInfo.contextFree(GroupArgument::group));

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        ArgumentTypeInfos.registerByClass(AreaPropertyArgument.class, AREA_PROPERTY_ARG_TYPE.get());
        ArgumentTypeInfos.registerByClass(DirectionArgument.class, DIRECTION_ARG_TYPE.get());
        ArgumentTypeInfos.registerByClass(GroupArgument.class, GROUP_ARG_TYPE.get());

        AreaSelectorOption.register();
    }
}
