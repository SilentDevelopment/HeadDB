package io.github.silentdevelopment.headdb.paper.gui.common;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class HeadPriceLore {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String TEXT_KEY = "head-price-lore";
    private static final String DEFAULT_TEMPLATE = "<gray>Price: <gold>%price%";

    private HeadPriceLore() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull ItemStack decorate(@NotNull HeadDBPlugin plugin, @NotNull ItemStack item, @NotNull Head head) {
        return decorate(plugin, item, head, null);
    }

    public static @NotNull ItemStack decorate(@NotNull HeadDBPlugin plugin, @NotNull ItemStack item, @NotNull Head head, @Nullable String customCategoryId) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(head, "head");

        if (!plugin.economy().enabled()) {
            return item;
        }

        String template = plugin.guiConfig().text(TEXT_KEY, DEFAULT_TEMPLATE).trim();
        if (template.isBlank()) {
            return item;
        }

        String formattedPrice = plugin.economy().format(plugin.economy().price(head, customCategoryId));
        Component priceLine = MINI_MESSAGE.deserialize(
                template.replace("%price%", "<price>"),
                Placeholder.unparsed("price", formattedPrice)
        ).decoration(TextDecoration.ITALIC, false);

        item.editMeta(meta -> {
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());

            if (!lore.isEmpty()) {
                lore.add(Component.empty());
            }

            lore.add(priceLine);
            meta.lore(lore);
        });

        return item;
    }
}
