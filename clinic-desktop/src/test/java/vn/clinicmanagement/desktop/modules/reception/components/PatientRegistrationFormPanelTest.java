package vn.clinicmanagement.desktop.modules.reception.components;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.reception.RegisterPatientCommand;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.common.enums.LoaiBaoHiem;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class PatientRegistrationFormPanelTest {
    private static final AuthorizationContext AUTHORIZATION =
            new AuthorizationContext(10, 20, "RECEPTION", 30, 40, UUID.randomUUID());
    private static final AuditActorContext AUDIT =
            new AuditActorContext(10, 20, "RECEPTION", 30, 40, "test-workstation", UUID.randomUUID());

    @Test
    void extractsRegistrationCommandAndBindsDemographicsAndInsurance() throws Exception {
        RegisterPatientCommand command = onEdt(() -> {
            PatientRegistrationFormPanel panel = createPanelWithRoom();
            BenhNhan patient = syntheticPatient();
            BaoHiem insurance = syntheticInsurance();
            panel.selectPatient(patient, insurance);
            return panel.toRegisterCommand(AUTHORIZATION, AUDIT);
        });

        assertEquals(7, command.roomId());
        assertEquals("Khám bệnh", command.registrationType());
        assertEquals(10, command.createdBy());
        assertEquals(51, command.patient().getId());
        assertEquals("PAT-00051", command.patient().getMaBenhNhanFormat());
        assertEquals("Nguyễn Văn", command.patient().getHo());
        assertEquals("An", command.patient().getTen());
        assertEquals(GioiTinh.F, command.patient().getGioiTinh());
        assertEquals(LocalDate.of(1990, 4, 12), command.patient().getNgaySinh());
        assertEquals("Bắc Ninh", command.patient().getNoiSinh());
        assertEquals("12 phố Tổng hợp", command.patient().getSoNhaThonPho());
        assertEquals("PATIENT-ADDRESS", command.patient().getDiaChi());
        assertEquals("0912345678", command.patient().getDienThoai());
        assertEquals("patient@example.test", command.patient().getEmail());
        assertEquals("Việt Nam", command.patient().getQuocTich());
        assertEquals("123456789012345", command.patient().getSoCCCD());
        assertEquals(LocalDate.of(2020, 2, 3), command.patient().getNgayCapCCCD());
        assertEquals("Công an", command.patient().getNoiCapCCCD());
        assertEquals("Bệnh viện", command.patient().getNoiLamViec());
        assertNotNull(command.insurance());
        assertEquals("AB1234567890123", command.insurance().getSoBaoHiem());
        assertEquals("K1", command.insurance().getMaNoiSong());
        assertEquals("01-002", command.insurance().getMaNoiDangKyKCB());
        assertEquals("Bệnh viện huyện", command.insurance().getNoiDangKyKCB());
        assertEquals(LocalDate.of(2024, 1, 1), command.insurance().getNgayBatDau());
        assertEquals(LocalDate.of(2024, 12, 31), command.insurance().getNgayHetHan());
        assertEquals(80, command.insurance().getMucHuong());
        assertTrue(command.insurance().isCoGiayKhaiSinh());
        assertTrue(command.insurance().isKhongGiuThe());
        assertTrue(command.insurance().isMienCungChiTra());
        assertEquals(LocalDate.of(2024, 2, 1), command.insurance().getNgayMienCungChiTra());
        assertEquals(LocalDate.of(2024, 3, 1), command.insurance().getNgayDu5NamLienTuc());
    }

    @Test
    void mapsOtherGenderAndLegacyComboValuesAndAcceptsExplicitEdits() throws Exception {
        onEdt(() -> {
            PatientRegistrationFormPanel panel = createPanelWithRoom();
            BenhNhan patient = syntheticPatient();
            patient.setGioiTinh(GioiTinh.O);
            patient.setDanToc("Legacy ethnicity");
            patient.setQuocTich("Legacy nationality");
            patient.setMaTinh("TINH-01");
            patient.setMaHuyen("HUYEN-01");
            patient.setNguoiLienHeTen("Synthetic Contact");
            patient.setNguoiLienHeDienThoai("legacy extension 42");
            BaoHiem insurance = syntheticInsurance();
            insurance.setLoaiBaoHiem(LoaiBaoHiem.BAO_HIEM_TAI_NAN);
            insurance.setMaNoiSong("K-legacy");
            panel.selectPatient(patient, insurance);

            JComboBox<?> gender = comboStartingWith(panel, "Nữ");
            JComboBox<?> ethnicity = comboStartingWith(panel, "Kinh");
            JComboBox<?> nationality = comboStartingWith(panel, "Việt nam");
            assertEquals("Khác", gender.getSelectedItem());
            assertEquals("Legacy ethnicity", ethnicity.getSelectedItem());
            assertEquals("Legacy nationality", nationality.getSelectedItem());
            RegisterPatientCommand roundTrip = panel.toRegisterCommand(AUTHORIZATION, AUDIT);
            assertEquals(GioiTinh.O, roundTrip.patient().getGioiTinh());
            assertEquals("Legacy ethnicity", roundTrip.patient().getDanToc());
            assertEquals("Legacy nationality", roundTrip.patient().getQuocTich());
            assertEquals("K-legacy", roundTrip.insurance().getMaNoiSong());

            gender.setSelectedItem("Nam");
            ethnicity.setSelectedItem("Tày");
            nationality.setSelectedItem("Khác");
            editText(panel, "patient@example.test", "updated@example.test");
            editText(panel, "01-002", "01-003");
            RegisterPatientCommand edited = panel.toRegisterCommand(AUTHORIZATION, AUDIT);
            assertEquals(GioiTinh.M, edited.patient().getGioiTinh());
            assertEquals("Tày", edited.patient().getDanToc());
            assertEquals("Khác", edited.patient().getQuocTich());
            assertEquals("updated@example.test", edited.patient().getEmail());
            assertEquals("01-003", edited.insurance().getMaNoiDangKyKCB());
            assertEquals("K-legacy", edited.insurance().getMaNoiSong());
            assertNull(edited.patient().getMaTinh());
            assertNull(edited.patient().getMaHuyen());
            assertNull(edited.patient().getNguoiLienHeTen());
            assertNull(edited.patient().getNguoiLienHeDienThoai());
            assertEquals("TINH-01", patient.getMaTinh());
            assertEquals("HUYEN-01", patient.getMaHuyen());
            assertEquals("Synthetic Contact", patient.getNguoiLienHeTen());
            assertEquals("legacy extension 42", patient.getNguoiLienHeDienThoai());
            assertEquals(LoaiBaoHiem.BAO_HIEM_TAI_NAN, insurance.getLoaiBaoHiem());
            return null;
        });
    }

    @Test
    void switchingPatientsPreservesUnsetCombosAndClearsPreviouslyLoadedInsurance() throws Exception {
        onEdt(() -> {
            PatientRegistrationFormPanel panel = createPanelWithRoom();
            BenhNhan first = syntheticPatient();
            first.setGioiTinh(GioiTinh.O);
            first.setDanToc("Legacy ethnicity");
            first.setQuocTich("Legacy nationality");
            panel.selectPatient(first, syntheticInsurance());

            BenhNhan second = syntheticPatient();
            second.setId(52);
            second.setDanToc(null);
            second.setQuocTich("");
            panel.selectPatient(second, null);

            RegisterPatientCommand command = panel.toRegisterCommand(AUTHORIZATION, AUDIT);
            assertEquals(52, command.patient().getId());
            assertEquals(GioiTinh.F, command.patient().getGioiTinh());
            assertNull(command.patient().getDanToc());
            assertEquals("", command.patient().getQuocTich());
            assertNull(command.insurance());
            assertEquals(5, comboStartingWith(panel, "Kinh").getItemCount());
            assertEquals(3, comboStartingWith(panel, "Việt nam").getItemCount());

            panel.clearForm();
            RegisterPatientCommand cleared = panel.toRegisterCommand(AUTHORIZATION, AUDIT);
            assertEquals("Kinh", cleared.patient().getDanToc());
            assertEquals("Việt nam", cleared.patient().getQuocTich());
            assertEquals(2, comboStartingWith(panel, "Việt nam").getItemCount());
            return null;
        });
    }

    @Test
    void enterInEitherSearchFieldInvokesTheSharedSearchCallback() throws Exception {
        List<String> searchTerms = onEdt(() -> {
            PatientRegistrationFormPanel panel = new PatientRegistrationFormPanel();
            List<String> terms = new ArrayList<>();
            panel.setOnSearch((phone, cccd) -> terms.add(phone + "|" + cccd));
            List<JTextField> fields = findTextFields(panel);
            fields.get(0).setText(" 0912345678 ");
            fields.get(1).setText(" 123456789012345 ");
            fields.get(0).postActionEvent();
            fields.get(1).postActionEvent();
            return terms;
        });

        assertEquals(List.of("0912345678|123456789012345", "0912345678|123456789012345"), searchTerms);
    }

    @Test
    void clearFormResetsPatientStateAndSetsCurrentRegistrationTime() throws Exception {
        List<Object> cleared = onEdt(() -> {
            PatientRegistrationFormPanel panel = createPanelWithRoom();
            panel.selectPatient(syntheticPatient(), syntheticInsurance());
            panel.clearForm();
            RegisterPatientCommand command = panel.toRegisterCommand(AUTHORIZATION, AUDIT);
            List<String> textValues = findTextFields(panel).stream().map(JTextField::getText).toList();
            return List.of(command, textValues);
        });

        RegisterPatientCommand command = (RegisterPatientCommand) cleared.get(0);
        @SuppressWarnings("unchecked")
        List<String> textValues = (List<String>) cleared.get(1);
        assertNull(command.patient().getId());
        assertEquals("", command.patient().getMaBenhNhanFormat());
        assertEquals("", command.patient().getHoTen());
        assertNull(command.patient().getNgaySinh());
        assertNull(command.insurance());
        assertTrue(textValues.stream().noneMatch(value -> value.contains("PAT-00051")));
        String currentTime = textValues.stream()
                .filter(value -> Pattern.matches("\\d{2}:\\d{2} \\d{2}/\\d{2}/\\d{4}", value))
                .findFirst().orElseThrow();
        assertNotNull(LocalDateTime.parse(currentTime, DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
    }

    private static PatientRegistrationFormPanel createPanelWithRoom() {
        PatientRegistrationFormPanel panel = new PatientRegistrationFormPanel();
        PhongKham room = new PhongKham();
        room.setId(7);
        room.setTenPhong("PK Nhi 1");
        panel.setClinicRooms(List.of(room));
        return panel;
    }

    private static BenhNhan syntheticPatient() {
        BenhNhan patient = new BenhNhan();
        patient.setId(51);
        patient.setMaBenhNhanFormat("PAT-00051");
        patient.setHo("Nguyễn Văn");
        patient.setTen("An");
        patient.setGioiTinh(GioiTinh.F);
        patient.setNgaySinh(LocalDate.of(1990, 4, 12));
        patient.setNoiSinh("Bắc Ninh");
        patient.setSoNhaThonPho("12 phố Tổng hợp");
        patient.setMaXa("XA-01");
        patient.setDiaChi("PATIENT-ADDRESS");
        patient.setNgheNghiep("Nhân viên");
        patient.setDienThoai("0912345678");
        patient.setEmail("patient@example.test");
        patient.setDanToc("Kinh");
        patient.setQuocTich("Việt Nam");
        patient.setSoCCCD("123456789012345");
        patient.setNgayCapCCCD(LocalDate.of(2020, 2, 3));
        patient.setNoiCapCCCD("Công an");
        patient.setNoiLamViec("Bệnh viện");
        return patient;
    }

    private static BaoHiem syntheticInsurance() {
        BaoHiem insurance = new BaoHiem();
        insurance.setSoBaoHiem("AB1234567890123");
        insurance.setMaNoiSong("K1");
        insurance.setMaNoiDangKyKCB("01-002");
        insurance.setNoiDangKyKCB("Bệnh viện huyện");
        insurance.setNgayBatDau(LocalDate.of(2024, 1, 1));
        insurance.setNgayHetHan(LocalDate.of(2024, 12, 31));
        insurance.setMucHuong(80);
        insurance.setCoGiayKhaiSinh(true);
        insurance.setKhongGiuThe(true);
        insurance.setMienCungChiTra(true);
        insurance.setNgayMienCungChiTra(LocalDate.of(2024, 2, 1));
        insurance.setNgayDu5NamLienTuc(LocalDate.of(2024, 3, 1));
        return insurance;
    }

    private static List<JTextField> findTextFields(Container container) {
        return findComponents(container, JTextField.class);
    }

    private static void editText(Container container, String before, String after) {
        findTextFields(container).stream().filter(field -> before.equals(field.getText()))
                .findFirst().orElseThrow().setText(after);
    }

    private static JComboBox<?> comboStartingWith(Container container, String firstItem) {
        return findComponents(container, JComboBox.class).stream()
                .filter(combo -> firstItem.equals(combo.getItemAt(0)))
                .findFirst().orElseThrow();
    }

    private static <T> List<T> findComponents(Container container, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) result.add(type.cast(component));
            if (component instanceof Container child) result.addAll(findComponents(child, type));
        }
        return result;
    }

    private static <T> T onEdt(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
