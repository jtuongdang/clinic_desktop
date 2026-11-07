package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;
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

public final class GetPatientDebtHandler implements QueryHandler<GetPatientDebtQuery, List<PatientDebtDto>> {

    private static final Set<String> ALLOWED_ROLES = Set.of("CASHIER", "ADMIN", "RECEPTIONIST");

    private final IHoaDonRepository invoiceRepository;
    private final AuditWritePort auditWritePort;

    public GetPatientDebtHandler(IHoaDonRepository invoiceRepository, AuditWritePort auditWritePort) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public Result<List<PatientDebtDto>> handle(GetPatientDebtQuery query) {
        if (query == null) {
            return Result.failure("VALIDATION_ERROR", "Truy vấn công nợ không được để trống.");
        }

        // 1. Phân quyền
        AuthorizationContext auth = query.authorizationContext();
        if (auth == null || !isRoleAllowed(auth.roleCode())) {
            logAudit(query.auditActorContext(), query.patientId(), AuditOutcome.DENIED, AuditReasonCode.AUTH_FORBIDDEN);
            return Result.failure("AUTHORIZATION_DENIED", "Tài khoản không có quyền tra cứu công nợ bệnh nhân.");
        }

        if (query.patientId() <= 0) {
            return Result.failure("INVALID_PATIENT", "Mã bệnh nhân không hợp lệ.");
        }

        // 2. Tra cứu danh sách công nợ
        List<CongNoBenhNhan> debts = invoiceRepository.getCongNoByBenhNhan(query.patientId());
        List<PatientDebtDto> results = new ArrayList<>();

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
                results.add(new PatientDebtDto(
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

        logAudit(query.auditActorContext(), query.patientId(), AuditOutcome.SUCCESS, null);
        return Result.success(results);
    }

    private boolean isRoleAllowed(String roleCode) {
        if (roleCode == null) return false;
        return ALLOWED_ROLES.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }

    private void logAudit(AuditActorContext actor, int patientId, AuditOutcome outcome, AuditReasonCode reason) {
        if (actor == null) return;
        AuditResourceType resourceType = patientId > 0 ? AuditResourceType.PATIENT : null;
        Integer resourceId = patientId > 0 ? patientId : null;
        auditWritePort.appendIndependently(new SystemAuditEntry(
                actor.employeeId(),
                actor.roleId(),
                actor.roleCode() != null ? actor.roleCode().trim().toUpperCase(Locale.ROOT) : null,
                actor.facilityId(),
                actor.departmentId(),
                AuditActionCode.PATIENT_VIEW,
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
