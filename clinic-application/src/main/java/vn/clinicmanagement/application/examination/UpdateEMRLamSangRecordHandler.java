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
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class UpdateEMRLamSangRecordHandler implements CommandHandler<UpdateEMRLamSangRecordCommand, UpdateEMRLamSangRecordBienNhan> {
    /** Trường `visitRepository`: luotkham kho dữ liệu. */
    private final ILuotKhamRepository visitRepository;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public UpdateEMRLamSangRecordHandler(ILuotKhamRepository visitRepository,
                                         GiaoDichRanhGioi transactions,
                                         KiemToanWriteCongKetNoi audit) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<UpdateEMRLamSangRecordBienNhan> handle(UpdateEMRLamSangRecordCommand command) {
        if (command == null || command.visitId() <= 0 || command.doctorId() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin cập nhật bệnh án không hợp lệ.");
        }

        PhanQuyenNguCanh context = command.authorizationContext();
        if (context == null || !context.isValid() || !"DOCTOR".equals(context.roleCode())) {
            return deny(context, command.auditActorContext());
        }

        return transactions.inTransaction(() -> {
            Optional<LuotKham> visitResult = visitRepository.getById(command.visitId());
            if (visitResult.isEmpty()) {
                return KetQua.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
            }

            LuotKham visit = visitResult.get();
            if (visit.getMaBacSi() != null && visit.getMaBacSi() != command.doctorId()) {
                return KetQua.failure("DOCTOR_MISMATCH", "Chỉ bác sĩ phụ trách mới có quyền cập nhật bệnh án.");
            }

            if (visit.isEmrLocked()) {
                return KetQua.failure("EMR_ALREADY_LOCKED", "Hồ sơ bệnh án đã bị khóa không thể chỉnh sửa.");
            }

            if (visit.getTrangThai() != TrangThaiLuotKham.DANG_KHAM) {
                return KetQua.failure("INVALID_STATE", "Chỉ có thể cập nhật bệnh án khi đang khám.");
            }

            if (command.quaTrinhBenhLy() != null) {
                visit.setQuaTrinhBenhLy(command.quaTrinhBenhLy());
            }
            if (command.tienSuBanThan() != null) {
                visit.setTienSuBanThan(command.tienSuBanThan());
            }
            if (command.tienSuGiaDinh() != null) {
                visit.setTienSuGiaDinh(command.tienSuGiaDinh());
            }
            if (command.khamToanThan() != null) {
                visit.setKhamToanThan(command.khamToanThan());
            }
            if (command.khamBoPhan() != null) {
                visit.setKhamBoPhan(command.khamBoPhan());
            }
            if (command.tomTatBenhAn() != null) {
                visit.setTomTatBenhAn(command.tomTatBenhAn());
            }
            if (command.huongDieuTri() != null) {
                visit.setHuongDieuTri(command.huongDieuTri());
            }
            if (command.loiDanBacSi() != null) {
                visit.setLoiDanBacSi(command.loiDanBacSi());
            }

            visitRepository.update(visit);
            appendSuccessAudit(command.auditActorContext(), context, command.visitId());
            return KetQua.success(new UpdateEMRLamSangRecordBienNhan(
                    visit.getId(), visit.isEmrLocked(), LocalDateTime.now()));
        });
    }

    private KetQua<UpdateEMRLamSangRecordBienNhan> deny(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        boolean validContext = context != null && context.isValid();
        Integer actorId = null;
        Integer roleId = null;
        String roleCode = null;
        Integer facilityId = null;
        Integer departmentId = null;
        if (validContext) {
            actorId = context.employeeId();
            roleId = context.roleId();
            roleCode = context.roleCode();
            facilityId = context.facilityId();
            departmentId = context.departmentId();
        }

        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                actorId,
                roleId,
                roleCode,
                facilityId,
                departmentId,
                KiemToanHanhDongMa.AUTH_ACCESS_DENIED,
                null,
                null,
                KiemToanKetQua.DENIED,
                KiemToanLyDoMa.AUTH_FORBIDDEN,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                actor != null ? actor.correlationId() : UUID.randomUUID()));
        return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép cập nhật bệnh án trong phiên hiện tại.");
    }

    /** Hàm `appendSuccessAudit`: thêm vào thành công kiểm toán. */
    private void appendSuccessAudit(KiemToanTacNhanNguCanh actor, PhanQuyenNguCanh context, int visitId) {
        audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                KiemToanHanhDongMa.EMR_UPDATED,
                KiemToanTaiNguyenLoai.VISIT,
                visitId,
                KiemToanKetQua.SUCCESS,
                null,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                actor != null ? actor.correlationId() : UUID.randomUUID()));
    }
}
