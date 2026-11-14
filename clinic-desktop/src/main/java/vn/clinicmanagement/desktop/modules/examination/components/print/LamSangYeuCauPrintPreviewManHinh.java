package vn.clinicmanagement.desktop.modules.examination.components.print;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * Giao diện Xem và In: "PHIẾU CHỈ ĐỊNH CẬN LÂM SÀNG" (EHC EMR Clinical Order Print Station).
 * Kích hoạt khi người dùng nhấn nút [🖨 Phiếu Chỉ Định (F2)] trên tab Xét Nghiệm.
 * Khớp 100% media_1793380792703_8babf04f.jpg:
 * - Cột chức năng bên trái (Bệnh án điện tử):
 *   + [✍ Ký Tài Liệu]
 *   + [☑ Duyệt Tài Liệu] (disabled)
 *   + [💾 Lưu Tài Liệu]
 *   + [✕ Hủy Tài Liệu] (disabled)
 *   + Xuất file... [📄 PDF] [☰ ▾]
 *   + Dòng chữ đỏ chân trang: "Chưa ký 1"
 * - Thanh công cụ đỉnh:
 *   + Cụm trái: [🖨 In Tài Liệu], Loại chữ ký : ---, Hiệu lực : ---, [✔ Kiểm Tra]
 *   + Cụm phải: Banner thông tin bệnh nhân NGUYỄN HOÀNG LONG | BN-20261107-0003 | 1995 (31 tuổi) | Nam | BHYT 80%
 * - Vùng trung tâm: Trang giấy A4 mô phỏng chuẩn xác mẫu PHIẾU CHỈ ĐỊNH CẬN LÂM SÀNG
 *   + Logo Bệnh Viện Sản Nhi màu vàng đỏ
 *   + Barcode PID: 22001324 / Số BA: 22000063
 *   + Tiêu đề: PHIẾU CHỈ ĐỊNH CẬN LÂM SÀNG
 *   + Thông tin hành chính: Họ tên, NS, Giới tính, Địa chỉ, Đối tượng, Số thẻ BHYT, Khoa phòng, Chẩn đoán
 *   + Bảng dịch vụ chỉ định:
 *     - Nhóm I: PHÒNG LẤY MẪU BỆNH PHẨM (Tế bào: Xét nghiệm dịch/tả, Nhuộm Periodic Acide Schiff)
 *     - Nhóm II: DT CHẨN ĐOÁN HÌNH ẢNH (X-quang: Chụp Hirtz, Chụp xương chính mũi nghiêng)
 */
public class LamSangYeuCauPrintPreviewManHinh extends JPanel {

    /** Trường `name`: tên. */
    /** Trường `unit`: đơn vị. */
    public record OrderItem(String group, String name, String quantity, String unit, String notes) {}

    public record ClinicalOrderData(
            /** Trường `patientCode`: bệnh nhân mã. */
            String patientCode,
            /** Trường `patientName`: bệnh nhân tên. */
            String patientName,
            /** Trường `patientBirthYear`: bệnh nhân `Birth` `Year`. */
            String patientBirthYear,
            /** Trường `patientAge`: bệnh nhân `Age`. */
            String patientAge,
            /** Trường `patientGender`: bệnh nhân `Gender`. */
            String patientGender,
            /** Trường `patientBhyt`: bệnh nhân `Bhyt`. */
            String patientBhyt,
            /** Trường `patientExpiry`: bệnh nhân `Expiry`. */
            String patientExpiry,
            /** Trường `patientAddress`: bệnh nhân `Address`. */
            String patientAddress,
            /** Trường `patientDepartment`: bệnh nhân khoa. */
            String patientDepartment,
            /** Trường `patientDiagnosis`: bệnh nhân chandoan. */
            String patientDiagnosis,
            /** Trường `medicalRecordCode`: yte ghi nhận mã. */
            String medicalRecordCode,
            /** Trường `pidCode`: `pid` mã. */
            String pidCode,
            /** Trường `invoiceCode`: hóa đơn mã. */
            String invoiceCode,
            String daysLeft,
            /** Trường `orderDateTime`: yêu cầu ngày thời gian. */
            String orderDateTime,
            java.util.List<OrderItem> items) {
        public ClinicalOrderData {
            items = items == null ? java.util.List.of() : java.util.List.copyOf(items);
        }
    }

    /** Trường `boundData`: `bound` dữ liệu. */
    private ClinicalOrderData boundData;
    /** Trường `patientCode`: bệnh nhân mã. */
    private String patientCode = "";
    /** Trường `patientName`: bệnh nhân tên. */
    private String patientName = "";
    /** Trường `patientBirthYear`: bệnh nhân `Birth` `Year`. */
    private String patientBirthYear = "";
    /** Trường `patientAge`: bệnh nhân `Age`. */
    private String patientAge = "";
    /** Trường `patientGender`: bệnh nhân `Gender`. */
    private String patientGender = "";
    /** Trường `patientBhyt`: bệnh nhân `Bhyt`. */
    private String patientBhyt = "";
    /** Trường `patientExpiry`: bệnh nhân `Expiry`. */
    private String patientExpiry = "";
    /** Trường `patientAddress`: bệnh nhân `Address`. */
    private String patientAddress = "";
    /** Trường `patientDepartment`: bệnh nhân khoa. */
    private String patientDepartment = "";
    /** Trường `patientDiagnosis`: bệnh nhân chandoan. */
    private String patientDiagnosis = "";
    /** Trường `medicalRecordCode`: yte ghi nhận mã. */
    private String medicalRecordCode = "";
    /** Trường `pidCode`: `pid` mã. */
    private String pidCode = "";
    /** Trường `invoiceCode`: hóa đơn mã. */
    private String invoiceCode = "";
    private String daysLeft = "";
    /** Trường `orderDateTime`: yêu cầu ngày thời gian. */
    private String orderDateTime = "";

    public LamSangYeuCauPrintPreviewManHinh() {
        this(null);
    }

    public LamSangYeuCauPrintPreviewManHinh(ClinicalOrderData data) {
        bindData(data);
    }

    /** Hàm `bindData`: gắn kết dữ liệu. */
    public void bindData(ClinicalOrderData data) {
        this.boundData = data;
        if (data != null) {
            this.patientCode = data.patientCode() != null ? data.patientCode() : "";
            this.patientName = data.patientName() != null ? data.patientName() : "";
            this.patientBirthYear = data.patientBirthYear() != null ? data.patientBirthYear() : "";
            this.patientAge = data.patientAge() != null ? data.patientAge() : "";
            this.patientGender = data.patientGender() != null ? data.patientGender() : "";
            this.patientBhyt = data.patientBhyt() != null ? data.patientBhyt() : "";
            this.patientExpiry = data.patientExpiry() != null ? data.patientExpiry() : "";
            this.patientAddress = data.patientAddress() != null ? data.patientAddress() : "";
            this.patientDepartment = data.patientDepartment() != null ? data.patientDepartment() : "";
            this.patientDiagnosis = data.patientDiagnosis() != null ? data.patientDiagnosis() : "";
            this.medicalRecordCode = data.medicalRecordCode() != null ? data.medicalRecordCode() : "";
            this.pidCode = data.pidCode() != null ? data.pidCode() : "";
            this.invoiceCode = data.invoiceCode() != null ? data.invoiceCode() : "";
            this.daysLeft = data.daysLeft() != null ? data.daysLeft() : "";
            this.orderDateTime = data.orderDateTime() != null ? data.orderDateTime() : "";
        }
        rebuildUi();
    }

    /** Hàm `getBoundData`: lấy `Bound` dữ liệu. */
    public ClinicalOrderData getBoundData() {
        return boundData;
    }

    /** Hàm `rebuildUi`: `rebuild` giao diện. */
    private void rebuildUi() {
        removeAll();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. THANH CÔNG CỤ ĐỈNH KÈM BANNER BỆNH NHÂN GÓC PHẢI
        add(buildTopToolBar(), BorderLayout.NORTH);

        // 2. KHUNG TRUNG TÂM: CỘT BÊN TRÁI + VÙNG XEM TÀI LIỆU
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBackground(new Color(90, 98, 110));

        // Cột bên trái: Các nút Ký, Duyệt, Lưu, Hủy, Xuất PDF
        centerContainer.add(buildLeftSidebar(), BorderLayout.WEST);

        // Vùng xem trước trang giấy in A4
        JScrollPane scrollPane = new JScrollPane(new ClinicalOrderSheet());
        scrollPane.getViewport().setBackground(new Color(105, 115, 128));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        centerContainer.add(scrollPane, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /** Hàm `buildTopToolBar`: tạo tren congcu thanh. */
    private JPanel buildTopToolBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(242, 246, 252));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 215, 228)));
        bar.setPreferredSize(new Dimension(1000, 48));

        // Cụm trái: In tài liệu, Loại chữ ký, Hiệu lực, Kiểm tra
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        left.setOpaque(false);

        JButton btnPrint = new JButton(" In Tài Liệu", new PrinterVectorIcon(18, 18));
        btnPrint.setFont(UngDungPhongChu.SMALL_BOLD);
        btnPrint.setBackground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrint.setPreferredSize(new Dimension(105, 34));
        btnPrint.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        btnPrint.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Hệ thống đang gửi lệnh in tài liệu tới máy in mặc định:\n" +
                    "▶ Mẫu in: PHIẾU CHỈ ĐỊNH CẬN LÂM SÀNG\n" +
                    "▶ Bệnh nhân: " + patientName + " (Mã BN: " + patientCode + ")\n" +
                    "▶ Số BA: " + medicalRecordCode,
                    "In Tài Liệu", JOptionPane.INFORMATION_MESSAGE);
        });

        JLabel lblSigType = new JLabel("Loại chữ ký : ---");
        lblSigType.setFont(UngDungPhongChu.SMALL);
        lblSigType.setForeground(new Color(40, 45, 55));

        JLabel lblValidity = new JLabel("Hiệu lực : ---");
        lblValidity.setFont(UngDungPhongChu.SMALL);
        lblValidity.setForeground(new Color(40, 45, 55));

        JButton btnVerify = new JButton(" Kiểm Tra", new CheckmarkMiniIcon());
        btnVerify.setFont(UngDungPhongChu.SMALL_BOLD);
        btnVerify.setBackground(Color.WHITE);
        btnVerify.setFocusPainted(false);
        btnVerify.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVerify.setPreferredSize(new Dimension(92, 26));
        btnVerify.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(180, 190, 205), 1),
                new EmptyBorder(1, 4, 1, 4)
        ));

        left.add(btnPrint);
        left.add(Box.createHorizontalStrut(6));
        left.add(lblSigType);
        left.add(lblValidity);
        left.add(Box.createHorizontalStrut(4));
        left.add(btnVerify);

        // Cụm phải: Banner thông tin bệnh nhân góc phải
        JPanel right = new JPanel(new BorderLayout(0, 0));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(2, 6, 2, 8));

        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        line1.setOpaque(false);
        JLabel lblPtName = new JLabel(patientName + " | " + patientCode + " | " + patientBirthYear + " (" + patientAge + " tuổi) | " + patientGender);
        lblPtName.setFont(UngDungPhongChu.SMALL_BOLD);
        lblPtName.setForeground(new Color(140, 20, 20));

        JLabel lblBhyt = new JLabel("BHYT 80%");
        lblBhyt.setFont(UngDungPhongChu.SMALL_BOLD);
        lblBhyt.setForeground(new Color(180, 20, 20));

        line1.add(lblPtName);
        line1.add(lblBhyt);

        JLabel lblLine2 = new JLabel("Mã VP : " + invoiceCode + " | " + patientBhyt + " | " + daysLeft, SwingConstants.RIGHT);
        lblLine2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblLine2.setForeground(new Color(60, 65, 75));

        JLabel lblLine3 = new JLabel(patientAddress, SwingConstants.RIGHT);
        lblLine3.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblLine3.setForeground(new Color(60, 65, 75));

        JPanel pnlRightLines = new JPanel();
        pnlRightLines.setLayout(new BoxLayout(pnlRightLines, BoxLayout.Y_AXIS));
        pnlRightLines.setOpaque(false);
        line1.setAlignmentX(Component.RIGHT_ALIGNMENT);
        lblLine2.setAlignmentX(Component.RIGHT_ALIGNMENT);
        lblLine3.setAlignmentX(Component.RIGHT_ALIGNMENT);
        pnlRightLines.add(line1);
        pnlRightLines.add(lblLine2);
        pnlRightLines.add(lblLine3);

        right.add(pnlRightLines, BorderLayout.CENTER);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    /** Hàm `buildLeftSidebar`: tạo `Left` thanh bên. */
    private JPanel buildLeftSidebar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setPreferredSize(new Dimension(110, 600));
        bar.setBackground(new Color(240, 243, 248));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(205, 215, 226)));

        // Tiêu đề: Bệnh án điện tử
        JLabel lblTitle = new JLabel("Bệnh án điện tử", SwingConstants.CENTER);
        lblTitle.setFont(UngDungPhongChu.SMALL_BOLD);
        lblTitle.setForeground(new Color(30, 35, 45));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setBorder(new EmptyBorder(8, 4, 6, 4));

        bar.add(lblTitle);
        bar.add(Box.createVerticalStrut(4));

        // 4 nút thao tác chính
        bar.add(createSidebarButton("Ký Tài Liệu", new PenSignatureIcon(), true));
        bar.add(Box.createVerticalStrut(6));
        bar.add(createSidebarButton("Duyệt Tài Liệu", new ApproveCheckIcon(), false));
        bar.add(Box.createVerticalStrut(6));
        bar.add(createSidebarButton("Lưu Tài Liệu", new FloppyDiskMiniIcon(), true));
        bar.add(Box.createVerticalStrut(6));
        bar.add(createSidebarButton("Hủy Tài Liệu", new GreyCrossIcon(), false));

        bar.add(Box.createVerticalStrut(12));

        // Nhãn "Xuất file..."
        JLabel lblExport = new JLabel("Xuất file...");
        lblExport.setFont(UngDungPhongChu.SMALL);
        lblExport.setForeground(new Color(50, 60, 70));
        lblExport.setAlignmentX(Component.CENTER_ALIGNMENT);
        bar.add(lblExport);
        bar.add(Box.createVerticalStrut(4));

        // Cụm nút Xuất file: [PDF] và [☰ ▾]
        JPanel exportGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        exportGroup.setOpaque(false);
        exportGroup.setMaximumSize(new Dimension(100, 32));

        JButton btnPdf = new JButton("PDF", new PdfVectorIcon());
        btnPdf.setFont(new Font(UngDungPhongChu.FAMILY, Font.BOLD, 10));
        btnPdf.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnPdf.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPdf.setPreferredSize(new Dimension(54, 34));
        btnPdf.setBackground(Color.WHITE);
        btnPdf.setFocusPainted(false);
        btnPdf.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPdf.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        btnPdf.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Đang xuất phiếu chỉ định ra tệp PDF:\n" +
                    "▶ Tệp: PhieuChiDinh_CanLamSang_" + patientCode + ".pdf",
                    "Xuất PDF", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnMore = new JButton("☰ ▾");
        btnMore.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnMore.setPreferredSize(new Dimension(36, 34));
        btnMore.setBackground(Color.WHITE);
        btnMore.setFocusPainted(false);
        btnMore.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMore.setBorder(new LineBorder(new Color(185, 195, 210), 1));

        exportGroup.add(btnPdf);
        exportGroup.add(btnMore);
        bar.add(exportGroup);

        bar.add(Box.createVerticalGlue());

        // Chân trang sidebar: Dòng chữ đỏ "Chưa ký 1" khớp 100% media_1793380792703_8babf04f.jpg
        JLabel lblStatus = new JLabel("Chưa ký 1", SwingConstants.CENTER);
        lblStatus.setFont(UngDungPhongChu.SMALL_BOLD);
        lblStatus.setForeground(new Color(205, 30, 30));
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblStatus.setBorder(new EmptyBorder(6, 4, 12, 4));

        bar.add(lblStatus);

        return bar;
    }

    /** Hàm `createSidebarButton`: tạo thanh bên nút. */
    private JButton createSidebarButton(String text, Icon icon, boolean isEnabled) {
        JButton btn = new JButton(text, icon);
        btn.setFont(UngDungPhongChu.SMALL_BOLD);
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setIconTextGap(3);
        btn.setPreferredSize(new Dimension(98, 44));
        btn.setMaximumSize(new Dimension(98, 44));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setEnabled(isEnabled);
        btn.setCursor(new Cursor(isEnabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));

        if (isEnabled) {
            btn.setBackground(Color.WHITE);
            btn.setForeground(new Color(30, 35, 45));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(185, 195, 210), 1),
                    new EmptyBorder(2, 4, 2, 4)
            ));
        } else {
            btn.setBackground(new Color(245, 247, 250));
            btn.setForeground(new Color(155, 165, 175));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(215, 222, 230), 1),
                    new EmptyBorder(2, 4, 2, 4)
            ));
        }
        return btn;
    }

    /**
     * Khung giấy A4 mô phỏng bản in "PHIẾU CHỈ ĐỊNH CẬN LÂM SÀNG"
     */
    private class ClinicalOrderSheet extends JPanel {

        private final int PAGE_W = 750;
        private final int PAGE_H = 640;

        public ClinicalOrderSheet() {
            setOpaque(true);
            setBackground(new Color(105, 115, 128));
            setPreferredSize(new Dimension(PAGE_W + 60, PAGE_H + 50));
        }

        /** Hàm `paintComponent`: `paint` thành phần. Gốc khai báo tại `JPanel#paintComponent`. */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int x = Math.max(25, (getWidth() - PAGE_W) / 2);
            int y = 20;

            // Bóng đổ (Drop Shadow)
            g2.setColor(new Color(60, 68, 78, 140));
            g2.fillRect(x + 5, y + 5, PAGE_W, PAGE_H);
            g2.setColor(new Color(80, 88, 98, 100));
            g2.fillRect(x + 2, y + 2, PAGE_W, PAGE_H);

            // Nền giấy trắng
            g2.setColor(Color.WHITE);
            g2.fillRect(x, y, PAGE_W, PAGE_H);
            g2.setColor(new Color(200, 205, 215));
            g2.drawRect(x, y, PAGE_W - 1, PAGE_H - 1);

            // --- 1. HEADER (LOGO + BỆNH VIỆN SẢN NHI + BARCODE) ---
            int padX = x + 35;
            int topY = y + 25;

            // Logo Bệnh Viện Sản Nhi (tròn vàng có tâm đỏ)
            paintSanNhiLogo(g2, padX, topY, 54);

            // Tên bệnh viện
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Times New Roman", Font.BOLD, 13));
            g2.drawString("BỆNH VIỆN SẢN NHI", padX + 115, topY + 22);
            g2.setFont(new Font("Times New Roman", Font.PLAIN, 11));
            g2.drawString("Đường Bình Than, Võ Cường, TP. Bắc Ninh", padX + 85, topY + 38);

            // Barcode bên phải: PID: 22001324 / Barcode / Số BA: 22000063
            int barRightX = x + PAGE_W - 170;
            g2.setFont(new Font("Times New Roman", Font.PLAIN, 10));
            g2.drawString("PID: " + pidCode, barRightX + 22, topY + 12);
            paintBarcode(g2, barRightX, topY + 17, 130, 22);
            g2.drawString("Số BA: " + medicalRecordCode, barRightX + 2, topY + 52);

            // --- 2. TIÊU ĐỀ: PHIẾU CHỈ ĐỊNH CẬN LÂM SÀNG ---
            int curY = topY + 80;
            g2.setFont(new Font("Times New Roman", Font.BOLD, 15));
            String title = "PHIẾU CHỈ ĐỊNH CẬN LÂM SÀNG";
            FontMetrics fmt = g2.getFontMetrics();
            g2.drawString(title, x + (PAGE_W - fmt.stringWidth(title)) / 2, curY);

            // --- 3. THÔNG TIN HÀNH CHÍNH BỆNH NHÂN ---
            curY += 26;
            g2.setFont(new Font("Times New Roman", Font.PLAIN, 11));
            int lineH = 18;

            // Dòng 1: Họ tên, NS, Giới tính
            g2.drawString("- Họ tên người bệnh: " + patientName, padX, curY);
            g2.drawString("NS: " + patientBirthYear + " (" + patientAge + " tuổi)", padX + 230, curY);
            g2.drawString("Giới tính: " + patientGender, padX + 370, curY);

            // Dòng 2: Địa chỉ
            curY += lineH;
            g2.drawString("- Địa chỉ: " + patientAddress, padX, curY);

            // Dòng 3: Đối tượng
            curY += lineH;
            g2.drawString("- Đối tượng: Bảo hiểm", padX, curY);

            // Dòng 4: Số thẻ BHYT, Hạn thẻ
            curY += lineH;
            g2.drawString("- Số thẻ BHYT: " + patientBhyt, padX, curY);
            g2.drawString("- Hạn thẻ đến: " + patientExpiry, padX + 250, curY);

            // Dòng 5: Khoa phòng, Phòng giường
            curY += lineH;
            g2.drawString("- Khoa phòng: " + patientDepartment, padX, curY);
            g2.drawString("Phòng Giường: /", padX + 200, curY);

            // Dòng 6: Chẩn đoán
            curY += lineH;
            g2.drawString("- Chẩn đoán: " + patientDiagnosis, padX, curY);

            // Dòng 7: Chẩn đoán kèm theo
            curY += lineH;
            g2.drawString("- Chẩn đoán kèm theo: --", padX, curY);

            // --- 4. BẢNG DỊCH VỤ CHỈ ĐỊNH ---
            curY += 16;
            int tblX = padX;
            int tblW = PAGE_W - 70;

            int[] colW = {290, 48, 50, 150, 62};
            // Total = 290 + 48 + 50 + 150 + 62 = 600

            // Header bảng
            int headerH = 34;
            g2.setColor(Color.BLACK);
            g2.drawRect(tblX, curY, tblW, headerH);

            int cx = tblX;
            for (int i = 0; i < colW.length - 1; i++) {
                cx += colW[i];
                g2.drawLine(cx, curY, cx, curY + headerH);
            }

            g2.setFont(new Font("Times New Roman", Font.BOLD, 10));
            g2.drawString("Tên dịch vụ", tblX + 110, curY + 20);

            g2.drawString("Đơn vị", tblX + colW[0] + 6, curY + 14);
            g2.drawString("tính", tblX + colW[0] + 12, curY + 26);

            g2.drawString("Số", tblX + colW[0] + colW[1] + 16, curY + 14);
            g2.drawString("lượng", tblX + colW[0] + colW[1] + 10, curY + 26);

            g2.drawString("Ghi chú", tblX + colW[0] + colW[1] + colW[2] + 55, curY + 20);

            g2.drawString("Đã thực", tblX + colW[0] + colW[1] + colW[2] + colW[3] + 10, curY + 14);
            g2.drawString("hiện", tblX + colW[0] + colW[1] + colW[2] + colW[3] + 18, curY + 26);

            curY += headerH;

            // --- NHÓM I: PHÒNG LẤY MẪU BỆNH PHẨM ---
            int grpH = 22;
            g2.drawRect(tblX, curY, tblW, grpH);
            g2.setFont(new Font("Times New Roman", Font.BOLD, 10));
            g2.drawString("I. PHÒNG LẤY MẪU BỆNH PHẨM (Mở rộng...TT | 1 |, Barcode | 17190 | [ Tầng 2-Khoa xét nghiệm ]", tblX + 5, curY + 15);
            curY += grpH;

            // Phân nhóm con: Tế bào
            int subH = 18;
            g2.drawRect(tblX, curY, tblW, subH);
            g2.setFont(new Font("Times New Roman", Font.ITALIC, 10));
            g2.drawString("Tế bào", tblX + 8, curY + 13);
            curY += subH;

            // Dịch vụ 1.1: Xét nghiệm các loại dịch, nhuộm...
            curY = paintServiceRow(g2, tblX, curY, tblW, colW, 28,
                    "1. Xét nghiệm các loại dịch, nhuộm và chẩn đoán tế bào học", "Lần", "1", "", "");

            // Dịch vụ 1.2: Nhuộm Periodic Acid Schiff (PAS)
            curY = paintServiceRow(g2, tblX, curY, tblW, colW, 36,
                    "2. Xét nghiệm và chẩn đoán tế bào học bằng phương pháp nhuộm Periodic Acid Schiff (PAS)", "Lần", "1", "", "");

            // --- NHÓM II: ĐẾN PHÒNG CHẨN ĐOÁN HÌNH ẢNH ---
            g2.drawRect(tblX, curY, tblW, grpH);
            g2.setFont(new Font("Times New Roman", Font.BOLD, 10));
            g2.drawString("II. Đến phòng [ DT CHẨN ĐOÁN HÌNH ẢNH ] để lấy STT, ID | 2609 | [ Tầng 1-Khu Nhà kính... tiếp CDHA )", tblX + 5, curY + 15);
            curY += grpH;

            // Phân nhóm con: X-quang
            g2.drawRect(tblX, curY, tblW, subH);
            g2.setFont(new Font("Times New Roman", Font.ITALIC, 10));
            g2.drawString("X-quang", tblX + 8, curY + 13);
            curY += subH;

            // Dịch vụ 2.1: Chụp Xquang Hirtz
            curY = paintServiceRow(g2, tblX, curY, tblW, colW, 26,
                    "1. Chụp Xquang Hirtz [số hóa 1 phim] [ #ACC : 4135 ]", "Lần", "1", "", "");

            // Dịch vụ 2.2: Chụp Xquang xương chính mũi nghiêng hoặc tiếp tuyến
            curY = paintServiceRow(g2, tblX, curY, tblW, colW, 34,
                    "2. Chụp Xquang xương chính mũi nghiêng hoặc tiếp tuyến [số hóa 1 phim] [ #ACC : 4136 ]", "Lần", "1", "", "");

            // --- 5. KHÚC CUỐI: NGÀY GIỜ VÀ CHỮ KÝ BÁC SĨ CHỈ ĐỊNH ---
            curY += 20;
            int signX = tblX + tblW - 250;

            g2.setFont(new Font("Times New Roman", Font.ITALIC, 11));
            g2.drawString(orderDateTime, signX, curY);

            curY += 18;
            g2.setFont(new Font("Times New Roman", Font.BOLD, 12));
            g2.drawString("BÁC SĨ CHỈ ĐỊNH", signX + 45, curY);

            g2.dispose();
        }

        /** Hàm `paintServiceRow`: `paint` dịch vụ `Row`. */
        private int paintServiceRow(Graphics2D g2, int x, int y, int w, int[] colW, int rowH,
                                    String name, String unit, String qty, String note, String done) {
            g2.setColor(Color.BLACK);
            g2.drawRect(x, y, w, rowH);

            int cx = x;
            for (int i = 0; i < colW.length - 1; i++) {
                cx += colW[i];
                g2.drawLine(cx, y, cx, y + rowH);
            }

            g2.setFont(new Font("Times New Roman", Font.PLAIN, 10));

            // Wrap text nếu tên dài
            if (name.length() > 60) {
                String l1 = name.substring(0, 52);
                String l2 = name.substring(52).trim();
                g2.drawString(l1, x + 6, y + 14);
                g2.drawString(l2, x + 18, y + 27);
            } else {
                g2.drawString(name, x + 6, y + 16);
            }

            // Đơn vị tính
            g2.drawString(unit, x + colW[0] + 14, y + 16);

            // Số lượng
            g2.drawString(qty, x + colW[0] + colW[1] + 20, y + 16);

            // Ghi chú
            if (note != null && !note.isEmpty()) {
                g2.drawString(note, x + colW[0] + colW[1] + colW[2] + 6, y + 16);
            }

            // Đã thực hiện
            if (done != null && !done.isEmpty()) {
                g2.drawString(done, x + colW[0] + colW[1] + colW[2] + colW[3] + 6, y + 16);
            }

            return y + rowH;
        }

        private void paintSanNhiLogo(Graphics2D g2, int x, int y, int size) {
            // Vòng tròn vàng ngoài
            g2.setColor(new Color(245, 195, 30));
            g2.setStroke(new BasicStroke(3.0f));
            g2.drawOval(x, y, size, size);

            g2.setColor(new Color(255, 245, 205));
            g2.fillOval(x + 2, y + 2, size - 4, size - 4);

            // Dòng chữ cong hoặc text viền
            g2.setColor(new Color(175, 40, 20));
            g2.setFont(new Font("Arial", Font.BOLD, 6));
            g2.drawString("BỆNH VIỆN SẢN NHI", x + 5, y + 11);

            // Biểu tượng trái tim đỏ tâm
            int cx = x + size / 2;
            int cy = y + size / 2 + 1;
            g2.setColor(new Color(210, 35, 30));
            g2.fillRoundRect(cx - 10, cy - 8, 20, 16, 6, 6);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 5));
            g2.drawString("HÀ NỘI", cx - 8, cy + 2);
        }

        private void paintBarcode(Graphics2D g2, int x, int y, int w, int h) {
            g2.setColor(Color.BLACK);
            int[] bars = {2, 1, 3, 1, 2, 1, 1, 3, 2, 1, 2, 1, 3, 1, 1, 2, 2, 1, 3, 1, 2, 1, 1, 3, 2, 1, 2, 1, 1, 3, 1, 2};
            int cx = x;
            boolean draw = true;
            for (int bw : bars) {
                if (draw) {
                    g2.fillRect(cx, y, bw, h);
                }
                cx += bw + 1;
                draw = !draw;
                if (cx >= x + w) break;
            }
        }
    }

    // =========================================================================
    // VECTOR ICONS CHO TOOLBAR & SIDEBAR
    // =========================================================================

    private static class PrinterVectorIcon implements Icon {
        private final int width, height;
        public PrinterVectorIcon(int w, int h) { this.width = w; this.height = h; }
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return width; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return height; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(60, 65, 75));
            g2.fillRect(x + 3, y + 1, 12, 4);
            g2.fillRoundRect(x + 1, y + 5, 16, 8, 2, 2);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 3, y + 9, 12, 6);
            g2.setColor(new Color(90, 95, 105));
            g2.drawRect(x + 3, y + 9, 12, 6);
            g2.dispose();
        }
    }

    private static class CheckmarkMiniIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 14; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(40, 160, 75));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 7, x + 5, y + 11);
            g2.drawLine(x + 5, y + 11, x + 12, y + 3);
            g2.dispose();
        }
    }

    private static class PenSignatureIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 18; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 18; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(235, 180, 50));
            g2.fillRect(x + 2, y + 2, 10, 14);
            g2.setColor(new Color(60, 70, 85));
            g2.drawRect(x + 2, y + 2, 10, 14);
            g2.setColor(new Color(65, 115, 215));
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(x + 15, y + 3, x + 8, y + 13);
            g2.dispose();
        }
    }

    private static class ApproveCheckIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 18; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 18; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(175, 185, 195));
            g2.drawRect(x + 2, y + 2, 14, 14);
            g2.drawLine(x + 4, y + 9, x + 8, y + 13);
            g2.drawLine(x + 8, y + 13, x + 14, y + 5);
            g2.dispose();
        }
    }

    private static class FloppyDiskMiniIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 18; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 18; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(125, 45, 140));
            g2.fillRoundRect(x + 2, y + 2, 14, 14, 2, 2);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 5, y + 3, 8, 5);
            g2.fillRect(x + 4, y + 10, 10, 5);
            g2.dispose();
        }
    }

    private static class GreyCrossIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 18; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 18; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(170, 180, 190));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawLine(x + 4, y + 4, x + 14, y + 14);
            g2.drawLine(x + 14, y + 4, x + 4, y + 14);
            g2.dispose();
        }
    }

    private static class PdfVectorIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 18; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(210, 45, 45));
            g2.fillRoundRect(x + 2, y + 1, 14, 14, 2, 2);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 8));
            g2.drawString("PDF", x + 3, y + 11);
            g2.dispose();
        }
    }
}
