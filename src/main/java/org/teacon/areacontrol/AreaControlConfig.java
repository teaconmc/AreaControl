package org.teacon.areacontrol;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

public class AreaControlConfig {

    public static ForgeConfigSpec.ConfigValue<String> persistenceMode;

    public static ForgeConfigSpec.ConfigValue<String> areaClaimTool;

    public static ForgeConfigSpec setup(ForgeConfigSpec.Builder configSpec) {
        persistenceMode = configSpec.comment("The format in which the area data are stored. Currently supports json and toml.")
                .translation("area_control.config.persistence_mode")
                .define("persistenceMode", "toml");
        areaClaimTool = configSpec.comment("The item id of the item that should be used when marking areas for claiming. For example: minecraft:stick.")
                .translation("area_control.config.area_claim_tool")
                .define("areaClaimTool", "minecraft:stick", input -> {
                    try {
                        new ResourceLocation(input.toString());
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });
        return configSpec.build();
    }
}
