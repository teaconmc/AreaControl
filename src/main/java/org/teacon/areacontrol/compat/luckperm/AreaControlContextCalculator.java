package org.teacon.areacontrol.compat.luckperm;

import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.teacon.areacontrol.AreaManager;

public enum AreaControlContextCalculator implements ContextCalculator<ServerPlayer> {

    INSTANCE;

    @Override
    public void calculate(@NonNull ServerPlayer target, @NonNull ContextConsumer consumer) {
        var pos = target.blockPosition();
        var dim = target.level.dimension();
        var area = AreaManager.INSTANCE.findBy(dim, pos);
        if (area.owner != null) {
            consumer.accept("area_control.owning_current_position", Boolean.toString(area.owner.equals(target.getGameProfile().getId())));
        }
        // TODO Add all of our things into LuckPerm context, gg
    }
}
