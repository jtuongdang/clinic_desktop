package vn.clinicmanagement.desktop.modules.print.pharmacy;


import vn.clinicmanagement.desktop.modules.pharmacy.components.*;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.reception.components.TiepDonBieuTuong;
import vn.clinicmanagement.desktop.modules.print.reception.PhieuInBieuTuong;

import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

/**
 * Cửa sổ xem trước & in Phiếu Nhập Nhà Cung Cấp (mẫu EHC TH_001_A4_PhieuNhapNhaCungCap).
 * Hỗ trợ in trực tiếp qua PrinterJob, xuất PDF 300 dpi (giữ nguyên dấu tiếng Việt) và xuất file Word (.doc).
 */
public final class PhieuNhapNhaCungCapInManHinh extends JPanel {

    public record LineData(
            String name,
            String unit,
            String origin,
            String batch,
            LocalDate expiry,
            BigDecimal quantity,
            BigDecimal unitPriceWithVat,
            BigDecimal amount) {}

    public record ReceiptPrintData(
            String warehouseName,
            String receiptCode,
            LocalDate docDate,
            String supplierName,
            String invoiceNumber,
            LocalDate invoiceDate,
            String creatorName,
            List<LineData> lines,
            BigDecimal totalAmount) {
        public ReceiptPrintData {
            lines = lines != null ? List.copyOf(lines) : List.of();
            totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        }
    }

    private static final DateTimeFormatter DATE_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_DASH = DateTimeFormatter.ofPattern("dd - MM - yyyy");
    private static final DecimalFormat MONEY_FMT;
    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator(',');
        sym.setDecimalSeparator('.');
        MONEY_FMT = new DecimalFormat("#,##0", sym);
    }

    private final ReceiptPrintData data;
    private final String documentTitle;
    private final String partnerLabel;
    private final ReceiptPrintable printable;
    private final JComboBox<PrintService> printers = new JComboBox<>();
    private final JComboBox<MediaTray> trays = new JComboBox<>();
    private final JSpinner copies = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JCheckBox autoPrint = new JCheckBox("In tự động");
    private final JCheckBox keepOpen = new JCheckBox("In tiếp");
    private final JCheckBox duplex = new JCheckBox("In 2 mặt");
    private final JCheckBox a5onA4 = new JCheckBox("In A5 vào khay giấy A4");
    private final JComboBox<String> scope = new JComboBox<>(new String[]{"In tất cả các trang", "Trang hiện tại", "Các trang chỉ định"});
    private final JTextField pageRange = new JTextField();
    private final JComboBox<String> orientation = new JComboBox<>(new String[]{"Chiều dọc", "Chiều ngang"});
    private final JComboBox<String> paper = new JComboBox<>(new String[]{"A4 (210mm x 297mm)", "A5 (148mm x 210mm)"});
    private final JSpinner page;
    private final JSlider zoom = new JSlider(40, 200, 100);
    private final JLabel zoomLabel = new JLabel("100%");
    private final PreviewPanel preview = new PreviewPanel();
    private final JButton btnPrint = new JButton("In", new PhieuInBieuTuong(false));
    private final JButton btnPdf = new JButton("Pdf", new PhieuInBieuTuong(true));
    private final JButton btnWord = new JButton("Word");
    private PageFormat configuredFormat;
    private boolean busy;
    private boolean closed;
    private static boolean autoPrintPref;
    private static boolean keepOpenPref;

    public PhieuNhapNhaCungCapInManHinh(ReceiptPrintData data) {
        this(data, "PHIẾU NHẬP", "Nhà cung cấp: ");
    }

    public PhieuNhapNhaCungCapInManHinh(ReceiptPrintData data, String documentTitle, String partnerLabel) {
        this.data = Objects.requireNonNull(data, "data");
        this.documentTitle = documentTitle != null && !documentTitle.isBlank() ? documentTitle : "PHIẾU NHẬP";
        this.partnerLabel = partnerLabel != null && !partnerLabel.isBlank() ? partnerLabel : "Nhà cung cấp: ";
        this.printable = new ReceiptPrintable(data, this.documentTitle, this.partnerLabel);
        this.page = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));

        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 240));
        add(buildSidebar(), BorderLayout.WEST);

        JScrollPane scroll = new JScrollPane(preview);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(22);
        add(scroll, BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);

        btnPrint.addActionListener(e -> printDocument());
        btnPdf.addActionListener(e -> exportPdf());
        btnWord.addActionListener(e -> exportWord());
        zoom.addChangeListener(e -> updatePreview());
        paper.addActionListener(e -> { configuredFormat = null; updatePreview(); });
        orientation.addActionListener(e -> { configuredFormat = null; updatePreview(); });
        a5onA4.addActionListener(e -> { configuredFormat = null; updatePreview(); });
        scope.addActionListener(e -> pageRange.setEnabled(scope.getSelectedIndex() == 2));
        pageRange.setEnabled(false);
        printers.addActionListener(e -> updatePrinterOptions());
        autoPrint.setSelected(autoPrintPref);
        keepOpen.setSelected(keepOpenPref);
        autoPrint.addActionListener(e -> autoPrintPref = autoPrint.isSelected());
        keepOpen.addActionListener(e -> keepOpenPref = keepOpen.isSelected());

        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control P"), "print");
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("meta P"), "print");
        getActionMap().put("print", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { printDocument(); }
        });

        loadPrinters();
        updatePreview();
    }

    public static void showPreviewDialog(Component parent, ReceiptPrintData data) {
        showPreviewDialog(parent, data, "PHIẾU NHẬP", "Nhà cung cấp: ", "EHC - In - TH_001_A4_PhieuNhapNhaCungCap.doc");
    }

    public static void showPreviewDialog(Component parent, ReceiptPrintData data, String documentTitle, String partnerLabel, String dialogTitle) {
        if (GraphicsEnvironment.isHeadless() || data == null) return;
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        PhieuNhapNhaCungCapInManHinh panel = new PhieuNhapNhaCungCapInManHinh(data, documentTitle, partnerLabel);
        JDialog dialog = new JDialog(owner, dialogTitle != null && !dialogTitle.isBlank() ? dialogTitle : "EHC - In Phiếu", Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(panel);
        dialog.setSize(1120, 740);
        dialog.setMinimumSize(new Dimension(900, 600));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(null);
        side.setPreferredSize(new Dimension(222, 680));
        side.setBackground(new Color(248, 248, 248));
        side.setBorder(new MatteBorder(0, 0, 0, 1, Color.GRAY));

        btnPrint.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnPrint.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPrint.setFont(UngDungPhongChu.PHIEU_IN_NUT);
        btnPrint.setBorder(new LineBorder(new Color(95, 85, 185)));
        place(side, btnPrint, 6, 7, 110, 86);

        place(side, new JLabel("Bản in"), 122, 9, 44, 22);
        place(side, copies, 168, 7, 46, 24);
        place(side, autoPrint, 122, 42, 96, 24);
        place(side, keepOpen, 122, 68, 96, 24);

        addLabel(side, "Máy In", 6, 104, true);
        place(side, printers, 6, 128, 208, 24);
        place(side, trays, 6, 155, 208, 23);

        JButton settings = new JButton("<html><u>Cấu hình máy in</u></html>");
        settings.setForeground(new Color(55, 50, 180));
        settings.setBorderPainted(false);
        settings.setContentAreaFilled(false);
        settings.setMargin(new Insets(0, 0, 0, 0));
        settings.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settings.addActionListener(e -> printerSettings());
        place(side, settings, 100, 182, 114, 22);

        addLabel(side, "Tùy Chọn", 6, 214, true);
        place(side, scope, 6, 238, 208, 24);
        addLabel(side, "Trang in :", 6, 268, false);
        place(side, pageRange, 78, 265, 136, 24);
        place(side, duplex, 78, 294, 136, 23);

        addLabel(side, "Chiều giấy", 6, 322, false);
        place(side, orientation, 6, 345, 208, 24);
        addLabel(side, "Khổ giấy", 6, 376, false);
        place(side, paper, 6, 399, 208, 24);
        place(side, a5onA4, 6, 428, 210, 23);

        addLabel(side, "Xuất File...", 6, 462, true);
        btnPdf.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnPdf.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPdf.setFont(UngDungPhongChu.PHIEU_IN_NUT);
        btnPdf.setBorder(new LineBorder(new Color(95, 85, 185)));
        place(side, btnPdf, 6, 490, 98, 66);

        btnWord.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnWord.setHorizontalTextPosition(SwingConstants.CENTER);
        btnWord.setFont(UngDungPhongChu.PHIEU_IN_NUT);
        btnWord.setBorder(new LineBorder(new Color(95, 85, 185)));
        btnWord.setIcon(new WordDocIcon());
        place(side, btnWord, 114, 490, 98, 66);

        for (Component c : side.getComponents()) {
            if (c != btnPrint && c != btnPdf && c != btnWord) {
                c.setFont(c instanceof JLabel && c.getFont().isBold() ? UngDungPhongChu.BODY_BOLD : UngDungPhongChu.BODY);
            }
            if (c instanceof JComponent jc) {
                jc.setOpaque(!(c instanceof JCheckBox) && jc.isOpaque());
            }
        }
        settings.setFont(UngDungPhongChu.SMALL);
        return side;
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setPreferredSize(new Dimension(1000, 40));
        bar.setBorder(new MatteBorder(1, 0, 0, 0, Color.GRAY));

        JPanel menu = new JPanel(new FlowLayout(FlowLayout.LEFT));
        menu.setPreferredSize(new Dimension(222, 40));
        JButton closeBtn = new JButton(TiepDonBieuTuong.createMenuIcon());
        closeBtn.setToolTipText("Đóng cửa sổ xem trước");
        closeBtn.addActionListener(e -> close());
        menu.add(closeBtn);
        bar.add(menu, BorderLayout.WEST);

        JPanel pages = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JButton prev = new JButton("◀");
        JButton next = new JButton("▶");
        prev.setEnabled(false);
        next.setEnabled(false);
        pages.add(prev);
        pages.add(page);
        pages.add(new JLabel("/ 1"));
        pages.add(next);
        bar.add(pages, BorderLayout.CENTER);

        JPanel scale = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton minus = new JButton("−");
        JButton plus = new JButton("+");
        minus.addActionListener(e -> zoom.setValue(zoom.getValue() - 10));
        plus.addActionListener(e -> zoom.setValue(zoom.getValue() + 10));
        zoom.setPreferredSize(new Dimension(220, 24));
        scale.add(zoomLabel);
        scale.add(minus);
        scale.add(zoom);
        scale.add(plus);
        bar.add(scale, BorderLayout.EAST);
        return bar;
    }

    private static void place(JPanel p, Component c, int x, int y, int w, int h) {
        c.setBounds(x, y, w, h);
        p.add(c);
    }

    private static void addLabel(JPanel p, String text, int x, int y, boolean bold) {
        JLabel l = new JLabel(text);
        l.setFont(bold ? UngDungPhongChu.BODY_BOLD : UngDungPhongChu.BODY);
        place(p, l, x, y, 206, 22);
    }

    private void loadPrinters() {
        btnPrint.setEnabled(false);
        new SwingWorker<PrintService[], Void>() {
            @Override protected PrintService[] doInBackground() {
                return PrintServiceLookup.lookupPrintServices(null, null);
            }

            @Override protected void done() {
                try {
                    for (PrintService s : get()) printers.addItem(s);
                    printers.setRenderer(new DefaultListCellRenderer() {
                        @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                                                                                int index, boolean selected, boolean focus) {
                            return super.getListCellRendererComponent(list,
                                    value instanceof PrintService s ? s.getName() : "Không có máy in", index, selected, focus);
                        }
                    });
                    PrintService def = PrintServiceLookup.lookupDefaultPrintService();
                    if (def != null) printers.setSelectedItem(def);
                    btnPrint.setEnabled(printers.getItemCount() > 0);
                    updatePrinterOptions();
                    if (autoPrint.isSelected() && isShowing() && !closed && printers.getItemCount() > 0) {
                        printDocument();
                    }
                } catch (Exception ignored) {
                    btnPrint.setEnabled(false);
                }
            }
        }.execute();
    }

    private void updatePrinterOptions() {
        PrintService svc = (PrintService) printers.getSelectedItem();
        trays.removeAllItems();
        trays.addItem(null);
        if (svc != null) {
            Object vals = svc.getSupportedAttributeValues(Media.class, null, null);
            if (vals instanceof Media[] media) {
                for (Media m : media) if (m instanceof MediaTray t) trays.addItem(t);
            }
        }
        trays.setEnabled(trays.getItemCount() > 1);
        duplex.setEnabled(svc != null && svc.isAttributeValueSupported(Sides.DUPLEX, null, null));
        if (!duplex.isEnabled()) duplex.setSelected(false);
    }

    private double[] dimensions() {
        if (configuredFormat != null) return new double[]{configuredFormat.getWidth(), configuredFormat.getHeight()};
        boolean a4 = paper.getSelectedIndex() == 0 || a5onA4.isSelected();
        double w = (a4 ? 210 : 148) / 25.4 * 72;
        double h = (a4 ? 297 : 210) / 25.4 * 72;
        return orientation.getSelectedIndex() == 0 ? new double[]{w, h} : new double[]{h, w};
    }

    private PageFormat pageFormat() {
        if (configuredFormat != null) return (PageFormat) configuredFormat.clone();
        double[] d = dimensions();
        Paper sheet = new Paper();
        sheet.setSize(d[0], d[1]);
        sheet.setImageableArea(0, 0, d[0], d[1]);
        PageFormat fmt = new PageFormat();
        fmt.setPaper(sheet);
        return fmt;
    }

    private void printerSettings() {
        PrinterJob job = PrinterJob.getPrinterJob();
        PrintService svc = (PrintService) printers.getSelectedItem();
        try {
            if (svc != null) job.setPrintService(svc);
        } catch (PrinterException ignored) {}
        PageFormat next = job.pageDialog(pageFormat());
        if (next != null) {
            configuredFormat = next;
            updatePreview();
        }
    }

    private void updatePreview() {
        zoomLabel.setText(zoom.getValue() + "%");
        double[] d = dimensions();
        double scale = 1.25 * zoom.getValue() / 100.0;
        preview.setPreferredSize(new Dimension(
                (int) Math.ceil(d[0] * scale) + 48,
                (int) Math.ceil(d[1] * scale) + 48));
        preview.revalidate();
        preview.repaint();
    }

    private void printDocument() {
        if (busy) return;
        PrintService svc = (PrintService) printers.getSelectedItem();
        if (svc == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy máy in khả dụng.", "In phiếu nhập", JOptionPane.WARNING_MESSAGE);
            return;
        }
        busy = true;
        btnPrint.setEnabled(false);
        PageFormat fmt = pageFormat();
        int copyCount = (int) copies.getValue();
        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        attrs.add(new Copies(copyCount));
        if (duplex.isSelected()) attrs.add(Sides.DUPLEX);
        if (trays.getSelectedItem() instanceof MediaTray tray) attrs.add(tray);

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPrintService(svc);
                job.setJobName("PhieuNhap_" + data.receiptCode());
                Book book = new Book();
                book.append(printable, fmt, 1);
                job.setPageable(book);
                job.print(attrs);
                return null;
            }

            @Override protected void done() {
                busy = false;
                btnPrint.setEnabled(printers.getItemCount() > 0);
                try {
                    get();
                    if (!keepOpen.isSelected()) close();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PhieuNhapNhaCungCapInManHinh.this,
                            "Không thể in phiếu nhập: " + ex.getMessage(), "Lỗi in", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void exportPdf() {
        if (busy) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("PhieuNhap_" + safeFileCode(data.receiptCode()) + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path target = chooser.getSelectedFile().toPath();
        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            target = target.resolveSibling(target.getFileName() + ".pdf");
        }
        Path dest = target;
        PageFormat fmt = pageFormat();
        busy = true;
        btnPdf.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                writePdf(printable, dest, fmt);
                return null;
            }

            @Override protected void done() {
                busy = false;
                btnPdf.setEnabled(true);
                try {
                    get();
                    JOptionPane.showMessageDialog(PhieuNhapNhaCungCapInManHinh.this,
                            "Đã xuất PDF thành công:\n" + dest.toAbsolutePath(),
                            "Xuất PDF", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PhieuNhapNhaCungCapInManHinh.this,
                            "Không thể xuất PDF: " + ex.getMessage(), "Lỗi xuất PDF", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void exportWord() {
        if (busy) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("TH_001_A4_PhieuNhap_" + safeFileCode(data.receiptCode()) + ".doc"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path target = chooser.getSelectedFile().toPath();
        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".doc")) {
            target = target.resolveSibling(target.getFileName() + ".doc");
        }
        try {
            Files.writeString(target, buildWordHtml(data, documentTitle, partnerLabel), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this,
                    "Đã xuất file Word thành công:\n" + target.toAbsolutePath(),
                    "Xuất Word", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Không thể xuất file Word: " + ex.getMessage(), "Lỗi xuất Word", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void close() {
        closed = true;
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) w.dispose();
    }

    private final class PreviewPanel extends JPanel {
        PreviewPanel() { setBackground(new Color(225, 225, 225)); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            double[] d = dimensions();
            double scale = 1.25 * zoom.getValue() / 100.0;
            int w = (int) Math.round(d[0] * scale);
            int h = (int) Math.round(d[1] * scale);
            int x = Math.max(20, (getWidth() - w) / 2);
            int y = 20;
            g2.setColor(new Color(170, 170, 170));
            g2.fillRect(x + 3, y + 3, w, h);
            g2.setColor(Color.WHITE);
            g2.fillRect(x, y, w, h);
            g2.setColor(Color.GRAY);
            g2.drawRect(x, y, w, h);
            g2.translate(x, y);
            g2.scale(scale, scale);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            try {
                printable.print(g2, pageFormat(), 0);
            } catch (PrinterException ignored) {}
            g2.dispose();
        }
    }

    /** Bộ vẽ trang Phiếu Nhập A4 chuẩn EHC TH_001_A4_PhieuNhapNhaCungCap. */
    static final class ReceiptPrintable implements Printable {
        private final ReceiptPrintData d;
        private final String documentTitle;
        private final String partnerLabel;

        ReceiptPrintable(ReceiptPrintData d) {
            this(d, "PHIẾU NHẬP", "Nhà cung cấp: ");
        }

        ReceiptPrintable(ReceiptPrintData d, String documentTitle, String partnerLabel) {
            this.d = d;
            this.documentTitle = documentTitle;
            this.partnerLabel = partnerLabel;
        }

        @Override
        public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {
            if (pageIndex > 0) return NO_SUCH_PAGE;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.BLACK);

            double w = pf.getWidth();
            double margin = 36;
            double contentW = w - margin * 2;
            int left = (int) margin;
            int right = (int) (w - margin);

            Font serif = new Font("Times New Roman", Font.PLAIN, 10);
            Font serifBold = serif.deriveFont(Font.BOLD);
            Font serifItalic = serif.deriveFont(Font.ITALIC);
            Font titleFont = new Font("Times New Roman", Font.BOLD, 16);
            Font headerFont = new Font("Times New Roman", Font.BOLD, 11);

            int y = 38;
            // Header row: Warehouse | Quốc hiệu | Số & Ngày lập
            g.setFont(serif);
            String wh = d.warehouseName() != null && !d.warehouseName().isBlank()
                    ? d.warehouseName().toUpperCase(Locale.ROOT) : "NHÀ THUỐC";
            drawCentered(g, wh, left + 75, y);

            g.setFont(headerFont);
            drawCentered(g, "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", (int) (w / 2), y);
            g.setFont(serif);
            drawCentered(g, "Độc lập – Tự do – Hạnh phúc", (int) (w / 2), y + 15);

            g.setFont(serifItalic);
            String soText = "Số: " + safe(d.receiptCode());
            String dateText = "Ngày lập: " + (d.docDate() != null ? DATE_SLASH.format(d.docDate()) : "");
            g.drawString(soText, right - g.getFontMetrics().stringWidth(soText), y);
            g.drawString(dateText, right - g.getFontMetrics().stringWidth(dateText), y + 15);

            // Title
            y += 40;
            g.setFont(titleFont);
            drawCentered(g, documentTitle, (int) (w / 2), y);

            // Supplier & Invoice meta
            y += 28;
            g.setFont(serifBold);
            g.drawString(partnerLabel, left, y);
            int supLabelW = g.getFontMetrics().stringWidth(partnerLabel);
            g.setFont(serif);
            g.drawString(safe(d.supplierName()), left + supLabelW, y);

            String invNo = safe(d.invoiceNumber());
            String invDate = d.invoiceDate() != null ? DATE_DASH.format(d.invoiceDate()) : "";
            int invBlockX = (int) (w * 0.56);
            g.setFont(serifBold);
            g.drawString("Số HĐ: ", invBlockX, y);
            int soHdW = g.getFontMetrics().stringWidth("Số HĐ: ");
            g.setFont(serif);
            g.drawString(invNo, invBlockX + soHdW, y);

            int invDateX = (int) (w * 0.72);
            g.setFont(serifBold);
            g.drawString("Ngày HĐ: ", invDateX, y);
            int ngHdW = g.getFontMetrics().stringWidth("Ngày HĐ: ");
            g.setFont(serif);
            g.drawString(invDate, invDateX + ngHdW, y);

            // Table
            y += 18;
            double[] ratios = {0.055, 0.255, 0.08, 0.095, 0.085, 0.095, 0.09, 0.115, 0.13};
            int[] colW = new int[ratios.length];
            int used = 0;
            for (int i = 0; i < ratios.length - 1; i++) {
                colW[i] = (int) Math.round(contentW * ratios[i]);
                used += colW[i];
            }
            colW[ratios.length - 1] = (int) contentW - used;

            String[] headers = {"STT", "Tên thuốc, vật tư", "Đơn vị", "Nơi sản\nxuất", "Số lô", "Hạn\ndùng", "Số\nlượng", "Đơn giá\n(đã VAT)", "Thành tiền"};
            int hdrH = 28;
            g.setFont(serifBold.deriveFont(9.5f));
            int cx = left;
            for (int i = 0; i < headers.length; i++) {
                g.drawRect(cx, y, colW[i], hdrH);
                drawMultilineCentered(g, headers[i], cx, y, colW[i], hdrH);
                cx += colW[i];
            }
            y += hdrH;

            g.setFont(serif.deriveFont(9.5f));
            int rowH = 19;
            for (int i = 0; i < d.lines().size(); i++) {
                LineData line = d.lines().get(i);
                String[] vals = {
                        String.valueOf(i + 1),
                        safe(line.name()),
                        safe(line.unit()),
                        safe(line.origin()),
                        safe(line.batch()),
                        line.expiry() != null ? DATE_SLASH.format(line.expiry()) : "",
                        formatQty(line.quantity()),
                        MONEY_FMT.format(line.unitPriceWithVat()),
                        MONEY_FMT.format(line.amount())
                };
                cx = left;
                for (int c = 0; c < vals.length; c++) {
                    g.drawRect(cx, y, colW[c], rowH);
                    if (c == 0 || c == 2 || c == 4 || c == 5) {
                        drawCentered(g, clipText(g, vals[c], colW[c] - 4), cx + colW[c] / 2, y + 13);
                    } else if (c >= 6) {
                        drawRight(g, clipText(g, vals[c], colW[c] - 6), cx + colW[c] - 4, y + 13);
                    } else {
                        g.drawString(clipText(g, vals[c], colW[c] - 6), cx + 4, y + 13);
                    }
                    cx += colW[c];
                }
                y += rowH;
            }

            // Total row inside table
            int labelSpan = 0;
            for (int i = 0; i < colW.length - 1; i++) labelSpan += colW[i];
            g.drawRect(left, y, labelSpan, rowH);
            g.drawRect(left + labelSpan, y, colW[colW.length - 1], rowH);
            g.setFont(serif.deriveFont(9.5f));
            drawRight(g, "Tổng tiền", left + labelSpan - 6, y + 13);
            drawRight(g, MONEY_FMT.format(d.totalAmount()), right - 4, y + 13);
            y += rowH + 18;

            // Summary lines
            g.setFont(serif);
            g.drawString("Cộng khoản: ", left, y);
            int w1 = g.getFontMetrics().stringWidth("Cộng khoản: ");
            g.setFont(serifBold);
            g.drawString(d.lines().size() + " khoản", left + w1, y);

            y += 16;
            g.setFont(serif);
            g.drawString("Tổng tiền: ", left, y);
            int w2 = g.getFontMetrics().stringWidth("Tổng tiền: ");
            g.setFont(serifBold.deriveFont(Font.BOLD | Font.ITALIC));
            String words = numberToVietnameseWords(d.totalAmount());
            g.drawString(MONEY_FMT.format(d.totalAmount()) + " đ - " + words, left + w2, y);

            // Signature date & 4 columns
            y += 22;
            LocalDate signDate = d.docDate() != null ? d.docDate() : LocalDate.now();
            g.setFont(serifItalic);
            String vnDate = String.format("Ngày %02d tháng %02d năm %d",
                    signDate.getDayOfMonth(), signDate.getMonthValue(), signDate.getYear());
            drawRight(g, vnDate, right - 18, y);

            y += 16;
            String[] roles = {"Người lập phiếu", "Người nhận", "Kế Toán", "Phụ trách chuyên môn"};
            int sigW = (int) (contentW / 4.0);
            for (int i = 0; i < roles.length; i++) {
                int centerX = left + sigW * i + sigW / 2;
                g.setFont(serifBold);
                drawCentered(g, roles[i], centerX, y);
                g.setFont(serif);
                drawCentered(g, "(Ký, họ tên)", centerX, y + 13);
            }

            if (d.creatorName() != null && !d.creatorName().isBlank()) {
                g.setFont(serifBold);
                drawCentered(g, d.creatorName().trim(), left + sigW / 2, y + 72);
            }

            g.dispose();
            return PAGE_EXISTS;
        }

        private static void drawCentered(Graphics2D g, String s, int cx, int y) {
            int sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, cx - sw / 2, y);
        }

        private static void drawRight(Graphics2D g, String s, int rx, int y) {
            int sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, rx - sw, y);
        }

        private static void drawMultilineCentered(Graphics2D g, String text, int x, int y, int w, int h) {
            String[] lines = text.split("\n");
            FontMetrics fm = g.getFontMetrics();
            int totalH = lines.length * fm.getHeight();
            int startY = y + (h - totalH) / 2 + fm.getAscent();
            for (int i = 0; i < lines.length; i++) {
                drawCentered(g, lines[i], x + w / 2, startY + i * fm.getHeight());
            }
        }

        private static String clipText(Graphics2D g, String text, int maxW) {
            if (text == null) return "";
            FontMetrics fm = g.getFontMetrics();
            if (fm.stringWidth(text) <= maxW) return text;
            String s = text;
            while (s.length() > 1 && fm.stringWidth(s + "…") > maxW) {
                s = s.substring(0, s.length() - 1);
            }
            return s + "…";
        }
    }

    private static void writePdf(Printable doc, Path dest, PageFormat fmt) throws IOException, PrinterException {
        double pw = fmt.getWidth(), ph = fmt.getHeight();
        int rw = (int) Math.ceil(pw / 72 * 300);
        int rh = (int) Math.ceil(ph / 72 * 300);
        BufferedImage img = new BufferedImage(rw, rh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, rw, rh);
        g.scale(300.0 / 72, 300.0 / 72);
        doc.print(g, fmt, 0);
        g.dispose();

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (DeflaterOutputStream zip = new DeflaterOutputStream(compressed)) {
            byte[] row = new byte[rw * 3];
            for (int y = 0; y < rh; y++) {
                for (int x = 0; x < rw; x++) {
                    int rgb = img.getRGB(x, y);
                    row[x * 3] = (byte) (rgb >> 16);
                    row[x * 3 + 1] = (byte) (rgb >> 8);
                    row[x * 3 + 2] = (byte) rgb;
                }
                zip.write(row);
            }
        }
        byte[] imgBytes = compressed.toByteArray();
        byte[] cmdBytes = String.format(Locale.ROOT, "q %.4f 0 0 %.4f 0 0 cm /Im0 Do Q", pw, ph).getBytes(StandardCharsets.US_ASCII);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        out.write("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".getBytes(StandardCharsets.ISO_8859_1));
        pdfObj(out, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        pdfObj(out, offsets, 2, "<< /Type /Pages /Count 1 /Kids [3 0 R] >>");
        pdfObj(out, offsets, 3, String.format(Locale.ROOT,
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.4f %.4f] /Resources << /XObject << /Im0 4 0 R >> >> /Contents 5 0 R >>", pw, ph));
        pdfStream(out, offsets, 4, "/Type /XObject /Subtype /Image /Width " + rw + " /Height " + rh
                + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode", imgBytes);
        pdfStream(out, offsets, 5, "", cmdBytes);
        int xref = out.size();
        out.write(("xref\n0 " + offsets.size() + "\n0000000000 65535 f \n").getBytes(StandardCharsets.US_ASCII));
        for (int i = 1; i < offsets.size(); i++) {
            out.write(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i)).getBytes(StandardCharsets.US_ASCII));
        }
        out.write(("trailer\n<< /Size " + offsets.size() + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n")
                .getBytes(StandardCharsets.US_ASCII));
        Files.write(dest, out.toByteArray());
    }

    private static void pdfObj(ByteArrayOutputStream out, List<Integer> offsets, int id, String body) throws IOException {
        offsets.add(out.size());
        out.write((id + " 0 obj\n" + body + "\nendobj\n").getBytes(StandardCharsets.US_ASCII));
    }

    private static void pdfStream(ByteArrayOutputStream out, List<Integer> offsets, int id, String dict, byte[] data) throws IOException {
        offsets.add(out.size());
        String header = id + " 0 obj\n<< " + dict + (dict.isEmpty() ? "" : " ") + "/Length " + data.length + " >>\nstream\n";
        out.write(header.getBytes(StandardCharsets.US_ASCII));
        out.write(data);
        out.write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));
    }

    private static String buildWordHtml(ReceiptPrintData d, String documentTitle, String partnerLabel) {
        LocalDate signDate = d.docDate() != null ? d.docDate() : LocalDate.now();
        StringBuilder sb = new StringBuilder();
        sb.append("<html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word'>")
                .append("<head><meta charset='UTF-8'><style>")
                .append("body{font-family:'Times New Roman',serif;font-size:12pt;margin:28px;}")
                .append("table.grid{border-collapse:collapse;width:100%;margin-top:10px;}")
                .append("table.grid th,table.grid td{border:1px solid #000;padding:4px 6px;}")
                .append(".r{text-align:right;}.c{text-align:center;}")
                .append("</style></head><body>")
                .append("<table style='width:100%'><tr>")
                .append("<td style='width:25%;text-align:center'>").append(esc(d.warehouseName().toUpperCase(Locale.ROOT))).append("</td>")
                .append("<td style='width:50%;text-align:center'><b>CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</b><br>Độc lập – Tự do – Hạnh phúc</td>")
                .append("<td style='width:25%;text-align:right'><i>Số: ").append(esc(d.receiptCode()))
                .append("<br>Ngày lập: ").append(d.docDate() != null ? DATE_SLASH.format(d.docDate()) : "").append("</i></td>")
                .append("</tr></table>")
                .append("<h2 style='text-align:center;margin:14px 0 10px 0'>").append(esc(documentTitle)).append("</h2>")
                .append("<table style='width:100%'><tr>")
                .append("<td><b>").append(esc(partnerLabel)).append("</b> ").append(esc(d.supplierName())).append("</td>")
                .append("<td><b>Số HĐ:</b> ").append(esc(d.invoiceNumber())).append("</td>")
                .append("<td class='r'><b>Ngày HĐ:</b> ").append(d.invoiceDate() != null ? DATE_DASH.format(d.invoiceDate()) : "").append("</td>")
                .append("</tr></table>")
                .append("<table class='grid'><thead><tr>")
                .append("<th>STT</th><th>Tên thuốc, vật tư</th><th>Đơn vị</th><th>Nơi sản xuất</th><th>Số lô</th><th>Hạn dùng</th><th>Số lượng</th><th>Đơn giá (đã VAT)</th><th>Thành tiền</th>")
                .append("</tr></thead><tbody>");
        for (int i = 0; i < d.lines().size(); i++) {
            LineData l = d.lines().get(i);
            sb.append("<tr>")
                    .append("<td class='c'>").append(i + 1).append("</td>")
                    .append("<td>").append(esc(l.name())).append("</td>")
                    .append("<td class='c'>").append(esc(l.unit())).append("</td>")
                    .append("<td>").append(esc(l.origin())).append("</td>")
                    .append("<td class='c'>").append(esc(l.batch())).append("</td>")
                    .append("<td class='c'>").append(l.expiry() != null ? DATE_SLASH.format(l.expiry()) : "").append("</td>")
                    .append("<td class='r'>").append(formatQty(l.quantity())).append("</td>")
                    .append("<td class='r'>").append(MONEY_FMT.format(l.unitPriceWithVat())).append("</td>")
                    .append("<td class='r'>").append(MONEY_FMT.format(l.amount())).append("</td>")
                    .append("</tr>");
        }
        sb.append("<tr><td colspan='8' class='r'>Tổng tiền</td><td class='r'>")
                .append(MONEY_FMT.format(d.totalAmount())).append("</td></tr></tbody></table>")
                .append("<p>Cộng khoản: <b>").append(d.lines().size()).append(" khoản</b><br>")
                .append("Tổng tiền: <b><i>").append(MONEY_FMT.format(d.totalAmount())).append(" đ - ")
                .append(esc(numberToVietnameseWords(d.totalAmount()))).append("</i></b></p>")
                .append("<p class='r'><i>").append(String.format("Ngày %02d tháng %02d năm %d",
                        signDate.getDayOfMonth(), signDate.getMonthValue(), signDate.getYear())).append("</i></p>")
                .append("<table style='width:100%;text-align:center;margin-top:8px'><tr>")
                .append("<td><b>Người lập phiếu</b><br>(Ký, họ tên)<br><br><br><b>").append(esc(d.creatorName())).append("</b></td>")
                .append("<td><b>Người nhận</b><br>(Ký, họ tên)</td>")
                .append("<td><b>Kế Toán</b><br>(Ký, họ tên)</td>")
                .append("<td><b>Phụ trách chuyên môn</b><br>(Ký, họ tên)</td>")
                .append("</tr></table></body></html>");
        return sb.toString();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String safeFileCode(String code) {
        String c = safe(code).replaceAll("[^A-Za-z0-9_-]", "_");
        return c.isEmpty() ? "PhieuNhap" : c;
    }

    private static String formatQty(BigDecimal v) {
        if (v == null) return "0";
        BigDecimal s = v.stripTrailingZeros();
        return s.scale() <= 0 ? MONEY_FMT.format(s) : s.toPlainString();
    }

    /** Đọc số tiền sang chữ tiếng Việt (vd: 1,000,000 -> "Một triệu đồng."). */
    public static String numberToVietnameseWords(BigDecimal amount) {
        if (amount == null) return "Không đồng.";
        long n = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        if (n == 0) return "Không đồng.";
        boolean neg = n < 0;
        if (neg) n = -n;
        String[] units = {"", " nghìn", " triệu", " tỷ", " nghìn tỷ"};
        List<String> groups = new ArrayList<>();
        int u = 0;
        while (n > 0) {
            int g = (int) (n % 1000);
            if (g > 0) {
                boolean full = n >= 1000;
                groups.add(0, readTriple(g, full) + units[Math.min(u, units.length - 1)]);
            }
            n /= 1000;
            u++;
        }
        String joined = String.join(" ", groups).replaceAll("\\s+", " ").trim();
        if (joined.isEmpty()) return "Không đồng.";
        String result = (neg ? "Âm " : "") + Character.toUpperCase(joined.charAt(0)) + joined.substring(1) + " đồng.";
        return result;
    }

    private static String readTriple(int n, boolean full) {
        String[] d = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
        int h = n / 100, t = (n / 10) % 10, o = n % 10;
        StringBuilder sb = new StringBuilder();
        if (h > 0 || full) {
            sb.append(d[h]).append(" trăm");
            if (t == 0 && o > 0) sb.append(" lẻ");
        }
        if (t > 1) {
            sb.append(sb.length() > 0 ? " " : "").append(d[t]).append(" mươi");
            if (o == 1) sb.append(" mốt");
            else if (o == 5) sb.append(" lăm");
            else if (o > 0) sb.append(" ").append(d[o]);
        } else if (t == 1) {
            sb.append(sb.length() > 0 ? " " : "").append("mười");
            if (o == 5) sb.append(" lăm");
            else if (o > 0) sb.append(" ").append(d[o]);
        } else if (o > 0) {
            sb.append(sb.length() > 0 ? " " : "").append(d[o]);
        }
        return sb.toString();
    }

    private static final class WordDocIcon implements Icon {
        @Override public int getIconWidth() { return 28; }
        @Override public int getIconHeight() { return 28; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(43, 87, 154));
            g2.fillRoundRect(x + 2, y + 2, 24, 24, 4, 4);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.drawString("W", x + 8, y + 19);
            g2.dispose();
        }
    }
}
