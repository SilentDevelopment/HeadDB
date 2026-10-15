package io.github.silentdevelopment.headdb.paper.gui.category;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryFacetTest {

    @Test
    void matchesConfiguredTagPrefix() {
        CategoryFacet facet = new CategoryFacet("alphabet", "Alphabet Styles", "font-", "Font (", ")");

        assertTrue(facet.matchesTag("font-oak"));
        assertTrue(facet.matchesTag(" FONT-DIAMOND "));
        assertFalse(facet.matchesTag("alphabet-greek"));
    }

    @Test
    void derivesDisplayNameFromTagMetadata() {
        CategoryFacet facet = new CategoryFacet("alphabet", "Alphabet Styles", "font-", "Font (", ")");

        assertEquals("Oak", facet.displayName("font-oak", "Font (Oak)"));
        assertEquals("GUI", facet.displayName("font-gui", "Font (GUI)"));
    }

    @Test
    void fallsBackToHumanizedTagId() {
        CategoryFacet facet = new CategoryFacet("alphabet", "Alphabet Styles", "font-", "Font (", ")");

        assertEquals("Cherry Planks", facet.displayName("font-cherry-planks", "Unexpected"));
    }

    @Test
    void registryExposesAlphabetFacetOnly() {
        assertTrue(CategoryFacetRegistry.find("alphabet").isPresent());
        assertTrue(CategoryFacetRegistry.find(" ALPHABET ").isPresent());
        assertFalse(CategoryFacetRegistry.find("animals").isPresent());
    }
}
