package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import vn.clinicmanagement.application.examination.KhamBenhLichSuMatHang;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.desktop.modules.examination.components.KhamBenhManHinhIconVaRenderer;
import vn.clinicmanagement.desktop.modules.examination.components.KhamBenhManHinhIconVaRenderer.CheckBoxMiniIcon;
import vn.clinicmanagement.desktop.modules.examination.components.KhamBenhManHinhIconVaRenderer.HistoryStatusDotComponent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Nội dung Tab "Bệnh Nhân" trên màn hình Khám Bệnh: lưới hành chính 8 hàng và bảng Lịch Sử Khám và Điều Trị.
 */
public final class KhamBenhBenhNhanTheManHinh extends JPanel {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final JLabel lblMaBN;
    private final JLabel lblMaBA;
    private final JLabel lblTenBN;
    private final JLabel lblGioiTinh;
    private final JLabel lblNgaySinh;
    private final JLabel lblTuoi;
    private final JLabel lblDanToc;
    private final JLabel lblNgheNghiep;
    private final JLabel lblQuocTich;
    private final JLabel lblDienThoai;
    private final JLabel lblDiaChi;
    private final JLabel lblCmnd;
    private final JLabel lblDonTiepLuc;
    private final JLabel lblLoaiDonTiep;
    private final JLabel lblDoiTuong;
    private final JLabel lblSoVaoKhoa;
    private final JLabel lblMaDieuTri;
    private final JLabel lblLyDoKham;
    private final JLabel lblKhoaPhong;
    private final JLabel lblBacSiKham;
    private final JLabel lblBenhAn;
    private final JLabel lblNgayVao;
    private final JLabel lblNgayRa;
    private final JLabel lblXuTri;
    private final JLabel lblChanDoan;

    private final DefaultTableModel historyModel;

    public KhamBenhBenhNhanTheManHinh() {
        setLayout(new BorderLayout(0, 4));
        setBackground(new Color(248, 250, 253));
        setBorder(new EmptyBorder(4, 6, 4, 6));

        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setBackground(new Color(248, 250, 253));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 3, 1, 3);
        c.anchor = GridBagConstraints.WEST;

        int r = 0;

        lblMaBN = createValueLabel("", true);
        lblMaBA = createValueLabel("", true);
        lblTenBN = createValueLabel("", true);
        addRow(formGrid, c, r++,
                "Mã BN:", lblMaBN, 1,
                "Mã Bệnh Án:", lblMaBA, 1,
                "Bệnh nhân:", lblTenBN, 3,
                null, null, 0);

        lblGioiTinh = createValueLabel("", true);
        lblNgaySinh = createValueLabel("", true);
        lblTuoi = createValueLabel("", true);
        lblDanToc = createValueLabel("", true);
        addRow(formGrid, c, r++,
                "Giới tính:", lblGioiTinh, 1,
                "Ngày sinh:", lblNgaySinh, 1,
                "Tuổi:", lblTuoi, 1,
                "Dân tộc:", lblDanToc, 1);

        lblNgheNghiep = createValueLabel("", true);
        lblQuocTich = createValueLabel("", true);
        lblDienThoai = createValueLabel("", true);
        addRow(formGrid, c, r++,
                "Nghề nghiệp:", lblNgheNghiep, 1,
                "Quốc tịch:", lblQuocTich, 1,
                "Điện thoại:", lblDienThoai, 3,
                null, null, 0);

        lblDiaChi = createValueLabel("", true);
        lblCmnd = createValueLabel("", false);
        addRow(formGrid, c, r++,
                "Địa chỉ:", lblDiaChi, 3,
                "CMND/CCCD:", lblCmnd, 3,
                null, null, 0,
                null, null, 0);

        lblDonTiepLuc = createValueLabel("", true);
        lblLoaiDonTiep = createValueLabel("", true);
        lblDoiTuong = createValueLabel("", true);
        addRow(formGrid, c, r++,
                "Đón tiếp lúc:", lblDonTiepLuc, 1,
                "Loại đón tiếp:", lblLoaiDonTiep, 1,
                "Đối tượng:", lblDoiTuong, 3,
                null, null, 0);

        JLabel lblSoVaoVien = createValueLabel("", false);
        lblSoVaoKhoa = createValueLabel("", true);
        lblMaDieuTri = createValueLabel("", true);
        lblLyDoKham = createValueLabel("", true);
        addRow(formGrid, c, r++,
                "Số vào viện:", lblSoVaoVien, 1,
                "Số vào khoa:", lblSoVaoKhoa, 1,
                "Mã điều trị:", lblMaDieuTri, 1,
                "Lý do khám:", lblLyDoKham, 1);

        lblKhoaPhong = createValueLabel("", true);
        lblBacSiKham = createValueLabel("", true);
        JLabel lblDieuDuong = createValueLabel("", false);
        addRow(formGrid, c, r++,
                "Khoa phòng:", lblKhoaPhong, 3,
                "Bác sĩ khám:", lblBacSiKham, 1,
                "Điều dưỡng:", lblDieuDuong, 1,
                null, null, 0);

        lblBenhAn = createValueLabel("", true);
        lblNgayVao = createValueLabel("", true);
        lblNgayRa = createValueLabel("", true);
        lblXuTri = createValueLabel("", true);
        addRow(formGrid, c, r++,
                "Bệnh án:", lblBenhAn, 1,
                "Ngày vào:", lblNgayVao, 1,
                "Ngày ra:", lblNgayRa, 1,
                "Xử trí:", lblXuTri, 1);

        lblChanDoan = createValueLabel("", true);
        addRow(formGrid, c, r,
                "Chẩn đoán:", lblChanDoan, 3,
                null, null, 0,
                null, null, 0,
                null, null, 0);

        add(formGrid, BorderLayout.NORTH);

        // Dải tab lịch sử phía dưới
        JTabbedPane bottomTabs = KhamBenhManHinhIconVaRenderer.createStyledTabbedPane();
        String[] cols = {"", "Ngày Vào", "Hình Thức Vào", "Khoa", "Phòng", "Thẻ BHYT", "Ngày Sinh", "Ngày Ra", "Xử Trí"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        bottomTabs.addTab("Lịch Sử Khám và Điều Trị", buildHistoryTablePanel());
        bottomTabs.addTab("Thông Tin Xác Thực", createPlaceholderTab("Thông tin thẻ BHYT & xác thực CCCD"));
        add(bottomTabs, BorderLayout.CENTER);
    }

    private JPanel buildHistoryTablePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);

        JTable historyTable = new JTable(historyModel) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int rowH = getRowHeight();
                int paintedH = getRowCount() * rowH;
                int totalH = getParent() != null ? getParent().getHeight() : getHeight();
                if (totalH > paintedH && rowH > 0) {
                    g.setColor(new Color(238, 241, 245));
                    for (int y = paintedH; y < totalH; y += rowH) {
                        g.drawLine(0, y, getWidth(), y);
                    }
                    int x = 0;
                    for (int i = 0; i < getColumnCount(); i++) {
                        x += getColumnModel().getColumn(i).getWidth();
                        g.drawLine(x - 1, paintedH, x - 1, totalH);
                    }
                }
            }

            @Override
            public boolean getScrollableTracksViewportHeight() {
                return getPreferredSize().height < getParent().getHeight();
            }
        };

        historyTable.setFont(UngDungPhongChu.SMALL);
        historyTable.setRowHeight(24);
        historyTable.setShowGrid(true);
        historyTable.setGridColor(new Color(235, 239, 244));
        historyTable.setBackground(Color.WHITE);
        historyTable.setSelectionBackground(new Color(215, 232, 250));
        historyTable.setSelectionForeground(Color.BLACK);

        JTableHeader header = historyTable.getTableHeader();
        header.setPreferredSize(new Dimension(100, 25));
        header.setFont(UngDungPhongChu.SMALL_BOLD);
        header.setBackground(new Color(242, 245, 250));
        header.setForeground(new Color(25, 25, 25));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 216, 224)));

        int[] colWidths = {26, 125, 105, 130, 165, 110, 95, 95, 95};
        for (int i = 0; i < colWidths.length; i++) {
            historyTable.getColumnModel().getColumn(i).setPreferredWidth(colWidths[i]);
        }
        historyTable.getColumnModel().getColumn(0).setMaxWidth(28);

        historyTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                return new HistoryStatusDotComponent(isSelected, value != null ? value.toString() : "");
            }
        });

        historyTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setIcon(new CheckBoxMiniIcon());
                lbl.setText(". " + (value != null ? value.toString() : ""));
                lbl.setFont(UngDungPhongChu.SMALL);
                return lbl;
            }
        });

        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setColumnHeaderView(historyTable.getTableHeader());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(205, 214, 226)));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    /** Gắn thông tin hành chính và trạng thái lượt khám hiện tại của bệnh nhân. */
    public void bindPatient(AdminPatientItem p) {
        if (p == null) return;
        lblMaBN.setText(p.code);
        lblMaBA.setText(p.medicalRecordCode);
        lblTenBN.setText(p.name);
        lblGioiTinh.setText(p.gender);
        lblNgaySinh.setText(p.birthDate);
        lblTuoi.setText(p.formAge);
        lblDanToc.setText(p.ethnicity != null && !p.ethnicity.isBlank() ? p.ethnicity : "Kinh");
        lblNgheNghiep.setText(p.occupation != null ? p.occupation : "");
        lblQuocTich.setText(p.nationality != null && !p.nationality.isBlank() ? p.nationality : "Việt Nam");
        lblDienThoai.setText(p.phone);
        lblDiaChi.setText(p.address);
        lblCmnd.setText(p.cccd != null ? p.cccd : "");
        lblDonTiepLuc.setText(p.admissionDate);
        lblLoaiDonTiep.setText(p.registrationType != null && !p.registrationType.isBlank() ? p.registrationType : "Khám bệnh");
        lblDoiTuong.setText(p.requestType);
        lblSoVaoKhoa.setText("KKB/" + p.code);
        lblMaDieuTri.setText(p.invoiceCode);
        lblLyDoKham.setText(p.diagnosis != null ? p.diagnosis : "");
        lblKhoaPhong.setText(p.department);
        lblBacSiKham.setText(p.doctor);
        lblBenhAn.setText("Khám đa khoa");
        lblNgayVao.setText(p.admissionDate);
        lblNgayRa.setText(p.dischargeTime != null ? p.dischargeTime : "");
        lblXuTri.setText(KhamBenhManHinh.formatVisitStatus(p.visitStatus));
        lblChanDoan.setText(p.diagnosis);
    }

    /** Đổ danh sách lượt khám lịch sử từ DB (hoặc fallback từ item hiện tại nếu chưa có dữ liệu DB). */
    public void setVisitHistory(AdminPatientItem p, List<KhamBenhLichSuMatHang> historyRows) {
        historyModel.setRowCount(0);
        if (p == null) {
            return;
        }
        if (historyRows != null && !historyRows.isEmpty()) {
            for (KhamBenhLichSuMatHang row : historyRows) {
                String admission = row.admissionTime() != null ? row.admissionTime().format(DATE_TIME_FORMATTER) : "";
                String discharge = row.dischargeTime() != null ? row.dischargeTime().format(DATE_TIME_FORMATTER) : "";
                String visitBhyt = "BHYT".equalsIgnoreCase(row.payerType()) ? row.bhytNumber() : "";
                historyModel.addRow(new Object[]{
                        row.visitStatus() != null ? row.visitStatus() : "",
                        admission,
                        row.registrationType(),
                        row.departmentName(),
                        row.clinicRoomName(),
                        visitBhyt,
                        p.birthDate,
                        discharge,
                        KhamBenhManHinh.formatVisitStatus(row.visitStatus())
                });
            }
            return;
        }
        String regType = p.registrationType != null && !p.registrationType.isBlank() ? p.registrationType : "Khám bệnh";
        String deptName = p.departmentName != null && !p.departmentName.isBlank() ? p.departmentName : "Khoa Khám Bệnh";
        String roomDisplay = p.clinicRoomName != null && !p.clinicRoomName.isBlank() ? p.clinicRoomName : p.department;
        historyModel.addRow(new Object[]{
                p.visitStatus,
                p.admissionDate,
                regType,
                deptName,
                roomDisplay,
                p.bhytNumber != null ? p.bhytNumber : "",
                p.birthDate,
                p.dischargeTime != null ? p.dischargeTime : "",
                KhamBenhManHinh.formatVisitStatus(p.visitStatus)
        });
    }

    public void clearHistory() {
        historyModel.setRowCount(0);
    }

    public void setRecordType(String recordType) {
        lblBenhAn.setText(recordType);
    }

    public DefaultTableModel getHistoryModel() {
        return historyModel;
    }

    private static void addRow(JPanel panel, GridBagConstraints c, int row,
                               String l1, JComponent v1, int span1,
                               String l2, JComponent v2, int span2,
                               String l3, JComponent v3, int span3,
                               String l4, JComponent v4, int span4) {
        c.gridy = row;
        int col = 0;

        String[] labels = {l1, l2, l3, l4};
        JComponent[] vals = {v1, v2, v3, v4};
        int[] spans = {span1, span2, span3, span4};

        for (int i = 0; i < 4; i++) {
            if (labels[i] == null || vals[i] == null) continue;
            c.gridx = col++;
            c.gridwidth = 1;
            c.weightx = 0;
            panel.add(createFieldTitle(labels[i]), c);

            c.gridx = col;
            c.gridwidth = spans[i];
            c.weightx = spans[i] >= 3 ? 0.50 : 0.25;
            panel.add(vals[i], c);
            col += spans[i];
        }
        c.gridwidth = 1;
    }

    private static JLabel createFieldTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UngDungPhongChu.SMALL);
        l.setForeground(new Color(45, 45, 45));
        l.setPreferredSize(new Dimension(76, 18));
        return l;
    }

    private static JLabel createValueLabel(String text, boolean isNavy) {
        JLabel l = new JLabel(text);
        l.setFont(UngDungPhongChu.SMALL);
        l.setForeground(isNavy ? new Color(0, 65, 155) : Color.BLACK);
        l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 215, 222)));
        l.setPreferredSize(new Dimension(95, 18));
        return l;
    }

    private static JPanel createPlaceholderTab(String desc) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        JLabel lbl = new JLabel(desc, SwingConstants.CENTER);
        lbl.setFont(UngDungPhongChu.BODY);
        lbl.setForeground(Color.GRAY);
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }
}
