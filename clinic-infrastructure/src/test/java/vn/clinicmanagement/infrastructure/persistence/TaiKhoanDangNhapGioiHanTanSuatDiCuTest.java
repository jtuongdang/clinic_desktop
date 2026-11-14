package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaiKhoanDangNhapGioiHanTanSuatDiCuTest {
    private static final String SCRIPT_PATH = "migrations/V11__Add_Account_Login_Throttle.sql";

    @Test
    void addsDefaultSafeThrottleColumnsWithoutRewritingEmployeeRows() throws IOException {
        String script = readScript();
        String upper = script.toUpperCase();

        assertTrue(upper.contains("LOGINFAILUREWINDOWSTARTEDAT DATETIME2(7) NULL"));
        assertTrue(upper.contains("LOGINFAILURECOUNT INT NOT NULL"));
        assertTrue(upper.contains("DEFAULT (0) WITH VALUES"));
        assertTrue(upper.contains("LOGINLASTFAILUREAT DATETIME2(7) NULL"));
        assertTrue(upper.contains("LOGINDELAYSECONDS INT NOT NULL"));
        assertTrue(upper.contains("LOGINNEXTALLOWEDAT DATETIME2(7) NULL"));
        assertTrue(upper.contains("CK_NHANVIEN_LOGINTHROTTLE_STATE"));
        assertTrue(upper.contains("IN (0, 30, 60, 120, 240, 480, 900)"));
        assertTrue(upper.contains("COL_LENGTH('DBO.NHANVIEN'"));
        assertFalse(Pattern.compile("(?im)^\\s*(?:UPDATE|DELETE)\\s+(?:DBO\\.)?NHANVIEN\\b")
                .matcher(script).find(), "V11 must not rewrite or delete employee data");
        assertFalse(upper.contains("DROP DATABASE"));
        assertFalse(upper.contains("DROP TABLE"));
    }

    private static String readScript() throws IOException {
        try (InputStream input = TaiKhoanDangNhapGioiHanTanSuatDiCuTest.class.getClassLoader()
                .getResourceAsStream(SCRIPT_PATH)) {
            if (input == null) throw new AssertionError("Missing migration script: " + SCRIPT_PATH);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
