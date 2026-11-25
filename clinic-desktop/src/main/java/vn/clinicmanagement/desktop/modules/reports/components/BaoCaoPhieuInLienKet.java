package vn.clinicmanagement.desktop.modules.reports.components;

import vn.clinicmanagement.application.reports.PhieuBaoCaoReadCongKetNoi.Loai;
import vn.clinicmanagement.application.reports.PhieuBaoCaoReadCongKetNoi.Phieu;
import vn.clinicmanagement.desktop.modules.examination.components.print.*;
import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cầu nối mở đúng mẫu phiếu in xem trước từ một dòng dữ liệu trên màn hình Danh Sách Báo Cáo.
 */
public final class BaoCaoPhieuInLienKet {
    private static final DateTimeFormatter DATE_VN = DateTimeFormatter.ofPattern("'Ngày' dd 'tháng' MM 'năm' yyyy");
    private static final DateTimeFormatter TIME_VN = DateTimeFormatter.ofPattern("HH 'giờ' mm 'ngày' dd 'tháng' MM 'năm' yyyy");
    private static final DateTimeFormatter ORDER_TIME = DateTimeFormatter.ofPattern("HH:mm, 'ngày' dd 'tháng' MM 'năm' yyyy");

    private BaoCaoPhieuInLienKet() {}

    /** Mở hộp thoại xem trước mẫu phiếu in phù hợp với loại phiếu đang chọn (chạy nền truy vấn CSDL). */
    public static void openForTicket(Window owner, Phieu ticket) {
        if (ticket == null || GraphicsEnvironment.isHeadless()) return;
        new SwingWorker<JDialog, Void>() {
            private String error;
            @Override
            protected JDialog doInBackground() {
                try {
                    return buildDialog(owner, ticket);
                } catch (Exception ex) {
                    error = ex.getMessage();
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    JDialog dialog = get();
                    if (dialog != null) {
                        dialog.setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(owner,
                                error != null && !error.isBlank()
                                        ? error
                                        : "Không tải được mẫu phiếu in cho dòng đã chọn.",
                                "Không thể mở phiếu in",
                                JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(owner,
                            "Không thể mở mẫu phiếu in: " + ex.getMessage(),
                            "Lỗi mở phiếu in",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private static JDialog buildDialog(Window owner, Phieu ticket) {
        return switch (ticket.type()) {
            case HOA_DON, PHIEU_THU, PHIEU_HOAN_TRA -> {
                var slip = YTeVienPhiPhieuManHinh.loadFromDatabase(ticket.visitId());
                if (slip == null) {
                    throw new IllegalStateException("Chưa tìm thấy dữ liệu hóa đơn / viện phí cho lượt khám #" + ticket.visitId());
                }
                yield new YTeVienPhiPhieuHopThoai(owner, slip);
            }
            case DON_THUOC -> new DonThuocPrintPreviewHopThoai(owner, loadPrescriptionData(ticket));
            case CHI_DINH -> new LamSangYeuCauPrintPreviewHopThoai(owner, loadClinicalOrderData(ticket));
            case KHAM_BENH -> new NgoaiTruYTeRecordPrintHopThoai(owner, loadMedicalRecordData(ticket));
            case LICH_HEN -> buildAppointmentDialog(owner, ticket);
        };
    }

    private static DonThuocPrintPreviewManHinh.PrescriptionData loadPrescriptionData(Phieu ticket) {
        String pName = safe(ticket.patientName()).toUpperCase(Locale.ROOT);
        String pCode = safe(ticket.patientCode());
        String birthYear = "", gender = "", bhyt = "", diagnosis = "";
        String doctor = safe(ticket.author());
        List<DonThuocPrintPreviewManHinh.PrescriptionItem> meds = new ArrayList<>();

        try (Connection c = CoSoDuLieuKetNoi.getConnection()) {
            String headerSql = "SELECT bn.maBenhNhanFormat, bn.ho + N' ' + bn.ten AS hoTen, bn.ngaySinh, bn.gioiTinh, "
                    + "ISNULL(bs.ho + N' ' + bs.ten, N'') AS bacSi, "
                    + "(SELECT TOP 1 soBaoHiem FROM dbo.BaoHiem bh WHERE bh.maBenhNhan = bn.id AND bh.isDeleted = 0 ORDER BY bh.id DESC) AS soBaoHiem "
                    + "FROM dbo.DonThuoc dt "
                    + "JOIN dbo.LuotKham lk ON lk.id = dt.maLuotKham "
                    + "JOIN dbo.BenhNhan bn ON bn.id = lk.maBenhNhan "
                    + "LEFT JOIN dbo.NhanVien bs ON bs.id = dt.maBacSi "
                    + "WHERE dt.id = ? AND dt.isDeleted = 0";
            try (PreparedStatement ps = c.prepareStatement(headerSql)) {
                ps.setInt(1, ticket.id());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        pCode = safe(rs.getString("maBenhNhanFormat"));
                        pName = safe(rs.getString("hoTen")).toUpperCase(Locale.ROOT);
                        Date dob = rs.getDate("ngaySinh");
                        if (dob != null) birthYear = String.valueOf(dob.toLocalDate().getYear());
                        gender = formatGender(rs.getString("gioiTinh"));
                        bhyt = safe(rs.getString("soBaoHiem"));
                        String bs = safe(rs.getString("bacSi"));
                        if (!bs.isBlank()) doctor = bs;
                    }
                }
            }
            diagnosis = loadDiagnosis(c, ticket.visitId());
            String medSql = "SELECT it.tenItem, ISNULL(it.donViCoBan, N'Viên') AS dvt, ct.soLuongKe, "
                    + "ISNULL(ct.lieuDung, N'') AS lieuDung, ISNULL(ct.huongDan, N'') AS huongDan, ISNULL(ct.duongDung, N'') AS duongDung "
                    + "FROM dbo.ChiTietDonThuoc ct JOIN dbo.Item it ON it.id = ct.maItem "
                    + "WHERE ct.maDonThuoc = ? AND ct.isDeleted = 0 ORDER BY ct.id";
            try (PreparedStatement ps = c.prepareStatement(medSql)) {
                ps.setInt(1, ticket.id());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        var qty = rs.getBigDecimal("soLuongKe");
                        String qtyStr = qty == null ? "1" : qty.stripTrailingZeros().toPlainString();
                        String instr = joinNonBlank(" - ", rs.getString("duongDung"), rs.getString("lieuDung"), rs.getString("huongDan"));
                        meds.add(new DonThuocPrintPreviewManHinh.PrescriptionItem(
                                safe(rs.getString("tenItem")), qtyStr, safe(rs.getString("dvt")), instr));
                    }
                }
            }
        } catch (SQLException ignored) {}

        return new DonThuocPrintPreviewManHinh.PrescriptionData(
                pName, pCode, birthYear, gender, bhyt, diagnosis, meds, doctor);
    }

    private static LamSangYeuCauPrintPreviewManHinh.ClinicalOrderData loadClinicalOrderData(Phieu ticket) {
        String pCode = safe(ticket.patientCode());
        String pName = safe(ticket.patientName()).toUpperCase(Locale.ROOT);
        String birthYear = "", age = "", gender = "", bhyt = "", expiry = "", address = "", dept = safe(ticket.department());
        String diagnosis = "", invoiceCode = "", daysLeft = "";
        String orderTime = ticket.date() != null ? ticket.date().format(ORDER_TIME) : LocalDateTime.now().format(ORDER_TIME);
        List<LamSangYeuCauPrintPreviewManHinh.OrderItem> items = new ArrayList<>();

        try (Connection c = CoSoDuLieuKetNoi.getConnection()) {
            String visitSql = "SELECT bn.maBenhNhanFormat, bn.ho + N' ' + bn.ten AS hoTen, bn.ngaySinh, bn.gioiTinh, bn.diaChi, "
                    + "ISNULL(k.tenKhoa, N'Khoa Khám Bệnh') AS tenKhoa, "
                    + "(SELECT TOP 1 soHoaDon FROM dbo.HoaDon h WHERE h.maLuotKham = lk.id AND h.isDeleted = 0 ORDER BY h.id DESC) AS soHoaDon, "
                    + "(SELECT TOP 1 soBaoHiem FROM dbo.BaoHiem bh WHERE bh.maBenhNhan = bn.id AND bh.isDeleted = 0 ORDER BY bh.id DESC) AS soBaoHiem, "
                    + "(SELECT TOP 1 ngayHetHan FROM dbo.BaoHiem bh WHERE bh.maBenhNhan = bn.id AND bh.isDeleted = 0 ORDER BY bh.id DESC) AS ngayHetHan "
                    + "FROM dbo.LuotKham lk JOIN dbo.BenhNhan bn ON bn.id = lk.maBenhNhan LEFT JOIN dbo.Khoa k ON k.id = lk.maKhoa "
                    + "WHERE lk.id = ? AND lk.isDeleted = 0";
            try (PreparedStatement ps = c.prepareStatement(visitSql)) {
                ps.setInt(1, ticket.visitId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        pCode = safe(rs.getString("maBenhNhanFormat"));
                        pName = safe(rs.getString("hoTen")).toUpperCase(Locale.ROOT);
                        Date dob = rs.getDate("ngaySinh");
                        if (dob != null) {
                            birthYear = String.valueOf(dob.toLocalDate().getYear());
                            age = String.valueOf(Math.max(0, LocalDate.now().getYear() - dob.toLocalDate().getYear()));
                        }
                        gender = formatGender(rs.getString("gioiTinh"));
                        address = safe(rs.getString("diaChi"));
                        dept = safe(rs.getString("tenKhoa"));
                        invoiceCode = safe(rs.getString("soHoaDon"));
                        bhyt = safe(rs.getString("soBaoHiem"));
                        Date exp = rs.getDate("ngayHetHan");
                        if (exp != null) {
                            expiry = exp.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), exp.toLocalDate());
                            daysLeft = days >= 0 ? ("Còn " + days + " ngày") : "Hết hạn";
                        }
                    }
                }
            }
            diagnosis = loadDiagnosis(c, ticket.visitId());
            String ordSql = "SELECT dv.tenDichVu, ISNULL(dv.nhomDichVu, dv.loaiDichVu) AS nhom, cd.soLuong, ISNULL(cd.ghiChu, N'') AS ghiChu "
                    + "FROM dbo.ChiDinhDichVu cd JOIN dbo.DanhMucDichVu dv ON dv.id = cd.maDichVu "
                    + "WHERE cd.maLuotKham = ? AND cd.isDeleted = 0 AND cd.trangThai <> 'DA_HUY' ORDER BY cd.id";
            try (PreparedStatement ps = c.prepareStatement(ordSql)) {
                ps.setInt(1, ticket.visitId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        var qty = rs.getBigDecimal("soLuong");
                        String qtyStr = qty == null ? "1" : qty.stripTrailingZeros().toPlainString();
                        items.add(new LamSangYeuCauPrintPreviewManHinh.OrderItem(
                                safe(rs.getString("nhom")), safe(rs.getString("tenDichVu")), qtyStr, "Lần", safe(rs.getString("ghiChu"))));
                    }
                }
            }
        } catch (SQLException ignored) {}

        if (items.isEmpty()) {
            String svc = safe(ticket.details().get("Dịch vụ"));
            if (!svc.isBlank()) {
                items.add(new LamSangYeuCauPrintPreviewManHinh.OrderItem("Cận lâm sàng", svc, "1", "Lần", safe(ticket.details().get("Kết luận"))));
            }
        }

        return new LamSangYeuCauPrintPreviewManHinh.ClinicalOrderData(
                pCode, pName, birthYear, age, gender, bhyt, expiry, address, dept, diagnosis,
                "BA-" + ticket.visitId(), pCode, invoiceCode, daysLeft, orderTime, items);
    }

    private static YTeRecordPrintPreviewManHinh.MedicalRecordData loadMedicalRecordData(Phieu ticket) {
        String pName = safe(ticket.patientName()).toUpperCase(Locale.ROOT);
        String pCode = safe(ticket.patientCode());
        String birthYear = "", gender = "", bhyt = "", address = "";
        String reason = "", illness = "", personal = "", family = "", exam = "", diagnosis = "", treatment = "";

        try (Connection c = CoSoDuLieuKetNoi.getConnection()) {
            String sql = "SELECT bn.maBenhNhanFormat, bn.ho + N' ' + bn.ten AS hoTen, bn.ngaySinh, bn.gioiTinh, bn.diaChi, "
                    + "lk.lyDoKham, lk.quaTrinhBenhLy, lk.tienSuBanThan, lk.tienSuGiaDinh, lk.khamToanThan, lk.khamBoPhan, lk.huongDieuTri, lk.loiDanBacSi, "
                    + "(SELECT TOP 1 soBaoHiem FROM dbo.BaoHiem bh WHERE bh.maBenhNhan = bn.id AND bh.isDeleted = 0 ORDER BY bh.id DESC) AS soBaoHiem "
                    + "FROM dbo.LuotKham lk JOIN dbo.BenhNhan bn ON bn.id = lk.maBenhNhan "
                    + "WHERE lk.id = ? AND lk.isDeleted = 0";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, ticket.visitId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        pCode = safe(rs.getString("maBenhNhanFormat"));
                        pName = safe(rs.getString("hoTen")).toUpperCase(Locale.ROOT);
                        Date dob = rs.getDate("ngaySinh");
                        if (dob != null) birthYear = String.valueOf(dob.toLocalDate().getYear());
                        gender = formatGender(rs.getString("gioiTinh"));
                        address = safe(rs.getString("diaChi"));
                        bhyt = safe(rs.getString("soBaoHiem"));
                        reason = safe(rs.getString("lyDoKham"));
                        illness = safe(rs.getString("quaTrinhBenhLy"));
                        personal = safe(rs.getString("tienSuBanThan"));
                        family = safe(rs.getString("tienSuGiaDinh"));
                        exam = joinNonBlank("\n", rs.getString("khamToanThan"), rs.getString("khamBoPhan"));
                        treatment = joinNonBlank("\n", rs.getString("huongDieuTri"), rs.getString("loiDanBacSi"));
                    }
                }
            }
            diagnosis = loadDiagnosis(c, ticket.visitId());
        } catch (SQLException ignored) {}

        return new YTeRecordPrintPreviewManHinh.MedicalRecordData(
                pName, pCode, birthYear, gender, bhyt, address,
                "BA-" + ticket.visitId(), reason, illness, personal, family, exam, diagnosis, treatment);
    }

    private static JDialog buildAppointmentDialog(Window owner, Phieu ticket) {
        String code = safe(ticket.patientCode());
        String name = safe(ticket.patientName()).toUpperCase(Locale.ROOT);
        String age = "", gender = "", address = "";
        String appointTime = ticket.date() != null ? ticket.date().format(TIME_VN) : "";
        String instructions = safe(ticket.details().get("Lý do hẹn"));
        String date = LocalDate.now().format(DATE_VN);
        String doctor = safe(ticket.author());

        try (Connection c = CoSoDuLieuKetNoi.getConnection()) {
            String sql = "SELECT bn.maBenhNhanFormat, bn.ho + N' ' + bn.ten AS hoTen, bn.ngaySinh, bn.gioiTinh, bn.diaChi, "
                    + "lh.thoiGianBatDau, lh.lyDo, ISNULL(bs.ho + N' ' + bs.ten, N'') AS bacSi "
                    + "FROM dbo.LichHen lh JOIN dbo.BenhNhan bn ON bn.id = lh.maBenhNhan "
                    + "LEFT JOIN dbo.NhanVien bs ON bs.id = lh.maBacSi "
                    + "WHERE lh.id = ? AND lh.isDeleted = 0";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, ticket.id());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        code = safe(rs.getString("maBenhNhanFormat"));
                        name = safe(rs.getString("hoTen")).toUpperCase(Locale.ROOT);
                        Date dob = rs.getDate("ngaySinh");
                        if (dob != null) age = String.valueOf(Math.max(0, LocalDate.now().getYear() - dob.toLocalDate().getYear()));
                        gender = formatGender(rs.getString("gioiTinh"));
                        address = safe(rs.getString("diaChi"));
                        Timestamp at = rs.getTimestamp("thoiGianBatDau");
                        if (at != null) appointTime = at.toLocalDateTime().format(TIME_VN);
                        String lyDo = safe(rs.getString("lyDo"));
                        if (!lyDo.isBlank()) instructions = lyDo;
                        String bs = safe(rs.getString("bacSi"));
                        if (!bs.isBlank()) doctor = bs;
                    }
                }
            }
        } catch (SQLException ignored) {}

        return new TheoTiepLichHenPrintHopThoai(owner, code, name, age, gender, address, appointTime, instructions, date, doctor);
    }

    private static String loadDiagnosis(Connection c, int visitId) {
        if (visitId <= 0) return "";
        String sql = "SELECT cd.chanDoanText, icd.maICD FROM dbo.ChanDoan cd "
                + "LEFT JOIN dbo.ICD icd ON cd.maICD = icd.id "
                + "WHERE cd.maLuotKham = ? AND cd.isDeleted = 0 ORDER BY cd.laChinh DESC, cd.id DESC";
        List<String> parts = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, visitId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = safe(rs.getString("maICD"));
                    String text = safe(rs.getString("chanDoanText"));
                    if (!text.isBlank()) parts.add(code.isBlank() ? text : (code + " - " + text));
                }
            }
        } catch (SQLException ignored) {}
        return String.join("; ", parts);
    }

    private static String formatGender(String g) {
        String s = safe(g);
        if ("M".equalsIgnoreCase(s) || "NAM".equalsIgnoreCase(s)) return "Nam";
        return s.isBlank() ? "" : "Nữ";
    }

    private static String joinNonBlank(String sep, String... items) {
        List<String> out = new ArrayList<>();
        for (String item : items) {
            if (item != null && !item.isBlank()) out.add(item.trim());
        }
        return String.join(sep, out);
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
