package vn.clinicmanagement.desktop.modules.pharmacy.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.function.Consumer;

/** Menu tạo phiếu nhà thuốc, chia nhóm và thứ tự theo ảnh thiết kế. */
public final class NhaThuocTaoPhieuMenu extends JPopupMenu {
    private static final int CHIEU_RONG = 246;
    private static final int CHIEU_CAO_DONG = 19;
    private static final Font CHU_MUC = new Font("Arial", Font.PLAIN, 12);

    public NhaThuocTaoPhieuMenu(Consumer<String> khiChonLoaiPhieu) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(new LineBorder(new Color(163, 163, 155)));
        setFocusable(true);
        getAccessibleContext().setAccessibleName("Tạo phiếu nhà thuốc");
        addGroup("Nhà Cung Cấp");
        addItem("Nhập Thuốc Từ Nhà Cung Cấp", khiChonLoaiPhieu);
        addItem("Xuất Trả Thuốc Cho Nhà Cung Cấp", khiChonLoaiPhieu);
        addGroup("Kho Tổng");
        addItem("Nhập Thuốc/Vật Tư Từ Kho Tổng", khiChonLoaiPhieu);
        addItem("Xuất Trả Thuốc/Vật Tư Cho Kho Tổng", khiChonLoaiPhieu);
        addGroup("Kho Khác");
        addItem("Nhập Từ Kho Khác", khiChonLoaiPhieu);
        addItem("Xuất Trả Thuốc đến Kho Khác", khiChonLoaiPhieu);
        addGroup("Khách Hàng");
        addItem("Xuất Thuốc Cho Khách Hàng", khiChonLoaiPhieu);
        addItem("Nhập Trả Thuốc Từ Khách Hàng", khiChonLoaiPhieu);
        addGroup("Kho Nhà Thuốc");
        addItem("Nhập Bù", khiChonLoaiPhieu);
        addItem("Xuất Thanh Lý", khiChonLoaiPhieu);
        addItem("Xuất Kiểm Nghiệm", khiChonLoaiPhieu);
        addItem("Xuất Hủy (Mất/Hỏng/Vỡ)", khiChonLoaiPhieu);
        addItem("Xuất Khác", khiChonLoaiPhieu);
    }

    /** Tiêu đề nhóm là nhãn, không thể chọn như một loại phiếu. */
    private void addGroup(String title) {
        JLabel heading = new JLabel(title) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setPaint(new GradientPaint(0, 0, new Color(250, 250, 246),
                        0, getHeight(), new Color(234, 234, 228)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(224, 224, 216));
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        heading.setFont(CHU_MUC.deriveFont(Font.BOLD));
        heading.setForeground(Color.BLACK);
        heading.setBorder(new EmptyBorder(0, 8, 0, 4));
        sizeRow(heading);
        add(heading);
    }

    /** Mỗi mục phát lại tên loại phiếu để màn hình nối xử lý nghiệp vụ sau. */
    private void addItem(String title, Consumer<String> callback) {
        JMenuItem item = new JMenuItem(title);
        item.setUI(new javax.swing.plaf.basic.BasicMenuItemUI());
        item.setFont(CHU_MUC);
        item.setForeground(Color.BLACK);
        item.setBackground(Color.WHITE);
        item.setOpaque(true);
        item.setBorder(new EmptyBorder(0, 28, 0, 4));
        item.setIconTextGap(0);
        item.addActionListener(e -> { if (callback != null) callback.accept(title); });
        sizeRow(item);
        add(item);
    }

    private static void sizeRow(JComponent row) {
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(CHIEU_RONG - 2, CHIEU_CAO_DONG));
        row.setMinimumSize(new Dimension(CHIEU_RONG - 2, CHIEU_CAO_DONG));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, CHIEU_CAO_DONG));
    }
}
