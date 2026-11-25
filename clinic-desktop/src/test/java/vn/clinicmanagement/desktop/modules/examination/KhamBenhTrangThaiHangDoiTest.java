package vn.clinicmanagement.desktop.modules.examination;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.examination.KhamBenhLichSuMatHang;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc.InMemoryExamQueuePort;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;

import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KhamBenhTrangThaiHangDoiTest {

    @BeforeAll
    static void enableHeadlessMode() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void mapsAllDatabaseVisitStatusesIncludingHoanTatAccurately() {
        assertEquals("Chờ khám", KhamBenhManHinh.formatVisitStatus("CHO_KHAM"));
        assertEquals("Đang khám", KhamBenhManHinh.formatVisitStatus("DANG_KHAM"));
        assertEquals("Chờ cận lâm sàng", KhamBenhManHinh.formatVisitStatus("CHO_CLS"));
        assertEquals("Đã khám", KhamBenhManHinh.formatVisitStatus("DA_KHAM"));
        assertEquals("Kết luận", KhamBenhManHinh.formatVisitStatus("KET_LUAN"));
        assertEquals("Hoàn thành", KhamBenhManHinh.formatVisitStatus("HOAN_THANH"));
        assertEquals("Hoàn thành", KhamBenhManHinh.formatVisitStatus("HOAN_TAT"));
        assertEquals("Đã hủy", KhamBenhManHinh.formatVisitStatus("DA_HUY"));

        assertFalse(KhamBenhManHinh.isCompletedOrCancelledVisitStatus("CHO_KHAM"));
        assertFalse(KhamBenhManHinh.isCompletedOrCancelledVisitStatus("DANG_KHAM"));
        assertFalse(KhamBenhManHinh.isCompletedOrCancelledVisitStatus("CHO_CLS"));
        assertTrue(KhamBenhManHinh.isCompletedOrCancelledVisitStatus("DA_KHAM"));
        assertTrue(KhamBenhManHinh.isCompletedOrCancelledVisitStatus("KET_LUAN"));
        assertTrue(KhamBenhManHinh.isCompletedOrCancelledVisitStatus("HOAN_THANH"));
        assertTrue(KhamBenhManHinh.isCompletedOrCancelledVisitStatus("HOAN_TAT"));
        assertTrue(KhamBenhManHinh.isCompletedOrCancelledVisitStatus("DA_HUY"));
    }

    @Test
    void historyTableAndSummaryLabelsReflectRealVisitStatusInsteadOfHardcodedValues() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            KhamBenhManHinh panel = new KhamBenhManHinh(KhamBenhGhepNoiGoc.createDefault());

            assertFalse(panel.getTabPatientsTextForTest().contains("(29)"),
                    "Queue header count must not use hardcoded (29)");
            assertFalse(panel.getFilterSummaryTextForTest().contains("20/07/22"),
                    "Filter summary must not use hardcoded 20/07/22 date");

            AdminPatientItem waitingPatient = new AdminPatientItem(
                    "01", "TRẦN MINH KHÔI", "BN260002", "Viện phí", "4",
                    true, "Nam", "2022", "4 tuổi", "4", "2022-10-20",
                    "Bắc Ninh", "Bắc Ninh", "0912345002", "BA-2020",
                    "09:10 18/11/2026", "Khoa Khám Bệnh > PK Tai Mũi Họng", "Chưa phân công",
                    "Đau họng", "VP-2020", 0, 2020, "CHO_KHAM", "Khám bệnh",
                    "Khoa Khám Bệnh", "PK Tai Mũi Họng", "Kinh", "Trẻ em", "Việt Nam",
                    "", "", 0, "");
            panel.selectPatientForTest(waitingPatient);

            DefaultTableModel history = panel.getHistoryModelForTest();
            assertEquals(1, history.getRowCount());
            assertEquals("CHO_KHAM", history.getValueAt(0, 0));
            assertEquals("Chờ khám", history.getValueAt(0, 8));
            assertEquals("", history.getValueAt(0, 5));

            AdminPatientItem completedPatient = new AdminPatientItem(
                    "01", "NGUYỄN THỊ LAN", "BN260001", "BHYT", "34",
                    false, "Nữ", "1992", "34 tuổi", "34", "1992-05-10",
                    "Bắc Ninh", "Bắc Ninh", "0912345001", "BA-2014",
                    "08:00 10/11/2026", "Khoa Khám Bệnh > PK Nội", "BS. An",
                    "Khám định kỳ", "VP-2014", 0, 2014, "HOAN_TAT", "Tái khám",
                    "Khoa Khám Bệnh", "PK Nội", "Kinh", "Giáo viên", "Việt Nam",
                    "027192001001", "DN4272710001001", 80, "08:35 10/11/2026");
            panel.selectPatientForTest(completedPatient);

            assertEquals(1, history.getRowCount());
            assertEquals("HOAN_TAT", history.getValueAt(0, 0));
            assertEquals("Tái khám", history.getValueAt(0, 2));
            assertEquals("DN4272710001001", history.getValueAt(0, 5));
            assertEquals("08:35 10/11/2026", history.getValueAt(0, 7));
            assertEquals("Hoàn thành", history.getValueAt(0, 8));
        });
    }

    @Test
    void loadsMultiVisitHistoryFromExamQueuePortWithoutDirectSqlInPanel() throws Exception {
        KhamBenhGhepNoiGoc.Services base = KhamBenhGhepNoiGoc.createDefault();
        InMemoryExamQueuePort queuePort = (InMemoryExamQueuePort) base.examQueue();
        queuePort.setHistory(2, List.of(
                new KhamBenhLichSuMatHang(
                        2020, "CHO_KHAM",
                        LocalDateTime.of(2026, 11, 18, 9, 10), null,
                        "Khám bệnh", "THU_PHI", "Khoa Khám Bệnh", "PK Tai Mũi Họng", ""),
                new KhamBenhLichSuMatHang(
                        1, "HOAN_TAT",
                        LocalDateTime.of(2026, 11, 10, 8, 30), LocalDateTime.of(2026, 11, 10, 9, 5),
                        "Tái khám", "BHYT", "Khoa Khám Bệnh", "PK Nhi 1", "TE1272710002002")
        ));

        KhamBenhManHinh[] holder = new KhamBenhManHinh[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new KhamBenhManHinh(base);
            AdminPatientItem patient = new AdminPatientItem(
                    "01", "TRẦN MINH KHÔI", "BN260002", "Viện phí", "4",
                    true, "Nam", "2022", "4 tuổi", "4", "2022-10-20",
                    "Bắc Ninh", "Bắc Ninh", "0912345002", "BA-2020",
                    "09:10 18/11/2026", "Khoa Khám Bệnh > PK Tai Mũi Họng", "Chưa phân công",
                    "Đau họng", "VP-2020", 2, 2020, "CHO_KHAM", "Khám bệnh",
                    "Khoa Khám Bệnh", "PK Tai Mũi Họng", "Kinh", "Trẻ em", "Việt Nam",
                    "", "", 0, "");
            holder[0].selectPatientForTest(patient);
        });

        // Chờ SwingWorker tải lịch sử xong
        long deadline = System.currentTimeMillis() + 3000;
        DefaultTableModel history = holder[0].getHistoryModelForTest();
        while (history.getRowCount() < 2 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }

        SwingUtilities.invokeAndWait(() -> {
            assertEquals(2, history.getRowCount());
            assertEquals("CHO_KHAM", history.getValueAt(0, 0));
            assertEquals("Chờ khám", history.getValueAt(0, 8));
            assertEquals("HOAN_TAT", history.getValueAt(1, 0));
            assertEquals("Hoàn thành", history.getValueAt(1, 8));
            assertEquals("TE1272710002002", history.getValueAt(1, 5));
        });
    }

    @Test
    void rendersChoClsQueueItemDistinctlyWithoutErrors() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AdminPatientItem clsPatient = new AdminPatientItem(
                    "01", "TRẦN MINH KHÔI", "BN260002", "Viện phí", "4",
                    true, "Nam", "2022", "4 tuổi", "4", "2022-10-20",
                    "Bắc Ninh", "Bắc Ninh", "0912345002", "BA-2020",
                    "09:10 18/11/2026", "Khoa Khám Bệnh", "BS. Tiến",
                    "Sốt nhẹ", "VP-2020", 2, 2020, "CHO_CLS", "Khám bệnh",
                    "Khoa Khám Bệnh", "PK Tai Mũi Họng", "Kinh", "Trẻ em", "Việt Nam",
                    "", "", 0, "");

            javax.swing.JTable tbl = new javax.swing.JTable(new Object[][]{{clsPatient, clsPatient}}, new String[]{"STT", "Ten"});
            var sttComp = new vn.clinicmanagement.desktop.modules.examination.components.KhamBenhManHinhIconVaRenderer.SttCellRenderer()
                    .getTableCellRendererComponent(tbl, clsPatient, false, false, 0, 0);
            var nameComp = new vn.clinicmanagement.desktop.modules.examination.components.KhamBenhManHinhIconVaRenderer.PatientNameCellRenderer()
                    .getTableCellRendererComponent(tbl, clsPatient, false, false, 0, 1);

            sttComp.setSize(48, 47);
            nameComp.setSize(220, 47);
            var img = new java.awt.image.BufferedImage(280, 60, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            assertDoesNotThrow(() -> {
                sttComp.paint(img.createGraphics());
                nameComp.paint(img.createGraphics());
            });
        });
    }
}
