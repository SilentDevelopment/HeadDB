package io.github.silentdevelopment.headdb.paper.command;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.sound.SoundKey;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.command.Signature;
import io.github.silentdevelopment.relay.paper.text.PaperCommandResponseRenderer;
import io.github.silentdevelopment.relay.paper.text.usage.PaperUsageComponentRenderer;
import io.github.silentdevelopment.relay.paper.text.usage.StyledPaperUsageComponentRenderer;
import io.github.silentdevelopment.relay.text.CommandText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public final class CommandResponseRenderer extends PaperCommandResponseRenderer {

    private final HeadDBPlugin plugin;
    private final PaperUsageComponentRenderer usageRenderer;

    public CommandResponseRenderer(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.usageRenderer = new StyledPaperUsageComponentRenderer(false);
    }

    @Override
    public @NotNull Component renderUnknownCommand(@NotNull CommandSender source) {
        play(source, SoundKey.INVALID);
        return plugin.messages().relayUnknownCommand(source);
    }

    @Override
    public @NotNull Component renderNoHandler(@NotNull CommandSender source) {
        play(source, SoundKey.INVALID);
        return plugin.messages().relayNoHandler(source);
    }

    @Override
    public @NotNull Component renderRequirementFailure(@NotNull CommandSender source, @NotNull CommandText message) {
        play(source, SoundKey.NO_PERMISSION);
        return plugin.messages().render(source, message);
    }

    @Override
    public @NotNull Component renderAbort(@NotNull CommandSender source, @NotNull CommandText message) {
        play(source, SoundKey.INVALID);
        return plugin.messages().render(source, message);
    }

    @Override
    public @NotNull Component renderInvalidUsage(@NotNull CommandSender source, @NotNull CommandText message, @NotNull List<String> usages) {
        play(source, SoundKey.INVALID);
        Component component = plugin.messages().relayInvalidUsage(source, message);

        if (usages.isEmpty()) {
            return component;
        }

        component = component.append(Component.newline()).append(plugin.messages().relayValidUsages(source));

        for (String usage : usages) {
            component = component.append(Component.newline())
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(usage(source, usage));
        }

        return component;
    }

    @Override
    public @NotNull Component renderInvalidUsage(@NotNull CommandSender source, @NotNull CommandText message, @NotNull String path, @NotNull Command command) {
        play(source, SoundKey.INVALID);
        Component component = plugin.messages().relayInvalidUsage(source, message);

        if (command.signatures().isEmpty()) {
            return component;
        }

        component = component.append(Component.newline()).append(plugin.messages().relayValidUsages(source));

        for (Signature signature : command.signatures()) {
            component = component.append(Component.newline())
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(usageRenderer.render(path, command, signature));
        }

        return component;
    }

    private void play(@NotNull CommandSender source, @NotNull SoundKey key) {
        if (!(source instanceof Player player)) {
            return;
        }

        plugin.sounds().play(player, key);
    }

    private static @NotNull Component usage(@NotNull CommandSender source, @NotNull String usage) {
        Component component = Component.text(usage, NamedTextColor.GREEN);

        if (!(source instanceof Player)) {
            return component;
        }

        return component.clickEvent(ClickEvent.suggestCommand(usage));
    }
}