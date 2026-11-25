package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuyenMaTranDiCuKichBanTest {

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
    void migrationRunnerRegistersV1ThroughV16InOrder() throws ReflectiveOperationException {
        var field = CoSoDuLieuDiCuChay.class.getDeclaredField("MIGRATION_FILES");
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
                "migrations/V9__Enforce_Active_Patient_Facility_Uniqueness.sql",
                "migrations/V10__Add_TienSuBenh_Facility_Department_Scope.sql",
                "migrations/V11__Add_Account_Login_Throttle.sql",
                "migrations/V12__Add_Reception_Price_Snapshot.sql",
                "migrations/V13__Repair_Nonpositive_Reception_Ids.sql",
                "migrations/V14__Enforce_Visit_Billing_Ledger.sql",
            "migrations/V15__Add_Drug_Directory_Metadata.sql",
            "migrations/V16__Add_Drug_Full_Form.sql",
            "migrations/V17__Retail_Stock_Issue.sql",
            "migrations/V18__Seed_Drug_Classification_And_Catalog.sql",
            "migrations/V19__Supplier_Receipt_Payments.sql",
            "migrations/V20__Lab_Specimen_Workflow_And_Seed.sql",
            "migrations/V21__Lab_Results_Detail_Seed.sql",
            "migrations/V22__Add_ChiTietHoaDon_Payer_Split_And_PhieuHoanTra.sql",
            "migrations/V23__Service_Configuration.sql",
            "migrations/V24__Cabinet_Transfer_Lifecycle.sql",
            "migrations/V25__Cabinet_Par_Configuration.sql",
            "migrations/V26__Cabinet_Department_Scope.sql",
            "migrations/V27__Inventory_Operation_Payload_Fingerprint.sql",
            "migrations/V28__Service_Consumption_And_Demand_Model.sql",
            "migrations/V29__Service_Consumption_Permissions.sql",
            "migrations/V30__Add_Service_Hospital_And_Foreign_Prices.sql"
        ), List.of(migrationFiles));
    }

    private static String readScript() throws IOException {
        try (InputStream input = QuyenMaTranDiCuKichBanTest.class.getClassLoader()
                .getResourceAsStream(SCRIPT_PATH)) {
            if (input == null) throw new AssertionError("Missing migration script: " + SCRIPT_PATH);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
