package io.github.silentdevelopment.headdb.paper.prompt;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.prompts.Prompt;
import io.github.silentdevelopment.prompts.core.DefaultPrompt;
import io.github.silentdevelopment.prompts.paper.PaperPrompts;
import io.github.silentdevelopment.prompts.paper.chat.PaperChatTransport;
import io.github.silentdevelopment.prompts.paper.text.PaperPromptText;
import io.github.silentdevelopment.prompts.parser.ParseResult;
import io.github.silentdevelopment.prompts.result.PromptResult;
import io.github.silentdevelopment.prompts.result.PromptResultStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

public final class PromptInputService {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(45);
    private static final String CANCEL_TOKEN = "cancel";

    private final PaperPrompts prompts;

    public PromptInputService(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.prompts = PaperPrompts.builder(plugin).transport(new PaperChatTransport(plugin)).build();
    }

    public void request(@NotNull Player player, @NotNull Component message, @NotNull Consumer<String> input) {
        request(player, message, DEFAULT_TIMEOUT, input, () -> player.sendMessage(Component.text("Prompt cancelled.", NamedTextColor.GRAY)));
    }

    public void request(@NotNull Player player, @NotNull Component message, @NotNull Consumer<String> input, @NotNull Runnable cancel) {
        request(player, message, DEFAULT_TIMEOUT, input, cancel);
    }

    public void request(@NotNull Player player, @NotNull Component message, @NotNull Duration timeout, @NotNull Consumer<String> input, @NotNull Runnable cancel) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(cancel, "cancel");

        Component promptMessage = message.append(Component.text(" Type cancel to abort.", NamedTextColor.GRAY));
        Prompt<String> prompt = new DefaultPrompt<>(PaperPromptText.of(promptMessage), ParseResult::success, timeout, PaperChatTransport.DEFAULT_NAME);

        prompts.askReplacingAndHandleSync(player, prompt, result -> handle(result, input, cancel));
    }

    public void shutdown() {
        prompts.shutdown();
    }

    private static void handle(@NotNull PromptResult<String> result, @NotNull Consumer<String> input, @NotNull Runnable cancel) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(cancel, "cancel");

        if (result.status() == PromptResultStatus.SUPERSEDED) {
            return;
        }

        if (!result.successful()) {
            cancel.run();
            return;
        }

        String value = result.value().orElse("").trim();
        if (value.isBlank() || value.equalsIgnoreCase(CANCEL_TOKEN)) {
            cancel.run();
            return;
        }

        input.accept(value);
    }
}