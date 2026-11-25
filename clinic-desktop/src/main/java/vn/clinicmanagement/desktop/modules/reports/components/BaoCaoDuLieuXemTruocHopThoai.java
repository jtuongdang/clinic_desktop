package vn.clinicmanagement.desktop.modules.reports.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.reports.catalog.BaoCaoMau;
import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Hộp thoại xem trước & xuất dữ liệu báo cáo thật từ SQL Server
 * cho các mẫu trong Danh Sách Báo Cáo.
 */
public class BaoCaoDuLieuXemTruocHopThoai extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final BaoCaoMau report;
    private final JTextField txtFrom = new JTextField(LocalDate.now().minusDays(30).format(DATE_FMT), 10);
    private final JTextField txtTo = new JTextField(LocalDate.now().format(DATE_FMT), 10);
    private final JLabel lblCount = new JLabel("Đang tải dữ liệu...");
    private final DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);

    public BaoCaoDuLieuXemTruocHopThoai(Window owner, BaoCaoMau report) {
        super(owner, "Dữ Liệu Báo Cáo: " + report.name(), ModalityType.APPLICATION_MODAL);
        this.report = report;
        initUI();
        loadData();
    }

    private void initUI() {
        setSize(980, 620);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(Color.WHITE);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.setBackground(new Color(246, 248, 252));
        top.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)));

        JLabel title = new JLabel(report.name() + (report.reportId().isBlank() ? "" : " (ID: " + report.reportId() + ")"));
        title.setFont(UngDungPhongChu.BODY_BOLD);
        title.setForeground(new Color(20, 75, 145));

        JButton btnReload = new JButton("Xem Dữ Liệu");
        btnReload.setFont(UngDungPhongChu.SMALL_BOLD);
        btnReload.addActionListener(e -> loadData());

        JButton btnExport = new JButton("Xuất Excel / CSV");
        btnExport.setFont(UngDungPhongChu.SMALL_BOLD);
        btnExport.setForeground(new Color(20, 120, 55));
        btnExport.addActionListener(e -> exportCsv());

        top.add(title);
        top.add(new JLabel("   Từ ngày:"));
        top.add(txtFrom);
        top.add(new JLabel("Đến ngày:"));
        top.add(txtTo);
        top.add(btnReload);
        top.add(btnExport);
        add(top, BorderLayout.NORTH);

        table.setRowHeight(24);
        table.setFont(UngDungPhongChu.BODY);
        table.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 218, 228)),
                new EmptyBorder(6, 12, 6, 12)));
        lblCount.setFont(UngDungPhongChu.SMALL);
        bottom.add(lblCount, BorderLayout.WEST);

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(UngDungPhongChu.SMALL);
        btnClose.addActionListener(e -> dispose());
        bottom.add(btnClose, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    private void loadData() {
        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = LocalDate.parse(txtFrom.getText().trim(), DATE_FMT);
            toDate = LocalDate.parse(txtTo.getText().trim(), DATE_FMT);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ngày phải có định dạng yyyy-MM-dd.", "Lỗi định dạng", JOptionPane.WARNING_MESSAGE);
            return;
        }
        lblCount.setText("Đang truy vấn CSDL...");
        String sql = resolveQuery(report);

        new SwingWorker<Void, Void>() {
            private String[] columns = {};
            private java.util.List<Object[]> rows = new java.util.ArrayList<>();
            private String error;

            @Override protected Void doInBackground() {
                try (Connection c = CoSoDuLieuKetNoi.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setDate(1, java.sql.Date.valueOf(fromDate));
                    ps.setDate(2, java.sql.Date.valueOf(toDate.plusDays(1)));
                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int colCount = meta.getColumnCount();
                        columns = new String[colCount];
                        for (int i = 0; i < colCount; i++) {
                            columns[i] = meta.getColumnLabel(i + 1);
                        }
                        while (rs.next()) {
                            Object[] r = new Object[colCount];
                            for (int i = 0; i < colCount; i++) r[i] = rs.getObject(i + 1);
                            rows.add(r);
                        }
                    }
                } catch (Exception ex) {
                    error = ex.getMessage();
                }
                return null;
            }

            @Override protected void done() {
                if (error != null) {
                    lblCount.setText("Lỗi truy vấn: " + error);
                    return;
                }
                model.setDataVector(rows.toArray(new Object[0][]), columns);
                lblCount.setText("Tổng số dòng từ CSDL: " + rows.size());
            }
        }.execute();
    }

    private static String resolveQuery(BaoCaoMau r) {
        String keyText = (r.name() + " " + r.group() + " " + r.fileName()).toLowerCase(Locale.ROOT);
        if (keyText.contains("lịch hẹn") || keyText.contains("datlichhen")) {
            return "SELECT lh.id AS [Mã Hẹn], bn.maBenhNhanFormat AS [Mã BN], bn.ho + ' ' + bn.ten AS [Họ Tên], "
                    + "bn.dienThoai AS [SĐT], lh.ngayHen AS [Ngày Hẹn], lh.trangThai AS [Trạng Thái], lh.ghiChu AS [Ghi Chú] "
                    + "FROM dbo.LichHen lh JOIN dbo.BenhNhan bn ON lh.maBenhNhan = bn.id "
                    + "WHERE lh.isDeleted = 0 AND lh.ngayHen >= ? AND lh.ngayHen < ? ORDER BY lh.ngayHen DESC";
        }
        if (keyText.contains("thu chi") || keyText.contains("thu tiền") || keyText.contains("hoàn tiền")
                || keyText.contains("tạm ứng") || keyText.contains("hoàn ứng") || keyText.contains("miễn giảm")
                || keyText.contains("thanh toán")) {
            return "SELECT hd.id AS [Mã HĐ], bn.maBenhNhanFormat AS [Mã BN], bn.ho + ' ' + bn.ten AS [Bệnh Nhân], "
                    + "hd.tongTien AS [Tổng Tiền], hd.daThanhToan AS [Đã Thu], hd.trangThai AS [Trạng Thái], hd.createdAt AS [Ngày Lập] "
                    + "FROM dbo.HoaDon hd JOIN dbo.LuotKham lk ON hd.maLuotKham = lk.id "
                    + "JOIN dbo.BenhNhan bn ON lk.maBenhNhan = bn.id "
                    + "WHERE hd.isDeleted = 0 AND hd.createdAt >= ? AND hd.createdAt < ? ORDER BY hd.id DESC";
        }
        if (keyText.contains("đơn thuốc")) {
            return "SELECT dt.id AS [Mã Đơn], bn.maBenhNhanFormat AS [Mã BN], bn.ho + ' ' + bn.ten AS [Bệnh Nhân], "
                    + "mh.tenItem AS [Tên Thuốc], ct.soLuongKe AS [SL], ct.duongDung AS [Đường Dùng], dt.trangThai AS [Trạng Thái], dt.ngayKe AS [Ngày Kê] "
                    + "FROM dbo.DonThuoc dt JOIN dbo.ChiTietDonThuoc ct ON dt.id = ct.maDonThuoc AND ct.isDeleted = 0 "
                    + "JOIN dbo.MatHang mh ON ct.maItem = mh.id "
                    + "JOIN dbo.LuotKham lk ON dt.maLuotKham = lk.id "
                    + "JOIN dbo.BenhNhan bn ON lk.maBenhNhan = bn.id "
                    + "WHERE dt.isDeleted = 0 AND dt.ngayKe >= ? AND dt.ngayKe < ? ORDER BY dt.id DESC";
        }
        if (keyText.contains("xét nghiệm") || keyText.contains("cận lâm sàng") || keyText.contains("labo") || keyText.contains("chỉ định")) {
            return "SELECT cd.id AS [Mã CĐ], bn.maBenhNhanFormat AS [Mã BN], bn.ho + ' ' + bn.ten AS [Bệnh Nhân], "
                    + "dv.tenDichVu AS [Dịch Vụ], cd.trangThai AS [Trạng Thái], cd.ketQua AS [Kết Quả], cd.ketLuan AS [Kết Luận], cd.createdAt AS [Ngày CĐ] "
                    + "FROM dbo.ChiDinhDichVu cd JOIN dbo.DanhMucDichVu dv ON cd.maDichVu = dv.id "
                    + "JOIN dbo.LuotKham lk ON cd.maLuotKham = lk.id "
                    + "JOIN dbo.BenhNhan bn ON lk.maBenhNhan = bn.id "
                    + "WHERE cd.isDeleted = 0 AND cd.createdAt >= ? AND cd.createdAt < ? ORDER BY cd.id DESC";
        }
        // Mặc định: Sổ khám bệnh / Phiếu khám / Bệnh án
        return "SELECT lk.id AS [Mã Lượt], bn.maBenhNhanFormat AS [Mã BN], bn.ho + ' ' + bn.ten AS [Họ Tên], "
                + "bn.dienThoai AS [SĐT], lk.thoiGianTiepNhan AS [Tiếp Nhận], lk.trangThai AS [Trạng Thái], "
                + "lk.huongXuTri AS [Xử Trí], lk.emrLocked AS [Đã Khóa EMR] "
                + "FROM dbo.LuotKham lk JOIN dbo.BenhNhan bn ON lk.maBenhNhan = bn.id "
                + "WHERE lk.isDeleted = 0 AND lk.thoiGianTiepNhan >= ? AND lk.thoiGianTiepNhan < ? ORDER BY lk.id DESC";
    }

    private void exportCsv() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu để xuất.", "Xuất Báo Cáo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("baocao-" + report.reportId() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (var w = Files.newBufferedWriter(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8)) {
            w.write('\ufeff');
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) w.write(',');
                w.write(csvCell(model.getColumnName(c)));
            }
            w.newLine();
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) w.write(',');
                    w.write(csvCell(Objects.toString(model.getValueAt(r, c), "")));
                }
                w.newLine();
            }
            JOptionPane.showMessageDialog(this, "Đã xuất báo cáo thành công!", "Xuất Báo Cáo", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không thể xuất file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String csvCell(String v) {
        if (v.startsWith("=") || v.startsWith("+") || v.startsWith("-") || v.startsWith("@")) v = "'" + v;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
