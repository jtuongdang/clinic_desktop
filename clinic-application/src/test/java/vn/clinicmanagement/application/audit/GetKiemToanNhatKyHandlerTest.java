package vn.clinicmanagement.application.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.KetQua;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GetAuditLogsHandler Tests (AUD-01)")
class GetKiemToanNhatKyHandlerTest {

    private InMemoryAuditReadPort auditReadPort;
    private GetKiemToanNhatKyHandler handler;

    @BeforeEach
    void setUp() {
        auditReadPort = new InMemoryAuditReadPort();
        handler = new GetKiemToanNhatKyHandler(auditReadPort);
    }

    @Test
    @DisplayName("Cho phép người dùng có vai trò ADMIN tra cứu nhật ký kiểm toán")
    void shouldAllowAdminToQueryAuditLogs() {
        auditReadPort.logs.add(new KiemToanNhatKyDuLieu(
                1L, "admin", "ADMIN", "FACILITY", "ROOM_CREATED",
                "PK-01", "Created room PK-01", "127.0.0.1", LocalDateTime.now(), "corr-1"
        ));

        GetKiemToanNhatKyQuery query = new GetKiemToanNhatKyQuery("ADMIN");

        KetQua<List<KiemToanNhatKyDuLieu>> result = handler.handle(query);

        assertTrue(result.isSuccess(), "ADMIN phải được phép tra cứu nhật ký");
        List<KiemToanNhatKyDuLieu> logs = result.valueOrThrow();
        assertEquals(1, logs.size());
        assertEquals("ROOM_CREATED", logs.get(0).getActionCode());
    }

    @Test
    @DisplayName("Cho phép người dùng có vai trò AUDITOR tra cứu nhật ký kiểm toán")
    void shouldAllowAuditorToQueryAuditLogs() {
        auditReadPort.logs.add(new KiemToanNhatKyDuLieu(
                2L, "auditor1", "AUDITOR", "SYSTEM", "AUDIT_VIEWED",
                "LOG-01", "Auditor viewed system config", "192.168.1.50", LocalDateTime.now(), "corr-2"
        ));

        GetKiemToanNhatKyQuery query = new GetKiemToanNhatKyQuery("AUDITOR");

        KetQua<List<KiemToanNhatKyDuLieu>> result = handler.handle(query);

        assertTrue(result.isSuccess(), "AUDITOR phải được phép tra cứu nhật ký");
        assertEquals(1, result.valueOrThrow().size());
    }

    @Test
    @DisplayName("Từ chối vai trò DOCTOR tra cứu nhật ký kiểm toán")
    void shouldDenyDoctorRoleFromQueryingAuditLogs() {
        GetKiemToanNhatKyQuery query = new GetKiemToanNhatKyQuery("DOCTOR");

        KetQua<List<KiemToanNhatKyDuLieu>> result = handler.handle(query);

        assertTrue(result.isFailure(), "DOCTOR không được phép tra cứu nhật ký kiểm toán");
        assertEquals("AUTHZ_DENIED", result.getError().orElseThrow().code());
    }

    @Test
    @DisplayName("Từ chối vai trò RECEPTIONIST, PHARMACIST, CASHIER, LAB_TECH hoặc null")
    void shouldDenyOtherRolesAndNullRole() {
        String[] unauthorizedRoles = {"RECEPTIONIST", "PHARMACIST", "CASHIER", "LAB_TECH", "GUEST", "", null};

        for (String role : unauthorizedRoles) {
            GetKiemToanNhatKyQuery query = new GetKiemToanNhatKyQuery(role);
            KetQua<List<KiemToanNhatKyDuLieu>> result = handler.handle(query);

            assertTrue(result.isFailure(), "Vai trò " + role + " phải bị từ chối");
            assertEquals("AUTHZ_DENIED", result.getError().orElseThrow().code());
        }
    }

    @Test
    @DisplayName("Tự động che giấu tuyệt đối mật khẩu, token, bí mật trong chi tiết nhật ký (Secret Masking)")
    void shouldMaskSecretsInReturnedAuditLogDetails() {
        // Dữ liệu thô từ cổng đọc chứa mật khẩu và token nhạy cảm
        KiemToanNhatKyDuLieu rawLog1 = new KiemToanNhatKyDuLieu(
                10L, "admin", "ADMIN", "AUTH", "USER_LOGIN",
                "USR-1", "{\"username\": \"admin\", \"password\": \"SuperSecretP@ss123\"}",
                "10.0.0.1", LocalDateTime.now(), "corr-10"
        );
        KiemToanNhatKyDuLieu rawLog2 = new KiemToanNhatKyDuLieu(
                11L, "system", "SYSTEM", "INTEGRATION", "API_CALL",
                "EXT-BHYT", "Request header Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.secretToken and apiKey=BHYT_SECRET_KEY_999",
                "10.0.0.2", LocalDateTime.now(), "corr-11"
        );
        KiemToanNhatKyDuLieu rawLog3 = new KiemToanNhatKyDuLieu(
                12L, "tiennv", "ADMIN", "USER_MANAGEMENT", "CHANGE_PASSWORD",
                "USR-2", "Cap nhat matKhau: NewPassword456! va token=my_access_token",
                "10.0.0.3", LocalDateTime.now(), "corr-12"
        );

        auditReadPort.logs.add(rawLog1);
        auditReadPort.logs.add(rawLog2);
        auditReadPort.logs.add(rawLog3);

        GetKiemToanNhatKyQuery query = new GetKiemToanNhatKyQuery("ADMIN");

        KetQua<List<KiemToanNhatKyDuLieu>> result = handler.handle(query);

        assertTrue(result.isSuccess());
        List<KiemToanNhatKyDuLieu> maskedLogs = result.valueOrThrow();
        assertEquals(3, maskedLogs.size());

        // Kiểm tra Log 1: mật khẩu được che giấu
        String details1 = maskedLogs.get(0).getDetails();
        assertFalse(details1.contains("SuperSecretP@ss123"), "Mật khẩu không được xuất hiện trong kết quả");
        assertTrue(details1.contains("\"password\": \"******\""));

        // Kiểm tra Log 2: Bearer token và apiKey được che giấu
        String details2 = maskedLogs.get(1).getDetails();
        assertFalse(details2.contains("eyJhbGciOiJIUzI1NiJ9.secretToken"), "Token không được xuất hiện");
        assertFalse(details2.contains("BHYT_SECRET_KEY_999"), "apiKey không được xuất hiện");
        assertTrue(details2.contains("Bearer ******"));
        assertTrue(details2.contains("apiKey=******"));

        // Kiểm tra Log 3: matKhau và token dạng text được che giấu
        String details3 = maskedLogs.get(2).getDetails();
        assertFalse(details3.contains("NewPassword456!"));
        assertFalse(details3.contains("my_access_token"));
        assertTrue(details3.contains("matKhau: ******"));
        assertTrue(details3.contains("token=******"));
    }

    @Test
    @DisplayName("Truyền đúng các tham số lọc và phân trang vào AuditReadPort")
    void shouldPassFilteringParametersToAuditReadPort() {
        LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 11, 7, 23, 59);

        GetKiemToanNhatKyQuery query = new GetKiemToanNhatKyQuery(
                "ADMIN",
                "tiennv",
                "ROOM",
                "ROOM_CREATED",
                from,
                to,
                2,
                25
        );

        handler.handle(query);

        assertEquals("tiennv", auditReadPort.lastActor);
        assertEquals("ROOM", auditReadPort.lastResourceType);
        assertEquals("ROOM_CREATED", auditReadPort.lastActionCode);
        assertEquals(from, auditReadPort.lastFromTime);
        assertEquals(to, auditReadPort.lastToTime);
        assertEquals(2, auditReadPort.lastPage);
        assertEquals(25, auditReadPort.lastSize);
    }

    @Test
    @DisplayName("Xử lý ngoại lệ từ AuditReadPort an toàn, không làm đổ vỡ ứng dụng")
    void shouldHandleAuditPortExceptionGracefully() {
        KiemToanReadCongKetNoi faultyPort = (actor, res, act, from, to, page, size) -> {
            throw new RuntimeException("Cơ sở dữ liệu tạm thời gián đoạn");
        };

        GetKiemToanNhatKyHandler faultyHandler = new GetKiemToanNhatKyHandler(faultyPort);
        GetKiemToanNhatKyQuery query = new GetKiemToanNhatKyQuery("ADMIN");

        KetQua<List<KiemToanNhatKyDuLieu>> result = faultyHandler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("AUDIT_QUERY_FAILED", result.getError().orElseThrow().code());
        assertTrue(result.getError().orElseThrow().message().contains("Cơ sở dữ liệu tạm thời gián đoạn"));
    }

    @Test
    @DisplayName("Từ chối khi truy vấn null")
    void shouldFailWhenQueryIsNull() {
        KetQua<List<KiemToanNhatKyDuLieu>> result = handler.handle(null);

        assertTrue(result.isFailure());
        assertEquals("AUDIT_QUERY_INVALID", result.getError().orElseThrow().code());
    }

    // In-memory Mock Port cho kiểm thử độc lập 100% không chạm DB
    private static class InMemoryAuditReadPort implements KiemToanReadCongKetNoi {
        final List<KiemToanNhatKyDuLieu> logs = new ArrayList<>();

        String lastActor;
        String lastResourceType;
        String lastActionCode;
        LocalDateTime lastFromTime;
        LocalDateTime lastToTime;
        int lastPage;
        int lastSize;

        @Override
        public List<KiemToanNhatKyDuLieu> findAuditLogs(String actor, String resourceType, String actionCode,
                                              LocalDateTime fromTime, LocalDateTime toTime,
                                              int page, int size) {
            this.lastActor = actor;
            this.lastResourceType = resourceType;
            this.lastActionCode = actionCode;
            this.lastFromTime = fromTime;
            this.lastToTime = toTime;
            this.lastPage = page;
            this.lastSize = size;
            return new ArrayList<>(logs);
        }
    }
}
