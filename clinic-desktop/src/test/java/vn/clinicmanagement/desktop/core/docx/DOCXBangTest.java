package vn.clinicmanagement.desktop.core.docx;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DOCXBangTest {
    @Test
    void constructorsAndHeaderAccessorUseExpectedDefaultsAndValues() {
        DOCXBang defaultTable = new DOCXBang();
        assertTrue(defaultTable.isHasHeader());

        DOCXBang headerlessTable = new DOCXBang(false);
        assertFalse(headerlessTable.isHasHeader());
        headerlessTable.setHasHeader(true);
        assertTrue(headerlessTable.isHasHeader());
    }

    @Test
    void rowAccessorReturnsRowsAndNullCellsAreNormalized() {
        DOCXBang table = new DOCXBang();
        table.addRow("Header", null, "Value");

        assertEquals(List.of(List.of("Header", "", "Value")), table.getRows());
    }

    @Test
    void emptyRowsAndWidthsAreSupported() {
        DOCXBang table = new DOCXBang();
        assertTrue(table.getRows().isEmpty());
        assertTrue(table.getColumnWidthsPercent().isEmpty());

        table.addRow();
        assertEquals(List.of(List.of()), table.getRows());

        table.setColumnWidthsPercent(30);
        table.setColumnWidthsPercent();
        assertTrue(table.getColumnWidthsPercent().isEmpty());
    }

    @Test
    void rowCellsAndColumnWidthsDoNotRetainCallerArrays() {
        DOCXBang table = new DOCXBang();
        String[] cells = {"Original"};
        int[] widths = {40, 60};

        table.addRow(cells);
        table.setColumnWidthsPercent(widths);
        cells[0] = "Changed";
        widths[0] = 99;

        assertEquals(List.of(List.of("Original")), table.getRows());
        assertEquals(List.of(40, 60), table.getColumnWidthsPercent());
    }
}
