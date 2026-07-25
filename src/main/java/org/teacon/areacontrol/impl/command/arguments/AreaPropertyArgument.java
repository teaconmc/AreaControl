package org.teacon.areacontrol.impl.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.ModList;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Specialized version of {@link StringArgumentType#string()} that also
 * provides suggestions of all known area properties.
 */
public class AreaPropertyArgument implements ArgumentType<String> {

    private static final List<String> EXAMPLES = List.of(AreaProperties.ALLOW_BREAK, AreaProperties.ALLOW_USE_ITEM);
    // TODO Remove these hardcode
    private static final List<String> SUGGEST_BLOCKS = List.of(
            AreaProperties.ALLOW_BREAK, AreaProperties.ALLOW_PLACE_BLOCK, AreaProperties.ALLOW_ACTIVATE, AreaProperties.ALLOW_CLICK);
    private static final List<String> SUGGEST_ITEM = List.of(AreaProperties.ALLOW_USE_ITEM, AreaProperties.ALLOW_POSSESS);
    private static final List<String> SUGGEST_ENTITY = List.of(AreaProperties.ALLOW_PVE, AreaProperties.ALLOW_INTERACT_ENTITY,
            AreaProperties.ALLOW_SPAWN, AreaProperties.ALLOW_RIDE);
    private static final List<String> SUGGEST_EFFECT = List.of(AreaProperties.ALLOW_ACTIVE_EFFECT);

    public static AreaPropertyArgument areaProperty() {
        return new AreaPropertyArgument();
    }

    @Override
    public String parse(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        var current = builder.getRemainingLowerCase();
        for (var prop : AreaProperties.KNOWN_PROPERTIES) {
            if (current.startsWith(prop)) {
                if (SUGGEST_BLOCKS.contains(prop)) {
                    fillSuggestions(current, prop, BuiltInRegistries.BLOCK, builder);
                } else if (SUGGEST_ITEM.contains(prop)) {
                    fillSuggestions(current, prop, BuiltInRegistries.ITEM, builder);
                } else if (SUGGEST_ENTITY.contains(prop)) {
                    fillSuggestions(current, prop, BuiltInRegistries.ENTITY_TYPE, builder);
                } else if (SUGGEST_EFFECT.contains(prop)) {
                    fillSuggestions(current, prop, BuiltInRegistries.MOB_EFFECT, builder);
                }
            } else if (prop.startsWith(current)) {
                builder.suggest(prop);
            }
        }
        return builder.buildFuture();
    }

    private static void fillSuggestions(String current, String prop, Registry<?> registry, SuggestionsBuilder builder) {
        String sub = current.substring(prop.length());
        if (sub.startsWith(".")) {
            for (var mod : ModList.get().getMods()) {
                var modId = mod.getModId();
                if (("." + modId).startsWith(sub)) {
                    builder.suggest(prop + "." + modId);
                }
            }
            SharedSuggestionProvider.suggestResource(registry.keySet(), builder, prop + ".");
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

}
