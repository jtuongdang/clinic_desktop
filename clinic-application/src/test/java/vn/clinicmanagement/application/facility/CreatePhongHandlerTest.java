package vn.clinicmanagement.application.facility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CreateRoomHandler Tests (FAC-01)")
class CreatePhongHandlerTest {

    private InMemoryPhongKhamRepository phongKhamRepository;
    private CreatePhongHandler handler;

    @BeforeEach
    void setUp() {
        phongKhamRepository = new InMemoryPhongKhamRepository();
        handler = new CreatePhongHandler(phongKhamRepository);
    }

    @Test
    @DisplayName("Tạo phòng khám thành công khi thông tin hợp lệ và quyền ADMIN")
    void shouldCreateRoomSuccessfullyWhenDataIsValid() {
        CreatePhongCommand command = new CreatePhongCommand(
                "ADMIN",
                "PK-NOI-01",
                "Phòng Khám Nội 01",
                1,
                10,
                "KHAM_BENH",
                "Tầng 1 - Khu A"
        );

        KetQua<CreatePhongBienNhan> result = handler.handle(command);

        assertTrue(result.isSuccess(), () -> "Kết quả phải thành công khi dữ liệu hợp lệ: " + result.getError());
        CreatePhongBienNhan receipt = result.valueOrThrow();
        assertEquals("PK-NOI-01", receipt.getMaPhong());
        assertEquals("Phòng Khám Nội 01", receipt.getTenPhong());
        assertEquals(1, receipt.getMaKhoa());
        assertEquals(10, receipt.getMaCoSo());
        assertEquals("KHAM_BENH", receipt.getLoaiPhong());
        assertTrue(receipt.getRoomId() > 0);
        assertTrue(receipt.isHoatDong());
        assertNotNull(receipt.getCreatedAt());

        // Kiểm tra phòng đã được lưu vào repository in-memory
        assertTrue(phongKhamRepository.existsByMaPhong("PK-NOI-01"));
        assertEquals(1, phongKhamRepository.getAll().size());
    }

    @Test
    @DisplayName("Từ chối khi mã phòng khám đã tồn tại trong hệ thống")
    void shouldFailWhenRoomCodeAlreadyExists() {
        // Chuẩn bị dữ liệu mẫu đã có mã phòng PK-RHM-01
        phongKhamRepository.existingCodes.add("PK-RHM-01");

        CreatePhongCommand command = new CreatePhongCommand(
                "ADMIN",
                "PK-RHM-01",
                "Phòng Khám Răng Hàm Mặt",
                2,
                10,
                "CHUYEN_KHOA"
        );

        KetQua<CreatePhongBienNhan> result = handler.handle(command);

        assertTrue(result.isFailure(), "Phải thất bại khi mã phòng đã trùng");
        KetQua.ApplicationError error = result.getError().orElseThrow();
        assertEquals("ROOM_CODE_DUPLICATE", error.code());
        assertTrue(error.message().contains("PK-RHM-01"));
    }

    @Test
    @DisplayName("Từ chối khi mã phòng trống hoặc chỉ có khoảng trắng")
    void shouldFailWhenRoomCodeIsEmpty() {
        CreatePhongCommand emptyCodeCommand = new CreatePhongCommand(
                "ADMIN",
                "   ",
                "Phòng Cấp Cứu",
                1,
                10,
                "CAP_CUU"
        );

        KetQua<CreatePhongBienNhan> result = handler.handle(emptyCodeCommand);

        assertTrue(result.isFailure());
        assertEquals("ROOM_CODE_REQUIRED", result.getError().orElseThrow().code());
    }

    @Test
    @DisplayName("Từ chối khi tên phòng trống hoặc chỉ có khoảng trắng")
    void shouldFailWhenRoomNameIsEmpty() {
        CreatePhongCommand emptyNameCommand = new CreatePhongCommand(
                "ADMIN",
                "PK-XQ-01",
                "   ",
                3,
                10,
                "CAN_LAM_SANG"
        );

        KetQua<CreatePhongBienNhan> result = handler.handle(emptyNameCommand);

        assertTrue(result.isFailure());
        assertEquals("ROOM_NAME_REQUIRED", result.getError().orElseThrow().code());
    }

    @Test
    @DisplayName("Từ chối khi vai trò không phải ADMIN")
    void shouldFailWhenUserIsNotAdmin() {
        CreatePhongCommand doctorCommand = new CreatePhongCommand(
                "DOCTOR",
                "PK-001",
                "Phòng Khám Tim Mạch",
                1,
                10,
                "KHAM_BENH"
        );

        KetQua<CreatePhongBienNhan> result = handler.handle(doctorCommand);

        assertTrue(result.isFailure(), "Bác sĩ không được phép tạo phòng khám");
        assertEquals("AUTHZ_DENIED", result.getError().orElseThrow().code());
    }

    @Test
    @DisplayName("Từ chối khi vai trò bị null hoặc rỗng")
    void shouldFailWhenRoleIsNull() {
        CreatePhongCommand nullRoleCommand = new CreatePhongCommand(
                null,
                "PK-001",
                "Phòng Khám Da Liễu",
                1,
                10,
                "KHAM_BENH"
        );

        KetQua<CreatePhongBienNhan> result = handler.handle(nullRoleCommand);

        assertTrue(result.isFailure());
        assertEquals("AUTHZ_DENIED", result.getError().orElseThrow().code());
    }

    @Test
    @DisplayName("Từ chối khi mã khoa hoặc mã cơ sở không hợp lệ (<= 0)")
    void shouldFailWhenDepartmentOrFacilityIsInvalid() {
        CreatePhongCommand invalidDept = new CreatePhongCommand("ADMIN", "PK-01", "Phòng 1", 0, 10, "KHAM");
        KetQua<CreatePhongBienNhan> resDept = handler.handle(invalidDept);
        assertTrue(resDept.isFailure());
        assertEquals("ROOM_DEPARTMENT_INVALID", resDept.getError().orElseThrow().code());

        CreatePhongCommand invalidFac = new CreatePhongCommand("ADMIN", "PK-02", "Phòng 2", 1, -1, "KHAM");
        KetQua<CreatePhongBienNhan> resFac = handler.handle(invalidFac);
        assertTrue(resFac.isFailure());
        assertEquals("ROOM_FACILITY_INVALID", resFac.getError().orElseThrow().code());
    }

    @Test
    @DisplayName("Hỗ trợ TransactionBoundary khi được cấu hình")
    void shouldExecuteWithinTransactionWhenTransactionBoundaryProvided() {
        AtomicInteger transactionCount = new AtomicInteger(0);
        GiaoDichRanhGioi txBoundary = new GiaoDichRanhGioi() {
            @Override
            public <T> KetQua<T> inTransaction(Supplier<KetQua<T>> operation) {
                transactionCount.incrementAndGet();
                return operation.get();
            }
        };

        CreatePhongHandler txHandler = new CreatePhongHandler(phongKhamRepository, txBoundary);

        CreatePhongCommand command = new CreatePhongCommand(
                "ADMIN",
                "PK-TX-01",
                "Phòng Giao Dịch",
                1,
                1,
                "HANH_CHINH"
        );

        KetQua<CreatePhongBienNhan> result = txHandler.handle(command);

        assertTrue(result.isSuccess());
        assertEquals(1, transactionCount.get(), "Thao tác phải được bọc trong giao tác");
    }

    @Test
    @DisplayName("Từ chối khi command null")
    void shouldFailWhenCommandIsNull() {
        KetQua<CreatePhongBienNhan> result = handler.handle(null);
        assertTrue(result.isFailure());
        assertEquals("ROOM_INVALID", result.getError().orElseThrow().code());
    }

    // In-memory Mock Repository hoàn toàn độc lập, không truy cập CSDL thật
    private static class InMemoryPhongKhamRepository implements IPhongKhamRepository {
        final Set<String> existingCodes = new HashSet<>();
        final Map<Integer, PhongKham> store = new HashMap<>();
        final Map<String, PhongKham> codeToRoom = new HashMap<>();
        int idSeq = 1;

        @Override
        public Optional<PhongKham> getById(int id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<PhongKham> getByMaPhong(String maPhong) {
            return Optional.ofNullable(codeToRoom.get(maPhong));
        }

        @Override
        public boolean existsByMaPhong(String maPhong) {
            return existingCodes.contains(maPhong) || codeToRoom.containsKey(maPhong);
        }

        @Override
        public List<PhongKham> getAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<PhongKham> getByMaKhoa(int maKhoa) {
            return store.values().stream().filter(p -> p.getMaKhoa() == maKhoa).toList();
        }

        @Override
        public List<PhongKham> getByMaCoSo(int maCoSo) {
            return new ArrayList<>(store.values());
        }

        @Override
        public void add(PhongKham phongKham) {
            int id = idSeq++;
            phongKham.setId(id);
            store.put(id, phongKham);
            if (phongKham.getMaPhong() != null) {
                codeToRoom.put(phongKham.getMaPhong(), phongKham);
                existingCodes.add(phongKham.getMaPhong());
            }
            codeToRoom.put(phongKham.getTenPhong(), phongKham);
            existingCodes.add(phongKham.getTenPhong());
        }

        @Override
        public void update(PhongKham phongKham) {
            if (phongKham.getId() != null) {
                store.put(phongKham.getId(), phongKham);
            }
        }

        @Override
        public void delete(PhongKham phongKham) {
            if (phongKham.getId() != null) {
                store.remove(phongKham.getId());
            }
        }
    }
}
