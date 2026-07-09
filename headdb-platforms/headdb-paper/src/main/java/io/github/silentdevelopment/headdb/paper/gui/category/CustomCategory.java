package io.github.silentdevelopment.headdb.paper.gui.category;

import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record CustomCategory(@NotNull String id, @NotNull String name, @NotNull String material, @NotNull String description, boolean draft, @NotNull Set<HeadId> headIds) {

    public CustomCategory(@NotNull String id, @NotNull String name, @NotNull String material) {
        this(id, name, material, "Local custom category.", false, Set.of());
    }

    public CustomCategory(@NotNull String id, @NotNull String name, @NotNull String material, @NotNull Set<HeadId> headIds) {
        this(id, name, material, "Local custom category.", false, headIds);
    }

    public CustomCategory {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(headIds, "headIds");

        id = id.trim().toLowerCase(Locale.ROOT);
        name = name.trim();
        material = normalizeIcon(material);
        description = description.trim().isBlank() ? "Local custom category." : description.trim();
        headIds = Set.copyOf(new LinkedHashSet<>(headIds));

        if (id.isBlank()) {
            throw new IllegalArgumentException("Custom category id cannot be blank.");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("Custom category name cannot be blank.");
        }
    }

    public boolean headIcon() {
        return material.startsWith("HEAD:");
    }

    public @NotNull String headIconId() {
        if (!headIcon()) {
            return "";
        }

        return material.substring("HEAD:".length());
    }

    public @NotNull CustomCategory withName(@NotNull String name) {
        return new CustomCategory(id, name, material, description, draft, headIds);
    }

    public @NotNull CustomCategory withMaterial(@NotNull String material) {
        return new CustomCategory(id, name, material, description, draft, headIds);
    }

    public @NotNull CustomCategory withDescription(@NotNull String description) {
        return new CustomCategory(id, name, material, description, draft, headIds);
    }

    public @NotNull CustomCategory withDraft(boolean draft) {
        return new CustomCategory(id, name, material, description, draft, headIds);
    }

    public @NotNull CustomCategory withHeadIds(@NotNull Set<HeadId> headIds) {
        return new CustomCategory(id, name, material, description, draft, headIds);
    }

    public @NotNull HeadCategory toHeadCategory() {
        String displayName = draft ? "[DRAFT] " + name : name;
        return new HeadCategory(id, displayName, description);
    }

    private static @NotNull String normalizeIcon(@NotNull String value) {
        String trimmed = value.trim();
        if (trimmed.toUpperCase(Locale.ROOT).startsWith("HEAD:")) {
            String id = trimmed.substring("HEAD:".length()).trim();
            if (id.isBlank()) {
                return Material.CHEST.name();
            }

            return "HEAD:" + new HeadId(id).toString();
        }

        return GuiMaterials.itemOr(trimmed, Material.CHEST).name();
    }
}
