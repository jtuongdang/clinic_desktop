package vn.clinicmanagement.desktop.core.docx;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxDocumentTest {

    @Test
    void toByteArrayProducesZipPackageWithRequiredOpenXmlEntries() throws IOException {
        DocxDocument doc = new DocxDocument();
        doc.addTitle("Tiêu đề thử nghiệm");
        doc.addParagraph("Nội dung đoạn văn mẫu.");

        byte[] zipBytes = doc.toByteArray();
        assertNotNull(zipBytes);
        assertTrue(zipBytes.length > 0);

        Set<String> entryNames = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                zis.closeEntry();
            }
        }

        assertTrue(entryNames.contains("[Content_Types].xml"), "Must contain [Content_Types].xml");
        assertTrue(entryNames.contains("_rels/.rels"), "Must contain _rels/.rels");
        assertTrue(entryNames.contains("word/_rels/document.xml.rels"), "Must contain word/_rels/document.xml.rels");
        assertTrue(entryNames.contains("word/styles.xml"), "Must contain word/styles.xml");
        assertTrue(entryNames.contains("word/document.xml"), "Must contain word/document.xml");
    }

    @Test
    void documentXmlEscapesXmlSpecialCharactersInText() throws IOException {
        DocxDocument doc = new DocxDocument();
        String specialInput = "Bệnh nhân & Bác sĩ <chẩn đoán> \"đặc biệt\" 'lưu ý'";
        doc.addParagraph(specialInput);

        byte[] zipBytes = doc.toByteArray();
        assertNotNull(zipBytes);

        String documentXml = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    documentXml = out.toString(StandardCharsets.UTF_8);
                    zis.closeEntry();
                    break;
                }
                zis.closeEntry();
            }
        }

        assertNotNull(documentXml, "word/document.xml must exist in the zip package");
        assertTrue(documentXml.contains("&amp;"), "Ampersand '&' must be escaped to '&amp;'");
        assertTrue(documentXml.contains("&lt;chẩn đoán&gt;"), "Tags '<' and '>' must be escaped to '&lt;' and '&gt;'");
        assertTrue(documentXml.contains("&quot;đặc biệt&quot;"), "Double quotes '\"' must be escaped to '&quot;'");
        assertTrue(documentXml.contains("&apos;lưu ý&apos;"), "Single quotes '\'' must be escaped to '&apos;'");

        assertFalse(documentXml.contains("<chẩn đoán>"), "Raw unescaped '<chẩn đoán>' must not be present");
        assertFalse(documentXml.contains("& Bác sĩ"), "Raw unescaped '&' must not be present");
        assertFalse(documentXml.contains("\"đặc biệt\""), "Raw unescaped double quotes must not be present");
        assertFalse(documentXml.contains("'lưu ý'"), "Raw unescaped single quotes must not be present");
    }
}
