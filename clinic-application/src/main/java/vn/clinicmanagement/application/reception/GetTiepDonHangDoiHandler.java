package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenQuyetDinh;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.common.UngDungThatBaiChanDoanLoi;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;

import java.time.LocalDate;

/** Authorized queue and room-summary read. */
public final class GetTiepDonHangDoiHandler implements QueryHandler<GetTiepDonHangDoiQuery, TiepDonHangDoiAnhChup> {
    /** Trường `receptionQueue`: tiếp đón hangdoi. */
    private final TiepDonHangDoiCongKetNoi receptionQueue;
    /** Trường `authorization`: phanquyen. */
    private final PhanQuyenDichVu authorization;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public GetTiepDonHangDoiHandler(TiepDonHangDoiCongKetNoi receptionQueue, PhanQuyenDichVu authorization,
                                    KiemToanWriteCongKetNoi audit) {
        this.receptionQueue = receptionQueue;
        this.authorization = authorization;
        this.audit = audit;
    }

    /** Legacy wiring fails closed; protected results require an authorization service and audit port. */
    @Deprecated
    public GetTiepDonHangDoiHandler(TiepDonHangDoiCongKetNoi receptionQueue) {
        this(receptionQueue, null, null);
    }

    @Override
    public KetQua<TiepDonHangDoiAnhChup> handle(GetTiepDonHangDoiQuery query) {
        if (query == null) return KetQua.failure("RECEPTION_QUEUE_FAILED", "Không thể tải hàng đợi tiếp đón.");
        PhanQuyenQuyetDinh decision = TiepDonReadKiemToan.evaluate(query.authorizationContext(),
                query.auditActorContext(), authorization, "RECEPTION");
        if (!decision.allowed()) {
            TiepDonReadKiemToan.denied(audit, query.auditActorContext(), decision);
            return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép xem hàng đợi trong phiên hiện tại.");
        }
        try {
            LocalDate date = query.date() == null ? LocalDate.now() : query.date();
            int facilityId = query.authorizationContext().facilityId();
            int departmentId = query.authorizationContext().departmentId();
            TiepDonHangDoiAnhChup snapshot = new TiepDonHangDoiAnhChup(
                    receptionQueue.getQueue(date, facilityId, departmentId),
                    receptionQueue.getRoomSummaries(date, facilityId, departmentId));
            TiepDonReadKiemToan.success(audit, query.auditActorContext(), KiemToanHanhDongMa.PATIENT_VIEW);
            return KetQua.success(snapshot);
        } catch (RuntimeException ex) {
            UngDungThatBaiChanDoanLoi.logUnexpectedFailure("RECEPTION_GET_QUEUE", "RECEPTION_QUEUE_FAILED",
                    correlationId(query), ex);
            return KetQua.failure("RECEPTION_QUEUE_FAILED", "Không thể tải hàng đợi tiếp đón.");
        }
    }

    /** Hàm `correlationId`: `correlation` mã. */
    private static String correlationId(GetTiepDonHangDoiQuery query) {
        return query.auditActorContext() == null || query.auditActorContext().correlationId() == null
                ? null : query.auditActorContext().correlationId().toString();
    }
}
