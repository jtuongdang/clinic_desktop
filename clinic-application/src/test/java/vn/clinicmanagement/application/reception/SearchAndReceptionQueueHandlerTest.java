package vn.clinicmanagement.application.reception;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.common.enums.LoaiBaoHiem;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.repositories.IBaoHiemRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.ISpecification;
import vn.clinicmanagement.domain.repositories.PagedResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchAndReceptionQueueHandlerTest {

    private static final String REPOSITORY_FAILURE_MESSAGE = "synthetic repository failure";

    @Test
    void searchRejectsNullAndBlankQueriesBeforeCallingRepositories() {
        FakePatientRepository patientRepo = new FakePatientRepository();
        FakeInsuranceRepository insuranceRepo = new FakeInsuranceRepository();
        SearchPatientsHandler handler = new SearchPatientsHandler(patientRepo, insuranceRepo);

        List<SearchPatientsQuery> invalidQueries = new ArrayList<>();
        invalidQueries.add(null);
        invalidQueries.add(new SearchPatientsQuery(null));
        invalidQueries.add(new SearchPatientsQuery(""));
        invalidQueries.add(new SearchPatientsQuery("   \t\n  "));

        for (SearchPatientsQuery query : invalidQueries) {
            Result<List<PatientSearchItem>> result = handler.handle(query);

            assertTrue(result.isFailure());
            Result.ApplicationError error = result.getError().orElseThrow();
            assertEquals("SEARCH_KEYWORD_REQUIRED", error.code());
            assertFalse(error.message().isBlank());
        }

        assertEquals(0, patientRepo.searchCallCount);
        assertEquals(0, insuranceRepo.lookupCallCount);
    }

    @Test
    void searchTrimsNonBlankKeywordAndProjectsLatestInsurancePerPatient() {
        FakePatientRepository patientRepo = new FakePatientRepository();
        FakeInsuranceRepository insuranceRepo = new FakeInsuranceRepository();

        BenhNhan firstPatient = syntheticPatient(101, "BN-SYN-0101", "Họ Mẫu", "Một");
        BenhNhan secondPatient = syntheticPatient(102, "BN-SYN-0102", "Họ Mẫu", "Hai");
        patientRepo.patientsToReturn = List.of(firstPatient, secondPatient);

        BaoHiem olderCard = syntheticInsurance(1, 101, "BH-SYN-OLD-0101", LocalDate.of(2025, 12, 31));
        BaoHiem latestCard = syntheticInsurance(2, 101, "BH-SYN-NEW-0101", LocalDate.of(2026, 12, 31));
        insuranceRepo.add(olderCard);
        insuranceRepo.add(latestCard);

        SearchPatientsHandler handler = new SearchPatientsHandler(patientRepo, insuranceRepo);

        Result<List<PatientSearchItem>> result = handler.handle(new SearchPatientsQuery("  BN-SYN  "));

        assertTrue(result.isSuccess());
        assertEquals("BN-SYN", patientRepo.lastSearchKeyword);
        assertEquals(1, patientRepo.searchCallCount);

        List<PatientSearchItem> items = result.valueOrThrow();
        assertEquals(2, items.size());
        assertSame(firstPatient, items.get(0).patient());
        assertSame(latestCard, items.get(0).latestInsurance());
        assertSame(secondPatient, items.get(1).patient());
        assertNull(items.get(1).latestInsurance());
        assertEquals(List.of(101, 102), insuranceRepo.queriedPatientIds);
    }

    @Test
    void searchCapsResultsAtFiftyAndAttachesLatestInsuranceForEachIncludedPatient() {
        FakePatientRepository patientRepo = new FakePatientRepository();
        FakeInsuranceRepository insuranceRepo = new FakeInsuranceRepository();

        List<BenhNhan> sixtyPatients = new ArrayList<>();
        Map<Integer, BaoHiem> expectedLatestByPatient = new HashMap<>();
        for (int i = 1; i <= 60; i++) {
            BenhNhan patient = syntheticPatient(
                    i,
                    String.format("BN-SYN-%04d", i),
                    "Họ Tổng Hợp",
                    "Bệnh Nhân " + i
            );
            sixtyPatients.add(patient);

            if (i % 3 != 0) {
                BaoHiem older = syntheticInsurance(
                        i * 10,
                        i,
                        String.format("BH-SYN-OLD-%04d", i),
                        LocalDate.of(2025, 6, 30)
                );
                BaoHiem latest = syntheticInsurance(
                        i * 10 + 1,
                        i,
                        String.format("BH-SYN-LATEST-%04d", i),
                        LocalDate.of(2026, 12, 31)
                );
                insuranceRepo.add(older);
                insuranceRepo.add(latest);
                expectedLatestByPatient.put(i, latest);
            }
        }
        patientRepo.patientsToReturn = sixtyPatients;

        SearchPatientsHandler handler = new SearchPatientsHandler(patientRepo, insuranceRepo);

        Result<List<PatientSearchItem>> result = handler.handle(new SearchPatientsQuery("Họ Tổng Hợp"));

        assertTrue(result.isSuccess());
        List<PatientSearchItem> items = result.valueOrThrow();
        assertEquals(50, items.size());
        assertEquals(50, insuranceRepo.lookupCallCount);

        for (int index = 0; index < 50; index++) {
            int expectedPatientId = index + 1;
            PatientSearchItem item = items.get(index);
            assertEquals(expectedPatientId, item.patient().getId());
            assertSame(expectedLatestByPatient.get(expectedPatientId), item.latestInsurance());
        }
        assertFalse(insuranceRepo.queriedPatientIds.contains(51));
    }

    @Test
    void searchMapsPatientRepositoryFailureToGenericErrorWithoutLeakingExceptionMessage() {
        FakePatientRepository patientRepo = new FakePatientRepository();
        patientRepo.failure = new RuntimeException(REPOSITORY_FAILURE_MESSAGE);
        FakeInsuranceRepository insuranceRepo = new FakeInsuranceRepository();
        SearchPatientsHandler handler = new SearchPatientsHandler(patientRepo, insuranceRepo);

        Result<List<PatientSearchItem>> result = handler.handle(new SearchPatientsQuery("BN-SYN-0001"));

        assertTrue(result.isFailure());
        Result.ApplicationError error = result.getError().orElseThrow();
        assertEquals("PATIENT_SEARCH_FAILED", error.code());
        assertEquals("Không thể tìm hồ sơ lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.", error.message());
        assertFalse(error.message().contains(REPOSITORY_FAILURE_MESSAGE));
        assertEquals(0, insuranceRepo.lookupCallCount);
    }

    @Test
    void searchMapsInsuranceRepositoryFailureToGenericErrorWithoutLeakingExceptionMessage() {
        FakePatientRepository patientRepo = new FakePatientRepository();
        patientRepo.patientsToReturn = List.of(syntheticPatient(201, "BN-SYN-0201", "Họ Mẫu", "Ba"));
        FakeInsuranceRepository insuranceRepo = new FakeInsuranceRepository();
        insuranceRepo.failure = new RuntimeException(REPOSITORY_FAILURE_MESSAGE);
        SearchPatientsHandler handler = new SearchPatientsHandler(patientRepo, insuranceRepo);

        Result<List<PatientSearchItem>> result = handler.handle(new SearchPatientsQuery("BN-SYN-0201"));

        assertTrue(result.isFailure());
        Result.ApplicationError error = result.getError().orElseThrow();
        assertEquals("PATIENT_SEARCH_FAILED", error.code());
        assertEquals("Không thể tìm hồ sơ lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.", error.message());
        assertFalse(error.message().contains(REPOSITORY_FAILURE_MESSAGE));
        assertEquals(1, insuranceRepo.lookupCallCount);
    }

    @Test
    void queueUsesExplicitDateAndReturnsBothQueueEntriesAndRoomSummaries() {
        FakeReceptionQueuePort queuePort = new FakeReceptionQueuePort();
        LocalDate explicitDate = LocalDate.of(2026, 11, 5);
        ReceptionQueueItem queueItem = new ReceptionQueueItem(
                501,
                301,
                "BN-SYN-0301",
                "Họ Mẫu Bốn",
                11,
                "Phòng Khám Nội 1",
                1,
                "001",
                "CHO_KHAM",
                LocalDateTime.of(2026, 11, 5, 8, 30)
        );
        ClinicRoomQueueSummary roomSummary = new ClinicRoomQueueSummary(
                11,
                "Phòng Khám Nội 1",
                "Khoa Nội Tổng Hợp",
                5,
                3,
                1
        );
        queuePort.queueItems = List.of(queueItem);
        queuePort.roomSummaries = List.of(roomSummary);

        GetReceptionQueueHandler handler = new GetReceptionQueueHandler(queuePort);

        Result<ReceptionQueueSnapshot> result = handler.handle(new GetReceptionQueueQuery(explicitDate));

        assertTrue(result.isSuccess());
        assertEquals(explicitDate, queuePort.lastQueueDate);
        assertEquals(explicitDate, queuePort.lastRoomSummariesDate);

        ReceptionQueueSnapshot snapshot = result.valueOrThrow();
        assertEquals(List.of(queueItem), snapshot.queue());
        assertEquals(List.of(roomSummary), snapshot.rooms());
    }

    @Test
    void queueDefaultsToCurrentDateWhenQueryOrDateIsNull() {
        FakeReceptionQueuePort queuePort = new FakeReceptionQueuePort();
        ReceptionQueueItem queueItem = new ReceptionQueueItem(
                502,
                302,
                "BN-SYN-0302",
                "Họ Mẫu Năm",
                12,
                "Phòng Khám Nội 2",
                2,
                "002",
                "DANG_KHAM",
                LocalDateTime.of(2026, 11, 5, 9, 0)
        );
        ClinicRoomQueueSummary roomSummary = new ClinicRoomQueueSummary(
                12,
                "Phòng Khám Nội 2",
                "Khoa Nội Tổng Hợp",
                4,
                2,
                1
        );
        queuePort.queueItems = List.of(queueItem);
        queuePort.roomSummaries = List.of(roomSummary);

        GetReceptionQueueHandler handler = new GetReceptionQueueHandler(queuePort);

        LocalDate beforeNullQuery = LocalDate.now();
        Result<ReceptionQueueSnapshot> nullQueryResult = handler.handle(null);
        LocalDate afterNullQuery = LocalDate.now();

        assertTrue(nullQueryResult.isSuccess());
        assertNotNull(queuePort.lastQueueDate);
        assertEquals(queuePort.lastQueueDate, queuePort.lastRoomSummariesDate);
        assertTrue(!queuePort.lastQueueDate.isBefore(beforeNullQuery) && !queuePort.lastQueueDate.isAfter(afterNullQuery));
        assertEquals(List.of(queueItem), nullQueryResult.valueOrThrow().queue());
        assertEquals(List.of(roomSummary), nullQueryResult.valueOrThrow().rooms());

        LocalDate beforeNullDate = LocalDate.now();
        Result<ReceptionQueueSnapshot> nullDateResult = handler.handle(new GetReceptionQueueQuery(null));
        LocalDate afterNullDate = LocalDate.now();

        assertTrue(nullDateResult.isSuccess());
        assertNotNull(queuePort.lastQueueDate);
        assertEquals(queuePort.lastQueueDate, queuePort.lastRoomSummariesDate);
        assertTrue(!queuePort.lastQueueDate.isBefore(beforeNullDate) && !queuePort.lastQueueDate.isAfter(afterNullDate));
        assertEquals(List.of(queueItem), nullDateResult.valueOrThrow().queue());
        assertEquals(List.of(roomSummary), nullDateResult.valueOrThrow().rooms());
    }

    @Test
    void queueMapsPortFailuresToGenericErrorWithoutLeakingExceptionMessage() {
        LocalDate targetDate = LocalDate.of(2026, 11, 5);

        FakeReceptionQueuePort failingQueuePort = new FakeReceptionQueuePort();
        failingQueuePort.queueFailure = new RuntimeException(REPOSITORY_FAILURE_MESSAGE);
        GetReceptionQueueHandler queueFailureHandler = new GetReceptionQueueHandler(failingQueuePort);

        Result<ReceptionQueueSnapshot> queueFailureResult =
                queueFailureHandler.handle(new GetReceptionQueueQuery(targetDate));

        assertTrue(queueFailureResult.isFailure());
        Result.ApplicationError queueError = queueFailureResult.getError().orElseThrow();
        assertEquals("RECEPTION_QUEUE_FAILED", queueError.code());
        assertEquals("Không thể tải hàng đợi tiếp đón.", queueError.message());
        assertFalse(queueError.message().contains(REPOSITORY_FAILURE_MESSAGE));
        assertEquals(0, failingQueuePort.roomSummariesCallCount);

        FakeReceptionQueuePort failingRoomsPort = new FakeReceptionQueuePort();
        failingRoomsPort.roomSummariesFailure = new RuntimeException(REPOSITORY_FAILURE_MESSAGE);
        GetReceptionQueueHandler roomsFailureHandler = new GetReceptionQueueHandler(failingRoomsPort);

        Result<ReceptionQueueSnapshot> roomsFailureResult =
                roomsFailureHandler.handle(new GetReceptionQueueQuery(targetDate));

        assertTrue(roomsFailureResult.isFailure());
        Result.ApplicationError roomsError = roomsFailureResult.getError().orElseThrow();
        assertEquals("RECEPTION_QUEUE_FAILED", roomsError.code());
        assertEquals("Không thể tải hàng đợi tiếp đón.", roomsError.message());
        assertFalse(roomsError.message().contains(REPOSITORY_FAILURE_MESSAGE));
        assertEquals(1, failingRoomsPort.queueCallCount);
        assertEquals(1, failingRoomsPort.roomSummariesCallCount);
    }

    private static BenhNhan syntheticPatient(int id, String patientCode, String lastName, String firstName) {
        BenhNhan patient = new BenhNhan();
        patient.setId(id);
        patient.setMaBenhNhanFormat(patientCode);
        patient.setHo(lastName);
        patient.setTen(firstName);
        patient.setNgaySinh(LocalDate.of(1990, 1, 1));
        patient.setGioiTinh(GioiTinh.M);
        patient.setDienThoai("0900000100");
        return patient;
    }

    private static BaoHiem syntheticInsurance(int id, int patientId, String cardNumber, LocalDate expiryDate) {
        BaoHiem insurance = new BaoHiem();
        insurance.setId(id);
        insurance.setMaBenhNhan(patientId);
        insurance.setLoaiBaoHiem(LoaiBaoHiem.BHYT);
        insurance.setSoBaoHiem(cardNumber);
        insurance.setNgayBatDau(expiryDate.minusYears(1));
        insurance.setNgayHetHan(expiryDate);
        insurance.setMucHuong(80);
        return insurance;
    }

    private static final class FakePatientRepository implements IBenhNhanRepository {
        private List<BenhNhan> patientsToReturn = List.of();
        private RuntimeException failure;
        private String lastSearchKeyword;
        private int searchCallCount;

        @Override
        public List<BenhNhan> search(String keyword) {
            searchCallCount++;
            lastSearchKeyword = keyword;
            if (failure != null) {
                throw failure;
            }
            return patientsToReturn;
        }

        @Override
        public Optional<BenhNhan> getByCCCD(String cccd) {
            return Optional.empty();
        }

        @Override
        public Optional<BenhNhan> getByPhone(String phone) {
            return Optional.empty();
        }

        @Override
        public Optional<BenhNhan> getById(int id) {
            return patientsToReturn.stream().filter(p -> p.getId() != null && p.getId() == id).findFirst();
        }

        @Override
        public List<BenhNhan> getAll() {
            return patientsToReturn;
        }

        @Override
        public PagedResult<BenhNhan> getPaged(int pageIndex, int pageSize, ISpecification<BenhNhan> specification) {
            return new PagedResult<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<BenhNhan> find(ISpecification<BenhNhan> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ISpecification<BenhNhan> specification) {
            return false;
        }

        @Override
        public int count(ISpecification<BenhNhan> specification) {
            return 0;
        }

        @Override
        public void add(BenhNhan entity) {
        }

        @Override
        public void update(BenhNhan entity) {
        }

        @Override
        public void delete(BenhNhan entity) {
        }

        @Override
        public IUnitOfWork getUnitOfWork() {
            return null;
        }
    }

    private static final class FakeInsuranceRepository implements IBaoHiemRepository {
        private final Map<Integer, List<BaoHiem>> cardsByPatientId = new HashMap<>();
        private final List<Integer> queriedPatientIds = new ArrayList<>();
        private RuntimeException failure;
        private int lookupCallCount;

        @Override
        public Optional<BaoHiem> getById(int id) {
            return cardsByPatientId.values().stream()
                    .flatMap(List::stream)
                    .filter(card -> card.getId() != null && card.getId() == id)
                    .findFirst();
        }

        @Override
        public Optional<BaoHiem> getLatestByPatientId(int patientId) {
            lookupCallCount++;
            queriedPatientIds.add(patientId);
            if (failure != null) {
                throw failure;
            }
            List<BaoHiem> cards = cardsByPatientId.getOrDefault(patientId, List.of());
            return cards.stream()
                    .max(Comparator.comparing(
                            BaoHiem::getNgayHetHan,
                            Comparator.nullsFirst(Comparator.naturalOrder())
                    ));
        }

        @Override
        public List<BaoHiem> getByPatientId(int patientId) {
            return List.copyOf(cardsByPatientId.getOrDefault(patientId, List.of()));
        }

        @Override
        public void add(BaoHiem insurance) {
            cardsByPatientId.computeIfAbsent(insurance.getMaBenhNhan(), ignored -> new ArrayList<>()).add(insurance);
        }

        @Override
        public void update(BaoHiem insurance) {
        }
    }

    private static final class FakeReceptionQueuePort implements ReceptionQueuePort {
        private List<ReceptionQueueItem> queueItems = List.of();
        private List<ClinicRoomQueueSummary> roomSummaries = List.of();
        private RuntimeException queueFailure;
        private RuntimeException roomSummariesFailure;
        private LocalDate lastQueueDate;
        private LocalDate lastRoomSummariesDate;
        private int queueCallCount;
        private int roomSummariesCallCount;

        @Override
        public LuotKham registerVisit(int patientId, int facilityId, int departmentId, int roomId,
                                      String registrationType, String reason, DoiTuongKham payerType,
                                      boolean priority, int createdBy) {
            throw new UnsupportedOperationException("Not used in queue query tests");
        }

        @Override
        public List<ReceptionQueueItem> getQueue(LocalDate date) {
            queueCallCount++;
            lastQueueDate = date;
            if (queueFailure != null) {
                throw queueFailure;
            }
            return queueItems;
        }

        @Override
        public List<ClinicRoomQueueSummary> getRoomSummaries(LocalDate date) {
            roomSummariesCallCount++;
            lastRoomSummariesDate = date;
            if (roomSummariesFailure != null) {
                throw roomSummariesFailure;
            }
            return roomSummaries;
        }
    }
}
