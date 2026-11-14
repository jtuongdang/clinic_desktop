package vn.clinicmanagement.application.prescription;

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
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CancelDonThuocHandler
        implements CommandHandler<CancelDonThuocCommand, CancelDonThuocBienNhan> {
    /** Trường `prescriptionRepository`: đơn thuốc kho dữ liệu. */
    private final IDonThuocRepository prescriptionRepository;
    /** Trường `visitRepository`: luotkham kho dữ liệu. */
    private final ILuotKhamRepository visitRepository;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public CancelDonThuocHandler(
            IDonThuocRepository prescriptionRepository,
            ILuotKhamRepository visitRepository,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit) {
        this.prescriptionRepository = Objects.requireNonNull(prescriptionRepository, "prescriptionRepository");
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<CancelDonThuocBienNhan> handle(CancelDonThuocCommand command) {
        if (command == null || command.prescriptionId() <= 0 || command.doctorId() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin hủy đơn thuốc không hợp lệ.");
        }
        if (command.lyDoHuy() == null || command.lyDoHuy().trim().length() < 5) {
            return KetQua.failure("VALIDATION_ERROR", "Lý do hủy đơn thuốc phải có ít nhất 5 ký tự.");
        }

        PhanQuyenNguCanh context = command.authorizationContext();
        if (context == null || !context.isValid()
                || !("DOCTOR".equals(context.roleCode()) || "ADMIN".equals(context.roleCode()))) {
            return deny(context, command.auditActorContext());
        }

        Optional<DonThuoc> prescriptionResult = prescriptionRepository.getById(command.prescriptionId());
        if (prescriptionResult.isEmpty()) {
            return KetQua.failure("NOT_FOUND", "Không tìm thấy đơn thuốc.");
        }

        DonThuoc prescription = prescriptionResult.get();
        if ("DOCTOR".equals(context.roleCode())
                && prescription.getMaBacSi() > 0
                && prescription.getMaBacSi() != command.doctorId()) {
            return KetQua.failure("DOCTOR_MISMATCH", "Chỉ bác sĩ kê đơn hoặc quản trị viên mới có quyền hủy đơn.");
        }
        if (prescription.getTrangThai() == TrangThaiDonThuoc.DA_XUAT_HET) {
            return KetQua.failure("ALREADY_DISPENSED", "Không thể hủy đơn thuốc đã xuất hết.");
        }
        if (prescription.getTrangThai() == TrangThaiDonThuoc.HUY) {
            return KetQua.failure("ALREADY_CANCELLED", "Đơn thuốc đã bị hủy trước đó.");
        }

        Optional<LuotKham> visitResult = visitRepository.getById(prescription.getMaLuotKham());
        if (visitResult.isPresent() && visitResult.get().isEmrLocked()) {
            return KetQua.failure("EMR_LOCKED", "Hồ sơ bệnh án đã bị khóa không thể hủy đơn thuốc.");
        }

        String cancellationReason = command.lyDoHuy().trim();
        return transactions.inTransaction(() -> {
            prescription.cancel(cancellationReason);
            prescriptionRepository.update(prescription);
            audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(
                    context.employeeId(),
                    context.roleId(),
                    context.roleCode(),
                    context.facilityId(),
                    context.departmentId(),
                    KiemToanHanhDongMa.RX_CANCELLED,
                    KiemToanTaiNguyenLoai.PRESCRIPTION,
                    prescription.getId(),
                    KiemToanKetQua.SUCCESS,
                    null,
                    Instant.now(),
                    command.auditActorContext() == null ? null : command.auditActorContext().workstationId(),
                    command.auditActorContext() == null
                            ? context.sessionId()
                            : command.auditActorContext().correlationId()));
            return KetQua.success(new CancelDonThuocBienNhan(
                    prescription.getId(),
                    prescription.getMaLuotKham(),
                    cancellationReason,
                    prescription.getTrangThai(),
                    LocalDateTime.now()));
        });
    }

    private KetQua<CancelDonThuocBienNhan> deny(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        boolean validContext = context != null && context.isValid();
        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                validContext ? context.employeeId() : actor == null ? null : actor.employeeId(),
                validContext ? context.roleId() : actor == null ? null : actor.roleId(),
                validContext ? context.roleCode() : actor == null ? null : actor.roleCode(),
                validContext ? context.facilityId() : actor == null ? null : actor.facilityId(),
                validContext ? context.departmentId() : actor == null ? null : actor.departmentId(),
                KiemToanHanhDongMa.AUTH_ACCESS_DENIED,
                null,
                null,
                KiemToanKetQua.DENIED,
                KiemToanLyDoMa.AUTH_FORBIDDEN,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
                actor == null ? UUID.randomUUID() : actor.correlationId()));
        return KetQua.failure("AUTH_ACCESS_DENIED", "Không được phép hủy đơn thuốc trong phiên hiện tại.");
    }
}
