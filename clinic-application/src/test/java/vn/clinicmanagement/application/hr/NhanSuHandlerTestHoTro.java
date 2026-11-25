package vn.clinicmanagement.application.hr;

import org.junit.jupiter.api.BeforeEach;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.ITieuChuan;
import vn.clinicmanagement.domain.repositories.PhanTrangKetQua;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

abstract class NhanSuHandlerTestHoTro {
    protected final InMemoryEmployeeRepository employees = new InMemoryEmployeeRepository();
    protected final List<RecordedAudit> audits = new ArrayList<>();
    protected boolean authorized = true;
    protected final NhanSuPhanQuyen authorization = actorId -> authorized && actorId == 7;
    protected final NhanSuKiemToanNhatKy auditLog = (action, actorId, employeeId) ->
            audits.add(new RecordedAudit(action, actorId, employeeId));
    protected final GiaoDichRanhGioi transactions = new GiaoDichRanhGioi() {
        @Override
        public <T> KetQua<T> inTransaction(java.util.function.Supplier<KetQua<T>> operation) {
            return operation.get();
        }
    };

    @BeforeEach
    void resetFixtures() {
        employees.clear();
        audits.clear();
        authorized = true;
    }

    protected NhanVien employee(int id, String code, String username, boolean active) {
        NhanVien employee = new NhanVien();
        employee.setId(id);
        employee.setMaNhanVien(code);
        employee.setHo("Nguyen");
        employee.setTen("Synthetic");
        employee.setSoDienThoai("0000000000");
        employee.setUsername(username);
        employee.setHoatDong(active);
        return employee;
    }

    protected record RecordedAudit(NhanSuKiemToanHanhDong action, int actorId, int employeeId) {}

    protected static final class InMemoryEmployeeRepository implements INhanVienRepository {
        private final List<NhanVien> data = new ArrayList<>();
        private final Map<Integer, List<String>> roleNames = new HashMap<>();

        void clear() {
            data.clear();
            roleNames.clear();
        }
        void setRoleNames(int employeeId, List<String> roles) { roleNames.put(employeeId, roles); }
        @Override public Optional<NhanVien> getById(int id) {
            return data.stream().filter(e -> e.getId() == id && !e.isDeleted()).findFirst();
        }
        @Override public List<NhanVien> getAll() { return new ArrayList<>(data); }
        @Override public PhanTrangKetQua<NhanVien> getPaged(int pageIndex, int pageSize,
                                                        ITieuChuan<NhanVien> specification) {
            throw new UnsupportedOperationException();
        }
        @Override public List<NhanVien> find(ITieuChuan<NhanVien> specification) {
            throw new UnsupportedOperationException();
        }
        @Override public boolean exists(ITieuChuan<NhanVien> specification) {
            throw new UnsupportedOperationException();
        }
        @Override public int count(ITieuChuan<NhanVien> specification) {
            throw new UnsupportedOperationException();
        }
        @Override public void add(NhanVien entity) {
            if (entity.getId() == null) entity.setId(data.stream().mapToInt(NhanVien::getId).max().orElse(0) + 1);
            data.add(entity);
        }
        @Override public void update(NhanVien entity) {
            int index = data.indexOf(entity);
            if (index >= 0) data.set(index, entity);
        }
        @Override public void delete(NhanVien entity) { entity.softDelete(7); }
        @Override public IUnitOfWork getUnitOfWork() { return null; }
        @Override public Optional<NhanVien> getByUsername(String username) {
            return data.stream().filter(e -> e.getUsername().equalsIgnoreCase(username))
                    .filter(e -> !e.isDeleted()).findFirst();
        }
        @Override public List<NhanVien> getActiveDoctors(int maCoSo) { return List.of(); }
        @Override public List<String> getRoleNames(int employeeId) {
            return roleNames.getOrDefault(employeeId, List.of());
        }
        @Override public List<vn.clinicmanagement.domain.identity.VaiTro> getAssignedRoles(int employeeId) {
            return List.of();
        }
        @Override public List<vn.clinicmanagement.domain.identity.NhanVienWorkPhamVi> getAssignedWorkScopes(int employeeId) {
            return List.of();
        }
        @Override public void recordSuccessfulLogin(int employeeId, LocalDateTime loginAt) {}
    }
}
