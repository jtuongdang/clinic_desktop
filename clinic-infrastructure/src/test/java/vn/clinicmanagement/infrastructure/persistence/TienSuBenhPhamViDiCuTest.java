package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TienSuBenhPhamViDiCuTest {

    private static final String SCRIPT_PATH = "migrations/V10__Add_TienSuBenh_Facility_Department_Scope.sql";

    @Test
    void addsNullablePairConstraintsAndQueryIndexWithoutBackfillingLegacyRows() throws IOException {
        String script = readScript();
        String upper = script.toUpperCase();

        assertTrue(upper.contains("ADD MACOSO INT NULL"));
        assertTrue(upper.contains("ADD MAKHOA INT NULL"));
        assertTrue(upper.contains("CK_TIENSUBENH_FACILITY_DEPARTMENT_PAIR"));
        assertTrue(Pattern.compile("(?is)CHECK\\s*\\(\\s*\\(MACOSO IS NULL AND MAKHOA IS NULL\\)"
                + "\\s*OR\\s*\\(MACOSO IS NOT NULL AND MAKHOA IS NOT NULL\\)\\s*\\)")
                .matcher(upper).find());
        assertTrue(upper.contains("FOREIGN KEY (MAKHOA, MACOSO) REFERENCES DBO.KHOA(ID, MACOSO)"));
        assertTrue(upper.contains("IX_TIENSUBENH_FACILITY_DEPARTMENT_PATIENT"));
        assertTrue(upper.contains("TR_TIENSUBENH_REQUIRE_SCOPE_ON_INSERT"));
        assertTrue(upper.contains("AFTER INSERT"));
        assertTrue(upper.contains("THROW 51010"));
        assertFalse(Pattern.compile("(?im)^\\s*UPDATE\\s+(?:DBO\\.)?TIENSUBENH\\b").matcher(script).find(),
                "V10 must preserve legacy rows without inferred scope");
        assertFalse(Pattern.compile("(?im)^\\s*INSERT\\s+INTO\\s+(?:DBO\\.)?TIENSUBENH\\b")
                .matcher(script).find());
        assertTrue(upper.contains("COL_LENGTH('DBO.TIENSUBENH', 'MACOSO') IS NULL"));
        assertTrue(upper.contains("COL_LENGTH('DBO.TIENSUBENH', 'MAKHOA') IS NULL"));
    }

    private static String readScript() throws IOException {
        try (InputStream input = TienSuBenhPhamViDiCuTest.class.getClassLoader()
                .getResourceAsStream(SCRIPT_PATH)) {
            if (input == null) throw new AssertionError("Missing migration script: " + SCRIPT_PATH);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
