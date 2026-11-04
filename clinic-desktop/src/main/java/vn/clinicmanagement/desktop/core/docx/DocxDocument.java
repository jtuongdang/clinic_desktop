package vn.clinicmanagement.desktop.core.docx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Bộ tạo tài liệu Word DOCX thuần Java (Zero-dependency WordprocessingML Generator).
 * Tuân thủ tiêu chuẩn ISO/IEC 29500 (OpenXML), mở tốt trên Microsoft Word, LibreOffice, WPS, Google Docs.
 */
public class DocxDocument {

    private final StringBuilder bodyXml = new StringBuilder();
    private String defaultFont = "Times New Roman";

    public DocxDocument() {}

    public DocxDocument(String defaultFont) {
        if (defaultFont != null && !defaultFont.isBlank()) {
            this.defaultFont = defaultFont;
        }
    }

    /**
     * Thêm tiêu đề lớn ở giữa trang (Title)
     */
    public DocxDocument addTitle(String text) {
        return addParagraph(text, true, false, "center", 32, 240, 120); // 16pt, space before/after
    }

    /**
     * Thêm tiêu đề mục cấp 1 (Heading 1)
     */
    public DocxDocument addHeading1(String text) {
        return addParagraph(text, true, false, "left", 26, 180, 80); // 13pt bold
    }

    /**
     * Thêm đoạn văn bản chuẩn (13pt Times New Roman)
     */
    public DocxDocument addParagraph(String text) {
        return addParagraph(text, false, false, "left", 26, 0, 80);
    }

    /**
     * Thêm đoạn văn bản căn lề và định dạng tùy chỉnh
     */
    public DocxDocument addParagraph(String text, boolean bold, boolean italic, String align, int sizeHalfPt) {
        return addParagraph(text, bold, italic, align, sizeHalfPt, 0, 60);
    }

    /**
     * Thêm đoạn văn bản đầy đủ tham số
     */
    public DocxDocument addParagraph(String text, boolean bold, boolean italic, String align, int sizeHalfPt, int spaceBefore, int spaceAfter) {
        bodyXml.append("<w:p>");
        bodyXml.append("<w:pPr>");
        if (align != null && !align.isBlank()) {
            bodyXml.append("<w:jc w:val=\"").append(escapeXml(align)).append("\"/>");
        }
        if (spaceBefore > 0 || spaceAfter > 0) {
            bodyXml.append("<w:spacing w:before=\"").append(spaceBefore).append("\" w:after=\"").append(spaceAfter).append("\"/>");
        }
        bodyXml.append("</w:pPr>");

        bodyXml.append("<w:r>");
        bodyXml.append("<w:rPr>");
        bodyXml.append("<w:rFonts w:ascii=\"").append(escapeXml(defaultFont)).append("\" w:hAnsi=\"").append(escapeXml(defaultFont)).append("\"/>");
        if (bold) bodyXml.append("<w:b/>");
        if (italic) bodyXml.append("<w:i/>");
        if (sizeHalfPt > 0) bodyXml.append("<w:sz w:val=\"").append(sizeHalfPt).append("\"/>");
        bodyXml.append("</w:rPr>");
        bodyXml.append("<w:t xml:space=\"preserve\">").append(escapeXml(text)).append("</w:t>");
        bodyXml.append("</w:r>");
        bodyXml.append("</w:p>");
        return this;
    }

    /**
     * Thêm một dòng Thông tin theo dạng: [Nhãn đậm]: [Giá trị thường]
     */
    public DocxDocument addKeyValueParagraph(String label, String value) {
        bodyXml.append("<w:p>");
        bodyXml.append("<w:pPr><w:spacing w:after=\"60\"/></w:pPr>");
        
        // Label Run (Bold)
        bodyXml.append("<w:r><w:rPr>");
        bodyXml.append("<w:rFonts w:ascii=\"").append(escapeXml(defaultFont)).append("\" w:hAnsi=\"").append(escapeXml(defaultFont)).append("\"/>");
        bodyXml.append("<w:b/><w:sz w:val=\"26\"/>");
        bodyXml.append("</w:rPr><w:t xml:space=\"preserve\">").append(escapeXml(label)).append(": </w:t></w:r>");

        // Value Run (Normal)
        bodyXml.append("<w:r><w:rPr>");
        bodyXml.append("<w:rFonts w:ascii=\"").append(escapeXml(defaultFont)).append("\" w:hAnsi=\"").append(escapeXml(defaultFont)).append("\"/>");
        bodyXml.append("<w:sz w:val=\"26\"/>");
        bodyXml.append("</w:rPr><w:t xml:space=\"preserve\">").append(escapeXml(value != null ? value : "")).append("</w:t></w:r>");

        bodyXml.append("</w:p>");
        return this;
    }

    /**
     * Thêm bảng dữ liệu biểu mẫu
     */
    public DocxDocument addTable(DocxTable table) {
        if (table == null || table.getRows().isEmpty()) return this;

        bodyXml.append("<w:tbl>");
        // Table properties
        bodyXml.append("<w:tblPr>");
        bodyXml.append("<w:tblW w:w=\"5000\" w:type=\"pct\"/>");
        bodyXml.append("<w:tblBorders>");
        bodyXml.append("<w:top w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CCCCCC\"/>");
        bodyXml.append("<w:left w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CCCCCC\"/>");
        bodyXml.append("<w:bottom w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CCCCCC\"/>");
        bodyXml.append("<w:right w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CCCCCC\"/>");
        bodyXml.append("<w:insideH w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"EEEEEE\"/>");
        bodyXml.append("<w:insideV w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"EEEEEE\"/>");
        bodyXml.append("</w:tblBorders>");
        bodyXml.append("</w:tblPr>");

        List<List<String>> rows = table.getRows();
        List<Integer> colWidths = table.getColumnWidthsPercent();
        int numCols = rows.get(0).size();

        for (int r = 0; r < rows.size(); r++) {
            boolean isHeader = (r == 0 && table.isHasHeader());
            List<String> rowData = rows.get(r);

            bodyXml.append("<w:tr>");
            for (int c = 0; c < rowData.size(); c++) {
                int pct = (colWidths.size() > c) ? (colWidths.get(c) * 50) : (5000 / Math.max(1, numCols));
                String cellText = rowData.get(c);

                bodyXml.append("<w:tc>");
                bodyXml.append("<w:tcPr>");
                bodyXml.append("<w:tcW w:w=\"").append(pct).append("\" w:type=\"pct\"/>");
                if (isHeader) {
                    bodyXml.append("<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"F2F4F7\"/>");
                }
                bodyXml.append("</w:tcPr>");

                // Paragraph inside cell
                bodyXml.append("<w:p>");
                bodyXml.append("<w:pPr>");
                if (isHeader) {
                    bodyXml.append("<w:jc w:val=\"center\"/>");
                }
                bodyXml.append("<w:spacing w:after=\"40\" w:before=\"40\"/>");
                bodyXml.append("</w:pPr>");
                bodyXml.append("<w:r><w:rPr>");
                bodyXml.append("<w:rFonts w:ascii=\"").append(escapeXml(defaultFont)).append("\" w:hAnsi=\"").append(escapeXml(defaultFont)).append("\"/>");
                if (isHeader) {
                    bodyXml.append("<w:b/>");
                }
                bodyXml.append("<w:sz w:val=\"24\"/>"); // 12pt
                bodyXml.append("</w:rPr><w:t xml:space=\"preserve\">").append(escapeXml(cellText)).append("</w:t></w:r>");
                bodyXml.append("</w:p>");

                bodyXml.append("</w:tc>");
            }
            bodyXml.append("</w:tr>");
        }

        bodyXml.append("</w:tbl>");
        return this;
    }

    /**
     * Thêm đường phân cách trang
     */
    public DocxDocument addPageBreak() {
        bodyXml.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>");
        return this;
    }

    /**
     * Xuất toàn bộ file ra mảng byte (OpenXML ZIP container)
     */
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            // 1. [Content_Types].xml
            writeZipEntry(zos, "[Content_Types].xml", getContentTypesXml());

            // 2. _rels/.rels
            writeZipEntry(zos, "_rels/.rels", getRelsXml());

            // 3. word/_rels/document.xml.rels
            writeZipEntry(zos, "word/_rels/document.xml.rels", getDocumentRelsXml());

            // 4. word/styles.xml
            writeZipEntry(zos, "word/styles.xml", getStylesXml());

            // 5. word/document.xml
            writeZipEntry(zos, "word/document.xml", getDocumentXml());
        }
        return baos.toByteArray();
    }

    /**
     * Lưu tài liệu ra File vật lý
     */
    public void saveToFile(File file) throws IOException {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(toByteArray());
        }
    }

    private void writeZipEntry(ZipOutputStream zos, String entryName, String content) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        zos.write(bytes);
        zos.closeEntry();
    }

    private String getDocumentXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n"
                + "  <w:body>\n"
                + bodyXml
                + "    <w:sectPr>\n"
                + "      <w:pgSz w:w=\"11906\" w:h=\"16838\"/>\n" // A4: 210mm x 297mm
                + "      <w:pgMar w:top=\"1134\" w:right=\"1134\" w:bottom=\"1134\" w:left=\"1134\"/>\n" // 20mm margins
                + "    </w:sectPr>\n"
                + "  </w:body>\n"
                + "</w:document>";
    }

    private String getContentTypesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n"
                + "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n"
                + "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n"
                + "  <Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>\n"
                + "  <Override PartName=\"/word/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml\"/>\n"
                + "</Types>";
    }

    private String getRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n"
                + "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>\n"
                + "</Relationships>";
    }

    private String getDocumentRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n"
                + "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n"
                + "</Relationships>";
    }

    private String getStylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n"
                + "  <w:docDefaults>\n"
                + "    <w:rPrDefault>\n"
                + "      <w:rPr>\n"
                + "        <w:rFonts w:ascii=\"" + escapeXml(defaultFont) + "\" w:hAnsi=\"" + escapeXml(defaultFont) + "\"/>\n"
                + "        <w:sz w:val=\"26\"/>\n"
                + "      </w:rPr>\n"
                + "    </w:rPrDefault>\n"
                + "  </w:docDefaults>\n"
                + "</w:styles>";
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
