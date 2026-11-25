package vn.clinicmanagement.desktop.modules.reception.components;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.reception.register.RegisterBenhNhanCommand;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.common.enums.LoaiBaoHiem;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.service.DanhMucDichVu;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class BenhNhanDangKyBieuMauManHinhTest {
    private static final PhanQuyenNguCanh AUTHORIZATION =
            new PhanQuyenNguCanh(10, 20, "RECEPTION", 30, 40, UUID.randomUUID());
    private static final KiemToanTacNhanNguCanh AUDIT =
            new KiemToanTacNhanNguCanh(10, 20, "RECEPTION", 30, 40, "test-workstation", UUID.randomUUID());

    @Test
    void extractsRegistrationCommandAndBindsDemographicsAndInsurance() throws Exception {
        RegisterBenhNhanCommand command = onEdt(() -> {
            BenhNhanDangKyBieuMauManHinh panel = createPanelWithRoom();
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
    void rejectsImpossibleInsuranceStartAndEndDates() throws Exception {
        onEdt(() -> {
            BenhNhanDangKyBieuMauManHinh panel = createPanelWithRoom();
            panel.selectPatient(syntheticPatient(), syntheticInsurance());

            editInsuranceDate(panel, 0, "31", "04", "2026");
            assertThrows(IllegalArgumentException.class,
                    () -> panel.toRegisterCommand(AUTHORIZATION, AUDIT));

            editInsuranceDate(panel, 0, "01", "01", "2024");
            editInsuranceDate(panel, 1, "29", "02", "2025");
            assertThrows(IllegalArgumentException.class,
                    () -> panel.toRegisterCommand(AUTHORIZATION, AUDIT));
            return null;
        });
    }

    @Test
    void mapsOtherGenderAndLegacyComboValuesAndAcceptsExplicitEdits() throws Exception {
        onEdt(() -> {
            BenhNhanDangKyBieuMauManHinh panel = createPanelWithRoom();
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
            RegisterBenhNhanCommand roundTrip = panel.toRegisterCommand(AUTHORIZATION, AUDIT);
            assertEquals(GioiTinh.O, roundTrip.patient().getGioiTinh());
            assertEquals("Legacy ethnicity", roundTrip.patient().getDanToc());
            assertEquals("Legacy nationality", roundTrip.patient().getQuocTich());
            assertEquals("K-legacy", roundTrip.insurance().getMaNoiSong());

            gender.setSelectedItem("Nam");
            ethnicity.setSelectedItem("Tày");
            nationality.setSelectedItem("Khác");
            editText(panel, "patient@example.test", "updated@example.test");
            editText(panel, "01-002", "01-003");
            RegisterBenhNhanCommand edited = panel.toRegisterCommand(AUTHORIZATION, AUDIT);
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
            BenhNhanDangKyBieuMauManHinh panel = createPanelWithRoom();
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

            RegisterBenhNhanCommand command = panel.toRegisterCommand(AUTHORIZATION, AUDIT);
            assertEquals(52, command.patient().getId());
            assertEquals(GioiTinh.F, command.patient().getGioiTinh());
            assertNull(command.patient().getDanToc());
            assertEquals("", command.patient().getQuocTich());
            assertNull(command.insurance());
            assertEquals(5, comboStartingWith(panel, "Kinh").getItemCount());
            assertEquals(3, comboStartingWith(panel, "Việt nam").getItemCount());

            panel.clearForm();
            assertThrows(DangKyKhamNhapLieuNgoaiLe.class, () -> panel.toRegisterCommand(AUTHORIZATION, AUDIT));
            assertEquals("Kinh", comboStartingWith(panel, "Kinh").getSelectedItem());
            assertEquals("Việt nam", comboStartingWith(panel, "Việt nam").getSelectedItem());
            assertEquals(2, comboStartingWith(panel, "Việt nam").getItemCount());
            return null;
        });
    }

    @Test
    void enterInEitherSearchFieldInvokesTheSharedSearchCallback() throws Exception {
        List<String> searchTerms = onEdt(() -> {
            BenhNhanDangKyBieuMauManHinh panel = new BenhNhanDangKyBieuMauManHinh();
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
        onEdt(() -> {
            var panel = createPanelWithRoom(); panel.selectPatient(syntheticPatient(), syntheticInsurance());
            panel.clearForm();
            var error = assertThrows(DangKyKhamNhapLieuNgoaiLe.class, () -> panel.toRegisterCommand(AUTHORIZATION, AUDIT));
            assertTrue(error.getMessage().contains("họ và tên"));
            List<String> values = findTextFields(panel).stream().map(JTextField::getText).toList();
            assertTrue(values.stream().noneMatch(value -> value.contains("PAT-00051") || value.contains("Nguyễn Văn An")));
            String time = values.stream().filter(value -> Pattern.matches("\\d{2}:\\d{2} \\d{2}/\\d{2}/\\d{4}", value)).findFirst().orElseThrow();
            assertNotNull(LocalDateTime.parse(time, DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
            return null;
        });
    }

    @Test
    void selectingLegacyResidenceCodeAddsAndSelectsOptionAndSwitchingAwayRemovesIt() throws Exception {
        onEdt(() -> {
            BenhNhanDangKyBieuMauManHinh panel = createPanelWithRoom();

            BenhNhan patientWithLegacy = syntheticPatient();
            BaoHiem insuranceWithLegacy = syntheticInsurance();
            insuranceWithLegacy.setMaNoiSong("K9-LEGACY");

            panel.selectPatient(patientWithLegacy, insuranceWithLegacy);

            JComboBox<?> residenceCombo = findResidenceCombo(panel);
            assertEquals("K9-LEGACY", residenceCombo.getSelectedItem(),
                    "Legacy residence code should be selected");
            assertTrue(comboContains(residenceCombo, "K9-LEGACY"),
                    "Legacy residence code should be present in combo model");

            // Switch to a patient with no insurance card
            BenhNhan patientWithoutInsurance = syntheticPatient();
            patientWithoutInsurance.setId(99);
            panel.selectPatient(patientWithoutInsurance, null);

            assertFalse(comboContains(residenceCombo, "K9-LEGACY"),
                    "Legacy residence option should be removed when switching to patient with no insurance");
            assertTrue(residenceCombo.getSelectedItem() == null || "".equals(residenceCombo.getSelectedItem()),
                    "Residence combo should reset to default empty option");

            // Select legacy again, then verify clearForm removes it as well
            panel.selectPatient(patientWithLegacy, insuranceWithLegacy);
            assertTrue(comboContains(residenceCombo, "K9-LEGACY"),
                    "Legacy residence code should be present after reselection");

            panel.clearForm();
            assertFalse(comboContains(residenceCombo, "K9-LEGACY"),
                    "Legacy residence option should be removed when calling clearForm");
            assertTrue(residenceCombo.getSelectedItem() == null || "".equals(residenceCombo.getSelectedItem()),
                    "Residence combo should reset after clearForm");

            return null;
        });
    }

    @Test
    void dynamicallySetsClinicalServicesFromDatabase() throws Exception {
        onEdt(() -> {
            BenhNhanDangKyBieuMauManHinh panel = createPanelWithRoom();

            DanhMucDichVu dv1 = new DanhMucDichVu();
            dv1.setId(1);
            dv1.setMaDichVu("DV_KHAM_NHI");
            dv1.setTenDichVu("Khám Nhi chuyên khoa");
            dv1.setLoaiDichVu(LoaiDichVu.KHAM);

            DanhMucDichVu dv2 = new DanhMucDichVu();
            dv2.setId(2);
            dv2.setMaDichVu("DV_KHAM_NOI");
            dv2.setTenDichVu("Khám Nội tổng quát");
            dv2.setLoaiDichVu(LoaiDichVu.KHAM);

            panel.setClinicalServices(List.of(dv1, dv2));
            panel.selectPatient(syntheticPatient(), null);

            assertEquals(2, panel.getClinicalServices().size());
            assertArrayEquals(new String[]{"Khám Nhi chuyên khoa", "Khám Nội tổng quát"},
                    BenhNhanDangKyBieuMauManHinh.clinicalServiceChooserLabels(panel.getClinicalServices()));
            assertEquals("Khám Nhi chuyên khoa", panel.getChooserService().getText());

            RegisterBenhNhanCommand defaultCommand = panel.toRegisterCommand(AUTHORIZATION, AUDIT);
            assertEquals(Integer.valueOf(1), defaultCommand.serviceId());

            panel.getChooserService().setText("Khám Nội tổng quát");
            RegisterBenhNhanCommand secondCommand = panel.toRegisterCommand(AUTHORIZATION, AUDIT);
            assertEquals(Integer.valueOf(2), secondCommand.serviceId());

            panel.getChooserService().setText("Dịch vụ không tồn tại");
            assertThrows(DangKyKhamNhapLieuNgoaiLe.class, () -> panel.toRegisterCommand(AUTHORIZATION, AUDIT));

            panel.getChooserService().setText("");
            assertThrows(DangKyKhamNhapLieuNgoaiLe.class, () -> panel.toRegisterCommand(AUTHORIZATION, AUDIT));

            return null;
        });
    }

    private static BenhNhanDangKyBieuMauManHinh createPanelWithRoom() {
        BenhNhanDangKyBieuMauManHinh panel = new BenhNhanDangKyBieuMauManHinh();
        PhongKham room = new PhongKham();
        room.setId(7);
        room.setTenPhong("PK Nhi 1");
        panel.setClinicRooms(List.of(room));
        var service = new DanhMucDichVu(); service.setId(11); service.setTenDichVu("Khám kiểm thử");
        service.setLoaiDichVu(LoaiDichVu.KHAM); service.setHoatDong(true);
        panel.setClinicalServices(List.of(service));
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

    private static void editInsuranceDate(Container panel, int index, String day, String month, String year) {
        var date = findComponents(panel, TiepDonNgayNhapManHinh.class).get(index);
        List<JTextField> fields = findTextFields(date);
        fields.get(0).setText(day); fields.get(1).setText(month); fields.get(2).setText(year);
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

    private static JComboBox<?> findResidenceCombo(Container container) {
        return findComponents(container, JComboBox.class).stream()
                .filter(combo -> comboContains(combo, "K1"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing residence code combo"));
    }

    private static boolean comboContains(JComboBox<?> combo, Object item) {
        ComboBoxModel<?> model = combo.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            if (Objects.equals(item, model.getElementAt(i))) {
                return true;
            }
        }
        return false;
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
