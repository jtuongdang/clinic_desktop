package vn.clinicmanagement.desktop.modules.doctor.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.modules.doctor.model.PatientExamDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Lịch sử khám bệnh đã xác thực của bệnh nhân
 */
public class DoctorHistoryPanel extends JPanel {

    private static final String NO_VERIFIED_HISTORY_MESSAGE =
            "Chưa có dữ liệu lịch sử khám đã xác thực.";
    private final JLabel historyStatus;

    public DoctorHistoryPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 218, 226)));

        JLabel lblHistTitle = new JLabel("Lịch Sử Khám và Điều Trị");
        lblHistTitle.setFont(AppFonts.BODY_BOLD);
        lblHistTitle.setForeground(new Color(24, 75, 120));
        lblHistTitle.setBorder(new EmptyBorder(6, 4, 4, 4));
        add(lblHistTitle, BorderLayout.NORTH);

        historyStatus = new JLabel(NO_VERIFIED_HISTORY_MESSAGE, SwingConstants.CENTER);
        historyStatus.setFont(AppFonts.BODY);
        historyStatus.setForeground(new Color(100, 110, 120));
        historyStatus.setBorder(new EmptyBorder(16, 8, 16, 8));
        add(historyStatus, BorderLayout.CENTER);
    }

    public void updateHistory(PatientExamDto patient) {
        historyStatus.setText(NO_VERIFIED_HISTORY_MESSAGE);
    }
}
