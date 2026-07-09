package io.github.silentdevelopment.headdb.paper.sound;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record SoundEntry(boolean enabled, @NotNull String sound, float volume, float pitch) {

    public SoundEntry {
        Objects.requireNonNull(sound, "sound");
        volume = clamp(volume, 0.0F, 10.0F);
        pitch = clamp(pitch, 0.0F, 2.0F);
    }

    private static float clamp(float value, float min, float max) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return min;
        }

        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }
}
