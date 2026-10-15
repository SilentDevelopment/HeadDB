package io.github.silentdevelopment.headdb.paper.economy;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

public final class ItemEconomyProvider implements EconomyProvider {

    public static final String MATCH_MATERIAL = "material";

    private final Material material;
    private final String singular;
    private final String plural;

    public ItemEconomyProvider(@NotNull Material material, @NotNull String singular, @NotNull String plural) {
        this.material = Objects.requireNonNull(material, "material");
        this.singular = Objects.requireNonNull(singular, "singular");
        this.plural = Objects.requireNonNull(plural, "plural");
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public @NotNull String format(double amount) {
        Long units = wholeUnits(amount);
        if (units == null) {
            return String.format(Locale.ROOT, "%.2f %s", amount, plural);
        }

        return units + " " + (units == 1L ? singular : plural);
    }

    @Override
    public @NotNull EconomyWithdrawal withdraw(@NotNull Player player, double amount) {
        Objects.requireNonNull(player, "player");

        Long required = wholeUnits(amount);
        if (required == null) {
            return new EconomyWithdrawal(false, "Item economy requires whole-number prices.");
        }

        if (required <= 0L) {
            return new EconomyWithdrawal(true, "");
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack[] updated = withdrawStorage(inventory.getStorageContents(), material, required);
        if (updated == null) {
            return new EconomyWithdrawal(false, "Insufficient item currency.");
        }

        inventory.setStorageContents(updated);
        return new EconomyWithdrawal(true, "");
    }

    static boolean isWholeAmount(double amount) {
        return wholeUnits(amount) != null;
    }

    static @Nullable ItemStack[] withdrawStorage(@NotNull ItemStack[] storage, @NotNull Material material, long required) {
        Objects.requireNonNull(storage, "storage");
        Objects.requireNonNull(material, "material");

        if (required < 0L) {
            throw new IllegalArgumentException("Required amount cannot be negative.");
        }

        int[] matchingAmounts = new int[storage.length];
        for (int index = 0; index < storage.length; index++) {
            ItemStack item = storage[index];
            if (item != null && item.getType() == material) {
                matchingAmounts[index] = item.getAmount();
            }
        }

        int[] remainingAmounts = withdrawAmounts(matchingAmounts, required);
        if (remainingAmounts == null) {
            return null;
        }

        ItemStack[] updated = new ItemStack[storage.length];
        for (int index = 0; index < storage.length; index++) {
            ItemStack item = storage[index];
            updated[index] = item == null ? null : item.clone();

            if (item == null || item.getType() != material) {
                continue;
            }

            int remaining = remainingAmounts[index];
            if (remaining <= 0) {
                updated[index] = null;
            } else {
                updated[index].setAmount(remaining);
            }
        }

        return updated;
    }

    static @Nullable int[] withdrawAmounts(@NotNull int[] amounts, long required) {
        Objects.requireNonNull(amounts, "amounts");

        if (required < 0L) {
            throw new IllegalArgumentException("Required amount cannot be negative.");
        }

        long available = 0L;
        for (int amount : amounts) {
            if (amount < 0) {
                throw new IllegalArgumentException("Stack amount cannot be negative.");
            }

            available += amount;
            if (available >= required) {
                break;
            }
        }

        if (available < required) {
            return null;
        }

        int[] updated = amounts.clone();
        long remaining = required;

        for (int index = 0; index < updated.length && remaining > 0L; index++) {
            int remove = (int) Math.min(remaining, updated[index]);
            updated[index] -= remove;
            remaining -= remove;
        }

        return updated;
    }

    private static @Nullable Long wholeUnits(double amount) {
        if (!Double.isFinite(amount) || amount < 0.0D || amount != Math.rint(amount)) {
            return null;
        }

        if (amount > Long.MAX_VALUE) {
            return null;
        }

        return (long) amount;
    }
}
