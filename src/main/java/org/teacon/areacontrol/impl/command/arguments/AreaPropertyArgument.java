package org.teacon.areacontrol.impl.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
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
    // TODO Remove these hardcode
    private static final List<String> SUGGEST_BLOCKS = List.of(
            AreaProperties.ALLOW_BREAK, AreaProperties.ALLOW_PLACE_BLOCK, AreaProperties.ALLOW_ACTIVATE, AreaProperties.ALLOW_CLICK);
    private static final List<String> SUGGEST_ITEM = List.of(AreaProperties.ALLOW_USE_ITEM, "area.allow_possess");
    private static final List<String> SUGGEST_ENTITY = List.of(AreaProperties.ALLOW_PVE, AreaProperties.ALLOW_SPAWN, AreaProperties.ALLOW_RIDE);

    public static final ArgumentSerializer<AreaPropertyArgument> SERIALIZER = new EmptyArgumentSerializer<>(AreaPropertyArgument::new);

    public static AreaPropertyArgument areaProperty() {
        return new AreaPropertyArgument();
    }

    @Override
    public String parse(StringReader reader) {
        int start = reader.getCursor();
        while(reader.canRead() && reader.peek() != ' ') {
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
                    fillSuggestions(current, prop, ForgeRegistries.BLOCKS, builder);
                } else if (SUGGEST_ITEM.contains(prop)) {
                    fillSuggestions(current, prop, ForgeRegistries.ITEMS, builder);
                } else if (SUGGEST_ENTITY.contains(prop)) {
                    fillSuggestions(current, prop, ForgeRegistries.ENTITIES, builder);
                }
            } else if (prop.startsWith(current)) {
                builder.suggest(prop);
            }
        }
        return builder.buildFuture();
    }

    private static void fillSuggestions(String current, String prop, IForgeRegistry<?> registry, SuggestionsBuilder builder) {
        String sub = current.substring(prop.length());
        if (sub.startsWith(".")) {
            for (var mod : ModList.get().getMods()) {
                var modId = mod.getModId();
                if (("." + modId).startsWith(sub)) {
                    builder.suggest(prop + "." + modId);
                }
            }
            SharedSuggestionProvider.suggestResource(registry.getKeys(), builder, prop + ".");
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

}
