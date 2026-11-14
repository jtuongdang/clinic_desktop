package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GetDonThuocTheoLuotKhamHandler
        implements QueryHandler<GetDonThuocTheoLuotKhamQuery, List<DonThuocDuLieu>> {
    private static final Set<String> ALLOWED_ROLES = Set.of("DOCTOR", "RECEPTIONIST", "ADMIN");

    /** Trường `prescriptionRepository`: đơn thuốc kho dữ liệu. */
    private final IDonThuocRepository prescriptionRepository;
    /** Trường `visitRepository`: luotkham kho dữ liệu. */
    private final ILuotKhamRepository visitRepository;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public GetDonThuocTheoLuotKhamHandler(
            IDonThuocRepository prescriptionRepository,
            ILuotKhamRepository visitRepository,
            KiemToanWriteCongKetNoi audit) {
        this.prescriptionRepository = Objects.requireNonNull(prescriptionRepository, "prescriptionRepository");
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<List<DonThuocDuLieu>> handle(GetDonThuocTheoLuotKhamQuery query) {
        if (query == null || query.visitId() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Mã lượt khám không hợp lệ.");
        }

        PhanQuyenNguCanh context = query.authorizationContext();
        if (!isAuthorized(context)) {
            emitDeniedAudit(context, query.auditActorContext());
            return KetQua.failure("AUTH_ACCESS_DENIED", "Không có quyền xem đơn thuốc trong phiên hiện tại.");
        }

        Optional<LuotKham> visitResult = visitRepository.getById(query.visitId());
        if (visitResult.isEmpty()) {
            return KetQua.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
        }

        List<DonThuocDuLieu> prescriptions = prescriptionRepository.getByLuotKhamId(query.visitId()).stream()
                .map(this::toDto)
                .toList();
        emitSuccessAudit(context, query.auditActorContext(), query.visitId());
        return KetQua.success(prescriptions);
    }

    /** Hàm `isAuthorized`: là `Authorized`. */
    private boolean isAuthorized(PhanQuyenNguCanh context) {
        return context != null && context.isValid() && ALLOWED_ROLES.contains(context.roleCode());
    }

    /** Hàm `emitDeniedAudit`: `emit` tuchoi kiểm toán. */
    private void emitDeniedAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        boolean validContext = context != null && context.isValid();
        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                validContext ? context.employeeId() : null,
                validContext ? context.roleId() : null,
                validContext ? context.roleCode() : null,
                validContext ? context.facilityId() : null,
                validContext ? context.departmentId() : null,
                KiemToanHanhDongMa.AUTH_ACCESS_DENIED,
                null,
                null,
                KiemToanKetQua.DENIED,
                KiemToanLyDoMa.AUTH_FORBIDDEN,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    /** Hàm `emitSuccessAudit`: `emit` thành công kiểm toán. */
    private void emitSuccessAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor, int visitId) {
        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                KiemToanHanhDongMa.EMR_VIEW,
                KiemToanTaiNguyenLoai.PRESCRIPTION,
                visitId,
                KiemToanKetQua.SUCCESS,
                null,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    /** Hàm `correlationId`: `correlation` mã. */
    private UUID correlationId(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        if (actor != null) {
            return actor.correlationId();
        }
        if (context != null && context.sessionId() != null) {
            return context.sessionId();
        }
        return UUID.randomUUID();
    }

    /** Hàm `toDto`: den dữ liệu. */
    private DonThuocDuLieu toDto(DonThuoc prescription) {
        List<DonThuocMatHangDuLieu> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        if (prescription.getChiTietDonThuocs() != null) {
            for (ChiTietDonThuoc detail : prescription.getChiTietDonThuocs()) {
                BigDecimal quantity = detail.getSoLuongKe() == null ? BigDecimal.ZERO : detail.getSoLuongKe();
                BigDecimal unitPrice = detail.getDonGiaKe() == null ? BigDecimal.ZERO : detail.getDonGiaKe();
                BigDecimal lineTotal = quantity.multiply(unitPrice);
                total = total.add(lineTotal);
                items.add(toItemDto(detail, lineTotal));
            }
        }

        return new DonThuocDuLieu(
                prescription.getId(),
                prescription.getMaLuotKham(),
                prescription.getMaBacSi(),
                prescription.getBacSi() == null ? null : prescription.getBacSi().getHoTen(),
                prescription.getMaCoSo(),
                prescription.getMaKhoXuat(),
                prescription.getTrangThai(),
                prescription.getNgayKe(),
                prescription.getGhiChu(),
                prescription.getLoiDanDonThuoc(),
                total,
                List.copyOf(items));
    }

    /** Hàm `toItemDto`: den mathang dữ liệu. */
    private DonThuocMatHangDuLieu toItemDto(ChiTietDonThuoc detail, BigDecimal lineTotal) {
        MatHang item = detail.getItem();
        return new DonThuocMatHangDuLieu(
                detail.getId(),
                detail.getMaItem(),
                item == null ? null : item.getMaItem(),
                item == null ? null : item.getTenItem(),
                item == null ? null : item.getDonViCoBan(),
                detail.getSoLuongKe(),
                detail.getSoLuongDaXuat(),
                detail.getDonGiaKe(),
                lineTotal,
                detail.getDuongDung(),
                detail.getLieuDung(),
                detail.getTanSuat(),
                detail.getSoNgay(),
                detail.getSang(),
                detail.getTrua(),
                detail.getChieu(),
                detail.getToi(),
                detail.getHuongDan());
    }
}
