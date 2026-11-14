package vn.clinicmanagement.application.examination;

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
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GetLuotKhamChiTietHandler implements QueryHandler<GetLuotKhamChiTietQuery, LuotKhamChiTietDuLieu> {
    private static final Set<String> ALLOWED_ROLES = Set.of("DOCTOR", "RECEPTIONIST", "ADMIN");

    /** Trường `visitRepository`: luotkham kho dữ liệu. */
    private final ILuotKhamRepository visitRepository;
    /** Trường `auditWritePort`: kiểm toán ghi congketnoi. */
    private final KiemToanWriteCongKetNoi auditWritePort;

    public GetLuotKhamChiTietHandler(ILuotKhamRepository visitRepository, KiemToanWriteCongKetNoi auditWritePort) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public KetQua<LuotKhamChiTietDuLieu> handle(GetLuotKhamChiTietQuery query) {
        if (query == null || query.visitId() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Mã lượt khám không hợp lệ.");
        }

        PhanQuyenNguCanh context = query.sessionContext();
        if (!isAuthorized(context)) {
            emitDeniedAudit(context, query.auditActorContext());
            return KetQua.failure("AUTH_ACCESS_DENIED", "Không có quyền xem lượt khám trong phiên hiện tại.");
        }

        Optional<LuotKham> visitResult = visitRepository.getById(query.visitId());
        if (visitResult.isEmpty()) {
            return KetQua.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
        }

        LuotKhamChiTietDuLieu detail = toDto(visitResult.get());
        emitSuccessAudit(context, query.auditActorContext(), query.visitId());
        return KetQua.success(detail);
    }

    /** Hàm `isAuthorized`: là `Authorized`. */
    private boolean isAuthorized(PhanQuyenNguCanh context) {
        if (context == null || !context.isValid()) {
            return false;
        }
        String roleCode = context.roleCode().trim().toUpperCase(Locale.ROOT);
        return ALLOWED_ROLES.contains(roleCode);
    }

    /** Hàm `emitDeniedAudit`: `emit` tuchoi kiểm toán. */
    private void emitDeniedAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        boolean validContext = context != null && context.isValid();
        auditWritePort.appendIndependently(new HeThongKiemToanNhapLieu(
                validContext ? context.employeeId() : null,
                validContext ? context.roleId() : null,
                validContext ? context.roleCode().trim().toUpperCase(Locale.ROOT) : null,
                validContext ? context.facilityId() : null,
                validContext ? context.departmentId() : null,
                KiemToanHanhDongMa.AUTH_ACCESS_DENIED,
                null,
                null,
                KiemToanKetQua.DENIED,
                KiemToanLyDoMa.AUTH_FORBIDDEN,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                correlationId(context, actor)));
    }

    /** Hàm `emitSuccessAudit`: `emit` thành công kiểm toán. */
    private void emitSuccessAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor, int visitId) {
        auditWritePort.appendIndependently(new HeThongKiemToanNhapLieu(
                context.employeeId(),
                context.roleId(),
                context.roleCode().trim().toUpperCase(Locale.ROOT),
                context.facilityId(),
                context.departmentId(),
                KiemToanHanhDongMa.EMR_VIEW,
                KiemToanTaiNguyenLoai.VISIT,
                visitId,
                KiemToanKetQua.SUCCESS,
                null,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
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
    private LuotKhamChiTietDuLieu toDto(LuotKham visit) {
        return new LuotKhamChiTietDuLieu(
                visit.getId(),
                visit.getMaBenhNhan(),
                visit.getMaBacSi(),
                visit.getMaCoSo(),
                visit.getMaKhoa(),
                visit.getMaPhongKham(),
                visit.getSoThuTuDisplay(),
                visit.getSoThuTu(),
                visit.getTrangThai(),
                visit.getLyDoKham(),
                visit.getQuaTrinhBenhLy(),
                visit.getTienSuBanThan(),
                visit.getTienSuGiaDinh(),
                visit.getKhamToanThan(),
                visit.getKhamBoPhan(),
                visit.getTomTatBenhAn(),
                visit.getHuongDieuTri(),
                visit.getLoiDanBacSi(),
                visit.getKetQuaDieuTri(),
                visit.getHuongXuTri(),
                visit.getNgayHenTaiKham(),
                visit.isEmrLocked(),
                visit.getEmrLockedAt(),
                visit.getEmrLockedBy(),
                visit.getEmrSignature(),
                visit.getThoiGianBatDauKham(),
                visit.getThoiGianKetThucKham());
    }
}
