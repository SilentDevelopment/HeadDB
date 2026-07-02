package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.command.Suggestions;
import io.github.silentdevelopment.headdb.paper.command.format.HeadInfoFormatter;
import io.github.silentdevelopment.headdb.paper.command.search.SearchParser;
import io.github.silentdevelopment.headdb.paper.item.HeadItemIds;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.relay.argument.Argument;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.argument.PaperArgumentTypes;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public final class InfoCommand extends AbstractPaperCommand {

    private static final Argument<String> ID = Argument.optional("id", PaperArgumentTypes.STRING);

    private final HeadDBPlugin plugin;

    public InfoCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        if (!Permissions.has(context.sender(), Permissions.INFO)) {
            context.reply(plugin.messages().render(context.sender(), MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        Optional<HeadId> optionalId = resolveId(context);
        if (optionalId.isEmpty()) {
            return;
        }

        HeadId id = optionalId.get();
        plugin.headRegistry().resolve(id).whenComplete((head, throwable) -> plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            if (throwable != null || head.isEmpty()) {
                context.reply(plugin.messages().unknownHead(context.sender(), id));
                return;
            }

            Head resolvedHead = head.get();
            if (!Permissions.canViewCategory(context.sender(), resolvedHead.category())) {
                context.reply(plugin.messages().render(context.sender(), MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            for (Component line : HeadInfoFormatter.format(resolvedHead)) {
                context.reply(line);
            }
        }));
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("info")
                .alias("i")
                .description("Shows information about a head or the held head.")
                .requirement(CommandRequirements.permission(Permissions.INFO))
                .signature(ID)
                .suggest(ID, Suggestions.headIds(plugin))
                .noArgs()
                .build();
    }

    private @NotNull Optional<HeadId> resolveId(@NotNull PaperCommandContext context) {
        if (context.has(ID)) {
            return parseArgument(context, context.get(ID));
        }

        if (!context.isPlayer()) {
            context.reply(plugin.messages().infoConsoleUsage(context.sender()));
            return Optional.empty();
        }

        Player player = context.player();
        ItemStack item = player.getInventory().getItemInMainHand();
        Optional<HeadId> itemId = HeadItemIds.read(plugin, item);

        if (itemId.isPresent()) {
            return itemId;
        }

        context.reply(plugin.messages().heldHeadRequired(context.sender()));
        return Optional.empty();
    }

    private @NotNull Optional<HeadId> parseArgument(@NotNull PaperCommandContext context, @NotNull String raw) {
        try {
            return Optional.of(SearchParser.headId(raw));
        } catch (IllegalArgumentException exception) {
            context.reply(plugin.messages().invalidArgument(context.sender(), exception.getMessage()));
            return Optional.empty();
        }
    }
}