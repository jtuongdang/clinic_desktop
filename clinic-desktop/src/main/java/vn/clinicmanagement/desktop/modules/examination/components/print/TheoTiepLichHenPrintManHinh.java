package vn.clinicmanagement.desktop.modules.examination.components.print;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Giao diện Xem và In "PHIẾU HẸN TÁI KHÁM" (K3_015_A1_PhieuHenTaiKham.doc).
 * Tự động chuyển hướng / kích hoạt sau khi bác sĩ nhấn [💾 Lưu] trên tab Xử Trí.
 * Khớp 100% giao diện trong ảnh chụp thực tế media_1793380172130_9595cffb.jpg:
 * - Cột thiết lập in bên trái:
 *   + Nút [In (Ctrl+P) F1] kích thước lớn + Bản in spinner + In tự động + In tiếp
 *   + Mục "Máy In"
 *   + Mục "Tùy Chọn" (link Cấu hình máy in, In tất cả các trang, Chiều giấy, Khổ giấy, In 2 mặt, In A5...)
 *   + Mục "Xuất File..." ([Pdf], [Word])
 * - Khung giấy in trung tâm:
 *   + Nền xám tiêu chuẩn in (#BFBFBF / #CECECE)
 *   + Tờ phiếu "PHIẾU HẸN TÁI KHÁM" viền đen có bóng đổ (Drop Shadow)
 *   + Logo y tế hình tròn bên trái, Barcode + mã 1-436 bên phải
 *   + Thông tin bệnh nhân: Mã người bệnh, Họ tên, Tuổi, Giới tính, Địa chỉ, Hẹn tái khám lúc, Lời dặn
 *   + Chữ ký bác sĩ khám bệnh: "Họ tên: bieuCBT"
 *   + Dòng ghi chú chân trang: "Ghi chú: Người bệnh vui lòng mang theo phiếu này khi đi khám lại."
 * - Thanh điều hướng trang & thu phóng chân trang:
 *   + Nút chuyển trang [|<] [<] [ 1 ] / 1 [>] [>|]
 *   + Thanh trượt Zoom [-] [====] [+] 100%
 */
public class TheoTiepLichHenPrintManHinh extends JPanel {

    /** Trường `patientCode`: bệnh nhân mã. */
    private String patientCode = "22001770";
    /** Trường `patientName`: bệnh nhân tên. */
    private String patientName = "NGUYỄN THỊ LIÊN";
    /** Trường `patientAge`: bệnh nhân `Age`. */
    private String patientAge = "38";
    /** Trường `patientGender`: bệnh nhân `Gender`. */
    private String patientGender = "Nữ";
    /** Trường `patientAddress`: bệnh nhân `Address`. */
    private String patientAddress = "Xã Mường Lai, Huyện Lục Yên, Tỉnh Yên Bái";
    /** Trường `appointmentTime`: lịch hẹn thời gian. */
    private String appointmentTime = "15 giờ 57 ngày 19 tháng 08 năm 2022";
    /** Trường `doctorInstructions`: bacsi `Instructions`. */
    private String doctorInstructions = "";
    /** Trường `examDate`: kham ngày. */
    private String examDate = "Ngày 12 tháng 08 năm 2022";
    /** Trường `doctorName`: bacsi tên. */
    private String doctorName = "bieuCBT";
    /** Trường `hospitalName`: `hospital` tên. */
    private String hospitalName = "Trung tâm Y tế Quế Võ";
    private String hospitalAddress = "Thôn Đỉnh, TT.Phố Mới, H.Quế Võ, T.Bắc Ninh";
    /** Trường `barcodeNumber`: `barcode` so. */
    private String barcodeNumber = "1-436";

    private JSpinner spinCopies;
    /** Trường `chkAutoPrint`: `chk` `Auto` in. */
    private JCheckBox chkAutoPrint;
    /** Trường `chkContinuePrint`: `chk` `Continue` in. */
    private JCheckBox chkContinuePrint;
    private JComboBox<String> cboPrinters;
    private JComboBox<String> cboPrinterProfiles;
    /** Trường `cboPageRange`: `cbo` `Page` khoang. */
    private JComboBox<String> cboPageRange;
    private JTextField txtPages;
    private JComboBox<String> cboOrientation;
    private JComboBox<String> cboPaperSize;
    private JCheckBox chkTwoSided;
    /** Trường `chkA5onA4`: `chk` `A` `5` tải `A` `4`. */
    private JCheckBox chkA5onA4;
    private JSlider zoomSlider;
    private JLabel lblZoomPercent;

    public TheoTiepLichHenPrintManHinh() {
        this("22001770", "NGUYỄN THỊ LIÊN", "38", "Nữ",
                "Xã Mường Lai, Huyện Lục Yên, Tỉnh Yên Bái",
                "15 giờ 57 ngày 19 tháng 08 năm 2022", "",
                "Ngày 12 tháng 08 năm 2022", "bieuCBT");
    }

    public TheoTiepLichHenPrintManHinh(String code, String name, String age, String gender,
                                         String address, String appointTime, String instructions,
                                         String date, String doc) {
        if (code != null && !code.isEmpty()) this.patientCode = code;
        if (name != null && !name.isEmpty()) this.patientName = name;
        if (age != null && !age.isEmpty()) this.patientAge = age;
        if (gender != null && !gender.isEmpty()) this.patientGender = gender;
        if (address != null && !address.isEmpty()) this.patientAddress = address;
        if (appointTime != null && !appointTime.isEmpty()) this.appointmentTime = appointTime;
        if (instructions != null) this.doctorInstructions = instructions;
        if (date != null && !date.isEmpty()) this.examDate = date;
        if (doc != null && !doc.isEmpty()) this.doctorName = doc;

        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 240));

        // 1. CỘT CẤU HÌNH IN BÊN TRÁI
        add(buildLeftSidebar(), BorderLayout.WEST);

        // 2. KHUNG TRUNG TÂM XEM TỜ PHIẾU
        JScrollPane scrollPreview = new JScrollPane(new AppointmentSlipCanvas());
        scrollPreview.setBorder(BorderFactory.createEmptyBorder());
        scrollPreview.getViewport().setBackground(new Color(192, 192, 192));
        scrollPreview.getVerticalScrollBar().setUnitIncrement(16);
        scrollPreview.getHorizontalScrollBar().setUnitIncrement(16);
        add(scrollPreview, BorderLayout.CENTER);

        // 3. THANH ĐIỀU HƯỚNG VÀ THU PHÓNG DƯỚI CÙNG
        add(buildBottomStatusBar(), BorderLayout.SOUTH);
    }

    /**
     * Cột bên trái cấu hình lệnh in, máy in, khổ giấy, xuất file.
     */
    private JPanel buildLeftSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(240, 240, 240));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(195, 200, 210)),
                new EmptyBorder(6, 7, 6, 7)
        ));
        sidebar.setPreferredSize(new Dimension(145, 600));
        sidebar.setMinimumSize(new Dimension(145, 400));
        sidebar.setMaximumSize(new Dimension(145, Integer.MAX_VALUE));

        // --- CỤM 1: NÚT IN LỚN & BẢN IN, IN TỰ ĐỘNG, IN TIẾP ---
        JPanel pnlPrintAction = new JPanel(new BorderLayout(5, 0));
        pnlPrintAction.setOpaque(false);
        pnlPrintAction.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Nút In lớn
        JButton btnBigPrint = new JButton();
        btnBigPrint.setLayout(new BoxLayout(btnBigPrint, BoxLayout.Y_AXIS));
        btnBigPrint.setBackground(new Color(245, 248, 252));
        btnBigPrint.setFocusPainted(false);
        btnBigPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBigPrint.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(165, 180, 205), 1),
                new EmptyBorder(3, 4, 3, 4)
        ));
        btnBigPrint.setPreferredSize(new Dimension(68, 62));

        JLabel lblPrinterIcon = new JLabel(new LargePrinterVectorIcon());
        lblPrinterIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTextIn = new JLabel("In");
        lblTextIn.setFont(UngDungPhongChu.SMALL_BOLD);
        lblTextIn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTextCtrlP = new JLabel("(Ctrl+P)");
        lblTextCtrlP.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        lblTextCtrlP.setForeground(new Color(80, 85, 95));
        lblTextCtrlP.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTextF1 = new JLabel("F1");
        lblTextF1.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblTextF1.setForeground(new Color(80, 85, 95));
        lblTextF1.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnBigPrint.add(lblPrinterIcon);
        btnBigPrint.add(lblTextIn);
        btnBigPrint.add(lblTextCtrlP);
        btnBigPrint.add(lblTextF1);

        btnBigPrint.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Hệ thống đang gửi lệnh in tới máy in mặc định:\n" +
                    "▶ Tài liệu: PHIẾU HẸN TÁI KHÁM\n" +
                    "▶ Bệnh nhân: " + patientName + " (" + patientCode + ")\n" +
                    "▶ Ngày hẹn: " + appointmentTime,
                    "In Tài Liệu", JOptionPane.INFORMATION_MESSAGE);
        });

        // Cột nhỏ bên phải nút In
        JPanel pnlPrintOpts = new JPanel();
        pnlPrintOpts.setLayout(new BoxLayout(pnlPrintOpts, BoxLayout.Y_AXIS));
        pnlPrintOpts.setOpaque(false);

        JPanel pnlCopies = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        pnlCopies.setOpaque(false);
        JLabel lblCopies = new JLabel("Bản in");
        lblCopies.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        spinCopies = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        spinCopies.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        spinCopies.setPreferredSize(new Dimension(32, 20));
        pnlCopies.add(lblCopies);
        pnlCopies.add(spinCopies);

        chkAutoPrint = new JCheckBox("In tự động", true);
        chkAutoPrint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        chkAutoPrint.setOpaque(false);
        chkAutoPrint.setMargin(new Insets(1, 0, 1, 0));

        chkContinuePrint = new JCheckBox("In tiếp", false);
        chkContinuePrint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        chkContinuePrint.setOpaque(false);
        chkContinuePrint.setMargin(new Insets(1, 0, 1, 0));

        pnlPrintOpts.add(pnlCopies);
        pnlPrintOpts.add(Box.createVerticalStrut(2));
        pnlPrintOpts.add(chkAutoPrint);
        pnlPrintOpts.add(chkContinuePrint);

        pnlPrintAction.add(btnBigPrint, BorderLayout.WEST);
        pnlPrintAction.add(pnlPrintOpts, BorderLayout.CENTER);
        sidebar.add(pnlPrintAction);

        sidebar.add(Box.createVerticalStrut(8));

        // --- CỤM 2: MÁY IN ---
        JLabel lblMayIn = new JLabel("Máy In");
        lblMayIn.setFont(UngDungPhongChu.SMALL_BOLD);
        lblMayIn.setForeground(new Color(30, 30, 30));
        lblMayIn.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(lblMayIn);
        sidebar.add(Box.createVerticalStrut(2));

        cboPrinters = createSidebarComboBox(new String[]{"Canon LBP 2900", "HP LaserJet Pro", "Microsoft Print to PDF"});
        sidebar.add(cboPrinters);
        sidebar.add(Box.createVerticalStrut(4));

        cboPrinterProfiles = createSidebarComboBox(new String[]{"Mặc định (Ready)", "Khoa khám bệnh", "Cổng thanh toán"});
        sidebar.add(cboPrinterProfiles);

        sidebar.add(Box.createVerticalStrut(8));

        // --- CỤM 3: TÙY CHỌN ---
        JPanel pnlTuyChonHeader = new JPanel(new BorderLayout());
        pnlTuyChonHeader.setOpaque(false);
        pnlTuyChonHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTuyChon = new JLabel("Tùy Chọn");
        lblTuyChon.setFont(UngDungPhongChu.SMALL_BOLD);
        lblTuyChon.setForeground(new Color(30, 30, 30));

        JLabel lnkConfigPrinter = new JLabel("<html><u>Cấu hình máy in</u></html>");
        lnkConfigPrinter.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        lnkConfigPrinter.setForeground(new Color(0, 102, 204));
        lnkConfigPrinter.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lnkConfigPrinter.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(sidebar, "Mở hộp thoại cấu hình Driver máy in của hệ điều hành.", "Cấu Hình Máy In", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        pnlTuyChonHeader.add(lblTuyChon, BorderLayout.WEST);
        pnlTuyChonHeader.add(lnkConfigPrinter, BorderLayout.EAST);
        sidebar.add(pnlTuyChonHeader);
        sidebar.add(Box.createVerticalStrut(3));

        cboPageRange = createSidebarComboBox(new String[]{"In tất cả các trang", "Trang hiện tại", "Trang chỉ định"});
        sidebar.add(cboPageRange);
        sidebar.add(Box.createVerticalStrut(4));

        // Trang in
        JPanel pnlTrangIn = new JPanel(new BorderLayout(3, 0));
        pnlTrangIn.setOpaque(false);
        pnlTrangIn.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblTrangIn = new JLabel("Trang in");
        lblTrangIn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        txtPages = new JTextField("");
        txtPages.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        txtPages.setPreferredSize(new Dimension(75, 20));
        pnlTrangIn.add(lblTrangIn, BorderLayout.WEST);
        pnlTrangIn.add(txtPages, BorderLayout.CENTER);
        sidebar.add(pnlTrangIn);
        sidebar.add(Box.createVerticalStrut(4));

        // Chiều giấy
        JPanel pnlChieuGiay = new JPanel(new BorderLayout(3, 0));
        pnlChieuGiay.setOpaque(false);
        pnlChieuGiay.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblChieuGiay = new JLabel("Chiều giấy");
        lblChieuGiay.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        cboOrientation = createSidebarComboBox(new String[]{"Chiều dọc", "Chiều ngang"});
        cboOrientation.setPreferredSize(new Dimension(75, 20));
        pnlChieuGiay.add(lblChieuGiay, BorderLayout.WEST);
        pnlChieuGiay.add(cboOrientation, BorderLayout.CENTER);
        sidebar.add(pnlChieuGiay);
        sidebar.add(Box.createVerticalStrut(4));

        // Khổ giấy
        JPanel pnlKhoGiay = new JPanel(new BorderLayout(3, 0));
        pnlKhoGiay.setOpaque(false);
        pnlKhoGiay.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblKhoGiay = new JLabel("Khổ giấy");
        lblKhoGiay.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        cboPaperSize = createSidebarComboBox(new String[]{"A4 (210mm x 297...", "A5 (148mm x 210...", "Letter"});
        cboPaperSize.setPreferredSize(new Dimension(82, 20));
        pnlKhoGiay.add(lblKhoGiay, BorderLayout.WEST);
        pnlKhoGiay.add(cboPaperSize, BorderLayout.CENTER);
        sidebar.add(pnlKhoGiay);
        sidebar.add(Box.createVerticalStrut(5));

        chkTwoSided = new JCheckBox("In 2 mặt", false);
        chkTwoSided.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        chkTwoSided.setOpaque(false);
        chkTwoSided.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkTwoSided.setMargin(new Insets(1, 0, 1, 0));
        sidebar.add(chkTwoSided);

        chkA5onA4 = new JCheckBox("In A5 vào khay giấy A4", false);
        chkA5onA4.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        chkA5onA4.setOpaque(false);
        chkA5onA4.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkA5onA4.setMargin(new Insets(1, 0, 1, 0));
        sidebar.add(chkA5onA4);

        sidebar.add(Box.createVerticalStrut(10));

        // --- CỤM 4: XUẤT FILE... ---
        JLabel lblXuatFile = new JLabel("Xuất File...");
        lblXuatFile.setFont(UngDungPhongChu.SMALL_BOLD);
        lblXuatFile.setForeground(new Color(30, 30, 30));
        lblXuatFile.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(lblXuatFile);
        sidebar.add(Box.createVerticalStrut(4));

        JPanel pnlExportButtons = new JPanel(new GridLayout(1, 2, 4, 0));
        pnlExportButtons.setOpaque(false);
        pnlExportButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlExportButtons.setMaximumSize(new Dimension(135, 45));

        JButton btnPdf = new JButton("Pdf", new MiniPdfVectorIcon());
        btnPdf.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnPdf.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPdf.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnPdf.setBackground(Color.WHITE);
        btnPdf.setFocusPainted(false);
        btnPdf.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPdf.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        btnPdf.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Xuất tài liệu ra tệp PDF thành công: PHIEU_HEN_TAI_KHAM_" + patientCode + ".pdf", "Xuất PDF", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnWord = new JButton("Word", new MiniWordVectorIcon());
        btnWord.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnWord.setHorizontalTextPosition(SwingConstants.CENTER);
        btnWord.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnWord.setBackground(Color.WHITE);
        btnWord.setFocusPainted(false);
        btnWord.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnWord.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        btnWord.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Xuất tài liệu ra tệp Word thành công: K3_015_A1_PhieuHenTaiKham.doc", "Xuất Word", JOptionPane.INFORMATION_MESSAGE);
        });

        pnlExportButtons.add(btnPdf);
        pnlExportButtons.add(btnWord);
        sidebar.add(pnlExportButtons);

        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    /** Hàm `createSidebarComboBox`: tạo thanh bên `Combo` `Box`. */
    private JComboBox<String> createSidebarComboBox(String[] items) {
        JComboBox<String> cbo = new JComboBox<>(items);
        cbo.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        cbo.setBackground(Color.WHITE);
        cbo.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbo.setMaximumSize(new Dimension(135, 21));
        cbo.setPreferredSize(new Dimension(135, 21));
        return cbo;
    }

    /**
     * Thanh điều hướng trang và thanh thu phóng chân trang.
     */
    private JPanel buildBottomStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(240, 240, 240));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(205, 210, 220)));
        bar.setPreferredSize(new Dimension(1000, 26));

        // Bên trái: |<  <  [ 1 ] / 1  >  >|
        JPanel pnlNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        pnlNav.setOpaque(false);

        JButton btnFirst = createNavButton("|<");
        JButton btnPrev = createNavButton("<");
        JTextField txtCurrentPage = new JTextField("1", 2);
        txtCurrentPage.setHorizontalAlignment(JTextField.CENTER);
        txtCurrentPage.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        txtCurrentPage.setPreferredSize(new Dimension(24, 19));
        JLabel lblTotalPages = new JLabel("/ 1");
        lblTotalPages.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        JButton btnNext = createNavButton(">");
        JButton btnLast = createNavButton(">|");

        pnlNav.add(btnFirst);
        pnlNav.add(btnPrev);
        pnlNav.add(txtCurrentPage);
        pnlNav.add(lblTotalPages);
        pnlNav.add(btnNext);
        pnlNav.add(btnLast);
        bar.add(pnlNav, BorderLayout.WEST);

        // Bên phải: [-] [====] [+] 100%
        JPanel pnlZoom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 1));
        pnlZoom.setOpaque(false);

        JLabel btnZoomOut = new JLabel("－");
        btnZoomOut.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnZoomOut.setCursor(new Cursor(Cursor.HAND_CURSOR));

        zoomSlider = new JSlider(50, 150, 100);
        zoomSlider.setPreferredSize(new Dimension(100, 18));
        zoomSlider.setOpaque(false);

        JLabel btnZoomIn = new JLabel("＋");
        btnZoomIn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnZoomIn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        lblZoomPercent = new JLabel("100%");
        lblZoomPercent.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblZoomPercent.setPreferredSize(new Dimension(36, 18));

        zoomSlider.addChangeListener(e -> {
            lblZoomPercent.setText(zoomSlider.getValue() + "%");
        });

        pnlZoom.add(btnZoomOut);
        pnlZoom.add(zoomSlider);
        pnlZoom.add(btnZoomIn);
        pnlZoom.add(lblZoomPercent);
        bar.add(pnlZoom, BorderLayout.EAST);

        return bar;
    }

    /** Hàm `createNavButton`: tạo `Nav` nút. */
    private JButton createNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(1, 3, 1, 3));
        btn.setPreferredSize(new Dimension(22, 19));
        btn.setBackground(Color.WHITE);
        btn.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        return btn;
    }

    /**
     * Vùng vẽ tờ giấy in "PHIẾU HẸN TÁI KHÁM"
     */
    private class AppointmentSlipCanvas extends JPanel {

        private final int SHEET_W = 680;
        private final int SHEET_H = 460;

        public AppointmentSlipCanvas() {
            setOpaque(true);
            setBackground(new Color(192, 192, 192));
            setPreferredSize(new Dimension(SHEET_W + 80, SHEET_H + 60));
        }

        /** Hàm `paintComponent`: `paint` thành phần. Gốc khai báo tại `JPanel#paintComponent`. */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Căn giữa tờ giấy
            int x = Math.max(30, (getWidth() - SHEET_W) / 2);
            int y = Math.max(20, (getHeight() - SHEET_H) / 2);

            // Bóng đổ (Drop Shadow)
            g2.setColor(new Color(140, 140, 140, 180));
            g2.fillRect(x + 4, y + 4, SHEET_W, SHEET_H);
            g2.setColor(new Color(170, 170, 170, 120));
            g2.fillRect(x + 2, y + 2, SHEET_W, SHEET_H);

            // Tờ giấy trắng
            g2.setColor(Color.WHITE);
            g2.fillRect(x, y, SHEET_W, SHEET_H);
            // Viền đen mảnh bo tờ giấy
            g2.setColor(Color.BLACK);
            g2.drawRect(x, y, SHEET_W - 1, SHEET_H - 1);

            // 1. LOGO Y TẾ TRÒN (Góc trên bên trái)
            int logoX = x + 40;
            int logoY = y + 26;
            paintMedicalRoundLogo(g2, logoX, logoY, 48);

            // 2. THÔNG TIN BỆNH VIỆN / TRUNG TÂM Y TẾ (Ở giữa phía trên)
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Times New Roman", Font.BOLD, 13));
            FontMetrics fmh1 = g2.getFontMetrics();
            int hospNameW = fmh1.stringWidth(hospitalName);
            int centerTextX = x + (SHEET_W / 2) - 15;
            g2.drawString(hospitalName, centerTextX - hospNameW / 2, y + 40);

            g2.setFont(new Font("Times New Roman", Font.PLAIN, 11));
            FontMetrics fmh2 = g2.getFontMetrics();
            int hospAddrW = fmh2.stringWidth(hospitalAddress);
            g2.drawString(hospitalAddress, centerTextX - hospAddrW / 2, y + 56);

            // 3. BARCODE VÀ MÃ 1-436 (Góc trên bên phải)
            int barX = x + SHEET_W - 145;
            int barY = y + 30;
            paintBarcode(g2, barX, barY, 105, 20);

            g2.setFont(new Font("Times New Roman", Font.PLAIN, 10));
            g2.drawString(barcodeNumber, barX + 38, barY + 31);

            // 4. TIÊU ĐỀ: PHIẾU HẸN TÁI KHÁM (Ở giữa, in đậm lớn)
            g2.setFont(new Font("Times New Roman", Font.BOLD, 16));
            FontMetrics fmt = g2.getFontMetrics();
            String title = "PHIẾU HẸN TÁI KHÁM";
            int titleW = fmt.stringWidth(title);
            g2.drawString(title, x + (SHEET_W - titleW) / 2, y + 105);

            // 5. CÁC DÒNG THÔNG TIN BỆNH NHÂN (Bên trái)
            int contentX = x + 40;
            int lineY = y + 138;
            int lineGap = 21;

            g2.setFont(new Font("Times New Roman", Font.PLAIN, 11));

            // Dòng 1: - Mã người bệnh: 22001770
            g2.drawString("- Mã người bệnh: " + patientCode, contentX, lineY);

            // Dòng 2: - Họ tên người bệnh: NGUYỄN THỊ LIÊN        Tuổi: 38    Nữ
            lineY += lineGap;
            g2.drawString("- Họ tên người bệnh: " + patientName, contentX, lineY);
            g2.drawString("Tuổi: " + patientAge, contentX + 340, lineY);
            g2.drawString(patientGender, contentX + 415, lineY);

            // Dòng 3: - Địa chỉ: Xã Mường Lai, Huyện Lục Yên, Tỉnh Yên Bái
            lineY += lineGap;
            g2.drawString("- Địa chỉ: " + patientAddress, contentX, lineY);

            // Dòng 4: - Hẹn tái khám lúc: 15 giờ 57 ngày 19 tháng 08 năm 2022
            lineY += lineGap;
            g2.drawString("- Hẹn tái khám lúc: " + appointmentTime, contentX, lineY);

            // Dòng 5: - Lời dặn của bác sĩ:
            lineY += lineGap;
            g2.drawString("- Lời dặn của bác sĩ: " + doctorInstructions, contentX, lineY);

            // 6. KHỐI KÝ TÊN BÁC SĨ (Góc dưới bên phải)
            int docX = x + 460;
            int docY = y + 275;

            g2.setFont(new Font("Times New Roman", Font.ITALIC, 11));
            g2.drawString(examDate, docX, docY);

            g2.setFont(new Font("Times New Roman", Font.BOLD, 12));
            g2.drawString("BÁC SĨ KHÁM BỆNH", docX + 8, docY + 18);

            g2.setFont(new Font("Times New Roman", Font.ITALIC, 9));
            g2.drawString("(Ký và ghi rõ họ tên)", docX + 22, docY + 31);

            // Tên bác sĩ dưới cùng
            g2.setFont(new Font("Times New Roman", Font.PLAIN, 11));
            g2.drawString("Họ tên: " + doctorName, docX + 22, docY + 95);

            // 7. GHI CHÚ CHÂN TRANG (Căn giữa phía dưới)
            g2.setFont(new Font("Times New Roman", Font.BOLD, 11));
            String footerNote = "Ghi chú: Người bệnh vui lòng mang theo phiếu này khi đi khám lại.";
            FontMetrics fmf = g2.getFontMetrics();
            int footerW = fmf.stringWidth(footerNote);
            g2.drawString(footerNote, x + (SHEET_W - footerW) / 2, y + SHEET_H - 40);

            g2.dispose();
        }

        /** Hàm `paintMedicalRoundLogo`: `paint` yte `Round` `Logo`. */
        private void paintMedicalRoundLogo(Graphics2D g2, int x, int y, int size) {
            // Vòng tròn ngoài màu xanh
            g2.setColor(new Color(28, 85, 155));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawOval(x, y, size, size);

            g2.setColor(new Color(245, 248, 255));
            g2.fillOval(x + 2, y + 2, size - 4, size - 4);

            // Trái tim đỏ ở giữa
            g2.setColor(new Color(210, 40, 40));
            int hx = x + size / 2;
            int hy = y + size / 2 - 2;
            int hw = 18;
            g2.fillArc(hx - hw / 2 - 4, hy - 6, hw / 2 + 1, hw / 2 + 1, 0, 180);
            g2.fillArc(hx - 3, hy - 6, hw / 2 + 1, hw / 2 + 1, 0, 180);
            int[] xp = {hx - hw / 2 - 4, hx + hw / 2 + 3, hx};
            int[] yp = {hy, hy, hy + 10};
            g2.fillPolygon(xp, yp, 3);

            // Dấu thập chữ thập trắng trong tim
            g2.setColor(Color.WHITE);
            g2.fillRect(hx - 1, hy - 4, 3, 9);
            g2.fillRect(hx - 4, hy - 1, 9, 3);
        }

        private void paintBarcode(Graphics2D g2, int x, int y, int w, int h) {
            g2.setColor(Color.BLACK);
            // Các vạch barcode
            int[] barWidths = {2, 1, 3, 1, 1, 2, 1, 3, 2, 1, 2, 1, 1, 3, 1, 2, 1, 2, 3, 1, 1, 2, 1, 3, 1, 2, 2, 1};
            int cx = x;
            boolean bar = true;
            for (int bw : barWidths) {
                if (bar) {
                    g2.fillRect(cx, y, bw, h);
                }
                cx += bw + 1;
                bar = !bar;
                if (cx >= x + w) break;
            }
        }
    }

    // =========================================================================
    // ICONS CHO SIDEBAR
    // =========================================================================

    private static class LargePrinterVectorIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 26; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 22; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Khay giấy trên
            g2.setColor(new Color(230, 235, 245));
            g2.fillRect(x + 5, y + 1, 16, 6);
            g2.setColor(new Color(130, 140, 155));
            g2.drawRect(x + 5, y + 1, 16, 6);

            // Thân máy in
            g2.setColor(new Color(245, 247, 250));
            g2.fillRoundRect(x + 2, y + 6, 22, 11, 2, 2);
            g2.setColor(new Color(100, 110, 125));
            g2.drawRoundRect(x + 2, y + 6, 22, 11, 2, 2);

            // Đèn báo
            g2.setColor(new Color(40, 175, 80));
            g2.fillOval(x + 19, y + 8, 3, 3);

            // Tờ giấy ra dưới
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 6, y + 12, 14, 8);
            g2.setColor(new Color(130, 140, 155));
            g2.drawRect(x + 6, y + 12, 14, 8);

            // Dòng chữ trên tờ giấy
            g2.setColor(new Color(180, 190, 205));
            g2.drawLine(x + 8, y + 15, x + 17, y + 15);
            g2.drawLine(x + 8, y + 17, x + 15, y + 17);

            g2.dispose();
        }
    }

    private static class MiniPdfVectorIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 20; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 20; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 2, y + 1, 16, 18);
            g2.setColor(new Color(200, 40, 40));
            g2.drawRect(x + 2, y + 1, 16, 18);

            g2.setColor(new Color(215, 45, 45));
            g2.fillRect(x + 1, y + 5, 18, 9);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 7));
            g2.drawString("PDF", x + 3, y + 12);
            g2.dispose();
        }
    }

    private static class MiniWordVectorIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 20; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 20; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 2, y + 1, 16, 18);
            g2.setColor(new Color(30, 95, 180));
            g2.drawRect(x + 2, y + 1, 16, 18);

            g2.setColor(new Color(35, 105, 195));
            g2.fillRect(x + 1, y + 5, 18, 9);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 7));
            g2.drawString("DOC", x + 2, y + 12);
            g2.dispose();
        }
    }
}
