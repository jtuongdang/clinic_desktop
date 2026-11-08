package vn.clinicmanagement.desktop.modules.paraclinical;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiTietKetQuaXetNghiem;

import javax.swing.table.DefaultTableModel;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ParaclinicalResultEntryDialog Tests (HD-INTEG-03)")
class ParaclinicalResultEntryDialogTest {

    @BeforeEach
    void setUp() {
        UserSession.getInstance().clearSession();
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().clearSession();
    }

    @Test
    @DisplayName("Dialog initializes cleanly and populates default indicators and fields")
    void dialogInitializesCleanly() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ParaclinicalCompositionRoot.Services services = ParaclinicalCompositionRoot.createDefault();
        ParaclinicalResultEntryDialog dialog = new ParaclinicalResultEntryDialog(
                105, "NGUYỄN VĂN AN", "Tổng phân tích tế bào máu", services
        );

        assertNotNull(dialog, "Dialog instance must not be null");
        assertEquals(105, dialog.getOrderId());
        assertEquals("NGUYỄN VĂN AN", dialog.getPatientName());
        assertNotNull(dialog.getBtnSave(), "Save button must exist");
        assertNotNull(dialog.getBtnCancel(), "Cancel button must exist");
        assertNotNull(dialog.getTxtSummary(), "Summary field must exist");
        assertNotNull(dialog.getTxtDescription(), "Description field must exist");
        assertNotNull(dialog.getTxtConclusion(), "Conclusion field must exist");
        assertNotNull(dialog.getTableModel(), "Table model must exist");

        DefaultTableModel model = dialog.getTableModel();
        assertEquals(5, model.getColumnCount(), "Table must have 5 columns");
        assertEquals("Tên Chỉ Số", model.getColumnName(0));
        assertEquals("Kết Quả", model.getColumnName(1));
        assertEquals("Khoảng Tham Chiếu", model.getColumnName(2));
        assertEquals("Đơn Vị", model.getColumnName(3));
        assertEquals("Đánh Giá", model.getColumnName(4));

        assertTrue(model.getRowCount() >= 3, "Table must have default indicators");

        dialog.dispose();
    }

    @Test
    @DisplayName("Add and delete indicator rows operate cleanly")
    void rowModificationWorks() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ParaclinicalCompositionRoot.Services services = ParaclinicalCompositionRoot.createDefault();
        ParaclinicalResultEntryDialog dialog = new ParaclinicalResultEntryDialog(
                106, "TRẦN VĂN BÌNH", "Sinh hóa máu", services
        );

        DefaultTableModel model = dialog.getTableModel();
        int initialCount = model.getRowCount();

        assertNotNull(dialog.getBtnAddRow());
        dialog.getBtnAddRow().doClick();
        assertEquals(initialCount + 1, model.getRowCount());

        assertNotNull(dialog.getBtnDeleteRow());
        dialog.getBtnDeleteRow().doClick();
        assertEquals(initialCount, model.getRowCount());

        dialog.dispose();
    }

    @Test
    @DisplayName("Saving result updates service order in repository and invokes callback")
    void saveResultExecutesSuccessfully() throws InterruptedException {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ParaclinicalCompositionRoot.Services services = ParaclinicalCompositionRoot.createDefault();

        // Seed an order in repository
        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setId(205);
        order.setMaLuotKham(1);
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CHI_DINH);
        services.serviceRepository().save(order);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        ParaclinicalResultEntryDialog dialog = new ParaclinicalResultEntryDialog(
                null, 205, "TRẦN THỊ MAI", "Sinh hóa máu", services, () -> {
                    callbackInvoked.set(true);
                    latch.countDown();
                }
        );

        dialog.getTxtSummary().setText("Chỉ số đường huyết bình thường.");
        dialog.getTxtConclusion().setText("Kết quả trong giới hạn an toàn.");

        assertDoesNotThrow(dialog::saveResult);

        boolean finished = latch.await(3, TimeUnit.SECONDS);
        assertTrue(finished, "Save callback should trigger within timeout");
        assertTrue(callbackInvoked.get(), "Callback must be invoked upon successful save");

        Optional<ChiDinhDichVu> updatedOpt = services.serviceRepository().getById(205);
        assertTrue(updatedOpt.isPresent());
        ChiDinhDichVu updated = updatedOpt.get();
        assertEquals(TrangThaiChiDinhDichVu.DA_CO_KET_QUA, updated.getTrangThai());
        assertEquals("Chỉ số đường huyết bình thường.", updated.getKetQua());
        assertEquals("Kết quả trong giới hạn an toàn.", updated.getKetLuan());

        List<ChiTietKetQuaXetNghiem> results = services.serviceRepository().getKetQuaByChiDinh(205);
        assertTrue(results.size() >= 3);

        dialog.dispose();
    }

    @Test
    @DisplayName("btnSave click triggers saveResult workflow successfully")
    void btnSaveClickTriggersWorkflow() throws InterruptedException {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ParaclinicalCompositionRoot.Services services = ParaclinicalCompositionRoot.createDefault();

        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setId(301);
        order.setMaLuotKham(2);
        order.setTrangThai(TrangThaiChiDinhDichVu.CHO_KET_QUA);
        services.serviceRepository().save(order);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        ParaclinicalResultEntryDialog dialog = new ParaclinicalResultEntryDialog(
                null, 301, "LÊ VĂN LONG", "Công thức máu toàn phần", services, () -> {
                    callbackInvoked.set(true);
                    latch.countDown();
                }
        );

        dialog.getBtnSave().doClick();

        boolean finished = latch.await(3, TimeUnit.SECONDS);
        assertTrue(finished, "Save must complete within timeout");
        assertTrue(callbackInvoked.get());

        dialog.dispose();
    }
}
