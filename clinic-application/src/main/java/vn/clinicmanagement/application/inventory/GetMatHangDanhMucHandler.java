package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class GetMatHangDanhMucHandler
        implements QueryHandler<GetMatHangDanhMucQuery, List<MatHangTongHopDuLieu>> {
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "PHARMACIST", "DOCTOR", "RECEPTIONIST");

    /** Trường `inventoryRepository`: kho kho dữ liệu. */
    private final IKhoRepository inventoryRepository;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public GetMatHangDanhMucHandler(IKhoRepository inventoryRepository, KiemToanWriteCongKetNoi audit) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<List<MatHangTongHopDuLieu>> handle(GetMatHangDanhMucQuery query) {
        if (query == null) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin tra cứu danh mục không hợp lệ.");
        }

        PhanQuyenNguCanh context = query.authorizationContext();
        if (!isAuthorized(context)) {
            deny(context, query.auditActorContext());
            return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép xem danh mục hàng hóa trong phiên hiện tại.");
        }

        List<MatHangTongHopDuLieu> items = inventoryRepository.searchItems(query.keyword(), query.activeOnly()).stream()
                .map(GetMatHangDanhMucHandler::toSummary)
                .toList();
        appendSuccessAudit(context, query.auditActorContext());
        return KetQua.success(items);
    }

    /** Hàm `isAuthorized`: là `Authorized`. */
    private static boolean isAuthorized(PhanQuyenNguCanh context) {
        return context != null && context.isValid() && ALLOWED_ROLES.contains(context.roleCode());
    }

    private void deny(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        boolean validContext = context != null && context.isValid();
        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                validContext ? context.employeeId() : actor == null ? null : actor.employeeId(),
                validContext ? context.roleId() : actor == null ? null : actor.roleId(),
                validContext ? context.roleCode() : actor == null ? null : actor.roleCode(),
                validContext ? context.facilityId() : actor == null ? null : actor.facilityId(),
                validContext ? context.departmentId() : actor == null ? null : actor.departmentId(),
                KiemToanHanhDongMa.AUTH_ACCESS_DENIED, null, null, KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN,
                Instant.now(), actor == null ? null : actor.workstationId(), correlationId(context, actor)));
    }

    /** Hàm `appendSuccessAudit`: thêm vào thành công kiểm toán. */
    private void appendSuccessAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                context.departmentId(), KiemToanHanhDongMa.CATALOG_VIEW, null, null,
                KiemToanKetQua.SUCCESS, null, Instant.now(), actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    /** Hàm `correlationId`: `correlation` mã. */
    private static UUID correlationId(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        if (actor != null) return actor.correlationId();
        return context != null && context.sessionId() != null ? context.sessionId() : UUID.randomUUID();
    }

    /** Hàm `toSummary`: den tonghop. */
    private static MatHangTongHopDuLieu toSummary(MatHang item) {
        return new MatHangTongHopDuLieu(item.getId(), item.getMaItem(), item.getTenItem(), item.getLoaiItem(),
                item.getHoatChat(), item.getHamLuong(), item.getDonViCoBan(), item.getGiaBanMacDinh(),
                item.isHoatDong());
    }
}
