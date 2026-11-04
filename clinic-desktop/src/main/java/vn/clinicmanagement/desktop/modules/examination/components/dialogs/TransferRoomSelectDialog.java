package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Hộp thoại Chọn Phòng Khám Chuyển Đến:
 * "EHC - Chuyển Phòng Khám  --- Người đăng nhập: hienvv --  --- Version: 2022.3.11.1"
 * Khớp 100% media_1793381550077_6c743cbd.jpg
 */
public class TransferRoomSelectDialog extends JDialog {

    public static final String[] ROOM_LIST = {
            "Cấp cứu nội",
            "Cấp cứu sản",
            "Phòng khám hỗ trợ sinh sản - nam học",
            "Phòng Q quản lý HS",
            "Phòng Tiêm Chủng",
            "PK cách ly",
            "PK ĐT ngoại trú tâm thần",
            "PK Hẹn",
            "PK Hỗ trợ Sinh sản - Nam học",
            "PK Mắt",
            "PK Ngoại nhi",
            "PK Nhi 2",
            "PK Nhi 3",
            "PK Nhi 3 [T2,T3]",
            "PK Nhi 3 [T3,T4]",
            "PK Nhi 3 [T4]"
    };

    private String selectedRoom = null;
    private final JComboBox<String> cboRoom;

    public TransferRoomSelectDialog(Window owner) {
        super(owner, "EHC - Chuyển Phòng Khám  --- Người đăng nhập: hienvv --  --- Version: 2022.3.11.1", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setPreferredSize(new Dimension(460, 130));
        panel.setBackground(new Color(245, 246, 248));
        panel.setBorder(new EmptyBorder(16, 16, 14, 16));

        JLabel lblTitle = new JLabel("Chuyển bệnh nhân sang phòng khám");
        lblTitle.setFont(AppFonts.SMALL);
        lblTitle.setForeground(new Color(30, 35, 45));
        panel.add(lblTitle, BorderLayout.NORTH);

        cboRoom = new JComboBox<>(ROOM_LIST);
        cboRoom.setFont(AppFonts.SMALL);
        cboRoom.setBackground(Color.WHITE);
        cboRoom.setSelectedItem(ROOM_LIST[0]);
        cboRoom.setPreferredSize(new Dimension(420, 28));
        panel.add(cboRoom, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomBar.setOpaque(false);

        JButton btnAccept = new JButton("Lưu", new TransferRoomConfirmDialog.GreenCheckIcon());
        btnAccept.setFont(AppFonts.SMALL);
        btnAccept.setPreferredSize(new Dimension(84, 26));
        btnAccept.setBackground(Color.WHITE);
        btnAccept.addActionListener(e -> {
            selectedRoom = (String) cboRoom.getSelectedItem();
            dispose();
        });

        JButton btnCancel = new JButton("Đóng (Esc)", new TransferRoomConfirmDialog.RedCrossMiniIcon());
        btnCancel.setFont(AppFonts.SMALL);
        btnCancel.setPreferredSize(new Dimension(96, 26));
        btnCancel.setBackground(Color.WHITE);
        btnCancel.addActionListener(e -> {
            selectedRoom = null;
            dispose();
        });

        bottomBar.add(btnAccept);
        bottomBar.add(btnCancel);
        panel.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(panel);
        pack();
        setLocationRelativeTo(owner);
    }

    public static String selectRoom(Component parent) {
        Window win = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        TransferRoomSelectDialog dlg = new TransferRoomSelectDialog(win);
        dlg.setVisible(true);
        return dlg.getSelectedRoom();
    }

    public String getSelectedRoom() {
        return selectedRoom;
    }
}
