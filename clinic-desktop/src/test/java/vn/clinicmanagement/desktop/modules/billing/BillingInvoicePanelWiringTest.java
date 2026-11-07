package vn.clinicmanagement.desktop.modules.billing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.billing.CreateInvoiceHandler;
import vn.clinicmanagement.application.billing.GetInvoiceByVisitHandler;
import vn.clinicmanagement.application.billing.GetPatientDebtHandler;
import vn.clinicmanagement.application.billing.RecordPaymentHandler;
import vn.clinicmanagement.desktop.core.session.UserSession;
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
class BillingInvoicePanelWiringTest {

    private BillingCompositionRoot.InMemoryInvoiceRepository invoiceRepo;
    private BillingCompositionRoot.InMemoryVisitRepository visitRepo;
    private RecordingAuditPort auditPort;
    private BillingCompositionRoot.Services services;

    @BeforeEach
    void setUp() {
        invoiceRepo = new BillingCompositionRoot.InMemoryInvoiceRepository();
        visitRepo = new BillingCompositionRoot.InMemoryVisitRepository();
        auditPort = new RecordingAuditPort();

        CreateInvoiceHandler createInvoice = new CreateInvoiceHandler(invoiceRepo, visitRepo, auditPort);
        GetInvoiceByVisitHandler getInvoiceByVisit = new GetInvoiceByVisitHandler(invoiceRepo, auditPort);
        RecordPaymentHandler recordPayment = new RecordPaymentHandler(invoiceRepo, auditPort);
        GetPatientDebtHandler patientDebt = new GetPatientDebtHandler(invoiceRepo, auditPort);

        services = new BillingCompositionRoot.Services(createInvoice, getInvoiceByVisit, recordPayment, patientDebt);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().clearSession();
    }

    @Test
    @DisplayName("BillingCompositionRoot assembles billing handlers cleanly")
    void billingCompositionRootAssemblesBillingHandlersCleanly() {
        BillingCompositionRoot root = new BillingCompositionRoot(invoiceRepo, visitRepo, auditPort);
        assertNotNull(root.getCreateInvoiceHandler());
        assertNotNull(root.getGetInvoiceByVisitHandler());
        assertNotNull(root.getRecordPaymentHandler());
        assertNotNull(root.getGetPatientDebtHandler());
        assertNotNull(root.getServices());

        BillingCompositionRoot.Services defaultServices = BillingCompositionRoot.createDefault();
        assertNotNull(defaultServices);
        assertNotNull(defaultServices.createInvoice());
        assertNotNull(defaultServices.getInvoiceByVisit());
        assertNotNull(defaultServices.recordPayment());
        assertNotNull(defaultServices.patientDebt());
    }

    @Test
    @DisplayName("BillingInvoicePanel initializes cleanly without hardcoded sample strings HD26-0001 to HD26-0004")
    void initializesWithoutHardcodedSampleData() throws Exception {
        BillingInvoicePanel[] panelHolder = new BillingInvoicePanel[1];
        runOnEdtAndWait(() -> panelHolder[0] = new BillingInvoicePanel());
        BillingInvoicePanel panel = panelHolder[0];
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

        BillingInvoicePanel[] panelHolder = new BillingInvoicePanel[1];
        runOnEdtAndWait(() -> panelHolder[0] = new BillingInvoicePanel(services));
        BillingInvoicePanel panel = panelHolder[0];

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
        assertEquals("Khám chuyên khoa Tim Mạch", panel.getDetailTableModel().getValueAt(0, 2));
        assertEquals("Điện tâm đồ ECG", panel.getDetailTableModel().getValueAt(1, 2));
        assertEquals("Thuốc Amlodipine 5mg", panel.getDetailTableModel().getValueAt(2, 2));

        // Khẳng định thanh tổng kết footer được cập nhật chính xác
        assertTrue(panel.getLblTotalAmount().getText().contains("500,000 VNĐ"));
        assertTrue(panel.getLblPaidAmount().getText().contains("100,000 VNĐ"));
        assertTrue(panel.getLblUnpaidAmount().getText().contains("300,000 VNĐ"));
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

        BillingInvoicePanel[] panelHolder = new BillingInvoicePanel[1];
        runOnEdtAndWait(() -> panelHolder[0] = new BillingInvoicePanel(services));
        BillingInvoicePanel panel = panelHolder[0];

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

        // Khẳng định cập nhật thành công sau khi worker hoàn tất
        assertEquals("300,000 VNĐ", panel.getInvoiceTableModel().getValueAt(0, 9)); // Đã thu
        assertEquals("0 VNĐ", panel.getInvoiceTableModel().getValueAt(0, 10)); // Còn nợ
        assertEquals("Đã thanh toán", panel.getInvoiceTableModel().getValueAt(0, 11)); // Trạng thái

        assertTrue(panel.getLblPaidAmount().getText().contains("300,000 VNĐ"));
        assertTrue(panel.getLblUnpaidAmount().getText().contains("0 VNĐ"));
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

        BillingInvoicePanel[] panelHolder = new BillingInvoicePanel[1];
        runOnEdtAndWait(() -> panelHolder[0] = new BillingInvoicePanel(services));
        BillingInvoicePanel panel = panelHolder[0];

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
        BillingInvoicePanel[] panelHolder = new BillingInvoicePanel[1];
        runOnEdtAndWait(() -> panelHolder[0] = new BillingInvoicePanel(services));
        BillingInvoicePanel panel = panelHolder[0];

        runOnEdtAndWait(() -> panel.loadInvoiceByVisit(999999));
        waitForSwingWorkers(panel, 1000);

        assertEquals(0, panel.getInvoiceTableModel().getRowCount());
        assertEquals(0, panel.getDetailTableModel().getRowCount());
        assertTrue(panel.getLblTotalAmount().getText().contains("0 VNĐ"));
        assertTrue(panel.getLblPaidAmount().getText().contains("0 VNĐ"));
        assertTrue(panel.getLblUnpaidAmount().getText().contains("0 VNĐ"));
    }

    private static void runOnEdtAndWait(Runnable action) throws InterruptedException, InvocationTargetException {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeAndWait(action);
        }
    }

    private static void waitForSwingWorkers(BillingInvoicePanel panel, int timeoutMs) throws InterruptedException, InvocationTargetException {
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

    private static class RecordingAuditPort implements AuditWritePort {
        final List<SystemAuditEntry> systemEntries = new ArrayList<>();

        @Override
        public void appendInCurrentTransaction(SystemAuditEntry entry) {
            systemEntries.add(entry);
        }

        @Override
        public void appendIndependently(SystemAuditEntry entry) {
            systemEntries.add(entry);
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {}

        @Override
        public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {}
    }
}
