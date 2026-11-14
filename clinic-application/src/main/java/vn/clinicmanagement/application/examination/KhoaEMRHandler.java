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
import vn.clinicmanagement.domain.common.exceptions.EMRDaKhoaNgoaiLe;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class KhoaEMRHandler implements CommandHandler<KhoaEMRCommand, KhoaEMRBienNhan> {
    /** Trường `visitRepository`: luotkham kho dữ liệu. */
    private final ILuotKhamRepository visitRepository;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public KhoaEMRHandler(ILuotKhamRepository visitRepository,
                          GiaoDichRanhGioi transactions,
                          KiemToanWriteCongKetNoi audit) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<KhoaEMRBienNhan> handle(KhoaEMRCommand command) {
        if (command == null || command.visitId() <= 0 || command.doctorId() <= 0
                || command.signature() == null || command.signature().trim().isEmpty()) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin khóa bệnh án không hợp lệ.");
        }

        PhanQuyenNguCanh context = command.authorizationContext();
        if (context == null || !context.isValid()
                || !("DOCTOR".equals(context.roleCode()) || "ADMIN".equals(context.roleCode()))) {
            return deny(context, command.auditActorContext());
        }

        return transactions.inTransaction(() -> {
            Optional<LuotKham> visitResult = visitRepository.getById(command.visitId());
            if (visitResult.isEmpty()) {
                return KetQua.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
            }

            LuotKham visit = visitResult.get();
            if (visit.getTrangThai() != TrangThaiLuotKham.HOAN_THANH) {
                return KetQua.failure("INVALID_STATE", "Chỉ có thể khóa bệnh án khi lượt khám đã hoàn thành.");
            }

            try {
                visit.lockEmr(command.signature(), command.doctorId());
            } catch (EMRDaKhoaNgoaiLe ex) {
                return KetQua.failure("EMR_ALREADY_LOCKED", "Hồ sơ bệnh án đã bị khóa trước đó.");
            } catch (IllegalStateException ex) {
                return KetQua.failure("INVALID_STATE", ex.getMessage());
            } catch (IllegalArgumentException ex) {
                return KetQua.failure("VALIDATION_ERROR", ex.getMessage());
            }

            visitRepository.update(visit);
            appendSuccessAudit(command.auditActorContext(), context, command.visitId());
            return KetQua.success(new KhoaEMRBienNhan(
                    visit.getId(), visit.isEmrLocked(), visit.getEmrLockedAt(),
                    visit.getEmrLockedBy(), visit.getEmrSignature()));
        });
    }

    private KetQua<KhoaEMRBienNhan> deny(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
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
        return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép khóa bệnh án trong phiên hiện tại.");
    }

    /** Hàm `appendSuccessAudit`: thêm vào thành công kiểm toán. */
    private void appendSuccessAudit(KiemToanTacNhanNguCanh actor, PhanQuyenNguCanh context, int visitId) {
        audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                KiemToanHanhDongMa.EMR_LOCKED,
                KiemToanTaiNguyenLoai.VISIT,
                visitId,
                KiemToanKetQua.SUCCESS,
                null,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                actor != null ? actor.correlationId() : UUID.randomUUID()));
    }
}
