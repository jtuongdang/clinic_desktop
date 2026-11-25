package vn.clinicmanagement.desktop.modules.print.reception;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.zip.DeflaterOutputStream;

/** PDF trang ảnh 300 dpi: dùng đúng bộ vẽ của bản xem trước và giữ nguyên dấu tiếng Việt. */
public final class PhieuDangKyKhamPdf {
    private PhieuDangKyKhamPdf() { }
    public static void write(PhieuDangKyKhamTrang document, Path destination, int[] pages,
                             double paperWidth, double paperHeight) throws IOException {
        java.awt.print.Paper paper = new java.awt.print.Paper();
        paper.setSize(paperWidth, paperHeight); paper.setImageableArea(0, 0, paperWidth, paperHeight);
        java.awt.print.PageFormat format = new java.awt.print.PageFormat(); format.setPaper(paper);
        write(document, destination, pages, format);
    }
    public static void write(PhieuDangKyKhamTrang document, Path destination, int[] pages,
                             java.awt.print.PageFormat format) throws IOException {
        write(document, document.pageCount(), destination, pages, format);
    }
    public static void write(java.awt.print.Printable document, int pageCount, Path destination, int[] pages,
                             java.awt.print.PageFormat format) throws IOException {
        double paperWidth = format.getWidth(), paperHeight = format.getHeight();
        if (pages.length == 0) throw new IllegalArgumentException("Chưa chọn trang để xuất.");
        Path parent = destination.toAbsolutePath().getParent();
        Path temp = Files.createTempFile(parent, ".phieu-kham-", ".pdf");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            put(out, "%PDF-1.4\n%âãÏÓ\n");
            List<Integer> offsets = new ArrayList<>(); offsets.add(0);
            object(out, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
            StringBuilder kids = new StringBuilder();
            for (int i = 0; i < pages.length; i++) kids.append(3 + i * 3).append(" 0 R ");
            object(out, offsets, 2, "<< /Type /Pages /Count " + pages.length + " /Kids [" + kids + "] >>");
            for (int i = 0; i < pages.length; i++) {
                int id = 3 + i * 3;
                if (pages[i] < 0 || pages[i] >= pageCount) throw new IllegalArgumentException("Trang in không hợp lệ.");
                int rasterWidth = (int) Math.ceil(paperWidth / 72 * 300);
                BufferedImage image = new BufferedImage(rasterWidth, (int)Math.ceil(paperHeight / 72 * 300), BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D canvas = image.createGraphics();
                canvas.setColor(java.awt.Color.WHITE); canvas.fillRect(0, 0, image.getWidth(), image.getHeight());
                canvas.scale(300.0 / 72, 300.0 / 72);
                try { document.print(canvas, format, pages[i]); }
                catch (java.awt.print.PrinterException ex) { throw new IOException("Không thể vẽ trang PDF.", ex); }
                finally { canvas.dispose(); }
                object(out, offsets, id, String.format(Locale.ROOT,
                        "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.4f %.4f] /Resources << /XObject << /Im0 %d 0 R >> >> /Contents %d 0 R >>",
                        paperWidth, paperHeight, id + 1, id + 2));
                ByteArrayOutputStream compressed = new ByteArrayOutputStream();
                try (DeflaterOutputStream zip = new DeflaterOutputStream(compressed)) {
                    byte[] row = new byte[image.getWidth() * 3];
                    for (int y = 0; y < image.getHeight(); y++) {
                        for (int x = 0; x < image.getWidth(); x++) { int rgb = image.getRGB(x, y); row[x*3] = (byte)(rgb>>16); row[x*3+1]=(byte)(rgb>>8); row[x*3+2]=(byte)rgb; }
                        zip.write(row);
                    }
                }
                stream(out, offsets, id + 1, "/Type /XObject /Subtype /Image /Width " + image.getWidth() + " /Height " + image.getHeight() + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode", compressed.toByteArray());
                byte[] commands = String.format(Locale.ROOT, "q %.4f 0 0 %.4f %.4f %.4f cm /Im0 Do Q", paperWidth, paperHeight, 0.0, 0.0).getBytes(StandardCharsets.US_ASCII);
                stream(out, offsets, id + 2, "", commands);
            }
            int xref = out.size(); put(out, "xref\n0 " + offsets.size() + "\n0000000000 65535 f \n");
            for (int i = 1; i < offsets.size(); i++) put(out, String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i)));
            put(out, "trailer\n<< /Size " + offsets.size() + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
            Files.write(temp, out.toByteArray());
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
    private static void object(ByteArrayOutputStream out, List<Integer> offsets, int id, String body) throws IOException {
        offsets.add(out.size()); put(out, id + " 0 obj\n" + body + "\nendobj\n");
    }
    private static void stream(ByteArrayOutputStream out, List<Integer> offsets, int id, String dictionary, byte[] content) throws IOException {
        offsets.add(out.size()); put(out, id + " 0 obj\n<< " + dictionary + " /Length " + content.length + " >>\nstream\n");
        out.write(content); put(out, "\nendstream\nendobj\n");
    }
    private static void put(ByteArrayOutputStream out, String text) throws IOException { out.write(text.getBytes(StandardCharsets.ISO_8859_1)); }
}
