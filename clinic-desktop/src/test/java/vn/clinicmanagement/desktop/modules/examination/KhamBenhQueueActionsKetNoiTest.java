package vn.clinicmanagement.desktop.modules.examination;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.core.session.PhienThuNganKiemThu;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.QuanLyBuongGiuongNoiTruHopThoai;

import javax.swing.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class KhamBenhQueueActionsKetNoiTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @AfterEach
    void logout() {
        NguoiDungPhien.getInstance().clearSession();
    }

    @Test
    void inpatientBedPanelLoadsAdmissionsAndContextActionsAreWired() throws Exception {
        PhienThuNganKiemThu.login("DOCTOR");
        var calls = new AtomicInteger();
        KhamBenhQueueActionsPort fakePort = new KhamBenhQueueActionsPort() {
            @Override public void callPatientToRoom(int visitId, int facilityId, int actorId) { calls.incrementAndGet(); }
            @Override public void bookFollowUpAppointment(int visitId, int patientId, int doctorId, int facilityId, LocalDateTime date, String reason) {}
            @Override public GuardianInfo getGuardianInfo(int patientId) { return new GuardianInfo("Nguyễn Văn Bố", "0909123456"); }
            @Override public void updateGuardianInfo(int patientId, GuardianInfo info, int actorId) {}
            @Override public String getVisitNote(int visitId) { return "Theo dõi thêm"; }
            @Override public void updateVisitNote(int visitId, String note, int actorId) {}
            @Override public List<ReexamHistoryRow> listReexamHistory(int patientId, int facilityId) { return List.of(); }
            @Override public PatientCardData getPatientCardData(int visitId, int facilityId) { return null; }
            @Override public void changeVisitSubject(int visitId, String subjectType, int actorId) {}
            @Override public EmrSyncSummary syncEmrSummary(int visitId, int facilityId, int actorId) {
                return new EmrSyncSummary(visitId, 1, 1, 2, 1, LocalDateTime.now());
            }
            @Override
            public List<InpatientAdmissionRow> listInpatientAdmissions(int facilityId) {
                assertEquals(1, facilityId);
                return List.of(new InpatientAdmissionRow(
                        101, "LK-101", "BN001", "Lê Văn Hùng", "Nam",
                        "Khoa Nội", "Nhập viện - Khoa Nội", "Buồng: Buồng 101 | Giường: G01",
                        LocalDateTime.of(2026, 11, 23, 8, 30)));
            }
            @Override public void assignInpatientBed(int visitId, String wardName, String bedCode, String note, int actorId) {}
        };

        KhamBenhManHinh[] screen = new KhamBenhManHinh[1];
        QuanLyBuongGiuongNoiTruHopThoai[] bedPanel = new QuanLyBuongGiuongNoiTruHopThoai[1];
        SwingUtilities.invokeAndWait(() -> {
            screen[0] = new KhamBenhManHinh(KhamBenhGhepNoiGoc.createDefault());
            screen[0].setQueueActionsPort(fakePort);
            bedPanel[0] = screen[0].createInpatientBedPanel();
        });

        for (int i = 0; i < 100; i++) {
            Thread.sleep(20);
            int[] rows = {0};
            SwingUtilities.invokeAndWait(() -> rows[0] = bedPanel[0].getTable().getRowCount());
            if (rows[0] == 1) break;
        }

        SwingUtilities.invokeAndWait(() -> {
            assertEquals(1, bedPanel[0].getTable().getRowCount());
            assertEquals("LK-101", bedPanel[0].getTable().getValueAt(0, 1));
            assertEquals("Buồng: Buồng 101 | Giường: G01", bedPanel[0].getTable().getValueAt(0, 7));
        });
    }
}
