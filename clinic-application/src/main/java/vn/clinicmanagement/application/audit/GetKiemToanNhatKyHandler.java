package vn.clinicmanagement.application.audit;

import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;

import java.util.List;
import java.util.Objects;

/**
 * Xử lý truy vấn tra cứu nhật ký kiểm toán (AUD-01).
 * Giới hạn thẩm quyền nghiêm ngặt: Chỉ ADMIN hoặc AUDITOR.
 * Tự động thực thi Secret Masking trên toàn bộ nhật ký trả về.
 */
public final class GetKiemToanNhatKyHandler implements QueryHandler<GetKiemToanNhatKyQuery, List<KiemToanNhatKyDuLieu>> {
    /** Trường `auditReadPort`: kiểm toán đọc congketnoi. */
    private final KiemToanReadCongKetNoi auditReadPort;

    public GetKiemToanNhatKyHandler(KiemToanReadCongKetNoi auditReadPort) {
        this.auditReadPort = Objects.requireNonNull(auditReadPort, "auditReadPort");
    }

    @Override
    public KetQua<List<KiemToanNhatKyDuLieu>> handle(GetKiemToanNhatKyQuery query) {
        if (query == null) {
            return KetQua.failure("AUDIT_QUERY_INVALID", "Truy vấn tra cứu nhật ký kiểm toán không được để trống.");
        }

        // 1. Kiểm tra thẩm quyền (Chỉ ADMIN hoặc AUDITOR)
        String role = query.getCurrentUserRole();
        if (role == null || role.isBlank() ||
                (!role.trim().equalsIgnoreCase("ADMIN") && !role.trim().equalsIgnoreCase("AUDITOR"))) {
            return KetQua.failure("AUTHZ_DENIED",
                    "Chỉ Quản trị viên (ADMIN) hoặc Kiểm toán viên (AUDITOR) mới có quyền tra cứu nhật ký kiểm toán.");
        }

        // 2. Tra cứu dữ liệu từ cổng AuditReadPort
        try {
            List<KiemToanNhatKyDuLieu> rawLogs = auditReadPort.findAuditLogs(
                    query.getActor(),
                    query.getResourceType(),
                    query.getActionCode(),
                    query.getFromTime(),
                    query.getToTime(),
                    query.getPage(),
                    query.getPageSize()
            );

            if (rawLogs == null) {
                return KetQua.success(List.of());
            }

            // 3. Secret Masking: Che giấu tuyệt đối mật khẩu, chuỗi bí mật, token
            List<KiemToanNhatKyDuLieu> maskedLogs = rawLogs.stream()
                    .filter(Objects::nonNull)
                    .map(KiemToanNhatKyDuLieu::withMaskedSecrets)
                    .toList();

            return KetQua.success(maskedLogs);
        } catch (Exception ex) {
            return KetQua.failure("AUDIT_QUERY_FAILED",
                    "Không thể đọc dữ liệu nhật ký kiểm toán: " + ex.getMessage());
        }
    }
}
