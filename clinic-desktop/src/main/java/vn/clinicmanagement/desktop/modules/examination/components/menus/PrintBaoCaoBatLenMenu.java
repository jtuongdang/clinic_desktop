package vn.clinicmanagement.desktop.modules.examination.components.menus;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Menu xổ xuống (Dropdown Menu) khi nhấn nút "In ▾" trên thanh công cụ khám bệnh.
 * Khớp 100% chi tiết trong ảnh chụp mẫu với 3 phân nhóm:
 * - Phiếu (9 mục)
 * - Bệnh Án (1 mục)
 * - Báo Cáo (11 mục)
 */
public class PrintBaoCaoBatLenMenu extends JPopupMenu {

    public PrintBaoCaoBatLenMenu(Component parent, Consumer<String> onSelectPrintAction) {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(new Color(135, 148, 165), 1));

        // --- NHÓM 1: PHIẾU (9 mục) ---
        addSectionHeader("Phiếu");
        addReportItem("Đăng Ký Khám Bệnh, Chữa Bệnh", onSelectPrintAction);
        addReportItem("Phiếu Chỉ Định Dịch Vụ (Tất Cả Dịch Vụ)", onSelectPrintAction);
        addReportItem("Phiếu Chỉ Định Dịch Vụ (Chưa Thanh Toán)", onSelectPrintAction);
        addReportItem("Phiếu Chỉ Định Cận Lâm Sàng", onSelectPrintAction);
        addReportItem("Phiếu Hướng Dẫn Thực Hiện Cận Lâm Sàng", onSelectPrintAction);
        addReportItem("Phiếu Điều Trị", onSelectPrintAction);
        addReportItem("Phiếu Thanh Toán Viện Phí Ngoại Trú", onSelectPrintAction);
        addReportItem("Phiếu Thanh Toán Bhyt Ngoại Trú", onSelectPrintAction);
        addReportItem("Phiếu Tổng Hợp Kết Quả CLS", onSelectPrintAction);

        // --- NHÓM 2: BỆNH ÁN (1 mục) ---
        addSectionHeader("Bệnh Án");
        addReportItem("Bệnh Án Khám Bệnh", onSelectPrintAction);

        // --- NHÓM 3: BÁO CÁO (11 mục) ---
        addSectionHeader("Báo Cáo");
        addReportItem("Sổ Khám Bệnh", onSelectPrintAction);
        addReportItem("Báo Cáo Hoạt Động Điều Trị Ngoại Trú", onSelectPrintAction);
        addReportItem("Phiếu Điều Trị Nội Trú", onSelectPrintAction);
        addReportItem("Phiếu Khám Bệnh", onSelectPrintAction);
        addReportItem("Phiếu Chỉ Định Cận Lâm Sàng (Covid Gộp)", onSelectPrintAction);
        addReportItem("Phiếu Công Khai Thuốc, Vật Tư, Dịch Vụ (Xã)", onSelectPrintAction);
        addReportItem("Giấy Ra Viện (Cho Bn Trực Viện)", onSelectPrintAction);
        addReportItem("Báo Cáo Số Lượng Dịch Vụ", onSelectPrintAction);
        addReportItem("Phiếu Đăng Ký Dịch Vụ Yêu Cầu Nhiều Lần", onSelectPrintAction);
        addReportItem("Phiếu Đăng Ký Dịch Vụ Yêu Cầu 1 Lần", onSelectPrintAction);
        addReportItem("Phiếu Chỉ Định Chung Test", onSelectPrintAction);
    }

    /** Hàm `addSectionHeader`: thêm `Section` `Header`. */
    private void addSectionHeader(String title) {
        JLabel lblHeader = new JLabel(title);
        lblHeader.setFont(UngDungPhongChu.SMALL_BOLD);
        lblHeader.setForeground(new Color(40, 50, 65));
        lblHeader.setOpaque(true);
        lblHeader.setBackground(new Color(234, 240, 248)); // Nền xám xanh nhạt phân nhóm
        lblHeader.setBorder(new EmptyBorder(3, 10, 3, 10));
        lblHeader.setPreferredSize(new Dimension(280, 22));

        add(lblHeader);
    }

    /** Hàm `addReportItem`: thêm baocao mathang. */
    private void addReportItem(String title, Consumer<String> onSelectPrintAction) {
        JMenuItem mi = new JMenuItem(title);
        mi.setFont(UngDungPhongChu.BODY);
        mi.setForeground(new Color(20, 25, 30));
        mi.setBackground(Color.WHITE);
        mi.setPreferredSize(new Dimension(280, 22));
        mi.setBorder(new EmptyBorder(2, 14, 2, 8));
        mi.setCursor(new Cursor(Cursor.HAND_CURSOR));

        mi.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mi.setBackground(new Color(220, 235, 250)); // Nền xanh nhạt khi rê chuột
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mi.setBackground(Color.WHITE);
            }
        });

        mi.addActionListener(e -> {
            if (onSelectPrintAction != null) {
                onSelectPrintAction.accept(title);
            }
        });

        add(mi);
    }
}
