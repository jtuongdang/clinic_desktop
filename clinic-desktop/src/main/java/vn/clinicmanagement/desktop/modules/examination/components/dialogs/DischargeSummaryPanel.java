package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.desktop.modules.examination.components.tabs.TreatmentResolutionTabPanel;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Consumer;

/**
 * Giao diện màn hình: "EHC - Cập Nhật Thông Tin Ra Viện - Phiếu tóm tắt ra viện".
 * Tự động hiển thị khi chọn hình thức xử trí "Cấp toa cho về" trong tab Xử Trí.
 * Khớp 100% ảnh chụp thực tế media_1793377745349_a2277d12.jpg:
 * - Tiêu đề cửa sổ: ➕ EHC - Cập Nhật Thông Tin Ra Viện - Phiếu tóm tắt ra viện
 * - Hàng 1: Thời gian ra viện (23/08/2022 📅▾)
 * - Khung 2 cột x 3 hàng text areas:
 *   + Cột 1:
 *     1. Tình trạng bệnh nhân khi ra viện: (Nền vàng nhạt #FFFFE6, có con trỏ |)
 *     2. Hướng điều trị tiếp theo và chế độ dinh dưỡng, sinh hoạt:
 *     3. Lời dặn của bác sĩ:
 *   + Cột 2:
 *     1. Quá trình bệnh lý và diễn biến lâm sàng:
 *     2. Tóm tắt kết quả xét nghiệm:
 *     3. Lời dặn của bác sĩ:
 * - Hàng tái khám:
 *   + Thời gian hẹn tái khám (nếu có): [Chọn ngày 📅▾]
 *   + Phím tắt ngày nhanh: [2 ngày sau] [1 tuần sau] [2 tuần sau] [1 tháng sau]
 * - Nút chức năng góc phải dưới:
 *   + [✔ Lưu và In (Ctrl+P)]
 *   + [💾 Đóng (Esc)]
 */
public class DischargeSummaryPanel extends JPanel {

    private final Consumer<DischargeSummaryData> onSaveCallback;
    private final Runnable onCloseCallback;

    private JTextField txtThoiGianRaVien;
    private JTextArea txtTinhTrangRaVien;
    private JTextArea txtQuaTrinhBenhLy;
    private JTextArea txtHuongDieuTri;
    private JTextArea txtTomTatXetNghiem;
    private JTextArea txtLoiDanBacSiLeft;
    private JTextArea txtLoiDanBacSiRight;
    private JTextField txtHenTaiKham;

    private JButton btnSaveAndPrint;
    private JButton btnClose;

    public static class DischargeSummaryData {
        public String thoiGianRaVien;
        public String tinhTrangRaVien;
        public String quaTrinhBenhLy;
        public String huongDieuTri;
        public String tomTatXetNghiem;
        public String loiDanBacSiLeft;
        public String loiDanBacSiRight;
        public String henTaiKham;
    }

    public DischargeSummaryPanel(Consumer<DischargeSummaryData> onSaveCallback, Runnable onCloseCallback) {
        this.onSaveCallback = onSaveCallback;
        this.onCloseCallback = onCloseCallback;

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(240, 243, 248));
        setBorder(new LineBorder(new Color(160, 175, 195), 1));
        setPreferredSize(new Dimension(680, 520));

        // 1. THANH TIÊU ĐỀ WINDOWS EHC
        JPanel windowTitleBar = new JPanel(new BorderLayout());
        windowTitleBar.setBackground(Color.WHITE);
        windowTitleBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 220, 235)),
                new EmptyBorder(4, 8, 4, 6)
        ));

        JPanel leftTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftTitle.setBackground(Color.WHITE);
        JLabel lblIcon = new JLabel(new MedicalRedCrossIcon());
        JLabel lblTitle = new JLabel("EHC - Cập Nhật Thông Tin Ra Viện - Phiếu tóm tắt ra viện");
        lblTitle.setFont(AppFonts.SMALL_BOLD);
        lblTitle.setForeground(new Color(20, 25, 35));
        leftTitle.add(lblIcon);
        leftTitle.add(lblTitle);

        JButton btnTitleClose = new JButton("✕");
        btnTitleClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnTitleClose.setForeground(new Color(100, 110, 125));
        btnTitleClose.setBackground(Color.WHITE);
        btnTitleClose.setBorder(new EmptyBorder(2, 8, 2, 8));
        btnTitleClose.setFocusPainted(false);
        btnTitleClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTitleClose.addActionListener(e -> {
            if (onCloseCallback != null) onCloseCallback.run();
        });

        windowTitleBar.add(leftTitle, BorderLayout.WEST);
        windowTitleBar.add(btnTitleClose, BorderLayout.EAST);
        add(windowTitleBar, BorderLayout.NORTH);

        // 2. KHU VỰC NỘI DUNG CHÍNH
        JPanel contentPanel = new JPanel(new BorderLayout(0, 8));
        contentPanel.setBackground(new Color(240, 243, 248));
        contentPanel.setBorder(new EmptyBorder(8, 10, 8, 10));

        // 2a. Hàng 1: Thời gian ra viện
        JPanel pnlDischargeDate = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlDischargeDate.setBackground(new Color(240, 243, 248));

        JLabel lblThoiGian = new JLabel("Thời gian ra viện");
        lblThoiGian.setFont(AppFonts.SMALL);
        lblThoiGian.setForeground(new Color(25, 30, 40));

        JPanel dateInputContainer = new JPanel(new BorderLayout());
        dateInputContainer.setBackground(Color.WHITE);
        dateInputContainer.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        dateInputContainer.setPreferredSize(new Dimension(135, 24));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        txtThoiGianRaVien = new JTextField(sdf.format(new Date()));
        txtThoiGianRaVien.setFont(AppFonts.SMALL);
        txtThoiGianRaVien.setHorizontalAlignment(SwingConstants.CENTER);
        txtThoiGianRaVien.setBorder(new EmptyBorder(2, 4, 2, 2));

        JButton btnCalendarDischarge = new JButton(new CalendarDropIcon());
        btnCalendarDischarge.setPreferredSize(new Dimension(22, 22));
        btnCalendarDischarge.setBackground(new Color(245, 247, 250));
        btnCalendarDischarge.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(205, 214, 226)));
        btnCalendarDischarge.setFocusPainted(false);
        btnCalendarDischarge.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCalendarDischarge.addActionListener(e -> txtThoiGianRaVien.setText(sdf.format(new Date())));

        dateInputContainer.add(txtThoiGianRaVien, BorderLayout.CENTER);
        dateInputContainer.add(btnCalendarDischarge, BorderLayout.EAST);

        pnlDischargeDate.add(lblThoiGian);
        pnlDischargeDate.add(dateInputContainer);
        contentPanel.add(pnlDischargeDate, BorderLayout.NORTH);

        // 2b. Lưới 2 cột x 3 hàng các ô văn bản
        JPanel gridTextAreas = new JPanel(new GridLayout(3, 2, 8, 6));
        gridTextAreas.setBackground(new Color(240, 243, 248));

        // Hàng 1, Cột 1: Tình trạng bệnh nhân khi ra viện: (Nền vàng nhạt #FFFFE6)
        txtTinhTrangRaVien = new JTextArea();
        txtTinhTrangRaVien.setFont(AppFonts.SMALL);
        txtTinhTrangRaVien.setBackground(new Color(255, 255, 235)); // Màu kem vàng nhạt khớp ảnh
        txtTinhTrangRaVien.setLineWrap(true);
        txtTinhTrangRaVien.setWrapStyleWord(true);
        txtTinhTrangRaVien.setBorder(new EmptyBorder(3, 5, 3, 5));
        gridTextAreas.add(createLabeledArea("Tình trạng bệnh nhân khi ra viện:", txtTinhTrangRaVien));

        // Hàng 1, Cột 2: Quá trình bệnh lý và diễn biến lâm sàng:
        txtQuaTrinhBenhLy = createDefaultTextArea();
        gridTextAreas.add(createLabeledArea("Quá trình bệnh lý và diễn biến lâm sàng:", txtQuaTrinhBenhLy));

        // Hàng 2, Cột 1: Hướng điều trị tiếp theo và chế độ dinh dưỡng, sinh hoạt:
        txtHuongDieuTri = createDefaultTextArea();
        gridTextAreas.add(createLabeledArea("Hướng điều trị tiếp theo và chế độ dinh dưỡng, sinh hoạt:", txtHuongDieuTri));

        // Hàng 2, Cột 2: Tóm tắt kết quả xét nghiệm:
        txtTomTatXetNghiem = createDefaultTextArea();
        gridTextAreas.add(createLabeledArea("Tóm tắt kết quả xét nghiệm:", txtTomTatXetNghiem));

        // Hàng 3, Cột 1: Lời dặn của bác sĩ:
        txtLoiDanBacSiLeft = createDefaultTextArea();
        gridTextAreas.add(createLabeledArea("Lời dặn của bác sĩ:", txtLoiDanBacSiLeft));

        // Hàng 3, Cột 2: Lời dặn của bác sĩ:
        txtLoiDanBacSiRight = createDefaultTextArea();
        gridTextAreas.add(createLabeledArea("Lời dặn của bác sĩ:", txtLoiDanBacSiRight));

        contentPanel.add(gridTextAreas, BorderLayout.CENTER);

        // 2c. Hàng dưới: Hẹn tái khám + Nút Lưu & In / Đóng
        JPanel bottomSection = new JPanel(new BorderLayout(0, 8));
        bottomSection.setBackground(new Color(240, 243, 248));
        bottomSection.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel reexamPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        reexamPanel.setBackground(new Color(240, 243, 248));

        JLabel lblHenTaiKham = new JLabel("Thời gian hẹn tái khám (nếu có):");
        lblHenTaiKham.setFont(AppFonts.SMALL);
        lblHenTaiKham.setForeground(new Color(25, 30, 40));

        JPanel reexamInputContainer = new JPanel(new BorderLayout());
        reexamInputContainer.setBackground(Color.WHITE);
        reexamInputContainer.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        reexamInputContainer.setPreferredSize(new Dimension(135, 24));

        txtHenTaiKham = new JTextField("Chọn ngày");
        txtHenTaiKham.setFont(AppFonts.SMALL);
        txtHenTaiKham.setForeground(new Color(100, 110, 125));
        txtHenTaiKham.setHorizontalAlignment(SwingConstants.CENTER);
        txtHenTaiKham.setBorder(new EmptyBorder(2, 4, 2, 2));

        JButton btnCalendarReexam = new JButton(new CalendarDropIcon());
        btnCalendarReexam.setPreferredSize(new Dimension(22, 22));
        btnCalendarReexam.setBackground(new Color(245, 247, 250));
        btnCalendarReexam.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(205, 214, 226)));
        btnCalendarReexam.setFocusPainted(false);
        btnCalendarReexam.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCalendarReexam.addActionListener(e -> setReexamDateOffset(Calendar.DAY_OF_YEAR, 7));

        reexamInputContainer.add(txtHenTaiKham, BorderLayout.CENTER);
        reexamInputContainer.add(btnCalendarReexam, BorderLayout.EAST);

        reexamPanel.add(lblHenTaiKham);
        reexamPanel.add(reexamInputContainer);

        // Các nút bấm nhanh ngày hẹn tái khám
        reexamPanel.add(createQuickDateLink("2 ngày sau", Calendar.DAY_OF_YEAR, 2));
        reexamPanel.add(createQuickDateLink("1 tuần sau", Calendar.DAY_OF_YEAR, 7));
        reexamPanel.add(createQuickDateLink("2 tuần sau", Calendar.DAY_OF_YEAR, 14));
        reexamPanel.add(createQuickDateLink("1 tháng sau", Calendar.MONTH, 1));

        bottomSection.add(reexamPanel, BorderLayout.NORTH);

        // Hàng nút chức năng góc phải dưới: [Lưu và In (Ctrl+P)] [Đóng (Esc)]
        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actionButtonsPanel.setBackground(new Color(240, 243, 248));

        btnSaveAndPrint = new JButton("Lưu và In (Ctrl+P)", new GreenCheckIcon());
        btnSaveAndPrint.setFont(AppFonts.SMALL);
        btnSaveAndPrint.setForeground(new Color(25, 30, 40));
        btnSaveAndPrint.setBackground(Color.WHITE);
        btnSaveAndPrint.setFocusPainted(false);
        btnSaveAndPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSaveAndPrint.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        btnSaveAndPrint.addActionListener(e -> handleSaveAndPrint());

        btnClose = new JButton("Đóng (Esc)", new TreatmentResolutionTabPanel.SaveDiskIcon());
        btnClose.setFont(AppFonts.SMALL);
        btnClose.setForeground(new Color(25, 30, 40));
        btnClose.setBackground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        btnClose.addActionListener(e -> {
            if (onCloseCallback != null) onCloseCallback.run();
        });

        actionButtonsPanel.add(btnSaveAndPrint);
        actionButtonsPanel.add(btnClose);
        bottomSection.add(actionButtonsPanel, BorderLayout.SOUTH);

        contentPanel.add(bottomSection, BorderLayout.SOUTH);
        add(contentPanel, BorderLayout.CENTER);

        // Đăng ký phím tắt Ctrl+P và Esc
        setupKeyBindings();
    }

    private JTextArea createDefaultTextArea() {
        JTextArea area = new JTextArea();
        area.setFont(AppFonts.SMALL);
        area.setBackground(Color.WHITE);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(3, 5, 3, 5));
        return area;
    }

    private JPanel createLabeledArea(String labelText, JTextArea area) {
        JPanel pnl = new JPanel(new BorderLayout(0, 2));
        pnl.setBackground(new Color(240, 243, 248));

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(AppFonts.SMALL);
        lbl.setForeground(new Color(25, 30, 40));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(new LineBorder(new Color(185, 195, 210), 1));

        pnl.add(lbl, BorderLayout.NORTH);
        pnl.add(scroll, BorderLayout.CENTER);
        return pnl;
    }

    private JLabel createQuickDateLink(String text, int calendarField, int amount) {
        JLabel link = new JLabel(text);
        link.setFont(AppFonts.SMALL);
        link.setForeground(new Color(0, 51, 153));
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setReexamDateOffset(calendarField, amount);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                link.setText("<html><u>" + text + "</u></html>");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                link.setText(text);
            }
        });
        return link;
    }

    private void setReexamDateOffset(int field, int amount) {
        Calendar cal = Calendar.getInstance();
        cal.add(field, amount);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        txtHenTaiKham.setText(sdf.format(cal.getTime()));
        txtHenTaiKham.setForeground(new Color(25, 30, 40));
    }

    private void handleSaveAndPrint() {
        DischargeSummaryData data = new DischargeSummaryData();
        data.thoiGianRaVien = txtThoiGianRaVien.getText().trim();
        data.tinhTrangRaVien = txtTinhTrangRaVien.getText().trim();
        data.quaTrinhBenhLy = txtQuaTrinhBenhLy.getText().trim();
        data.huongDieuTri = txtHuongDieuTri.getText().trim();
        data.tomTatXetNghiem = txtTomTatXetNghiem.getText().trim();
        data.loiDanBacSiLeft = txtLoiDanBacSiLeft.getText().trim();
        data.loiDanBacSiRight = txtLoiDanBacSiRight.getText().trim();
        data.henTaiKham = txtHenTaiKham.getText().trim();

        if (onSaveCallback != null) {
            onSaveCallback.accept(data);
        }

        JOptionPane.showMessageDialog(this,
                "<html>Đã lưu và xuất in <b>Phiếu tóm tắt ra viện</b> thành công!<br>" +
                        "Thời gian ra viện: " + data.thoiGianRaVien + "<br>" +
                        "Hẹn tái khám: " + data.henTaiKham + "</html>",
                "Thông Báo Lưu Phiếu Ra Viện", JOptionPane.INFORMATION_MESSAGE);

        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDialog");
        am.put("closeDialog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onCloseCallback != null) onCloseCallback.run();
            }
        });

        int mask = GraphicsEnvironment.isHeadless() ? java.awt.event.InputEvent.CTRL_DOWN_MASK : Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, mask), "saveAndPrint");
        am.put("saveAndPrint", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSaveAndPrint();
            }
        });
    }

    // --- CÁC BIỂU TƯỢNG VECTOR KHỚP ẢNH ---
    public static class MedicalRedCrossIcon implements Icon {
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(180, 40, 40));
            g2.fillRoundRect(x + 1, y + 1, 14, 14, 3, 3);

            g2.setColor(Color.WHITE);
            g2.fillRect(x + 6, y + 3, 4, 10);
            g2.fillRect(x + 3, y + 6, 10, 4);

            g2.dispose();
        }
    }

    public static class CalendarDropIcon implements Icon {
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Tờ lịch
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 2, y + 2, 8, 9);
            g2.setColor(new Color(120, 135, 155));
            g2.drawRect(x + 2, y + 2, 8, 9);
            g2.setColor(new Color(190, 60, 60));
            g2.fillRect(x + 2, y + 2, 8, 3);

            // Mũi tên xổ
            g2.setColor(new Color(60, 70, 85));
            Polygon poly = new Polygon();
            poly.addPoint(x + 11, y + 5);
            poly.addPoint(x + 15, y + 5);
            poly.addPoint(x + 13, y + 8);
            g2.fillPolygon(poly);

            g2.dispose();
        }
    }

    public static class GreenCheckIcon implements Icon {
        @Override public int getIconWidth() { return 15; }
        @Override public int getIconHeight() { return 15; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(75, 140, 120));
            g2.fillRoundRect(x + 1, y + 1, 13, 13, 2, 2);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 3, y + 7, x + 6, y + 11);
            g2.drawLine(x + 6, y + 11, x + 12, y + 4);

            g2.dispose();
        }
    }
}
