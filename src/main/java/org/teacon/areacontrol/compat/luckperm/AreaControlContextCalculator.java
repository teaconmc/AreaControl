package org.teacon.areacontrol.compat.luckperm;

import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.areacontrol.AreaControlPlayerTracker;

import java.util.Objects;

public enum AreaControlContextCalculator implements ContextCalculator<ServerPlayer> {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");

    private static final String CONTEXT_IS_WILDNESS = "area_control:wildness";
    private static final String CONTEXT_AREA_ID = "area_control:current_area_id";
    private static final String CONTEXT_AREA_NAME = "area_control:current_area_name";
    private static final String CONTEXT_OWNER = "area_control:owning_current_position";
    private static final String CONTEXT_ALLY = "area_control:ally_of_current_position";

    @Override
    public void calculate(@NonNull ServerPlayer target, @NonNull ContextConsumer consumer) {
        var uid = target.getGameProfile().getId();
        final var area = AreaControlPlayerTracker.INSTANCE.getCurrentAreaForPlayer(uid);
        if (area == null) {
            consumer.accept(CONTEXT_IS_WILDNESS, "true");
            return;
        }
        consumer.accept(CONTEXT_IS_WILDNESS, "false");
        consumer.accept(CONTEXT_AREA_ID, area.uid.toString());
        consumer.accept(CONTEXT_AREA_NAME, area.name);
        consumer.accept(CONTEXT_OWNER, Boolean.toString(area.owners.contains(uid)));
        consumer.accept(CONTEXT_ALLY, Boolean.toString(area.builders.contains(uid) || area.owners.contains(uid)));
        for (var prop : area.properties.entrySet()) {
            consumer.accept("area_control:property/" + prop.getKey(), Objects.toString(prop.getValue().toString()));
        }
    }
}
