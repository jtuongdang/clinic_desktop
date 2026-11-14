package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GetBenhNhanCongNoHandler implements QueryHandler<GetBenhNhanCongNoQuery, List<BenhNhanCongNoDuLieu>> {

    private static final Set<String> ALLOWED_ROLES = Set.of("CASHIER", "ADMIN", "RECEPTIONIST");

    /** Trường `invoiceRepository`: hóa đơn kho dữ liệu. */
    private final IHoaDonRepository invoiceRepository;
    /** Trường `auditWritePort`: kiểm toán ghi congketnoi. */
    private final KiemToanWriteCongKetNoi auditWritePort;

    public GetBenhNhanCongNoHandler(IHoaDonRepository invoiceRepository, KiemToanWriteCongKetNoi auditWritePort) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public KetQua<List<BenhNhanCongNoDuLieu>> handle(GetBenhNhanCongNoQuery query) {
        if (query == null) {
            return KetQua.failure("VALIDATION_ERROR", "Truy vấn công nợ không được để trống.");
        }

        // 1. Phân quyền
        PhanQuyenNguCanh auth = query.authorizationContext();
        if (auth == null || !isRoleAllowed(auth.roleCode())) {
            logAudit(query.auditActorContext(), query.patientId(), KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN);
            return KetQua.failure("AUTHORIZATION_DENIED", "Tài khoản không có quyền tra cứu công nợ bệnh nhân.");
        }

        if (query.patientId() <= 0) {
            return KetQua.failure("INVALID_PATIENT", "Mã bệnh nhân không hợp lệ.");
        }

        // 2. Tra cứu danh sách công nợ
        List<CongNoBenhNhan> debts = invoiceRepository.getCongNoByBenhNhan(query.patientId());
        List<BenhNhanCongNoDuLieu> results = new ArrayList<>();

        if (debts != null) {
            for (CongNoBenhNhan cn : debts) {
                String invoiceNumber = "";
                if (cn.getMaHoaDon() > 0) {
                    Optional<HoaDon> inv = invoiceRepository.getById(cn.getMaHoaDon());
                    if (inv.isPresent()) {
                        invoiceNumber = inv.get().getSoHoaDon();
                    }
                }
                BigDecimal remaining = cn.getSoTienConLai();
                if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                    remaining = BigDecimal.ZERO;
                }
                results.add(new BenhNhanCongNoDuLieu(
                        cn.getId() != null ? cn.getId() : 0,
                        cn.getMaHoaDon(),
                        invoiceNumber,
                        cn.getMaBenhNhan(),
                        cn.getSoTienNo(),
                        cn.getSoTienDaTra(),
                        remaining,
                        cn.getHanThanhToan(),
                        cn.getTrangThai()
                ));
            }
        }

        logAudit(query.auditActorContext(), query.patientId(), KiemToanKetQua.SUCCESS, null);
        return KetQua.success(results);
    }

    /** Hàm `isRoleAllowed`: là vai trò `Allowed`. */
    private boolean isRoleAllowed(String roleCode) {
        if (roleCode == null) return false;
        return ALLOWED_ROLES.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }

    /** Hàm `logAudit`: nhatky kiểm toán. */
    private void logAudit(KiemToanTacNhanNguCanh actor, int patientId, KiemToanKetQua outcome, KiemToanLyDoMa reason) {
        if (actor == null) return;
        KiemToanTaiNguyenLoai resourceType = patientId > 0 ? KiemToanTaiNguyenLoai.PATIENT : null;
        Integer resourceId = patientId > 0 ? patientId : null;
        auditWritePort.appendIndependently(new HeThongKiemToanNhapLieu(
                actor.employeeId(),
                actor.roleId(),
                actor.roleCode() != null ? actor.roleCode().trim().toUpperCase(Locale.ROOT) : null,
                actor.facilityId(),
                actor.departmentId(),
                KiemToanHanhDongMa.PATIENT_VIEW,
                resourceType,
                resourceId,
                outcome,
                reason,
                Instant.now(),
                actor.workstationId(),
                actor.correlationId()
        ));
    }
}
