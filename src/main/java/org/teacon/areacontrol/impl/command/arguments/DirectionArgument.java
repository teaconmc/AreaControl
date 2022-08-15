package org.teacon.areacontrol.impl.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class DirectionArgument implements ArgumentType<Direction> {

    public static final ArgumentSerializer<DirectionArgument> SERIALIZER = new EmptyArgumentSerializer<>(DirectionArgument::new);

    private static final Dynamic2CommandExceptionType INVALID_ENUM = new Dynamic2CommandExceptionType(
            (found, unused) -> new TranslatableComponent("area_control.error.direction_argument", found));

    private static final List<String> EXAMPLES = List.of("up", "down", "north", "south", "west", "east");

    public static DirectionArgument direction() {
        return new DirectionArgument();
    }

    @Override
    public Direction parse(StringReader reader) throws CommandSyntaxException {
        var input = reader.readString();
        try {
            return Direction.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw INVALID_ENUM.createWithContext(reader, input, null);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (String s : EXAMPLES) {
            builder.suggest(s);
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
