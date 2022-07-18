package org.teacon.areacontrol.impl;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Specialized version of {@link StringArgumentType#string()} that also
 * provides suggestions of all known area properties.
 */
public class AreaPropertyArgument implements ArgumentType<String> {

    private static final List<String> EXAMPLES = List.of("area.allow_break", "area.allow_use_item");

    public static final ArgumentSerializer<AreaPropertyArgument> SERIALIZER = new EmptyArgumentSerializer<>(AreaPropertyArgument::new);

    public static AreaPropertyArgument areaProperty() {
        return new AreaPropertyArgument();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        var current = builder.getRemainingLowerCase();
        for (var prop : AreaProperties.KNOWN_PROPERTIES) {
            if (prop.startsWith(current)) {
                builder.suggest(prop);
            }
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

}
