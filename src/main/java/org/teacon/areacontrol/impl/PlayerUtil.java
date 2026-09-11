package org.teacon.areacontrol.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class PlayerUtil {

    public static void showOverlayMessageWithDebug(Player p, String key, Object... args) {
        if (p.getAbilities().instabuild) {
            // 如果玩家是创造模式，则显示带调试信息的版本。
            p.sendOverlayMessage(Component.translatable(key + ".debug", args));
        } else {
            // 否则，显示普通版本。
            p.sendOverlayMessage(Component.translatable(key, args));
        }
    }
}
