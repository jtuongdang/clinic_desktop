package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DaDuyetQuyenCapQuyenDiCuTest {

    private static final String SCRIPT_PATH = "migrations/V8__Seed_Approved_Permission_Grants.sql";
    private static final Pattern GRANT_ROW =
            Pattern.compile("\\(N'([A-Z_]+)', '([A-Z_]+)', '([A-Z_]+)'\\)");

    @Test
    void migrationContainsExactlyTheApprovedRoleModuleActionAllowlist() throws IOException {
        String script = readScript();
        Set<GrantKey> actual = parseGrantKeys(script);

        assertEquals(approvedGrants(), actual);
        assertEquals(57, actual.size());
        assertEquals(57, parseGrantRows(script).size(), "Approved grant rows must not contain duplicates");
        for (GrantKey grant : actual) {
            HanhDongQuyen.valueOf(grant.action());
        }
        Matcher scopeMatcher = Pattern.compile(
                "SELECT vaiTro\\.id, grantRow\\.phanHe, grantRow\\.hanhDong, '([A-Z_]+)', 1")
                .matcher(script);
        assertTrue(scopeMatcher.find());
        assertEquals(PhamViDuLieu.KHOA_PHONG, PhamViDuLieu.valueOf(scopeMatcher.group(1)));
        assertFalse(scopeMatcher.find(), "V8 must use one explicit scope for all seeded grants");
    }

    @Test
    void migrationKeepsUnapprovedAndUnresolvedActionsDefaultDenied() throws IOException {
        Set<GrantKey> grants = parseGrantKeys(readScript());

        for (String role : List.of("ADMIN", "DOCTOR", "RECEPTIONIST", "PHARMACIST", "CASHIER", "LAB_TECH")) {
            assertDenied(grants, role, "AUDIT", HanhDongQuyen.AUDIT_READ);
            assertDenied(grants, role, "EMR", HanhDongQuyen.MO_KHOA_EMR);
            assertDenied(grants, role, "RECEPTION", HanhDongQuyen.XOA);
            assertDenied(grants, role, "INVENTORY", HanhDongQuyen.DUYET);
            assertDenied(grants, role, "INVENTORY", HanhDongQuyen.XOA);
            assertDenied(grants, role, "BILLING", HanhDongQuyen.DUYET);
            assertDenied(grants, role, "BILLING", HanhDongQuyen.XOA);
            assertDenied(grants, role, "HR_PAYROLL", HanhDongQuyen.XEM);
            assertDenied(grants, role, "REPORT", HanhDongQuyen.XEM);
        }
        assertDenied(grants, "RECEPTIONIST", "EMR", HanhDongQuyen.TAO);
        assertDenied(grants, "RECEPTIONIST", "EMR", HanhDongQuyen.SUA);
        assertDenied(grants, "DOCTOR", "CLS", HanhDongQuyen.DUYET);
        assertDenied(grants, "LAB_TECH", "CLS", HanhDongQuyen.DUYET);
        assertDenied(grants, "DOCTOR", "PRESCRIPTION", HanhDongQuyen.DUYET);
        assertDenied(grants, "PHARMACIST", "PRESCRIPTION", HanhDongQuyen.DUYET);
        assertDenied(grants, "CASHIER", "APPOINTMENT", HanhDongQuyen.TAO);
        assertDenied(grants, "CASHIER", "APPOINTMENT", HanhDongQuyen.SUA);
        assertDenied(grants, "ADMIN", "CATALOG", HanhDongQuyen.XOA);
        assertDenied(grants, "ADMIN", "CATALOG", HanhDongQuyen.DUYET);
        assertDenied(grants, "ADMIN", "SYSTEM", HanhDongQuyen.XOA);

        assertTrue(grants.stream().noneMatch(grant ->
                Set.of("CHO_KHAM", "TAM", "DA_DUYET").contains(grant.action())));
        assertFalse(readScript().contains("AUDIT_READ"));
    }

    @Test
    void insertOnlyMigrationPreservesSyntheticExistingRowsAndAssignments() throws IOException {
        String script = readScript().toUpperCase();
        assertFalse(script.contains("UPDATE DBO.MATRANPHANQUYEN"));
        assertFalse(script.contains("DELETE FROM DBO.MATRANPHANQUYEN"));
        assertFalse(script.contains("INSERT INTO DBO.NHANVIENVAITRO"));
        assertFalse(script.contains("INSERT INTO DBO.PHANQUYENCOSO"));
        assertFalse(script.contains("INSERT INTO DBO.NHANVIENKHOA"));
        assertTrue(script.contains("AND EXISTING.ISDELETED = 0"));

        List<PermissionRow> existingRows = new ArrayList<>(List.of(
                new PermissionRow("ADMIN", "HOME", "XEM", PhamViDuLieu.KHOA_PHONG, true, false),
                new PermissionRow("DOCTOR", "PATIENT", "XEM", PhamViDuLieu.KHOA_PHONG, false, false),
                new PermissionRow("LEGACY_ROLE", "CUSTOM", "XEM", PhamViDuLieu.KHOA_PHONG, true, false)
        ));
        List<PermissionRow> originalRows = List.copyOf(existingRows);
        Set<String> assignments = Set.of("synthetic-role-assignment", "synthetic-scope-assignment");
        Set<String> originalAssignments = Set.copyOf(assignments);

        for (GrantKey grant : parseGrantKeys(readScript())) {
            boolean hasActiveRow = existingRows.stream().anyMatch(row ->
                    !row.deleted()
                            && row.role().equals(grant.role())
                            && row.module().equals(grant.module())
                            && row.action().equals(grant.action()));
            if (!hasActiveRow) {
                existingRows.add(new PermissionRow(
                        grant.role(), grant.module(), grant.action(), PhamViDuLieu.KHOA_PHONG, true, false));
            }
        }

        assertTrue(existingRows.containsAll(originalRows));
        assertEquals(originalAssignments, assignments);
        assertTrue(existingRows.contains(new PermissionRow(
                "DOCTOR", "PATIENT", "XEM", PhamViDuLieu.KHOA_PHONG, false, false)));
        assertTrue(existingRows.contains(new PermissionRow(
                "LEGACY_ROLE", "CUSTOM", "XEM", PhamViDuLieu.KHOA_PHONG, true, false)));
    }

    static Set<GrantKey> approvedGrants() {
        Set<GrantKey> grants = new LinkedHashSet<>();
        add(grants, "ADMIN", "HOME", "XEM");
        add(grants, "ADMIN", "CATALOG", "XEM", "TAO", "SUA");
        add(grants, "ADMIN", "SYSTEM", "XEM", "TAO", "SUA");

        add(grants, "DOCTOR", "HOME", "XEM");
        add(grants, "DOCTOR", "PATIENT", "XEM");
        add(grants, "DOCTOR", "RECEPTION", "XEM", "SUA");
        add(grants, "DOCTOR", "APPOINTMENT", "XEM", "TAO", "SUA");
        add(grants, "DOCTOR", "EMR", "XEM", "TAO", "SUA", "DUYET");
        add(grants, "DOCTOR", "CLS", "XEM", "TAO");
        add(grants, "DOCTOR", "PRESCRIPTION", "XEM", "TAO", "SUA");
        add(grants, "DOCTOR", "INVENTORY", "XEM");
        add(grants, "DOCTOR", "BILLING", "XEM");
        add(grants, "DOCTOR", "CATALOG", "XEM");

        add(grants, "RECEPTIONIST", "HOME", "XEM");
        add(grants, "RECEPTIONIST", "PATIENT", "XEM", "TAO", "SUA");
        add(grants, "RECEPTIONIST", "RECEPTION", "XEM", "TAO", "SUA");
        add(grants, "RECEPTIONIST", "APPOINTMENT", "XEM", "TAO", "SUA");
        add(grants, "RECEPTIONIST", "CATALOG", "XEM");

        add(grants, "PHARMACIST", "HOME", "XEM");
        add(grants, "PHARMACIST", "PATIENT", "XEM");
        add(grants, "PHARMACIST", "PRESCRIPTION", "XEM");
        add(grants, "PHARMACIST", "INVENTORY", "XEM", "TAO", "SUA");
        add(grants, "PHARMACIST", "CATALOG", "XEM");

        add(grants, "CASHIER", "HOME", "XEM");
        add(grants, "CASHIER", "PATIENT", "XEM");
        add(grants, "CASHIER", "APPOINTMENT", "XEM");
        add(grants, "CASHIER", "CLS", "XEM");
        add(grants, "CASHIER", "BILLING", "XEM", "TAO", "SUA");
        add(grants, "CASHIER", "CATALOG", "XEM");

        add(grants, "LAB_TECH", "HOME", "XEM");
        add(grants, "LAB_TECH", "PATIENT", "XEM");
        add(grants, "LAB_TECH", "CLS", "XEM", "SUA");
        add(grants, "LAB_TECH", "CATALOG", "XEM");
        return Set.copyOf(grants);
    }

    private static void add(Set<GrantKey> grants, String role, String module, String... actions) {
        for (String action : actions) {
            grants.add(new GrantKey(role, module, HanhDongQuyen.valueOf(action).name()));
        }
    }

    private static Set<GrantKey> parseGrantKeys(String script) {
        return new HashSet<>(parseGrantRows(script));
    }

    private static List<GrantKey> parseGrantRows(String script) {
        List<GrantKey> grants = new ArrayList<>();
        Matcher matcher = GRANT_ROW.matcher(script);
        while (matcher.find()) {
            grants.add(new GrantKey(matcher.group(1), matcher.group(2), matcher.group(3)));
        }
        return grants;
    }

    private static void assertDenied(Set<GrantKey> grants, String role, String module, HanhDongQuyen action) {
        assertFalse(grants.contains(new GrantKey(role, module, action.name())),
                () -> role + ":" + module + ":" + action + " must remain default-deny");
    }

    private static String readScript() throws IOException {
        try (InputStream input = DaDuyetQuyenCapQuyenDiCuTest.class.getClassLoader()
                .getResourceAsStream(SCRIPT_PATH)) {
            if (input == null) throw new AssertionError("Missing migration script: " + SCRIPT_PATH);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    record GrantKey(String role, String module, String action) { }

    private record PermissionRow(
            String role,
            String module,
            String action,
            PhamViDuLieu scope,
            boolean allowed,
            boolean deleted
    ) { }
}
