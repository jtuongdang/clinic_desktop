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
import vn.clinicmanagement.domain.common.enums.TrangThaiLichHen;
import vn.clinicmanagement.domain.repositories.ILichHenRepository;

import java.time.Instant;

public final class BookLichHenHandler implements CommandHandler<BookLichHenCommand, LichHenBienNhan> {
    /** Trường `appointments`: lịch hẹn. */
    private final ILichHenRepository appointments;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public BookLichHenHandler(ILichHenRepository appointments, GiaoDichRanhGioi transactions,
                                  KiemToanWriteCongKetNoi audit) {
        this.appointments = appointments;
        this.transactions = transactions;
        this.audit = audit;
    }

    @Override
    public KetQua<LichHenBienNhan> handle(BookLichHenCommand command) {
        if (command == null || command.patientId() <= 0 || command.doctorId() <= 0 || command.facilityId() <= 0
                || command.startTime() == null || command.endTime() == null) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin đặt lịch không hợp lệ.");
        }
        if (!command.startTime().isBefore(command.endTime())) {
            return KetQua.failure("VALIDATION_ERROR", "Khung giờ hẹn không hợp lệ.");
        }

        PhanQuyenNguCanh context = command.authorizationContext();
        KiemToanTacNhanNguCanh actor = command.auditActorContext();
        if (!isAuthorized(context, actor, command.facilityId())) {
            return deny(context, actor);
        }

        return transactions.inTransaction(() -> {
            if (appointments.countAppointmentsInSlot(command.doctorId(), command.startTime(), command.endTime()) > 0) {
                return KetQua.failure("SLOT_UNAVAILABLE",
                        "Bác sĩ đã có lịch hẹn trong khung giờ này. Không được phép đặt trùng.");
            }

            LichHen appointment = new LichHen(command.patientId(), command.doctorId(), command.facilityId(),
                    command.departmentId(), command.startTime(), command.endTime(), command.reason(),
                    command.bookingSource(), context.employeeId());
            appointment.setTrangThai(TrangThaiLichHen.CHO_XAC_NHAN);
            appointments.add(appointment);
            appendSuccessAudit(actor, context, command.patientId());
            return KetQua.success(new LichHenBienNhan(appointment.getId(), appointment.getTrangThai()));
        });
    }

    /** Hàm `isAuthorized`: là `Authorized`. */
    private static boolean isAuthorized(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor, int facilityId) {
        return context != null && context.isValid()
                && (context.roleCode().equals("RECEPTIONIST") || context.roleCode().equals("ADMIN"))
                && context.facilityId() == facilityId
                && actor != null
                && actor.employeeId() == context.employeeId()
                && actor.roleId() == context.roleId()
                && actor.roleCode().equals(context.roleCode())
                && actor.facilityId() == context.facilityId()
                && actor.departmentId() == context.departmentId();
    }

    private KetQua<LichHenBienNhan> deny(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        if (context != null && context.isValid() && actor != null) {
            audit.appendIndependently(new HeThongKiemToanNhapLieu(
                    context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                    context.departmentId(), KiemToanHanhDongMa.AUTH_ACCESS_DENIED, null, null,
                    KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN, Instant.now(),
                    actor.workstationId(), actor.correlationId()));
        }
        return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép đặt lịch trong phiên hiện tại.");
    }

    /** Hàm `appendSuccessAudit`: thêm vào thành công kiểm toán. */
    private void appendSuccessAudit(KiemToanTacNhanNguCanh actor, PhanQuyenNguCanh context, int patientId) {
        audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(
                context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                context.departmentId(), KiemToanHanhDongMa.VISIT_REGISTERED, KiemToanTaiNguyenLoai.PATIENT, patientId,
                KiemToanKetQua.SUCCESS, null, Instant.now(), actor.workstationId(), actor.correlationId()));
    }
}
