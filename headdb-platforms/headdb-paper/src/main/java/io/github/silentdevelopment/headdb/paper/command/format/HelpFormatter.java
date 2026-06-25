package io.github.silentdevelopment.headdb.paper.command.format;

import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class HelpFormatter {

    private static final String LINE = "────────────────────────";

    private HelpFormatter() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull List<Component> format(@NotNull CommandSender sender) {
        Objects.requireNonNull(sender, "sender");

        List<Component> lines = new ArrayList<>();

        lines.add(Component.empty());
        lines.add(header());
        lines.add(subtitle());
        lines.add(grammarLegend());

        addSection(lines, sender, new HelpSection("General", List.of(
                new HelpEntry(List.of("help"), "h", List.of(), "/hdb help", "Show this command reference.", Permissions.HELP),
                new HelpEntry(List.of("version"), null, List.of(), "/hdb version", "Show version and build information.", Permissions.VERSION),
                new HelpEntry(List.of("open"), "o", List.of(), "/hdb open", "Open the main HeadDB GUI.", Permissions.OPEN)
        )));

        addSection(lines, sender, new HelpSection("Heads", List.of(
                new HelpEntry(List.of("info"), "i", args(optional("id")), "/hdb info ", "Inspect a head by ID or held item.", Permissions.INFO),
                new HelpEntry(List.of("give"), "g", args(required("id"), optional("player"), optional("amount")), "/hdb give ", "Give a HeadDB item.", Permissions.GIVE),
                new HelpEntry(List.of("player"), "p", args(required("name|uuid"), optional("player"), optional("amount")), "/hdb player ", "Give a player head.", Permissions.PLAYER),
                new HelpEntry(List.of("random"), "rnd", args(optional("amount"), optional("category"), optional("player")), "/hdb random ", "Give a random HeadDB item.", Permissions.GIVE)
        )));

        addSection(lines, sender, new HelpSection("Browse", List.of(
                new HelpEntry(List.of("open"), "o", args(required("category"), optional("player")), "/hdb open ", "Open a category GUI.", Permissions.OPEN),
                new HelpEntry(List.of("categories"), "cat/cats", args(optional("page")), "/hdb categories ", "List available categories.", Permissions.SEARCH),
                new HelpEntry(List.of("tags"), "t", args(optional("query"), optional("page")), "/hdb tags ", "List or search tags.", Permissions.SEARCH),
                new HelpEntry(List.of("collections"), "col/cols", args(optional("query"), optional("page")), "/hdb collections ", "List or search collections.", Permissions.SEARCH)
        )));

        addSection(lines, sender, new HelpSection("Search", List.of(
                new HelpEntry(List.of("search"), "s", args(required("query")), "/hdb search ", "Search heads by text.", Permissions.SEARCH),
                new HelpEntry(List.of("search", "text"), "s text", args(required("query")), "/hdb search text ", "Search text explicitly.", Permissions.SEARCH),
                new HelpEntry(List.of("search", "head"), "s head", args(required("id")), "/hdb search head ", "Search one head by ID.", Permissions.SEARCH),
                new HelpEntry(List.of("search", "tag"), "s tag", args(required("tag")), "/hdb search tag ", "Search heads by tag.", Permissions.SEARCH),
                new HelpEntry(List.of("search", "category"), "s category", args(required("category")), "/hdb search category ", "Search heads by category.", Permissions.SEARCH),
                new HelpEntry(List.of("search", "collection"), "s collection", args(required("collection")), "/hdb search collection ", "Search heads by collection.", Permissions.SEARCH)
        )));


        addSection(lines, sender, new HelpSection("Local", List.of(
                new HelpEntry(List.of("custom", "list"), null, args(optional("page")), "/hdb custom list", "List local custom heads.", Permissions.CUSTOM_LIST),
                new HelpEntry(List.of("custom", "create"), null, args(required("id"), required("texture"), optional("name")), "/hdb custom create ", "Create a local custom head.", Permissions.CUSTOM_CREATE),
                new HelpEntry(List.of("custom", "createheld"), null, args(required("id"), optional("name")), "/hdb custom createheld ", "Create a custom head from your held head.", Permissions.CUSTOM_CREATE),
                new HelpEntry(List.of("custom", "give"), null, args(required("id"), optional("player"), optional("amount")), "/hdb custom give ", "Give a local custom head.", Permissions.CUSTOM_GIVE),
                new HelpEntry(List.of("edit"), null, args(required("remote-id"), required("action"), optional("value")), "/hdb edit ", "Edit local metadata overrides for remote heads.", Permissions.EDIT)
        )));

        addSection(lines, sender, new HelpSection("Database", List.of(
                new HelpEntry(List.of("status"), "st", List.of(), "/hdb status", "Show database state and counts.", Permissions.STATUS),
                new HelpEntry(List.of("debug"), "d", List.of(), "/hdb debug", "Show detailed runtime diagnostics.", Permissions.DEBUG),
                new HelpEntry(List.of("verify"), "v", List.of(), "/hdb verify", "Verify the public remote without replacing the active database.", Permissions.VERIFY),
                new HelpEntry(List.of("refresh"), "ref", List.of(), "/hdb refresh", "Fetch the latest remote database.", Permissions.REFRESH),
                new HelpEntry(List.of("reload"), "rl", List.of(), "/hdb reload", "Reload config, messages, and runtime.", Permissions.RELOAD)
        )));

        addSection(lines, sender, new HelpSection("Admin", List.of(
                new HelpEntry(List.of("update"), null, List.of(), "/hdb update", "Check for and download the latest version.", Permissions.UPDATE),
                new HelpEntry(List.of("itemcache", "clear"), "ic clear", List.of(), "/hdb itemcache clear", "Clear generated item cache.", Permissions.ITEM_CACHE)
        )));

        lines.add(Component.empty());
        lines.add(footer());
        lines.add(Component.empty());

        return List.copyOf(lines);
    }

    private static void addSection(@NotNull List<Component> lines, @NotNull CommandSender sender, @NotNull HelpSection section) {
        List<HelpEntry> visibleEntries = section.entries().stream().filter(entry -> canUse(sender, entry.permission())).toList();

        if (visibleEntries.isEmpty()) {
            return;
        }

        lines.add(Component.empty());
        lines.add(sectionTitle(section.title()));

        for (HelpEntry entry : visibleEntries) {
            lines.add(command(sender, entry));
        }
    }

    private static boolean canUse(@NotNull CommandSender sender, @Nullable String permission) {
        if (permission == null || permission.isBlank()) {
            return true;
        }

        return Permissions.has(sender, permission);
    }

    private static @NotNull Component header() {
        return Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(Component.text(LINE.substring(0, 7), NamedTextColor.DARK_GRAY))
                .append(Component.text(" HeadDB ", NamedTextColor.RED))
                .append(Component.text("Commands", NamedTextColor.GOLD))
                .append(Component.text(" " + LINE.substring(0, 7), NamedTextColor.DARK_GRAY));
    }

    private static @NotNull Component subtitle() {
        return Component.text("  Click commands to suggest them.", NamedTextColor.DARK_GRAY);
    }

    private static @NotNull Component grammarLegend() {
        return Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(Component.text("()", NamedTextColor.BLUE))
                .append(Component.text(" = alias", NamedTextColor.DARK_GRAY))
                .append(Component.text(" · ", NamedTextColor.DARK_GRAY))
                .append(Component.text("<>", NamedTextColor.RED))
                .append(Component.text(" = required", NamedTextColor.DARK_GRAY))
                .append(Component.text(" · ", NamedTextColor.DARK_GRAY))
                .append(Component.text("[]", NamedTextColor.AQUA))
                .append(Component.text(" = optional", NamedTextColor.DARK_GRAY));
    }

    private static @NotNull Component sectionTitle(@NotNull String title) {
        return Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(Component.text(title.toUpperCase(), NamedTextColor.RED))
                .append(Component.text(" " + LINE.substring(0, 16), NamedTextColor.DARK_GRAY));
    }

    private static @NotNull Component command(@NotNull CommandSender sender, @NotNull HelpEntry entry) {
        Component usage = usage(entry);
        Component interactiveUsage = interactive(sender, usage, entry.suggestion());

        return Component.text("  │ ", NamedTextColor.DARK_GRAY)
                .append(interactiveUsage)
                .append(Component.text("  -  ", NamedTextColor.DARK_GRAY))
                .append(Component.text(entry.description(), NamedTextColor.GRAY));
    }

    private static @NotNull Component usage(@NotNull HelpEntry entry) {
        Component component = Component.text("/hdb", NamedTextColor.GOLD);

        for (String commandPart : entry.commandParts()) {
            component = component.append(Component.text(" " + commandPart, NamedTextColor.GOLD));
        }

        if (entry.alias() != null && !entry.alias().isBlank()) {
            component = component.append(Component.text(" (" + entry.alias() + ")", NamedTextColor.BLUE));
        }

        for (HelpArgument argument : entry.arguments()) {
            component = component.append(Component.text(" "));
            component = component.append(argument(argument));
        }

        return component;
    }

    private static @NotNull Component argument(@NotNull HelpArgument argument) {
        if (argument.kind() == ArgumentKind.REQUIRED) {
            return Component.text("<" + argument.name() + ">", NamedTextColor.RED);
        }

        return Component.text("[" + argument.name() + "]", NamedTextColor.AQUA);
    }

    private static @NotNull Component interactive(@NotNull CommandSender sender, @NotNull Component component, @NotNull String suggestion) {
        if (!(sender instanceof Player)) {
            return component;
        }

        return component.clickEvent(ClickEvent.suggestCommand(suggestion))
                .hoverEvent(HoverEvent.showText(Component.text("Click to suggest " + suggestion, NamedTextColor.GRAY)));
    }

    private static @NotNull Component footer() {
        return Component.text("  ", NamedTextColor.DARK_GRAY).append(Component.text(LINE, NamedTextColor.DARK_GRAY));
    }

    private static @NotNull List<HelpArgument> args(@NotNull HelpArgument... arguments) {
        return List.copyOf(Arrays.asList(arguments));
    }

    private static @NotNull HelpArgument required(@NotNull String name) {
        return new HelpArgument(name, ArgumentKind.REQUIRED);
    }

    private static @NotNull HelpArgument optional(@NotNull String name) {
        return new HelpArgument(name, ArgumentKind.OPTIONAL);
    }

    private record HelpSection(@NotNull String title, @NotNull List<HelpEntry> entries) {

        private HelpSection {
            Objects.requireNonNull(title, "title");
            entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        }
    }

    private record HelpEntry(@NotNull List<String> commandParts, @Nullable String alias, @NotNull List<HelpArgument> arguments, @NotNull String suggestion, @NotNull String description, @Nullable String permission) {

        private HelpEntry {
            commandParts = List.copyOf(Objects.requireNonNull(commandParts, "commandParts"));
            arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
            Objects.requireNonNull(suggestion, "suggestion");
            Objects.requireNonNull(description, "description");
        }
    }

    private record HelpArgument(@NotNull String name, @NotNull ArgumentKind kind) {

        private HelpArgument {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(kind, "kind");
        }
    }

    private enum ArgumentKind {
        REQUIRED,
        OPTIONAL
    }
}
