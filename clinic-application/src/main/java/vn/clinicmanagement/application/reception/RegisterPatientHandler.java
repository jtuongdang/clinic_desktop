package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.authorization.AuthorizationDecision;
import vn.clinicmanagement.application.authorization.AuthorizationRequest;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.application.common.ApplicationFailureDiagnostics;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.patient.PatientIdentityConflictException;
import vn.clinicmanagement.domain.repositories.IBaoHiemRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;

import java.time.Instant;
import java.time.LocalDate;

public final class RegisterPatientHandler implements CommandHandler<RegisterPatientCommand, RegistrationReceipt> {
    private final IBenhNhanRepository patients;
    private final IBaoHiemRepository insuranceCards;
    private final IPhongKhamRepository rooms;
    private final ReceptionQueuePort receptionQueue;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;
    private final AuthorizationService authorization;

    public RegisterPatientHandler(IBenhNhanRepository patients, IBaoHiemRepository insuranceCards,
                                  IPhongKhamRepository rooms, ReceptionQueuePort receptionQueue,
                                  TransactionBoundary transactions, AuditWritePort audit,
                                  AuthorizationService authorization) {
        this.patients = patients;
        this.insuranceCards = insuranceCards;
        this.rooms = rooms;
        this.receptionQueue = receptionQueue;
        this.transactions = transactions;
        this.audit = audit;
        this.authorization = authorization;
    }

    @Override
    public Result<RegistrationReceipt> handle(RegisterPatientCommand command) {
        String validationError = validate(command);
        if (validationError != null) return Result.failure("PATIENT_VALIDATION", validationError);

        AuthorizationContext context = command.authorizationContext();
        boolean existing = command.patient().getId() != null && command.patient().getId() > 0;
        HanhDongQuyen patientAction = existing ? HanhDongQuyen.SUA : HanhDongQuyen.TAO;
        if (!isAuthorized(context, "PATIENT", patientAction)
                || !isAuthorized(context, "RECEPTION", HanhDongQuyen.TAO)) {
            return deny(context, command.auditActorContext(), AuditReasonCode.AUTH_FORBIDDEN);
        }

        return transactions.inTransaction(() -> {
            try {
                BenhNhan patient = command.patient();
                PhongKham room = rooms.getById(command.roomId()).orElse(null);
                if (room == null || !room.isHoatDong() || room.getKhoa() == null || room.getKhoa().getId() == null
                        || room.getKhoa().getCoSo() == null || room.getKhoa().getCoSo().getId() == null) {
                    return Result.failure("ROOM_UNAVAILABLE", "Phòng khám không hoạt động hoặc chưa gắn với cơ sở/khoa.");
                }
                if (room.getKhoa().getCoSo().getId() != context.facilityId()
                        || room.getKhoa().getId() != context.departmentId()
                        || room.getMaKhoa() != context.departmentId()) {
                    return deny(context, command.auditActorContext(), AuditReasonCode.SCOPE_MISMATCH);
                }

                var scopedPatient = existing
                        ? patients.getByIdInScope(patient.getId(), context.facilityId(), context.departmentId())
                        : java.util.Optional.<BenhNhan>empty();
                if (existing) {
                    if (scopedPatient.isEmpty()) {
                        return deny(context, command.auditActorContext(), AuditReasonCode.SCOPE_MISMATCH);
                    }
                    BenhNhan persistedPatient = scopedPatient.get();
                    patient.setVersion(persistedPatient.getVersion());
                    patient.setCreatedAt(persistedPatient.getCreatedAt());
                    patient.setCreatedBy(persistedPatient.getCreatedBy());
                    patient.setUpdatedBy(command.createdBy());
                }

                if (existing) patients.update(patient);
                else patients.add(patient);
                appendAudit(command.auditActorContext(), context,
                        existing ? AuditActionCode.PATIENT_UPDATED : AuditActionCode.PATIENT_CREATED,
                        AuditResourceType.PATIENT, patient.getId());

                if (hasInsuranceNumber(command.insurance())) {
                    BaoHiem card = command.insurance();
                    card.setMaBenhNhan(patient.getId());
                    var currentCard = insuranceCards.getLatestByPatientId(patient.getId());
                    if (currentCard.isPresent()) {
                        BaoHiem persistedCard = currentCard.get();
                        card.setId(persistedCard.getId());
                        card.setVersion(persistedCard.getVersion());
                        card.setCreatedAt(persistedCard.getCreatedAt());
                        card.setCreatedBy(persistedCard.getCreatedBy());
                        card.setUpdatedBy(command.createdBy());
                        insuranceCards.update(card);
                        appendAudit(command.auditActorContext(), context, AuditActionCode.INSURANCE_UPDATED,
                                AuditResourceType.INSURANCE, card.getId());
                    } else {
                        insuranceCards.add(card);
                        appendAudit(command.auditActorContext(), context, AuditActionCode.INSURANCE_CREATED,
                                AuditResourceType.INSURANCE, card.getId());
                    }
                }

                var visit = receptionQueue.registerVisit(patient.getId(), context.facilityId(), context.departmentId(),
                        command.roomId(), command.registrationType(), command.reason(), command.payerType(),
                        command.priority(), command.createdBy());
                appendAudit(command.auditActorContext(), context, AuditActionCode.VISIT_REGISTERED,
                        AuditResourceType.VISIT, visit.getId());

                return Result.success(new RegistrationReceipt(patient.getId(), patient.getMaBenhNhanFormat(),
                        visit.getId(), visit.getSoThuTu() == null ? 0 : visit.getSoThuTu(), visit.getSoThuTuDisplay()));
            } catch (PatientIdentityConflictException ex) {
                return registrationFailure();
            } catch (RuntimeException ex) {
                ApplicationFailureDiagnostics.logUnexpectedFailure("RECEPTION_REGISTER_PATIENT",
                        "PATIENT_REGISTRATION_FAILED", command.auditActorContext().correlationId().toString(), ex);
                return Result.failure("PATIENT_REGISTRATION_FAILED", "Không thể lưu đăng ký. Dữ liệu chưa được ghi; hãy thử lại.");
            }
        });
    }

    private boolean isAuthorized(AuthorizationContext context, String module, HanhDongQuyen action) {
        AuthorizationDecision decision = authorization.evaluate(new AuthorizationRequest(
                context, module, action, context.facilityId(), context.departmentId()));
        return decision.allowed();
    }

    private Result<RegistrationReceipt> deny(AuthorizationContext context,
                                             vn.clinicmanagement.application.audit.AuditActorContext auditActor,
                                             AuditReasonCode reasonCode) {
        try {
            audit.appendIndependently(new SystemAuditEntry(
                    context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                    context.departmentId(), AuditActionCode.AUTH_ACCESS_DENIED, null, null,
                    AuditOutcome.DENIED, reasonCode, Instant.now(),
                    auditActor.workstationId(), auditActor.correlationId()));
        } catch (RuntimeException ignored) {
            // A denial remains a denial even when its independent audit write is unavailable.
        }
        String code = reasonCode == AuditReasonCode.SCOPE_MISMATCH
                ? "AUTH_SCOPE_MISMATCH" : "AUTHORIZATION_DENIED";
        return Result.failure(code, "Không được phép thực hiện đăng ký trong phiên hiện tại.");
    }

    private static Result<RegistrationReceipt> registrationFailure() {
        return Result.failure("PATIENT_REGISTRATION_FAILED",
                "Không thể hoàn tất đăng ký người bệnh. Vui lòng kiểm tra thông tin và thử lại.");
    }

    private void appendAudit(vn.clinicmanagement.application.audit.AuditActorContext auditActor,
                             AuthorizationContext actor, AuditActionCode actionCode,
                             AuditResourceType resourceType, Integer resourceId) {
        audit.appendInCurrentTransaction(new SystemAuditEntry(
                actor.employeeId(), actor.roleId(), actor.roleCode(), actor.facilityId(), actor.departmentId(),
                actionCode, resourceType, resourceId, AuditOutcome.SUCCESS, null, Instant.now(),
                auditActor.workstationId(), auditActor.correlationId()));
    }

    private static String validate(RegisterPatientCommand command) {
        if (command == null || command.patient() == null) return "Thiếu dữ liệu người bệnh.";
        BenhNhan patient = command.patient();
        if (patient.getHo() == null || patient.getHo().isBlank() || patient.getTen() == null || patient.getTen().isBlank())
            return "Vui lòng nhập họ và tên người bệnh.";
        if (patient.getNgaySinh() == null || patient.getNgaySinh().isAfter(LocalDate.now()))
            return "Ngày sinh không hợp lệ.";
        if (patient.getGioiTinh() == null) return "Vui lòng chọn giới tính.";
        if (command.roomId() <= 0) return "Vui lòng chọn phòng khám.";
        if (command.registrationType() == null || command.registrationType().isBlank()) return "Vui lòng chọn loại đăng ký khám.";
        if (command.payerType() == null) return "Vui lòng chọn đối tượng khám.";
        if (hasInsuranceNumber(command.insurance())) {
            BaoHiem card = command.insurance();
            if (card.getNgayHetHan() == null) return "Vui lòng nhập hạn thẻ BHYT.";
            if (card.getMucHuong() < 0 || card.getMucHuong() > 100) return "Mức hưởng BHYT phải từ 0 đến 100%.";
            if (card.getNgayBatDau() != null && card.getNgayBatDau().isAfter(card.getNgayHetHan()))
                return "Ngày bắt đầu thẻ BHYT phải trước hoặc bằng ngày hết hạn.";
        } else if (command.payerType() == DoiTuongKham.BHYT) {
            return "Đối tượng BHYT cần có số thẻ và hạn thẻ.";
        }
        return null;
    }

    private static boolean hasInsuranceNumber(BaoHiem insurance) {
        return insurance != null && insurance.getSoBaoHiem() != null && !insurance.getSoBaoHiem().isBlank();
    }
}
