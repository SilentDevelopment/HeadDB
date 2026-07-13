package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.command.Suggestions;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.sound.SoundKey;
import io.github.silentdevelopment.relay.argument.Argument;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.argument.PaperArgumentTypes;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public final class PlayerCommand extends AbstractPaperCommand {

    private static final int MAX_AMOUNT = 64;
    private static final Argument<String> LOOKUP = Argument.required("name-or-uuid", PaperArgumentTypes.STRING);
    private static final Argument<String> TARGET = Argument.optional("player-or-amount", PaperArgumentTypes.STRING);
    private static final Argument<String> AMOUNT = Argument.optional("amount", PaperArgumentTypes.STRING);

    private final HeadDBPlugin plugin;

    public PlayerCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        String lookup = context.get(LOOKUP).trim();
        ParsedTarget parsedTarget;

        try {
            parsedTarget = parseTarget(context);
        } catch (IllegalArgumentException exception) {
            plugin.messages().send(context.sender(), plugin.messages().invalidArgument(context.sender(), exception.getMessage()));
            play(context, SoundKey.INVALID);
            return;
        }

        Player target = target(context, parsedTarget.targetName());
        if (target == null) {
            return;
        }

        if (!Permissions.canPlayerHeadFor(context.sender(), target)) {
            plugin.messages().send(context.sender(), plugin.messages().render(context.sender(), io.github.silentdevelopment.headdb.paper.message.MessageKey.COMMAND_ERROR_NO_PERMISSION));
            play(context, SoundKey.NO_PERMISSION);
            return;
        }

        plugin.messages().send(context.sender(), Component.text("Resolving player head for ", NamedTextColor.GRAY).append(Component.text(lookup, NamedTextColor.GOLD)).append(Component.text("...", NamedTextColor.GRAY)));
        plugin.headRegistry().playerHeads().resolve(lookup).whenComplete((head, throwable) -> plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            if (throwable != null) {
                plugin.messages().send(context.sender(), Component.text("Could not resolve player head: ", NamedTextColor.RED).append(Component.text(message(throwable), NamedTextColor.GRAY)));
                play(context, SoundKey.INVALID);
                return;
            }

            if (context.isPlayer() && !plugin.economy().charge(context.player(), head, parsedTarget.amount())) {
                return;
            }

            for (int index = 0; index < parsedTarget.amount(); index++) {
                if (!give(target, head)) {
                    plugin.messages().send(context.sender(), plugin.messages().giveInventoryFull(context.sender(), target));
                    play(context, SoundKey.INVALID);
                    return;
                }
            }

            plugin.messages().send(context.sender(), plugin.messages().giveSuccess(context.sender(), head, target));
            plugin.sounds().play(target, SoundKey.PLAYER_HEAD);
            if (context.isPlayer() && !context.player().equals(target)) {
                plugin.sounds().play(context.player(), SoundKey.PLAYER_HEAD);
            }
        }));
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("player")
                .alias("p")
                .description("Gives a player head.")
                .requirement(CommandRequirements.permission(Permissions.PLAYER))
                .signature(LOOKUP, TARGET, AMOUNT)
                .suggest(LOOKUP, Suggestions.players())
                .suggest(TARGET, Suggestions.players())
                .suggest(AMOUNT, Suggestions.amounts())
                .build();
    }

    private @NotNull ParsedTarget parseTarget(@NotNull PaperCommandContext context) {
        if (!context.has(TARGET)) {
            return new ParsedTarget(null, 1);
        }

        String second = context.get(TARGET).trim();
        if (second.isEmpty()) {
            throw new IllegalArgumentException("Usage: /hdb player <name|uuid> [player] [amount]");
        }

        if (context.has(AMOUNT)) {
            return new ParsedTarget(second, parseAmount(context.get(AMOUNT)));
        }

        if (isAmount(second)) {
            return new ParsedTarget(null, parseAmount(second));
        }

        return new ParsedTarget(second, 1);
    }

    private @Nullable Player target(@NotNull PaperCommandContext context, @Nullable String targetName) {
        if (targetName == null || targetName.isBlank()) {
            if (context.isPlayer()) {
                return context.player();
            }
            plugin.messages().send(context.sender(), Component.text("Usage: /hdb player <name|uuid> <player> [amount]", NamedTextColor.RED));
            play(context, SoundKey.INVALID);
            return null;
        }

        Player target = Bukkit.getPlayerExact(targetName.trim());
        if (target == null) {
            plugin.messages().send(context.sender(), plugin.messages().playerNotOnline(context.sender(), targetName));
            play(context, SoundKey.INVALID);
            return null;
        }
        return target;
    }

    private boolean give(@NotNull Player target, @NotNull Head head) {
        ItemStack item = plugin.itemFactory().create(head);
        if (target.getInventory().firstEmpty() == -1) {
            return false;
        }
        Map<Integer, ItemStack> remaining = target.getInventory().addItem(item);
        return remaining.isEmpty();
    }

    private void play(@NotNull PaperCommandContext context, @NotNull SoundKey key) {
        if (!context.isPlayer()) {
            return;
        }

        plugin.sounds().play(context.player(), key);
    }

    private static boolean isAmount(@NotNull String raw) {
        try {
            parseAmount(raw);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static int parseAmount(@NotNull String raw) {
        int amount;

        try {
            amount = Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Amount must be a number.");
        }

        if (amount < 1 || amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("Amount must be between 1 and " + MAX_AMOUNT + ".");
        }

        return amount;
    }

    private record ParsedTarget(@Nullable String targetName, int amount) {
    }

    private static @NotNull String message(@NotNull Throwable throwable) {
        Throwable cause = throwable;

        if (throwable instanceof java.util.concurrent.CompletionException && throwable.getCause() != null) {
            cause = throwable.getCause();
        }

        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }

        return message;
    }
}
