package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.appointment.LichHen;
import vn.clinicmanagement.domain.repositories.ILichHenRepository;

import java.time.Instant;

public final class CancelLichHenHandler
        implements CommandHandler<CancelLichHenCommand, CancelLichHenBienNhan> {
    /** Trường `appointments`: lịch hẹn. */
    private final ILichHenRepository appointments;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public CancelLichHenHandler(ILichHenRepository appointments, GiaoDichRanhGioi transactions,
                                    KiemToanWriteCongKetNoi audit) {
        this.appointments = appointments;
        this.transactions = transactions;
        this.audit = audit;
    }

    @Override
    public KetQua<CancelLichHenBienNhan> handle(CancelLichHenCommand command) {
        if (command == null || command.appointmentId() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin hủy lịch hẹn không hợp lệ.");
        }
        if (command.cancellationReason() == null || command.cancellationReason().isBlank()) {
            return KetQua.failure("VALIDATION_ERROR", "Lý do hủy không được để trống.");
        }

        PhanQuyenNguCanh context = command.authorizationContext();
        KiemToanTacNhanNguCanh actor = command.auditActorContext();
        if (!isAuthorized(context)) {
            return deny(context, actor);
        }

        return transactions.inTransaction(() -> {
            var appointmentResult = appointments.getById(command.appointmentId());
            if (appointmentResult.isEmpty()) {
                return KetQua.failure("NOT_FOUND", "Không tìm thấy lịch hẹn.");
            }

            LichHen appointment = appointmentResult.get();
            try {
                appointment.huyLichHen(command.cancellationReason());
            } catch (IllegalStateException ex) {
                return KetQua.failure("INVALID_STATE", ex.getMessage());
            }

            appointments.update(appointment);
            appendSuccessAudit(actor, context, appointment);
            return KetQua.success(new CancelLichHenBienNhan(appointment.getId()));
        });
    }

    /** Hàm `isAuthorized`: là `Authorized`. */
    private static boolean isAuthorized(PhanQuyenNguCanh context) {
        return context != null && context.isValid()
                && ("RECEPTIONIST".equals(context.roleCode()) || "ADMIN".equals(context.roleCode()));
    }

    private KetQua<CancelLichHenBienNhan> deny(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        if (context != null && context.isValid()) {
            audit.appendIndependently(new HeThongKiemToanNhapLieu(
                    context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                    context.departmentId(), KiemToanHanhDongMa.AUTH_ACCESS_DENIED, null, null,
                    KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN, Instant.now(),
                    actor == null ? null : actor.workstationId(),
                    actor == null ? context.sessionId() : actor.correlationId()));
        } else if (actor != null) {
            audit.appendIndependently(new HeThongKiemToanNhapLieu(
                    actor.employeeId(), actor.roleId(), actor.roleCode(), actor.facilityId(),
                    actor.departmentId(), KiemToanHanhDongMa.AUTH_ACCESS_DENIED, null, null,
                    KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN, Instant.now(),
                    actor.workstationId(), actor.correlationId()));
        }
        return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép hủy lịch hẹn trong phiên hiện tại.");
    }

    /** Hàm `appendSuccessAudit`: thêm vào thành công kiểm toán. */
    private void appendSuccessAudit(KiemToanTacNhanNguCanh actor, PhanQuyenNguCanh context, LichHen appointment) {
        audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(
                context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                context.departmentId(), KiemToanHanhDongMa.VISIT_REGISTERED, KiemToanTaiNguyenLoai.PATIENT,
                appointment.getMaBenhNhan(), KiemToanKetQua.SUCCESS, null, Instant.now(),
                actor == null ? null : actor.workstationId(),
                actor == null ? context.sessionId() : actor.correlationId()));
    }
}
