package vn.clinicmanagement.desktop.modules.billing;

import vn.clinicmanagement.application.billing.QuanLyDoanhThuReadPort;
import vn.clinicmanagement.application.billing.QuanLyDoanhThuReadPort.ClinicSummaryStats;
import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.billing.QuanLyDoanhThuRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Màn hình "Quản Lý Phòng Khám" (mở từ Quản Lý -> Quản Lý Phòng Khám).
 * Hiển thị 9 thẻ KPI tổng hợp doanh thu & biểu đồ trực tiếp từ clinicDb qua QuanLyDoanhThuReadPort.
 */
public final class QuanLyPhongKhamManHinh extends CoSoManHinhManHinh {
    private static final Color TILE_BLUE = new Color(10, 86, 179);
    private static final Color TOOLBAR_BG = new Color(238, 242, 247);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");

    private final QuanLyDoanhThuReadPort readPort;
    private final JTextField fromField = new JTextField();
    private final JTextField toField = new JTextField();
    private final CardLayout contentCards = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentCards);

    private final JLabel valVisits = tileValueLabel();
    private final JLabel valTotalRev = tileValueLabel();
    private final JLabel valExamRev = tileValueLabel();
    private final JLabel valLabRev = tileValueLabel();
    private final JLabel valImagingRev = tileValueLabel();
    private final JLabel valRetailDrug = tileValueLabel();
    private final JLabel valCollected = tileValueLabel();
    private final JLabel valSvcDiscount = tileValueLabel();
    private final JLabel valInvDiscount = tileValueLabel();

    private ClinicSummaryStats latestStats = new ClinicSummaryStats(
            0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    private int loadGen;

    public QuanLyPhongKhamManHinh() {
        super("clinic-management", "Quản Lý", "🏥");
        var uow = new JdbcUnitOfWork();
        var tx = new JdbcGiaoDichQuanLy(uow);
        this.readPort = new QuanLyDoanhThuRepository(new JdbcKetNoiNhaCungCap(tx));

        LocalDate today = LocalDate.now();
        fromField.setText("00:00 " + today.minusDays(30).format(DATE_FMT));
        toField.setText("23:59 " + today.format(DATE_FMT));

        setBackground(Color.WHITE);
        add(buildToolbar(), BorderLayout.NORTH);

        contentPanel.setBackground(Color.WHITE);
        contentPanel.add(buildKpiGrid(), "KPI");
        contentPanel.add(buildChartPanel(), "CHART");
        add(contentPanel, BorderLayout.CENTER);

        refreshData();
    }

    @Override
    public void refreshData() {
        loadStats();
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(TOOLBAR_BG);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(180, 188, 200)));
        bar.setPreferredSize(new Dimension(100, 48));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        left.setOpaque(false);

        JButton menuBtn = new JButton("☰");
        menuBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        menuBtn.setForeground(new Color(0, 102, 184));
        menuBtn.setFocusPainted(false);
        menuBtn.setContentAreaFilled(false);
        menuBtn.setBorder(new EmptyBorder(4, 6, 4, 6));
        menuBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        menuBtn.addActionListener(e -> contentCards.show(contentPanel, "KPI"));

        JButton reportBtn = toolbarBtn("Báo Cáo ▾", reportIcon(20));
        reportBtn.addActionListener(e -> exportSummaryCsv());

        JButton revBtn = toolbarBtn("Doanh Thu", revIcon(20));
        revBtn.addActionListener(e -> contentCards.show(contentPanel, "KPI"));

        JButton chartBtn = toolbarBtn("Biểu Đồ", chartIcon(20));
        chartBtn.addActionListener(e -> {
            contentPanel.repaint();
            contentCards.show(contentPanel, "CHART");
        });

        left.add(menuBtn);
        left.add(reportBtn);
        left.add(revBtn);
        left.add(chartBtn);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        right.setOpaque(false);

        JLabel lFrom = new JLabel("Từ ngày");
        lFrom.setFont(UngDungPhongChu.BODY);
        fromField.setFont(UngDungPhongChu.BODY);
        fromField.setPreferredSize(new Dimension(136, 26));

        JLabel lTo = new JLabel("Đến ngày");
        lTo.setFont(UngDungPhongChu.BODY);
        toField.setFont(UngDungPhongChu.BODY);
        toField.setPreferredSize(new Dimension(136, 26));

        JButton searchBtn = toolbarBtn("Tìm Kiếm ▾", searchIcon(20));
        searchBtn.addActionListener(e -> loadStats());

        right.add(lFrom);
        right.add(fromField);
        right.add(lTo);
        right.add(toField);
        right.add(searchBtn);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildKpiGrid() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Color.WHITE);
        outer.setBorder(new EmptyBorder(14, 18, 18, 18));

        JPanel grid = new JPanel(new GridLayout(3, 4, 18, 18));
        grid.setOpaque(false);

        grid.add(kpiTile("Số lượt khám", valVisits));
        grid.add(kpiTile("Tổng doanh thu", valTotalRev));
        grid.add(kpiTile("Tổng doanh thu khám", valExamRev));
        grid.add(kpiTile("Tổng doanh thu - Xét nghiệm", valLabRev));

        grid.add(kpiTile("Tổng doanh thu - CDHA", valImagingRev));
        grid.add(kpiTile("Tổng doanh thu - Thuốc bán lẻ", valRetailDrug));
        grid.add(kpiTile("Tổng tiền đã thu", valCollected));
        grid.add(kpiTile("Tổng miễn giảm dịch vụ", valSvcDiscount));

        grid.add(kpiTile("Tổng miễn giảm theo phiếu", valInvDiscount));
        grid.add(emptySlot());
        grid.add(emptySlot());
        grid.add(emptySlot());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(grid, BorderLayout.NORTH);
        outer.add(wrapper, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildChartPanel() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics gr) {
                super.paintComponent(gr);
                Graphics2D g = (Graphics2D) gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, w, h);

                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                g.setColor(new Color(20, 60, 120));
                g.drawString("BIỂU ĐỒ TỔNG HỢP DOANH THU PHÒNG KHÁM", 30, 34);

                String[] labels = {"Khám", "Xét nghiệm", "CĐHA", "Thuốc bán lẻ", "Đã thu", "Miễn giảm DV", "Miễn giảm phiếu"};
                BigDecimal[] vals = {
                        latestStats.examRevenue(), latestStats.labRevenue(), latestStats.imagingRevenue(),
                        latestStats.retailDrugRevenue(), latestStats.totalCollected(),
                        latestStats.serviceDiscount(), latestStats.invoiceDiscount()
                };
                BigDecimal max = BigDecimal.ONE;
                for (BigDecimal v : vals) if (v.compareTo(max) > 0) max = v;

                int baseY = h - 70;
                int topY = 70;
                int availH = Math.max(80, baseY - topY);
                int barW = Math.max(48, (w - 120) / (labels.length * 2));
                int step = (w - 80) / labels.length;

                g.setColor(new Color(180, 188, 200));
                g.drawLine(40, baseY, w - 40, baseY);

                for (int i = 0; i < labels.length; i++) {
                    int x = 50 + i * step;
                    double ratio = vals[i].doubleValue() / max.doubleValue();
                    int bh = (int) Math.round(ratio * availH);
                    g.setColor(TILE_BLUE);
                    g.fillRect(x, baseY - bh, barW, bh);
                    g.setColor(Color.BLACK);
                    g.setFont(UngDungPhongChu.SMALL_BOLD);
                    g.drawString(MONEY_FMT.format(vals[i]), x, baseY - bh - 6);
                    g.setFont(UngDungPhongChu.SMALL);
                    g.drawString(labels[i], x, baseY + 18);
                }
                g.dispose();
            }
        };
        p.setBackground(Color.WHITE);
        return p;
    }

    private void loadStats() {
        int gen = ++loadGen;
        LocalDateTime from = parseDt(fromField.getText(), LocalDate.now().minusDays(30).atStartOfDay());
        LocalDateTime to = parseDt(toField.getText(), LocalDate.now().atTime(23, 59, 59));
        var ctx = KhamBenhPhienNguCanh.current();
        int facilityId = ctx == null ? 1 : ctx.facilityId();

        new SwingWorker<ClinicSummaryStats, Void>() {
            @Override protected ClinicSummaryStats doInBackground() {
                return readPort.getClinicSummary(facilityId, from, to);
            }

            @Override protected void done() {
                if (gen != loadGen || !isDisplayable()) return;
                try {
                    latestStats = get();
                    valVisits.setText(MONEY_FMT.format(latestStats.visitCount()));
                    valTotalRev.setText(MONEY_FMT.format(latestStats.totalRevenue()));
                    valExamRev.setText(MONEY_FMT.format(latestStats.examRevenue()));
                    valLabRev.setText(MONEY_FMT.format(latestStats.labRevenue()));
                    valImagingRev.setText(MONEY_FMT.format(latestStats.imagingRevenue()));
                    valRetailDrug.setText(MONEY_FMT.format(latestStats.retailDrugRevenue()));
                    valCollected.setText(MONEY_FMT.format(latestStats.totalCollected()));
                    valSvcDiscount.setText(MONEY_FMT.format(latestStats.serviceDiscount()));
                    valInvDiscount.setText(MONEY_FMT.format(latestStats.invoiceDiscount()));
                    contentPanel.repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(QuanLyPhongKhamManHinh.this,
                            "Không tải được thống kê phòng khám: " + ex.getMessage(),
                            "Quản Lý Phòng Khám", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void exportSummaryCsv() {
        JFileChooser picker = new JFileChooser();
        picker.setSelectedFile(new File("quan-ly-phong-kham.csv"));
        if (picker.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        List<String[]> rows = List.of(
                new String[]{"Chỉ tiêu", "Giá trị"},
                new String[]{"Số lượt khám", MONEY_FMT.format(latestStats.visitCount())},
                new String[]{"Tổng doanh thu", MONEY_FMT.format(latestStats.totalRevenue())},
                new String[]{"Tổng doanh thu khám", MONEY_FMT.format(latestStats.examRevenue())},
                new String[]{"Tổng doanh thu - Xét nghiệm", MONEY_FMT.format(latestStats.labRevenue())},
                new String[]{"Tổng doanh thu - CDHA", MONEY_FMT.format(latestStats.imagingRevenue())},
                new String[]{"Tổng doanh thu - Thuốc bán lẻ", MONEY_FMT.format(latestStats.retailDrugRevenue())},
                new String[]{"Tổng tiền đã thu", MONEY_FMT.format(latestStats.totalCollected())},
                new String[]{"Tổng miễn giảm dịch vụ", MONEY_FMT.format(latestStats.serviceDiscount())},
                new String[]{"Tổng miễn giảm theo phiếu", MONEY_FMT.format(latestStats.invoiceDiscount())}
        );
        try (var writer = new OutputStreamWriter(new FileOutputStream(picker.getSelectedFile()), StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            for (String[] r : rows) {
                writer.write('"' + r[0] + "\",\"" + r[1] + "\"\r\n");
            }
            JOptionPane.showMessageDialog(this, "Đã xuất báo cáo CSV.", "Quản Lý Phòng Khám", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không ghi được tệp CSV.", "Quản Lý Phòng Khám", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JPanel kpiTile(String title, JLabel valueLabel) {
        JPanel tile = new JPanel(new BorderLayout());
        tile.setBackground(TILE_BLUE);
        tile.setPreferredSize(new Dimension(220, 145));
        tile.setBorder(new EmptyBorder(10, 14, 14, 14));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        titleLbl.setForeground(Color.WHITE);
        tile.add(titleLbl, BorderLayout.NORTH);
        tile.add(valueLabel, BorderLayout.CENTER);
        return tile;
    }

    private static JLabel tileValueLabel() {
        JLabel l = new JLabel("0", SwingConstants.CENTER);
        l.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 32));
        l.setForeground(Color.WHITE);
        return l;
    }

    private static JPanel emptySlot() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }

    private static JButton toolbarBtn(String text, Icon icon) {
        JButton b = new JButton(text, icon);
        b.setFont(UngDungPhongChu.BODY);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorder(new EmptyBorder(4, 8, 4, 8));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static LocalDateTime parseDt(String text, LocalDateTime fallback) {
        try { return LocalDateTime.parse(text == null ? "" : text.trim(), INPUT_FMT); }
        catch (DateTimeParseException ex) { return fallback; }
    }

    private static Icon reportIcon(int size) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.translate(x, y);
                g.setColor(new Color(90, 150, 210));
                g.fillRoundRect(2, 6, size - 4, size - 9, 3, 3);
                g.setColor(Color.WHITE);
                g.fillRect(5, 2, size - 10, 6);
                g.fillRect(5, 11, size - 10, 7);
                g.dispose();
            }
        };
    }

    private static Icon revIcon(int size) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.translate(x, y);
                g.setColor(Color.WHITE);
                g.fillRect(3, 2, size - 6, size - 4);
                g.setColor(new Color(80, 95, 115));
                g.drawRect(3, 2, size - 6, size - 4);
                g.setColor(new Color(195, 45, 45));
                g.fillRect(6, 6, 4, 4);
                g.dispose();
            }
        };
    }

    private static Icon chartIcon(int size) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.translate(x, y);
                g.setColor(new Color(40, 150, 90));
                g.fillRect(2, 10, 4, 8);
                g.setColor(new Color(230, 145, 30));
                g.fillRect(8, 6, 4, 12);
                g.setColor(new Color(45, 115, 200));
                g.fillRect(14, 3, 4, 15);
                g.dispose();
            }
        };
    }

    private static Icon searchIcon(int size) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.translate(x, y);
                g.setColor(new Color(65, 135, 195));
                g.setStroke(new BasicStroke(2f));
                g.drawOval(2, 2, size - 9, size - 9);
                g.drawLine(size - 8, size - 8, size - 2, size - 2);
                g.dispose();
            }
        };
    }
}
