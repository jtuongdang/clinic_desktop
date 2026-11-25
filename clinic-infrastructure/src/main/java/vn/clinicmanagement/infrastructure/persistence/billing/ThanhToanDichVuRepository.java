package vn.clinicmanagement.infrastructure.persistence.billing;

import vn.clinicmanagement.application.billing.ThanhToanDichVuReadCongKetNoi;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Read-only source metadata and patient insurance lookup, restricted by invoice, visit and facility. */
public final class ThanhToanDichVuRepository implements ThanhToanDichVuReadCongKetNoi {
    private final JdbcKetNoiNhaCungCap connections;

    public ThanhToanDichVuRepository(JdbcKetNoiNhaCungCap connections) {
        this.connections = Objects.requireNonNull(connections);
    }

    static final String SQL = """
        SELECT ct.id, COALESCE(cd.thoiGianChiDinh,dt.ngayKe,CASE WHEN ct.loaiDoiTuong='DICH_VU_KHAM' THEN lk.thoiGianTiepNhan END) orderedAt,
          COALESCE(dv.maDichVu,exam.maDichVu,rxItem.maItem,CASE WHEN ct.loaiDoiTuong='DON_THUOC' THEN CONCAT(N'DT-',dt.id) ELSE N'' END,N'') equivalentCode,
          COALESCE(dv.loaiDichVu,exam.loaiDichVu,ct.loaiDoiTuong) category,
          CASE WHEN ct.loaiDoiTuong='DON_THUOC' THEN COALESCE(NULLIF(rxItem.donViCoBan,N''),N'Viên') ELSE N'Lần' END unit,
          ISNULL(k.tenKhoa,N'') department,
          COALESCE(bs.ho+N' '+bs.ten,rxbs.ho+N' '+rxbs.ten,lkbs.ho+N' '+lkbs.ten,N'') doctor,
          COALESCE(dv.giaBHYT,exam.giaBHYT,ct.giaBHYT) catalogInsurancePrice,
          COALESCE(dv.donGia,exam.donGia,NULLIF(rxItem.donGiaKe,0),NULLIF(rxItem.giaBanMacDinh,0),ct.donGia) catalogServicePrice,
          rxItem.thuocBHYT,
          rxItem.thongTinThuoc
        FROM ChiTietHoaDon ct JOIN HoaDon h ON h.id=ct.maHoaDon AND h.isDeleted=0
        JOIN LuotKham lk ON lk.id=h.maLuotKham AND lk.isDeleted=0
        LEFT JOIN ChiDinhDichVu cd ON ct.loaiDoiTuong='DICH_VU_CLS' AND cd.id=ct.maDoiTuong AND cd.maLuotKham=lk.id
        LEFT JOIN DanhMucDichVu dv ON dv.id=cd.maDichVu
        LEFT JOIN DanhMucDichVu exam ON ct.loaiDoiTuong='DICH_VU_KHAM' AND exam.id=ct.maDoiTuong
        LEFT JOIN DonThuoc dt ON ct.loaiDoiTuong='DON_THUOC' AND dt.id=ct.maDoiTuong AND dt.maLuotKham=lk.id
        OUTER APPLY (
            SELECT TOP 1 it.maItem, it.donViCoBan, ctdt.donGiaKe, it.giaBanMacDinh, it.thuocBHYT, it.thongTinThuoc
            FROM ChiTietDonThuoc ctdt
            JOIN Item it ON it.id=ctdt.maItem
            WHERE ct.loaiDoiTuong='DON_THUOC' AND ctdt.maDonThuoc=ct.maDoiTuong AND ctdt.isDeleted=0
              AND (ct.moTa = CONCAT(it.tenItem, N' (', it.donViCoBan, N')') OR ct.moTa LIKE CONCAT(it.tenItem, N'%'))
            ORDER BY ctdt.id
        ) rxItem
        LEFT JOIN NhanVien bs ON bs.id=cd.maBacSiChiDinh
        LEFT JOIN NhanVien rxbs ON rxbs.id=dt.maBacSi
        LEFT JOIN NhanVien lkbs ON lkbs.id=lk.maBacSi
        LEFT JOIN Khoa k ON k.id=lk.maKhoa
        WHERE h.id=? AND h.maLuotKham=? AND h.maCoSo=? AND lk.maCoSo=? AND ct.isDeleted=0
        ORDER BY ct.id
        """;

    static final String INSURANCE_SQL = """
        SELECT TOP 1 soBaoHiem, ngayBatDau, ngayHetHan, mucHuong
        FROM BaoHiem
        WHERE maBenhNhan=? AND isDeleted=0
        ORDER BY ngayHetHan DESC, id DESC
        """;

    @Override
    public Map<Integer, NguonDong> getByInvoice(int invoiceId, int visitId, int facilityId) {
        Map<Integer, NguonDong> result = new LinkedHashMap<>();
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(SQL)) {
            ps.setInt(1, invoiceId);
            ps.setInt(2, visitId);
            ps.setInt(3, facilityId);
            ps.setInt(4, facilityId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp at = rs.getTimestamp("orderedAt");
                    BigDecimal insPrice = rs.getBigDecimal("catalogInsurancePrice");
                    BigDecimal svcPrice = rs.getBigDecimal("catalogServicePrice");
                    String drugXml = rs.getString("thongTinThuoc");
                    Boolean drugInsured = (Boolean) rs.getObject("thuocBHYT");
                    if (drugXml != null && !drugXml.isBlank()) {
                        Properties props = parseXmlProps(drugXml);
                        BigDecimal xmlBhyt = parseDecimal(props.getProperty("giaBHYT"));
                        BigDecimal xmlVienPhi = parseDecimal(props.getProperty("giaVienPhi"));
                        if (xmlBhyt != null && xmlBhyt.signum() > 0 && (insPrice == null || insPrice.signum() <= 0)) {
                            insPrice = xmlBhyt;
                        }
                        if (xmlVienPhi != null && xmlVienPhi.signum() > 0) {
                            svcPrice = xmlVienPhi;
                        }
                    }
                    if ((insPrice == null || insPrice.signum() <= 0) && Boolean.TRUE.equals(drugInsured)
                            && svcPrice != null && svcPrice.signum() > 0) {
                        insPrice = svcPrice;
                    }
                    result.put(rs.getInt("id"), new NguonDong(
                            at == null ? null : at.toLocalDateTime(),
                            rs.getString("equivalentCode"),
                            rs.getString("category"),
                            rs.getString("unit"),
                            rs.getString("department"),
                            rs.getString("doctor"),
                            insPrice,
                            svcPrice
                    ));
                }
            }
            return Map.copyOf(result);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải thông tin dịch vụ của hóa đơn.", ex);
        }
    }

    @Override
    public Optional<ThongTinBaoHiemBenhNhan> getActiveInsuranceByPatient(int patientId) {
        if (patientId <= 0) return Optional.empty();
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(INSURANCE_SQL)) {
            ps.setInt(1, patientId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                java.sql.Date start = rs.getDate("ngayBatDau");
                java.sql.Date end = rs.getDate("ngayHetHan");
                return Optional.of(new ThongTinBaoHiemBenhNhan(
                        rs.getString("soBaoHiem"),
                        start != null ? start.toLocalDate() : null,
                        end != null ? end.toLocalDate() : null,
                        rs.getInt("mucHuong")
                ));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể truy vấn thẻ BHYT của bệnh nhân.", ex);
        }
    }

    private static Properties parseXmlProps(String xml) {
        Properties props = new Properties();
        try {
            props.loadFromXML(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ignored) {
        }
        return props;
    }

    private static BigDecimal parseDecimal(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
