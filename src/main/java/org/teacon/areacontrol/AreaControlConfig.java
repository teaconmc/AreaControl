package org.teacon.areacontrol;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

public class AreaControlConfig {

    public static ModConfigSpec.BooleanValue disableInSinglePlayer;
    public static ModConfigSpec.BooleanValue grantBypassToCreativeModePlayerOnLogin;

    public static ModConfigSpec.ConfigValue<String> persistenceMode;

    public static ModConfigSpec.ConfigValue<String> areaClaimTool;

    public static ModConfigSpec.ConfigValue<String> groupProvider;

    public static ModConfigSpec.BooleanValue allowBreakBlock, allowPlaceBlock, allowClickBlock, allowActivateBlock;
    public static ModConfigSpec.BooleanValue allowPossessItem, allowUseItem;
    public static ModConfigSpec.BooleanValue allowSpawnEntity, allowRideEntity, allowInteractEntity, allowPvP, allowEvP, allowPvE;
    public static ModConfigSpec.BooleanValue allowEntitySelectingFromParent;
    public static ModConfigSpec.BooleanValue allowEntitySelectingFromChild;
    public static ModConfigSpec.BooleanValue allowCBSelectingFromParent;
    public static ModConfigSpec.BooleanValue allowCBSelectingFromChild;
    public static ModConfigSpec.BooleanValue allowOpenSafe;
    public static ModConfigSpec.BooleanValue allowActiveEffect;

    public static ModConfigSpec setup(ModConfigSpec.Builder configSpec) {
        disableInSinglePlayer = configSpec.comment("Disable nearly all protection measures when in singleplayer.")
                .translation("area_control.config.disable_in_single_player")
                .define("disableInSinglePlayer", true);
        grantBypassToCreativeModePlayerOnLogin = configSpec.comment("If true, when a player log in, if that player is in creative mode, global bypass will be turned on automatically.")
                .translation("area_control.config.grant_bypass_to_creative_mode_player_on_login")
                .define("grantBypassToCreativeModePlayerOnLogin", true);
        persistenceMode = configSpec.comment("The format in which the area data are stored. Currently supports json and toml.")
                .translation("area_control.config.persistence_mode")
                .define("persistenceMode", "toml");
        areaClaimTool = configSpec.comment("The item id of the item that should be used when marking areas for claiming. For example: minecraft:stick.")
                .translation("area_control.config.area_claim_tool")
                .define("areaClaimTool", "minecraft:stick", input -> {
                    try {
                        Identifier.parse(input.toString());
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });
        groupProvider = configSpec.comment("Group provider used to provide 'groups'. Used in /ac current [claimer|builder] add group.")
                .translation("area_control.config.group_provider")
                .define("groupProvider", "vanilla");

        configSpec
                .translation("area_control.config.global_default_properties")
                .push("Default properties");
        allowBreakBlock = configSpec.comment("Default value for area.allow_break_block")
                .translation("area_control.config.global_default_properties.break_block")
                .define("allowBreakBlock", false);
        allowPlaceBlock = configSpec.comment("Default value for area.allow_place_block")
                .translation("area_control.config.global_default_properties.place_block")
                .define("allowPlaceBlock", false);
        allowActivateBlock = configSpec.comment("Default value for area.allow_activate_block")
                .translation("area_control.config.global_default_properties.activate_block")
                .define("allowActivateBlock", true);
        allowClickBlock = configSpec.comment("Default value for area.allow_click_block")
                .translation("area_control.config.global_default_properties.click_block")
                .define("allowClickBlock", true);
        allowPossessItem = configSpec
                .translation("area_control.config.global_default_properties.possess")
                .define("allowPossessItem", true);
        allowUseItem = configSpec
                .translation("area_control.config.global_default_properties.use_item")
                .define("allowUseItem", true);
        allowSpawnEntity = configSpec
                .translation("area_control.config.global_default_properties.spawn")
                .define("allowSpawnEntity", true);
        allowRideEntity = configSpec
                .translation("area_control.config.global_default_properties.ride")
                .define("allowRideEntity", true);
        allowInteractEntity = configSpec
                .translation("area_control.config.global_default_properties.interact")
                .define("allowInteractEntity", true);
        allowPvP = configSpec
                .translation("area_control.config.global_default_properties.pvp")
                .define("allowPvP", false);
        allowEvP = configSpec
                .translation("area_control.config.global_default_properties.evp")
                .define("allowEvP", false);
        allowPvE = configSpec
                .translation("area_control.config.global_default_properties.attack")
                .define("allowPvE", false);
        allowEntitySelectingFromParent = configSpec.comment("Default value for select_from_parent_area_by_entity")
                .translation("area_control.config.global_default_properties.select_from_parent_area_by_entity")
                .define("allowEntityUseEntitySelectorToSelectEntitiesFromParentArea", true);
        allowEntitySelectingFromChild = configSpec.comment("Default value for select_from_child_area_by_entity")
                .translation("area_control.config.global_default_properties.select_from_child_area_by_entity")
                .define("allowEntityUseEntitySelectorToSelectEntitiesFromChildArea", true);
        allowCBSelectingFromParent = configSpec.comment("Default value for select_from_parent_area_by_command_block")
                .translation("area_control.config.global_default_properties.select_from_parent_area_by_command_block")
                .define("allowCommandBlockUseEntitySelectorToSelectEntitiesFromParentArea", true);
        allowCBSelectingFromChild = configSpec.comment("Default value for select_from_child_area_by_command_block")
                .translation("area_control.config.global_default_properties.select_from_child_area_by_command_block")
                .define("allowCommandBlockUseEntitySelectorToSelectEntitiesFromChildArea", true);
        allowOpenSafe = configSpec.comment("Default value for open_safe")
                .define("allowOpenSafe", true);
        allowActiveEffect = configSpec.comment("Default value for active_effect")
                .define("allowActiveEffect", true);
        configSpec.pop();
        return configSpec.build();
    }
}
