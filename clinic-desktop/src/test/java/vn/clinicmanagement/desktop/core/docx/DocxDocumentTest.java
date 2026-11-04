package vn.clinicmanagement.desktop.core.docx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class DocxDocumentTest {

    @Test
    void toByteArrayProducesZipPackageWithRequiredOpenXmlEntries() throws IOException {
        byte[] documentBytes = new DocxDocument()
                .addTitle("Synthetic sample document")
                .addParagraph("This content is generated for a package test.")
                .toByteArray();

        Map<String, String> entries = readZipEntries(documentBytes);

        assertTrue(entries.keySet().containsAll(Set.of(
                "[Content_Types].xml",
                "_rels/.rels",
                "word/_rels/document.xml.rels",
                "word/styles.xml",
                "word/document.xml")));
        assertTrue(entries.get("word/document.xml").contains("Synthetic sample document"));
        assertTrue(entries.get("word/document.xml").contains("This content is generated for a package test."));
    }

    @Test
    void documentXmlEscapesXmlSpecialCharactersInText() throws IOException {
        String sampleText = "Synthetic & <sample> with \"quotes\" and 'apostrophes'.";
        String expectedEscapedText = "Synthetic &amp; &lt;sample&gt; with &quot;quotes&quot; and &apos;apostrophes&apos;.";

        Map<String, String> entries = readZipEntries(
                new DocxDocument().addParagraph(sampleText).toByteArray());
        String documentXml = entries.get("word/document.xml");

        assertNotNull(documentXml);
        assertTrue(documentXml.contains(expectedEscapedText));
        assertEquals(-1, documentXml.indexOf("<sample>"));
    }

    private Map<String, String> readZipEntries(byte[] documentBytes) throws IOException {
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(documentBytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8));
                zipInputStream.closeEntry();
            }
        }
        return entries;
    }
}
