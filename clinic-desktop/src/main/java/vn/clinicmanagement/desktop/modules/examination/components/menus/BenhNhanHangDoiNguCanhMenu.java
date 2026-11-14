package vn.clinicmanagement.desktop.modules.examination.components.menus;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.function.Consumer;

/**
 * Menu ngữ cảnh chuột phải trên Danh sách hàng đợi bệnh nhân (Queue Table).
 * Tự động chuyển đổi giao diện chính xác theo trạng thái bệnh nhân:
 * - BỆNH NHÂN ĐÃ KHÁM XONG (khớp ảnh chụp thực tế):
 *   1. Bệnh Án
 *   2. Gọi Bệnh Nhân (MỜ ẨN / DISABLED)
 *   3. Lịch Sử Khám Bệnh (Được bôi nền vàng nhạt)
 *   4. Lịch Sử Sự Kiện Của Bệnh Án (Icon 'i' xanh)
 *   5. Đặt Hẹn (Icon lịch hẹn)
 *   6. Sửa Thông Tin Bố, Mẹ, NND (Người Nuôi Dưỡng)
 *   7. Thông Tin Ghi Chú
 *   8. MỞ LẠI BỆNH ÁN (Thao tác then chốt cho bệnh nhân đã khám xong)
 *   9. Chức Năng Khác ▶
 * - BỆNH NHÂN ĐANG CHỜ KHÁM:
 *   - Gọi Bệnh Nhân (Active)
 *   - Chuyển Bệnh Nhân Sang Phòng Khác (Highlight xanh)
 */
public class BenhNhanHangDoiNguCanhMenu extends JPopupMenu {

    /** Trường `patientName`: bệnh nhân tên. */
    private final String patientName;
    /** Trường `patientCode`: bệnh nhân mã. */
    private final String patientCode;
    /** Trường `isExamined`: là `Examined`. */
    private final boolean isExamined;
    /** Trường `onActionSelected`: tải hành động duocchon. */
    private final Consumer<String> onActionSelected;

    public BenhNhanHangDoiNguCanhMenu(String patientName, String patientCode, Consumer<String> onActionSelected) {
        this(patientName, patientCode, false, onActionSelected);
    }

    public BenhNhanHangDoiNguCanhMenu(String patientName, String patientCode, boolean isExamined, Consumer<String> onActionSelected) {
        this.patientName = patientName != null ? patientName : "Bệnh nhân";
        this.patientCode = patientCode != null ? patientCode : "";
        this.isExamined = isExamined;
        this.onActionSelected = onActionSelected;

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(new Color(170, 180, 195), 1));

        // 1. Bệnh Án
        add(createMenuItem("Bệnh Án", new MedicalRecordIcon(), false, false));

        // 2. Gọi Bệnh Nhân (Nếu đã khám xong thì MỜ ẨN / DISABLED)
        JMenuItem itemCall = createMenuItem("Gọi Bệnh Nhân", new CallPatientIcon(!isExamined), false, false);
        if (isExamined) {
            itemCall.setEnabled(false);
            itemCall.setForeground(new Color(160, 165, 175));
            itemCall.setToolTipText("Bệnh nhân đã hoàn thành khám, không thể gọi vào phòng");
        }
        add(itemCall);

        // 3. Lịch Sử Khám Bệnh (Được bôi màu vàng nhạt mềm #FEF6DE như ảnh chụp)
        JMenuItem itemHistory = createMenuItem("Lịch Sử Khám Bệnh", new HistoryClockIcon(), isExamined, true);
        add(itemHistory);

        // 4. Lịch Sử Sự Kiện Của Bệnh Án (Chữ i xanh)
        add(createMenuItem("Lịch Sử Sự Kiện Của Bệnh Án", new EventLogInfoIcon(), false, false));

        // Nếu CHƯA KHÁM XONG: Hiện mục "Chuyển Bệnh Nhân Sang Phòng Khác"
        if (!isExamined) {
            JMenuItem itemTransfer = createMenuItem("Chuyển Bệnh Nhân Sang Phòng Khác", new TransferRoomIcon(), true, false);
            add(itemTransfer);
        }

        // 5. Đặt Hẹn
        add(createMenuItem("Đặt Hẹn", new SendAppointmentIcon(), false, false));

        // 6. Sửa Thông Tin Bố, Mẹ, NND
        add(createMenuItem("Sửa Thông Tin Bố, Mẹ, NND", new EditGuardianIcon(), false, false));

        // 7. Thông Tin Ghi Chú
        add(createMenuItem("Thông Tin Ghi Chú", new StickyNoteIcon(), false, false));

        // 8. MỞ LẠI BỆNH ÁN (Thao tác đặc biệt khi bệnh nhân đã khám xong)
        if (isExamined) {
            JMenuItem itemReopen = createReopenMenuItem();
            add(itemReopen);
        } else {
            add(createMenuItem("Lịch sử khám lại", new ReExamHistoryIcon(), false, false));
        }

        addSeparator();

        // 9. Chức Năng Khác ▶
        add(buildOtherFunctionsMenu());
    }

    /** Hàm `createMenuItem`: tạo menu mathang. */
    private JMenuItem createMenuItem(String text, Icon icon, boolean isHighlighted, boolean isYellowTheme) {
        JMenuItem item = new JMenuItem(text, icon);
        item.setFont(UngDungPhongChu.SMALL);
        item.setIconTextGap(8);
        item.setPreferredSize(new Dimension(225, 25));
        item.setBorder(new EmptyBorder(2, 6, 2, 6));

        if (isHighlighted && isYellowTheme) {
            // Nền vàng nhạt khớp 100% dòng "Lịch Sử Khám Bệnh" trong ảnh chụp thực tế
            item.setBackground(new Color(254, 246, 222));
            item.setForeground(new Color(30, 30, 30));
            item.setOpaque(true);
        } else if (isHighlighted) {
            item.setBackground(new Color(0, 120, 215)); // Royal blue
            item.setForeground(Color.WHITE);
            item.setOpaque(true);
        } else {
            item.setBackground(Color.WHITE);
            item.setForeground(new Color(25, 30, 40));
            item.setOpaque(true);
        }

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (item.isEnabled()) {
                    item.setBackground(new Color(0, 120, 215));
                    item.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!item.isEnabled()) return;
                if (isHighlighted && isYellowTheme) {
                    item.setBackground(new Color(254, 246, 222));
                    item.setForeground(new Color(30, 30, 30));
                } else if (isHighlighted) {
                    item.setBackground(new Color(0, 120, 215));
                    item.setForeground(Color.WHITE);
                } else {
                    item.setBackground(Color.WHITE);
                    item.setForeground(new Color(25, 30, 40));
                }
            }
        });

        item.addActionListener(e -> triggerAction(text));
        return item;
    }

    /** Tạo mục "Mở Lại Bệnh Án" với style vector nổi bật */
    private JMenuItem createReopenMenuItem() {
        JMenuItem item = new JMenuItem("Mở Lại Bệnh Án", new ReopenMedicalRecordIcon());
        item.setFont(UngDungPhongChu.SMALL_BOLD);
        item.setIconTextGap(8);
        item.setPreferredSize(new Dimension(225, 26));
        item.setBackground(Color.WHITE);
        item.setForeground(new Color(15, 110, 45)); // Xanh lá cây đậm y tế
        item.setOpaque(true);
        item.setBorder(new EmptyBorder(3, 6, 3, 6));

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                item.setBackground(new Color(230, 248, 235));
                item.setForeground(new Color(10, 95, 35));
                item.setBorder(new CompoundBorder(
                        new LineBorder(new Color(160, 220, 180), 1),
                        new EmptyBorder(2, 5, 2, 5)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                item.setBackground(Color.WHITE);
                item.setForeground(new Color(15, 110, 45));
                item.setBorder(new EmptyBorder(3, 6, 3, 6));
            }
        });

        item.addActionListener(e -> triggerAction("Mở Lại Bệnh Án"));
        return item;
    }

    /** Hàm `buildOtherFunctionsMenu`: tạo khac `Functions` menu. */
    private JMenu buildOtherFunctionsMenu() {
        JMenu menuOthers = new JMenu("Chức Năng Khác");
        menuOthers.setIcon(new MoreFunctionsIcon());
        menuOthers.setFont(UngDungPhongChu.SMALL);
        menuOthers.setBackground(Color.WHITE);
        menuOthers.setOpaque(true);
        menuOthers.setBorder(new EmptyBorder(3, 6, 3, 6));

        JMenuItem sub1 = new JMenuItem("Hủy tiếp đón");
        sub1.setFont(UngDungPhongChu.SMALL);
        sub1.addActionListener(e -> triggerAction("Hủy tiếp đón"));

        JMenuItem sub2 = new JMenuItem("In vòng tay người bệnh");
        sub2.setFont(UngDungPhongChu.SMALL);
        sub2.addActionListener(e -> triggerAction("In vòng tay người bệnh"));

        JMenuItem sub3 = new JMenuItem("In thẻ khám bệnh");
        sub3.setFont(UngDungPhongChu.SMALL);
        sub3.addActionListener(e -> triggerAction("In thẻ khám bệnh"));

        JMenuItem sub4 = new JMenuItem("Đổi đối tượng khám (BHYT / Viện phí)");
        sub4.setFont(UngDungPhongChu.SMALL);
        sub4.addActionListener(e -> triggerAction("Đổi đối tượng khám"));

        JMenuItem sub5 = new JMenuItem("Đồng bộ dữ liệu EMR hồ sơ điện tử");
        sub5.setFont(UngDungPhongChu.SMALL);
        sub5.addActionListener(e -> triggerAction("Đồng bộ dữ liệu EMR"));

        menuOthers.add(sub1);
        menuOthers.add(sub2);
        menuOthers.add(sub3);
        menuOthers.add(sub4);
        menuOthers.add(sub5);
        return menuOthers;
    }

    /** Hàm `triggerAction`: `trigger` hành động. */
    private void triggerAction(String actionName) {
        if (onActionSelected != null) {
            onActionSelected.accept(actionName);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Đã chọn thao tác: " + actionName + "\n▶ Bệnh nhân: " + patientName + " (" + patientCode + ")",
                    "Thao Tác Bệnh Nhân", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // =========================================================================
    // VECTOR ICONS SẮC NÉT (GRAPHICS2D KHỚP 100% ẢNH MẪU EHC HIS)
    // =========================================================================

    // 1. Icon Bệnh Án (Sổ bệnh án xanh)
    private static class MedicalRecordIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 16; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(45, 120, 205));
            g2.fillRoundRect(x + 2, y + 1, 12, 14, 2, 2);
            g2.setColor(Color.WHITE);
            g2.drawLine(x + 5, y + 5, x + 11, y + 5);
            g2.drawLine(x + 5, y + 8, x + 11, y + 8);
            g2.drawLine(x + 5, y + 11, x + 9, y + 11);
            g2.dispose();
        }
    }

    // 2. Icon Gọi Bệnh Nhân (Loa phát thanh / Megaphone)
    private static class CallPatientIcon implements Icon {
        /** Trường `enabled`: bat. */
        private final boolean enabled;

        public CallPatientIcon(boolean enabled) {
            this.enabled = enabled;
        }

        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 16; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color baseColor = enabled ? new Color(30, 140, 60) : new Color(175, 180, 190);
            g2.setColor(baseColor);

            // Vẽ loa phát thanh (Megaphone shape)
            Path2D.Double cone = new Path2D.Double();
            cone.moveTo(x + 3, y + 6);
            cone.lineTo(x + 9, y + 3);
            cone.lineTo(x + 9, y + 13);
            cone.lineTo(x + 3, y + 10);
            cone.closePath();
            g2.fill(cone);

            g2.fillRect(x + 2, y + 6, 2, 4);

            // Tay cầm loa
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawLine(x + 5, y + 10, x + 5, y + 14);

            // Sóng âm
            if (enabled) {
                g2.drawArc(x + 10, y + 4, 4, 8, -60, 120);
            }
            g2.dispose();
        }
    }

    // 3. Icon Lịch Sử Khám Bệnh (Đồng hồ xoay xanh)
    private static class HistoryClockIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 16; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(65, 135, 210));
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawOval(x + 2, y + 2, 12, 12);
            g2.drawLine(x + 8, y + 4, x + 8, y + 8);
            g2.drawLine(x + 8, y + 8, x + 11, y + 8);
            g2.dispose();
        }
    }

    // 4. Icon Nhật Ký Sự Kiện Của Bệnh Án (Chữ 'i' thông tin tròn xanh)
    private static class EventLogInfoIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 16; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(25, 115, 215));
            g2.fillOval(x + 2, y + 2, 12, 12);
            g2.setColor(Color.WHITE);
            g2.fillOval(x + 7, y + 4, 2, 2);
            g2.fillRect(x + 7, y + 7, 2, 5);
            g2.dispose();
        }
    }

    // 5. Icon Đặt Hẹn (Lịch hẹn 📅)
    private static class SendAppointmentIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 16; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(235, 80, 70));
            g2.fillRect(x + 2, y + 2, 12, 4);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 2, y + 6, 12, 8);
            g2.setColor(new Color(110, 120, 135));
            g2.drawRect(x + 2, y + 2, 12, 12);
            g2.fillRect(x + 5, y + 8, 2, 2);
            g2.fillRect(x + 9, y + 8, 2, 2);
            g2.dispose();
        }
    }

    // 6. Icon Sửa Thông Tin Bố, Mẹ, NND (Thẻ thông tin + bút vẽ)
    private static class EditGuardianIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 16; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(230, 165, 45));
            g2.fillRect(x + 2, y + 3, 9, 11);
            g2.setColor(new Color(60, 70, 85));
            g2.drawRect(x + 2, y + 3, 9, 11);
            // Cây bút chéo
            g2.setColor(new Color(65, 115, 215));
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawLine(x + 14, y + 2, x + 8, y + 10);
            g2.dispose();
        }
    }

    // 7. Icon Thông Tin Ghi Chú (Giấy note vàng)
    private static class StickyNoteIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 16; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(250, 215, 75));
            g2.fillRect(x + 2, y + 2, 12, 12);
            g2.setColor(new Color(210, 175, 40));
            g2.drawRect(x + 2, y + 2, 12, 12);
            // Dòng ghi chú
            g2.setColor(new Color(145, 115, 25));
            g2.drawLine(x + 4, y + 6, x + 11, y + 6);
            g2.drawLine(x + 4, y + 9, x + 9, y + 9);
            g2.dispose();
        }
    }

    // 8. Icon MỞ LẠI BỆNH ÁN (Thư mục hồ sơ xanh lá với mũi tên xoay lại)
    private static class ReopenMedicalRecordIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 16; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Bìa hồ sơ bệnh án vàng
            g2.setColor(new Color(245, 195, 75));
            g2.fillRect(x + 2, y + 3, 12, 10);
            g2.setColor(new Color(215, 165, 45));
            g2.drawRect(x + 2, y + 3, 12, 10);

            // Giấy tờ kẹp bên trong
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 4, y + 1, 8, 4);

            // Dấu cộng xanh lá hoặc mũi tên mở lại bệnh án ở góc
            g2.setColor(new Color(40, 165, 75));
            g2.fillOval(x + 8, y + 7, 7, 7);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawLine(x + 10, y + 10, x + 13, y + 10);
            g2.drawLine(x + 11, y + 8, x + 11, y + 12);

            g2.dispose();
        }
    }

    // 9. Icon Chuyển Phòng Khám (Dành cho bệnh nhân chưa khám xong)
    private static class TransferRoomIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 16; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(120, 135, 155));
            g2.drawRect(x + 2, y + 2, 7, 12);
            g2.fillRect(x + 3, y + 3, 5, 10);
            g2.setColor(new Color(35, 160, 230));
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawLine(x + 7, y + 8, x + 14, y + 8);
            g2.drawLine(x + 11, y + 5, x + 14, y + 8);
            g2.drawLine(x + 11, y + 11, x + 14, y + 8);
            g2.dispose();
        }
    }

    // 10. Icon Lịch sử khám lại
    private static class ReExamHistoryIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 16; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(105, 120, 140));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(x + 2, y + 2, 12, 12);
            g2.drawLine(x + 8, y + 5, x + 8, y + 8);
            g2.drawLine(x + 8, y + 8, x + 6, y + 10);
            g2.dispose();
        }
    }

    // 11. Icon Chức Năng Khác
    private static class MoreFunctionsIcon implements Icon {
        /** Hàm `getIconWidth`: lấy `Icon` `Width`. Gốc khai báo tại `Icon#getIconWidth`. */
        @Override public int getIconWidth() { return 16; }
        /** Hàm `getIconHeight`: lấy `Icon` `Height`. Gốc khai báo tại `Icon#getIconHeight`. */
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(90, 100, 115));
            g2.fillOval(x + 4, y + 4, 8, 8);
            g2.setColor(Color.WHITE);
            g2.fillOval(x + 6, y + 6, 4, 4);
            g2.dispose();
        }
    }
}
