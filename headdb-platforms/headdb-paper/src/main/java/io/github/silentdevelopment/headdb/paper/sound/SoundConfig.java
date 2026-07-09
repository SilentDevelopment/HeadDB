package io.github.silentdevelopment.headdb.paper.sound;

import io.github.silentdevelopment.headdb.paper.config.ConfigException;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class SoundConfig {

    private final boolean enabled;
    private final Map<SoundKey, SoundEntry> entries;

    private SoundConfig(boolean enabled, @NotNull Map<SoundKey, SoundEntry> entries) {
        this.enabled = enabled;
        this.entries = Map.copyOf(Objects.requireNonNull(entries, "entries"));
    }

    public boolean enabled() {
        return enabled;
    }

    public @NotNull SoundEntry entry(@NotNull SoundKey key) {
        Objects.requireNonNull(key, "key");
        SoundEntry entry = entries.get(key);

        if (entry != null) {
            return entry;
        }

        return new SoundEntry(false, "UI_BUTTON_CLICK", 0.0F, 1.0F);
    }

    public static @NotNull SoundConfig fromMap(@NotNull Map<String, Object> root) {
        Objects.requireNonNull(root, "root");
        Map<String, Object> sounds = section(root, "sounds");
        boolean enabled = booleanValue(sounds, "enabled", true);
        Map<SoundKey, SoundEntry> entries = new EnumMap<>(SoundKey.class);

        for (SoundKey key : SoundKey.values()) {
            Map<String, Object> entry = optionalSection(sounds, key.configKey());
            entries.put(key, new SoundEntry(booleanValue(entry, "enabled", true), stringValue(entry, "sound", "UI_BUTTON_CLICK"), floatValue(entry, "volume", 0.7F), floatValue(entry, "pitch", 1.0F)));
        }

        return new SoundConfig(enabled, entries);
    }

    private static @NotNull Map<String, Object> section(@NotNull Map<String, Object> root, @NotNull String key) {
        Object value = root.get(key);
        if (!(value instanceof Map<?, ?> map)) {
            throw new ConfigException("sounds.yml section '" + key + "' must be an object.");
        }

        return stringKeyMap(map);
    }

    private static @NotNull Map<String, Object> optionalSection(@NotNull Map<String, Object> root, @NotNull String key) {
        Object value = root.get(key);
        if (value == null) {
            return Map.of();
        }

        if (!(value instanceof Map<?, ?> map)) {
            throw new ConfigException("sounds.yml sounds." + key + " must be an object.");
        }

        return stringKeyMap(map);
    }

    private static boolean booleanValue(@NotNull Map<String, Object> map, @NotNull String key, boolean fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }

        if (value instanceof Boolean bool) {
            return bool;
        }

        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static @NotNull String stringValue(@NotNull Map<String, Object> map, @NotNull String key, @NotNull String fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }

        return String.valueOf(value).trim();
    }

    private static float floatValue(@NotNull Map<String, Object> map, @NotNull String key, float fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }

        if (value instanceof Number number) {
            return number.floatValue();
        }

        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new ConfigException("sounds.yml " + key + " must be a number.", exception);
        }
    }

    private static @NotNull Map<String, Object> stringKeyMap(@NotNull Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : source.entrySet()) {
            result.put(String.valueOf(entry.getKey()).trim().toLowerCase(Locale.ROOT), entry.getValue());
        }

        return result;
    }
}
