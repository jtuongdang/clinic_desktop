package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

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
    void migrationRunnerRegistersV7ImmediatelyAfterV6() throws ReflectiveOperationException {
        var field = DatabaseMigrationRunner.class.getDeclaredField("MIGRATION_FILES");
        field.setAccessible(true);
        String[] migrationFiles = (String[]) field.get(null);

        assertTrue(migrationFiles.length >= 2);
        assertTrue(migrationFiles[migrationFiles.length - 2].endsWith("V6__Enhance_Audit_Tables_Append_Only.sql"));
        assertTrue(migrationFiles[migrationFiles.length - 1].endsWith("V7__Harden_Permission_Matrix_Defaults.sql"));
    }

    private static String readScript() throws IOException {
        try (InputStream input = PermissionMatrixMigrationScriptTest.class.getClassLoader()
                .getResourceAsStream(SCRIPT_PATH)) {
            if (input == null) throw new AssertionError("Missing migration script: " + SCRIPT_PATH);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
