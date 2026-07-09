package io.github.silentdevelopment.headdb.paper.gui.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class GuiTitles {


    private GuiTitles() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull Component title(@NotNull String title, boolean adminMode) {
        Objects.requireNonNull(title, "title");

        return Component.text(title.trim(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
    }

    public static @NotNull Component title(@NotNull String prefix, @NotNull String detail, boolean adminMode) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(detail, "detail");

        String clean = prefix.trim() + ": " + shortTitle(detail.trim());
        return title(clean, adminMode);
    }

    private static @NotNull String shortTitle(@NotNull String value) {
        if (value.length() <= 16) {
            return value;
        }

        return value.substring(0, 13) + "...";
    }
}
