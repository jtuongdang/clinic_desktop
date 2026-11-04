package vn.clinicmanagement.desktop.modules.examination.components.menus;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Menu xổ xuống (Dropdown Menu) khi nhấn nút "Thuốc/VT ▾" trên thanh công cụ khám bệnh.
 * Khớp 100% chi tiết trong ảnh chụp mẫu hệ thống:
 * - Nhóm 1: Chỉ Định
 *   + Chỉ Định Thuốc        Ctrl+T
 *   + Chỉ Định Vật Tư
 *   + Chỉ Định Thực Phẩm Chức Năng
 * - Đường kẻ phân cách
 * - Nhóm 2: Hoàn Trả
 *   + Hoàn Trả Thuốc
 *   + Hoàn Trả Vật Tư
 */
public class MedicineSuppliesPopupMenu extends JPopupMenu {

    public MedicineSuppliesPopupMenu(Component parent, Consumer<String> onSelectAction) {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(new Color(135, 148, 165), 1));

        // --- NHÓM 1: CHỈ ĐỊNH ---
        addSectionHeader("Chỉ Định");
        addMenuItemWithShortcut("Chỉ Định Thuốc", "Ctrl+T", onSelectAction);
        addMenuItemWithShortcut("Chỉ Định Vật Tư", null, onSelectAction);
        addMenuItemWithShortcut("Chỉ Định Thực Phẩm Chức Năng", null, onSelectAction);

        // Đường kẻ phân cách nhóm
        addSeparator();

        // --- NHÓM 2: HOÀN TRẢ ---
        addSectionHeader("Hoàn Trả");
        addMenuItemWithShortcut("Hoàn Trả Thuốc", null, onSelectAction);
        addMenuItemWithShortcut("Hoàn Trả Vật Tư", null, onSelectAction);
    }

    private void addSectionHeader(String title) {
        JLabel lblHeader = new JLabel(title);
        lblHeader.setFont(AppFonts.SMALL_BOLD);
        lblHeader.setForeground(new Color(35, 45, 60));
        lblHeader.setHorizontalAlignment(SwingConstants.LEFT);
        lblHeader.setOpaque(true);
        lblHeader.setBackground(new Color(236, 241, 248)); // Nền xám xanh nhạt phân nhóm chuẩn EHC
        lblHeader.setBorder(new EmptyBorder(3, 12, 3, 10));
        lblHeader.setPreferredSize(new Dimension(230, 22));
        lblHeader.setMaximumSize(new Dimension(Short.MAX_VALUE, 22));
        lblHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(lblHeader);
    }

    private void addMenuItemWithShortcut(String title, String shortcut, Consumer<String> onSelectAction) {
        JMenuItem mi = new JMenuItem();
        mi.setLayout(new BorderLayout());
        mi.setBackground(Color.WHITE);
        mi.setPreferredSize(new Dimension(230, 23));
        mi.setMaximumSize(new Dimension(Short.MAX_VALUE, 23));
        mi.setBorder(new EmptyBorder(2, 18, 2, 12));
        mi.setCursor(new Cursor(Cursor.HAND_CURSOR));
        mi.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(AppFonts.BODY);
        lblTitle.setForeground(new Color(20, 25, 30));

        mi.add(lblTitle, BorderLayout.WEST);

        if (shortcut != null && !shortcut.isEmpty()) {
            JLabel lblShortcut = new JLabel(shortcut);
            lblShortcut.setFont(AppFonts.SMALL);
            lblShortcut.setForeground(new Color(40, 40, 40));
            mi.add(lblShortcut, BorderLayout.EAST);
        }

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
            if (onSelectAction != null) {
                onSelectAction.accept(title);
            }
        });

        add(mi);
    }

    @Override
    public void addSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(new Color(210, 218, 228));
        sep.setBackground(Color.WHITE);
        sep.setPreferredSize(new Dimension(230, 2));
        sep.setMaximumSize(new Dimension(Short.MAX_VALUE, 2));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(sep);
    }
}
