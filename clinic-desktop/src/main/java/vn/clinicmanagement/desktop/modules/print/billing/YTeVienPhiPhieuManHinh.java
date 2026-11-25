package vn.clinicmanagement.desktop.modules.print.billing;


import vn.clinicmanagement.desktop.modules.print.examination.*;
import vn.clinicmanagement.application.billing.HoaDonDongDuLieu;
import vn.clinicmanagement.application.billing.HoaDonDuLieu;
import vn.clinicmanagement.application.billing.VienPhiManHinhReadCongKetNoi;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Giao diện xem trước và in: "BẢNG KÊ CHI PHÍ KHÁM BỆNH" (Mẫu 01/KCCB chuẩn Bộ Y Tế).
 * Nối trực tiếp dữ liệu thật từ SQL Server (`LuotKham`, `BenhNhan`, `BaoHiem`, `ChanDoan`,
 * `HoaDon`, `ChiTietHoaDon`, `ChiDinhDichVu`, `DonThuoc`).
 */
public class YTeVienPhiPhieuManHinh extends JPanel {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FULL_FMT = DateTimeFormatter.ofPattern("HH 'giờ' mm 'phút, ngày' dd / MM / yyyy");

    public record CostRow(
            String content,
            String unit,
            String quantity,
            String unitPriceBv,
            String unitPriceBh,
            String servicePayRate,
            String totalBv,
            String bhPayRate,
            String totalBh,
            String bhytFundPaid,
            String patientCoPay,
            String otherPaid,
            String patientSelfPay,
            boolean bold,
            int indent
    ) {}

    public record BillingSlipData(
            String patientName,
            String patientCode,
            String birthYear,
            String age,
            String gender,
            String feeCode,
            String bhytCode,
            String daysLeft,
            String address,
            String bhytPercent,
            BigDecimal totalAmount,
            BigDecimal bhytAmount,
            BigDecimal patientAmount,
            String departmentName,
            String departmentCode,
            String bhytValidRange,
            String bhytFacilityName,
            String bhytFacilityCode,
            String arrivalTimeText,
            String endTimeText,
            String diagnosisCode,
            String diagnosisText,
            boolean inRoute,
            List<CostRow> costRows
    ) {
        public BillingSlipData(
                String patientName,
                String patientCode,
                String birthYear,
                String age,
                String gender,
                String feeCode,
                String bhytCode,
                String daysLeft,
                String address,
                String bhytPercent,
                BigDecimal totalAmount,
                BigDecimal bhytAmount,
                BigDecimal patientAmount) {
            this(patientName, patientCode, birthYear, age, gender, feeCode, bhytCode, daysLeft,
                    address, bhytPercent, totalAmount, bhytAmount, patientAmount,
                    "Khoa khám bệnh", "K01", "", "", "", "", "", "", "", true, List.of());
        }
    }

    private BillingSlipData boundData;
    private String patientName = "";
    private String patientCode = "";
    private String patientBirthYear = "";
    private String patientAge = "";
    private String patientGender = "";
    private String feeCode = "";
    private String bhytCode = "";
    private String daysLeft = "";
    private String address = "";
    private String bhytPercent = "0%";
    private String departmentName = "Khoa khám bệnh";
    private String departmentCode = "K01";
    private String bhytValidRange = "";
    private String bhytFacilityName = "";
    private String bhytFacilityCode = "";
    private String arrivalTimeText = "";
    private String endTimeText = "";
    private String diagnosisCode = "";
    private String diagnosisText = "";
    private boolean inRoute = true;
    private List<CostRow> costRows = List.of();
    private JPanel page1Ref;
    private JPanel page2Ref;

    public YTeVienPhiPhieuManHinh() {
        this(null);
    }

    public YTeVienPhiPhieuManHinh(BillingSlipData data) {
        bindData(data);
    }

    /** Nạp toàn bộ dữ liệu bảng kê chi phí thật từ SQL Server theo `visitId`. */
    public static BillingSlipData loadFromDatabase(int visitId) {
        if (visitId <= 0 || GraphicsEnvironment.isHeadless() || System.getProperty("surefire.test.class.path") != null) {
            return null;
        }
        try (Connection c = CoSoDuLieuKetNoi.getConnection()) {
            String visitSql = "SELECT lk.id, lk.maBenhNhan, lk.thoiGianTiepNhan, lk.thoiGianKetThucKham, lk.doiTuongKham, "
                    + "bn.maBenhNhanFormat, bn.ho + ' ' + bn.ten AS hoTen, bn.ngaySinh, bn.gioiTinh, bn.diaChi, "
                    + "ISNULL(k.tenKhoa, N'Khoa khám bệnh') AS tenKhoa, "
                    + "CASE WHEN k.id IS NOT NULL THEN CONCAT('K', RIGHT('00' + CAST(k.id AS VARCHAR(10)), 2)) ELSE 'K01' END AS maKhoa, "
                    + "pk.tenPhong "
                    + "FROM dbo.LuotKham lk "
                    + "JOIN dbo.BenhNhan bn ON lk.maBenhNhan = bn.id "
                    + "LEFT JOIN dbo.Khoa k ON lk.maKhoa = k.id "
                    + "LEFT JOIN dbo.PhongKham pk ON lk.maPhongKham = pk.id "
                    + "WHERE lk.id = ? AND lk.isDeleted = 0";
            int patientId = 0;
            String pName = "", pCode = "", pBirth = "", pAge = "", pGender = "", pAddr = "", subject = "VIEN_PHI";
            String dept = "Khoa khám bệnh", deptCode = "K01", arrText = "", endText = "";
            try (PreparedStatement ps = c.prepareStatement(visitSql)) {
                ps.setInt(1, visitId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    patientId = rs.getInt("maBenhNhan");
                    pCode = safe(rs.getString("maBenhNhanFormat"));
                    pName = safe(rs.getString("hoTen")).toUpperCase(Locale.ROOT);
                    Date dobSql = rs.getDate("ngaySinh");
                    if (dobSql != null) {
                        LocalDate dob = dobSql.toLocalDate();
                        pBirth = dob.format(DATE_FMT);
                        pAge = String.valueOf(Math.max(0, LocalDate.now().getYear() - dob.getYear()));
                    }
                    String g = safe(rs.getString("gioiTinh"));
                    pGender = ("NAM".equalsIgnoreCase(g) || "M".equalsIgnoreCase(g)) ? "Nam" : (g.isEmpty() ? "" : "Nữ");
                    pAddr = safe(rs.getString("diaChi"));
                    dept = safe(rs.getString("tenKhoa"));
                    String room = safe(rs.getString("tenPhong"));
                    if (!room.isEmpty()) dept = dept + " - " + room;
                    deptCode = safe(rs.getString("maKhoa"));
                    subject = safe(rs.getString("doiTuongKham"));
                    Timestamp arr = rs.getTimestamp("thoiGianTiepNhan");
                    if (arr != null) arrText = arr.toLocalDateTime().format(TIME_FULL_FMT);
                    Timestamp end = rs.getTimestamp("thoiGianKetThucKham");
                    if (end != null) endText = end.toLocalDateTime().format(TIME_FULL_FMT);
                }
            }

            // Bảo hiểm
            String bhytNo = "", bhytPct = "0%", bhytValid = "", bhytFac = "", bhytFacCode = "", daysLeftText = "";
            boolean isInRoute = true;
            int mucHuong = 0;
            String insSql = "SELECT TOP 1 soBaoHiem, ngayBatDau, ngayHetHan, mucHuong, maNoiDangKyKCB, noiDangKyKCB, dungTuyen "
                    + "FROM dbo.BaoHiem WHERE maBenhNhan = ? AND isDeleted = 0 ORDER BY id DESC";
            try (PreparedStatement ps = c.prepareStatement(insSql)) {
                ps.setInt(1, patientId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        bhytNo = safe(rs.getString("soBaoHiem"));
                        mucHuong = rs.getInt("mucHuong");
                        if (mucHuong > 0) bhytPct = mucHuong + "%";
                        Date dFrom = rs.getDate("ngayBatDau");
                        Date dTo = rs.getDate("ngayHetHan");
                        if (dFrom != null && dTo != null) {
                            bhytValid = "Giá trị từ " + dFrom.toLocalDate().format(DATE_FMT) + " đến " + dTo.toLocalDate().format(DATE_FMT);
                        }
                        if (dTo != null) {
                            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dTo.toLocalDate());
                            daysLeftText = days >= 0 ? ("Còn " + days + " ngày") : "Hết hạn";
                        }
                        bhytFac = safe(rs.getString("noiDangKyKCB"));
                        bhytFacCode = safe(rs.getString("maNoiDangKyKCB"));
                        isInRoute = rs.getObject("dungTuyen") == null || rs.getBoolean("dungTuyen");
                    }
                }
            } catch (SQLException ignored) {}

            // Chẩn đoán
            String diagCode = "", diagText = "";
            String diagSql = "SELECT cd.chanDoanText, icd.maICD, cd.laChinh "
                    + "FROM dbo.ChanDoan cd LEFT JOIN dbo.ICD icd ON cd.maICD = icd.id "
                    + "WHERE cd.maLuotKham = ? AND cd.isDeleted = 0 ORDER BY cd.laChinh DESC, cd.id DESC";
            try (PreparedStatement ps = c.prepareStatement(diagSql)) {
                ps.setInt(1, visitId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<String> secondary = new ArrayList<>();
                    while (rs.next()) {
                        String code = safe(rs.getString("maICD"));
                        String txt = safe(rs.getString("chanDoanText"));
                        if (rs.getBoolean("laChinh") && diagText.isEmpty()) {
                            diagCode = code;
                            diagText = txt;
                        } else if (!txt.isEmpty()) {
                            secondary.add(code.isEmpty() ? txt : (code + " - " + txt));
                        }
                    }
                    if (diagText.isEmpty() && !secondary.isEmpty()) {
                        diagText = String.join("; ", secondary);
                    }
                }
            } catch (SQLException ignored) {}

            // Chi tiết chi phí từ HoaDon mới nhất (hoặc tổng hợp từ ChiDinhDichVu + DonThuoc nếu chưa lập HoaDon)
            BigDecimal totalAmt = BigDecimal.ZERO;
            BigDecimal bhytAmt = BigDecimal.ZERO;
            BigDecimal patientAmt = BigDecimal.ZERO;
            List<RawLine> rawLines = new ArrayList<>();

            String invSql = "SELECT TOP 1 id, tongTien, soTienBaoHiemChiTra, soTienBenhNhanChiTra "
                    + "FROM dbo.HoaDon WHERE maLuotKham = ? AND isDeleted = 0 AND trangThai <> 'DA_HUY' ORDER BY id DESC";
            int invoiceId = 0;
            try (PreparedStatement ps = c.prepareStatement(invSql)) {
                ps.setInt(1, visitId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        invoiceId = rs.getInt("id");
                        totalAmt = nz(rs.getBigDecimal("tongTien"));
                        bhytAmt = nz(rs.getBigDecimal("soTienBaoHiemChiTra"));
                        patientAmt = nz(rs.getBigDecimal("soTienBenhNhanChiTra"));
                    }
                }
            }

            if (invoiceId > 0) {
                String invLineSql = "SELECT ct.loaiDoiTuong, ct.moTa, ct.soLuong, ct.donGia, dv.loaiDichVu "
                        + "FROM dbo.ChiTietHoaDon ct "
                        + "LEFT JOIN dbo.ChiDinhDichVu cd ON ct.loaiDoiTuong IN ('DICH_VU','DICH_VU_CLS','CLS','KHAM','DICH_VU_KHAM') AND ct.maDoiTuong = cd.id "
                        + "LEFT JOIN dbo.DanhMucDichVu dv ON cd.maDichVu = dv.id "
                        + "WHERE ct.maHoaDon = ? AND ct.isDeleted = 0 ORDER BY ct.id ASC";
                try (PreparedStatement ps = c.prepareStatement(invLineSql)) {
                    ps.setInt(1, invoiceId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String itemType = safe(rs.getString("loaiDoiTuong"));
                            String svcType = safe(rs.getString("loaiDichVu"));
                            String unit = itemType.contains("THUOC") ? "Viên" : "Lần";
                            BigDecimal qty = nz(rs.getBigDecimal("soLuong"));
                            if (qty.signum() <= 0) qty = BigDecimal.ONE;
                            BigDecimal price = nz(rs.getBigDecimal("donGia"));
                            rawLines.add(new RawLine(classifyGroup(itemType, svcType), safe(rs.getString("moTa")), unit, qty, price));
                        }
                    }
                }
            }

            if (rawLines.isEmpty()) {
                // Chưa lập hóa đơn → tổng hợp từ chỉ định dịch vụ và đơn thuốc hiện tại
                String svcSql = "SELECT dv.loaiDichVu, dv.tenDichVu, N'Lần' AS dvt, cd.soLuong, cd.donGia "
                        + "FROM dbo.ChiDinhDichVu cd JOIN dbo.DanhMucDichVu dv ON cd.maDichVu = dv.id "
                        + "WHERE cd.maLuotKham = ? AND cd.isDeleted = 0 AND cd.trangThai <> 'DA_HUY'";
                try (PreparedStatement ps = c.prepareStatement(svcSql)) {
                    ps.setInt(1, visitId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            BigDecimal q = nz(rs.getBigDecimal("soLuong"));
                            if (q.signum() <= 0) q = BigDecimal.ONE;
                            BigDecimal p = nz(rs.getBigDecimal("donGia"));
                            rawLines.add(new RawLine(classifyGroup("DICH_VU", safe(rs.getString("loaiDichVu"))),
                                    safe(rs.getString("tenDichVu")), safe(rs.getString("dvt")), q, p));
                        }
                    }
                }
                String rxSql = "SELECT mh.tenItem, ISNULL(mh.donViCoBan, N'Viên') AS dvt, ct.soLuongKe, ct.donGiaKe "
                        + "FROM dbo.DonThuoc dt JOIN dbo.ChiTietDonThuoc ct ON dt.id = ct.maDonThuoc AND ct.isDeleted = 0 "
                        + "JOIN dbo.Item mh ON ct.maItem = mh.id "
                        + "WHERE dt.maLuotKham = ? AND dt.isDeleted = 0 AND dt.trangThai <> 'DA_HUY'";
                try (PreparedStatement ps = c.prepareStatement(rxSql)) {
                    ps.setInt(1, visitId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            BigDecimal q = nz(rs.getBigDecimal("soLuongKe"));
                            if (q.signum() <= 0) q = BigDecimal.ONE;
                            BigDecimal p = nz(rs.getBigDecimal("donGiaKe"));
                            rawLines.add(new RawLine("6. Thuốc, dịch truyền:", safe(rs.getString("tenItem")),
                                    safe(rs.getString("dvt")), q, p));
                        }
                    }
                }
                for (RawLine rl : rawLines) {
                    totalAmt = totalAmt.add(rl.qty.multiply(rl.price));
                }
                if (!bhytNo.isEmpty() && mucHuong > 0) {
                    bhytAmt = totalAmt.multiply(BigDecimal.valueOf(mucHuong)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                }
                patientAmt = totalAmt.subtract(bhytAmt).max(BigDecimal.ZERO);
            }

            List<CostRow> rows = buildGroupedCostRows(dept, rawLines, totalAmt, bhytAmt, patientAmt,
                    !bhytNo.isEmpty() && "BHYT".equalsIgnoreCase(subject) ? mucHuong : 0);

            return new BillingSlipData(
                    pName,
                    pCode,
                    pBirth,
                    pAge,
                    pGender,
                    String.valueOf(visitId),
                    bhytNo.isEmpty() ? subject : bhytNo,
                    daysLeftText,
                    pAddr,
                    bhytPct,
                    totalAmt,
                    bhytAmt,
                    patientAmt,
                    dept,
                    deptCode,
                    bhytValid,
                    bhytFac,
                    bhytFacCode,
                    arrText,
                    endText,
                    diagCode,
                    diagText,
                    isInRoute,
                    rows
            );
        } catch (SQLException ex) {
            return null;
        }
    }

    /** Tạo BillingSlipData trực tiếp từ `HoaDonDuLieu` tại màn hình Thu Ngân (nếu DB trả thêm chi tiết thì tự bổ sung). */
    public static BillingSlipData fromInvoice(HoaDonDuLieu invoice, VienPhiManHinhReadCongKetNoi.ThongTin info) {
        if (invoice == null) return null;
        BillingSlipData dbData = loadFromDatabase(invoice.visitId());
        if (dbData != null && !dbData.costRows().isEmpty()) {
            return dbData;
        }
        List<RawLine> rawLines = new ArrayList<>();
        if (invoice.lines() != null) {
            for (HoaDonDongDuLieu l : invoice.lines()) {
                BigDecimal q = l.quantity() != null && l.quantity().signum() > 0 ? l.quantity() : BigDecimal.ONE;
                BigDecimal p = nz(l.unitPrice());
                String t = safe(l.itemType());
                rawLines.add(new RawLine(classifyGroup(t, t), safe(l.description()), t.contains("THUOC") ? "Viên" : "Lần", q, p));
            }
        }
        BigDecimal total = nz(invoice.totalAmount());
        BigDecimal bh = nz(invoice.insurancePaidAmount());
        BigDecimal pt = nz(invoice.patientPaidAmount());
        String pName = info != null ? safe(info.patientName()).toUpperCase(Locale.ROOT) : "";
        String pCode = info != null ? safe(info.patientCode()) : "";
        List<CostRow> rows = buildGroupedCostRows("Khoa khám bệnh", rawLines, total, bh, pt, bh.signum() > 0 ? 80 : 0);
        return new BillingSlipData(
                pName, pCode, "", "", "", String.valueOf(invoice.visitId()),
                invoice.invoiceNumber(), "", "", bh.signum() > 0 ? "80%" : "0%",
                total, bh, pt, "Khoa khám bệnh", "K01", "", "", "", "", "", "", "", true, rows);
    }

    private record RawLine(String group, String name, String unit, BigDecimal qty, BigDecimal price) {}

    private static String classifyGroup(String itemType, String serviceType) {
        String s = (itemType + " " + serviceType).toUpperCase(Locale.ROOT);
        if (s.contains("THUOC")) return "6. Thuốc, dịch truyền:";
        if (s.contains("VAT_TU") || s.contains("VATTU")) return "7. Vật tư y tế:";
        if (s.contains("XET_NGHIEM")) return "3. Xét nghiệm:";
        if (s.contains("CHAN_DOAN_HINH_ANH") || s.contains("CDHA")) return "4. Chẩn đoán hình ảnh:";
        if (s.contains("DICH_VU_KY_THUAT") || s.contains("THU_THUAT") || s.contains("CHUYEN_KHOA")) return "5. Thủ thuật, phẫu thuật:";
        return "1. Khám bệnh:";
    }

    private static List<CostRow> buildGroupedCostRows(
            String dept, List<RawLine> rawLines,
            BigDecimal totalAmt, BigDecimal bhytAmt, BigDecimal patientAmt, int bhRate) {
        List<CostRow> rows = new ArrayList<>();
        BigDecimal invRatio = totalAmt.signum() > 0 ? bhytAmt.divide(totalAmt, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        rows.add(new CostRow(dept, "", "", "", "", "", fmt(totalAmt), "", fmt(bhytAmt), fmt(bhytAmt),
                fmt(patientAmt.subtract(BigDecimal.ZERO)), "0", fmt(bhytAmt.signum() > 0 ? BigDecimal.ZERO : patientAmt), true, 0));

        Map<String, List<RawLine>> grouped = new LinkedHashMap<>();
        for (RawLine rl : rawLines) {
            grouped.computeIfAbsent(rl.group, k -> new ArrayList<>()).add(rl);
        }

        for (var entry : grouped.entrySet()) {
            BigDecimal grpTotal = BigDecimal.ZERO;
            BigDecimal grpBh = BigDecimal.ZERO;
            for (RawLine l : entry.getValue()) {
                BigDecimal lineAmt = l.qty.multiply(l.price);
                grpTotal = grpTotal.add(lineAmt);
                grpBh = grpBh.add(lineAmt.multiply(invRatio).setScale(0, RoundingMode.HALF_UP));
            }
            BigDecimal grpPt = grpTotal.subtract(grpBh).max(BigDecimal.ZERO);
            rows.add(new CostRow(entry.getKey(), "", "", "", "", "", fmt(grpTotal), "", fmt(grpBh), fmt(grpBh),
                    "0", "0", fmt(grpPt), true, 0));

            for (RawLine l : entry.getValue()) {
                BigDecimal lineAmt = l.qty.multiply(l.price);
                BigDecimal lineBh = lineAmt.multiply(invRatio).setScale(0, RoundingMode.HALF_UP);
                BigDecimal linePt = lineAmt.subtract(lineBh).max(BigDecimal.ZERO);
                rows.add(new CostRow(
                        l.name, l.unit, l.qty.stripTrailingZeros().toPlainString(),
                        fmt(l.price), bhRate > 0 ? fmt(l.price) : "0", "100",
                        fmt(lineAmt), String.valueOf(bhRate), fmt(lineBh), fmt(lineBh),
                        "0", "0", fmt(linePt), false, 1));
            }
        }

        BigDecimal totalSelf = bhytAmt.signum() > 0 ? BigDecimal.ZERO : patientAmt;
        BigDecimal totalCoPay = bhytAmt.signum() > 0 ? patientAmt : BigDecimal.ZERO;
        rows.add(new CostRow("Cộng", "", "", "", "", "", fmt(totalAmt), "", fmt(bhytAmt), fmt(bhytAmt),
                fmt(totalCoPay), "0", fmt(totalSelf), true, 0));
        return rows;
    }

    public void bindData(BillingSlipData data) {
        this.boundData = data;
        if (data != null) {
            this.patientName = safe(data.patientName());
            this.patientCode = safe(data.patientCode());
            this.patientBirthYear = safe(data.birthYear());
            this.patientAge = safe(data.age());
            this.patientGender = safe(data.gender());
            this.feeCode = safe(data.feeCode());
            this.bhytCode = safe(data.bhytCode());
            this.daysLeft = safe(data.daysLeft());
            this.address = safe(data.address());
            this.bhytPercent = safe(data.bhytPercent()).isEmpty() ? "0%" : data.bhytPercent();
            this.departmentName = safe(data.departmentName()).isEmpty() ? "Khoa khám bệnh" : data.departmentName();
            this.departmentCode = safe(data.departmentCode()).isEmpty() ? "K01" : data.departmentCode();
            this.bhytValidRange = safe(data.bhytValidRange());
            this.bhytFacilityName = safe(data.bhytFacilityName());
            this.bhytFacilityCode = safe(data.bhytFacilityCode());
            this.arrivalTimeText = safe(data.arrivalTimeText());
            this.endTimeText = safe(data.endTimeText());
            this.diagnosisCode = safe(data.diagnosisCode());
            this.diagnosisText = safe(data.diagnosisText());
            this.inRoute = data.inRoute();
            this.costRows = data.costRows() != null ? data.costRows() : List.of();
        }
        rebuildUi();
    }

    public BillingSlipData getBoundData() {
        return boundData;
    }

    private void rebuildUi() {
        removeAll();
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        add(buildTopToolBar(), BorderLayout.NORTH);

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);

        JScrollPane scrollPane = new JScrollPane(buildMultiPageDocumentView());
        scrollPane.getViewport().setBackground(new Color(105, 110, 115));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JButton btnFloatingPrint = createFloatingPrintButton();
        layeredPane.add(scrollPane, Integer.valueOf(1));
        layeredPane.add(btnFloatingPrint, Integer.valueOf(2));
        layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                int w = layeredPane.getWidth(), h = layeredPane.getHeight();
                scrollPane.setBounds(0, 0, w, h);
                btnFloatingPrint.setBounds(w - 65, 20, 40, 40);
            }
        });
        add(layeredPane, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel buildTopToolBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(238, 241, 245));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(195, 205, 218)));
        bar.setPreferredSize(new Dimension(1000, 46));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        left.setOpaque(false);
        JLabel lblSigType = new JLabel("Loại chữ ký : ---");
        lblSigType.setFont(UngDungPhongChu.BODY);
        JLabel lblValidity = new JLabel("Hiệu lực : ---");
        lblValidity.setFont(UngDungPhongChu.BODY);
        JButton btnVerify = new JButton(" Kiểm Tra", new CheckmarkIcon());
        btnVerify.setFont(UngDungPhongChu.BODY_BOLD);
        btnVerify.setBackground(Color.WHITE);
        btnVerify.setFocusPainted(false);
        btnVerify.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVerify.setPreferredSize(new Dimension(96, 26));
        btnVerify.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(175, 185, 198), 1),
                new EmptyBorder(1, 4, 1, 4)));
        left.add(lblSigType);
        left.add(lblValidity);
        left.add(btnVerify);

        JPanel right = new JPanel(new BorderLayout(8, 0));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(2, 6, 2, 12));

        JPanel textPanel = new JPanel(new GridLayout(3, 1, 0, 0));
        textPanel.setOpaque(false);
        String line1 = (patientName.isBlank() ? "---" : patientName) + " | "
                + (patientCode.isBlank() ? "---" : patientCode) + " | "
                + (patientBirthYear.isBlank() ? "---" : patientBirthYear)
                + (patientAge.isBlank() ? "" : " (" + patientAge + " tuổi)") + " | "
                + (patientGender.isBlank() ? "---" : patientGender);
        JLabel l1 = new JLabel(line1, SwingConstants.RIGHT);
        l1.setFont(UngDungPhongChu.SECTION);
        l1.setForeground(new Color(125, 20, 20));

        String line2 = "Mã VP : " + (feeCode.isBlank() ? "---" : feeCode) + " | "
                + (bhytCode.isBlank() ? "---" : bhytCode)
                + (daysLeft.isBlank() ? "" : " | " + daysLeft);
        JLabel l2 = new JLabel(line2, SwingConstants.RIGHT);
        l2.setFont(UngDungPhongChu.BODY);
        l2.setForeground(new Color(125, 20, 20));

        JLabel l3 = new JLabel(address.isBlank() ? "---" : address, SwingConstants.RIGHT);
        l3.setFont(UngDungPhongChu.BODY);
        l3.setForeground(new Color(125, 20, 20));

        textPanel.add(l1);
        textPanel.add(l2);
        textPanel.add(l3);

        JLabel lblBhyt = new JLabel("<html><center><font color='#800000'><b>BHYT</b></font><br><font color='#0044CC'><b>" + bhytPercent + "</b></font></center></html>", SwingConstants.CENTER);
        lblBhyt.setFont(UngDungPhongChu.SECTION);
        lblBhyt.setBorder(new EmptyBorder(0, 10, 0, 4));
        right.add(textPanel, BorderLayout.CENTER);
        right.add(lblBhyt, BorderLayout.EAST);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JButton createFloatingPrintButton() {
        JButton btn = new JButton(new PrinterVectorIcon(20, 20)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 30, 30, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(80, 80, 80));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("In bảng kê chi phí");
        btn.addActionListener(e -> printPages());
        return btn;
    }

    private void printPages() {
        if (GraphicsEnvironment.isHeadless()) return;
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("BangKeChiPhi_" + (patientCode.isBlank() ? feeCode : patientCode));
        List<JPanel> pages = new ArrayList<>();
        if (page1Ref != null) pages.add(page1Ref);
        if (page2Ref != null) pages.add(page2Ref);
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex >= pages.size()) return Printable.NO_SUCH_PAGE;
            JPanel target = pages.get(pageIndex);
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            double scale = Math.min(pageFormat.getImageableWidth() / target.getWidth(),
                    pageFormat.getImageableHeight() / target.getHeight());
            g2.scale(scale, scale);
            target.printAll(g2);
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Không thể in bảng kê: " + ex.getMessage(), "Lỗi In", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel buildMultiPageDocumentView() {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(15, 20, 30, 20));

        List<CostRow> allRows = costRows;
        int splitAt = Math.min(allRows.size(), 8);
        List<CostRow> page1Rows = allRows.subList(0, splitAt);
        List<CostRow> page2Rows = splitAt < allRows.size() ? allRows.subList(splitAt, allRows.size()) : List.of();

        int p1Height = 520 + Math.max(2, page1Rows.size()) * 22;
        page1Ref = createPaperContainer(p1Height);
        page1Ref.add(buildPage1Content(page1Rows));
        wrapper.add(wrapInCenter(page1Ref));

        if (!page2Rows.isEmpty()) {
            int p2Height = 140 + page2Rows.size() * 22;
            page2Ref = createPaperContainer(p2Height);
            page2Ref.add(buildPage2Content(page2Rows));
            wrapper.add(Box.createVerticalStrut(22));
            wrapper.add(wrapInCenter(page2Ref));
        } else {
            page2Ref = null;
        }
        return wrapper;
    }

    private JPanel wrapInCenter(JComponent comp) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.setOpaque(false);
        p.add(comp);
        return p;
    }

    private JPanel createPaperContainer(int height) {
        JPanel paper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(30, 30, 30, 110));
                g2.fillRect(3, 3, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth() - 3, getHeight() - 3);
                g2.setColor(new Color(195, 195, 195));
                g2.drawRect(0, 0, getWidth() - 3, getHeight() - 3);
                g2.dispose();
            }
        };
        paper.setOpaque(false);
        paper.setPreferredSize(new Dimension(880, height));
        paper.setBorder(new EmptyBorder(14, 20, 14, 20));
        return paper;
    }

    private JPanel buildPage1Content(List<CostRow> rows) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.add(buildPaperHeader());
        content.add(Box.createVerticalStrut(6));

        JLabel lblDocTitle = new JLabel("BẢNG KÊ CHI PHÍ KHÁM BỆNH", SwingConstants.CENTER);
        lblDocTitle.setFont(new Font(UngDungPhongChu.FAMILY, Font.BOLD, 21));
        lblDocTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(lblDocTitle);
        content.add(Box.createVerticalStrut(8));

        content.add(buildAdminSection());
        content.add(Box.createVerticalStrut(6));
        content.add(buildCostSectionHeader());
        content.add(Box.createVerticalStrut(6));
        content.add(new CostTableCustomView(rows));
        return content;
    }

    private JPanel buildPage2Content(List<CostRow> rows) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(10, 0, 0, 0));
        content.add(new CostTableCustomView(rows));
        return content;
    }

    private JPanel buildPaperHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(createDocText("Bộ Y tế/Sở Y tế/ Y tế ngành: SỞ Y TẾ BẮC NINH", true));
        left.add(createDocText("Cơ sở khám, chữa bệnh: BỆNH VIỆN SẢN NHI", true));
        left.add(createDocText("Khoa: " + departmentName, false));
        left.add(createDocText("Mã khoa: " + departmentCode, false));

        JPanel right = new JPanel(new BorderLayout(10, 0));
        right.setOpaque(false);
        JPanel rightText = new JPanel();
        rightText.setLayout(new BoxLayout(rightText, BoxLayout.Y_AXIS));
        rightText.setOpaque(false);
        rightText.add(createDocText("Mẫu số: 01/KCCB", true));
        rightText.add(createDocText("Mã số người bệnh: " + patientCode, false));
        rightText.add(createDocText("Số khám bệnh: " + feeCode, false));
        rightText.add(Box.createVerticalStrut(3));
        rightText.add(new JLabel(new BarcodeVectorIcon(96, 24)));

        JLabel box1 = new JLabel("1", SwingConstants.CENTER);
        box1.setFont(new Font("Times New Roman", Font.BOLD, 16));
        box1.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        box1.setPreferredSize(new Dimension(32, 52));
        right.add(rightText, BorderLayout.CENTER);
        right.add(box1, BorderLayout.EAST);

        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel buildAdminSection() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.add(createDocText("I. Phần hành chính:", true));

        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r1.setOpaque(false);
        r1.add(createDocText("(1) Họ tên người bệnh: ", false));
        r1.add(createDocText(patientName.isBlank() ? "---" : patientName, true));
        r1.add(Box.createHorizontalStrut(20));
        r1.add(createDocText("Ngày, tháng, năm sinh: " + patientBirthYear, false));
        r1.add(Box.createHorizontalStrut(20));
        r1.add(createDocText("Giới tính: " + patientGender, false));
        r1.add(Box.createHorizontalStrut(20));
        r1.add(createNumberedBox("2"));
        p.add(r1);

        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r2.setOpaque(false);
        r2.add(createDocText("(2) Địa chỉ hiện tại: " + address, false));
        r2.add(Box.createHorizontalStrut(25));
        r2.add(createDocText("(3) Mã khu vực (K1/K2/K3):", false));
        r2.add(createBoxText("", 40));
        p.add(r2);

        JPanel r3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r3.setOpaque(false);
        r3.add(createDocText("(4) Mã thẻ BHYT: ", false));
        r3.add(createBoxText(bhytCode, 170));
        r3.add(Box.createHorizontalStrut(10));
        r3.add(createDocText(bhytValidRange, false));
        p.add(r3);

        JPanel r4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r4.setOpaque(false);
        r4.add(createDocText("(5) Cơ sở ĐK KCB ban đầu: " + bhytFacilityName, false));
        r4.add(Box.createHorizontalStrut(20));
        r4.add(createDocText("(6) Mã:", false));
        r4.add(createBoxText(bhytFacilityCode, 66));
        p.add(r4);

        JPanel r5 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r5.setOpaque(false);
        r5.add(createDocText("(7) Đến khám: " + (arrivalTimeText.isBlank() ? "---" : arrivalTimeText), false));
        p.add(r5);

        JPanel r6 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r6.setOpaque(false);
        r6.add(createDocText("(8) Điều trị ngoại trú/nội trú từ: " + (arrivalTimeText.isBlank() ? "---" : arrivalTimeText), false));
        r6.add(Box.createHorizontalStrut(12));
        r6.add(createDocText("Tổng số ngày điều trị: 1", false));
        r6.add(Box.createHorizontalStrut(16));
        r6.add(createDocText("(10) Tình trạng ra viện:", false));
        r6.add(createBoxText("", 40));
        p.add(r6);

        JPanel r7 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r7.setOpaque(false);
        r7.add(createDocText("(9) Kết thúc khám/điều trị: " + (endTimeText.isBlank() ? "---" : endTimeText), false));
        r7.add(Box.createHorizontalStrut(10));
        r7.add(createDocText("(13) Thông tuyến ☐  (14) Trái tuyến", false));
        r7.add(createBoxText(inRoute ? "" : "X", 24));
        p.add(r7);

        JPanel r8 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r8.setOpaque(false);
        r8.add(createDocText("(11) Cấp cứu ☐   (12) Đúng tuyến", false));
        r8.add(createBoxText(inRoute ? "X" : "", 20));
        r8.add(Box.createHorizontalStrut(20));
        r8.add(createDocText("(16) Mã bệnh (ICD-10):", false));
        r8.add(createBoxText(diagnosisCode, 70));
        p.add(r8);

        JPanel r9 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r9.setOpaque(false);
        r9.add(createDocText("(15) Chẩn đoán xác định: " + diagnosisText, false));
        p.add(r9);

        return p;
    }

    private JPanel buildCostSectionHeader() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.add(createDocText("II. Phần chi phí khám bệnh, chữa bệnh:", true));

        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r1.setOpaque(false);
        r1.add(createDocText("Mã thẻ BHYT: ", false));
        r1.add(createBoxText(bhytCode, 170));
        r1.add(Box.createHorizontalStrut(10));
        r1.add(createDocText(bhytValidRange, false));
        r1.add(Box.createHorizontalStrut(25));
        r1.add(createDocText("Mức hưởng", false));
        r1.add(createBoxText(bhytPercent, 56));
        p.add(r1);
        return p;
    }

    private static class CostTableCustomView extends JComponent {
        private final int[] colWidths = {200, 38, 28, 58, 58, 48, 68, 48, 68, 58, 58, 34, 76};
        private final int totalWidth = 840;
        private final int headerHeight = 58;
        private final int tier1H = 22;
        private final int tier2H = 36;
        private final int colNumRowHeight = 18;
        private final int rowHeight = 22;
        private final List<CostRow> rows;

        CostTableCustomView(List<CostRow> rows) {
            this.rows = rows != null ? rows : List.of();
            int totalH = headerHeight + colNumRowHeight + Math.max(1, this.rows.size()) * rowHeight;
            setPreferredSize(new Dimension(totalWidth, totalH));
            setMinimumSize(new Dimension(totalWidth, totalH));
            setMaximumSize(new Dimension(totalWidth, totalH));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Font fontRegular = new Font("Times New Roman", Font.PLAIN, 12);
            Font fontBold = new Font("Times New Roman", Font.BOLD, 12);
            Font fontHeader = new Font("Times New Roman", Font.BOLD, 11);
            Font fontColNum = new Font("Times New Roman", Font.PLAIN, 11);

            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, totalWidth, getHeight());
            g2.setColor(Color.BLACK);

            int[] colX = new int[colWidths.length + 1];
            for (int i = 0; i < colWidths.length; i++) colX[i + 1] = colX[i] + colWidths[i];

            String[] topTitles = {
                    "Nội dung", "Đơn\nvị\ntính", "Số\nlượng",
                    "Đơn giá\nBV\n(đồng)", "Đơn giá\nBH\n(đồng)",
                    "Tỷ lệ\nthanh\ntoán\ndịch vụ %", "Thành\ntiền\nBV\n(đồng)",
                    "Tỷ lệ\nBHYT\n%", "Thành\ntiền\nBH\n(đồng)"
            };
            for (int i = 0; i <= 8; i++) {
                g2.drawRect(colX[i], 0, colWidths[i], headerHeight);
                drawCenteredMultiLineText(g2, topTitles[i], colX[i], 0, colWidths[i], headerHeight, fontHeader);
            }

            int sourceX = colX[9], sourceW = colX[13] - colX[9];
            g2.drawRect(sourceX, 0, sourceW, tier1H);
            drawCenteredMultiLineText(g2, "Nguồn thanh toán (đồng)", sourceX, 0, sourceW, tier1H, fontHeader);

            String[] subTitles = {"Quỹ\nBHYT", "Người\nbệnh\ncùng\nchi trả", "Khác", "Người\nbệnh tự\ntrả"};
            for (int i = 0; i < 4; i++) {
                int cIdx = 9 + i;
                g2.drawRect(colX[cIdx], tier1H, colWidths[cIdx], tier2H);
                drawCenteredMultiLineText(g2, subTitles[i], colX[cIdx], tier1H, colWidths[cIdx], tier2H, fontHeader);
            }

            int colNumY = headerHeight;
            g2.setFont(fontColNum);
            for (int c = 0; c < colWidths.length; c++) {
                g2.drawRect(colX[c], colNumY, colWidths[c], colNumRowHeight);
                String numStr = "(" + (c + 1) + ")";
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(numStr);
                g2.drawString(numStr, colX[c] + (colWidths[c] - tw) / 2, colNumY + ((colNumRowHeight - fm.getHeight()) / 2) + fm.getAscent());
            }

            int curY = headerHeight + colNumRowHeight;
            Shape origClip = g2.getClip();
            if (rows.isEmpty()) {
                g2.setFont(fontRegular);
                for (int c = 0; c < colWidths.length; c++) g2.drawRect(colX[c], curY, colWidths[c], rowHeight);
                g2.drawString("(Chưa phát sinh chi phí)", colX[0] + 8, curY + 15);
                g2.dispose();
                return;
            }

            for (CostRow row : rows) {
                g2.setFont(row.bold() ? fontBold : fontRegular);
                String[] cells = {
                        row.content(), row.unit(), row.quantity(), row.unitPriceBv(), row.unitPriceBh(),
                        row.servicePayRate(), row.totalBv(), row.bhPayRate(), row.totalBh(),
                        row.bhytFundPaid(), row.patientCoPay(), row.otherPaid(), row.patientSelfPay()
                };
                for (int c = 0; c < colWidths.length; c++) {
                    g2.drawRect(colX[c], curY, colWidths[c], rowHeight);
                    String val = cells[c];
                    if (val == null || val.isEmpty()) continue;
                    g2.setClip(colX[c] + 1, curY + 1, colWidths[c] - 2, rowHeight - 2);
                    FontMetrics fm = g2.getFontMetrics();
                    int textY = curY + ((rowHeight - fm.getHeight()) / 2) + fm.getAscent();
                    if (c == 0) {
                        int pad = row.indent() > 0 ? 12 : 4;
                        g2.drawString(val, colX[c] + pad, textY);
                    } else if (c == 1 || c == 2) {
                        g2.drawString(val, colX[c] + (colWidths[c] - fm.stringWidth(val)) / 2, textY);
                    } else {
                        g2.drawString(val, colX[c] + colWidths[c] - fm.stringWidth(val) - 4, textY);
                    }
                    g2.setClip(origClip);
                }
                curY += rowHeight;
            }
            g2.dispose();
        }

        private void drawCenteredMultiLineText(Graphics2D g2, String text, int x, int y, int w, int h, Font font) {
            Shape orig = g2.getClip();
            g2.setClip(x + 1, y + 1, w - 2, h - 2);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            String[] lines = text.split("\n");
            int totalH = lines.length * (fm.getHeight() - 3);
            int startY = y + (h - totalH) / 2 + fm.getAscent() - 1;
            for (int i = 0; i < lines.length; i++) {
                g2.drawString(lines[i], x + (w - fm.stringWidth(lines[i])) / 2, startY + i * (fm.getHeight() - 3));
            }
            g2.setClip(orig);
        }
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static String fmt(BigDecimal v) { return MONEY_FMT.format(nz(v)); }
    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private JLabel createDocText(String text, boolean isBold) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Times New Roman", isBold ? Font.BOLD : Font.PLAIN, 13));
        l.setForeground(Color.BLACK);
        return l;
    }

    private JLabel createNumberedBox(String num) {
        JLabel l = new JLabel(num, SwingConstants.CENTER);
        l.setFont(new Font("Times New Roman", Font.BOLD, 13));
        l.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        l.setPreferredSize(new Dimension(38, 18));
        return l;
    }

    private JLabel createBoxText(String text, int width) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Times New Roman", Font.BOLD, 12));
        l.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        l.setPreferredSize(new Dimension(width, 18));
        return l;
    }

    private static class CheckmarkIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(40, 165, 70));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 7, x + 5, y + 11);
            g2.drawLine(x + 5, y + 11, x + 12, y + 2);
            g2.dispose();
        }
    }

    private static class BarcodeVectorIcon implements Icon {
        private final int width, height;
        BarcodeVectorIcon(int w, int h) { this.width = w; this.height = h; }
        @Override public int getIconWidth() { return width; }
        @Override public int getIconHeight() { return height; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.BLACK);
            int[] barWidths = {2, 1, 3, 1, 2, 4, 1, 2, 1, 3, 2, 1, 4, 1, 2, 3, 1, 2, 4, 1, 2, 1, 3, 2, 1, 4, 2, 1, 3, 1, 2};
            int curX = x;
            for (int i = 0; i < barWidths.length && curX < x + width; i++) {
                int w = barWidths[i];
                if (i % 2 == 0) g2.fillRect(curX, y, Math.min(w, x + width - curX), height);
                curX += w;
            }
            g2.dispose();
        }
    }

    private static class PrinterVectorIcon implements Icon {
        private final int w, h;
        PrinterVectorIcon(int w, int h) { this.w = w; this.h = h; }
        @Override public int getIconWidth() { return w; }
        @Override public int getIconHeight() { return h; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.drawRect(x + 4, y + 1, 11, 5);
            g2.fillRect(x + 1, y + 6, 17, 7);
            g2.setColor(new Color(40, 40, 40));
            g2.fillRect(x + 4, y + 10, 11, 6);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 5, y + 11, 9, 7);
            g2.dispose();
        }
    }
}
