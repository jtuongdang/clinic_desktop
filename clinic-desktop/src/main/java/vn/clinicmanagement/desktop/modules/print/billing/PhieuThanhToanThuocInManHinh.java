package vn.clinicmanagement.desktop.modules.print.billing;


import vn.clinicmanagement.desktop.modules.billing.*;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

/**
 * Cửa sổ xem trước & in Phiếu Thanh Toán Thuốc (mẫu EHC NT_029_A4_PhieuThanhToanThuoc.doc).
 * Tự mở sau khi Thu Ngân thu tiền hóa đơn có đơn thuốc và có thể mở lại bằng nút In (F7).
 */
public final class PhieuThanhToanThuocInManHinh extends JPanel {

    public record DrugBillLine(
            String name,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal total,
            BigDecimal insurancePay,
            BigDecimal patientPay) {}

    public record DrugBillData(
            String facilityName,
            String patientCode,
            String patientName,
            String birthYear,
            String gender,
            String address,
            String subjectLabel,
            String bhytNumber,
            LocalDate bhytExpiry,
            String departmentRoom,
            String diagnosis,
            LocalDateTime printedAt,
            List<DrugBillLine> lines,
            BigDecimal totalAmount,
            BigDecimal totalInsurance,
            BigDecimal totalPatient) {
        public DrugBillData {
            lines = lines != null ? List.copyOf(lines) : List.of();
            totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
            totalInsurance = totalInsurance != null ? totalInsurance : BigDecimal.ZERO;
            totalPatient = totalPatient != null ? totalPatient : BigDecimal.ZERO;
        }
    }

    private static final DateTimeFormatter DATE_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FMT;
    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator(',');
        sym.setDecimalSeparator('.');
        MONEY_FMT = new DecimalFormat("#,##0", sym);
    }

    private final DrugBillData data;
    private final DrugBillPrintable printable;
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
    private final JSpinner page = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
    private final JSlider zoom = new JSlider(40, 200, 100);
    private final JLabel zoomLabel = new JLabel("100%");
    private final PreviewPanel preview = new PreviewPanel();
    private final JButton btnPrint = new JButton("In", new PhieuInBieuTuong(false));
    private final JButton btnPdf = new JButton("Pdf", new PhieuInBieuTuong(true));
    private PageFormat configuredFormat;
    private boolean busy;
    private boolean closed;
    private static boolean autoPrintPref;
    private static boolean keepOpenPref;

    public PhieuThanhToanThuocInManHinh(DrugBillData data) {
        this.data = Objects.requireNonNull(data, "data");
        this.printable = new DrugBillPrintable(data);

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

    public static void showPreviewDialog(Component parent, DrugBillData data) {
        if (GraphicsEnvironment.isHeadless() || data == null) return;
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        PhieuThanhToanThuocInManHinh panel = new PhieuThanhToanThuocInManHinh(data);
        JDialog dialog = new JDialog(owner, "In - NT_029_A4_PhieuThanhToanThuoc.doc", Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(panel);
        GraphicsConfiguration gc = owner != null ? owner.getGraphicsConfiguration() : null;
        Rectangle screen;
        if (gc != null) {
            Rectangle bounds = gc.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            screen = new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
                    bounds.width - insets.left - insets.right,
                    bounds.height - insets.top - insets.bottom);
        } else {
            screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        }
        dialog.setBounds(screen);
        dialog.setVisible(true);
        SwingUtilities.invokeLater(panel::updatePreview);
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(null);
        side.setPreferredSize(new Dimension(270, 680));
        side.setBackground(new Color(248, 248, 248));
        side.setBorder(new MatteBorder(0, 0, 0, 1, Color.GRAY));

        btnPrint.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnPrint.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPrint.setFont(UngDungPhongChu.PHIEU_IN_NUT);
        btnPrint.setBorder(new LineBorder(new Color(95, 85, 185)));
        place(side, btnPrint, 10, 8, 130, 108);

        place(side, new JLabel("Bản in"), 149, 9, 54, 22);
        place(side, copies, 204, 7, 52, 26);
        place(side, autoPrint, 149, 42, 110, 26);
        place(side, keepOpen, 149, 72, 110, 26);

        addLabel(side, "Máy In", 8, 136, true);
        place(side, printers, 8, 159, 248, 30);
        place(side, trays, 8, 192, 248, 28);

        JButton settings = new JButton("<html><u>Cấu hình máy in</u></html>");
        settings.setForeground(new Color(55, 50, 180));
        settings.setBorderPainted(false);
        settings.setContentAreaFilled(false);
        settings.setMargin(new Insets(0, 0, 0, 0));
        settings.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settings.addActionListener(e -> printerSettings());
        place(side, settings, 134, 224, 122, 24);

        addLabel(side, "Tùy Chọn", 8, 258, true);
        place(side, scope, 8, 282, 248, 30);
        addLabel(side, "Trang in :", 8, 316, false);
        place(side, pageRange, 94, 313, 162, 28);
        place(side, duplex, 94, 345, 162, 26);

        addLabel(side, "Chiều giấy", 8, 379, false);
        place(side, orientation, 8, 402, 248, 30);
        addLabel(side, "Khổ giấy", 8, 436, false);
        place(side, paper, 8, 459, 248, 30);
        place(side, a5onA4, 8, 492, 250, 27);

        addLabel(side, "Xuất File...", 8, 528, true);
        btnPdf.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnPdf.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPdf.setFont(UngDungPhongChu.PHIEU_IN_NUT);
        btnPdf.setBorder(new LineBorder(new Color(95, 85, 185)));
        place(side, btnPdf, 8, 554, 120, 82);

        for (Component c : side.getComponents()) {
            if (c != btnPrint && c != btnPdf) {
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
        bar.setPreferredSize(new Dimension(1000, 54));
        bar.setBorder(new MatteBorder(1, 0, 0, 0, Color.GRAY));

        JPanel menu = new JPanel(new FlowLayout(FlowLayout.LEFT));
        menu.setPreferredSize(new Dimension(270, 54));
        JButton closeBtn = new JButton(TiepDonBieuTuong.createMenuIcon());
        closeBtn.setToolTipText("Đóng cửa sổ xem trước");
        closeBtn.addActionListener(e -> close());
        menu.add(closeBtn);
        bar.add(menu, BorderLayout.WEST);

        JPanel pages = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
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
        zoom.setPreferredSize(new Dimension(300, 24));
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
        double scale = previewScale(d);
        preview.setPreferredSize(new Dimension(
                (int) Math.ceil(d[0] * scale) + 48,
                (int) Math.ceil(d[1] * scale) + 48));
        preview.revalidate();
        preview.repaint();
    }

    private double previewScale(double[] pageDimensions) {
        double fit = 1.25;
        Container viewport = preview.getParent();
        if (viewport instanceof JViewport view) {
            Dimension extent = view.getExtentSize();
            if (extent.width > 0 && extent.height > 0) {
                double fitWidth = Math.max(1, extent.width - 48) / pageDimensions[0];
                double fitHeight = Math.max(1, extent.height - 48) / pageDimensions[1];
                fit = Math.min(fit, Math.min(fitWidth, fitHeight));
            }
        }
        return fit * zoom.getValue() / 100.0;
    }

    private void printDocument() {
        if (busy) return;
        PrintService svc = (PrintService) printers.getSelectedItem();
        if (svc == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy máy in khả dụng.", "In phiếu thanh toán thuốc", JOptionPane.WARNING_MESSAGE);
            return;
        }
        busy = true;
        btnPrint.setEnabled(false);
        PageFormat fmt = pageFormat();
        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        attrs.add(new Copies((int) copies.getValue()));
        if (duplex.isSelected()) attrs.add(Sides.DUPLEX);
        if (trays.getSelectedItem() instanceof MediaTray tray) attrs.add(tray);

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPrintService(svc);
                job.setJobName("PhieuThanhToanThuoc_" + safeFileCode(data.patientCode()));
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
                    JOptionPane.showMessageDialog(PhieuThanhToanThuocInManHinh.this,
                            "Không thể in phiếu thanh toán thuốc: " + ex.getMessage(), "Lỗi in", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void exportPdf() {
        if (busy) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("NT_029_PhieuThanhToanThuoc_" + safeFileCode(data.patientCode()) + ".pdf"));
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
                    JOptionPane.showMessageDialog(PhieuThanhToanThuocInManHinh.this,
                            "Đã xuất PDF thành công:\n" + dest.toAbsolutePath(),
                            "Xuất PDF", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PhieuThanhToanThuocInManHinh.this,
                            "Không thể xuất PDF: " + ex.getMessage(), "Lỗi xuất PDF", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
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
            double scale = previewScale(d);
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

    /** Bộ vẽ trang Phiếu Thanh Toán Thuốc A4 chuẩn EHC NT_029_A4_PhieuThanhToanThuoc.doc. */
    static final class DrugBillPrintable implements Printable {
        private final DrugBillData d;

        DrugBillPrintable(DrugBillData d) { this.d = d; }

        @Override
        public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {
            if (pageIndex > 0) return NO_SUCH_PAGE;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.BLACK);

            double w = pf.getWidth();
            int left = 36;
            int right = (int) (w - 36);
            int contentW = right - left;

            Font serif = new Font("Times New Roman", Font.PLAIN, 11);
            Font serifBold = serif.deriveFont(Font.BOLD);
            Font serifItalic = serif.deriveFont(Font.ITALIC);
            Font titleFont = new Font("Times New Roman", Font.BOLD, 15);

            int y = 34;
            // Top header: Sở Y Tế / Cơ sở (left) + PID & Barcode (right)
            g.setFont(serif);
            g.drawString("SỞ Y TẾ HÀ NỘI", left, y);
            String fac = safe(d.facilityName()).isEmpty() ? "PHÒNG KHÁM ĐA KHOA" : d.facilityName().toUpperCase(Locale.ROOT);
            g.drawString(fac, left, y + 16);

            String pidText = "PID: " + safe(d.patientCode());
            g.setFont(serifBold);
            int pidW = g.getFontMetrics().stringWidth(pidText);
            int barW = Math.max(115, pidW + 20);
            int barX = right - barW;
            drawCentered(g, pidText, barX + barW / 2, y);
            drawBarcode(g, safe(d.patientCode()), barX, y + 5, barW, 24);

            // Title
            y += 42;
            g.setFont(titleFont);
            drawCentered(g, "PHIẾU THANH TOÁN THUỐC", (int) (w / 2), y);

            // Patient details
            y += 28;
            g.setFont(serif);
            g.drawString("- Họ tên người bệnh: ", left, y);
            int lw = g.getFontMetrics().stringWidth("- Họ tên người bệnh: ");
            g.setFont(serifBold);
            g.drawString(safe(d.patientName()).toUpperCase(Locale.ROOT), left + lw, y);

            g.setFont(serif);
            String nsGender = "NS: " + safe(d.birthYear()) + "     " + safe(d.gender());
            g.drawString(nsGender, (int) (w * 0.62), y);

            y += 17;
            g.drawString("- Địa chỉ: " + safe(d.address()), left, y);

            y += 17;
            g.drawString("- Đối tượng: " + safe(d.subjectLabel()), left, y);

            y += 17;
            String bhytLine = "- Số thẻ BHYT: " + safe(d.bhytNumber());
            g.drawString(bhytLine, left, y);
            String expText = "- Hạn thẻ đến: " + (d.bhytExpiry() != null ? DATE_SLASH.format(d.bhytExpiry()) : "");
            g.drawString(expText, (int) (w * 0.48), y);

            y += 17;
            g.drawString("- Khoa phòng: " + safe(d.departmentRoom()), left, y);

            y += 17;
            g.drawString("- Chẩn đoán: " + clipText(g, safe(d.diagnosis()), contentW - 70), left, y);

            // Table with 2-level header (exact NT_029 layout)
            y += 14;
            double[] ratios = {0.39, 0.072, 0.083, 0.104, 0.13, 0.109, 0.112};
            int[] colW = new int[ratios.length];
            int used = 0;
            for (int i = 0; i < ratios.length - 1; i++) {
                colW[i] = (int) Math.round(contentW * ratios[i]);
                used += colW[i];
            }
            colW[ratios.length - 1] = contentW - used;

            int topHdrH = 40;
            int subHdrH = 40;
            int fullHdrH = topHdrH + subHdrH;
            int numRowH = 26;

            g.setFont(serifBold.deriveFont(10f));
            int cx = left;
            String[] singleHeaders = {"Tên thuốc", "Đơn\nvị\ntính", "Số\nlượng", "Đơn giá\n(đồng)", "Thành tiền\n(đồng)"};
            for (int i = 0; i < 5; i++) {
                g.drawRect(cx, y, colW[i], fullHdrH);
                drawMultilineCentered(g, singleHeaders[i], cx, y, colW[i], fullHdrH);
                cx += colW[i];
            }
            // Merged header: Nguồn thanh toán (đồng) -> BHYT | Người bệnh
            int srcW = colW[5] + colW[6];
            g.drawRect(cx, y, srcW, topHdrH);
            drawMultilineCentered(g, "Nguồn thanh toán\n(đồng)", cx, y, srcW, topHdrH);
            g.drawRect(cx, y + topHdrH, colW[5], subHdrH);
            drawMultilineCentered(g, "BHYT", cx, y + topHdrH, colW[5], subHdrH);
            g.drawRect(cx + colW[5], y + topHdrH, colW[6], subHdrH);
            drawMultilineCentered(g, "Người\nbệnh", cx + colW[5], y + topHdrH, colW[6], subHdrH);
            y += fullHdrH;

            // Column number row (1) .. (7)=(5)-(6)
            g.setFont(serif.deriveFont(9.5f));
            String[] colNums = {"(1)", "(2)", "(3)", "(4)", "(5)=(3)x(4)", "(6)", "(7)=(5)-(6)"};
            cx = left;
            for (int i = 0; i < colNums.length; i++) {
                g.drawRect(cx, y, colW[i], numRowH);
                drawMultilineCentered(g, colNums[i], cx, y, colW[i], numRowH);
                cx += colW[i];
            }
            y += numRowH;

            // Data rows
            int rowH = 24;
            g.setFont(serif.deriveFont(10f));
            for (int i = 0; i < d.lines().size(); i++) {
                DrugBillLine line = d.lines().get(i);
                String[] vals = {
                        (i + 1) + ". " + safe(line.name()),
                        safe(line.unit()),
                        formatQty(line.quantity()),
                        MONEY_FMT.format(line.unitPrice()),
                        MONEY_FMT.format(line.total()),
                        MONEY_FMT.format(line.insurancePay()),
                        MONEY_FMT.format(line.patientPay())
                };
                cx = left;
                for (int c = 0; c < vals.length; c++) {
                    g.drawRect(cx, y, colW[c], rowH);
                    if (c == 0) {
                        g.drawString(clipText(g, vals[c], colW[c] - 8), cx + 4, y + 17);
                    } else if (c == 1) {
                        drawCentered(g, clipText(g, vals[c], colW[c] - 4), cx + colW[c] / 2, y + 17);
                    } else {
                        drawRight(g, clipText(g, vals[c], colW[c] - 6), cx + colW[c] - 4, y + 17);
                    }
                    cx += colW[c];
                }
                y += rowH;
            }

            // Total row
            int labelSpan = colW[0] + colW[1] + colW[2] + colW[3];
            g.setFont(serifBold.deriveFont(10.5f));
            g.drawRect(left, y, labelSpan, rowH + 2);
            g.drawString("Tổng Tiền", left + 5, y + 17);
            int tx = left + labelSpan;
            BigDecimal[] totals = {d.totalAmount(), d.totalInsurance(), d.totalPatient()};
            for (int i = 0; i < 3; i++) {
                g.drawRect(tx, y, colW[4 + i], rowH + 2);
                drawRight(g, MONEY_FMT.format(totals[i]), tx + colW[4 + i] - 4, y + 17);
                tx += colW[4 + i];
            }
            y = Math.max(y + rowH + 28, (int) pf.getHeight() - 65);

            // Signature block bottom right
            LocalDateTime ts = d.printedAt() != null ? d.printedAt() : LocalDateTime.now();
            String dateStr = String.format("%02d:%02d, ngày %02d tháng %02d năm %d",
                    ts.getHour(), ts.getMinute(), ts.getDayOfMonth(), ts.getMonthValue(), ts.getYear());
            int sigCenterX = left + (int) (contentW * 0.72);
            g.setFont(serifItalic);
            drawCentered(g, dateStr, sigCenterX, y);
            g.setFont(serifBold);
            drawCentered(g, "BÁC SĨ CHỈ ĐỊNH", sigCenterX, y + 20);

            g.dispose();
            return PAGE_EXISTS;
        }

        private static void drawBarcode(Graphics2D g, String code, int x, int y, int w, int h) {
            String src = code.isEmpty() ? "00000000" : code;
            int bars = 48;
            double step = (double) w / (bars * 2);
            int hash = src.hashCode();
            for (int i = 0; i < bars; i++) {
                int ch = src.charAt(i % src.length());
                boolean wide = ((ch + i * 7 + hash) & 3) == 0;
                int bx = x + (int) Math.round(i * step * 2);
                int bw = Math.max(1, (int) Math.round(wide ? step * 1.45 : step * 0.85));
                if (bx + bw <= x + w) {
                    g.fillRect(bx, y, bw, h);
                }
            }
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

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String safeFileCode(String code) {
        String c = safe(code).replaceAll("[^A-Za-z0-9_-]", "_");
        return c.isEmpty() ? "BenhNhan" : c;
    }

    private static String formatQty(BigDecimal v) {
        if (v == null) return "0";
        BigDecimal s = v.stripTrailingZeros();
        return s.scale() <= 0 ? MONEY_FMT.format(s) : s.toPlainString();
    }
}
