package vn.clinicmanagement.desktop.modules.reception.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Thanh công cụ đỉnh phân hệ Tiếp Đón & Đăng Ký Khám Bệnh.
 * Khớp 100% chi tiết trong ảnh chụp mẫu:
 * - 6 nút hành động dọc (Icon phía trên, tên chức năng bên dưới)
 * - Ô vàng hiển thị "Cửa đón tiếp" [ 2 ]
 * - Nút loa phát thanh "Gọi Số Tiếp Theo"
 */
public class ReceptionTopToolbar extends JPanel {

    public ReceptionTopToolbar() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1000, 50));
        setBackground(new Color(238, 243, 248));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(195, 208, 222)));

        // --- CỤM NÚT TRÁI (Icon trên, chữ dưới) ---
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 3));
        leftGroup.setOpaque(false);

        leftGroup.add(createActionButton("Menu", ReceptionIcons.createMenuIcon()));
        leftGroup.add(createActionButton("In", ReceptionIcons.createPrintIcon()));
        leftGroup.add(createActionButton("Làm Mới", ReceptionIcons.createRefreshIcon()));
        leftGroup.add(createActionButton("Bệnh Nhân Mới", ReceptionIcons.createNewPatientIcon()));
        leftGroup.add(createActionButton("Nhập Thẻ BH", ReceptionIcons.createAddInsuranceIcon()));
        leftGroup.add(createActionButton("Kiểm Tra Thẻ BH", ReceptionIcons.createVerifyCardIcon()));

        // --- CỤM CỬA ĐÓN TIẾP & GỌI SỐ BÊN PHẢI ---
        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        rightGroup.setOpaque(false);

        // Hộp Cửa đón tiếp [ 2 ]
        JPanel counterBox = new JPanel();
        counterBox.setLayout(new BoxLayout(counterBox, BoxLayout.Y_AXIS));
        counterBox.setOpaque(false);

        JLabel lblCounterTitle = new JLabel("Cửa đón tiếp", SwingConstants.CENTER);
        lblCounterTitle.setFont(AppFonts.SMALL);
        lblCounterTitle.setForeground(new Color(30, 30, 30));
        lblCounterTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblCounterNum = new JLabel("2", SwingConstants.CENTER);
        lblCounterNum.setFont(new Font(AppFonts.FAMILY, Font.BOLD, 15));
        lblCounterNum.setOpaque(true);
        lblCounterNum.setBackground(new Color(255, 255, 60)); // Nền vàng rực đặc trưng
        lblCounterNum.setForeground(Color.BLACK);
        lblCounterNum.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        lblCounterNum.setPreferredSize(new Dimension(54, 20));
        lblCounterNum.setMaximumSize(new Dimension(54, 20));
        lblCounterNum.setAlignmentX(Component.CENTER_ALIGNMENT);

        counterBox.add(lblCounterTitle);
        counterBox.add(Box.createVerticalStrut(2));
        counterBox.add(lblCounterNum);

        // Nút Gọi Số Tiếp Theo (kèm Loa phát thanh)
        JPanel callBox = new JPanel();
        callBox.setLayout(new BoxLayout(callBox, BoxLayout.Y_AXIS));
        callBox.setOpaque(false);
        callBox.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblSpeaker = new JLabel(ReceptionIcons.createMegaphoneIcon(), SwingConstants.CENTER);
        lblSpeaker.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblCallText = new JLabel("Gọi Số Tiếp Theo", SwingConstants.CENTER);
        lblCallText.setFont(AppFonts.SMALL);
        lblCallText.setForeground(new Color(90, 100, 110));
        lblCallText.setAlignmentX(Component.CENTER_ALIGNMENT);

        callBox.add(lblSpeaker);
        callBox.add(Box.createVerticalStrut(2));
        callBox.add(lblCallText);

        callBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JOptionPane.showMessageDialog(ReceptionTopToolbar.this,
                        "Hệ thống tự động phát loa gọi số thứ tự tiếp theo tại Cửa số 2:\n" +
                        "\"Xin mời số thứ tự 22000326 vào cửa số 2 tiếp đón!\"",
                        "Gọi Số Đón Tiếp Tự Động", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        rightGroup.add(counterBox);
        rightGroup.add(callBox);

        add(leftGroup, BorderLayout.WEST);
        add(rightGroup, BorderLayout.EAST);
    }

    private JButton createActionButton(String title, Icon icon) {
        JButton btn = new JButton(title, icon);
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setFont(AppFonts.SMALL);
        btn.setForeground(new Color(20, 30, 40));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(2, 0, 2, 0));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(175, 195, 215), 1),
                        new EmptyBorder(1, 5, 1, 5)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            }
        });

        return btn;
    }
}
