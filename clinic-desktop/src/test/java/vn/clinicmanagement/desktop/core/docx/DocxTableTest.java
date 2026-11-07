package vn.clinicmanagement.desktop.core.docx;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxTableTest {
    @Test
    void constructorsAndHeaderAccessorUseExpectedDefaultsAndValues() {
        DocxTable defaultTable = new DocxTable();
        assertTrue(defaultTable.isHasHeader());

        DocxTable headerlessTable = new DocxTable(false);
        assertFalse(headerlessTable.isHasHeader());
        headerlessTable.setHasHeader(true);
        assertTrue(headerlessTable.isHasHeader());
    }

    @Test
    void rowAccessorReturnsRowsAndNullCellsAreNormalized() {
        DocxTable table = new DocxTable();
        table.addRow("Header", null, "Value");

        assertEquals(List.of(List.of("Header", "", "Value")), table.getRows());
    }

    @Test
    void emptyRowsAndWidthsAreSupported() {
        DocxTable table = new DocxTable();
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
        DocxTable table = new DocxTable();
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
