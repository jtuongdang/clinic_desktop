package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenQuyetDinh;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.common.UngDungThatBaiChanDoanLoi;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;

import java.util.List;

/** Authorized active-room picker read. */
public final class GetHoatDongTiepDonPhongHandler
        implements QueryHandler<GetHoatDongTiepDonPhongQuery, List<PhongKham>> {
    /** Trường `rooms`: phòng. */
    private final IPhongKhamRepository rooms;
    /** Trường `authorization`: phanquyen. */
    private final PhanQuyenDichVu authorization;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public GetHoatDongTiepDonPhongHandler(IPhongKhamRepository rooms, PhanQuyenDichVu authorization,
                                          KiemToanWriteCongKetNoi audit) {
        this.rooms = rooms;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Override
    public KetQua<List<PhongKham>> handle(GetHoatDongTiepDonPhongQuery query) {
        if (query == null) return KetQua.failure("ROOM_CATALOG_FAILED", "Không thể tải danh sách phòng khám.");
        PhanQuyenQuyetDinh decision = TiepDonReadKiemToan.evaluate(query.authorizationContext(),
                query.auditActorContext(), authorization, "CATALOG");
        if (!decision.allowed()) {
            TiepDonReadKiemToan.denied(audit, query.auditActorContext(), decision);
            return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép xem danh mục phòng trong phiên hiện tại.");
        }
        try {
            List<PhongKham> result = rooms.getActiveRoomsInScope(query.authorizationContext().facilityId(),
                    query.authorizationContext().departmentId());
            TiepDonReadKiemToan.success(audit, query.auditActorContext(), KiemToanHanhDongMa.CATALOG_VIEW);
            return KetQua.success(result);
        } catch (RuntimeException ex) {
            UngDungThatBaiChanDoanLoi.logUnexpectedFailure("RECEPTION_GET_ACTIVE_ROOMS", "ROOM_CATALOG_FAILED",
                    correlationId(query), ex);
            return KetQua.failure("ROOM_CATALOG_FAILED", "Không thể tải danh sách phòng khám.");
        }
    }

    /** Hàm `correlationId`: `correlation` mã. */
    private static String correlationId(GetHoatDongTiepDonPhongQuery query) {
        return query.auditActorContext() == null || query.auditActorContext().correlationId() == null
                ? null : query.auditActorContext().correlationId().toString();
    }
}
