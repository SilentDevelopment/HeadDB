package io.github.silentdevelopment.headdb.paper.economy;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
    void withdrawsAcrossMultipleStacks() {
        int[] amounts = {1, 0, 4};

        int[] updated = ItemEconomyProvider.withdrawAmounts(amounts, 3);

        assertArrayEquals(new int[]{0, 0, 2}, updated);
        assertArrayEquals(new int[]{1, 0, 4}, amounts);
    }

    @Test
    void insufficientCurrencyDoesNotProduceModifiedAmounts() {
        int[] amounts = {2, 0, 0};

        int[] updated = ItemEconomyProvider.withdrawAmounts(amounts, 3);

        assertNull(updated);
        assertArrayEquals(new int[]{2, 0, 0}, amounts);
    }
}
