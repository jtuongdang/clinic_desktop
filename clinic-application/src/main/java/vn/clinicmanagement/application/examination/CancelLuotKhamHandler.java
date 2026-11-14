package vn.clinicmanagement.application.examination;

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
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.time.Instant;

public final class CancelLuotKhamHandler implements CommandHandler<CancelLuotKhamCommand, CancelLuotKhamBienNhan> {
    private final ILuotKhamRepository visits;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public CancelLuotKhamHandler(ILuotKhamRepository visits, GiaoDichRanhGioi transactions, KiemToanWriteCongKetNoi audit) {
        this.visits = visits;
        this.transactions = transactions;
        this.audit = audit;
    }

    @Override
    public KetQua<CancelLuotKhamBienNhan> handle(CancelLuotKhamCommand command) {
        if (command == null) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin hủy lượt khám không hợp lệ.");
        }

        PhanQuyenNguCanh context = command.authorizationContext();
        if (context == null || !"RECEPTIONIST".equals(context.roleCode())) {
            return deny(context, command.auditActorContext());
        }
        if (command.cancellationReason() == null || command.cancellationReason().isBlank()) {
            return KetQua.failure("VALIDATION_ERROR", "Lý do hủy không được để trống.");
        }
        if (command.visitId() <= 0) {
            return KetQua.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
        }

        return transactions.inTransaction(() -> {
            var visitResult = visits.getById(command.visitId());
            if (visitResult.isEmpty()) {
                return KetQua.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
            }

            LuotKham visit = visitResult.get();
            if (visit.getTrangThai() != TrangThaiLuotKham.CHO_KHAM) {
                return KetQua.failure("INVALID_STATE", "Chỉ có thể hủy lượt khám ở trạng thái chờ khám.");
            }

            visit.setTrangThai(TrangThaiLuotKham.DA_HUY);
            visits.update(visit);
            appendSuccessAudit(command.auditActorContext(), context, command.visitId());
            return KetQua.success(new CancelLuotKhamBienNhan(command.visitId()));
        });
    }

    private KetQua<CancelLuotKhamBienNhan> deny(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        if (context != null && actor != null) {
            audit.appendIndependently(new HeThongKiemToanNhapLieu(
                    context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                    context.departmentId(), KiemToanHanhDongMa.AUTH_ACCESS_DENIED, null, null,
                    KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN, Instant.now(),
                    actor.workstationId(), actor.correlationId()));
        }
        return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép hủy lượt khám trong phiên hiện tại.");
    }

    /** Hàm `appendSuccessAudit`: thêm vào thành công kiểm toán. */
    private void appendSuccessAudit(KiemToanTacNhanNguCanh actor, PhanQuyenNguCanh context, int visitId) {
        audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(
                context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                context.departmentId(), KiemToanHanhDongMa.VISIT_TRANSFERRED, KiemToanTaiNguyenLoai.VISIT, visitId,
                KiemToanKetQua.SUCCESS, null, Instant.now(), actor.workstationId(), actor.correlationId()));
    }
}
