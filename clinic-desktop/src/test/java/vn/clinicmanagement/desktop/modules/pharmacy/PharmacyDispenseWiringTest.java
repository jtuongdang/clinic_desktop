package vn.clinicmanagement.desktop.modules.pharmacy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.pharmacy.components.PharmacyCategoryTreePanel.CategoryFilter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("PharmacyDispensePanel Main Wiring Test (HD-INTEG-02)")
class PharmacyDispenseWiringTest {

    @Test
    @DisplayName("PharmacyDispensePanel initializes cleanly with default and injected services")
    void dispensePanelInitializesCleanly() {
        PharmacyDispensePanel defaultPanel = new PharmacyDispensePanel();
        assertNotNull(defaultPanel, "Default PharmacyDispensePanel must not be null");
        assertNotNull(defaultPanel.getServices(), "Services must be initialized via fallback");
        assertNotNull(defaultPanel.getSubToolbar(), "SubToolbar must not be null");
        assertNotNull(defaultPanel.getTreePanel(), "TreePanel must not be null");
        assertNotNull(defaultPanel.getTablePanel(), "TablePanel must not be null");

        PharmacyCompositionRoot.Services customServices = PharmacyCompositionRoot.createDefault();
        PharmacyDispensePanel customPanel = new PharmacyDispensePanel(customServices);
        assertSame(customServices, customPanel.getServices(), "Services must match injected instance");

        assertDoesNotThrow(customPanel::onShow);
        assertDoesNotThrow(customPanel::refreshData);
    }

    @Test
    @DisplayName("Category selection in TreePanel propagates filter to TablePanel")
    void categorySelectionPropagatesFilter() {
        PharmacyDispensePanel panel = new PharmacyDispensePanel();

        // Select Import row (index 1)
        panel.getTreePanel().selectRow(1);
        assertEquals(CategoryFilter.NHAP_KHO, panel.getTablePanel().getCurrentFilter().ticketType(),
                "TablePanel filter should update to NHAP_KHO");

        // Select Prescription row (index 3)
        panel.getTreePanel().selectRow(3);
        assertEquals(CategoryFilter.DON_THUOC, panel.getTablePanel().getCurrentFilter().ticketType(),
                "TablePanel filter should update to DON_THUOC");

        // Select All row (index 0)
        panel.getTreePanel().selectRow(0);
        assertEquals(CategoryFilter.ALL, panel.getTablePanel().getCurrentFilter().ticketType(),
                "TablePanel filter should update to ALL");
    }

    @Test
    @DisplayName("SubToolbar search filters ticket table and buttons trigger actions")
    void subToolbarSearchAndButtonsTriggerActions() {
        PharmacyDispensePanel panel = new PharmacyDispensePanel();
        var subToolbar = panel.getSubToolbar();
        var tablePanel = panel.getTablePanel();

        // Type ticket code into subToolbar and trigger search via action listener
        subToolbar.getTxtTicketCode().setText("PNK0001");
        assertDoesNotThrow(() -> subToolbar.getBtnSearch().doClick());
        assertEquals("PNK0001", tablePanel.getTxtSearch().getText());

        // Click List button -> resets search filter
        assertDoesNotThrow(() -> subToolbar.getBtnList().doClick());
        assertEquals("", tablePanel.getTxtSearch().getText());

        // Click Report button -> reloads data without throwing
        assertDoesNotThrow(() -> subToolbar.getBtnReport().doClick());
    }
}
