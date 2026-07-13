package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.command.Suggestions;
import io.github.silentdevelopment.headdb.paper.item.HeadItemFactory;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.sound.SoundKey;
import io.github.silentdevelopment.headdb.query.HeadQuery;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomCommand extends AbstractPaperCommand {

    private static final int MAX_AMOUNT = 64;
    private static final Argument<String> FIRST = Argument.optional("amount-category-player", PaperArgumentTypes.STRING);
    private static final Argument<String> SECOND = Argument.optional("category-or-player", PaperArgumentTypes.STRING);
    private static final Argument<String> THIRD = Argument.optional("player", PaperArgumentTypes.STRING);

    private final HeadDBPlugin plugin;
    private final HeadItemFactory itemFactory;

    public RandomCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.itemFactory = plugin.itemFactory();
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        RandomRequest request;

        try {
            request = request(context);
        } catch (IllegalArgumentException exception) {
            plugin.messages().send(context.sender(), plugin.messages().invalidArgument(context.sender(), exception.getMessage()));
            play(context, SoundKey.INVALID);
            return;
        }

        Player target = target(context, request.playerName());
        if (target == null) {
            return;
        }

        if (!Permissions.canGiveTo(context.sender(), target)) {
            plugin.messages().send(context.sender(), plugin.messages().render(context.sender(), MessageKey.COMMAND_ERROR_NO_PERMISSION));
            play(context, SoundKey.NO_PERMISSION);
            return;
        }

        for (int index = 0; index < request.amount(); index++) {
            Optional<Head> optionalHead = randomHead(request.category());
            if (optionalHead.isEmpty()) {
                plugin.messages().send(context.sender(), plugin.messages().randomEmpty(context.sender()));
                play(context, SoundKey.INVALID);
                return;
            }

            if (context.isPlayer() && !plugin.economy().charge(context.player(), optionalHead.get(), 1)) {
                return;
            }

            if (!give(target, optionalHead.get())) {
                plugin.messages().send(context.sender(), plugin.messages().giveInventoryFull(context.sender(), target));
                play(context, SoundKey.INVALID);
                return;
            }

            plugin.messages().send(context.sender(), plugin.messages().giveSuccess(context.sender(), optionalHead.get(), target));
            plugin.sounds().play(target, SoundKey.GIVE_HEAD);
            if (context.isPlayer() && !context.player().equals(target)) {
                plugin.sounds().play(context.player(), SoundKey.GIVE_HEAD);
            }
        }
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("random")
                .alias("rnd")
                .description("Gives a random head.")
                .requirement(CommandRequirements.permission(Permissions.GIVE))
                .signature(FIRST, SECOND, THIRD)
                .suggest(FIRST, Suggestions.playerOrHeadIds(plugin))
                .suggest(SECOND, Suggestions.categories(plugin))
                .suggest(THIRD, Suggestions.players())
                .noArgs()
                .build();
    }

    private @NotNull Optional<Head> randomHead(@Nullable String category) {
        HeadQuery.Builder countBuilder = HeadQuery.builder().limit(1).sort(HeadSort.ID).direction(SortDirection.ASCENDING);
        if (category != null) {
            countBuilder.category(category);
        }

        HeadQueryResult countResult = plugin.headRegistry().search(countBuilder.build());
        if (countResult.total() <= 0) {
            return Optional.empty();
        }

        int offset = ThreadLocalRandom.current().nextInt(countResult.total());
        HeadQuery.Builder headBuilder = HeadQuery.builder().offset(offset).limit(1).sort(HeadSort.ID).direction(SortDirection.ASCENDING);
        if (category != null) {
            headBuilder.category(category);
        }

        HeadQueryResult result = plugin.headRegistry().search(headBuilder.build());
        if (result.heads().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(result.heads().getFirst());
    }

    private boolean give(@NotNull Player target, @NotNull Head head) {
        ItemStack item = itemFactory.create(head);
        if (target.getInventory().firstEmpty() == -1) {
            return false;
        }

        return target.getInventory().addItem(item).isEmpty();
    }

    private @Nullable Player target(@NotNull PaperCommandContext context, @Nullable String playerName) {
        if (playerName == null) {
            if (context.isPlayer()) {
                return context.player();
            }

            plugin.messages().send(context.sender(), plugin.messages().randomConsoleUsage(context.sender()));
            play(context, SoundKey.INVALID);
            return null;
        }

        Player target = Bukkit.getPlayerExact(playerName);
        if (target != null) {
            return target;
        }

        plugin.messages().send(context.sender(), plugin.messages().playerNotOnline(context.sender(), playerName));
        play(context, SoundKey.INVALID);
        return null;
    }

    private @NotNull RandomRequest request(@NotNull PaperCommandContext context) {
        RandomRequestBuilder builder = new RandomRequestBuilder();
        consume(context, builder, FIRST);
        consume(context, builder, SECOND);
        consume(context, builder, THIRD);
        return builder.build();
    }

    private void consume(@NotNull PaperCommandContext context, @NotNull RandomRequestBuilder builder, @NotNull Argument<String> argument) {
        if (!context.has(argument)) {
            return;
        }

        String value = context.get(argument).trim();
        if (value.isEmpty()) {
            return;
        }

        if (isAmount(value) && !builder.amountSet) {
            builder.amount(parseAmount(value));
            return;
        }

        if (plugin.headRegistry().category(value).isPresent() && builder.category == null) {
            builder.category(value);
            return;
        }

        if (builder.playerName == null) {
            builder.player(value);
            return;
        }

        throw new IllegalArgumentException("Could not parse random command argument: " + value);
    }

    private void play(@NotNull PaperCommandContext context, @NotNull SoundKey key) {
        if (!context.isPlayer()) {
            return;
        }

        plugin.sounds().play(context.player(), key);
    }

    private static boolean isAmount(@NotNull String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static int parseAmount(@NotNull String value) {
        int amount = Integer.parseInt(value);
        if (amount < 1 || amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("Random amount must be between 1 and " + MAX_AMOUNT + ".");
        }

        return amount;
    }

    private static final class RandomRequestBuilder {

        private int amount = 1;
        private boolean amountSet;
        private String category;
        private String playerName;

        private void amount(int amount) {
            this.amount = amount;
            this.amountSet = true;
        }

        private void category(@NotNull String category) {
            this.category = category.trim().toLowerCase(java.util.Locale.ROOT);
        }

        private void player(@NotNull String playerName) {
            this.playerName = playerName;
        }

        private @NotNull RandomRequest build() {
            return new RandomRequest(amount, category, playerName);
        }
    }

    private record RandomRequest(int amount, @Nullable String category, @Nullable String playerName) {
    }
}