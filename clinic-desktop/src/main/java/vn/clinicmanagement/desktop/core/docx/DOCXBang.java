package vn.clinicmanagement.desktop.core.docx;

import java.util.ArrayList;
import java.util.List;

/**
 * Biểu diễn bảng trong tài liệu DOCX (WordprocessingML Table).
 */
public class DOCXBang {
    /** Trường `rows`: đóng. */
    private final List<List<String>> rows = new ArrayList<>();
    private final List<Integer> columnWidthsPercent = new ArrayList<>();
    /** Trường `hasHeader`: có `Header`. */
    private boolean hasHeader = true;

    public DOCXBang() {}

    public DOCXBang(boolean hasHeader) {
        this.hasHeader = hasHeader;
    }

    /** Hàm `setColumnWidthsPercent`: gán `Column` `Widths` `Percent`. */
    public void setColumnWidthsPercent(int... widths) {
        columnWidthsPercent.clear();
        for (int w : widths) {
            columnWidthsPercent.add(w);
        }
    }

    /** Hàm `addRow`: thêm `Row`. */
    public void addRow(String... cells) {
        List<String> row = new ArrayList<>();
        for (String c : cells) {
            row.add(c != null ? c : "");
        }
        rows.add(row);
    }

    /** Hàm `getRows`: lấy đóng. */
    public List<List<String>> getRows() {
        return rows;
    }

    /** Hàm `getColumnWidthsPercent`: lấy `Column` `Widths` `Percent`. */
    public List<Integer> getColumnWidthsPercent() {
        return columnWidthsPercent;
    }

    /** Hàm `isHasHeader`: là có `Header`. */
    public boolean isHasHeader() {
        return hasHeader;
    }

    /** Hàm `setHasHeader`: gán có `Header`. */
    public void setHasHeader(boolean hasHeader) {
        this.hasHeader = hasHeader;
    }
}
