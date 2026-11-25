package vn.clinicmanagement.application.reception.get;

import vn.clinicmanagement.application.reception.TiepDonHangDoiAnhChup;
import vn.clinicmanagement.application.reception.TiepDonHangDoiCongKetNoi;
import vn.clinicmanagement.application.reception.TiepDonReadKiemToan;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenQuyetDinh;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.common.UngDungThatBaiChanDoanLoi;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;

import java.time.LocalDate;

/** Đọc hàng đợi và phần tổng hợp phòng sau khi kiểm tra quyền. */
public final class GetTiepDonHangDoiHandler implements QueryHandler<GetTiepDonHangDoiQuery, TiepDonHangDoiAnhChup> {
    private final TiepDonHangDoiCongKetNoi receptionQueue;
    private final PhanQuyenDichVu authorization;
    private final KiemToanWriteCongKetNoi audit;

    public GetTiepDonHangDoiHandler(TiepDonHangDoiCongKetNoi receptionQueue, PhanQuyenDichVu authorization,
                                    KiemToanWriteCongKetNoi audit) {
        this.receptionQueue = receptionQueue;
        this.authorization = authorization;
        this.audit = audit;
    }

    /** Cách ghép nối cũ từ chối an toàn; để trả dữ liệu được bảo vệ phải có dịch vụ phân quyền và cổng kiểm toán. */
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

    private static String correlationId(GetTiepDonHangDoiQuery query) {
        return query.auditActorContext() == null || query.auditActorContext().correlationId() == null
                ? null : query.auditActorContext().correlationId().toString();
    }
}
