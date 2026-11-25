package vn.clinicmanagement.desktop.modules.inventory;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicMenuItemUI;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/** Menu Báo Cáo của phân hệ Tủ Trực / Kho Vật Tư theo đúng danh sách 22 mẫu báo cáo. */
final class TuTrucBaoCaoMenu extends JPopupMenu {
    private static final int WIDTH = 315;
    private static final Color BORDER = new Color(160, 160, 155);
    private static final Color GUTTER = new Color(226, 226, 226);

    private static final List<String> REPORTS = List.of(
            "Báo Cáo Xuất Nhập Tồn",
            "Báo Cáo Nhập Xuất Tồn Chi Tiết",
            "Báo Cáo Xuất Vật Tư Theo Pk",
            "Báo Cáo Doanh Thu Vật Tư Bán Lẻ",
            "Báo Cáo Nhập Xuất Theo Ngày",
            "Báo Cáo Nhập Xuất Theo Khoa Phòng",
            "Báo Cáo Lợi Nhuận Vật Tư",
            "Báo Cáo Phiếu Nhập Nhà Cung Cấp",
            "Báo Cáo Thuốc Nhập Nhà Cung Cấp",
            "Báo Cáo Xuất Hao Phí Phòng Khám",
            "Báo Cáo Xuất Hao Phí Khoa Phòng",
            "Báo Cáo Kiểm Nhập Thuốc Vật Tư (Ncc)",
            "Báo Cáo Xuất Nhập Tồn Rút Gọn",
            "Biên Bản Kiểm Nhập Hóa Chất Từ Nhà Cung",
            "Biên Bản Kiểm Nhập Vật Tư Từ Nhà Cung Cấp",
            "Biên Bản Kiểm Kê Thuốc, Vật Tư",
            "Báo Cáo Xuất Nhập Tồn Hóa Chất",
            "Báo Cáo Xuất Nhập Tồn Kho Vật Tư Chờ Thanh Lý",
            "Thống Kê Tồn Vật Tư Toàn Viện",
            "Báo Cáo Xuất Nhập Tồn Vật Tư Toàn Viện",
            "Báo Cáo Xuất Nhập Tồn Kho Vật Tư",
            "Báo Gọn Báo Cáo Xuất Nhập Tồn Vật Tư Toàn Viện",
            "Rút Gọn Báo Cáo Xuất Nhập Tồn Kho Vật Tư"
    );

    TuTrucBaoCaoMenu(Consumer<String> onSelect) {
        setName("tuTrucBaoCaoMenu");
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(new LineBorder(BORDER));
        getAccessibleContext().setAccessibleName("Danh sách báo cáo vật tư");

        for (String title : REPORTS) {
            addReportItem(title, onSelect);
        }
    }

    private void addReportItem(String text, Consumer<String> onSelect) {
        JMenuItem item = new JMenuItem(text) {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                graphics.setColor(GUTTER);
                graphics.drawLine(24, 0, 24, getHeight() - 1);
            }
        };
        item.setUI(new BasicMenuItemUI() {
            @Override protected void installDefaults() {
                super.installDefaults();
                selectionBackground = new Color(255, 232, 166);
                selectionForeground = Color.BLACK;
            }
        });
        item.setFont(UngDungPhongChu.TU_TRUC_MENU);
        item.setForeground(Color.BLACK);
        item.setBackground(Color.WHITE);
        item.setOpaque(true);
        item.setBorder(new EmptyBorder(0, 32, 0, 8));
        item.setIconTextGap(0);
        item.setActionCommand(text);
        item.addActionListener(e -> {
            if (onSelect != null) onSelect.accept(e.getActionCommand());
        });
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setPreferredSize(new Dimension(WIDTH - 2, 22));
        item.setMinimumSize(new Dimension(WIDTH - 2, 22));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        add(item);
    }
}
