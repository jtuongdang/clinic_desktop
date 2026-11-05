package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionMatrixMigrationScriptTest {

    private static final String SCRIPT_PATH = "migrations/V7__Harden_Permission_Matrix_Defaults.sql";

    @Test
    void validatesUnsafeDataBeforeSchemaChangesAndNeverRewritesPermissionRows() throws IOException {
        String script = readScript();
        String validationSection = script.substring(0, script.toUpperCase().indexOf("\nGO"));
        String upperScript = script.toUpperCase();

        assertTrue(validationSection.contains("phamViDuLieu IS NULL OR phamViDuLieu <> 'KHOA_PHONG'"));
        assertTrue(validationSection.contains("HAVING COUNT_BIG(*) > 1"));
        assertTrue(validationSection.indexOf("THROW 51070") < validationSection.indexOf("THROW 51071"));
        assertTrue(Pattern.compile("(?is)IF EXISTS.*?THROW 51070").matcher(validationSection).find());
        assertTrue(Pattern.compile("(?is)IF EXISTS.*?THROW 51071").matcher(validationSection).find());

        int firstDdl = upperScript.indexOf("ALTER TABLE");
        assertTrue(firstDdl > validationSection.length(), "All validation must precede schema DDL");
        assertTrue(!upperScript.contains("INSERT INTO DBO.MATRANPHANQUYEN"));
        assertTrue(!upperScript.contains("UPDATE DBO.MATRANPHANQUYEN"));
        assertTrue(!upperScript.contains("DELETE FROM DBO.MATRANPHANQUYEN"));
        assertTrue(upperScript.contains("CHECK (ISDELETED = 1 OR DUOCPHEP = 0"));
        assertTrue(upperScript.contains("WHERE ISDELETED = 0"));
        assertTrue(upperScript.contains("CREATE UNIQUE INDEX UX_MATRANPHANQUYEN_ACTIVE_ROLE_MODULE_ACTION"));
    }

    @Test
    void migrationRunnerRegistersV1ThroughV9InOrder() throws ReflectiveOperationException {
        var field = DatabaseMigrationRunner.class.getDeclaredField("MIGRATION_FILES");
        field.setAccessible(true);
        String[] migrationFiles = (String[]) field.get(null);

        assertEquals(List.of(
                "migrations/V1__Initial_Clinic_Schema.sql",
                "migrations/V2__Seed_Initial_Data.sql",
                "migrations/V3__Repair_Demo_Account_Passwords.sql",
                "migrations/V4__Enforce_Active_Patient_CCCD_Uniqueness.sql",
                "migrations/V5__Seed_Session_Timeout_Parameters.sql",
                "migrations/V6__Enhance_Audit_Tables_Append_Only.sql",
                "migrations/V7__Harden_Permission_Matrix_Defaults.sql",
                "migrations/V8__Seed_Approved_Permission_Grants.sql",
                "migrations/V9__Enforce_Active_Patient_Facility_Uniqueness.sql"
        ), List.of(migrationFiles));
    }

    private static String readScript() throws IOException {
        try (InputStream input = PermissionMatrixMigrationScriptTest.class.getClassLoader()
                .getResourceAsStream(SCRIPT_PATH)) {
            if (input == null) throw new AssertionError("Missing migration script: " + SCRIPT_PATH);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
