package io.github.silentdevelopment.headdb.paper.gui.common;

import io.github.silentdevelopment.grafik.gui.GuiContext;
import io.github.silentdevelopment.grafik.key.GKey;
import io.github.silentdevelopment.grafik.paper.core.element.ItemElement;
import io.github.silentdevelopment.grafik.paper.page.PaperPageBuilder;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.config.GuiFillerConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class GuiItems {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private GuiItems() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static <C> void fillBorders(@NotNull HeadDBPlugin plugin, @NotNull PaperPageBuilder<C> page, int rows) {
        fillBorders(plugin, page, rows, Set.of());
    }

    public static <C> void fillBorders(@NotNull HeadDBPlugin plugin, @NotNull PaperPageBuilder<C> page, int rows, @NotNull Set<Integer> reservedSlots) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(reservedSlots, "reservedSlots");

        GuiFillerConfig filler = plugin.guiConfig().filler();

        if (!filler.enabled()) {
            return;
        }

        if (rows < 1) {
            return;
        }

        ItemStack item = fillerItem(filler);

        for (int slot : borderSlots(rows)) {
            if (reservedSlots.contains(slot)) {
                continue;
            }

            page.set(slot, ItemElement.<C>of(GKey.of("border_" + slot), ignored -> item.clone(), ignored -> {}));
        }
    }

    public static <C> void fillEmpty(@NotNull HeadDBPlugin plugin, @NotNull PaperPageBuilder<C> page, int rows, @NotNull Set<Integer> reservedSlots) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(reservedSlots, "reservedSlots");

        GuiFillerConfig filler = plugin.guiConfig().filler();

        if (!filler.enabled()) {
            return;
        }

        if (rows < 1) {
            return;
        }

        ItemStack item = fillerItem(filler);
        int size = rows * 9;

        for (int slot = 0; slot < size; slot++) {
            if (reservedSlots.contains(slot)) {
                continue;
            }

            page.set(slot, ItemElement.<C>of(GKey.of("fill_" + slot), ignored -> item.clone(), ignored -> {}));
        }
    }

    public static <C> @NotNull ItemElement<C> button(@NotNull String key, @NotNull Material material, @NotNull Component name, @NotNull List<Component> lore, @NotNull Consumer<GuiContext<C>> action) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lore, "lore");
        Objects.requireNonNull(action, "action");

        return ItemElement.<C>of(GKey.of(key), ignored -> item(material, name, lore), action);
    }

    public static @NotNull ItemStack item(@NotNull Material material, @NotNull Component name, @NotNull List<Component> lore) {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lore, "lore");

        ItemStack item = new ItemStack(material);

        item.editMeta(meta -> {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            meta.lore(cleanLore(lore));
        });

        return item;
    }

    public static @NotNull Component name(@NotNull String value, @NotNull NamedTextColor color) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(color, "color");

        return Component.text(value, color).decoration(TextDecoration.ITALIC, false);
    }

    public static @NotNull Component lore(@NotNull String value, @NotNull NamedTextColor color) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(color, "color");

        return Component.text(value, color).decoration(TextDecoration.ITALIC, false);
    }


    public static @NotNull Component detail(@NotNull String key, @NotNull Object value, @NotNull NamedTextColor valueColor) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(valueColor, "valueColor");

        return Component.text(key + ": ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(value), valueColor))
                .decoration(TextDecoration.ITALIC, false);
    }

    public static @NotNull Component idDetail(@NotNull String key, @NotNull Object value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        return detail(key, value, NamedTextColor.GOLD);
    }

    public static @NotNull Component metaDetail(@NotNull String key, @NotNull Object value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        return detail(key, value, NamedTextColor.GOLD);
    }

    public static @NotNull Component mini(@NotNull String value) {
        Objects.requireNonNull(value, "value");
        return MINI_MESSAGE.deserialize(value).decoration(TextDecoration.ITALIC, false);
    }


    public static @NotNull Component miniOrWhite(@NotNull String value) {
        Objects.requireNonNull(value, "value");

        if (value.contains("<")) {
            return mini(value);
        }

        return Component.text(value, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false);
    }

    public static @NotNull List<Component> miniLore(@NotNull List<String> lines) {
        Objects.requireNonNull(lines, "lines");

        List<Component> result = new ArrayList<>();

        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            result.add(mini(line));
        }

        return List.copyOf(result);
    }

    private static @NotNull ItemStack fillerItem(@NotNull GuiFillerConfig filler) {
        Material material = GuiMaterials.itemOr(filler.material(), Material.BLACK_STAINED_GLASS_PANE);
        return item(material, mini(filler.name()), miniLore(filler.lore()));
    }

    private static @NotNull List<Integer> borderSlots(int rows) {
        int size = rows * 9;
        List<Integer> slots = new ArrayList<>();

        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int column = slot % 9;

            if (row != 0 && row != rows - 1 && column != 0 && column != 8) {
                continue;
            }

            slots.add(slot);
        }

        return List.copyOf(slots);
    }

    private static @NotNull List<Component> cleanLore(@NotNull List<Component> lore) {
        Objects.requireNonNull(lore, "lore");

        List<Component> result = new ArrayList<>();

        for (Component line : lore) {
            result.add(line.decoration(TextDecoration.ITALIC, false));
        }

        return List.copyOf(result);
    }
}