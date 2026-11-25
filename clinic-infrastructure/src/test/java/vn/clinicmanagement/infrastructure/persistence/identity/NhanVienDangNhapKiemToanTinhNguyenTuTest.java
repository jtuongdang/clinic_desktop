package vn.clinicmanagement.infrastructure.persistence.identity;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NhanVienDangNhapKiemToanTinhNguyenTuTest {
    @Test
    void recordSuccessfulLoginRequiresAnActiveSharedTransaction() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = new JdbcGiaoDichQuanLy(unitOfWork);
        NhanVienRepository employees = new NhanVienRepository(
                new JdbcKetNoiNhaCungCap(transactions), unitOfWork);

        assertThrows(IllegalStateException.class,
                () -> employees.recordSuccessfulLogin(701, LocalDateTime.of(2026, 1, 15, 8, 30)));
    }
}
