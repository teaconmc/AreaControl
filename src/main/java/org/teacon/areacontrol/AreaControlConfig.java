package org.teacon.areacontrol;

import net.minecraftforge.common.ForgeConfigSpec;

public class AreaControlConfig {

    public static ForgeConfigSpec.ConfigValue<String> persistenceMode;

    public static ForgeConfigSpec.ConfigValue<String> areaClaimTool;

    public static ForgeConfigSpec setup(ForgeConfigSpec.Builder configSpec) {
        persistenceMode = configSpec.comment("The format in which the area data are stored. Currently supports json and toml.")
                .translation("area_control.config.persistence_mode")
                .define("persistenceMode", "json");
        areaClaimTool = configSpec.comment("The item id of the item that should be used when marking areas for claiming. For example: minecraft:stick.")
                .translation("area_control.config.area_claim_tool")
                .define("areaClaimTool", "minecraft:stick");
        return configSpec.build();
    }
}
