package vn.clinicmanagement.domain.patient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TienSuBenhTest {

    @Test
    void defaultsToUnscopedForLegacyHydration() {
        TienSuBenh history = new TienSuBenh();

        assertNull(history.getMaCoSo());
        assertNull(history.getMaKhoa());
        assertFalse(history.hasExactOwnershipScope(1, 1));
    }

    @Test
    void assignsAndMatchesOnlyTheExactPositivePair() {
        TienSuBenh history = new TienSuBenh();

        history.setOwnershipScope(7, 12);

        assertTrue(history.hasExactOwnershipScope(7, 12));
        assertFalse(history.hasExactOwnershipScope(7, 13));
        assertFalse(history.hasExactOwnershipScope(8, 12));
        assertFalse(history.hasExactOwnershipScope(0, 12));
    }

    @Test
    void rejectsPartialAndNonPositiveOwnershipPairsWithoutChangingExistingScope() {
        TienSuBenh history = new TienSuBenh();
        history.setOwnershipScope(7, 12);

        assertThrows(IllegalArgumentException.class, () -> history.setOwnershipScope(7, null));
        assertThrows(IllegalArgumentException.class, () -> history.setOwnershipScope(null, 12));
        assertThrows(IllegalArgumentException.class, () -> history.setOwnershipScope(0, 12));
        assertThrows(IllegalArgumentException.class, () -> history.setOwnershipScope(7, -1));
        assertTrue(history.hasExactOwnershipScope(7, 12));
    }

    @Test
    void acceptsNullPairForLegacyHydrationButNotAsAnExactScope() {
        TienSuBenh history = new TienSuBenh();

        history.setOwnershipScope(null, null);

        assertNull(history.getMaCoSo());
        assertNull(history.getMaKhoa());
        assertFalse(history.hasExactOwnershipScope(7, 12));
    }
}
