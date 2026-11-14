package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenQuyetDinh;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.common.UngDungThatBaiChanDoanLoi;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Authorized, audited read of the active doctor team queue. */
public final class GetBacSiHangDoiHandler implements QueryHandler<GetBacSiHangDoiQuery, List<TiepDonHangDoiMatHang>> {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Trường `doctorQueue`: bacsi hangdoi. */
    private final BacSiHangDoiCongKetNoi doctorQueue;
    /** Trường `authorization`: phanquyen. */
    private final PhanQuyenDichVu authorization;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public GetBacSiHangDoiHandler(BacSiHangDoiCongKetNoi doctorQueue, PhanQuyenDichVu authorization,
                                 KiemToanWriteCongKetNoi audit) {
        this.doctorQueue = doctorQueue;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Override
    public KetQua<List<TiepDonHangDoiMatHang>> handle(GetBacSiHangDoiQuery query) {
        if (query == null) return KetQua.failure("RECEPTION_QUEUE_FAILED", "Không thể tải hàng đợi bác sĩ.");
        PhanQuyenQuyetDinh decision = TiepDonReadKiemToan.evaluate(query.authorizationContext(),
                query.auditActorContext(), authorization, "RECEPTION");
        if (!decision.allowed()) {
            TiepDonReadKiemToan.denied(audit, query.auditActorContext(), decision);
            return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép xem hàng đợi trong phiên hiện tại.");
        }
        try {
            LocalDate date = query.date() == null ? LocalDate.now(BUSINESS_ZONE) : query.date();
            int facilityId = query.authorizationContext().facilityId();
            int departmentId = query.authorizationContext().departmentId();
            List<TiepDonHangDoiMatHang> items = doctorQueue.getActiveDoctorQueue(date, facilityId, departmentId);
            TiepDonReadKiemToan.success(audit, query.auditActorContext(), KiemToanHanhDongMa.PATIENT_VIEW);
            return KetQua.success(items);
        } catch (RuntimeException ex) {
            UngDungThatBaiChanDoanLoi.logUnexpectedFailure("DOCTOR_GET_QUEUE", "RECEPTION_QUEUE_FAILED",
                    correlationId(query), ex);
            return KetQua.failure("RECEPTION_QUEUE_FAILED", "Không thể tải hàng đợi bác sĩ.");
        }
    }

    /** Hàm `correlationId`: `correlation` mã. */
    private static String correlationId(GetBacSiHangDoiQuery query) {
        return query.auditActorContext() == null || query.auditActorContext().correlationId() == null
                ? null : query.auditActorContext().correlationId().toString();
    }
}
