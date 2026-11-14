package vn.clinicmanagement.application.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
import vn.clinicmanagement.domain.repositories.ITieuChuan;
import vn.clinicmanagement.domain.repositories.PhanTrangKetQua;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateHoaDonHandlerTest {

    private InMemoryInvoiceRepo invoiceRepo;
    private InMemoryVisitRepo visitRepo;
    private RecordingAuditPort auditPort;
    private CreateHoaDonHandler handler;

    @BeforeEach
    void setUp() {
        invoiceRepo = new InMemoryInvoiceRepo();
        visitRepo = new InMemoryVisitRepo();
        auditPort = new RecordingAuditPort();
        handler = new CreateHoaDonHandler(invoiceRepo, visitRepo, auditPort);
    }

    @Test
    @DisplayName("Cashier creates invoice successfully with discount, advance, and BHYT")
    void cashierCreatesInvoiceSuccessfully() {
        LuotKham visit = new LuotKham();
        visit.setId(101);
        visit.setTrangThai(TrangThaiLuotKham.HOAN_THANH);
        visitRepo.save(visit);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                40, 5, "CASHIER", 1, 1, UUID.randomUUID());
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(
                40, 5, "CASHIER", 1, 1, "WS-CASHIER-01", UUID.randomUUID());

        List<HoaDonMatHangDauVao> items = List.of(
                new HoaDonMatHangDauVao("KHAM_BENH", 1, "Khám chuyên khoa", BigDecimal.ONE, new BigDecimal("150000")),
                new HoaDonMatHangDauVao("DICH_VU_CLS", 2, "X-quang ngực thẳng", BigDecimal.ONE, new BigDecimal("120000")),
                new HoaDonMatHangDauVao("THUOC", 10, "Augmentin 1g", new BigDecimal("14"), new BigDecimal("18000"))
        );
        // Total = 150,000 + 120,000 + 252,000 = 522,000

        CreateHoaDonCommand command = new CreateHoaDonCommand(
                101, 2001, 1,
                new BigDecimal("100000"), // BHYT
                new BigDecimal("50000"),  // Tam ung
                new BigDecimal("22000"),  // Giam gia
                BigDecimal.ZERO,
                "Chính sách tri ân khách hàng",
                "Hóa đơn ngoại trú",
                items,
                auth,
                actor
        );

        KetQua<CreateHoaDonBienNhan> result = handler.handle(command);
        assertTrue(result.isSuccess());

        CreateHoaDonBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(new BigDecimal("522000"), receipt.totalAmount());
        assertEquals(new BigDecimal("100000"), receipt.insurancePaidAmount());
        assertEquals(new BigDecimal("50000"), receipt.advanceAmount());
        assertEquals(new BigDecimal("22000"), receipt.discountAmount());
        // Patient copay = 522,000 - 100,000 - 50,000 - 22,000 = 350,000
        assertEquals(new BigDecimal("350000"), receipt.patientPaidAmount());
        assertEquals(TrangThaiHoaDon.CHO_THANH_TOAN, receipt.status());
        assertNotNull(receipt.invoiceNumber());
        assertTrue(receipt.invoiceNumber().startsWith("HD"));

        // Verify repository state
        assertEquals(1, invoiceRepo.invoices.size());
        HoaDon saved = invoiceRepo.invoices.get(receipt.invoiceId());
        assertNotNull(saved);
        assertEquals(3, saved.getChiTietHoaDons().size());

        // Verify audit log
        assertFalse(auditPort.systemEntries.isEmpty());
        HeThongKiemToanNhapLieu audit = auditPort.systemEntries.get(0);
        assertEquals(KiemToanHanhDongMa.BILL_INVOICE_CREATED, audit.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.INVOICE, audit.resourceType());
        assertEquals(KiemToanKetQua.SUCCESS, audit.outcome());
    }

    @Test
    @DisplayName("Admin creates invoice with 100% insurance resulting in DA_THANH_TOAN")
    void adminCreatesFullyCoveredInvoice() {
        LuotKham visit = new LuotKham();
        visit.setId(102);
        visitRepo.save(visit);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                1, 1, "ADMIN", 1, 1, UUID.randomUUID());
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(
                1, 1, "ADMIN", 1, 1, "WS-ADMIN", UUID.randomUUID());

        List<HoaDonMatHangDauVao> items = List.of(
                new HoaDonMatHangDauVao("DICH_VU_CLS", 5, "Xét nghiệm máu", BigDecimal.ONE, new BigDecimal("100000"))
        );

        CreateHoaDonCommand command = new CreateHoaDonCommand(
                102, 2002, 1,
                new BigDecimal("100000"), // 100% BHYT
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "",
                "Miễn phí BHYT",
                items,
                auth,
                actor
        );

        KetQua<CreateHoaDonBienNhan> result = handler.handle(command);
        assertTrue(result.isSuccess());
        CreateHoaDonBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(BigDecimal.ZERO, receipt.patientPaidAmount());
        assertEquals(TrangThaiHoaDon.DA_THANH_TOAN, receipt.status());
    }

    @Test
    @DisplayName("Unauthorized role fails closed with audit log")
    void unauthorizedRoleFailsClosed() {
        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                20, 2, "DOCTOR", 1, 1, UUID.randomUUID());
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(
                20, 2, "DOCTOR", 1, 1, "WS-DOC", UUID.randomUUID());

        CreateHoaDonCommand command = new CreateHoaDonCommand(
                103, 2003, 1, null, null, null, null, null, null,
                List.of(new HoaDonMatHangDauVao("KHAM", 1, "Khám", BigDecimal.ONE, new BigDecimal("100000"))),
                auth, actor
        );

        KetQua<CreateHoaDonBienNhan> result = handler.handle(command);
        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getErrorCode().orElse(""));

        assertFalse(auditPort.systemEntries.isEmpty());
        HeThongKiemToanNhapLieu audit = auditPort.systemEntries.get(0);
        assertEquals(KiemToanKetQua.DENIED, audit.outcome());
    }

    @Test
    @DisplayName("Non existing visit returns VISIT_NOT_FOUND")
    void nonExistingVisitFails() {
        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(40, 5, "CASHIER", 1, 1, UUID.randomUUID());
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(40, 5, "CASHIER", 1, 1, "WS-01", UUID.randomUUID());

        CreateHoaDonCommand command = new CreateHoaDonCommand(
                999, 2001, 1, null, null, null, null, null, null,
                List.of(new HoaDonMatHangDauVao("KHAM", 1, "Khám", BigDecimal.ONE, new BigDecimal("100000"))),
                auth, actor
        );

        KetQua<CreateHoaDonBienNhan> result = handler.handle(command);
        assertTrue(result.isFailure());
        assertEquals("VISIT_NOT_FOUND", result.getErrorCode().orElse(""));
    }

    @Test
    @DisplayName("Duplicate invoice for active visit returns INVOICE_ALREADY_EXISTS")
    void duplicateInvoiceFails() {
        LuotKham visit = new LuotKham();
        visit.setId(104);
        visitRepo.save(visit);

        HoaDon existing = new HoaDon();
        existing.setId(1);
        existing.setSoHoaDon("HD-EXISTING-01");
        existing.setMaLuotKham(104);
        existing.setTrangThai(TrangThaiHoaDon.CHO_THANH_TOAN);
        invoiceRepo.save(existing);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(40, 5, "CASHIER", 1, 1, UUID.randomUUID());
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(40, 5, "CASHIER", 1, 1, "WS-01", UUID.randomUUID());

        CreateHoaDonCommand command = new CreateHoaDonCommand(
                104, 2001, 1, null, null, null, null, null, null,
                List.of(new HoaDonMatHangDauVao("KHAM", 1, "Khám", BigDecimal.ONE, new BigDecimal("100000"))),
                auth, actor
        );

        KetQua<CreateHoaDonBienNhan> result = handler.handle(command);
        assertTrue(result.isFailure());
        assertEquals("INVOICE_ALREADY_EXISTS", result.getErrorCode().orElse(""));
    }

    @Test
    @DisplayName("Validation fails when items list is empty or items have invalid price/qty")
    void invalidItemsFailValidation() {
        LuotKham visit = new LuotKham();
        visit.setId(105);
        visitRepo.save(visit);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(40, 5, "CASHIER", 1, 1, UUID.randomUUID());
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(40, 5, "CASHIER", 1, 1, "WS-01", UUID.randomUUID());

        CreateHoaDonCommand emptyItemsCmd = new CreateHoaDonCommand(
                105, 2001, 1, null, null, null, null, null, null,
                List.of(), auth, actor
        );
        KetQua<CreateHoaDonBienNhan> r1 = handler.handle(emptyItemsCmd);
        assertTrue(r1.isFailure());
        assertEquals("EMPTY_ITEMS", r1.getErrorCode().orElse(""));

        CreateHoaDonCommand zeroQtyCmd = new CreateHoaDonCommand(
                105, 2001, 1, null, null, null, null, null, null,
                List.of(new HoaDonMatHangDauVao("KHAM", 1, "Khám", BigDecimal.ZERO, new BigDecimal("100000"))),
                auth, actor
        );
        KetQua<CreateHoaDonBienNhan> r2 = handler.handle(zeroQtyCmd);
        assertTrue(r2.isFailure());
        assertEquals("INVALID_QUANTITY", r2.getErrorCode().orElse(""));

        CreateHoaDonCommand negativePriceCmd = new CreateHoaDonCommand(
                105, 2001, 1, null, null, null, null, null, null,
                List.of(new HoaDonMatHangDauVao("KHAM", 1, "Khám", BigDecimal.ONE, new BigDecimal("-50000"))),
                auth, actor
        );
        KetQua<CreateHoaDonBienNhan> r3 = handler.handle(negativePriceCmd);
        assertTrue(r3.isFailure());
        assertEquals("INVALID_PRICE", r3.getErrorCode().orElse(""));
    }

    static class InMemoryInvoiceRepo implements IHoaDonRepository {
        final Map<Integer, HoaDon> invoices = new HashMap<>();
        final List<PhieuThu> receipts = new ArrayList<>();
        final List<CongNoBenhNhan> debts = new ArrayList<>();
        int idSeq = 1;
        int receiptSeq = 1;
        int debtSeq = 1;

        @Override
        public Optional<HoaDon> getBySoHoaDon(String soHoaDon) {
            return invoices.values().stream().filter(h -> h.getSoHoaDon().equals(soHoaDon)).findFirst();
        }

        @Override
        public Optional<HoaDon> getByLuotKham(int maLuotKham) {
            return invoices.values().stream().filter(h -> h.getMaLuotKham() == maLuotKham).findFirst();
        }

        @Override
        public List<HoaDon> getByBenhNhan(int maBenhNhan) {
            return invoices.values().stream().filter(h -> h.getMaBenhNhan() == maBenhNhan).toList();
        }

        @Override
        public HoaDon save(HoaDon hoaDon) {
            if (hoaDon.getId() == null || hoaDon.getId() == 0) {
                hoaDon.setId(idSeq++);
            }
            invoices.put(hoaDon.getId(), hoaDon);
            return hoaDon;
        }

        @Override
        public void savePhieuThu(PhieuThu phieuThu) {
            if (phieuThu.getId() == null || phieuThu.getId() == 0) {
                phieuThu.setId(receiptSeq++);
            }
            receipts.add(phieuThu);
        }

        @Override
        public List<PhieuThu> getPhieuThuByHoaDon(int maHoaDon) {
            return receipts.stream().filter(r -> r.getMaHoaDon() == maHoaDon).toList();
        }

        @Override
        public void saveCongNo(CongNoBenhNhan congNo) {
            if (congNo.getId() == null || congNo.getId() == 0) {
                congNo.setId(debtSeq++);
            }
            debts.removeIf(d -> d.getId().equals(congNo.getId()));
            debts.add(congNo);
        }

        @Override
        public Optional<CongNoBenhNhan> getCongNoByHoaDon(int maHoaDon) {
            return debts.stream().filter(d -> d.getMaHoaDon() == maHoaDon).findFirst();
        }

        @Override
        public List<CongNoBenhNhan> getCongNoByBenhNhan(int maBenhNhan) {
            return debts.stream().filter(d -> d.getMaBenhNhan() == maBenhNhan).toList();
        }

        @Override
        public Optional<HoaDon> getById(int id) {
            return Optional.ofNullable(invoices.get(id));
        }

        @Override
        public List<HoaDon> getAll() {
            return new ArrayList<>(invoices.values());
        }

        @Override
        public PhanTrangKetQua<HoaDon> getPaged(int pageIndex, int pageSize, ITieuChuan<HoaDon> specification) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<HoaDon> find(ITieuChuan<HoaDon> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ITieuChuan<HoaDon> specification) {
            return false;
        }

        @Override
        public int count(ITieuChuan<HoaDon> specification) {
            return invoices.size();
        }

        @Override
        public void add(HoaDon entity) {
            save(entity);
        }

        @Override
        public void update(HoaDon entity) {
            save(entity);
        }

        @Override
        public void delete(HoaDon entity) {
            if (entity.getId() != null) invoices.remove(entity.getId());
        }

        @Override
        public IUnitOfWork getUnitOfWork() {
            return null;
        }
    }

    static class InMemoryVisitRepo implements ILuotKhamRepository {
        final Map<Integer, LuotKham> visits = new HashMap<>();

        public void save(LuotKham visit) {
            visits.put(visit.getId(), visit);
        }

        @Override
        public List<LuotKham> getVisitsByDoctor(int maBacSi, java.time.LocalDate date) {
            return List.of();
        }

        @Override
        public int getNextQueueNumber(int maCoSo, int maKhoa, java.time.LocalDate date) {
            return 1;
        }

        @Override
        public Optional<LuotKham> getById(int id) {
            return Optional.ofNullable(visits.get(id));
        }

        @Override
        public List<LuotKham> getAll() {
            return new ArrayList<>(visits.values());
        }

        @Override
        public PhanTrangKetQua<LuotKham> getPaged(int pageIndex, int pageSize, ITieuChuan<LuotKham> specification) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<LuotKham> find(ITieuChuan<LuotKham> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ITieuChuan<LuotKham> specification) {
            return false;
        }

        @Override
        public int count(ITieuChuan<LuotKham> specification) {
            return visits.size();
        }

        @Override
        public void add(LuotKham entity) {
            save(entity);
        }

        @Override
        public void update(LuotKham entity) {
            save(entity);
        }

        @Override
        public void delete(LuotKham entity) {
            visits.remove(entity.getId());
        }

        @Override
        public IUnitOfWork getUnitOfWork() {
            return null;
        }
    }

    static class RecordingAuditPort implements KiemToanWriteCongKetNoi {
        final List<HeThongKiemToanNhapLieu> systemEntries = new ArrayList<>();
        final List<YTeRecordKiemToanNhapLieu> emrEntries = new ArrayList<>();

        @Override
        public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {
            systemEntries.add(entry);
        }

        @Override
        public void appendIndependently(HeThongKiemToanNhapLieu entry) {
            systemEntries.add(entry);
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {
            emrEntries.add(entry);
        }

        @Override
        public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {
            emrEntries.add(entry);
        }
    }
}
