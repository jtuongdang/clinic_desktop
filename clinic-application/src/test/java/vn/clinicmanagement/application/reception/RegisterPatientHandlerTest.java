package vn.clinicmanagement.application.reception;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.ActiveSessionPort;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.LoaiBaoHiem;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.facility.Khoa;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.patient.PatientIdentityConflictException;
import vn.clinicmanagement.domain.patient.TienSuBenh;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkScope;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.repositories.IBaoHiemRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanCoSoRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;
import vn.clinicmanagement.domain.repositories.ISystemRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterPatientHandlerTest {
    private static final int ROOM_ID = 31;
    private static final int FACILITY_ID = 11;
    private static final int DEPARTMENT_ID = 21;
    private static final String SYNTHETIC_CCCD = "synthetic-cccd-001";
    private static final UUID CORRELATION_ID =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final AuthorizationContext ACTOR =
            new AuthorizationContext(5, 31, "RECEPTIONIST", FACILITY_ID, DEPARTMENT_ID, CORRELATION_ID);
    private static final UUID AUDIT_CORRELATION_ID = UUID.fromString("f48e1b30-1ca3-4f24-9a01-5ddc001ac123");

    @Test
    void invalidCommandIsRejectedBeforeTransactionOrPersistence() {
        Fixture fixture = new Fixture();

        Result<RegistrationReceipt> result = fixture.handler.handle(null);

        assertFailure(result, "PATIENT_VALIDATION");
        assertEquals(0, fixture.transactions.calls);
        assertNoWritesOrVisit(fixture);
    }

    @Test
    void optionalPatientAndEmergencyPhonesRemainUnchangedOnInsertAndUpdate() {
        for (boolean existing : List.of(false, true)) {
            for (String primary : new String[]{null, "", " \t "}) {
                for (String emergency : new String[]{null, "", " \t "}) {
                    Fixture fixture = new Fixture();
                    BenhNhan patient = validPatient();
                    patient.setDienThoai(primary);
                    if (existing) {
                        patient.setId(52);
                        fixture.duplicatePatient = validPatient();
                        fixture.duplicatePatient.setId(52);
                        fixture.duplicatePatient.setNguoiLienHeDienThoai(emergency);
                    } else {
                        patient.setNguoiLienHeDienThoai(emergency);
                    }

                    Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, null,
                            DoiTuongKham.THU_PHI));

                    assertTrue(result.isSuccess());
                    assertEquals(existing ? 0 : 1, fixture.patientAdds.get());
                    assertEquals(existing ? 1 : 0, fixture.patientUpdates.get());
                    assertEquals(primary, fixture.savedPatientPhone);
                    assertEquals(emergency, fixture.savedEmergencyPhone);
                    assertEquals(primary, patient.getDienThoai());
                }
            }
        }
    }

    @Test
    void normalizesEditablePhonesAndPreservesStoredEmergencyPhoneOnUpdate() {
        for (boolean existing : List.of(false, true)) {
            for (boolean internationalPrimary : List.of(false, true)) {
                Fixture fixture = new Fixture();
                BenhNhan patient = validPatient();
                patient.setDienThoai(internationalPrimary ? " +84 3.123-456\t78 " : " 03 123.456-78 ");
                String emergency = internationalPrimary ? " 09 123.456-78 " : " +84 9.123-456\t78 ";
                if (existing) {
                    patient.setId(52);
                    fixture.duplicatePatient = validPatient();
                    fixture.duplicatePatient.setId(52);
                    fixture.duplicatePatient.setNguoiLienHeDienThoai(emergency);
                } else {
                    patient.setNguoiLienHeDienThoai(emergency);
                }

                Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, null,
                        DoiTuongKham.THU_PHI));

                assertTrue(result.isSuccess());
                assertEquals(existing ? 0 : 1, fixture.patientAdds.get());
                assertEquals(existing ? 1 : 0, fixture.patientUpdates.get());
                assertEquals(internationalPrimary ? "+84312345678" : "0312345678", fixture.savedPatientPhone);
                String normalizedEmergency = internationalPrimary ? "0912345678" : "+84912345678";
                assertEquals(existing ? emergency : normalizedEmergency, fixture.savedEmergencyPhone);
            }
        }
    }

    @Test
    void rejectsInvalidPatientOrEmergencyPhoneBeforeTransaction() {
        for (boolean emergencyPhone : List.of(false, true)) {
            Fixture fixture = new Fixture();
            BenhNhan patient = validPatient();
            if (emergencyPhone) patient.setNguoiLienHeDienThoai("not-a-phone");
            else patient.setDienThoai("not-a-phone");

            Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, null,
                    DoiTuongKham.THU_PHI));

            assertFailure(result, "PATIENT_VALIDATION");
            assertEquals(0, fixture.transactions.calls);
            assertNoWritesOrVisit(fixture);
            assertFalse(result.getError().orElseThrow().message().contains("not-a-phone"));
            assertTrue(fixture.auditEntries.isEmpty());
            assertTrue(fixture.independentAuditEntries.isEmpty());
        }
    }

    @Test
    void insuranceWithUnknownStartAndValidExpiryIsAcceptedWithoutInventingStart() {
        Fixture fixture = new Fixture();
        BaoHiem insurance = validInsurance();
        LocalDate expiry = LocalDate.now().plusYears(1);
        insurance.setNgayBatDau(null);
        insurance.setNgayHetHan(expiry);

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), insurance));

        assertCardRegistrationSucceeded(fixture, result);
        assertNull(insurance.getNgayBatDau());
        assertEquals(expiry, insurance.getNgayHetHan());
    }

    @Test
    void expiredInsuranceIsAcceptedWithoutAdjudicatingCoverage() {
        Fixture fixture = new Fixture();
        BaoHiem insurance = validInsurance();
        LocalDate expiry = LocalDate.now().minusDays(1);
        LocalDate start = expiry.minusYears(1);
        insurance.setNgayBatDau(start);
        insurance.setNgayHetHan(expiry);

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), insurance));

        assertCardRegistrationSucceeded(fixture, result);
        assertEquals(start, insurance.getNgayBatDau());
        assertEquals(expiry, insurance.getNgayHetHan());
    }

    @Test
    void insuranceStartingInFutureIsAcceptedWithoutAdjudicatingCoverage() {
        Fixture fixture = new Fixture();
        BaoHiem insurance = validInsurance();
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate expiry = start.plusYears(1);
        insurance.setNgayBatDau(start);
        insurance.setNgayHetHan(expiry);

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), insurance));

        assertCardRegistrationSucceeded(fixture, result);
        assertEquals(start, insurance.getNgayBatDau());
        assertEquals(expiry, insurance.getNgayHetHan());
    }

    @Test
    void insuranceStartingAndExpiringTodayIsAcceptedWithInclusiveBoundary() {
        Fixture fixture = new Fixture();
        BaoHiem insurance = validInsurance();
        LocalDate today = LocalDate.now();
        insurance.setNgayBatDau(today);
        insurance.setNgayHetHan(today);

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), insurance));

        assertCardRegistrationSucceeded(fixture, result);
        assertEquals(today, insurance.getNgayBatDau());
        assertEquals(today, insurance.getNgayHetHan());
    }

    @Test
    void invertedKnownInsuranceDatesAreRejectedBeforeTransactionForAnyPayerWithCardNumber() {
        for (DoiTuongKham payer : List.of(DoiTuongKham.BHYT, DoiTuongKham.THU_PHI)) {
            Fixture fixture = new Fixture();
            BaoHiem insurance = validInsurance();
            insurance.setNgayBatDau(LocalDate.of(2025, 1, 2));
            insurance.setNgayHetHan(LocalDate.of(2025, 1, 1));

            Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), insurance, payer));

            assertValidationBeforeTransaction(fixture, result,
                    "Ngày bắt đầu thẻ BHYT phải trước hoặc bằng ngày hết hạn.");
        }
    }

    @Test
    void missingInsuranceExpiryIsRejectedBeforeTransactionForKnownOrUnknownStart() {
        for (DoiTuongKham payer : List.of(DoiTuongKham.BHYT, DoiTuongKham.THU_PHI)) {
            for (boolean knownStart : List.of(false, true)) {
                Fixture fixture = new Fixture();
                BaoHiem insurance = validInsurance();
                insurance.setNgayBatDau(knownStart ? LocalDate.of(2025, 1, 1) : null);
                insurance.setNgayHetHan(null);

                Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), insurance, payer));

                assertValidationBeforeTransaction(fixture, result, "Vui lòng nhập hạn thẻ BHYT.");
            }
        }
    }

    @Test
    void bhytPayerWithoutInsuranceOrCardNumberIsRejectedBeforeTransaction() {
        List<BaoHiem> cards = new ArrayList<>();
        cards.add(null);
        for (String number : new String[]{null, "", "  "}) {
            BaoHiem insurance = validInsurance();
            insurance.setSoBaoHiem(number);
            cards.add(insurance);
        }
        for (BaoHiem insurance : cards) {
            Fixture fixture = new Fixture();

            Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), insurance));

            assertValidationBeforeTransaction(fixture, result, "Đối tượng BHYT cần có số thẻ và hạn thẻ.");
        }
    }

    @Test
    void insuranceDatesWithoutCardNumberDoNotBlockSelfPayReceptionOrPersistCard() {
        Fixture fixture = new Fixture();
        BaoHiem insurance = validInsurance();
        insurance.setSoBaoHiem("  ");
        insurance.setNgayBatDau(LocalDate.of(2025, 1, 2));
        insurance.setNgayHetHan(LocalDate.of(2025, 1, 1));

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), insurance,
                DoiTuongKham.THU_PHI));

        assertTrue(result.isSuccess());
        assertEquals(1, fixture.patientAdds.get());
        assertEquals(1, fixture.visitCalls.get());
        assertEquals(0, fixture.insuranceAdds.get());
        assertEquals(0, fixture.insuranceUpdates.get());
        assertFalse(fixture.transactions.committedWrites.contains("insurance"));
    }

    @Test
    void newPatientRegistrationPersistsPatientInsuranceAndVisitReceipt() {
        Fixture fixture = new Fixture();
        LocalDateTime patientCreatedAt = LocalDateTime.of(2026, 5, 1, 8, 0);
        LocalDateTime insuranceCreatedAt = LocalDateTime.of(2026, 5, 1, 8, 1);
        BenhNhan patient = validPatient();
        patient.setCreatedAt(patientCreatedAt);
        patient.setCreatedBy(ACTOR.employeeId());
        BaoHiem insurance = validInsurance();
        insurance.setCreatedAt(insuranceCreatedAt);
        insurance.setCreatedBy(ACTOR.employeeId());
        fixture.patientInsertId = 41;
        fixture.visit = visit(91, 7, "Q-007");

        Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, insurance));

        assertTrue(result.isSuccess());
        RegistrationReceipt receipt = result.valueOrThrow();
        assertEquals(41, receipt.patientId());
        assertEquals("SYNTH-PATIENT-41", receipt.patientCode());
        assertEquals(91, receipt.visitId());
        assertEquals(7, receipt.queueNumber());
        assertEquals("Q-007", receipt.queueNumberDisplay());
        assertEquals(1, fixture.patientAdds.get());
        assertEquals(0, fixture.patientUpdates.get());
        assertEquals(1, fixture.insuranceAdds.get());
        assertEquals(0, fixture.insuranceUpdates.get());
        assertEquals(41, insurance.getMaBenhNhan());
        assertNotNull(fixture.addedPatientMetadata);
        assertNull(fixture.addedPatientMetadata.id());
        assertEquals(0L, fixture.addedPatientMetadata.version());
        assertEquals(patientCreatedAt, fixture.addedPatientMetadata.createdAt());
        assertEquals(ACTOR.employeeId(), fixture.addedPatientMetadata.createdBy());
        assertNull(fixture.addedPatientMetadata.updatedBy());
        assertNull(fixture.updatedPatientMetadata);
        assertNotNull(fixture.addedInsuranceMetadata);
        assertNull(fixture.addedInsuranceMetadata.id());
        assertEquals(0L, fixture.addedInsuranceMetadata.version());
        assertEquals(insuranceCreatedAt, fixture.addedInsuranceMetadata.createdAt());
        assertEquals(ACTOR.employeeId(), fixture.addedInsuranceMetadata.createdBy());
        assertNull(fixture.addedInsuranceMetadata.updatedBy());
        assertNull(fixture.updatedInsuranceMetadata);
        assertEquals(1, fixture.visitCalls.get());
        assertEquals(List.of(41, FACILITY_ID, DEPARTMENT_ID, ROOM_ID, "Khám bệnh",
                "Synthetic reason", DoiTuongKham.BHYT, true, 5), fixture.visitArguments);
        assertEquals(1, fixture.patientFacilityCalls.get());
        assertEquals(List.of(41, FACILITY_ID, ACTOR.employeeId()), fixture.patientFacilityArguments);
        assertTrue(fixture.transactions.committedWrites.contains("patient-facility"));
        assertEquals(1, fixture.transactions.calls);
        assertEquals(0, fixture.transactions.failures);
        assertAuditEvents(fixture.auditEntries,
                AuditActionCode.PATIENT_CREATED, AuditResourceType.PATIENT, 41,
                AuditActionCode.INSURANCE_CREATED, AuditResourceType.INSURANCE, 71,
                AuditActionCode.VISIT_REGISTERED, AuditResourceType.VISIT, 91);
    }

    @Test
    void existingPatientAndInsuranceMergeFormFieldsAndPreserveStoredData() {
        Fixture fixture = new Fixture();
        LocalDateTime persistedPatientCreatedAt = LocalDateTime.of(2025, 2, 10, 8, 15);
        LocalDateTime persistedInsuranceCreatedAt = LocalDateTime.of(2025, 2, 10, 8, 20);
        BenhNhan patient = validPatient();
        patient.setId(52);
        patient.setMaBenhNhanFormat("SYNTH-SUBMITTED-52");
        patient.setHo("Updated Synthetic");
        patient.setTen("Record");
        patient.setNgaySinh(LocalDate.of(1991, 3, 4));
        patient.setGioiTinh(GioiTinh.O);
        patient.setNoiSinh("Synthetic birthplace");
        patient.setSoNhaThonPho("Synthetic street");
        patient.setMaXa("XA-UPDATED");
        patient.setDiaChi("Synthetic updated address");
        patient.setNgheNghiep("Synthetic job");
        patient.setDienThoai(" 03 123.456-78 ");
        patient.setEmail("");
        patient.setDanToc("Legacy ethnicity");
        patient.setQuocTich("Legacy nationality");
        patient.setSoCCCD("synthetic-cccd-edited");
        patient.setNgayCapCCCD(LocalDate.of(2020, 5, 6));
        patient.setNoiCapCCCD("Synthetic issuing authority");
        patient.setNoiLamViec("Synthetic workplace");
        patient.setCreatedAt(LocalDateTime.of(2026, 6, 1, 9, 0));
        patient.setCreatedBy(99);
        BenhNhan storedPatient = validPatient();
        storedPatient.setId(52);
        storedPatient.setMaBenhNhanFormat("SYNTH-EXISTING-52");
        storedPatient.setGioiTinh(GioiTinh.O);
        storedPatient.setDanToc("Legacy ethnicity");
        storedPatient.setQuocTich("Legacy nationality");
        storedPatient.setEmail("stored@example.test");
        storedPatient.setMaTinh("TINH-01");
        storedPatient.setMaHuyen("HUYEN-01");
        storedPatient.setNguoiLienHeTen("Synthetic Contact");
        storedPatient.setNguoiLienHeDienThoai("legacy extension 42");
        storedPatient.setUsername("synthetic-account");
        storedPatient.setPasswordHash("synthetic-hash-marker");
        TienSuBenh history = new TienSuBenh();
        storedPatient.setTienSuBenhs(List.of(history));
        storedPatient.setVersion(4L);
        storedPatient.setCreatedAt(persistedPatientCreatedAt);
        storedPatient.setCreatedBy(2);
        fixture.duplicatePatient = storedPatient;
        BaoHiem insurance = validInsurance();
        insurance.setNgayBatDau(LocalDate.of(2029, 1, 1));
        insurance.setMaNoiSong("K2");
        insurance.setMaNoiDangKyKCB("SYNTH-KCB");
        insurance.setNoiDangKyKCB("Synthetic facility");
        insurance.setDiaChiThe("Synthetic card address");
        insurance.setDungTuyen(false);
        insurance.setCoGiayKhaiSinh(true);
        insurance.setKhongGiuThe(true);
        insurance.setMienCungChiTra(true);
        insurance.setNgayMienCungChiTra(LocalDate.of(2029, 2, 1));
        insurance.setNgayDu5NamLienTuc(LocalDate.of(2029, 3, 1));
        insurance.setCreatedAt(LocalDateTime.of(2026, 6, 1, 9, 5));
        insurance.setCreatedBy(98);
        BaoHiem currentInsurance = new BaoHiem();
        currentInsurance.setId(8);
        currentInsurance.setMaBenhNhan(52);
        currentInsurance.setLoaiBaoHiem(LoaiBaoHiem.BAO_HIEM_TAI_NAN);
        currentInsurance.setBenhNhan(storedPatient);
        currentInsurance.setVersion(3L);
        currentInsurance.setCreatedAt(persistedInsuranceCreatedAt);
        currentInsurance.setCreatedBy(3);
        fixture.currentInsurance = currentInsurance;
        fixture.visit = visit(92, 8, "Q-008");

        Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, insurance));

        assertTrue(result.isSuccess());
        assertEquals(52, result.valueOrThrow().patientId());
        assertEquals("SYNTH-EXISTING-52", result.valueOrThrow().patientCode());
        assertEquals(0, fixture.patientAdds.get());
        assertEquals(1, fixture.patientUpdates.get());
        assertEquals(0, fixture.insuranceAdds.get());
        assertEquals(1, fixture.insuranceUpdates.get());
        assertNotNull(fixture.updatedPatientMetadata);
        assertEquals(52, fixture.updatedPatientMetadata.id());
        assertEquals(4L, fixture.updatedPatientMetadata.version());
        assertEquals(persistedPatientCreatedAt, fixture.updatedPatientMetadata.createdAt());
        assertEquals(2, fixture.updatedPatientMetadata.createdBy());
        assertEquals(ACTOR.employeeId(), fixture.updatedPatientMetadata.updatedBy());
        BenhNhan savedPatient = fixture.updatedPatient;
        assertEquals("TINH-01", savedPatient.getMaTinh());
        assertEquals("HUYEN-01", savedPatient.getMaHuyen());
        assertEquals("Synthetic Contact", savedPatient.getNguoiLienHeTen());
        assertEquals("legacy extension 42", savedPatient.getNguoiLienHeDienThoai());
        assertEquals("synthetic-account", savedPatient.getUsername());
        assertEquals("synthetic-hash-marker", savedPatient.getPasswordHash());
        assertEquals(List.of(history), savedPatient.getTienSuBenhs());
        assertEquals(patient.getHo(), savedPatient.getHo());
        assertEquals(patient.getTen(), savedPatient.getTen());
        assertEquals(patient.getNgaySinh(), savedPatient.getNgaySinh());
        assertEquals(GioiTinh.O, savedPatient.getGioiTinh());
        assertEquals(patient.getNoiSinh(), savedPatient.getNoiSinh());
        assertEquals(patient.getSoNhaThonPho(), savedPatient.getSoNhaThonPho());
        assertEquals(patient.getMaXa(), savedPatient.getMaXa());
        assertEquals(patient.getDiaChi(), savedPatient.getDiaChi());
        assertEquals(patient.getNgheNghiep(), savedPatient.getNgheNghiep());
        assertEquals("0312345678", savedPatient.getDienThoai());
        assertEquals("", savedPatient.getEmail());
        assertEquals("Legacy ethnicity", savedPatient.getDanToc());
        assertEquals("Legacy nationality", savedPatient.getQuocTich());
        assertEquals(patient.getSoCCCD(), savedPatient.getSoCCCD());
        assertEquals(patient.getNgayCapCCCD(), savedPatient.getNgayCapCCCD());
        assertEquals(patient.getNoiCapCCCD(), savedPatient.getNoiCapCCCD());
        assertEquals(patient.getNoiLamViec(), savedPatient.getNoiLamViec());
        assertNotNull(fixture.updatedInsuranceMetadata);
        assertEquals(8, fixture.updatedInsuranceMetadata.id());
        assertEquals(3L, fixture.updatedInsuranceMetadata.version());
        assertEquals(persistedInsuranceCreatedAt, fixture.updatedInsuranceMetadata.createdAt());
        assertEquals(3, fixture.updatedInsuranceMetadata.createdBy());
        assertEquals(ACTOR.employeeId(), fixture.updatedInsuranceMetadata.updatedBy());
        BaoHiem savedCard = fixture.updatedInsurance;
        assertEquals(52, savedCard.getMaBenhNhan());
        assertEquals(LoaiBaoHiem.BAO_HIEM_TAI_NAN, savedCard.getLoaiBaoHiem());
        assertSame(storedPatient, savedCard.getBenhNhan());
        assertEquals(insurance.getSoBaoHiem(), savedCard.getSoBaoHiem());
        assertEquals(insurance.getNgayBatDau(), savedCard.getNgayBatDau());
        assertEquals(insurance.getNgayHetHan(), savedCard.getNgayHetHan());
        assertEquals(insurance.getMucHuong(), savedCard.getMucHuong());
        assertEquals(insurance.getMaNoiSong(), savedCard.getMaNoiSong());
        assertEquals(insurance.getMaNoiDangKyKCB(), savedCard.getMaNoiDangKyKCB());
        assertEquals(insurance.getNoiDangKyKCB(), savedCard.getNoiDangKyKCB());
        assertEquals(insurance.getDiaChiThe(), savedCard.getDiaChiThe());
        assertFalse(savedCard.isDungTuyen());
        assertTrue(savedCard.isCoGiayKhaiSinh());
        assertTrue(savedCard.isKhongGiuThe());
        assertTrue(savedCard.isMienCungChiTra());
        assertEquals(insurance.getNgayMienCungChiTra(), savedCard.getNgayMienCungChiTra());
        assertEquals(insurance.getNgayDu5NamLienTuc(), savedCard.getNgayDu5NamLienTuc());
        assertEquals(1, fixture.visitCalls.get());
        assertEquals(1, fixture.patientFacilityCalls.get());
        assertEquals(List.of(52, FACILITY_ID, ACTOR.employeeId()), fixture.patientFacilityArguments);
        assertEquals(1, fixture.transactions.calls);
        assertEquals(0, fixture.transactions.failures);
        assertAuditEvents(fixture.auditEntries,
                AuditActionCode.PATIENT_UPDATED, AuditResourceType.PATIENT, 52,
                AuditActionCode.INSURANCE_UPDATED, AuditResourceType.INSURANCE, 8,
                AuditActionCode.VISIT_REGISTERED, AuditResourceType.VISIT, 92);
    }

    @Test
    void existingInsuranceIsRetainedWhenFormOmitsCardOrCardNumber() {
        for (BaoHiem insurance : new BaoHiem[]{null, new BaoHiem()}) {
            Fixture fixture = new Fixture();
            BenhNhan patient = validPatient();
            patient.setId(52);
            fixture.duplicatePatient = validPatient();
            fixture.duplicatePatient.setId(52);
            fixture.currentInsurance = validInsurance();
            fixture.currentInsurance.setId(8);
            fixture.currentInsurance.setMaBenhNhan(52);
            fixture.currentInsurance.setLoaiBaoHiem(LoaiBaoHiem.KHAC);

            Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, insurance,
                    DoiTuongKham.THU_PHI));

            assertTrue(result.isSuccess());
            assertEquals(0, fixture.insuranceAdds.get());
            assertEquals(0, fixture.insuranceUpdates.get());
            assertEquals("synthetic-insurance-001", fixture.currentInsurance.getSoBaoHiem());
            assertEquals(LoaiBaoHiem.KHAC, fixture.currentInsurance.getLoaiBaoHiem());
            assertAuditEvents(fixture.auditEntries,
                    AuditActionCode.PATIENT_UPDATED, AuditResourceType.PATIENT, 52,
                    AuditActionCode.VISIT_REGISTERED, AuditResourceType.VISIT, 93);
        }
    }

    @Test
    void existingPatientWithoutExistingInsuranceUpdatesPatientMetadataAndAddsNewInsurance() {
        Fixture fixture = new Fixture();
        LocalDateTime persistedPatientCreatedAt = LocalDateTime.of(2025, 3, 15, 10, 30);
        LocalDateTime newInsuranceCreatedAt = LocalDateTime.of(2026, 6, 2, 11, 0);
        BenhNhan patient = validPatient();
        patient.setId(52);
        patient.setMaBenhNhanFormat("SYNTH-EXISTING-52");
        patient.setCreatedAt(LocalDateTime.of(2026, 6, 2, 10, 59));
        patient.setCreatedBy(99);
        fixture.duplicatePatient = validPatient();
        fixture.duplicatePatient.setId(52);
        fixture.duplicatePatient.setVersion(6L);
        fixture.duplicatePatient.setCreatedAt(persistedPatientCreatedAt);
        fixture.duplicatePatient.setCreatedBy(2);
        BaoHiem insurance = validInsurance();
        insurance.setCreatedAt(newInsuranceCreatedAt);
        insurance.setCreatedBy(ACTOR.employeeId());
        fixture.visit = visit(95, 11, "Q-011");

        Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, insurance));

        assertTrue(result.isSuccess());
        assertEquals(0, fixture.patientAdds.get());
        assertEquals(1, fixture.patientUpdates.get());
        assertEquals(1, fixture.insuranceAdds.get());
        assertEquals(0, fixture.insuranceUpdates.get());
        assertNotNull(fixture.updatedPatientMetadata);
        assertEquals(52, fixture.updatedPatientMetadata.id());
        assertEquals(6L, fixture.updatedPatientMetadata.version());
        assertEquals(persistedPatientCreatedAt, fixture.updatedPatientMetadata.createdAt());
        assertEquals(2, fixture.updatedPatientMetadata.createdBy());
        assertEquals(ACTOR.employeeId(), fixture.updatedPatientMetadata.updatedBy());
        assertNotNull(fixture.addedInsuranceMetadata);
        assertNull(fixture.addedInsuranceMetadata.id());
        assertEquals(0L, fixture.addedInsuranceMetadata.version());
        assertEquals(newInsuranceCreatedAt, fixture.addedInsuranceMetadata.createdAt());
        assertEquals(ACTOR.employeeId(), fixture.addedInsuranceMetadata.createdBy());
        assertNull(fixture.addedInsuranceMetadata.updatedBy());
        assertNull(fixture.updatedInsuranceMetadata);
        assertAuditEvents(fixture.auditEntries,
                AuditActionCode.PATIENT_UPDATED, AuditResourceType.PATIENT, 52,
                AuditActionCode.INSURANCE_CREATED, AuditResourceType.INSURANCE, 71,
                AuditActionCode.VISIT_REGISTERED, AuditResourceType.VISIT, 95);
    }

    @Test
    void registrationWithoutPersistedInsuranceWritesNoInsuranceAuditEvent() {
        Fixture fixture = new Fixture();
        fixture.visit = visit(94, 10, "Q-010");

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), null,
                DoiTuongKham.THU_PHI));

        assertTrue(result.isSuccess());
        assertEquals(0, fixture.insuranceAdds.get());
        assertEquals(0, fixture.insuranceUpdates.get());
        assertAuditEvents(fixture.auditEntries,
                AuditActionCode.PATIENT_CREATED, AuditResourceType.PATIENT, 41,
                AuditActionCode.VISIT_REGISTERED, AuditResourceType.VISIT, 94);
    }

    @Test
    void inactiveOrUnlinkedRoomIsRejectedWithoutWrites() {
        List<PhongKham> unavailableRooms = new ArrayList<>();
        PhongKham inactive = activeRoom();
        inactive.setHoatDong(false);
        unavailableRooms.add(inactive);
        PhongKham withoutDepartment = activeRoom();
        withoutDepartment.setKhoa(null);
        unavailableRooms.add(withoutDepartment);
        PhongKham withoutFacility = activeRoom();
        withoutFacility.getKhoa().setCoSo(null);
        unavailableRooms.add(withoutFacility);

        for (PhongKham room : unavailableRooms) {
            Fixture fixture = new Fixture();
            fixture.room = room;

            Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), validInsurance()));

            assertFailure(result, "ROOM_UNAVAILABLE");
            assertNoWritesOrVisit(fixture);
            assertEquals(1, fixture.transactions.failures);
        }
    }

    @Test
    void roomOutsideSelectedSessionScopeIsDeniedAndAuditedWithoutBusinessWrites() {
        Fixture fixture = new Fixture();
        fixture.room = activeRoom(FACILITY_ID, DEPARTMENT_ID + 1);
        BenhNhan patient = validPatient();
        patient.setDienThoai("03 123.456-78");
        patient.setNguoiLienHeDienThoai("+84 9.123-456 78");

        Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, validInsurance()));

        assertFailure(result, "AUTH_SCOPE_MISMATCH");
        assertNoWritesOrVisit(fixture);
        assertEquals("03 123.456-78", patient.getDienThoai());
        assertEquals("+84 9.123-456 78", patient.getNguoiLienHeDienThoai());
        assertEquals(1, fixture.independentAuditEntries.size());
        SystemAuditEntry denial = fixture.independentAuditEntries.get(0);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, denial.actionCode());
        assertEquals(AuditOutcome.DENIED, denial.outcome());
        assertEquals(AuditReasonCode.SCOPE_MISMATCH, denial.reasonCode());
        assertEquals(ACTOR.employeeId(), denial.actorEmployeeId());
        assertEquals(ACTOR.facilityId(), denial.facilityId());
        assertEquals(ACTOR.departmentId(), denial.departmentId());
        assertNull(denial.resourceId());
        assertNull(denial.resourceType());
        assertEquals(1, fixture.transactions.failures);
    }

    @Test
    void roomOutsideSelectedFacilityIsDeniedAndAudited() {
        Fixture fixture = new Fixture();
        fixture.room = activeRoom(FACILITY_ID + 1, DEPARTMENT_ID);

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), validInsurance()));

        assertFailure(result, "AUTH_SCOPE_MISMATCH");
        assertNoWritesOrVisit(fixture);
        assertEquals(1, fixture.independentAuditEntries.size());
        assertNull(fixture.independentAuditEntries.get(0).resourceId());
    }

    @Test
    void missingPatientGrantIsDeniedAndAuditedWithoutFormOrPatientDetails() {
        Fixture fixture = new Fixture();
        fixture.grants.removeIf(grant -> grant.getPhanHe().equals("PATIENT"));
        BenhNhan patient = validPatient();
        patient.setDienThoai("03 123.456-78");
        patient.setNguoiLienHeDienThoai("+84 9.123-456 78");

        Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, validInsurance()));

        assertFailure(result, "AUTHORIZATION_DENIED");
        assertEquals(0, fixture.transactions.calls);
        assertNoWritesOrVisit(fixture);
        assertEquals("03 123.456-78", patient.getDienThoai());
        assertEquals("+84 9.123-456 78", patient.getNguoiLienHeDienThoai());
        assertEquals(1, fixture.independentAuditEntries.size());
        SystemAuditEntry denial = fixture.independentAuditEntries.get(0);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, denial.actionCode());
        assertNull(denial.resourceId());
        assertNull(denial.resourceType());
        assertFalse(denial.toString().contains(SYNTHETIC_CCCD));
        assertFalse(denial.toString().contains("Synthetic reason"));
        assertFalse(denial.toString().contains("synthetic-insurance-001"));
    }

    @Test
    void denialAuditFailureCannotPermitRegistrationOrExposeAuditError() {
        Fixture fixture = new Fixture();
        fixture.grants.removeIf(grant -> grant.getPhanHe().equals("PATIENT"));
        fixture.independentAuditFailure =
                new IllegalStateException("synthetic audit failure containing " + SYNTHETIC_CCCD);

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), validInsurance()));

        assertFailure(result, "AUTHORIZATION_DENIED");
        assertNoWritesOrVisit(fixture);
        assertFalse(result.getError().orElseThrow().message().contains("synthetic audit failure"));
        assertFalse(result.getError().orElseThrow().message().contains(SYNTHETIC_CCCD));
    }

    @Test
    void existingPatientRequiresPatientUpdateGrant() {
        Fixture fixture = new Fixture();
        fixture.grants.removeIf(grant -> grant.getPhanHe().equals("PATIENT")
                && grant.getHanhDong() == HanhDongQuyen.SUA);
        BenhNhan patient = validPatient();
        patient.setId(52);
        fixture.duplicatePatient = validPatient();
        fixture.duplicatePatient.setId(52);

        Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, validInsurance()));

        assertFailure(result, "AUTHORIZATION_DENIED");
        assertEquals(0, fixture.transactions.calls);
        assertEquals(0, fixture.patientUpdates.get());
        assertEquals(0, fixture.visitCalls.get());
        assertEquals(1, fixture.independentAuditEntries.size());
    }

    @Test
    void visitRequiresReceptionCreateGrant() {
        Fixture fixture = new Fixture();
        fixture.grants.removeIf(grant -> grant.getPhanHe().equals("RECEPTION"));

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), validInsurance()));

        assertFailure(result, "AUTHORIZATION_DENIED");
        assertEquals(0, fixture.transactions.calls);
        assertNoWritesOrVisit(fixture);
        assertEquals(1, fixture.independentAuditEntries.size());
    }

    @Test
    void existingPatientOutsideScopeIsNeverUpdated() {
        Fixture fixture = new Fixture();
        BenhNhan patient = validPatient();
        patient.setId(912);
        patient.setDienThoai("03 123.456-78");
        patient.setNguoiLienHeDienThoai("+84 9.123-456 78");

        Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, validInsurance()));

        assertFailure(result, "AUTH_SCOPE_MISMATCH");
        assertEquals("03 123.456-78", patient.getDienThoai());
        assertEquals("+84 9.123-456 78", patient.getNguoiLienHeDienThoai());
        assertEquals(0, fixture.patientUpdates.get());
        assertEquals(0, fixture.insuranceAdds.get());
        assertEquals(0, fixture.visitCalls.get());
        assertEquals(1, fixture.transactions.failures);
    }

    @Test
    void databaseCccdConflictOnInsertOrUpdateReturnsStableErrorWithoutInsuranceOrVisit() {
        for (boolean existing : List.of(false, true)) {
            Fixture fixture = new Fixture();
            fixture.identityConflictOnWrite = true;
            BenhNhan patient = validPatient();
            if (existing) {
                patient.setId(52);
                fixture.duplicatePatient = validPatient();
                fixture.duplicatePatient.setId(52);
            }

            Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, validInsurance()));

            assertFailure(result, "PATIENT_REGISTRATION_FAILED");
            assertEquals(existing ? 0 : 1, fixture.patientAdds.get());
            assertEquals(existing ? 1 : 0, fixture.patientUpdates.get());
            assertEquals(0, fixture.insuranceAdds.get());
            assertEquals(0, fixture.insuranceUpdates.get());
            assertEquals(0, fixture.visitCalls.get());
            assertTrue(fixture.auditEntries.isEmpty());
            assertEquals(1, fixture.transactions.failures);
        }
    }

    @Test
    void insuranceFailureReturnsSafeErrorAndDoesNotCreateVisit() {
        Fixture fixture = new Fixture();
        fixture.insuranceFailure = new IllegalStateException("failure containing " + SYNTHETIC_CCCD);

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), validInsurance()));

        assertSafePersistenceFailure(result);
        assertEquals(1, fixture.patientAdds.get());
        assertEquals(1, fixture.insuranceAdds.get());
        assertEquals(0, fixture.visitCalls.get());
        assertEquals(1, fixture.transactions.failures);
    }

    @Test
    void queueFailureReturnsSafeErrorWithoutFurtherPersistenceCalls() {
        Fixture fixture = new Fixture();
        fixture.queueFailure = new IllegalStateException("failure containing " + SYNTHETIC_CCCD);

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), validInsurance()));

        assertSafePersistenceFailure(result);
        assertEquals(1, fixture.patientAdds.get());
        assertEquals(1, fixture.insuranceAdds.get());
        assertEquals(1, fixture.visitCalls.get());
        assertEquals(1, fixture.transactions.failures);
    }

    @Test
    void existingActivePatientFacilityLinkDoesNotStageAnotherInsert() {
        Fixture fixture = new Fixture();
        fixture.patientFacilityAlreadyActive = true;
        BenhNhan patient = validPatient();
        patient.setId(52);
        fixture.duplicatePatient = validPatient();
        fixture.duplicatePatient.setId(52);

        Result<RegistrationReceipt> result = fixture.handler.handle(command(patient, validInsurance()));

        assertTrue(result.isSuccess());
        assertEquals(1, fixture.patientFacilityCalls.get());
        assertEquals(List.of(52, FACILITY_ID, ACTOR.employeeId()), fixture.patientFacilityArguments);
        assertFalse(fixture.transactions.committedWrites.contains("patient-facility"));
    }

    @Test
    void patientFacilityFailureRollsBackFullRegistrationAndReturnsSafeFailure() {
        Fixture fixture = new Fixture();
        fixture.patientFacilityFailure =
                new IllegalStateException("synthetic facility-link failure containing " + SYNTHETIC_CCCD);

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), validInsurance()));

        assertSafePersistenceFailure(result);
        assertEquals(1, fixture.patientAdds.get());
        assertEquals(1, fixture.insuranceAdds.get());
        assertEquals(1, fixture.visitCalls.get());
        assertEquals(1, fixture.patientFacilityCalls.get());
        assertEquals(List.of(41, FACILITY_ID, ACTOR.employeeId()), fixture.patientFacilityArguments);
        assertEquals(List.of(), fixture.transactions.committedWrites);
        assertEquals(List.of(), fixture.transactions.stagedWrites);
        assertEquals(1, fixture.transactions.failures);
    }

    @Test
    void auditFailureAfterBusinessWritesReturnsSafeFailureAndTransactionRollsBackAllWrites() {
        Fixture fixture = new Fixture();
        fixture.auditFailureAction = AuditActionCode.VISIT_REGISTERED;
        fixture.auditFailure = new IllegalStateException("synthetic audit storage failure");

        Result<RegistrationReceipt> result = fixture.handler.handle(command(validPatient(), validInsurance()));

        assertSafePersistenceFailure(result);
        assertEquals(1, fixture.patientAdds.get());
        assertEquals(1, fixture.insuranceAdds.get());
        assertEquals(1, fixture.visitCalls.get());
        assertEquals(List.of(), fixture.transactions.committedWrites);
        assertEquals(List.of(), fixture.transactions.stagedWrites);
        assertEquals(1, fixture.transactions.failures);
    }

    private static void assertCardRegistrationSucceeded(Fixture fixture, Result<RegistrationReceipt> result) {
        assertTrue(result.isSuccess());
        assertEquals(1, fixture.transactions.calls);
        assertEquals(0, fixture.transactions.failures);
        assertEquals(1, fixture.patientAdds.get());
        assertEquals(1, fixture.insuranceAdds.get());
        assertEquals(1, fixture.visitCalls.get());
        assertEquals(1, fixture.patientFacilityCalls.get());
        assertTrue(fixture.transactions.committedWrites.contains("insurance"));
    }

    private static void assertValidationBeforeTransaction(Fixture fixture, Result<RegistrationReceipt> result,
                                                         String message) {
        assertFailure(result, "PATIENT_VALIDATION");
        assertEquals(message, result.getError().orElseThrow().message());
        assertEquals(0, fixture.transactions.calls);
        assertNoWritesOrVisit(fixture);
        assertEquals(0, fixture.patientFacilityCalls.get());
        assertTrue(fixture.auditEntries.isEmpty());
        assertTrue(fixture.independentAuditEntries.isEmpty());
        assertTrue(fixture.transactions.stagedWrites.isEmpty());
        assertTrue(fixture.transactions.committedWrites.isEmpty());
    }

    private static void assertSafePersistenceFailure(Result<RegistrationReceipt> result) {
        assertFailure(result, "PATIENT_REGISTRATION_FAILED");
        String message = result.getError().orElseThrow().message();
        assertFalse(message.contains("failure containing"));
        assertFalse(message.contains(SYNTHETIC_CCCD));
    }

    private static void assertFailure(Result<RegistrationReceipt> result, String code) {
        assertTrue(result.isFailure());
        assertEquals(code, result.getError().orElseThrow().code());
    }

    private static void assertNoWritesOrVisit(Fixture fixture) {
        assertEquals(0, fixture.patientAdds.get());
        assertEquals(0, fixture.patientUpdates.get());
        assertEquals(0, fixture.insuranceAdds.get());
        assertEquals(0, fixture.insuranceUpdates.get());
        assertEquals(0, fixture.visitCalls.get());
    }

    private static RegisterPatientCommand command(BenhNhan patient, BaoHiem insurance) {
        return command(patient, insurance, DoiTuongKham.BHYT);
    }

    private static RegisterPatientCommand command(BenhNhan patient, BaoHiem insurance, DoiTuongKham payerType) {
        return new RegisterPatientCommand(patient, insurance, ROOM_ID, "Khám bệnh", "Synthetic reason",
                payerType, true, 5, ACTOR, new AuditActorContext(5, 31, "RECEPTIONIST", FACILITY_ID,
                DEPARTMENT_ID, "synthetic-station", AUDIT_CORRELATION_ID));
    }

    private static void assertAuditEvents(List<SystemAuditEntry> events, Object... expected) {
        assertEquals(expected.length / 3, events.size());
        for (int i = 0; i < events.size(); i++) {
            SystemAuditEntry event = events.get(i);
            assertEquals(expected[i * 3], event.actionCode());
            assertEquals(expected[i * 3 + 1], event.resourceType());
            assertEquals(expected[i * 3 + 2], event.resourceId());
            assertEquals(ACTOR.employeeId(), event.actorEmployeeId());
            assertEquals(ACTOR.roleId(), event.roleId());
            assertEquals(ACTOR.roleCode(), event.roleCode());
            assertEquals(ACTOR.facilityId(), event.facilityId());
            assertEquals(ACTOR.departmentId(), event.departmentId());
            assertEquals("synthetic-station", event.workstationId());
            assertEquals(AUDIT_CORRELATION_ID, event.correlationId());
            assertEquals(AuditOutcome.SUCCESS, event.outcome());
            assertNull(event.reasonCode());
            assertNotNull(event.occurredAtUtc());
            assertFalse(event.toString().contains(SYNTHETIC_CCCD));
            assertFalse(event.toString().contains("Synthetic Patient"));
            assertFalse(event.toString().contains("Synthetic reason"));
            assertFalse(event.toString().contains("synthetic-insurance-001"));
        }
    }

    private static BenhNhan validPatient() {
        BenhNhan patient = new BenhNhan();
        patient.setHo("Synthetic");
        patient.setTen("Patient");
        patient.setNgaySinh(LocalDate.of(1990, 1, 1));
        patient.setSoCCCD(SYNTHETIC_CCCD);
        return patient;
    }

    private static BaoHiem validInsurance() {
        BaoHiem insurance = new BaoHiem();
        insurance.setSoBaoHiem("synthetic-insurance-001");
        insurance.setNgayHetHan(LocalDate.of(2030, 1, 1));
        insurance.setMucHuong(80);
        return insurance;
    }

    private static PhongKham activeRoom() {
        return activeRoom(FACILITY_ID, DEPARTMENT_ID);
    }

    private static PhongKham activeRoom(int facilityId, int departmentId) {
        CoSo facility = new CoSo();
        facility.setId(facilityId);
        Khoa department = new Khoa();
        department.setId(departmentId);
        department.setCoSo(facility);
        PhongKham room = new PhongKham();
        room.setId(ROOM_ID);
        room.setMaKhoa(departmentId);
        room.setKhoa(department);
        return room;
    }

    private static LuotKham visit(int id, int queueNumber, String displayNumber) {
        LuotKham visit = new LuotKham();
        visit.setId(id);
        visit.setSoThuTu(queueNumber);
        visit.setSoThuTuDisplay(displayNumber);
        return visit;
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> instance == args[0];
                    case "hashCode" -> System.identityHashCode(instance);
                    case "toString" -> type.getSimpleName() + " test proxy";
                    default -> null;
                };
            }
            return invocation.invoke(method.getName(), args);
        });
        return type.cast(proxy);
    }

    private static Object defaultValue(String method) {
        return switch (method) {
            case "getById", "getByIdInScope", "getLatestByPatientId", "getByCCCD", "getByCCCDInScope", "getByPhone" -> Optional.empty();
            case "getAll", "search", "getByPatientId", "getActiveRooms" -> List.of();
            default -> null;
        };
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] args);
    }

    private static final class Fixture {
        private final AtomicInteger patientAdds = new AtomicInteger();
        private final AtomicInteger patientUpdates = new AtomicInteger();
        private final AtomicInteger insuranceAdds = new AtomicInteger();
        private final AtomicInteger insuranceUpdates = new AtomicInteger();
        private final AtomicInteger visitCalls = new AtomicInteger();
        private final AtomicInteger patientFacilityCalls = new AtomicInteger();
        private final List<Object> visitArguments = new ArrayList<>();
        private final List<Object> patientFacilityArguments = new ArrayList<>();
        private final List<SystemAuditEntry> auditEntries = new ArrayList<>();
        private final List<SystemAuditEntry> independentAuditEntries = new ArrayList<>();
        private final TransactionProbe transactions = new TransactionProbe();
        private final RegisterPatientHandler handler;
        private final List<MaTranPhanQuyen> grants = new ArrayList<>(List.of(
                grant("PATIENT", HanhDongQuyen.TAO),
                grant("PATIENT", HanhDongQuyen.SUA),
                grant("RECEPTION", HanhDongQuyen.TAO)));
        private PhongKham room = activeRoom();
        private BenhNhan duplicatePatient;
        private BaoHiem currentInsurance;
        private MetadataSnapshot addedPatientMetadata;
        private MetadataSnapshot updatedPatientMetadata;
        private MetadataSnapshot addedInsuranceMetadata;
        private MetadataSnapshot updatedInsuranceMetadata;
        private BenhNhan updatedPatient;
        private BaoHiem updatedInsurance;
        private String savedPatientPhone;
        private String savedEmergencyPhone;
        private int patientInsertId = 41;
        private boolean identityConflictOnWrite;
        private RuntimeException insuranceFailure;
        private RuntimeException queueFailure;
        private RuntimeException patientFacilityFailure;
        private RuntimeException auditFailure;
        private RuntimeException independentAuditFailure;
        private AuditActionCode auditFailureAction;
        private int insuranceInsertId = 71;
        private boolean patientFacilityAlreadyActive;
        private LuotKham visit = visit(93, 9, "Q-009");

        private Fixture() {
            IBenhNhanRepository patients = proxy(IBenhNhanRepository.class, (method, args) -> {
                return switch (method) {
                    case "getByIdInScope" -> Optional.ofNullable(duplicatePatient);
                    case "add" -> {
                        patientAdds.incrementAndGet();
                        if (identityConflictOnWrite) throw new PatientIdentityConflictException();
                        BenhNhan patient = (BenhNhan) args[0];
                        addedPatientMetadata = MetadataSnapshot.of(patient);
                        savedPatientPhone = patient.getDienThoai();
                        savedEmergencyPhone = patient.getNguoiLienHeDienThoai();
                        patient.setId(patientInsertId);
                        patient.setMaBenhNhanFormat("SYNTH-PATIENT-" + patientInsertId);
                        transactions.stage("patient");
                        yield null;
                    }
                    case "update" -> {
                        patientUpdates.incrementAndGet();
                        if (identityConflictOnWrite) throw new PatientIdentityConflictException();
                        BenhNhan patient = (BenhNhan) args[0];
                        updatedPatient = patient;
                        updatedPatientMetadata = MetadataSnapshot.of(patient);
                        savedPatientPhone = patient.getDienThoai();
                        savedEmergencyPhone = patient.getNguoiLienHeDienThoai();
                        transactions.stage("patient");
                        yield null;
                    }
                    default -> defaultValue(method);
                };
            });
            IBenhNhanCoSoRepository patientFacilities = proxy(IBenhNhanCoSoRepository.class, (method, args) -> {
                if (!method.equals("ensureActiveLink")) return defaultValue(method);
                patientFacilityCalls.incrementAndGet();
                patientFacilityArguments.clear();
                patientFacilityArguments.addAll(List.of(args));
                if (patientFacilityFailure != null) throw patientFacilityFailure;
                if (patientFacilityAlreadyActive) return false;
                transactions.stage("patient-facility");
                return true;
            });
            IBaoHiemRepository insuranceCards = proxy(IBaoHiemRepository.class, (method, args) -> {
                return switch (method) {
                    case "getLatestByPatientId" -> Optional.ofNullable(currentInsurance);
                    case "add" -> {
                        insuranceAdds.incrementAndGet();
                        if (insuranceFailure != null) throw insuranceFailure;
                        BaoHiem card = (BaoHiem) args[0];
                        addedInsuranceMetadata = MetadataSnapshot.of(card);
                        card.setId(insuranceInsertId);
                        transactions.stage("insurance");
                        yield null;
                    }
                    case "update" -> {
                        insuranceUpdates.incrementAndGet();
                        if (insuranceFailure != null) throw insuranceFailure;
                        updatedInsurance = (BaoHiem) args[0];
                        updatedInsuranceMetadata = MetadataSnapshot.of(updatedInsurance);
                        transactions.stage("insurance");
                        yield null;
                    }
                    default -> defaultValue(method);
                };
            });
            IPhongKhamRepository rooms = proxy(IPhongKhamRepository.class, (method, args) ->
                    method.equals("getById") ? Optional.ofNullable(room) : defaultValue(method));
            ReceptionQueuePort queue = proxy(ReceptionQueuePort.class, (method, args) -> {
                if (!method.equals("registerVisit")) return defaultValue(method);
                visitCalls.incrementAndGet();
                visitArguments.clear();
                visitArguments.addAll(List.of(args));
                if (queueFailure != null) throw queueFailure;
                transactions.stage("visit");
                return visit;
            });
            AuditWritePort audit = proxy(AuditWritePort.class, (method, args) -> {
                if (method.equals("appendIndependently")) {
                    if (independentAuditFailure != null) throw independentAuditFailure;
                    independentAuditEntries.add((SystemAuditEntry) args[0]);
                    return null;
                }
                if (!method.equals("appendInCurrentTransaction")) return null;
                SystemAuditEntry entry = (SystemAuditEntry) args[0];
                if (entry.actionCode() == auditFailureAction && auditFailure != null) throw auditFailure;
                auditEntries.add(entry);
                transactions.stage("audit");
                return null;
            });
            handler = new RegisterPatientHandler(patients, patientFacilities, insuranceCards, rooms, queue,
                    transactions, audit, authorizationService());
        }

        private AuthorizationService authorizationService() {
            NhanVien employee = new NhanVien();
            employee.setId(ACTOR.employeeId());
            VaiTro role = new VaiTro();
            role.setId(ACTOR.roleId());
            role.setTenVaiTro(ACTOR.roleCode());
            INhanVienRepository employeeRepository = proxy(INhanVienRepository.class, (method, args) -> switch (method) {
                case "getById" -> Optional.of(employee);
                case "getAssignedRoles" -> List.of(role);
                case "getAssignedWorkScopes" -> List.of(new NhanVienWorkScope(
                        FACILITY_ID, "Synthetic facility", DEPARTMENT_ID, "Synthetic department"));
                default -> defaultValue(method);
            });
            ISystemRepository systemRepository = new ISystemRepository() {
                @Override public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) { return grants; }
                @Override public Optional<String> getSystemParameterValue(String key) { return Optional.empty(); }
            };
            ActiveSessionPort activeSessionPort = sessionId -> Optional.of(new ActiveSessionPort.ActiveSession(
                    sessionId, ACTOR.employeeId(), ACTOR.roleId(), ACTOR.roleCode(),
                    FACILITY_ID, DEPARTMENT_ID, true, true, true));
            return new AuthorizationService(activeSessionPort, employeeRepository, systemRepository);
        }
    }

    private static MaTranPhanQuyen grant(String module, HanhDongQuyen action) {
        MaTranPhanQuyen grant = new MaTranPhanQuyen();
        grant.setId(1);
        grant.setMaVaiTro(ACTOR.roleId());
        grant.setPhanHe(module);
        grant.setHanhDong(action);
        grant.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG);
        grant.setDuocPhep(true);
        return grant;
    }

    private static final class TransactionProbe implements TransactionBoundary {
        private int calls;
        private int failures;
        private final List<String> stagedWrites = new ArrayList<>();
        private final List<String> committedWrites = new ArrayList<>();

        @Override
        public <T> Result<T> inTransaction(java.util.function.Supplier<Result<T>> operation) {
            calls++;
            int writeCheckpoint = stagedWrites.size();
            Result<T> result = operation.get();
            if (result.isFailure()) {
                failures++;
                stagedWrites.subList(writeCheckpoint, stagedWrites.size()).clear();
            } else {
                committedWrites.addAll(stagedWrites.subList(writeCheckpoint, stagedWrites.size()));
                stagedWrites.subList(writeCheckpoint, stagedWrites.size()).clear();
            }
            return result;
        }

        private void stage(String write) {
            stagedWrites.add(write);
        }
    }

    private record MetadataSnapshot(Integer id, Long version, LocalDateTime createdAt,
                                    Integer createdBy, LocalDateTime updatedAt, Integer updatedBy) {
        private static MetadataSnapshot of(BaseEntity<Integer> entity) {
            return new MetadataSnapshot(entity.getId(), entity.getVersion(), entity.getCreatedAt(),
                    entity.getCreatedBy(), entity.getUpdatedAt(), entity.getUpdatedBy());
        }
    }
}
