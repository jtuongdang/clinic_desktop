package vn.clinicmanagement.desktop.core.docx;

import java.util.ArrayList;
import java.util.List;

/**
 * Biểu diễn bảng trong tài liệu DOCX (WordprocessingML Table).
 */
public class DocxTable {
    private final List<List<String>> rows = new ArrayList<>();
    private final List<Integer> columnWidthsPercent = new ArrayList<>();
    private boolean hasHeader = true;

    public DocxTable() {}

    public DocxTable(boolean hasHeader) {
        this.hasHeader = hasHeader;
    }

    public void setColumnWidthsPercent(int... widths) {
        columnWidthsPercent.clear();
        for (int w : widths) {
            columnWidthsPercent.add(w);
        }
    }

    public void addRow(String... cells) {
        List<String> row = new ArrayList<>();
        for (String c : cells) {
            row.add(c != null ? c : "");
        }
        rows.add(row);
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public List<Integer> getColumnWidthsPercent() {
        return columnWidthsPercent;
    }

    public boolean isHasHeader() {
        return hasHeader;
    }

    public void setHasHeader(boolean hasHeader) {
        this.hasHeader = hasHeader;
    }
}
