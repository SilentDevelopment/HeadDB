package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.command.Suggestions;
import io.github.silentdevelopment.headdb.paper.command.format.HeadInfoFormatter;
import io.github.silentdevelopment.headdb.paper.command.search.SearchParser;
import io.github.silentdevelopment.headdb.paper.local.override.RemoteHeadOverride;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.relay.argument.Argument;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.argument.PaperArgumentTypes;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class EditCommand extends AbstractPaperCommand {

    private static final Argument<String> ID = Argument.required("remote-id", PaperArgumentTypes.STRING);
    private static final Argument<String> ACTION = Argument.optional("action", PaperArgumentTypes.STRING);
    private static final Argument<String> VALUE = Argument.optional("value", PaperArgumentTypes.STRING);
    private static final Argument<String> EXTRA = Argument.optional("extra", PaperArgumentTypes.STRING);

    private final HeadDBPlugin plugin;

    public EditCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        HeadId id;
        try {
            id = SearchParser.headId(context.get(ID));
        } catch (IllegalArgumentException exception) {
            plugin.messages().send(context.sender(), plugin.messages().invalidArgument(context.sender(), exception.getMessage()));
            return;
        }

        if (!id.isRemote()) {
            plugin.messages().send(context.sender(), Component.text("Only remote heads can be edited with /hdb edit. Use /hdb custom for custom heads.", NamedTextColor.RED));
            return;
        }

        if (plugin.runtime().database().findById(id).isEmpty()) {
            plugin.messages().send(context.sender(), plugin.messages().unknownHead(context.sender(), id));
            return;
        }

        String action = context.has(ACTION) ? context.get(ACTION).trim().toLowerCase(java.util.Locale.ROOT) : "info";

        try {
            switch (action) {
                case "info" -> info(context, id);
                case "name" -> name(context, id);
                case "lore-set" -> loreSet(context, id);
                case "lore-clear" -> loreClear(context, id);
                case "tag-add" -> tagAdd(context, id);
                case "tag-remove" -> tagRemove(context, id);
                case "tags-replace" -> tagsReplace(context, id);
                case "category" -> category(context, id);
                case "hide" -> hidden(context, id, true);
                case "show" -> hidden(context, id, false);
                case "reset" -> reset(context, id);
                default -> usage(context);
            }
        } catch (IllegalArgumentException exception) {
            plugin.messages().send(context.sender(), plugin.messages().invalidArgument(context.sender(), exception.getMessage()));
        }
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("edit")
                .description("Edits local metadata overrides for remote heads.")
                .requirement(CommandRequirements.permission(Permissions.EDIT))
                .signature(ID, ACTION, VALUE, EXTRA)
                .suggest(ID, Suggestions.headIds(plugin))
                .suggest(ACTION, context -> List.of("info", "name", "lore-set", "lore-clear", "tag-add", "tag-remove", "tags-replace", "category", "hide", "show", "reset"))
                .suggest(VALUE, Suggestions.tags(plugin))
                .build();
    }

    private void info(@NotNull PaperCommandContext context, @NotNull HeadId id) {
        require(context, Permissions.EDIT);
        Head head = plugin.headRegistry().find(id).orElseThrow(() -> new IllegalArgumentException("Unknown head: " + id));
        for (Component line : HeadInfoFormatter.format(head)) {
            plugin.messages().send(context.sender(), line);
        }
        plugin.headRegistry().overrides().find(id).ifPresentOrElse(
                override -> plugin.messages().send(context.sender(), Component.text("Local override: present", NamedTextColor.GOLD)),
                () -> plugin.messages().send(context.sender(), Component.text("Local override: none", NamedTextColor.GRAY))
        );
    }

    private void name(@NotNull PaperCommandContext context, @NotNull HeadId id) {
        require(context, Permissions.EDIT_NAME);
        save(context, override(context, id).withName(required(context, VALUE, "Usage: /hdb edit <id> name <name>"), actor(context)));
    }

    private void loreSet(@NotNull PaperCommandContext context, @NotNull HeadId id) {
        require(context, Permissions.EDIT_LORE);
        save(context, override(context, id).withLore(List.of(required(context, VALUE, "Usage: /hdb edit <id> lore-set <line>")), actor(context)));
    }

    private void loreClear(@NotNull PaperCommandContext context, @NotNull HeadId id) {
        require(context, Permissions.EDIT_LORE);
        save(context, override(context, id).withLore(null, actor(context)));
    }

    private void tagAdd(@NotNull PaperCommandContext context, @NotNull HeadId id) {
        require(context, Permissions.EDIT_TAGS);
        save(context, override(context, id).withTagAdded(required(context, VALUE, "Usage: /hdb edit <id> tag-add <tag>"), actor(context)));
    }

    private void tagRemove(@NotNull PaperCommandContext context, @NotNull HeadId id) {
        require(context, Permissions.EDIT_TAGS);
        save(context, override(context, id).withTagRemoved(required(context, VALUE, "Usage: /hdb edit <id> tag-remove <tag>"), actor(context)));
    }

    private void tagsReplace(@NotNull PaperCommandContext context, @NotNull HeadId id) {
        require(context, Permissions.EDIT_TAGS);
        String raw = required(context, VALUE, "Usage: /hdb edit <id> tags-replace <tag,tag,...>");
        Set<String> tags = new LinkedHashSet<>();
        Arrays.stream(raw.split(",")).map(String::trim).filter(value -> !value.isBlank()).map(value -> value.toLowerCase(java.util.Locale.ROOT)).forEach(tags::add);
        save(context, override(context, id).withReplacementTags(tags, actor(context)));
    }

    private void category(@NotNull PaperCommandContext context, @NotNull HeadId id) {
        require(context, Permissions.EDIT_CATEGORY);
        save(context, override(context, id).withCategory(required(context, VALUE, "Usage: /hdb edit <id> category <category>"), actor(context)));
    }

    private void hidden(@NotNull PaperCommandContext context, @NotNull HeadId id, boolean hidden) {
        require(context, Permissions.EDIT_VISIBILITY);
        save(context, override(context, id).withHidden(hidden, actor(context)));
    }

    private void reset(@NotNull PaperCommandContext context, @NotNull HeadId id) {
        require(context, Permissions.EDIT_RESET);
        boolean deleted = plugin.headRegistry().overrides().delete(id);
        changed();
        plugin.messages().send(context.sender(), Component.text(deleted ? "Reset local override for " : "No local override existed for ", deleted ? NamedTextColor.GRAY : NamedTextColor.RED).append(Component.text(id.display(), NamedTextColor.GOLD)));
    }

    private @NotNull RemoteHeadOverride override(@NotNull PaperCommandContext context, @NotNull HeadId id) {
        return plugin.headRegistry().overrides().find(id).orElseGet(() -> RemoteHeadOverride.empty(id, actor(context)));
    }

    private void save(@NotNull PaperCommandContext context, @NotNull RemoteHeadOverride override) {
        plugin.headRegistry().overrides().save(override);
        changed();
        plugin.messages().send(context.sender(), Component.text("Saved local override for ", NamedTextColor.GRAY).append(Component.text(override.headId().display(), NamedTextColor.GOLD)));
    }

    private void require(@NotNull PaperCommandContext context, @NotNull String permission) {
        if (!Permissions.has(context.sender(), permission)) {
            throw new IllegalArgumentException("You do not have permission to do that.");
        }
    }

    private @NotNull String required(@NotNull PaperCommandContext context, @NotNull Argument<String> argument, @NotNull String message) {
        if (!context.has(argument) || context.get(argument).trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return context.get(argument).trim();
    }

    private UUID actor(@NotNull PaperCommandContext context) {
        return context.isPlayer() ? context.player().getUniqueId() : null;
    }

    private void changed() {
        plugin.headRegistry().onLocalMutation();
        plugin.clearSearchCache();
        plugin.clearItemCache();
    }

    private void usage(@NotNull PaperCommandContext context) {
        plugin.messages().send(context.sender(), Component.text("Usage: /hdb edit <remote-id> <info|name|lore-set|lore-clear|tag-add|tag-remove|tags-replace|category|hide|show|reset> ...", NamedTextColor.RED));
    }
}
