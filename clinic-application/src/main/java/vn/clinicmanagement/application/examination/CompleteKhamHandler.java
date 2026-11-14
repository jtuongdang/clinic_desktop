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
import vn.clinicmanagement.domain.common.exceptions.EMRDaKhoaNgoaiLe;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CompleteKhamHandler implements CommandHandler<CompleteKhamCommand, CompleteKhamBienNhan> {
    /** Trường `visitRepository`: luotkham kho dữ liệu. */
    private final ILuotKhamRepository visitRepository;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public CompleteKhamHandler(ILuotKhamRepository visitRepository,
                               GiaoDichRanhGioi transactions,
                               KiemToanWriteCongKetNoi audit) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<CompleteKhamBienNhan> handle(CompleteKhamCommand command) {
        if (command == null || command.visitId() <= 0 || command.doctorId() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin kết thúc khám không hợp lệ.");
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
                return KetQua.failure("DOCTOR_MISMATCH", "Chỉ bác sĩ phụ trách mới có quyền kết thúc lượt khám.");
            }

            if (command.ketQuaDieuTri() != null) {
                visit.setKetQuaDieuTri(command.ketQuaDieuTri());
            }
            if (command.huongXuTri() != null) {
                visit.setHuongXuTri(command.huongXuTri());
            }
            if (command.loiDanBacSi() != null) {
                visit.setLoiDanBacSi(command.loiDanBacSi());
            }
            if (command.ngayHenTaiKham() != null) {
                visit.setNgayHenTaiKham(command.ngayHenTaiKham());
            }

            try {
                visit.completeExam();
            } catch (IllegalStateException | EMRDaKhoaNgoaiLe ex) {
                return KetQua.failure("INVALID_STATE", ex.getMessage());
            }

            visitRepository.update(visit);
            appendSuccessAudit(command.auditActorContext(), context, command.visitId());
            return KetQua.success(new CompleteKhamBienNhan(
                    visit.getId(), visit.getTrangThai(), visit.getThoiGianKetThucKham()));
        });
    }

    private KetQua<CompleteKhamBienNhan> deny(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
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
                actor != null ? actor.workstationId() : null,
                actor != null ? actor.correlationId() : UUID.randomUUID()));
        return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép kết thúc khám trong phiên hiện tại.");
    }

    /** Hàm `appendSuccessAudit`: thêm vào thành công kiểm toán. */
    private void appendSuccessAudit(KiemToanTacNhanNguCanh actor, PhanQuyenNguCanh context, int visitId) {
        audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                KiemToanHanhDongMa.EMR_COMPLETED,
                KiemToanTaiNguyenLoai.VISIT,
                visitId,
                KiemToanKetQua.SUCCESS,
                null,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                actor != null ? actor.correlationId() : UUID.randomUUID()));
    }
}
