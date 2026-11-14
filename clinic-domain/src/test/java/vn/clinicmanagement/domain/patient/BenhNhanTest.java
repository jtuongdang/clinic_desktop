package vn.clinicmanagement.domain.patient;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.common.events.BenhNhanDaDangKySuKien;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.examination.LuotKham;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenhNhanTest {

    @Test
    void defaultsPatientFieldsAndCollections() {
        BenhNhan patient = new BenhNhan();

        assertEquals("", patient.getHo());
        assertEquals("", patient.getTen());
        assertNull(patient.getNgaySinh());
        assertEquals(GioiTinh.M, patient.getGioiTinh());
        assertEquals("", patient.getDienThoai());
        assertNull(patient.getEmail());
        assertNull(patient.getSoCCCD());
        assertNull(patient.getDiaChi());
        assertEquals("", patient.getMaBenhNhanFormat());
        assertEquals("", patient.getNgheNghiep());
        assertEquals("Kinh", patient.getDanToc());
        assertEquals("Việt Nam", patient.getQuocTich());
        assertEquals("", patient.getNoiSinh());
        assertEquals("", patient.getSoNhaThonPho());
        assertNull(patient.getNgayCapCCCD());
        assertEquals("", patient.getNoiCapCCCD());
        assertEquals("", patient.getNoiLamViec());
        assertTrue(patient.getBenhNhanCoSo().isEmpty());
        assertTrue(patient.getBaoHiems().isEmpty());
        assertTrue(patient.getTienSuBenhs().isEmpty());
        assertTrue(patient.getLuotKhams().isEmpty());
        assertTrue(patient.getHoaDons().isEmpty());
        assertFalse(patient.isDeleted());
        assertNotNull(patient.getCreatedAt());
    }

    @Test
    void constructorSetsPatientDataAndRegistersRegistrationEvent() {
        LocalDate birthDate = LocalDate.of(1990, 5, 20);
        BenhNhan patient = new BenhNhan(
                "Nguyen", "An", birthDate, GioiTinh.F, "0900000000",
                "an@example.test", "000000000001", "Synthetic address", 17);

        assertEquals("Nguyen", patient.getHo());
        assertEquals("An", patient.getTen());
        assertEquals("Nguyen An", patient.getHoTen());
        assertEquals(birthDate, patient.getNgaySinh());
        assertEquals(GioiTinh.F, patient.getGioiTinh());
        assertEquals("0900000000", patient.getDienThoai());
        assertEquals("an@example.test", patient.getEmail());
        assertEquals("000000000001", patient.getSoCCCD());
        assertEquals("Synthetic address", patient.getDiaChi());
        assertEquals(17, patient.getCreatedBy());
        assertEquals(1, patient.getDomainEvents().size());

        BenhNhanDaDangKySuKien event = assertInstanceOf(
                BenhNhanDaDangKySuKien.class, patient.getDomainEvents().get(0));
        assertEquals(0, event.getPatientId());
        assertEquals("000000000001", event.getCccd());
        assertEquals("Nguyen An", event.getFullName());
    }

    @Test
    void constructorRequiresPositiveCreatorId() {
        assertThrows(IllegalArgumentException.class, () -> new BenhNhan(
                "Nguyen", "An", LocalDate.of(1990, 5, 20), GioiTinh.F,
                "0900000000", null, "000000000001", null, 0));
        assertThrows(IllegalArgumentException.class, () -> new BenhNhan(
                "Nguyen", "An", LocalDate.of(1990, 5, 20), GioiTinh.F,
                "0900000000", null, "000000000001", null, -1));
    }

    @Test
    void calculatesAgeAndFullNameFromAvailablePatientData() {
        BenhNhan patient = new BenhNhan();

        assertEquals(0, patient.getTuoi());
        assertEquals("", patient.getHoTen());
        patient.setHo("  Nguyen  ");
        patient.setTen("  An  ");
        assertEquals("Nguyen     An", patient.getHoTen());

        LocalDate today = LocalDate.now();
        patient.setNgaySinh(today.minusYears(30));
        assertEquals(30, patient.getTuoi());
        patient.setNgaySinh(today.minusYears(30).plusDays(1));
        assertEquals(29, patient.getTuoi());
    }

    @Test
    void exposesPatientPropertiesThroughGettersAndSetters() {
        BenhNhan patient = new BenhNhan();
        LocalDate date = LocalDate.of(2020, 2, 3);
        List<BenhNhanCoSo> facilities = List.of(new BenhNhanCoSo());
        List<BaoHiem> insurances = List.of(new BaoHiem());
        List<TienSuBenh> histories = List.of(new TienSuBenh());
        List<LuotKham> visits = List.of(new LuotKham());
        List<HoaDon> invoices = List.of(new HoaDon());

        patient.setHo("Tran");
        patient.setTen("Binh");
        patient.setNgaySinh(date);
        patient.setGioiTinh(GioiTinh.O);
        patient.setDienThoai("0911111111");
        patient.setEmail("binh@example.test");
        patient.setSoCCCD("000000000002");
        patient.setDiaChi("Another synthetic address");
        patient.setNguoiLienHeTen("Synthetic contact");
        patient.setNguoiLienHeDienThoai("0922222222");
        patient.setUsername("synthetic-user");
        patient.setPasswordHash("synthetic-hash");
        patient.setMaBenhNhanFormat("BN-SYN-001");
        patient.setNgheNghiep("Synthetic occupation");
        patient.setDanToc("Synthetic ethnicity");
        patient.setQuocTich("Synthetic nationality");
        patient.setMaTinh("01");
        patient.setMaHuyen("001");
        patient.setMaXa("00001");
        patient.setNoiSinh("Synthetic birthplace");
        patient.setSoNhaThonPho("Synthetic street");
        patient.setNgayCapCCCD(date);
        patient.setNoiCapCCCD("Synthetic issuing authority");
        patient.setNoiLamViec("Synthetic workplace");
        patient.setBenhNhanCoSo(facilities);
        patient.setBaoHiems(insurances);
        patient.setTienSuBenhs(histories);
        patient.setLuotKhams(visits);
        patient.setHoaDons(invoices);

        assertEquals("Tran", patient.getHo());
        assertEquals("Binh", patient.getTen());
        assertEquals(date, patient.getNgaySinh());
        assertEquals(GioiTinh.O, patient.getGioiTinh());
        assertEquals("0911111111", patient.getDienThoai());
        assertEquals("binh@example.test", patient.getEmail());
        assertEquals("000000000002", patient.getSoCCCD());
        assertEquals("Another synthetic address", patient.getDiaChi());
        assertEquals("Synthetic contact", patient.getNguoiLienHeTen());
        assertEquals("0922222222", patient.getNguoiLienHeDienThoai());
        assertEquals("synthetic-user", patient.getUsername());
        assertEquals("synthetic-hash", patient.getPasswordHash());
        assertEquals("BN-SYN-001", patient.getMaBenhNhanFormat());
        assertEquals("Synthetic occupation", patient.getNgheNghiep());
        assertEquals("Synthetic ethnicity", patient.getDanToc());
        assertEquals("Synthetic nationality", patient.getQuocTich());
        assertEquals("01", patient.getMaTinh());
        assertEquals("001", patient.getMaHuyen());
        assertEquals("00001", patient.getMaXa());
        assertEquals("Synthetic birthplace", patient.getNoiSinh());
        assertEquals("Synthetic street", patient.getSoNhaThonPho());
        assertEquals(date, patient.getNgayCapCCCD());
        assertEquals("Synthetic issuing authority", patient.getNoiCapCCCD());
        assertEquals("Synthetic workplace", patient.getNoiLamViec());
        assertEquals(facilities, patient.getBenhNhanCoSo());
        assertEquals(insurances, patient.getBaoHiems());
        assertEquals(histories, patient.getTienSuBenhs());
        assertEquals(visits, patient.getLuotKhams());
        assertEquals(invoices, patient.getHoaDons());
    }

    @Test
    void supportsUpdateSoftDeleteAndRestoreLifecycle() {
        BenhNhan patient = new BenhNhan();

        patient.markAsUpdated(21);
        assertEquals(21, patient.getUpdatedBy());
        assertNotNull(patient.getUpdatedAt());

        patient.softDelete(22);
        assertTrue(patient.isDeleted());
        assertEquals(22, patient.getDeletedBy());
        assertNotNull(patient.getDeletedAt());

        patient.restore();
        assertFalse(patient.isDeleted());
        assertNull(patient.getDeletedAt());
        assertNull(patient.getDeletedBy());
    }
}
