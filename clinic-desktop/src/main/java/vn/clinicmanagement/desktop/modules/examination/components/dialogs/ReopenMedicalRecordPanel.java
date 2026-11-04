package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.desktop.modules.examination.ExaminationPanel.AdminPatientItem;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Giao diện màn hình: "EHC - Mở Lại Bệnh Án"
 * Khớp 100% ảnh chụp thực tế media_1793455095013_78d4d94b.jpg:
 * - Tiêu đề cửa sổ: EHC - Mở Lại Bệnh Án --- Người đăng nhập : tiennv -- --- Version : 2022.77.5.3 ---
 * - Banner bệnh nhân góc phải:
 *   + Dòng 1: ĐỖ MỸ LINH | 22000305 | 1998 (24 tuổi) | Nam (Màu đỏ sẫm #912323)
 *   + Dòng 2: Diện: Viện phí
 *   + Dòng 3: Phường Dịch Vọng Hậu, Q, Cầu Giấy, TP Hà Nội
 * - Khung nhập liệu:
 *   + Ngày yêu cầu: 10:13 11/08/2022
 *   + Người yêu cầu: TIENNV
 *   + Loại mở bệnh án (*): [Mở tạm để sửa chữa hồ sơ (không cho xử lý lại) ▾] (Nền vàng nhạt)
 *   + Lý do mở bệnh án (*): Vùng văn bản nhiều dòng
 * - Nút bấm góc dưới phải: [💾 Lưu]
 */
public class ReopenMedicalRecordPanel extends JPanel {

    public static class ReopenRecordRequest {
        public final String patientCode;
        public final String patientName;
        public final String requestTime;
        public final String requestedBy;
        public final String reopenType;
        public final String reason;

        public ReopenRecordRequest(String patientCode, String patientName, String requestTime,
                                   String requestedBy, String reopenType, String reason) {
            this.patientCode = patientCode;
            this.patientName = patientName;
            this.requestTime = requestTime;
            this.requestedBy = requestedBy;
            this.reopenType = reopenType;
            this.reason = reason;
        }
    }

    private final AdminPatientItem patient;
    private final Consumer<ReopenRecordRequest> onSaveCallback;
    private final Runnable onCloseCallback;

    private JTextField txtRequestTime;
    private JTextField txtRequestedBy;
    private JComboBox<String> cboReopenType;
    private JTextArea txtReason;

    private Point dragPoint;

    public ReopenMedicalRecordPanel(AdminPatientItem patient,
                                   Consumer<ReopenRecordRequest> onSaveCallback,
                                   Runnable onCloseCallback) {
        this.patient = patient;
        this.onSaveCallback = onSaveCallback;
        this.onCloseCallback = onCloseCallback;

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new LineBorder(new Color(150, 165, 185), 1));
        setPreferredSize(new Dimension(530, 420));

        // 1. THANH TIÊU ĐỀ TÙY BIẾN (Custom Draggable Title Bar)
        add(buildTitleBar(), BorderLayout.NORTH);

        // 2. PHẦN THÂN GIAO DIỆN CHÍNH
        JPanel bodyPanel = new JPanel(new BorderLayout(0, 8));
        bodyPanel.setBackground(Color.WHITE);
        bodyPanel.setBorder(new EmptyBorder(6, 12, 10, 12));

        // 2.1. Banner thông tin bệnh nhân + Nút menu ☰
        bodyPanel.add(buildPatientBanner(), BorderLayout.NORTH);

        // 2.2. Form nhập liệu thông tin mở lại bệnh án
        bodyPanel.add(buildFormInputs(), BorderLayout.CENTER);

        // 2.3. Nút Lưu ở góc dưới bên phải
        bodyPanel.add(buildBottomBar(), BorderLayout.SOUTH);

        add(bodyPanel, BorderLayout.CENTER);
    }

    /** Thanh tiêu đề: EHC - Mở Lại Bệnh Án --- Người đăng nhập : ... */
    private JPanel buildTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(245, 247, 250));
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 215, 225)));
        titleBar.setPreferredSize(new Dimension(530, 26));

        // Icon chữ thập đỏ + Text tiêu đề
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        leftPanel.setOpaque(false);

        JLabel lblIcon = new JLabel(new RedCrossBadgeIcon());
        String currentUser = UserSession.getInstance().getUsername();
        if (currentUser == null || currentUser.isEmpty()) {
            currentUser = "tiennv";
        }
        JLabel lblTitle = new JLabel("EHC - Mở Lại Bệnh Án  --- Người đăng nhập : " + currentUser + " --  --- Version : 2022.77.5.3 ---");
        lblTitle.setFont(AppFonts.SMALL);
        lblTitle.setForeground(new Color(35, 45, 60));

        leftPanel.add(lblIcon);
        leftPanel.add(lblTitle);

        // Nút đóng ✕
        JButton btnClose = new JButton("✕");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.setForeground(new Color(100, 110, 120));
        btnClose.setBackground(new Color(245, 247, 250));
        btnClose.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnClose.setBackground(new Color(232, 17, 35));
                btnClose.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnClose.setBackground(new Color(245, 247, 250));
                btnClose.setForeground(new Color(100, 110, 120));
            }
        });
        btnClose.addActionListener(e -> {
            if (onCloseCallback != null) onCloseCallback.run();
        });

        titleBar.add(leftPanel, BorderLayout.WEST);
        titleBar.add(btnClose, BorderLayout.EAST);

        // Hỗ trợ kéo di chuyển cửa sổ
        titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragPoint = e.getPoint();
            }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Window win = SwingUtilities.getWindowAncestor(ReopenMedicalRecordPanel.this);
                if (win != null && dragPoint != null) {
                    Point curr = win.getLocation();
                    win.setLocation(curr.x + e.getX() - dragPoint.x, curr.y + e.getY() - dragPoint.y);
                }
            }
        });

        return titleBar;
    }

    /** Banner thông tin bệnh nhân góc phải + Nút hamburger ☰ */
    private JPanel buildPatientBanner() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 225, 235)));
        panel.setPreferredSize(new Dimension(500, 68));

        // Nút hamburger ☰ bên trái
        JButton btnMenu = new JButton("☰");
        btnMenu.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnMenu.setForeground(new Color(30, 85, 145));
        btnMenu.setBackground(new Color(245, 248, 252));
        btnMenu.setFocusPainted(false);
        btnMenu.setBorder(new LineBorder(new Color(185, 200, 220), 1));
        btnMenu.setPreferredSize(new Dimension(34, 28));
        btnMenu.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        leftBox.setOpaque(false);
        leftBox.add(btnMenu);

        // Thông tin tóm tắt bệnh nhân bên phải
        JPanel infoBox = new JPanel();
        infoBox.setLayout(new BoxLayout(infoBox, BoxLayout.Y_AXIS));
        infoBox.setOpaque(false);

        String name = patient != null ? patient.name : "ĐỖ MỸ LINH";
        String code = patient != null ? patient.code : "22000305";
        String birthYear = patient != null ? patient.birthYear : "1998";
        String age = patient != null ? patient.bannerAge : "24 tuổi";
        String gender = patient != null ? patient.gender : "Nam";
        String reqType = patient != null ? patient.requestType : "Viện phí";
        String address = patient != null && patient.bannerAddress != null && !patient.bannerAddress.isEmpty()
                ? patient.bannerAddress : "Phường Dịch Vọng Hậu, Q, Cầu Giấy, TP Hà Nội";

        // Dòng 1: ĐỖ MỸ LINH | 22000305 | 1998 (24 tuổi) | Nam
        JLabel lblLine1 = new JLabel(name + " | " + code + " | " + birthYear + " (" + age + ") | " + gender);
        lblLine1.setFont(AppFonts.BODY_BOLD);
        lblLine1.setForeground(new Color(145, 35, 35)); // Đỏ sẫm y tế
        lblLine1.setAlignmentX(Component.RIGHT_ALIGNMENT);

        // Dòng 2: Diện: Viện phí
        JLabel lblLine2 = new JLabel("Diện: " + reqType);
        lblLine2.setFont(AppFonts.SMALL);
        lblLine2.setForeground(new Color(60, 65, 75));
        lblLine2.setAlignmentX(Component.RIGHT_ALIGNMENT);

        // Dòng 3: Địa chỉ
        JLabel lblLine3 = new JLabel(address);
        lblLine3.setFont(AppFonts.SMALL);
        lblLine3.setForeground(new Color(90, 95, 105));
        lblLine3.setAlignmentX(Component.RIGHT_ALIGNMENT);

        infoBox.add(lblLine1);
        infoBox.add(Box.createVerticalStrut(2));
        infoBox.add(lblLine2);
        infoBox.add(Box.createVerticalStrut(2));
        infoBox.add(lblLine3);

        panel.add(leftBox, BorderLayout.WEST);
        panel.add(infoBox, BorderLayout.EAST);

        return panel;
    }

    /** Form các trường thông tin: Ngày yêu cầu, Người yêu cầu, Loại mở, Lý do */
    private JPanel buildFormInputs() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(8, 0, 6, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. Ngày yêu cầu
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.0;
        JLabel lblDate = new JLabel("Ngày yêu cầu");
        lblDate.setFont(AppFonts.BODY);
        lblDate.setPreferredSize(new Dimension(135, 24));
        form.add(lblDate, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0;
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
        txtRequestTime = new JTextField(nowStr);
        txtRequestTime.setFont(AppFonts.BODY);
        txtRequestTime.setPreferredSize(new Dimension(200, 24));
        txtRequestTime.setBorder(new CompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        form.add(txtRequestTime, gbc);

        // 2. Người yêu cầu
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel lblUser = new JLabel("Người yêu cầu");
        lblUser.setFont(AppFonts.BODY);
        form.add(lblUser, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.weightx = 1.0;
        String curUsername = UserSession.getInstance().getUsername();
        if (curUsername == null || curUsername.isEmpty()) curUsername = "TIENNV";
        else curUsername = curUsername.toUpperCase();

        txtRequestedBy = new JTextField(curUsername);
        txtRequestedBy.setFont(AppFonts.BODY);
        txtRequestedBy.setPreferredSize(new Dimension(200, 24));
        txtRequestedBy.setBorder(new CompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        form.add(txtRequestedBy, gbc);

        // 3. Loại mở bệnh án (*)
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weightx = 0.0;
        JLabel lblType = new JLabel("Loại mở bệnh án (*)");
        lblType.setFont(AppFonts.BODY);
        form.add(lblType, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        gbc.weightx = 1.0;
        String[] types = {
                "Mở tạm để sửa chữa hồ sơ (không cho xử trí lại)",
                "Mở tạm để sửa chữa hồ sơ (có cho xử trí lại)",
                "Mở lại bệnh án, huỷ xử trí"
        };
        cboReopenType = new JComboBox<>(types);
        cboReopenType.setFont(AppFonts.BODY);
        cboReopenType.setBackground(new Color(254, 246, 222)); // Nền vàng nhạt khớp 100% ảnh chụp
        cboReopenType.setSelectedIndex(0); // Mặc định dòng 1
        cboReopenType.setPreferredSize(new Dimension(340, 25));
        form.add(cboReopenType, gbc);

        // 4. Lý do mở bệnh án (*)
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel lblReason = new JLabel("Lý do mở bệnh án (*)");
        lblReason.setFont(AppFonts.BODY);
        form.add(lblReason, gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        txtReason = new JTextArea(6, 20);
        txtReason.setFont(AppFonts.BODY);
        txtReason.setLineWrap(true);
        txtReason.setWrapStyleWord(true);
        txtReason.setBorder(new EmptyBorder(4, 6, 4, 6));

        JScrollPane scrollReason = new JScrollPane(txtReason);
        scrollReason.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        scrollReason.setPreferredSize(new Dimension(320, 140));
        form.add(scrollReason, gbc);

        return form;
    }

    /** Thanh dưới cùng chứa nút [💾 Lưu] */
    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 235, 245)));

        JButton btnSave = new JButton("Lưu", new FloppyDiskIcon());
        btnSave.setFont(AppFonts.BODY_BOLD);
        btnSave.setBackground(new Color(245, 248, 252));
        btnSave.setForeground(new Color(30, 45, 65));
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.setPreferredSize(new Dimension(88, 28));
        btnSave.setBorder(new CompoundBorder(
                new LineBorder(new Color(175, 190, 210), 1),
                new EmptyBorder(2, 8, 2, 8)
        ));

        btnSave.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnSave.setBackground(new Color(225, 238, 252));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnSave.setBackground(new Color(245, 248, 252));
            }
        });

        btnSave.addActionListener(e -> handleSave());
        bar.add(btnSave);

        return bar;
    }

    private void handleSave() {
        String reason = txtReason.getText() != null ? txtReason.getText().trim() : "";
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập 'Lý do mở bệnh án (*)' trước khi lưu!",
                    "Thiếu Thông Tin", JOptionPane.WARNING_MESSAGE);
            txtReason.requestFocus();
            return;
        }

        String code = patient != null ? patient.code : "22000305";
        String name = patient != null ? patient.name : "ĐỖ MỸ LINH";
        String time = txtRequestTime.getText();
        String user = txtRequestedBy.getText();
        String type = (String) cboReopenType.getSelectedItem();

        ReopenRecordRequest req = new ReopenRecordRequest(code, name, time, user, type, reason);

        if (onSaveCallback != null) {
            onSaveCallback.accept(req);
        }

        JOptionPane.showMessageDialog(this,
                "Đã ghi nhận yêu cầu mở lại bệnh án cho bệnh nhân [" + name + " - " + code + "].\n"
                        + "▶ Loại mở: " + type + "\n"
                        + "▶ Trạng thái: Hồ sơ đã được mở khóa để chỉnh sửa thông tin.",
                "Mở Lại Bệnh Án Thành Công", JOptionPane.INFORMATION_MESSAGE);

        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    // =========================================================================
    // VECTOR ICONS (KHỚP 100% ẢNH MẪU EHC HIS)
    // =========================================================================

    /** Icon chữ thập đỏ trên góc tiêu đề cửa sổ EHC */
    private static class RedCrossBadgeIcon implements Icon {
        @Override public int getIconWidth() { return 15; }
        @Override public int getIconHeight() { return 15; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Nền trắng viền xám
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(x + 1, y + 1, 13, 13, 2, 2);
            g2.setColor(new Color(180, 190, 205));
            g2.drawRoundRect(x + 1, y + 1, 13, 13, 2, 2);
            // Chữ thập đỏ
            g2.setColor(new Color(220, 45, 45));
            g2.fillRect(x + 6, y + 3, 3, 9);
            g2.fillRect(x + 3, y + 6, 9, 3);
            g2.dispose();
        }
    }

    /** Icon đĩa mềm Lưu (Floppy Disk) màu tím đậm / navy y tế */
    private static class FloppyDiskIcon implements Icon {
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Thân đĩa tím navy
            g2.setColor(new Color(110, 85, 145));
            g2.fillRoundRect(x + 2, y + 2, 12, 12, 2, 2);
            // Cửa trượt kim loại trắng
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 4, y + 2, 6, 4);
            g2.setColor(new Color(110, 85, 145));
            g2.fillRect(x + 6, y + 3, 2, 2);
            // Nhãn đĩa trắng
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 4, y + 8, 8, 5);
            g2.setColor(new Color(110, 85, 145));
            g2.drawLine(x + 5, y + 10, x + 10, y + 10);
            g2.dispose();
        }
    }
}
