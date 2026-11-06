package vn.clinicmanagement.desktop.modules.doctor.components;

import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.core.components.StyledTable;
import vn.clinicmanagement.desktop.modules.doctor.model.PatientExamDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Bảng lịch sử khám bệnh và điều trị của bệnh nhân
 */
public class DoctorHistoryPanel extends JPanel {

    private final DefaultTableModel historyTableModel;
    private final JTable historyTable;

    public DoctorHistoryPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 218, 226)));

        JLabel lblHistTitle = new JLabel("Lịch Sử Khám và Điều Trị");
        lblHistTitle.setFont(AppFonts.BODY_BOLD);
        lblHistTitle.setForeground(new Color(24, 75, 120));
        lblHistTitle.setBorder(new EmptyBorder(6, 4, 4, 4));
        add(lblHistTitle, BorderLayout.NORTH);

        String[] hCols = {"Ngày Vào", "Hình Thức Vào", "Khoa", "Phòng", "Ngày Ra", "Xử Trí", "Dịch", "Chẩn Đoán"};
        historyTableModel = new DefaultTableModel(hCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        historyTable = StyledTable.create(historyTableModel);
        historyTable.setFont(AppFonts.BODY);
        historyTable.setRowHeight(24);

        JScrollPane histScroll = new JScrollPane(historyTable);
        histScroll.setPreferredSize(new Dimension(500, 160));
        histScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 226, 234)));
        add(histScroll, BorderLayout.CENTER);
    }

    public void updateHistory(PatientExamDto p) {
        if (p == null) return;
        historyTableModel.setRowCount(0);
        historyTableModel.addRow(new Object[]{
                p.getAdmissionDate(), "Nhập viện", p.getDepartment().split(" > ")[0], "P.Điều Trị Ngoại", "", "Đang điều trị", "", p.getDiagnosis()
        });
        historyTableModel.addRow(new Object[]{
                "13:42 07/11/2018", "Trực tiếp", "Khoa Khám bệnh", "P. Khám Tổng Hợp", "07:23 07/11/2018", "Nhập viện", "Ngoại Đầu cổ", p.getDiagnosis()
        });
    }
}
