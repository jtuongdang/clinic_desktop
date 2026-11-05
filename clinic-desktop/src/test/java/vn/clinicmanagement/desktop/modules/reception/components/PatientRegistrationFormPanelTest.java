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
            assertNull(newCommand.insurance());
            assertEquals(302, newCommand.roomId());
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
