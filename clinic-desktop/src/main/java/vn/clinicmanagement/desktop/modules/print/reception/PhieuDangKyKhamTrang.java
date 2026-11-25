package vn.clinicmanagement.desktop.modules.print.reception;

import vn.clinicmanagement.application.reception.PhieuDangKyKhamDuLieu;
import java.awt.*;
import java.awt.print.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/** Một bộ vẽ theo mẫu 821×1166, dùng chung cho xem trước, máy in và PDF. */
public final class PhieuDangKyKhamTrang implements Printable {
    public static final int WIDTH = 821, HEIGHT = 1166;
    private final PhieuDangKyKhamDuLieu data;
    public PhieuDangKyKhamTrang(PhieuDangKyKhamDuLieu data) { this.data = Objects.requireNonNull(data); }
    public PhieuDangKyKhamDuLieu data() { return data; }
    public int pageCount() { return Math.max(1, (data.services().size() + 5) / 6); }
    private static String safe(String value) { return value == null ? "" : value; }
    private static Font font(int style, int size) { return new Font("Serif", style, size); }
    private static void fit(Graphics2D g, String text, int x, int y, int width, int style, int size, boolean center) {
        text = safe(text); Font f = font(style, size); int actual = g.getFontMetrics(f).stringWidth(text);
        if (actual > width) f = f.deriveFont(java.awt.geom.AffineTransform.getScaleInstance(width / (double) actual, 1));
        g.setFont(f); g.drawString(text, center ? x + (width - g.getFontMetrics().stringWidth(text)) / 2 : x, y);
    }
    private static List<String> wrap(Graphics2D g, String value, int width) {
        List<String> lines = new ArrayList<>(); String current = "";
        for (String word : safe(value).split("\\s+")) {
            if (!current.isEmpty() && g.getFontMetrics().stringWidth(current + " " + word) > width) { lines.add(current); current = word; }
            else current = current.isEmpty() ? word : current + " " + word;
        }
        if (!current.isEmpty()) lines.add(current);
        return lines;
    }
    public static String money(java.math.BigDecimal value) {
        if (value == null) return "Chưa chốt giá";
        return new DecimalFormat("#,##0.##", java.text.DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }
    public void paint(Graphics2D original, int page) {
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE); g.fillRect(0, 0, WIDTH, HEIGHT); g.setColor(Color.BLACK);
            fit(g, safe(data.facilityName()).toUpperCase(new Locale("vi", "VN")), 60, 150, 701, Font.BOLD, 30, true);
            fit(g, data.facilityAddress(), 60, 184, 701, Font.PLAIN, 28, true);
            fit(g, data.facilityPhone(), 60, 216, 701, Font.PLAIN, 28, true);
            fit(g, "Họ và tên:", 60, 284, 132, Font.PLAIN, 30, false);
            fit(g, safe(data.patientName()).toUpperCase(new Locale("vi", "VN")), 193, 284, 285, Font.BOLD, 30, false);
            fit(g, "NS: " + (data.birthDate() == null ? "" : data.birthDate().format(DateTimeFormatter.ofPattern("dd/MM/uuuu"))), 552, 284, 235, Font.BOLD, 29, false);
            fit(g, "Giới tính: " + safe(data.gender()), 552, 334, 235, Font.PLAIN, 30, false);
            g.setFont(font(Font.PLAIN, 30));
            List<String> addresses = wrap(g, "Địa chỉ: " + safe(data.address()), 480);
            while (addresses.size() > 3 && g.getFont().getSize() > 20) { g.setFont(font(Font.PLAIN, g.getFont().getSize() - 1)); addresses = wrap(g, "Địa chỉ: " + safe(data.address()), 480); }
            int y = 324;
            for (String line : addresses) { fit(g, line, 60, y, 480, Font.PLAIN, g.getFont().getSize(), false); y += 36; }
            int tableY = Math.max(395, y - 12), left = 60, right = 791, divide = 508;
            g.setColor(new Color(216, 216, 216)); g.fillRect(left, tableY, right - left, 39); g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.6f));
            fit(g, "Tên dịch vụ", left, tableY + 29, divide - left, Font.PLAIN, 30, true);
            fit(g, "Giá tiền", divide, tableY + 29, right - divide, Font.PLAIN, 30, true);
            g.drawLine(left, tableY, right, tableY); g.drawLine(left, tableY + 39, right, tableY + 39); g.drawLine(divide, tableY, divide, tableY + 39);
            y = tableY + 39;
            fit(g, "Phòng khám: " + safe(data.roomName()), left + 13, y + 30, 705, Font.BOLD, 30, false);
            y += 40; g.drawLine(left, y, right, y);
            int start = page * 6, end = Math.min(start + 6, data.services().size());
            for (int i = start; i < end; i++) {
                var line = data.services().get(i);
                int rowHeight = 40;
                fit(g, line.name(), left + 13, y + 29, 420, Font.PLAIN, 30, false);
                fit(g, money(line.unitPrice() == null ? null : line.unitPrice().multiply(line.quantity())), divide + 14, y + 29, 255, Font.PLAIN, 30, false);
                g.drawLine(divide, y, divide, y + rowHeight); y += rowHeight; g.drawLine(left, y, right, y);
            }
            fit(g, page == pageCount() - 1 ? "Tổng tiền" : "Còn tiếp…", left + 13, y + 29, 420, Font.PLAIN, 30, false);
            if (page == pageCount() - 1) fit(g, money(data.total()), divide + 14, y + 29, 255, Font.PLAIN, 30, false);
            y += 40; g.drawLine(left, y, right, y); g.drawLine(left, tableY, left, y); g.drawLine(right, tableY, right, y);
            if (page == pageCount() - 1) {
                fit(g, "Bằng chữ: " + SoTienBangChu.read(data.total()), left, y + 52, 731, Font.PLAIN, 30, false);
                int footer = Math.max(670, y + 120);
                if (data.collectedAt() != null) fit(g, data.collectedAt().format(DateTimeFormatter.ofPattern("HH:mm, 'ngày' dd 'tháng' MM 'năm' uuuu")), 260, footer, 490, Font.ITALIC, 28, true);
                fit(g, "Người thu", 380, footer + 36, 270, Font.PLAIN, 30, true);
                fit(g, data.collectorName(), 275, footer + 176, 440, Font.PLAIN, 30, true);
            }
        } finally { g.dispose(); }
    }
    public BufferedImage image(int page, int width) {
        BufferedImage image = new BufferedImage(width, (int)Math.round(width * HEIGHT / (double)WIDTH), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); g.scale(width / (double)WIDTH, width / (double)WIDTH); paint(g, page); g.dispose(); return image;
    }
    @Override public int print(Graphics graphics, PageFormat format, int page) {
        if (page < 0 || page >= pageCount()) return NO_SUCH_PAGE;
        Graphics2D g = (Graphics2D) graphics.create();
        // Đặt A5 thật vào giữa phần in được, tuyệt đối không kéo giãn nội dung theo A4.
        double scale = Math.min(148 / 25.4 * 72 / WIDTH, Math.min(format.getImageableWidth() / WIDTH, format.getImageableHeight() / HEIGHT));
        g.translate(format.getImageableX() + (format.getImageableWidth() - WIDTH * scale) / 2, format.getImageableY());
        g.scale(scale, scale); paint(g, page); g.dispose(); return PAGE_EXISTS;
    }
}
