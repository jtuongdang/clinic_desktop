package vn.clinicmanagement.desktop.modules.reception.components;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.reception.RegisterPatientCommand;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientRegistrationFormPanelTest {
    private static final String SYNTHETIC_CCCD = "000000006789";
    private static final String SYNTHETIC_PHONE = "0900000123";

    static {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeAll
    static void verifyHeadlessMode() {
        System.setProperty("java.awt.headless", "true");
        assertTrue(GraphicsEnvironment.isHeadless(), "Swing regression tests must run in headless mode");
    }

    @Test
    void enteredCccdIsMaskedToExposeAtMostLastFourCharacters() throws Exception {
        assertEquals("", PatientRegistrationFormPanel.maskCccd(null));
        assertEquals("", PatientRegistrationFormPanel.maskCccd("   "));
        assertEquals("****", PatientRegistrationFormPanel.maskCccd("9"));
        assertEquals("****", PatientRegistrationFormPanel.maskCccd("1234"));
        assertEquals("***6789", PatientRegistrationFormPanel.maskCccd("  " + SYNTHETIC_CCCD + "  "));

        onEdt(() -> {
            PatientRegistrationFormPanel panel = new PatientRegistrationFormPanel();
            JTextField cccdField = findByName(panel, "cccd", JTextField.class);

            cccdField.setText("  " + SYNTHETIC_CCCD + "  ");
            assertEquals(SYNTHETIC_CCCD, panel.enteredCccd());
            assertEquals("***6789", panel.maskedEnteredCccd());
            assertFalse(panel.maskedEnteredCccd().contains("00000000"));

            cccdField.setText(" 4321 ");
            assertEquals("****", panel.maskedEnteredCccd());

            cccdField.setText("   ");
            assertEquals("", panel.maskedEnteredCccd());
        });
    }

    @Test
    void prepareDuplicateCccdSearchCopiesEnteredCccdToSearchControlAndPreservesFormFields() throws Exception {
        onEdt(() -> {
            PatientRegistrationFormPanel panel = new PatientRegistrationFormPanel();
            panel.setClinicRooms(List.of(room(101, "PK Nội tổng hợp 01")));

            JTextField searchPhone = findByName(panel, "searchPhone", JTextField.class);
            JTextField searchCccd = findByName(panel, "searchCccd", JTextField.class);
            JTextField fullName = findByName(panel, "fullName", JTextField.class);
            JTextField dateOfBirth = findByName(panel, "dateOfBirth", JTextField.class);
            JTextField phone = findByName(panel, "phone", JTextField.class);
            JTextField cccd = findByName(panel, "cccd", JTextField.class);
            JTextField address = findByName(panel, "address", JTextField.class);
            JTextField reason = findByName(panel, "reason", JTextField.class);
            JTextField insuranceNumber = findByName(panel, "insuranceNumber", JTextField.class);

            searchPhone.setText("0900000999");
            searchCccd.setText("stale-cccd");
            fullName.setText("Synthetic Nguyen Van Mau");
            dateOfBirth.setText("15/04/1992");
            phone.setText(SYNTHETIC_PHONE);
            cccd.setText("  " + SYNTHETIC_CCCD + "  ");
            address.setText("100 Synthetic Ward");
            reason.setText("Synthetic checkup reason");
            insuranceNumber.setText("SYNTH-BHYT-0001");

            panel.prepareDuplicateCccdSearch();

            assertEquals("", searchPhone.getText());
            assertEquals(SYNTHETIC_CCCD, searchCccd.getText());
            assertEquals("Synthetic Nguyen Van Mau", fullName.getText());
            assertEquals("15/04/1992", dateOfBirth.getText());
            assertEquals(SYNTHETIC_PHONE, phone.getText());
            assertEquals("  " + SYNTHETIC_CCCD + "  ", cccd.getText());
            assertEquals("100 Synthetic Ward", address.getText());
            assertEquals("Synthetic checkup reason", reason.getText());
            assertEquals("SYNTH-BHYT-0001", insuranceNumber.getText());
            assertTrue(panel.hasUnsavedInput());
            assertEquals(101, panel.selectedClinicRoomId());

            RegisterPatientCommand command = panel.toRegisterCommand(9);
            assertEquals("Synthetic Nguyen Van", command.patient().getHo());
            assertEquals("Mau", command.patient().getTen());
            assertEquals(LocalDate.of(1992, 4, 15), command.patient().getNgaySinh());
            assertEquals(SYNTHETIC_PHONE, command.patient().getDienThoai());
            assertEquals(SYNTHETIC_CCCD, command.patient().getSoCCCD());
            assertEquals("100 Synthetic Ward", command.patient().getDiaChi());
            assertNotNull(command.insurance());
            assertEquals("SYNTH-BHYT-0001", command.insurance().getSoBaoHiem());
            assertEquals(101, command.roomId());
            assertEquals("Synthetic checkup reason", command.reason());
        });
    }

    @Test
    void searchByEnteredCccdInvokesSearchCallbackWithTrimmedCccdWhilePreservingRegistrationData() throws Exception {
        onEdt(() -> {
            PatientRegistrationFormPanel panel = new PatientRegistrationFormPanel();
            panel.setClinicRooms(List.of(room(201, "PK Nội 01")));

            List<String[]> searchCalls = new ArrayList<>();
            panel.setOnSearch((phoneCriteria, cccdCriteria) -> searchCalls.add(new String[]{phoneCriteria, cccdCriteria}));

            JTextField searchPhone = findByName(panel, "searchPhone", JTextField.class);
            JTextField searchCccd = findByName(panel, "searchCccd", JTextField.class);
            JTextField fullName = findByName(panel, "fullName", JTextField.class);
            JTextField dateOfBirth = findByName(panel, "dateOfBirth", JTextField.class);
            JTextField phone = findByName(panel, "phone", JTextField.class);
            JTextField cccd = findByName(panel, "cccd", JTextField.class);
            JTextField address = findByName(panel, "address", JTextField.class);
            JTextField reason = findByName(panel, "reason", JTextField.class);

            searchPhone.setText("  0900000888  ");
            fullName.setText("Synthetic Tran Thi B");
            dateOfBirth.setText("20/10/1988");
            phone.setText(SYNTHETIC_PHONE);
            cccd.setText("   " + SYNTHETIC_CCCD + "   ");
            address.setText("22 Synthetic Street");
            reason.setText("Synthetic follow-up");

            panel.searchByEnteredCccd();

            assertEquals(1, searchCalls.size());
            assertEquals("", searchCalls.get(0)[0]);
            assertEquals(SYNTHETIC_CCCD, searchCalls.get(0)[1]);
            assertEquals("", searchPhone.getText());
            assertEquals(SYNTHETIC_CCCD, searchCccd.getText());
            assertEquals("Synthetic Tran Thi B", fullName.getText());
            assertEquals("20/10/1988", dateOfBirth.getText());
            assertEquals(SYNTHETIC_PHONE, phone.getText());
            assertEquals("22 Synthetic Street", address.getText());
            assertEquals("Synthetic follow-up", reason.getText());
            assertTrue(panel.hasUnsavedInput());
        });
    }

    @Test
    void searchCallbackReceivesTrimmedPhoneAndCccdCriteria() throws Exception {
        onEdt(() -> {
            PatientRegistrationFormPanel panel = new PatientRegistrationFormPanel();
            List<String[]> received = new ArrayList<>();
            panel.setOnSearch((phoneCriteria, cccdCriteria) -> received.add(new String[]{phoneCriteria, cccdCriteria}));

            JTextField searchPhone = findByName(panel, "searchPhone", JTextField.class);
            JTextField searchCccd = findByName(panel, "searchCccd", JTextField.class);
            JButton searchButton = findByName(panel, "searchButton", JButton.class);

            searchPhone.setText("  0900000456  ");
            searchCccd.setText("  000000009999  ");
            searchButton.doClick();

            assertEquals(1, received.size());
            assertEquals("0900000456", received.get(0)[0]);
            assertEquals("000000009999", received.get(0)[1]);

            searchPhone.setText("   ");
            searchCccd.setText("  " + SYNTHETIC_CCCD + " ");
            panel.triggerSearch();

            assertEquals(2, received.size());
            assertEquals("", received.get(1)[0]);
            assertEquals(SYNTHETIC_CCCD, received.get(1)[1]);
        });
    }

    @Test
    void clearFormClearsPatientDetailsWithoutLosingSelectedClinicRoom() throws Exception {
        onEdt(() -> {
            PatientRegistrationFormPanel panel = new PatientRegistrationFormPanel();
            panel.setClinicRooms(List.of(room(301, "PK Nội 01"), room(302, "PK Nhi 02")));

            MedicalPopupChooser clinicRoomChooser = findByName(panel, "clinicRoom", MedicalPopupChooser.class);
            clinicRoomChooser.setText("PK Nhi 02");
            assertEquals("PK Nhi 02", panel.selectedClinicRoomName());
            assertEquals(302, panel.selectedClinicRoomId());

            BenhNhan selectedPatient = new BenhNhan();
            selectedPatient.setId(77);
            selectedPatient.setMaBenhNhanFormat("SYNTH-BN-0077");
            selectedPatient.setHo("Synthetic");
            selectedPatient.setTen("Patient");
            selectedPatient.setGioiTinh(GioiTinh.F);
            selectedPatient.setNgaySinh(LocalDate.of(1995, 6, 10));
            selectedPatient.setDienThoai(SYNTHETIC_PHONE);
            selectedPatient.setSoCCCD(SYNTHETIC_CCCD);
            selectedPatient.setDiaChi("88 Synthetic Avenue");

            BaoHiem selectedInsurance = new BaoHiem();
            selectedInsurance.setId(15);
            selectedInsurance.setSoBaoHiem("SYNTH-BHYT-0077");
            selectedInsurance.setNgayBatDau(LocalDate.of(2026, 1, 1));
            selectedInsurance.setNgayHetHan(LocalDate.of(2026, 12, 31));
            selectedInsurance.setMucHuong(80);

            panel.selectPatient(selectedPatient, selectedInsurance);
            findByName(panel, "reason", JTextField.class).setText("Synthetic visit reason");
            assertTrue(panel.hasUnsavedInput());

            panel.clearForm();

            assertFalse(panel.hasUnsavedInput());
            assertEquals("", findByName(panel, "patientCode", JTextField.class).getText());
            assertEquals("", findByName(panel, "fullName", JTextField.class).getText());
            assertEquals("", findByName(panel, "dateOfBirth", JTextField.class).getText());
            assertEquals("", findByName(panel, "phone", JTextField.class).getText());
            assertEquals("", findByName(panel, "cccd", JTextField.class).getText());
            assertEquals("", findByName(panel, "address", JTextField.class).getText());
            assertEquals("", findByName(panel, "reason", JTextField.class).getText());
            assertEquals("", findByName(panel, "insuranceNumber", JTextField.class).getText());
            assertEquals("", panel.enteredCccd());

            assertEquals("PK Nhi 02", panel.selectedClinicRoomName());
            assertEquals(302, panel.selectedClinicRoomId());
            assertEquals("PK Nhi 02", clinicRoomChooser.getText());

            findByName(panel, "fullName", JTextField.class).setText("Synthetic New Entry");
            findByName(panel, "dateOfBirth", JTextField.class).setText("01/02/2000");
            RegisterPatientCommand newCommand = panel.toRegisterCommand(12);
            assertNull(newCommand.patient().getId());
            assertNull(newCommand.patient().getUpdatedBy());
            assertEquals(12, newCommand.patient().getCreatedBy());
            assertNull(newCommand.insurance());
            assertEquals(302, newCommand.roomId());
        });
    }

    @Test
    void selectPatientMapsExistingSyntheticPatientAndPreservesIdentityAndVersionAfterEdits() throws Exception {
        onEdt(() -> {
            PatientRegistrationFormPanel panel = new PatientRegistrationFormPanel();
            panel.setClinicRooms(List.of(room(401, "PK Nội 01")));

            LocalDateTime initialCreatedAt = LocalDateTime.of(2026, 3, 10, 8, 30);
            int originalCreatorId = 11;
            int syntheticUpdaterUserId = 29;

            BenhNhan existingPatient = new BenhNhan();
            existingPatient.setId(504);
            existingPatient.setMaBenhNhanFormat("SYNTH-BN-0504");
            existingPatient.setVersion(7L);
            existingPatient.setCreatedAt(initialCreatedAt);
            existingPatient.setCreatedBy(originalCreatorId);
            existingPatient.setHo("Synthetic Le Thi");
            existingPatient.setTen("Minh");
            existingPatient.setGioiTinh(GioiTinh.F);
            existingPatient.setNgaySinh(LocalDate.of(1990, 12, 25));
            existingPatient.setNoiSinh("Synthetic Province");
            existingPatient.setSoNhaThonPho("12 Synthetic Alley");
            existingPatient.setMaTinh("01");
            existingPatient.setMaHuyen("002");
            existingPatient.setMaXa("00003");
            existingPatient.setDiaChi("12 Synthetic Alley, Synthetic District");
            existingPatient.setNgheNghiep("Synthetic Engineer");
            existingPatient.setDienThoai(SYNTHETIC_PHONE);
            existingPatient.setEmail("synthetic.patient@example.invalid");
            existingPatient.setDanToc("Tày");
            existingPatient.setQuocTich("Việt Nam");
            existingPatient.setSoCCCD(SYNTHETIC_CCCD);
            existingPatient.setNgayCapCCCD(LocalDate.of(2021, 5, 20));
            existingPatient.setNoiCapCCCD("Synthetic Registry Office");
            existingPatient.setNoiLamViec("Synthetic Co");
            existingPatient.setNguoiLienHeTen("Synthetic Contact");
            existingPatient.setNguoiLienHeDienThoai("0900000777");

            BaoHiem existingInsurance = new BaoHiem();
            existingInsurance.setId(88);
            existingInsurance.setVersion(3L);
            existingInsurance.setCreatedAt(initialCreatedAt);
            existingInsurance.setCreatedBy(originalCreatorId);
            existingInsurance.setSoBaoHiem("SYNTH-BHYT-0504");
            existingInsurance.setNgayBatDau(LocalDate.of(2026, 1, 1));
            existingInsurance.setNgayHetHan(LocalDate.of(2026, 12, 31));
            existingInsurance.setMucHuong(80);
            existingInsurance.setMaNoiSong("K1");
            existingInsurance.setMaNoiDangKyKCB("01-999");
            existingInsurance.setNoiDangKyKCB("Synthetic Clinic Facility");
            existingInsurance.setDiaChiThe("12 Synthetic Alley");
            existingInsurance.setDungTuyen(false);
            existingInsurance.setCoGiayKhaiSinh(true);
            existingInsurance.setKhongGiuThe(true);
            existingInsurance.setMienCungChiTra(true);
            existingInsurance.setNgayMienCungChiTra(LocalDate.of(2026, 6, 1));
            existingInsurance.setNgayDu5NamLienTuc(LocalDate.of(2025, 1, 1));

            panel.selectPatient(existingPatient, existingInsurance);

            JTextField patientCodeField = findByName(panel, "patientCode", JTextField.class);
            JTextField fullNameField = findByName(panel, "fullName", JTextField.class);
            JTextField dateOfBirthField = findByName(panel, "dateOfBirth", JTextField.class);
            JTextField phoneField = findByName(panel, "phone", JTextField.class);
            JTextField cccdField = findByName(panel, "cccd", JTextField.class);
            JTextField addressField = findByName(panel, "address", JTextField.class);
            JTextField reasonField = findByName(panel, "reason", JTextField.class);
            JTextField insuranceNumberField = findByName(panel, "insuranceNumber", JTextField.class);

            assertFalse(patientCodeField.isEditable());
            assertEquals("SYNTH-BN-0504", patientCodeField.getText());
            assertEquals("Synthetic Le Thi Minh", fullNameField.getText());
            assertEquals("25/12/1990", dateOfBirthField.getText());
            assertEquals(SYNTHETIC_PHONE, phoneField.getText());
            assertEquals(SYNTHETIC_CCCD, cccdField.getText());
            assertEquals("12 Synthetic Alley, Synthetic District", addressField.getText());
            assertEquals("SYNTH-BHYT-0504", insuranceNumberField.getText());
            assertTrue(panel.hasUnsavedInput());

            fullNameField.setText("Synthetic Le Thi Minh Updated");
            phoneField.setText("0900000321");
            addressField.setText("99 Updated Synthetic Street");
            reasonField.setText("Synthetic periodic follow-up");
            insuranceNumberField.setText("SYNTH-BHYT-0504-NEW");

            RegisterPatientCommand command = panel.toRegisterCommand(syntheticUpdaterUserId);

            assertEquals(401, command.roomId());
            assertEquals("Synthetic periodic follow-up", command.reason());
            assertEquals(syntheticUpdaterUserId, command.createdBy());

            BenhNhan updatedPatient = command.patient();
            assertNotNull(updatedPatient);
            assertEquals(504, updatedPatient.getId());
            assertEquals("SYNTH-BN-0504", updatedPatient.getMaBenhNhanFormat());
            assertEquals(7L, updatedPatient.getVersion());
            assertEquals(initialCreatedAt, updatedPatient.getCreatedAt());
            assertEquals(originalCreatorId, updatedPatient.getCreatedBy());
            assertEquals(syntheticUpdaterUserId, updatedPatient.getUpdatedBy());

            assertEquals("Synthetic Le Thi Minh", updatedPatient.getHo());
            assertEquals("Updated", updatedPatient.getTen());
            assertEquals(GioiTinh.F, updatedPatient.getGioiTinh());
            assertEquals(LocalDate.of(1990, 12, 25), updatedPatient.getNgaySinh());
            assertEquals("0900000321", updatedPatient.getDienThoai());
            assertEquals(SYNTHETIC_CCCD, updatedPatient.getSoCCCD());
            assertEquals("99 Updated Synthetic Street", updatedPatient.getDiaChi());
            assertEquals("Synthetic Province", updatedPatient.getNoiSinh());
            assertEquals("12 Synthetic Alley", updatedPatient.getSoNhaThonPho());
            assertEquals("01", updatedPatient.getMaTinh());
            assertEquals("002", updatedPatient.getMaHuyen());
            assertEquals("00003", updatedPatient.getMaXa());
            assertEquals("Synthetic Engineer", updatedPatient.getNgheNghiep());
            assertEquals("synthetic.patient@example.invalid", updatedPatient.getEmail());
            assertEquals("Tày", updatedPatient.getDanToc());
            assertEquals("Việt Nam", updatedPatient.getQuocTich());
            assertEquals(LocalDate.of(2021, 5, 20), updatedPatient.getNgayCapCCCD());
            assertEquals("Synthetic Registry Office", updatedPatient.getNoiCapCCCD());
            assertEquals("Synthetic Co", updatedPatient.getNoiLamViec());
            assertEquals("Synthetic Contact", updatedPatient.getNguoiLienHeTen());
            assertEquals("0900000777", updatedPatient.getNguoiLienHeDienThoai());

            BaoHiem updatedInsurance = command.insurance();
            assertNotNull(updatedInsurance);
            assertEquals(88, updatedInsurance.getId());
            assertEquals(3L, updatedInsurance.getVersion());
            assertEquals(initialCreatedAt, updatedInsurance.getCreatedAt());
            assertEquals(originalCreatorId, updatedInsurance.getCreatedBy());
            assertEquals(syntheticUpdaterUserId, updatedInsurance.getUpdatedBy());
            assertEquals("SYNTH-BHYT-0504-NEW", updatedInsurance.getSoBaoHiem());
            assertEquals(LocalDate.of(2026, 1, 1), updatedInsurance.getNgayBatDau());
            assertEquals(LocalDate.of(2026, 12, 31), updatedInsurance.getNgayHetHan());
            assertEquals(80, updatedInsurance.getMucHuong());
            assertEquals("K1", updatedInsurance.getMaNoiSong());
            assertEquals("01-999", updatedInsurance.getMaNoiDangKyKCB());
            assertEquals("Synthetic Clinic Facility", updatedInsurance.getNoiDangKyKCB());
            assertEquals("12 Synthetic Alley", updatedInsurance.getDiaChiThe());
            assertFalse(updatedInsurance.isDungTuyen());
            assertTrue(updatedInsurance.isCoGiayKhaiSinh());
            assertTrue(updatedInsurance.isKhongGiuThe());
            assertTrue(updatedInsurance.isMienCungChiTra());
            assertEquals(LocalDate.of(2026, 6, 1), updatedInsurance.getNgayMienCungChiTra());
            assertEquals(LocalDate.of(2025, 1, 1), updatedInsurance.getNgayDu5NamLienTuc());
        });
    }

    private static PhongKham room(int id, String name) {
        PhongKham room = new PhongKham();
        room.setId(id);
        room.setMaKhoa(10);
        room.setTenPhong(name);
        room.setHoatDong(true);
        return room;
    }

    private static <T extends Component> T findByName(Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName()) && type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container nested) {
                T found = findByNameOrNull(nested, name, type);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("Component not found: " + name);
    }

    private static <T extends Component> T findByNameOrNull(Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName()) && type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container nested) {
                T found = findByNameOrNull(nested, name, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void onEdt(EdtAction action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(SwingUtilities.isEventDispatchThread(), "Must execute on Swing EDT");
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        Throwable thrown = failure.get();
        if (thrown != null) {
            if (thrown instanceof Exception ex) {
                throw ex;
            }
            if (thrown instanceof Error err) {
                throw err;
            }
            throw new RuntimeException(thrown);
        }
    }

    @FunctionalInterface
    private interface EdtAction {
        void run() throws Exception;
    }
}
