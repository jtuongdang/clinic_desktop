package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.examination.TransferReasonCode;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TransferVisitHandler implements CommandHandler<TransferVisitCommand, TransferVisitReceipt> {
    private final ILuotKhamRepository visitRepository;
    private final IPhongKhamRepository roomRepository;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public TransferVisitHandler(ILuotKhamRepository visitRepository,
                                IPhongKhamRepository roomRepository,
                                TransactionBoundary transactions,
                                AuditWritePort audit) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.roomRepository = Objects.requireNonNull(roomRepository, "roomRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<TransferVisitReceipt> handle(TransferVisitCommand command) {
        if (command == null) {
            return Result.failure("COMMAND_INVALID", "Yêu cầu chuyển phòng không hợp lệ.");
        }

        AuthorizationContext context = command.context() != null ? command.context() : command.authorizationContext();
        TransferReasonCode reasonCode = command.reasonCode();

        if (context == null || !context.isValid() || reasonCode == null || !reasonCode.isEligibleRole(context.roleCode())) {
            return deny(context, command.auditActorContext(), AuditReasonCode.AUTH_FORBIDDEN);
        }

        Optional<LuotKham> visitOpt = visitRepository.getById(command.visitId());
        if (visitOpt.isEmpty()) {
            return Result.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
        }
        LuotKham visit = visitOpt.get();

        if (visit.getTrangThai() != TrangThaiLuotKham.CHO_KHAM && visit.getTrangThai() != TrangThaiLuotKham.DANG_KHAM) {
            return Result.failure("INVALID_STATE", "Lượt khám không ở trạng thái có thể chuyển phòng.");
        }

        Optional<PhongKham> roomOpt = roomRepository.getById(command.destinationRoomId());
        if (roomOpt.isEmpty()) {
            return Result.failure("ROOM_UNAVAILABLE", "Phòng khám đích không hợp lệ hoặc không hoạt động.");
        }
        PhongKham destinationRoom = roomOpt.get();
        if (!destinationRoom.isHoatDong()
                || destinationRoom.getKhoa() == null
                || destinationRoom.getKhoa().getCoSo() == null
                || destinationRoom.getKhoa().getCoSo().getId() == null
                || destinationRoom.getKhoa().getCoSo().getId() != context.facilityId()) {
            return Result.failure("ROOM_UNAVAILABLE", "Phòng khám đích không hợp lệ hoặc không hoạt động.");
        }

        if (visit.getMaPhongKham() != null && visit.getMaPhongKham().intValue() == command.destinationRoomId()) {
            return Result.failure("SAME_ROOM", "Phòng khám đích trùng với phòng khám hiện tại.");
        }

        int previousRoomId = visit.getMaPhongKham() != null ? visit.getMaPhongKham() : 0;

        return transactions.inTransaction(() -> {
            try {
                visit.setMaPhongKham(command.destinationRoomId());
                applyTransferReason(visit, reasonCode, command.transferNote());
                visitRepository.update(visit);

                int departmentId = (destinationRoom.getKhoa() != null
                        && destinationRoom.getKhoa().getId() != null
                        && destinationRoom.getKhoa().getId() > 0)
                        ? destinationRoom.getKhoa().getId()
                        : context.departmentId();

                audit.appendInCurrentTransaction(new SystemAuditEntry(
                        context.employeeId(),
                        context.roleId(),
                        context.roleCode(),
                        context.facilityId(),
                        departmentId,
                        AuditActionCode.VISIT_TRANSFERRED,
                        AuditResourceType.VISIT,
                        command.destinationRoomId(),
                        AuditOutcome.SUCCESS,
                        null,
                        Instant.now(),
                        command.auditActorContext() != null ? command.auditActorContext().workstationId() : null,
                        command.auditActorContext() != null ? command.auditActorContext().correlationId() : UUID.randomUUID()));

                return Result.success(new TransferVisitReceipt(
                        visit.getId() != null ? visit.getId() : command.visitId(),
                        previousRoomId,
                        command.destinationRoomId(),
                        reasonCode,
                        command.transferNote()));
            } catch (RuntimeException ex) {
                return Result.failure("TRANSFER_FAILED", "Không thể hoàn tất chuyển phòng khám: " + ex.getMessage());
            }
        });
    }

    private static void applyTransferReason(LuotKham visit, TransferReasonCode reasonCode, String transferNote) {
        String formattedReason = reasonCode.name();
        if (transferNote != null && !transferNote.isBlank()) {
            formattedReason += ": " + transferNote.trim();
        }
        String currentReason = visit.getLyDoChuyenTuyen();
        if (currentReason == null || currentReason.isBlank()) {
            visit.setLyDoChuyenTuyen(formattedReason);
        } else {
            visit.setLyDoChuyenTuyen(currentReason + "; " + formattedReason);
        }
    }

    private Result<TransferVisitReceipt> deny(AuthorizationContext context,
                                              AuditActorContext auditActor,
                                              AuditReasonCode reasonCode) {
        if (context != null && context.isValid()) {
            try {
                audit.appendIndependently(new SystemAuditEntry(
                        context.employeeId(),
                        context.roleId(),
                        context.roleCode(),
                        context.facilityId(),
                        context.departmentId(),
                        AuditActionCode.AUTH_ACCESS_DENIED,
                        null,
                        null,
                        AuditOutcome.DENIED,
                        reasonCode,
                        Instant.now(),
                        auditActor != null ? auditActor.workstationId() : null,
                        auditActor != null ? auditActor.correlationId() : UUID.randomUUID()));
            } catch (RuntimeException ignored) {
                // Denial stands even if audit write throws
            }
        }
        return Result.failure("AUTHORIZATION_DENIED", "Không được phép thực hiện chuyển phòng khám.");
    }
}
