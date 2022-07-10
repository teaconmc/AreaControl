package org.teacon.areacontrol.compat.luckperm;

import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.teacon.areacontrol.AreaManager;

public enum AreaControlContextCalculator implements ContextCalculator<ServerPlayer> {

    INSTANCE;

    private static final String CONTEXT_OWNER = "area_control.owning_current_position";
    private static final String CONTEXT_ALLY = "area_control.ally_of_current_position";

    @Override
    public void calculate(@NonNull ServerPlayer target, @NonNull ContextConsumer consumer) {
        var uid = target.getGameProfile().getId();
        var pos = target.blockPosition();
        var dim = target.level.dimension();
        var area = AreaManager.INSTANCE.findBy(dim, pos);
        if (area.owner != null) {
            consumer.accept(CONTEXT_OWNER, Boolean.toString(uid.equals(area.owner)));
        }
        consumer.accept(CONTEXT_ALLY, Boolean.toString(area.friends.contains(uid) || uid.equals(area.owner)));
        // TODO Add all of our things into LuckPerm context, gg
    }
}
