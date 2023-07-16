package org.teacon.areacontrol.impl.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.teacon.areacontrol.api.AreaControlAPI;

import java.util.concurrent.CompletableFuture;

public class GroupArgument implements ArgumentType<String> {

    public static final SimpleCommandExceptionType ERROR_MIXED_TYPE = new SimpleCommandExceptionType(Component.translatable("area_control.argument.error.unknown_group"));

    public static GroupArgument group() {
        return new GroupArgument();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        // FIXME This forbids group name to contain whitespace (U+0020)
        int start = reader.getCursor();
        while(reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        var maybeGroupName = reader.getString().substring(start, reader.getCursor());
        if (AreaControlAPI.groupProvider.isValidGroup(maybeGroupName)) {
            return maybeGroupName;
        } else {
            throw ERROR_MIXED_TYPE.createWithContext(reader);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(AreaControlAPI.groupProvider.getGroups(), builder);
    }
}
