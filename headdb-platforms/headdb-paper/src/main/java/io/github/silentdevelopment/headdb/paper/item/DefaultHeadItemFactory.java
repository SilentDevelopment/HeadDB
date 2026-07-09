package io.github.silentdevelopment.headdb.paper.item;

import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

public final class DefaultHeadItemFactory implements HeadItemFactory {

    private static final String TEXTURE_BASE_URL = "https://textures.minecraft.net/texture/";
    private static final int MAX_LORE_IDS = 6;

    private final HeadDBPlugin plugin;
    private final NamespacedKey headIdKey;

    public DefaultHeadItemFactory(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.headIdKey = HeadItemIds.key(plugin);
    }

    @Override
    public @NotNull ItemStack create(@NotNull Head head) {
        Objects.requireNonNull(head, "head");

        String textureHash = head.texture().hash();

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);

        item.editMeta(SkullMeta.class, meta -> {
            if (head.id().isPlayer()) {
                applyPlayerProfile(meta, head);
            } else {
                validateTextureHash(head, textureHash);
                meta.setPlayerProfile(profile(head, textureHash));
            }

            meta.displayName(Component.text(head.name(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore(head));
            meta.getPersistentDataContainer().set(headIdKey, PersistentDataType.STRING, head.id().toString());
        });

        return item;
    }

    public boolean isHeadDbItem(@NotNull ItemStack item) {
        Objects.requireNonNull(item, "item");
        return readHeadId(item).isPresent();
    }

    public @NotNull Optional<HeadId> readHeadId(@NotNull ItemStack item) {
        Objects.requireNonNull(item, "item");

        if (GuiMaterials.isAir(item.getType())) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }

        String value = meta.getPersistentDataContainer().get(headIdKey, PersistentDataType.STRING);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(parseCanonicalHeadId(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public @NotNull NamespacedKey headIdKey() {
        return headIdKey;
    }


    private static void applyPlayerProfile(@NotNull SkullMeta meta, @NotNull Head head) {
        try {
            UUID uuid = UUID.fromString(head.id().key());
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            return;
        } catch (IllegalArgumentException ignored) {
            // Fall through to deterministic profile.
        }

        PlayerProfile profile = Bukkit.createProfileExact(profileId(head), profileName(head));
        meta.setPlayerProfile(profile);
    }

    private static @NotNull PlayerProfile profile(@NotNull Head head, @NotNull String textureHash) {
        PlayerProfile profile = Bukkit.createProfileExact(profileId(head), profileName(head));
        profile.setProperty(new ProfileProperty("textures", textureValue(textureHash)));
        return profile;
    }

    private static @NotNull UUID profileId(@NotNull Head head) {
        return UUID.nameUUIDFromBytes(("headdb" + head.id()).getBytes(StandardCharsets.UTF_8));
    }

    private static @NotNull String profileName(@NotNull Head head) {
        String raw = head.id().source().name().toLowerCase() + "_" + head.id().key();
        String sanitized = raw.replaceAll("[^A-Za-z0-9_]", "_");

        if (sanitized.isBlank()) {
            return "HeadDB";
        }

        if (sanitized.length() > 16) {
            return sanitized.substring(0, 16);
        }

        return sanitized;
    }

    private static @NotNull String textureValue(@NotNull String textureHash) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + TEXTURE_BASE_URL + textureHash + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }


    private static @NotNull HeadId parseCanonicalHeadId(@NotNull String raw) {
        String value = raw.trim();

        if (value.regionMatches(true, 0, "remote:", 0, "remote:".length())) {
            String id = value.substring("remote:".length()).trim();
            return HeadId.remote(Integer.parseInt(id));
        }

        if (value.regionMatches(true, 0, "custom:", 0, "custom:".length())) {
            String id = value.substring("custom:".length()).trim();

            if (id.isEmpty()) {
                throw new IllegalArgumentException("Custom head ID cannot be empty.");
            }

            return HeadId.custom(id);
        }

        if (value.regionMatches(true, 0, "player:", 0, "player:".length())) {
            String id = value.substring("player:".length()).trim();
            return new HeadId("player:" + id);
        }

        throw new IllegalArgumentException("Unsupported HeadDB item ID: " + raw);
    }

    private static void validateTextureHash(@NotNull Head head, String textureHash) {
        if (textureHash == null || textureHash.isBlank()) {
            throw new IllegalArgumentException("Head texture hash cannot be empty for " + head.id() + ".");
        }

        for (int index = 0; index < textureHash.length(); index++) {
            char character = textureHash.charAt(index);

            if (character >= '0' && character <= '9') {
                continue;
            }

            if (character >= 'a' && character <= 'f') {
                continue;
            }

            if (character >= 'A' && character <= 'F') {
                continue;
            }

            throw new IllegalArgumentException("Head texture hash contains an invalid character for " + head.id() + ".");
        }
    }

    private static @NotNull Component name(@NotNull Head head) {
        return Component.text(head.name(), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false);
    }

    private @NotNull List<Component> lore(@NotNull Head head) {
        Objects.requireNonNull(head, "head");

        List<Component> lore = new ArrayList<>();

        try {
            plugin.headRegistry().lore(head.id()).ifPresent(lines -> {
                for (String line : lines) {
                    lore.add(GuiItems.miniOrWhite(line));
                }
            });
        } catch (IllegalStateException ignored) {
            // Registry is unavailable only during very early bootstrap.
        }

        if (!lore.isEmpty()) {
            lore.add(Component.empty());
        }

        if (!head.id().isPlayer()) {
            lore.add(line("Category", head.category()));
            lore.add(line("Tags", join(head.tags())));
            lore.add(line("Collections", join(head.collections())));
        }

        return cleanLore(lore);
    }

    private static @NotNull String values(@NotNull Collection<String> values) {
        Objects.requireNonNull(values, "values");

        if (values.isEmpty()) {
            return "none";
        }

        return String.join(", ", values);
    }

    private static @NotNull List<Component> cleanLore(@NotNull List<Component> lore) {
        Objects.requireNonNull(lore, "lore");

        List<Component> result = new ArrayList<>();

        for (Component line : lore) {
            result.add(line.decoration(TextDecoration.ITALIC, false));
        }

        return List.copyOf(result);
    }

    private static @NotNull Component line(@NotNull String key, @NotNull String value) {
        return Component.text(key + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false);
    }

    private static @NotNull String join(@NotNull Collection<String> values) {
        if (values.isEmpty()) {
            return "none";
        }

        StringJoiner joiner = new StringJoiner(", ");
        int index = 0;

        for (String value : values) {
            if (index >= MAX_LORE_IDS) {
                joiner.add("+" + (values.size() - MAX_LORE_IDS) + " more");
                break;
            }

            joiner.add(value);
            index++;
        }

        return joiner.toString();
    }

}
