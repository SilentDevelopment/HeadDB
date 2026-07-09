package io.github.silentdevelopment.headdb.paper.gui.common;

import io.github.silentdevelopment.grafik.gui.GuiContext;
import io.github.silentdevelopment.grafik.key.GKey;
import io.github.silentdevelopment.grafik.paper.core.element.ItemElement;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.MenuState;
import io.github.silentdevelopment.headdb.paper.gui.search.SearchMenuState;
import io.github.silentdevelopment.headdb.paper.gui.config.GuiIconConfig;
import io.github.silentdevelopment.headdb.paper.gui.config.GuiIconType;
import io.github.silentdevelopment.headdb.paper.gui.config.GuiButtonEditorMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public final class GuiHeadIcons {

    private GuiHeadIcons() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static <C> @NotNull ItemElement<C> button(@NotNull HeadDBPlugin plugin, @NotNull String elementKey, @NotNull String iconKey, @NotNull Consumer<GuiContext<C>> action) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(elementKey, "elementKey");
        Objects.requireNonNull(iconKey, "iconKey");
        Objects.requireNonNull(action, "action");

        return button(plugin, elementKey, plugin.guiConfig().icon(iconKey), action);
    }

    public static <C> @NotNull ItemElement<C> button(@NotNull HeadDBPlugin plugin, @NotNull String elementKey, @NotNull GuiIconConfig icon, @NotNull Consumer<GuiContext<C>> action) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(elementKey, "elementKey");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(action, "action");

        return ItemElement.<C>of(GKey.of(elementKey), ignored -> icon(plugin, icon), context -> {
            Optional<UUID> viewerId = viewerId(context);
            if (viewerId.isPresent() && GuiButtonEditorMenu.consumeEditedDrop(viewerId.get(), icon.key())) {
                return;
            }

            play(plugin, viewerId, icon.key());
            action.accept(context);
        });
    }

    public static <C> @NotNull ItemElement<C> button(@NotNull HeadDBPlugin plugin, @NotNull String elementKey, @NotNull String iconKey, @NotNull Component name, @NotNull List<Component> lore, @NotNull Consumer<GuiContext<C>> action) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(elementKey, "elementKey");
        Objects.requireNonNull(iconKey, "iconKey");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lore, "lore");
        Objects.requireNonNull(action, "action");

        return button(plugin, elementKey, plugin.guiConfig().icon(iconKey), name, lore, action);
    }

    public static <C> @NotNull ItemElement<C> button(@NotNull HeadDBPlugin plugin, @NotNull String elementKey, @NotNull GuiIconConfig icon, @NotNull Component name, @NotNull List<Component> lore, @NotNull Consumer<GuiContext<C>> action) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(elementKey, "elementKey");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lore, "lore");
        Objects.requireNonNull(action, "action");

        return ItemElement.<C>of(GKey.of(elementKey), ignored -> icon(plugin, icon, name, lore), context -> {
            Optional<UUID> viewerId = viewerId(context);
            if (viewerId.isPresent() && GuiButtonEditorMenu.consumeEditedDrop(viewerId.get(), icon.key())) {
                return;
            }

            play(plugin, viewerId, icon.key());
            action.accept(context);
        });
    }


    public static <C> @NotNull ItemElement<C> button(@NotNull HeadDBPlugin plugin, @NotNull String elementKey, @NotNull GuiIconConfig icon, @NotNull Consumer<GuiContext<C>> action, @NotNull Function<GuiContext<C>, Component> name, @NotNull Function<GuiContext<C>, List<Component>> lore) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(elementKey, "elementKey");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lore, "lore");

        return ItemElement.<C>of(GKey.of(elementKey), context -> icon(plugin, icon, name.apply(context), lore.apply(context)), context -> {
            Optional<UUID> viewerId = viewerId(context);
            if (viewerId.isPresent() && GuiButtonEditorMenu.consumeEditedDrop(viewerId.get(), icon.key())) {
                return;
            }

            play(plugin, viewerId, icon.key());
            action.accept(context);
        });
    }

    public static @NotNull ItemStack icon(@NotNull HeadDBPlugin plugin, @NotNull GuiIconConfig icon) {
        Objects.requireNonNull(icon, "icon");
        return icon(plugin, icon, GuiItems.mini(icon.name()), GuiItems.miniLore(icon.lore()));
    }

    public static @NotNull ItemStack icon(@NotNull HeadDBPlugin plugin, @NotNull GuiIconConfig icon, @NotNull Component name, @NotNull List<Component> lore) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lore, "lore");

        ItemStack item;

        if (icon.iconType() == GuiIconType.HEAD) {
            item = configuredHead(plugin, icon);

            if (item == null) {
                item = materialIcon(icon);
            }
        } else {
            item = materialIcon(icon);
        }

        item.editMeta(meta -> {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            meta.lore(cleanLore(lore));
            meta.getPersistentDataContainer().set(GuiButtonEditorMenu.iconKey(plugin), PersistentDataType.STRING, icon.key());
        });

        return item;
    }


    private static <C> @NotNull Optional<UUID> viewerId(@NotNull GuiContext<C> context) {
        Objects.requireNonNull(context, "context");

        Object source = context.source();
        if (source instanceof MenuState state) {
            return Optional.of(state.viewerId());
        }

        if (source instanceof SearchMenuState state) {
            return Optional.of(state.viewerId());
        }

        return Optional.empty();
    }

    private static void play(@NotNull HeadDBPlugin plugin, @NotNull Optional<UUID> viewerId, @NotNull String iconKey) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(iconKey, "iconKey");

        if (viewerId.isEmpty()) {
            return;
        }

        Player player = Bukkit.getPlayer(viewerId.get());
        if (player == null) {
            return;
        }

        plugin.sounds().playGuiIcon(player, iconKey);
    }

    private static ItemStack configuredHead(@NotNull HeadDBPlugin plugin, @NotNull GuiIconConfig icon) {
        if (icon.headId().isBlank()) {
            plugin.getSLF4JLogger().warn(
                    "GUI icon '{}' is type HEAD but has no head-id configured. Falling back to material '{}'.",
                    icon.key(),
                    icon.material()
            );
            return null;
        }

        HeadId id;

        try {
            id = parseHeadId(icon.headId());
        } catch (IllegalArgumentException exception) {
            plugin.getSLF4JLogger().warn(
                    "Invalid GUI icon head-id '{}' for icon '{}'. Falling back to material '{}'.",
                    icon.headId(),
                    icon.key(),
                    icon.material(),
                    exception
            );
            return null;
        }

        Optional<Head> head = plugin.headRegistry().find(id);

        if (head.isEmpty()) {
            plugin.getSLF4JLogger().warn(
                    "GUI icon '{}' references missing head '{}'. Falling back to material '{}'.",
                    icon.key(),
                    id,
                    icon.material()
            );
            return null;
        }

        try {
            return plugin.itemFactory().create(head.get());
        } catch (IllegalArgumentException exception) {
            plugin.getSLF4JLogger().warn(
                    "GUI icon '{}' could not create head item '{}'. Falling back to material '{}'.",
                    icon.key(),
                    id,
                    icon.material(),
                    exception
            );
            return null;
        }
    }

    private static @NotNull ItemStack materialIcon(@NotNull GuiIconConfig icon) {
        return new ItemStack(GuiMaterials.itemOr(icon.material(), Material.STONE));
    }

    private static @NotNull HeadId parseHeadId(@NotNull String raw) {
        Objects.requireNonNull(raw, "raw");

        String value = raw.trim();

        if (value.isBlank()) {
            throw new IllegalArgumentException("Head ID cannot be blank.");
        }

        if (value.matches("[1-9][0-9]*")) {
            return HeadId.remote(value);
        }

        return new HeadId(value);
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