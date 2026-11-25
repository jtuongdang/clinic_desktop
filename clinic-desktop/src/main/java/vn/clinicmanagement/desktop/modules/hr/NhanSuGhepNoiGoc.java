package vn.clinicmanagement.desktop.modules.hr;

import vn.clinicmanagement.application.hr.KhoDuLieuNhanSuPhanQuyen;
import vn.clinicmanagement.application.hr.NhanSuKiemToanNhatKy;
import vn.clinicmanagement.application.hr.NhanSuReadPort;
import vn.clinicmanagement.application.hr.PBKDF2NhanVienMatKhauBoBam;
import vn.clinicmanagement.application.hr.assign.AssignNhanVienVaiTroHandler;
import vn.clinicmanagement.application.hr.create.CreateNhanVienHandler;
import vn.clinicmanagement.application.hr.update.UpdateNhanVienTrangThaiHandler;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.ITieuChuan;
import vn.clinicmanagement.domain.repositories.PhanTrangKetQua;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanSuQueryRepository;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepository;

import java.util.List;
import java.util.Optional;

/** Composition root cho phân hệ Nhân sự, Chấm công và Tiền lương. */
public final class NhanSuGhepNoiGoc {

    public record Services(
            CreateNhanVienHandler createEmployee,
            AssignNhanVienVaiTroHandler assignRoles,
            UpdateNhanVienTrangThaiHandler updateStatus,
            NhanSuReadPort readPort
    ) {}

    private NhanSuGhepNoiGoc() {}

    public static Services createProduction() {
        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = new JdbcGiaoDichQuanLy(uow);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(tx);
        NhanVienRepository empRepo = new NhanVienRepository(connections, uow);
        NhanSuQueryRepository queryRepo = new NhanSuQueryRepository(connections);
        KhoDuLieuNhanSuPhanQuyen auth = new KhoDuLieuNhanSuPhanQuyen(empRepo);
        NhanSuKiemToanNhatKy audit = (action, actorId, targetEmployeeId) -> {};
        PBKDF2NhanVienMatKhauBoBam hasher = new PBKDF2NhanVienMatKhauBoBam();

        return new Services(
                new CreateNhanVienHandler(empRepo, hasher, auth, audit, tx),
                new AssignNhanVienVaiTroHandler(empRepo, queryRepo, auth, audit, tx),
                new UpdateNhanVienTrangThaiHandler(empRepo, auth, audit, tx),
                queryRepo
        );
    }

    public static Services createDefault() {
        INhanVienRepository inMemRepo = new InMemoryEmployeeRepo();
        NhanSuReadPort inMemRead = new NhanSuReadPort() {
            @Override public List<EmployeeRow> listEmployees(int facilityId) { return List.of(); }
            @Override public List<RoleOption> listRoles() { return List.of(); }
            @Override public List<PositionOption> listPositions() { return List.of(); }
            @Override public List<AttendanceRow> listAttendance(int facilityId, int month, int year) { return List.of(); }
            @Override public void recordCheckInOrOut(int employeeId, int facilityId, boolean checkOut, int actorId) {}
            @Override public List<PayrollPeriodOption> listPayrollPeriods() { return List.of(); }
            @Override public List<PayrollRow> listPayroll(int periodId) { return List.of(); }
            @Override public void calculateOrCreatePayrollPeriod(int month, int year, int actorId) {}
            @Override public void lockPayrollPeriod(int periodId, int actorId) {}
        };
        var tx = new vn.clinicmanagement.application.common.GiaoDichRanhGioi() {
            @Override public <T> vn.clinicmanagement.application.common.KetQua<T> inTransaction(java.util.function.Supplier<vn.clinicmanagement.application.common.KetQua<T>> op) {
                return op.get();
            }
        };
        var auth = new KhoDuLieuNhanSuPhanQuyen(inMemRepo);
        NhanSuKiemToanNhatKy audit = (action, actorId, targetEmployeeId) -> {};
        PBKDF2NhanVienMatKhauBoBam hasher = new PBKDF2NhanVienMatKhauBoBam();
        return new Services(
                new CreateNhanVienHandler(inMemRepo, hasher, auth, audit, tx),
                new AssignNhanVienVaiTroHandler(inMemRepo, (id, r) -> {}, auth, audit, tx),
                new UpdateNhanVienTrangThaiHandler(inMemRepo, auth, audit, tx),
                inMemRead
        );
    }

    private static final class InMemoryEmployeeRepo implements INhanVienRepository {
        @Override public IUnitOfWork getUnitOfWork() { return null; }
        @Override public Optional<NhanVien> getById(int id) { return Optional.empty(); }
        @Override public List<NhanVien> getAll() { return List.of(); }
        @Override public PhanTrangKetQua<NhanVien> getPaged(int pageIndex, int pageSize, ITieuChuan<NhanVien> spec) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }
        @Override public List<NhanVien> find(ITieuChuan<NhanVien> spec) { return List.of(); }
        @Override public boolean exists(ITieuChuan<NhanVien> spec) { return false; }
        @Override public int count(ITieuChuan<NhanVien> spec) { return 0; }
        @Override public void add(NhanVien entity) {}
        @Override public void update(NhanVien entity) {}
        @Override public void delete(NhanVien entity) {}
        @Override public Optional<NhanVien> getByUsername(String username) { return Optional.empty(); }
        @Override public List<NhanVien> getActiveDoctors(int maCoSo) { return List.of(); }
        @Override public List<String> getRoleNames(int employeeId) { return List.of("ADMIN"); }
        @Override public List<vn.clinicmanagement.domain.identity.VaiTro> getAssignedRoles(int employeeId) { return List.of(); }
        @Override public List<vn.clinicmanagement.domain.identity.NhanVienWorkPhamVi> getAssignedWorkScopes(int employeeId) { return List.of(); }
        @Override public void recordSuccessfulLogin(int employeeId, java.time.LocalDateTime loginAt) {}
    }
}
