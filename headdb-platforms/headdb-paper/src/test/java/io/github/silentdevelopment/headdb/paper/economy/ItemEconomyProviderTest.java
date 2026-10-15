package io.github.silentdevelopment.headdb.paper.economy;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemEconomyProviderTest {

    @Test
    void formatsSingularAndPluralAmounts() {
        ItemEconomyProvider provider = new ItemEconomyProvider(Material.DIAMOND, "Diamond", "Diamonds");

        assertEquals("1 Diamond", provider.format(1.0D));
        assertEquals("2 Diamonds", provider.format(2.0D));
    }

    @Test
    void validatesWholeAmounts() {
        assertTrue(ItemEconomyProvider.isWholeAmount(0.0D));
        assertTrue(ItemEconomyProvider.isWholeAmount(12.0D));
        assertFalse(ItemEconomyProvider.isWholeAmount(2.5D));
        assertFalse(ItemEconomyProvider.isWholeAmount(Double.NaN));
    }

    @Test
    void withdrawsAcrossMultipleStacksWithoutTouchingOtherItems() {
        ItemStack[] storage = {
                new ItemStack(Material.DIAMOND, 1),
                new ItemStack(Material.STONE, 32),
                new ItemStack(Material.DIAMOND, 4)
        };

        ItemStack[] updated = ItemEconomyProvider.withdrawStorage(storage, Material.DIAMOND, 3);

        assertEquals(1, storage[0].getAmount());
        assertEquals(4, storage[2].getAmount());
        assertNull(updated[0]);
        assertEquals(Material.STONE, updated[1].getType());
        assertEquals(32, updated[1].getAmount());
        assertEquals(Material.DIAMOND, updated[2].getType());
        assertEquals(2, updated[2].getAmount());
    }

    @Test
    void insufficientCurrencyDoesNotProduceModifiedStorage() {
        ItemStack[] storage = {
                new ItemStack(Material.DIAMOND, 2),
                new ItemStack(Material.EMERALD, 5)
        };

        ItemStack[] updated = ItemEconomyProvider.withdrawStorage(storage, Material.DIAMOND, 3);

        assertNull(updated);
        assertEquals(2, storage[0].getAmount());
        assertEquals(5, storage[1].getAmount());
    }
}
