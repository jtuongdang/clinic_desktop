package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivePatientFacilityMigrationTest {
    private static final String SCRIPT_PATH = "migrations/V9__Enforce_Active_Patient_Facility_Uniqueness.sql";

    @Test
    void migrationFailsSafelyOnDuplicateActivePairsAndCreatesFilteredUniqueIndex() throws IOException {
        String migration;
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(SCRIPT_PATH)) {
            if (stream == null) throw new AssertionError("Missing migration script: " + SCRIPT_PATH);
            migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        String normalized = migration.replaceAll("\\s+", " ").toUpperCase();
        assertTrue(normalized.contains("FROM BENHNHANCOSO WHERE ISDELETED = 0 GROUP BY MABENHNHAN, MACOSO HAVING COUNT(*) > 1"));
        assertTrue(normalized.contains("THROW 51090"));
        assertTrue(normalized.contains("CREATE UNIQUE NONCLUSTERED INDEX UX_BENHNHANCOSO_ACTIVE_PATIENT_FACILITY"));
        assertTrue(normalized.contains("ON BENHNHANCOSO(MABENHNHAN, MACOSO) WHERE ISDELETED = 0"));
    }
}
