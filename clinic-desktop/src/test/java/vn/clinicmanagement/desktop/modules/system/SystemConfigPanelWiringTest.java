package vn.clinicmanagement.desktop.modules.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.core.session.UserSession;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SystemConfigPanel Wiring & Integration Tests (HD-INTEG-04)")
class SystemConfigPanelWiringTest {

    @BeforeEach
    void setUp() {
        UserSession.getInstance().clearSession();
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().clearSession();
    }

    private void waitForWorkers(SystemConfigPanel panel) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(3);
        while (panel.getInFlightWorkersCount() > 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
    }

    @Test
    @DisplayName("Panel initializes cleanly in headless environment with default management items")
    void initializesCleanlyInHeadlessEnvironment() {
        SystemConfigPanel panel = new SystemConfigPanel();
        assertNotNull(panel, "Panel instance must not be null");
        assertNotNull(panel.getCategoryList(), "Category list must not be null");
        assertNotNull(panel.getTable(), "Table must not be null");
        assertNotNull(panel.getSearchField(), "Search field must not be null");

        assertEquals(1, panel.getCategoryList().getSelectedIndex(), "Default selected category should be Quản Lý Hệ Thống");
        assertEquals(26, panel.getTable().getRowCount(), "Should contain 26 default management items");
    }

    @Test
    @DisplayName("Loads system parameters asynchronously from repository into table")
    void loadsSystemParametersFromRepository() throws InterruptedException {
        SystemConfigPanel.InMemorySystemRepository repo = new SystemConfigPanel.InMemorySystemRepository();
        SystemConfigPanel panel = new SystemConfigPanel(repo);

        // Switch to Cấu Hình Hệ Thống (Index 0)
        panel.getCategoryList().setSelectedIndex(0);
        waitForWorkers(panel);

        DefaultTableModel model = panel.getTableModel();
        assertNotNull(model);
        assertEquals(4, model.getColumnCount(), "Must have 4 columns");
        assertEquals("Mã Tham Số", model.getColumnName(0));
        assertEquals("Tên Tham Số", model.getColumnName(1));
        assertEquals("Giá Trị Cấu Hình", model.getColumnName(2));
        assertEquals("Mô Tả", model.getColumnName(3));

        assertTrue(model.getRowCount() >= 5, "Should have loaded at least 5 default parameters");

        boolean foundClinicName = false;
        for (int r = 0; r < model.getRowCount(); r++) {
            if ("CLINIC_NAME".equals(model.getValueAt(r, 0))) {
                foundClinicName = true;
                break;
            }
        }
        assertTrue(foundClinicName, "CLINIC_NAME parameter must be present");
    }

    @Test
    @DisplayName("Updates and saves system parameter successfully to repository and UI")
    void updatesAndSavesSystemParameter() throws InterruptedException {
        SystemConfigPanel.InMemorySystemRepository repo = new SystemConfigPanel.InMemorySystemRepository();
        SystemConfigPanel panel = new SystemConfigPanel(repo);

        panel.getCategoryList().setSelectedIndex(0);
        waitForWorkers(panel);

        assertDoesNotThrow(() -> panel.saveParameter("MAX_QUEUE_PER_DAY", "250"));

        assertEquals("250", repo.getSystemParameterValue("MAX_QUEUE_PER_DAY").orElse(""));

        DefaultTableModel model = panel.getTableModel();
        boolean updatedInUi = false;
        for (int r = 0; r < model.getRowCount(); r++) {
            if ("MAX_QUEUE_PER_DAY".equals(model.getValueAt(r, 0))) {
                assertEquals("250", model.getValueAt(r, 2));
                updatedInUi = true;
                break;
            }
        }
        assertTrue(updatedInUi, "Parameter value must be updated in table UI");
    }

    @Test
    @DisplayName("Switches category to permission matrix view with correct columns and roles")
    void switchesCategoryToPermissionMatrix() throws InterruptedException {
        SystemConfigPanel.InMemorySystemRepository repo = new SystemConfigPanel.InMemorySystemRepository();
        SystemConfigPanel panel = new SystemConfigPanel(repo);

        // Switch to Công Cụ Quản Trị Hệ Thống (Index 2)
        panel.getCategoryList().setSelectedIndex(2);
        waitForWorkers(panel);

        DefaultTableModel model = panel.getTableModel();
        assertNotNull(model);
        assertEquals(4, model.getColumnCount(), "Must have 4 columns");
        assertEquals("Phân Hệ", model.getColumnName(0));
        assertEquals("Hành Động", model.getColumnName(1));
        assertEquals("Phạm Vi Dữ Liệu", model.getColumnName(2));
        assertEquals("Được Phép", model.getColumnName(3));

        assertTrue(model.getRowCount() >= 5, "Permission matrix must contain role permissions");
    }

    @Test
    @DisplayName("Filters table data realtime via search field")
    void filtersDataRealtimeWithSearchField() {
        SystemConfigPanel.InMemorySystemRepository repo = new SystemConfigPanel.InMemorySystemRepository();
        SystemConfigPanel panel = new SystemConfigPanel(repo);

        // Initially in category 1 with 26 rows
        panel.getCategoryList().setSelectedIndex(1);
        assertEquals(26, panel.getTable().getRowCount());

        // Search for "Thuốc"
        panel.getSearchField().setText("Thuốc");
        int filteredCount = panel.getTable().getRowCount();
        assertTrue(filteredCount > 0 && filteredCount < 26, "Should filter down to matching items");

        for (int i = 0; i < filteredCount; i++) {
            String value = panel.getTable().getValueAt(i, 0).toString();
            assertTrue(value.contains("Thuốc"), "Filtered row must match keyword: " + value);
        }

        // Clear search
        panel.getSearchField().setText("");
        assertEquals(26, panel.getTable().getRowCount(), "Should reset to all 26 rows");
    }
}
