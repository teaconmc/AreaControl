package org.teacon.areacontrol;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

public class AreaControlConfig {

    public static ForgeConfigSpec.BooleanValue disableInSinglePlayer;

    public static ForgeConfigSpec.ConfigValue<String> persistenceMode;

    public static ForgeConfigSpec.ConfigValue<String> areaClaimTool;

    public static ForgeConfigSpec.ConfigValue<String> groupProvider;

    public static ForgeConfigSpec.BooleanValue allowBreakBlock, allowPlaceBlock, allowClickBlock, allowActivateBlock;
    public static ForgeConfigSpec.BooleanValue allowPossessItem, allowUseItem;
    public static ForgeConfigSpec.BooleanValue allowSpawnEntity, allowRideEntity, allowInteractEntity, allowPvP, allowPvE;
    public static ForgeConfigSpec.BooleanValue allowEntitySelectingFromParent;
    public static ForgeConfigSpec.BooleanValue allowEntitySelectingFromChild;
    public static ForgeConfigSpec.BooleanValue allowCBSelectingFromParent;
    public static ForgeConfigSpec.BooleanValue allowCBSelectingFromChild;

    public static ForgeConfigSpec setup(ForgeConfigSpec.Builder configSpec) {
        disableInSinglePlayer = configSpec.comment("Disable nearly all protection measures when in singleplayer.")
                .translation("area_control.config.disable_in_single_player")
                .define("disableInSinglePlayer", true);
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
        groupProvider = configSpec.comment("Group provider used to provide 'groups'. Used in /ac current [claimer|builder] add group.")
                .translation("area_control.config.group_provider")
                .define("groupProvider", "vanilla");

        configSpec.push("Default properties");
        allowBreakBlock = configSpec.comment("Default value for area.allow_break_block")
                        .define("allowBreakBlock", false);
        allowPlaceBlock = configSpec.comment("Default value for area.allow_place_block")
                .define("allowPlaceBlock", false);
        allowActivateBlock = configSpec.comment("Default value for area.allow_activate_block")
                .define("allowActivateBlock", true);
        allowClickBlock = configSpec.comment("Default value for area.allow_click_block")
                .define("allowClickBlock", true);
        allowPossessItem = configSpec.define("allowPossessItem", true);
        allowUseItem = configSpec.define("allowUseItem", true);
        allowSpawnEntity = configSpec.define("allowSpawnEntity", true);
        allowRideEntity = configSpec.define("allowRideEntity", true);
        allowInteractEntity = configSpec.define("allowInteractEntity", true);
        allowPvP = configSpec.define("allowPvP", false);
        allowPvE = configSpec.define("allowPvE", false);
        allowEntitySelectingFromParent = configSpec.comment("Default value for area.allow_select_from_parent_area_by_entity")
                        .define("allowEntityUseEntitySelectorToSelectEntitiesFromParentArea", true);
        allowEntitySelectingFromChild = configSpec.comment("Default value for area.allow_select_from_child_area_by_entity")
                        .define("allowEntityUseEntitySelectorToSelectEntitiesFromChildArea", true);
        allowCBSelectingFromParent = configSpec.comment("Default value for area.allow_select_from_parent_area_by_command_block")
                        .define("allowCommandBlockUseEntitySelectorToSelectEntitiesFromParentArea", true);
        allowCBSelectingFromChild = configSpec.comment("Default value for area.allow_select_from_child_area_by_command_block")
                        .define("allowCommandBlockUseEntitySelectorToSelectEntitiesFromChildArea", true);
        configSpec.pop();
        return configSpec.build();
    }
}
