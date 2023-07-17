package org.teacon.areacontrol.impl.command.selector;

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.Area;

import java.util.UUID;
import java.util.function.Predicate;

public class AreaSelectorOption  {

    public static final SimpleCommandExceptionType ERROR_INVALID_NAME_OR_UUID = new SimpleCommandExceptionType(Component.translatable("argument.entity.invalid"));

    public static void register() {
        EntitySelectorOptions.register("area", parser -> {
            // TODO Fix Suggestion, it somehow does not work
            //parser.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> {
            //    return SharedSuggestionProvider.suggest(AreaManager.INSTANCE.getKnownAreaNames(), suggestionsBuilder);
            //});
            var reader = parser.getReader();
            if (reader.canRead()) {
                boolean invert = false;
                if (reader.peek() == '!') {
                    reader.skip();
                    invert = true;
                }
                if (reader.canRead()) {
                    if (reader.peek() == '#') {
                        reader.skip();
                        Area area = AreaManager.INSTANCE.findBy(reader.readString());
                        if (area != null) {
                            parser.addPredicate(new InsideArea(area, invert));
                        } else {
                            throw ERROR_INVALID_NAME_OR_UUID.createWithContext(reader);
                        }
                    } else {
                        try {
                            UUID uid = UUID.fromString(reader.readString());
                            Area area = AreaManager.INSTANCE.findBy(uid);
                            if (area != null) {
                                parser.addPredicate(new InsideArea(area, invert));
                            }
                        } catch (Exception ignored) {
                            throw ERROR_INVALID_NAME_OR_UUID.createWithContext(reader);
                        }
                    }
                }
            }
        }, parser -> true, Component.literal("area"));
    }

    static final class InsideArea implements Predicate<Entity> {

        private final Area area;
        private final boolean invert;

        public InsideArea(Area area, boolean invert) {
            this.area = area;
            this.invert = invert;
        }

        @Override
        public boolean test(Entity entity) {
            var level = entity.level();
            if (level != null && this.area.dimension.equals(level.dimension().location().toString())) {
                return (this.area.minX <= entity.getX() && entity.getX() <= this.area.maxX
                        && this.area.minY <= entity.getY() && entity.getY() <= this.area.maxY
                        && this.area.minZ <= entity.getZ() && entity.getZ() <= this.area.maxZ)
                        ^ this.invert;
            }
            return !this.invert;
        }
    }
}
