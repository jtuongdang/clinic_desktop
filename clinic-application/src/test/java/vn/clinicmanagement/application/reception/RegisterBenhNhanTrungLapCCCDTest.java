package vn.clinicmanagement.application.reception;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.HoatDongPhienCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.facility.Khoa;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkPhamVi;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.patient.BenhNhanDinhDanhXungDotNgoaiLe;
import vn.clinicmanagement.domain.repositories.IBaoHiemRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanCoSoRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;
import vn.clinicmanagement.domain.repositories.IHeThongRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterBenhNhanTrungLapCCCDTest {
    private static final UUID SESSION_ID =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final PhanQuyenNguCanh CONTEXT =
            new PhanQuyenNguCanh(1, 31, "RECEPTIONIST", 1, 2, SESSION_ID);

    @Test
    void duplicateDuringPatientInsertReturnsGuidanceAndRollsBackBeforeInsuranceOrVisit() {
        AtomicInteger insuranceCalls = new AtomicInteger();
        AtomicInteger visitCalls = new AtomicInteger();
        AtomicInteger patientInsertCalls = new AtomicInteger();

        IBenhNhanRepository patients = proxy(IBenhNhanRepository.class, (method, args) -> {
            if (method.equals("getByCCCDInScope")) return Optional.empty();
            if (method.equals("add")) {
                patientInsertCalls.incrementAndGet();
                throw new BenhNhanDinhDanhXungDotNgoaiLe();
            }
            return defaultValue(method);
        });
        IBaoHiemRepository insurance = proxy(IBaoHiemRepository.class, (method, args) -> {
            insuranceCalls.incrementAndGet();
            return defaultValue(method);
        });
        IPhongKhamRepository rooms = proxy(IPhongKhamRepository.class, (method, args) -> {
            if (method.equals("getById")) return Optional.of(activeRoom());
            return defaultValue(method);
        });
        TiepDonHangDoiCongKetNoi queue = proxy(TiepDonHangDoiCongKetNoi.class, (method, args) -> {
            if (method.equals("registerVisit")) visitCalls.incrementAndGet();
            return method.equals("getQueue") || method.equals("getRoomSummaries") ? List.of() : defaultValue(method);
        });
        AtomicInteger rollbackCalls = new AtomicInteger();
        GiaoDichRanhGioi transactions = new GiaoDichRanhGioi() {
            @Override
            public <T> KetQua<T> inTransaction(java.util.function.Supplier<KetQua<T>> operation) {
                KetQua<T> result = operation.get();
                if (result.isFailure()) rollbackCalls.incrementAndGet();
                return result;
            }
        };
        IBenhNhanCoSoRepository patientFacilities = proxy(IBenhNhanCoSoRepository.class,
                (method, args) -> method.equals("ensureActiveLink") ? true : defaultValue(method));
        RegisterBenhNhanHandler handler = new RegisterBenhNhanHandler(patients, patientFacilities, insurance, rooms,
                queue, transactions, noOpAudit(), authorizationService());

        BenhNhan patient = new BenhNhan();
        patient.setHo("Nguyen");
        patient.setTen("Synthetic");
        patient.setNgaySinh(LocalDate.of(1990, 1, 1));
        patient.setSoCCCD("synthetic-cccd");
        BaoHiem card = new BaoHiem();
        card.setSoBaoHiem("synthetic-insurance");
        card.setNgayHetHan(LocalDate.of(2030, 1, 1));
        RegisterBenhNhanCommand command = new RegisterBenhNhanCommand(patient, card, 3,
                "Khám bệnh", "Synthetic test", DoiTuongKham.BHYT, false, 1, CONTEXT,
                new KiemToanTacNhanNguCanh(1, 31, "RECEPTIONIST", 1, 2, "test-station", SESSION_ID));

        KetQua<DangKyBienNhan> result = handler.handle(command);

        assertTrue(result.isFailure());
        assertEquals("PATIENT_REGISTRATION_FAILED", result.getError().orElseThrow().code());
        assertFalse(result.getError().orElseThrow().message().toLowerCase().contains("cccd"));
        assertEquals(1, patientInsertCalls.get());
        assertEquals(1, rollbackCalls.get());
        assertEquals(0, insuranceCalls.get());
        assertEquals(0, visitCalls.get());
        assertFalse(result.getError().orElseThrow().message().contains("synthetic-cccd"));
    }

    private static PhanQuyenDichVu authorizationService() {
        NhanVien employee = new NhanVien();
        employee.setId(CONTEXT.employeeId());
        VaiTro role = new VaiTro();
        role.setId(CONTEXT.roleId());
        role.setTenVaiTro(CONTEXT.roleCode());
        INhanVienRepository employeeRepository = proxy(INhanVienRepository.class, (method, args) -> switch (method) {
            case "getById" -> Optional.of(employee);
            case "getAssignedRoles" -> List.of(role);
            case "getAssignedWorkScopes" -> List.of(new NhanVienWorkPhamVi(
                    1, "Synthetic facility", 2, "Synthetic department"));
            default -> defaultValue(method);
        });
        IHeThongRepository systemRepository = new IHeThongRepository() {
            @Override
            public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) {
                return List.of(permission("PATIENT", HanhDongQuyen.TAO),
                        permission("RECEPTION", HanhDongQuyen.TAO));
            }
            @Override public Optional<String> getSystemParameterValue(String key) { return Optional.empty(); }
        };
        HoatDongPhienCongKetNoi activeSessionPort = id -> Optional.of(new HoatDongPhienCongKetNoi.ActiveSession(
                id, CONTEXT.employeeId(), CONTEXT.roleId(), CONTEXT.roleCode(),
                CONTEXT.facilityId(), CONTEXT.departmentId(), true, true, true));
        return new PhanQuyenDichVu(activeSessionPort, employeeRepository, systemRepository);
    }

    private static KiemToanWriteCongKetNoi noOpAudit() {
        return new KiemToanWriteCongKetNoi() {
            @Override public void appendInCurrentTransaction(vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu entry) {}
            @Override public void appendIndependently(vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu entry) {}
            @Override public void appendMedicalRecordInCurrentTransaction(
                    vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu entry) {}
            @Override public void appendMedicalRecordIndependently(
                    vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu entry) {}
        };
    }

    private static MaTranPhanQuyen permission(String module, HanhDongQuyen action) {
        MaTranPhanQuyen permission = new MaTranPhanQuyen();
        permission.setId(1);
        permission.setMaVaiTro(CONTEXT.roleId());
        permission.setPhanHe(module);
        permission.setHanhDong(action);
        permission.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG);
        permission.setDuocPhep(true);
        return permission;
    }

    private static PhongKham activeRoom() {
        CoSo facility = new CoSo();
        facility.setId(1);
        Khoa department = new Khoa();
        department.setId(2);
        department.setCoSo(facility);
        PhongKham room = new PhongKham();
        room.setId(3);
        room.setMaKhoa(2);
        room.setKhoa(department);
        return room;
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
            case "getById", "getLatestByPatientId", "getByCCCD", "getByCCCDInScope", "getByPhone" -> Optional.empty();
            default -> null;
        };
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] args);
    }
}
