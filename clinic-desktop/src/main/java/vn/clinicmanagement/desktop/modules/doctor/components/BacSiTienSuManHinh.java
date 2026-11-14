package vn.clinicmanagement.desktop.modules.doctor.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.components.CoKieuBang;
import vn.clinicmanagement.desktop.modules.doctor.model.BenhNhanKhamDuLieu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Bảng lịch sử khám bệnh và điều trị của bệnh nhân
 */
public class BacSiTienSuManHinh extends JPanel {

    /** Trường `historyTableModel`: tiensu bảng mô hình. */
    private final DefaultTableModel historyTableModel;
    /** Trường `historyTable`: tiensu bảng. */
    private final JTable historyTable;

    public BacSiTienSuManHinh() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 218, 226)));

        JLabel lblHistTitle = new JLabel("Lịch Sử Khám và Điều Trị");
        lblHistTitle.setFont(UngDungPhongChu.BODY_BOLD);
        lblHistTitle.setForeground(new Color(24, 75, 120));
        lblHistTitle.setBorder(new EmptyBorder(6, 4, 4, 4));
        add(lblHistTitle, BorderLayout.NORTH);

        String[] hCols = {"Ngày Vào", "Hình Thức Vào", "Khoa", "Phòng", "Ngày Ra", "Xử Trí", "Dịch", "Chẩn Đoán"};
        historyTableModel = new DefaultTableModel(hCols, 0) {
            /** Hàm `isCellEditable`: là `Cell` `Editable`. */
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        historyTable = CoKieuBang.create(historyTableModel);
        historyTable.setFont(UngDungPhongChu.BODY);
        historyTable.setRowHeight(24);

        JScrollPane histScroll = new JScrollPane(historyTable);
        histScroll.setPreferredSize(new Dimension(500, 160));
        histScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 226, 234)));
        add(histScroll, BorderLayout.CENTER);
    }

    /** Hàm `updateHistory`: cập nhật tiensu. */
    public void updateHistory(BenhNhanKhamDuLieu p) {
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
