package vn.clinicmanagement.desktop.modules.billing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.billing.create.CreateHoaDonHandler;
import vn.clinicmanagement.application.billing.get.GetHoaDonTheoLuotKhamHandler;
import vn.clinicmanagement.application.billing.get.GetBenhNhanCongNoHandler;
import vn.clinicmanagement.application.billing.record.RecordThanhToanHandler;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.examination.LuotKham;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử đấu nối (Wiring Test) cho BillingInvoicePanel & BillingCompositionRoot.
 * Khẳng định phủ định dứt điểm: không chứa bất kỳ chuỗi dữ liệu mẫu tĩnh HD26-0001,
 * HD26-0002, HD26-0003, HD26-0004.
 * 100% in-memory, tuân thủ Hợp đồng Cách ly 18847587.
 */
class VienPhiHoaDonManHinhKetNoiTest {

    private VienPhiGhepNoiGoc.InMemoryInvoiceRepository invoiceRepo;
    private VienPhiGhepNoiGoc.InMemoryVisitRepository visitRepo;
    private RecordingAuditPort auditPort;
    private VienPhiGhepNoiGoc.Services services;

    @BeforeEach
    void setUp() {
        vn.clinicmanagement.desktop.core.session.PhienThuNganKiemThu.login("CASHIER");
        invoiceRepo = new VienPhiGhepNoiGoc.InMemoryInvoiceRepository();
        visitRepo = new VienPhiGhepNoiGoc.InMemoryVisitRepository();
        auditPort = new RecordingAuditPort();

        CreateHoaDonHandler createInvoice = new CreateHoaDonHandler(invoiceRepo, visitRepo, auditPort);
        GetHoaDonTheoLuotKhamHandler getInvoiceByVisit = new GetHoaDonTheoLuotKhamHandler(invoiceRepo, auditPort);
        RecordThanhToanHandler recordPayment = new RecordThanhToanHandler(invoiceRepo, auditPort);
        GetBenhNhanCongNoHandler patientDebt = new GetBenhNhanCongNoHandler(invoiceRepo, auditPort);

        services = new VienPhiGhepNoiGoc.Services(createInvoice, getInvoiceByVisit, recordPayment, patientDebt);
    }

    @AfterEach
    void tearDown() {
        NguoiDungPhien.getInstance().clearSession();
    }

    @Test
    @DisplayName("BillingCompositionRoot assembles billing handlers cleanly")
    void billingCompositionRootAssemblesBillingHandlersCleanly() {
        VienPhiGhepNoiGoc root = new VienPhiGhepNoiGoc(invoiceRepo, visitRepo, auditPort);
        assertNotNull(root.getCreateInvoiceHandler());
        assertNotNull(root.getGetInvoiceByVisitHandler());
        assertNotNull(root.getRecordPaymentHandler());
        assertNotNull(root.getGetPatientDebtHandler());
        assertNotNull(root.getServices());

        VienPhiGhepNoiGoc.Services defaultServices = VienPhiGhepNoiGoc.createDefault();
        assertNotNull(defaultServices);
        assertNotNull(defaultServices.createInvoice());
        assertNotNull(defaultServices.getInvoiceByVisit());
        assertNotNull(defaultServices.recordPayment());
        assertNotNull(defaultServices.patientDebt());
    }

    @Test
    @DisplayName("BillingInvoicePanel initializes cleanly without hardcoded sample strings HD26-0001 to HD26-0004")
    void initializesWithoutHardcodedSampleData() throws Exception {
        VienPhiHoaDonManHinh[] panelHolder = new VienPhiHoaDonManHinh[1];
        runOnEdtAndWait(() -> panelHolder[0] = new VienPhiHoaDonManHinh());
        VienPhiHoaDonManHinh panel = panelHolder[0];
        assertNotNull(panel);

        // Kiểm tra tất cả text trong các JLabel của panel
        List<JLabel> labels = descendantsOfType(panel, JLabel.class);
        for (JLabel lbl : labels) {
            String text = lbl.getText();
            if (text != null) {
                assertFalse(text.contains("HD26-0001"), "Must not contain HD26-0001: " + text);
                assertFalse(text.contains("HD26-0002"), "Must not contain HD26-0002: " + text);
                assertFalse(text.contains("HD26-0003"), "Must not contain HD26-0003: " + text);
                assertFalse(text.contains("HD26-0004"), "Must not contain HD26-0004: " + text);
            }
        }

        // Kiểm tra tất cả các ô trong bảng hóa đơn và chi tiết
        TableModel invoiceModel = panel.getInvoiceTableModel();
        for (int row = 0; row < invoiceModel.getRowCount(); row++) {
            for (int col = 0; col < invoiceModel.getColumnCount(); col++) {
                Object val = invoiceModel.getValueAt(row, col);
                if (val != null) {
                    String str = val.toString();
                    assertFalse(str.contains("HD26-0001"), "Invoice table cell must not contain HD26-0001: " + str);
                    assertFalse(str.contains("HD26-0002"), "Invoice table cell must not contain HD26-0002: " + str);
                    assertFalse(str.contains("HD26-0003"), "Invoice table cell must not contain HD26-0003: " + str);
                    assertFalse(str.contains("HD26-0004"), "Invoice table cell must not contain HD26-0004: " + str);
                }
            }
        }

        TableModel detailModel = panel.getDetailTableModel();
        for (int row = 0; row < detailModel.getRowCount(); row++) {
            for (int col = 0; col < detailModel.getColumnCount(); col++) {
                Object val = detailModel.getValueAt(row, col);
                if (val != null) {
                    String str = val.toString();
                    assertFalse(str.contains("HD26-0001"));
                    assertFalse(str.contains("HD26-0002"));
                }
            }
        }
    }

    @Test
    @DisplayName("Loads real invoice data asynchronously via SwingWorker and populates tables & footer")
    void loadsRealInvoiceDataAsynchronouslyViaSwingWorker() throws Exception {
        int visitId = 101;
        int patientId = 2001;
        String realInvoiceNumber = "HD-REAL-2026-9999";

        // Khởi tạo hóa đơn thực tế và lưu vào in-memory repo
        HoaDon invoice = new HoaDon();
        invoice.setId(1);
        invoice.setSoHoaDon(realInvoiceNumber);
        invoice.setMaLuotKham(visitId);
        invoice.setMaBenhNhan(patientId);
        invoice.setMaCoSo(1);
        invoice.setNgayLap(LocalDateTime.of(2026, 11, 7, 9, 30));
        invoice.setTongTien(new BigDecimal("500000"));
        invoice.setSoTienBaoHiemChiTra(new BigDecimal("100000"));
        invoice.setSoTienTamUng(BigDecimal.ZERO);
        invoice.setSoTienGiamGia(BigDecimal.ZERO);
        invoice.setSoTienBenhNhanChiTra(new BigDecimal("400000"));
        invoice.setDaThanhToan(new BigDecimal("100000"));
        invoice.setTrangThai(TrangThaiHoaDon.THANH_TOAN_MOT_PHAN);

        ChiTietHoaDon line1 = new ChiTietHoaDon();
        line1.setLoaiDoiTuong("KHAM_BENH");
        line1.setMaDoiTuong(1);
        line1.setMoTa("Khám chuyên khoa Tim Mạch");
        line1.setSoLuong(BigDecimal.ONE);
        line1.setDonGia(new BigDecimal("150000"));
        invoice.addChiTiet(line1);

        ChiTietHoaDon line2 = new ChiTietHoaDon();
        line2.setLoaiDoiTuong("DICH_VU_CLS");
        line2.setMaDoiTuong(2);
        line2.setMoTa("Điện tâm đồ ECG");
        line2.setSoLuong(BigDecimal.ONE);
        line2.setDonGia(new BigDecimal("100000"));
        invoice.addChiTiet(line2);

        ChiTietHoaDon line3 = new ChiTietHoaDon();
        line3.setLoaiDoiTuong("THUOC");
        line3.setMaDoiTuong(3);
        line3.setMoTa("Thuốc Amlodipine 5mg");
        line3.setSoLuong(new BigDecimal("10"));
        line3.setDonGia(new BigDecimal("25000"));
        invoice.addChiTiet(line3);

        invoiceRepo.save(invoice);

        VienPhiHoaDonManHinh[] panelHolder = new VienPhiHoaDonManHinh[1];
        runOnEdtAndWait(() -> panelHolder[0] = new VienPhiHoaDonManHinh(services));
        VienPhiHoaDonManHinh panel = panelHolder[0];

        // Tải hóa đơn theo lượt khám 101
        runOnEdtAndWait(() -> panel.loadInvoiceByVisit(visitId));
        waitForSwingWorkers(panel, 1000);

        // Khẳng định dữ liệu đã nạp vào bảng hóa đơn
        assertEquals(1, panel.getInvoiceTableModel().getRowCount());
        assertEquals(realInvoiceNumber, panel.getInvoiceTableModel().getValueAt(0, 1));
        assertEquals(String.valueOf(patientId), panel.getInvoiceTableModel().getValueAt(0, 2));
        assertEquals("500,000 VNĐ", panel.getInvoiceTableModel().getValueAt(0, 6));
        assertEquals("100,000 VNĐ", panel.getInvoiceTableModel().getValueAt(0, 7));
        assertEquals("400,000 VNĐ", panel.getInvoiceTableModel().getValueAt(0, 8));
        assertEquals("100,000 VNĐ", panel.getInvoiceTableModel().getValueAt(0, 9));
        assertEquals("300,000 VNĐ", panel.getInvoiceTableModel().getValueAt(0, 10));
        assertEquals("Thanh toán một phần", panel.getInvoiceTableModel().getValueAt(0, 11));

        // Khẳng định dữ liệu chi tiết hóa đơn (3 dòng)
        assertEquals(3, panel.getDetailTableModel().getRowCount());
        assertEquals("Khám chuyên khoa Tim Mạch", panel.getDetailTableModel().getValueAt(0, 0));
        assertEquals("Điện tâm đồ ECG", panel.getDetailTableModel().getValueAt(1, 0));
        assertEquals("Thuốc Amlodipine 5mg", panel.getDetailTableModel().getValueAt(2, 0));

        // Khẳng định thanh tổng kết footer được cập nhật chính xác
        assertTrue(panel.getLblTotalAmount().getText().contains("500,000đ"));
        assertTrue(panel.getLblPaidAmount().getText().contains("100,000đ"));
        assertTrue(panel.getLblUnpaidAmount().getText().contains("300,000đ"));
    }

    @Test
    @DisplayName("Records payment via background SwingWorker and updates invoice row & footer")
    void recordsPaymentViaBackgroundSwingWorkerAndUpdatesUi() throws Exception {
        int visitId = 102;
        int patientId = 2002;
        String invoiceNumber = "HD-REAL-PAY-002";

        HoaDon invoice = new HoaDon();
        invoice.setId(2);
        invoice.setSoHoaDon(invoiceNumber);
        invoice.setMaLuotKham(visitId);
        invoice.setMaBenhNhan(patientId);
        invoice.setMaCoSo(1);
        invoice.setNgayLap(LocalDateTime.of(2026, 11, 7, 10, 0));
        invoice.setTongTien(new BigDecimal("300000"));
        invoice.setSoTienBaoHiemChiTra(BigDecimal.ZERO);
        invoice.setSoTienBenhNhanChiTra(new BigDecimal("300000"));
        invoice.setDaThanhToan(BigDecimal.ZERO);
        invoice.setTrangThai(TrangThaiHoaDon.CHO_THANH_TOAN);

        ChiTietHoaDon line = new ChiTietHoaDon();
        line.setLoaiDoiTuong("KHAM_BENH");
        line.setMoTa("Khám tổng quát");
        line.setSoLuong(BigDecimal.ONE);
        line.setDonGia(new BigDecimal("300000"));
        invoice.addChiTiet(line);

        invoiceRepo.save(invoice);

        VienPhiHoaDonManHinh[] panelHolder = new VienPhiHoaDonManHinh[1];
        runOnEdtAndWait(() -> panelHolder[0] = new VienPhiHoaDonManHinh(services));
        VienPhiHoaDonManHinh panel = panelHolder[0];

        runOnEdtAndWait(() -> panel.loadInvoiceByVisit(visitId));
        waitForSwingWorkers(panel, 1000);

        assertEquals(1, panel.getInvoiceTableModel().getRowCount());
        assertEquals("300,000 VNĐ", panel.getInvoiceTableModel().getValueAt(0, 10));
        assertEquals("Chưa thanh toán", panel.getInvoiceTableModel().getValueAt(0, 11));

        // Chọn dòng và thực hiện thu tiền (F2)
        runOnEdtAndWait(() -> {
            panel.getInvoiceTable().setRowSelectionInterval(0, 0);
            panel.handleCollectPayment();
        });
        waitForSwingWorkers(panel, 1000);

        // Khẳng định cập nhật cả bảng, displayedInvoices/currentInvoices và footer sau khi worker hoàn tất
        assertEquals("300,000 VNĐ", panel.getInvoiceTableModel().getValueAt(0, 9)); // Đã thu
        assertEquals("0 VNĐ", panel.getInvoiceTableModel().getValueAt(0, 10)); // Còn nợ
        assertEquals("Đã thanh toán", panel.getInvoiceTableModel().getValueAt(0, 11)); // Trạng thái

        assertEquals(0, new BigDecimal("300000").compareTo(panel.getDisplayedInvoices().get(0).amountPaid()));
        assertEquals(0, BigDecimal.ZERO.compareTo(panel.getDisplayedInvoices().get(0).remainingAmount()));
        assertEquals(TrangThaiHoaDon.DA_THANH_TOAN, panel.getDisplayedInvoices().get(0).status());

        assertTrue(panel.getLblPaidAmount().getText().contains("300,000đ"));
        assertTrue(panel.getLblUnpaidAmount().getText().contains("0đ"));
    }

    @Test
    @DisplayName("Partial payment updates UI and a second collection creates a separate receipt")
    void partialPaymentsCreateSeparateReceiptsAndIgnoreRepeatedClicks() throws Exception {
        int visitId = 102;
        int patientId = 2002;
        String invoiceNumber = "HD-REAL-PAY-002";

        HoaDon invoice = new HoaDon();
        invoice.setId(2);
        invoice.setSoHoaDon(invoiceNumber);
        invoice.setMaLuotKham(visitId);
        invoice.setMaBenhNhan(patientId);
        invoice.setMaCoSo(1);
        invoice.setNgayLap(LocalDateTime.of(2026, 11, 7, 10, 0));
        invoice.setTongTien(new BigDecimal("300000"));
        invoice.setSoTienBaoHiemChiTra(BigDecimal.ZERO);
        invoice.setSoTienBenhNhanChiTra(new BigDecimal("300000"));
        invoice.setDaThanhToan(BigDecimal.ZERO);
        invoice.setTrangThai(TrangThaiHoaDon.CHO_THANH_TOAN);

        ChiTietHoaDon line = new ChiTietHoaDon();
        line.setLoaiDoiTuong("KHAM_BENH");
        line.setMoTa("Khám tổng quát");
        line.setSoLuong(BigDecimal.ONE);
        line.setDonGia(new BigDecimal("300000"));
        invoice.addChiTiet(line);

        invoiceRepo.save(invoice);

        VienPhiHoaDonManHinh[] panelHolder = new VienPhiHoaDonManHinh[1];
        runOnEdtAndWait(() -> panelHolder[0] = new VienPhiHoaDonManHinh(services) {
            @Override protected BigDecimal requestPaymentAmount(BigDecimal remaining) {
                return remaining.min(new BigDecimal("150000"));
            }
        });
        VienPhiHoaDonManHinh panel = panelHolder[0];

        runOnEdtAndWait(() -> panel.loadInvoiceByVisit(visitId));
        waitForSwingWorkers(panel, 1000);

        assertEquals(1, panel.getInvoiceTableModel().getRowCount());
        assertEquals("300,000 VNĐ", panel.getInvoiceTableModel().getValueAt(0, 10));
        assertEquals("Chưa thanh toán", panel.getInvoiceTableModel().getValueAt(0, 11));

        // Chọn dòng và thực hiện thu tiền (F2)
        runOnEdtAndWait(() -> {
            panel.getInvoiceTable().setRowSelectionInterval(0, 0);
            panel.handleCollectPayment();
            panel.handleCollectPayment();
        });
        waitForSwingWorkers(panel, 1000);

        assertEquals("150,000 VNĐ", panel.getInvoiceTableModel().getValueAt(0, 10));
        assertEquals(TrangThaiHoaDon.THANH_TOAN_MOT_PHAN, panel.getDisplayedInvoices().get(0).status());
        assertEquals(1, invoiceRepo.getPhieuThuByHoaDon(2).size());
        runOnEdtAndWait(panel::handleCollectPayment);
        waitForSwingWorkers(panel, 1000);
        assertEquals("0 VNĐ", panel.getInvoiceTableModel().getValueAt(0, 10));
        assertEquals(TrangThaiHoaDon.DA_THANH_TOAN, panel.getDisplayedInvoices().get(0).status());
        assertEquals(2, invoiceRepo.getPhieuThuByHoaDon(2).size());
    }

    @Test
    @DisplayName("Wires GetPatientDebtHandler via SwingWorker and populates latest patient debts")
    void wiresPatientDebtHandlerViaSwingWorker() throws Exception {
        int patientId = 3001;

        CongNoBenhNhan debt = new CongNoBenhNhan();
        debt.setId(1);
        debt.setMaBenhNhan(patientId);
        debt.setMaHoaDon(10);
        debt.setSoTienNo(new BigDecimal("250000"));
        debt.setSoTienDaTra(BigDecimal.ZERO);
        debt.setHanThanhToan(LocalDate.of(2026, 12, 1));
        debt.setTrangThai(TrangThaiCongNo.DANG_NO);
        invoiceRepo.saveCongNo(debt);

        VienPhiHoaDonManHinh[] panelHolder = new VienPhiHoaDonManHinh[1];
        runOnEdtAndWait(() -> panelHolder[0] = new VienPhiHoaDonManHinh(services));
        VienPhiHoaDonManHinh panel = panelHolder[0];

        runOnEdtAndWait(() -> panel.loadPatientDebt(patientId));
        waitForSwingWorkers(panel, 1000);

        assertNotNull(panel.getLatestPatientDebts());
        assertEquals(1, panel.getLatestPatientDebts().size());
        assertEquals(patientId, panel.getLatestPatientDebts().get(0).patientId());
        assertEquals(new BigDecimal("250000"), panel.getLatestPatientDebts().get(0).totalDebtAmount());
    }

    @Test
    @DisplayName("Handles empty visit gracefully without crashing or showing dummy data")
    void handlesEmptyStateGracefully() throws Exception {
        VienPhiHoaDonManHinh[] panelHolder = new VienPhiHoaDonManHinh[1];
        runOnEdtAndWait(() -> panelHolder[0] = new VienPhiHoaDonManHinh(services));
        VienPhiHoaDonManHinh panel = panelHolder[0];

        runOnEdtAndWait(() -> panel.loadInvoiceByVisit(999999));
        waitForSwingWorkers(panel, 1000);

        assertEquals(0, panel.getInvoiceTableModel().getRowCount());
        assertEquals(0, panel.getDetailTableModel().getRowCount());
        assertTrue(panel.getLblTotalAmount().getText().contains("0đ"));
        assertTrue(panel.getLblPaidAmount().getText().contains("0đ"));
        assertTrue(panel.getLblUnpaidAmount().getText().contains("0đ"));
    }

    @Test
    @DisplayName("CASHIER from another department still loads facility-wide visits onShow and shows patient names")
    void cashierLoadsFacilityWideVisitsAcrossDepartments() throws Exception {
        int visitId = 505;
        int patientId = 6005;
        HoaDon invoice = new HoaDon();
        invoice.setId(55);
        invoice.setSoHoaDon("DK26111800000505");
        invoice.setMaLuotKham(visitId);
        invoice.setMaBenhNhan(patientId);
        invoice.setMaCoSo(1);
        invoice.setNgayLap(LocalDateTime.of(2026, 11, 18, 9, 0));
        invoice.setTongTien(new BigDecimal("150000"));
        invoice.setSoTienBaoHiemChiTra(BigDecimal.ZERO);
        invoice.setSoTienBenhNhanChiTra(new BigDecimal("150000"));
        invoice.setDaThanhToan(BigDecimal.ZERO);
        invoice.setTrangThai(TrangThaiHoaDon.CHO_THANH_TOAN);
        ChiTietHoaDon line = new ChiTietHoaDon();
        line.setLoaiDoiTuong("DICH_VU_KHAM");
        line.setMoTa("Khám nhi");
        line.setSoLuong(BigDecimal.ONE);
        line.setDonGia(new BigDecimal("150000"));
        invoice.addChiTiet(line);
        invoiceRepo.save(invoice);

        var lifecycle = new vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu(
                NguoiDungPhien.getInstance(),
                new vn.clinicmanagement.application.session.PhienHetGioChinhSach(
                        java.time.Duration.ofMinutes(15), java.time.Duration.ofHours(12)),
                java.time.Clock.systemUTC(),
                auditPort,
                Runnable::run);
        lifecycle.startSession(new vn.clinicmanagement.application.auth.DaHoanTatDangNhapPhien(
                50,
                "CASHIER-01",
                "Thu ngân tài chính",
                new vn.clinicmanagement.application.auth.DaXacThucNguoiDung.RoleChoice(5, "CASHIER", "Thu ngân"),
                new vn.clinicmanagement.application.auth.DaXacThucNguoiDung.WorkScopeChoice(1, "Cơ sở 1", 9, "Phòng Tài chính"),
                UUID.randomUUID(),
                java.time.Instant.now()));

        vn.clinicmanagement.application.reception.TiepDonHangDoiCongKetNoi facilityQueuePort =
                new vn.clinicmanagement.application.reception.TiepDonHangDoiCongKetNoi() {
                    @Override
                    public LuotKham registerVisit(int pId, int fId, int dId, int rId, String rt, String rs,
                                                  vn.clinicmanagement.domain.common.enums.DoiTuongKham pt, boolean pr, int cb) {
                        return null;
                    }

                    @Override
                    public List<vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang> getQueue(
                            LocalDate date, int facilityId, int departmentId) {
                        return List.of(); // Phòng tài chính (dept 9) không có lượt khám trực tiếp
                    }

                    @Override
                    public List<vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang> getFacilityBillingQueue(
                            LocalDate date, int facilityId) {
                        assertEquals(1, facilityId);
                        return List.of(new vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang(
                                visitId, patientId, "BN260505", "TRẦN MINH KHÔI",
                                1, "PK Nhi 1", 1, "01", "CHO_KHAM", LocalDateTime.now()));
                    }

                    @Override
                    public List<vn.clinicmanagement.application.reception.PhongKhamPhongHangDoiTongHop> getRoomSummaries(
                            LocalDate date, int facilityId, int departmentId) {
                        return List.of();
                    }
                };

        VienPhiHoaDonManHinh[] panelHolder = new VienPhiHoaDonManHinh[1];
        runOnEdtAndWait(() -> panelHolder[0] = new VienPhiHoaDonManHinh(services, null, facilityQueuePort));
        VienPhiHoaDonManHinh panel = panelHolder[0];

        runOnEdtAndWait(panel::onShow);
        waitForSwingWorkers(panel, 1000);

        assertEquals(1, panel.getInvoiceTableModel().getRowCount());
        assertEquals("TRẦN MINH KHÔI", panel.getInvoiceTableModel().getValueAt(0, 3));
        assertTrue(panel.getLblUnpaidAmount().getText().contains("150,000đ"));
    }

    @Test
    @DisplayName("Toolbar checkboxes filter invoices, detail search converts view row indices safely, and missing screenRead metadata does not fail refresh")
    void toolbarFiltersAndServiceSearchWorkSafelyWithMissingVisitMetadata() throws Exception {
        HoaDon feeInvoice = new HoaDon();
        feeInvoice.setId(21);
        feeInvoice.setSoHoaDon("HD-FILTER-01");
        feeInvoice.setMaLuotKham(501);
        feeInvoice.setMaBenhNhan(601);
        feeInvoice.setMaCoSo(1);
        feeInvoice.setTongTien(new BigDecimal("205000"));
        feeInvoice.setSoTienBaoHiemChiTra(BigDecimal.ZERO);
        feeInvoice.setSoTienBenhNhanChiTra(new BigDecimal("205000"));
        feeInvoice.setDaThanhToan(BigDecimal.ZERO);
        feeInvoice.setTrangThai(TrangThaiHoaDon.CHO_THANH_TOAN);

        ChiTietHoaDon kLine = new ChiTietHoaDon();
        kLine.setLoaiDoiTuong("DICH_VU_KHAM");
        kLine.setMoTa("Khám Nhi chuyên khoa");
        kLine.setSoLuong(BigDecimal.ONE);
        kLine.setDonGia(new BigDecimal("120000"));
        feeInvoice.addChiTiet(kLine);

        ChiTietHoaDon clsLine = new ChiTietHoaDon();
        clsLine.setLoaiDoiTuong("DICH_VU_CLS");
        clsLine.setMoTa("Tổng phân tích tế bào máu");
        clsLine.setSoLuong(BigDecimal.ONE);
        clsLine.setDonGia(new BigDecimal("85000"));
        feeInvoice.addChiTiet(clsLine);
        invoiceRepo.save(feeInvoice);

        HoaDon bhytInvoice = new HoaDon();
        bhytInvoice.setId(22);
        bhytInvoice.setSoHoaDon("HD-FILTER-02");
        bhytInvoice.setMaLuotKham(502);
        bhytInvoice.setMaBenhNhan(602);
        bhytInvoice.setMaCoSo(1);
        bhytInvoice.setTongTien(new BigDecimal("100000"));
        bhytInvoice.setSoTienBaoHiemChiTra(new BigDecimal("80000"));
        bhytInvoice.setSoTienBenhNhanChiTra(new BigDecimal("20000"));
        bhytInvoice.setDaThanhToan(BigDecimal.ZERO);
        bhytInvoice.setTrangThai(TrangThaiHoaDon.CHO_THANH_TOAN);
        ChiTietHoaDon bhytLine = new ChiTietHoaDon();
        bhytLine.setLoaiDoiTuong("DICH_VU_KHAM");
        bhytLine.setMoTa("Khám Nội tổng quát");
        bhytLine.setSoLuong(BigDecimal.ONE);
        bhytLine.setDonGia(new BigDecimal("100000"));
        bhytInvoice.addChiTiet(bhytLine);
        invoiceRepo.save(bhytInvoice);

        var servicesWithScreenRead = services.withScreenRead((visitId, facilityId) -> {
            if (visitId == 501) {
                return java.util.Optional.of(new vn.clinicmanagement.application.billing.VienPhiManHinhReadCongKetNoi.ThongTin(
                        "BN501", "NGUYỄN VĂN A", LocalDate.of(1995, 1, 1), "M", "Bắc Ninh", "0900000001",
                        "Khoa Khám Bệnh", "PK 1", "THU_PHI", "DANG_KHAM", LocalDateTime.now(), null, java.util.Map.of()));
            }
            return java.util.Optional.empty(); // Mô phỏng thiếu metadata cho visit 502
        });

        VienPhiHoaDonManHinh[] holder = new VienPhiHoaDonManHinh[1];
        runOnEdtAndWait(() -> holder[0] = new VienPhiHoaDonManHinh(servicesWithScreenRead));
        VienPhiHoaDonManHinh panel = holder[0];

        runOnEdtAndWait(() -> panel.setVisitIds(List.of(501, 502)));
        waitForSwingWorkers(panel, 1000);

        assertEquals(2, panel.getDisplayedInvoices().size());

        // Lọc theo từ khóa dịch vụ CLS -> renderer phải chuyển đổi view index sang model index để hiện nhóm Cận lâm sàng
        runOnEdtAndWait(() -> {
            List<JTextField> fields = descendantsOfType(panel, JTextField.class);
            JTextField serviceSearch = fields.stream()
                    .filter(f -> "Tìm kiếm dịch vụ".equals(f.getClientProperty("JTextField.placeholderText")))
                    .findFirst().orElseThrow();
            serviceSearch.setText("tế bào máu");
            assertEquals(1, panel.getDetailTable().getRowCount());
            var comp = panel.getDetailTable().prepareRenderer(
                    panel.getDetailTable().getCellRenderer(0, 0), 0, 0);
            assertTrue(((JLabel) comp).getText().contains("Cận lâm sàng"));
        });

        // Bỏ chọn checkbox "Viện phí" -> chỉ giữ lại hóa đơn BHYT
        runOnEdtAndWait(() -> {
            JCheckBox feeBox = descendantsOfType(panel, JCheckBox.class).stream()
                    .filter(cb -> "Viện phí".equals(cb.getText()))
                    .findFirst().orElseThrow();
            feeBox.doClick();
            assertEquals(1, panel.getDisplayedInvoices().size());
            assertEquals("HD-FILTER-02", panel.getDisplayedInvoices().get(0).invoiceNumber());
        });
    }

    private static void runOnEdtAndWait(Runnable action) throws InterruptedException, InvocationTargetException {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeAndWait(action);
        }
    }

    private static void waitForSwingWorkers(VienPhiHoaDonManHinh panel, int timeoutMs) throws InterruptedException, InvocationTargetException {
        long start = System.currentTimeMillis();
        Thread.sleep(40);
        while (panel.getInFlightWorkersCount() > 0 && System.currentTimeMillis() - start < timeoutMs) {
            Thread.sleep(20);
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeAndWait(() -> {});
        }
        Thread.sleep(40);
    }

    private static <T extends Component> List<T> descendantsOfType(Container root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                matches.add(type.cast(component));
            }
            if (component instanceof Container) {
                matches.addAll(descendantsOfType((Container) component, type));
            }
        }
        return matches;
    }

    private static class RecordingAuditPort implements KiemToanWriteCongKetNoi {
        final List<HeThongKiemToanNhapLieu> systemEntries = new ArrayList<>();

        @Override
        public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {
            systemEntries.add(entry);
        }

        @Override
        public void appendIndependently(HeThongKiemToanNhapLieu entry) {
            systemEntries.add(entry);
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}

        @Override
        public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    }
}
