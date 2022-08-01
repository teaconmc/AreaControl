package org.teacon.areacontrol.impl;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.teacon.areacontrol.mixin.LecternContainerAccessor;

public class Bridge {

    public static LecternBlockEntity fromAnonymousContainer(Container c) {
        if (c instanceof LecternContainerAccessor cWithAccess) {
            return cWithAccess.getThis$0();
        } else {
            return null;
        }
    }
}
