package lk.com.synsoft.offlinepos.dto.auth;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The feature ids are the primary key every rights row points at, so the thing
 * worth testing is that they are exactly the ids in the database and that no two
 * features claim the same one.
 */
class FeatureTest {

    /**
     * U+00A0. Two of the seeded feature names carried one of these instead of a
     * plain space, which V5 corrects in the database; these labels must not have
     * inherited it.
     */
    private static final char NON_BREAKING_SPACE = ' ';

    @Test
    @DisplayName("there are 61 features, matching the seeded sysfeatures rows")
    void countMatchesTheDatabase() {
        assertEquals(61, Feature.values().length);
    }

    @Test
    @DisplayName("no two features share an SFID")
    void idsAreUnique() {
        Set<Integer> seen = new HashSet<>();

        for (Feature feature : Feature.values()) {
            assertTrue(seen.add(feature.id()), "SFID " + feature.id() + " is claimed twice.");
        }
    }

    @Test
    @DisplayName("the deleted ids are absent, and 70 was never issued")
    void gapsAreRespected() {
        for (int id = 23; id <= 30; id++) {
            assertTrue(Feature.byId(id).isEmpty(), "SFID " + id + " should not exist.");
        }
        assertTrue(Feature.byId(60).isEmpty());
        assertTrue(Feature.byId(61).isEmpty());
        assertTrue(Feature.byId(70).isEmpty());
    }

    @Test
    @DisplayName("an unknown id is ignored rather than fatal")
    void unknownIdIsEmpty() {
        // A database written by a newer build may hold rights for a feature this
        // one has never heard of. Dropping that row beats refusing the login.
        assertTrue(Feature.byId(9999).isEmpty());
        assertTrue(Feature.byId(0).isEmpty());
    }

    @Test
    @DisplayName("ids resolve back to the feature that claims them")
    void resolvesById() {
        assertEquals(Feature.PRODUCTS, Feature.byId(16).orElseThrow());
        assertEquals(Feature.RETAIL_SALES, Feature.byId(7).orElseThrow());
        assertEquals(Feature.RPT_BATCH_WISE_SALE, Feature.byId(72).orElseThrow());
    }

    @Test
    @DisplayName("every feature belongs to a module, and the reports module holds 33")
    void groupsByModule() {
        assertEquals(33, Feature.of(Module.REPORTS).size());
        assertEquals(7, Feature.of(Module.INVENTORY).size());
        assertEquals(6, Feature.of(Module.ORDERS).size());
        assertEquals(4, Feature.of(Module.ACCOUNTS).size());
        assertEquals(11, Feature.of(Module.SETTINGS).size());

        int total = 0;
        for (Module module : Module.values()) {
            total += Feature.of(module).size();
        }
        assertEquals(Feature.values().length, total);
    }

    @Test
    @DisplayName("labels are clean - no stray non-breaking spaces from the legacy dump")
    void labelsAreClean() {
        for (Feature feature : Feature.values()) {
            assertFalse(feature.label().isBlank(), feature + " has no label.");
            assertFalse(feature.label().indexOf(NON_BREAKING_SPACE) >= 0,
                    feature + " still carries a non-breaking space.");
            assertEquals(feature.label().trim(), feature.label(), feature + " has stray whitespace.");
        }
    }
}
