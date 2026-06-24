package io.github.silentdevelopment.headdb.paper.command;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.format.RootFormatter;
import io.github.silentdevelopment.headdb.paper.command.subcommand.CategoriesCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.CollectionsCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.PlayerCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.EditCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.CustomCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.DebugCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.GiveCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.HelpCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.InfoCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.ItemCacheCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.OpenCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.RandomCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.RefreshCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.ReloadCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.StatusCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.TagsCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.VerifyCommand;
import io.github.silentdevelopment.headdb.paper.command.subcommand.search.SearchCommand;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.relay.command.CommandDefinition;
import io.github.silentdevelopment.relay.core.command.builder.CommandBuilder;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommandGroup;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public final class RootCommand extends AbstractPaperCommandGroup {

    private final HeadDBPlugin plugin;
    private final List<CommandDefinition<CommandSender>> children;

    public RootCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.children = List.of(
                new HelpCommand(plugin),
                new StatusCommand(plugin),
                new DebugCommand(plugin),
                new VerifyCommand(plugin),
                new RefreshCommand(plugin),
                new ReloadCommand(plugin),
                new GiveCommand(plugin),
                new PlayerCommand(plugin),
                new CustomCommand(plugin),
                new EditCommand(plugin),
                new RandomCommand(plugin),
                new InfoCommand(plugin),
                new OpenCommand(plugin),
                new CategoriesCommand(plugin),
                new TagsCommand(plugin),
                new CollectionsCommand(plugin),
                new ItemCacheCommand(plugin),
                new SearchCommand(plugin)
        );
    }

    @Override
    public @NotNull List<CommandDefinition<CommandSender>> children() {
        return children;
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        if (plugin.config().guiOpenMainCommand() && context.sender() instanceof Player player) {
            if (!Permissions.has(player, Permissions.OPEN) || !Permissions.has(player, Permissions.GUI_MAIN)) {
                context.reply(plugin.messages().render(context.sender(), io.github.silentdevelopment.headdb.paper.message.MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            plugin.guis().openMain(player);
            return;
        }

        CommandSender sender = context.source();

        for (Component line : RootFormatter.format(plugin, context.sender())) {
            sender.sendMessage(line);
        }
    }

    @Override
    protected @NotNull CommandBuilder<CommandSender> buildCommand() {
        return CommandBuilder.<CommandSender>literal("hdb")
                .description("HeadDB main command.")
                .alias("headdb")
                .suggestAliases(true)
                .noArgs();
    }
}