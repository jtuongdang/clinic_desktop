package vn.clinicmanagement.application.reception.get;

import vn.clinicmanagement.application.reception.IPhieuDangKyKhamRepository;
import vn.clinicmanagement.application.reception.PhieuDangKyKhamDuLieu;
import vn.clinicmanagement.application.reception.TiepDonReadKiemToan;

import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.*;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.audit.*;

/** Kiểm tra quyền trước khi truy vấn và ghi kiểm toán khi đọc phiếu. */
public final class GetPhieuDangKyKhamHandler implements QueryHandler<GetPhieuDangKyKhamQuery, PhieuDangKyKhamDuLieu> {
    private final IPhieuDangKyKhamRepository repository;
    private final PhanQuyenDichVu authorization;
    private final KiemToanWriteCongKetNoi audit;
    public GetPhieuDangKyKhamHandler(IPhieuDangKyKhamRepository repository, PhanQuyenDichVu authorization,
                                    KiemToanWriteCongKetNoi audit) {
        this.repository = repository; this.authorization = authorization; this.audit = audit;
    }
    @Override public KetQua<PhieuDangKyKhamDuLieu> handle(GetPhieuDangKyKhamQuery query) {
        if (query == null || query.visitId() <= 0) return KetQua.failure("PRINT_VISIT_REQUIRED", "Vui lòng lưu hoặc chọn lượt đăng ký trước khi in phiếu.");
        var decision = TiepDonReadKiemToan.evaluate(query.authorizationContext(), query.auditActorContext(), authorization, "RECEPTION");
        if (decision.allowed()) {
            var action = query.action();
            if (action != vn.clinicmanagement.domain.common.enums.HanhDongQuyen.XEM
                    && action != vn.clinicmanagement.domain.common.enums.HanhDongQuyen.IN
                    && action != vn.clinicmanagement.domain.common.enums.HanhDongQuyen.XUAT_FILE)
                return KetQua.failure("PRINT_ACTION_INVALID", "Thao tác phiếu không hợp lệ.");
            if (action != vn.clinicmanagement.domain.common.enums.HanhDongQuyen.XEM)
                decision = authorization.evaluate(new vn.clinicmanagement.application.authorization.PhanQuyenYeuCau(
                        query.authorizationContext(), "RECEPTION", action, query.authorizationContext().facilityId(), query.authorizationContext().departmentId()));
        }
        if (!decision.allowed()) {
            TiepDonReadKiemToan.denied(audit, query.auditActorContext(), decision);
            return KetQua.failure("AUTHORIZATION_DENIED", "Không có quyền xem phiếu trong phiên hiện tại.");
        }
        try {
            var result = repository.getByVisit(query.visitId(), query.authorizationContext().facilityId(), query.authorizationContext().departmentId());
            if (result.isEmpty()) return KetQua.failure("PRINT_VISIT_NOT_FOUND", "Không tìm thấy lượt khám trong phạm vi được phép.");
            TiepDonReadKiemToan.success(audit, query.auditActorContext(), KiemToanHanhDongMa.PATIENT_VIEW);
            return KetQua.success(result.get());
        } catch (RuntimeException error) {
            UngDungThatBaiChanDoanLoi.logUnexpectedFailure("RECEPTION_PRINT_PREVIEW", "PRINT_LOAD_FAILED", query.auditActorContext().correlationId().toString(), error);
            return KetQua.failure("PRINT_LOAD_FAILED", "Không thể tải phiếu từ cơ sở dữ liệu. Vui lòng thử lại.");
        }
    }
}
