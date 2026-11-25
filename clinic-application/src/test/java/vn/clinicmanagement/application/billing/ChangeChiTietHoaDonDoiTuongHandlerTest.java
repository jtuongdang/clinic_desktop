package vn.clinicmanagement.application.billing;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.billing.payer.ChangeChiTietHoaDonDoiTuongCommand;
import vn.clinicmanagement.application.billing.payer.ChangeChiTietHoaDonDoiTuongHandler;
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuHoanTra;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;
import vn.clinicmanagement.domain.repositories.ITieuChuan;
import vn.clinicmanagement.domain.repositories.PhanTrangKetQua;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ChangeChiTietHoaDonDoiTuongHandlerTest {

    static BigDecimal bd(String val) { return new BigDecimal(val); }

    static PhanQuyenNguCanh auth(String role, int facility) {
        return new PhanQuyenNguCanh(3, 1, role, facility, 1, UUID.randomUUID());
    }

    static KiemToanTacNhanNguCanh actor(String role, int facility) {
        return new KiemToanTacNhanNguCanh(3, 1, role, facility, 1, "WS-TEST", UUID.randomUUID());
    }

    static HoaDon visit5015PaidInvoice() {
        HoaDon inv = new HoaDon();
        inv.setId(2001);
        inv.setSoHoaDon("DK26111900005015");
        inv.setMaLuotKham(5015);
        inv.setMaBenhNhan(3001);
        inv.setMaCoSo(1);
        inv.setNgayLap(LocalDateTime.of(2026, 11, 19, 21, 52));

        ChiTietHoaDon c1 = new ChiTietHoaDon();
        c1.setId(3001);
        c1.setLoaiDoiTuong("DICH_VU_KHAM");
        c1.setMaDoiTuong(2);
        c1.setMoTa("Khám Nội tổng quát");
        c1.setSoLuong(BigDecimal.ONE);
        c1.setDonGia(bd("100000.00"));
        inv.addChiTiet(c1);

        ChiTietHoaDon c2 = new ChiTietHoaDon();
        c2.setId(3002);
        c2.setLoaiDoiTuong("DICH_VU_CLS");
        c2.setMaDoiTuong(3001);
        c2.setMoTa("Tổng phân tích tế bào máu ngoại vi");
        c2.setSoLuong(BigDecimal.ONE);
        c2.setDonGia(bd("85000.00"));
        inv.addChiTiet(c2);

        ChiTietHoaDon c3 = new ChiTietHoaDon();
        c3.setId(3003);
        c3.setLoaiDoiTuong("DON_THUOC");
        c3.setMaDoiTuong(3001);
        c3.setMoTa("Thuốt Mới (Viên)");
        c3.setSoLuong(bd("10"));
        c3.setDonGia(bd("10000.00"));
        inv.addChiTiet(c3);

        ChiTietHoaDon c4 = new ChiTietHoaDon();
        c4.setId(3004);
        c4.setLoaiDoiTuong("DON_THUOC");
        c4.setMaDoiTuong(3001);
        c4.setMoTa("Vitamin C 500mg (Viên)");
        c4.setSoLuong(bd("10"));
        c4.setDonGia(bd("1000.00"));
        inv.addChiTiet(c4);

        inv.recalculateCharges();
        inv.recordPayment(bd("185000.00"), PhuongThucThanhToan.TIEN_MAT, 3, "Thu đợt 1");
        inv.recordPayment(bd("110000.00"), PhuongThucThanhToan.TIEN_MAT, 3, "Thu đợt 2");
        return inv;
    }

    static ThanhToanDichVuReadCongKetNoi catalogWithValidCard(int coverageRate) {
        return new ThanhToanDichVuReadCongKetNoi() {
            @Override
            public Map<Integer, NguonDong> getByInvoice(int invoiceId, int visitId, int facilityId) {
                var at = LocalDateTime.of(2026, 11, 19, 21, 52);
                return Map.of(
                        3001, new NguonDong(at, "DV_KHAM_NOI", "KHAM", "Lần", "Khoa Khám Bệnh", "Đặng J Tường", bd("39000.00"), bd("100000.00")),
                        3002, new NguonDong(at, "XN_CTM", "XET_NGHIEM", "Lần", "Khoa Khám Bệnh", "Đặng J Tường", bd("42000.00"), bd("85000.00")),
                        3003, new NguonDong(at, "DT-3001", "DON_THUOC", "Viên", "Khoa Khám Bệnh", "Đặng J Tường", null, bd("10000.00")),
                        3004, new NguonDong(at, "DT-3001", "DON_THUOC", "Viên", "Khoa Khám Bệnh", "Đặng J Tường", null, bd("1000.00"))
                );
            }

            @Override
            public Optional<ThongTinBaoHiemBenhNhan> getActiveInsuranceByPatient(int patientId) {
                if (coverageRate <= 0) return Optional.empty();
                return Optional.of(new ThongTinBaoHiemBenhNhan(
                        "DN4010337378888",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        coverageRate
                ));
            }
        };
    }

    @Test
    void convertsPaidServiceFromThuPhiToBhytAndBackGeneratingAndClearingPendingRefund() {
        TestInvoiceRepo repo = new TestInvoiceRepo(visit5015PaidInvoice());
        var handler = new ChangeChiTietHoaDonDoiTuongHandler(repo, catalogWithValidCard(80), new NoopAudit());

        // 1. Chuyển Khám Nội tổng quát (100,000) từ Thu phí -> BHYT (giá BHYT 39,000, hưởng 80% = 31,200; BN trả 7,800)
        var toBhyt = handler.handle(new ChangeChiTietHoaDonDoiTuongCommand(
                2001, 3001, "BHYT", auth("DOCTOR", 1), actor("DOCTOR", 1)));
        assertTrue(toBhyt.isSuccess());
        var res1 = toBhyt.valueOrThrow();
        assertEquals("THU_PHI", res1.previousPayerType());
        assertEquals("BHYT", res1.currentPayerType());
        assertEquals(0, bd("234000.00").compareTo(res1.totalAmount()));
        assertEquals(0, bd("31200.00").compareTo(res1.insurancePaidAmount()));
        assertEquals(0, bd("202800.00").compareTo(res1.patientPaidAmount()));
        assertEquals(0, bd("295000.00").compareTo(res1.amountPaid()));
        assertEquals(0, bd("92200.00").compareTo(res1.pendingRefundAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(res1.remainingAmount()));
        assertEquals(TrangThaiHoaDon.DA_THANH_TOAN, res1.invoiceStatus());

        // 2. Chuyển ngược lại BHYT -> Thu phí trước khi chi hoàn tiền: Cần hoàn tự về 0, Tổng về 295,000
        var backToThuPhi = handler.handle(new ChangeChiTietHoaDonDoiTuongCommand(
                2001, 3001, "THU_PHI", auth("CASHIER", 1), actor("CASHIER", 1)));
        assertTrue(backToThuPhi.isSuccess());
        var res2 = backToThuPhi.valueOrThrow();
        assertEquals("BHYT", res2.previousPayerType());
        assertEquals("THU_PHI", res2.currentPayerType());
        assertEquals(0, bd("295000.00").compareTo(res2.totalAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(res2.insurancePaidAmount()));
        assertEquals(0, bd("295000.00").compareTo(res2.patientPaidAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(res2.pendingRefundAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(res2.remainingAmount()));
    }

    @Test
    void convertsToBhytDichVuAndMienPhiAccurately() {
        TestInvoiceRepo repo = new TestInvoiceRepo(visit5015PaidInvoice());
        var handler = new ChangeChiTietHoaDonDoiTuongHandler(repo, catalogWithValidCard(80), new NoopAudit());

        // BHYT + Dịch vụ trên Khám Nội (giữ giá 100,000; BHYT trả 80% của 39,000 = 31,200; BN trả 68,800)
        var bhytDv = handler.handle(new ChangeChiTietHoaDonDoiTuongCommand(
                2001, 3001, "BHYT_DICH_VU", auth("DOCTOR", 1), actor("DOCTOR", 1)));
        assertTrue(bhytDv.isSuccess());
        var r1 = bhytDv.valueOrThrow();
        assertEquals(0, bd("295000.00").compareTo(r1.totalAmount()));
        assertEquals(0, bd("31200.00").compareTo(r1.insurancePaidAmount()));
        assertEquals(0, bd("263800.00").compareTo(r1.patientPaidAmount()));
        assertEquals(0, bd("31200.00").compareTo(r1.pendingRefundAmount()));

        // Chuyển Xét nghiệm (85,000) sang Miễn phí
        var mienPhi = handler.handle(new ChangeChiTietHoaDonDoiTuongCommand(
                2001, 3002, "MIEN_PHI", auth("CASHIER", 1), actor("CASHIER", 1)));
        assertTrue(mienPhi.isSuccess());
        var r2 = mienPhi.valueOrThrow();
        assertEquals(0, bd("85000.00").compareTo(r2.serviceDiscountAmount()));
        assertEquals(0, bd("178800.00").compareTo(r2.patientPaidAmount()));
        assertEquals(0, bd("116200.00").compareTo(r2.pendingRefundAmount()));
    }

    @Test
    void rejectsBhytConversionWhenCardMissingOrServiceHasNoBhytPrice() {
        TestInvoiceRepo repo = new TestInvoiceRepo(visit5015PaidInvoice());

        // Không có thẻ BHYT
        var noCardHandler = new ChangeChiTietHoaDonDoiTuongHandler(repo, catalogWithValidCard(0), new NoopAudit());
        var noCardResult = noCardHandler.handle(new ChangeChiTietHoaDonDoiTuongCommand(
                2001, 3001, "BHYT", auth("DOCTOR", 1), actor("DOCTOR", 1)));
        assertTrue(noCardResult.isFailure());
        assertEquals("BHYT_CARD_MISSING", noCardResult.getErrorCode().orElseThrow());

        // Có thẻ BHYT nhưng dòng thuốc (3003) không có giá BHYT trong danh mục
        var withCardHandler = new ChangeChiTietHoaDonDoiTuongHandler(repo, catalogWithValidCard(80), new NoopAudit());
        var noPriceResult = withCardHandler.handle(new ChangeChiTietHoaDonDoiTuongCommand(
                2001, 3003, "BHYT", auth("DOCTOR", 1), actor("DOCTOR", 1)));
        assertTrue(noPriceResult.isFailure());
        assertEquals("BHYT_PRICE_MISSING", noPriceResult.getErrorCode().orElseThrow());
    }

    static final class TestInvoiceRepo implements IHoaDonRepository {
        private final Map<Integer, HoaDon> map = new HashMap<>();
        private final List<PhieuThu> receipts = new ArrayList<>();
        private final List<PhieuHoanTra> refunds = new ArrayList<>();
        private CongNoBenhNhan debt;
        private int refundSeq = 1;

        TestInvoiceRepo(HoaDon inv) {
            map.put(inv.getId(), inv);
            int rId = 1;
            for (PhieuThu pt : inv.getPhieuThus()) {
                pt.setId(rId++);
                pt.setMaHoaDon(inv.getId());
                receipts.add(pt);
            }
        }

        @Override public Optional<HoaDon> getById(int id) { return Optional.ofNullable(map.get(id)); }
        @Override public Optional<HoaDon> getBySoHoaDon(String s) { return map.values().stream().findFirst(); }
        @Override public Optional<HoaDon> getByLuotKham(int v) { return map.values().stream().filter(i -> i.getMaLuotKham() == v).findFirst(); }
        @Override public List<HoaDon> getByBenhNhan(int p) { return List.copyOf(map.values()); }
        @Override public HoaDon save(HoaDon h) { map.put(h.getId(), h); return h; }
        @Override public void savePhieuThu(PhieuThu pt) { receipts.add(pt); }
        @Override public List<PhieuThu> getPhieuThuByHoaDon(int id) { return receipts.stream().filter(r -> r.getMaHoaDon() == id).toList(); }
        @Override public void savePhieuHoanTra(PhieuHoanTra pht) { if (pht.getId() == null) pht.setId(refundSeq++); refunds.add(pht); }
        @Override public List<PhieuHoanTra> getPhieuHoanTraByHoaDon(int id) { return refunds.stream().filter(r -> r.getMaHoaDon() == id).toList(); }
        @Override public void saveCongNo(CongNoBenhNhan c) { this.debt = c; }
        @Override public Optional<CongNoBenhNhan> getCongNoByHoaDon(int id) { return Optional.ofNullable(debt); }
        @Override public List<CongNoBenhNhan> getCongNoByBenhNhan(int p) { return debt == null ? List.of() : List.of(debt); }
        @Override public List<HoaDon> getAll() { return List.copyOf(map.values()); }
        @Override public PhanTrangKetQua<HoaDon> getPaged(int p, int s, ITieuChuan<HoaDon> spec) { return new PhanTrangKetQua<>(getAll(), map.size(), p, s); }
        @Override public List<HoaDon> find(ITieuChuan<HoaDon> spec) { return getAll(); }
        @Override public boolean exists(ITieuChuan<HoaDon> spec) { return !map.isEmpty(); }
        @Override public int count(ITieuChuan<HoaDon> spec) { return map.size(); }
        @Override public void add(HoaDon e) { save(e); }
        @Override public void update(HoaDon e) { save(e); }
        @Override public void delete(HoaDon e) { map.remove(e.getId()); }
        @Override public IUnitOfWork getUnitOfWork() { return new IUnitOfWork() { public int saveChanges() { return 1; } public boolean saveEntities() { return true; } }; }
    }

    static final class NoopAudit implements KiemToanWriteCongKetNoi {
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    }
}
