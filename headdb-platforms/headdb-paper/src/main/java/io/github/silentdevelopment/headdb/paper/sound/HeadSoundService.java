package io.github.silentdevelopment.headdb.paper.sound;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HeadSoundService {

    private static final long DUPLICATE_WINDOW_NANOS = 60_000_000L;

    private final HeadDBPlugin plugin;
    private final SoundConfig config;
    private final Set<String> warnedInvalidSounds = ConcurrentHashMap.newKeySet();
    private final Map<UUID, PlayedSound> lastPlayed = new ConcurrentHashMap<>();

    public HeadSoundService(@NotNull HeadDBPlugin plugin, @NotNull SoundConfig config) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
    }

    public void play(@NotNull Player player, @NotNull SoundKey key) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(key, "key");

        if (!config.enabled()) {
            return;
        }

        if (!player.isOnline()) {
            return;
        }

        SoundEntry entry = config.entry(key);
        if (!entry.enabled()) {
            return;
        }

        if (entry.volume() <= 0.0F) {
            return;
        }

        if (isDuplicate(player, key)) {
            return;
        }

        Location location = player.getLocation();
        String rawSound = entry.sound().trim();
        if (rawSound.isBlank()) {
            warnInvalid(key, rawSound);
            return;
        }

        String sound = normalizeSound(rawSound);

        try {
            player.playSound(
                    location,
                    sound,
                    entry.volume(),
                    entry.pitch()
            );
        } catch (IllegalArgumentException exception) {
            warnInvalid(key, rawSound);
        }

        player.playSound(location, sound, entry.volume(), entry.pitch());
    }

    public void playGuiAction(@NotNull Player player, @NotNull String action) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(action, "action");
        play(player, SoundKey.fromGuiAction(action));
    }

    public void playGuiIcon(@NotNull Player player, @NotNull String iconKey) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(iconKey, "iconKey");
        play(player, SoundKey.fromGuiIcon(iconKey));
    }

    private static @NotNull String normalizeSound(@NotNull String sound) {
        String normalized = sound.trim().toLowerCase(Locale.ROOT);

        if (normalized.contains(":") || normalized.contains(".")) {
            return normalized;
        }

        return "minecraft:" + normalized.replace('_', '.');
    }

    private boolean isDuplicate(@NotNull Player player, @NotNull SoundKey key) {
        long now = System.nanoTime();
        PlayedSound previous = lastPlayed.put(player.getUniqueId(), new PlayedSound(key, now));
        if (previous == null || now - previous.timeNanos() >= DUPLICATE_WINDOW_NANOS) {
            return false;
        }

        return previous.key() == key || suppressesAfterAnyRecentSound(key);
    }

    private static boolean suppressesAfterAnyRecentSound(@NotNull SoundKey key) {
        return switch (key) {
            case MENU_OPEN, CLICK, BACK, PAGE_NEXT, PAGE_PREVIOUS, SEARCH, OPEN_INPUT, SELECTOR_OPEN, TOGGLE, SETTINGS, LANGUAGE, CUSTOM_MANAGEMENT -> true;
            default -> false;
        };
    }

    private void warnInvalid(@NotNull SoundKey key, @NotNull String sound) {
        String value = sound.isBlank() ? "<blank>" : sound;
        String warningKey = key.configKey() + "=" + value;
        if (!warnedInvalidSounds.add(warningKey)) {
            return;
        }

        plugin.getSLF4JLogger().warn("Invalid sound '{}' configured for sounds.{}. The sound will be skipped.", value, key.configKey());
    }

    private static boolean isNamespacedSound(@NotNull String sound) {
        String normalized = sound.trim().toLowerCase(Locale.ROOT);
        return normalized.contains(":") || normalized.contains(".");
    }

    private record PlayedSound(@NotNull SoundKey key, long timeNanos) {}
}
