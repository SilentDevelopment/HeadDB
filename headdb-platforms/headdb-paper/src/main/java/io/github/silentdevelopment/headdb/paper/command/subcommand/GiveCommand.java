package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.command.Suggestions;
import io.github.silentdevelopment.headdb.paper.command.search.SearchParser;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.sound.SoundKey;
import io.github.silentdevelopment.relay.argument.Argument;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.argument.PaperArgumentTypes;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public final class GiveCommand extends AbstractPaperCommand {

    private static final int MAX_AMOUNT = 64;
    private static final Argument<String> FIRST = Argument.required("id-or-player", PaperArgumentTypes.STRING);
    private static final Argument<String> SECOND = Argument.optional("player-or-amount-or-id", PaperArgumentTypes.STRING);
    private static final Argument<String> AMOUNT = Argument.optional("amount", PaperArgumentTypes.STRING);

    private final HeadDBPlugin plugin;

    public GiveCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        GiveRequest request;

        try {
            request = request(context);
        } catch (IllegalArgumentException exception) {
            plugin.messages().send(context.sender(), plugin.messages().invalidArgument(context.sender(), exception.getMessage()));
            play(context, SoundKey.INVALID);
            return;
        }

        Player target = resolveTarget(context, request.targetName());
        if (target == null) {
            return;
        }

        plugin.headRegistry().resolve(request.id()).whenComplete((head, throwable) -> plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            if (throwable != null || head.isEmpty()) {
                plugin.messages().send(context.sender(), plugin.messages().unknownHead(context.sender(), request.id()));
                return;
            }

            give(context, target, head.get(), request.amount());
        }));
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("give")
                .alias("g")
                .description("Gives a head item.")
                .requirement(CommandRequirements.permission(Permissions.GIVE))
                .signature(FIRST, SECOND, AMOUNT)
                .suggest(FIRST, Suggestions.playerOrHeadIds(plugin))
                .suggest(SECOND, Suggestions.playerOrHeadIds(plugin))
                .suggest(AMOUNT, Suggestions.amounts())
                .noArgs()
                .build();
    }

    private @NotNull GiveRequest request(@NotNull PaperCommandContext context) {
        String first = context.get(FIRST).trim();

        if (first.isEmpty()) {
            throw new IllegalArgumentException(usage());
        }

        HeadId firstId = parseOptionalHeadId(first);
        if (!context.has(SECOND)) {
            if (firstId == null) {
                throw new IllegalArgumentException(usage());
            }

            return new GiveRequest(firstId, null, 1);
        }

        String second = context.get(SECOND).trim();
        if (second.isEmpty()) {
            throw new IllegalArgumentException(usage());
        }

        HeadId secondId = parseOptionalHeadId(second);
        if (context.has(AMOUNT)) {
            return requestWithExplicitAmount(first, firstId, second, secondId, context.get(AMOUNT));
        }

        if (firstId != null && secondId == null) {
            if (isAmount(second)) {
                return new GiveRequest(firstId, null, parseAmount(second));
            }

            return new GiveRequest(firstId, second, 1);
        }

        if (firstId == null && secondId != null) {
            return new GiveRequest(secondId, first, 1);
        }

        if (firstId != null) {
            throw new IllegalArgumentException("Ambiguous give command. Use /hdb give <id> <player> [amount] with a non-ID player name.");
        }

        throw new IllegalArgumentException(usage());
    }

    private @NotNull GiveRequest requestWithExplicitAmount(@NotNull String first, @Nullable HeadId firstId, @NotNull String second, @Nullable HeadId secondId, @NotNull String rawAmount) {
        int amount = parseAmount(rawAmount);

        if (firstId != null && secondId == null) {
            return new GiveRequest(firstId, second, amount);
        }

        if (firstId == null && secondId != null) {
            return new GiveRequest(secondId, first, amount);
        }

        if (firstId != null) {
            throw new IllegalArgumentException("Ambiguous give command. Use /hdb give <id> <player> <amount> with a non-ID player name.");
        }

        throw new IllegalArgumentException(usage());
    }

    private @Nullable Player resolveTarget(@NotNull PaperCommandContext context, @Nullable String targetName) {
        if (targetName == null) {
            if (context.isPlayer()) {
                return context.player();
            }

            plugin.messages().send(context.sender(), plugin.messages().consoleGiveUsage(context.sender()));
            return null;
        }

        String normalizedTargetName = targetName.trim();

        if (normalizedTargetName.isEmpty()) {
            plugin.messages().send(context.sender(), plugin.messages().targetEmpty(context.sender()));
            play(context, SoundKey.INVALID);
            return null;
        }

        Player target = Bukkit.getPlayerExact(normalizedTargetName);
        if (target == null) {
            plugin.messages().send(context.sender(), plugin.messages().playerNotOnline(context.sender(), normalizedTargetName));
            play(context, SoundKey.INVALID);
            return null;
        }

        if (!Permissions.canGiveTo(context.sender(), target)) {
            plugin.messages().send(context.sender(), plugin.messages().noGiveOthers(context.sender()));
            play(context, SoundKey.NO_PERMISSION);
            return null;
        }

        return target;
    }

    private void give(@NotNull PaperCommandContext context, @NotNull Player target, @NotNull Head head, int amount) {
        if (context.isPlayer() && !plugin.economy().charge(context.player(), head, amount)) {
            return;
        }

        for (int index = 0; index < amount; index++) {
            ItemStack item;

            try {
                item = plugin.itemFactory().create(head);
            } catch (IllegalArgumentException exception) {
                plugin.messages().send(context.sender(), plugin.messages().invalidArgument(context.sender(), exception.getMessage()));
                play(context, SoundKey.INVALID);
                return;
            }

            if (target.getInventory().firstEmpty() == -1) {
                plugin.messages().send(context.sender(), plugin.messages().giveInventoryFull(context.sender(), target));
                play(context, SoundKey.INVALID);
                return;
            }

            Map<Integer, ItemStack> remaining = target.getInventory().addItem(item);
            if (!remaining.isEmpty()) {
                plugin.messages().send(context.sender(), plugin.messages().giveInventoryFull(context.sender(), target));
                play(context, SoundKey.INVALID);
                return;
            }
        }

        plugin.messages().send(context.sender(), plugin.messages().giveSuccess(context.sender(), head, target));
        plugin.sounds().play(target, SoundKey.GIVE_HEAD);
        if (context.isPlayer() && !context.player().equals(target)) {
            plugin.sounds().play(context.player(), SoundKey.GIVE_HEAD);
        }

        if (!context.sender().equals(target)) {
            target.sendMessage(plugin.messages().giveReceived(target, head));
        }
    }

    private void play(@NotNull PaperCommandContext context, @NotNull SoundKey key) {
        if (!context.isPlayer()) {
            return;
        }

        plugin.sounds().play(context.player(), key);
    }

    private static @Nullable HeadId parseOptionalHeadId(@NotNull String raw) {
        try {
            return SearchParser.headId(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
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

    private static @NotNull String usage() {
        return "Usage: /hdb give <id> [player] [amount] or /hdb give <player> <id> [amount]";
    }

    private record GiveRequest(@NotNull HeadId id, @Nullable String targetName, int amount) {

        private GiveRequest {
            Objects.requireNonNull(id, "id");
        }
    }
}
