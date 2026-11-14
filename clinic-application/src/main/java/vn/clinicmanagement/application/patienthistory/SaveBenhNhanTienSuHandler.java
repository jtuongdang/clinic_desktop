package vn.clinicmanagement.application.patienthistory;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenQuyetDinh;
import vn.clinicmanagement.application.authorization.PhanQuyenYeuCau;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.common.UngDungThatBaiChanDoanLoi;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.patient.TienSuBenh;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.ITienSuBenhRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Audited, exact-session-scope create/update workflow for approved patient history categories. */
public final class SaveBenhNhanTienSuHandler implements CommandHandler<SaveBenhNhanTienSuCommand, BenhNhanTienSuBienNhan> {
    private static final String FAILURE_MESSAGE = "Không thể lưu tiền sử bệnh lúc này.";
    private final ITienSuBenhRepository histories;
    /** Trường `patients`: bệnh nhân. */
    private final IBenhNhanRepository patients;
    /** Trường `authorization`: phanquyen. */
    private final PhanQuyenDichVu authorization;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public SaveBenhNhanTienSuHandler(ITienSuBenhRepository histories, IBenhNhanRepository patients,
                                     PhanQuyenDichVu authorization, GiaoDichRanhGioi transactions,
                                     KiemToanWriteCongKetNoi audit) {
        this.histories = histories;
        this.patients = patients;
        this.authorization = authorization;
        this.transactions = transactions;
        this.audit = audit;
    }

    @Override
    public KetQua<BenhNhanTienSuBienNhan> handle(SaveBenhNhanTienSuCommand command) {
        KetQua<BenhNhanTienSuBienNhan> invalid = validatePayload(command);
        if (invalid != null) return invalid;
        PhanQuyenNguCanh context = command.authorizationContext();
        KiemToanTacNhanNguCanh actor = command.auditActorContext();
        if (histories == null || patients == null || authorization == null || transactions == null || audit == null
                || !isDoctor(context) || !sameActor(context, actor)) return denied();
        try {
            PhanQuyenQuyetDinh decision = authorization.evaluate(new PhanQuyenYeuCau(
                    context, "PATIENT", HanhDongQuyen.XEM, context.facilityId(), context.departmentId()));
            if (decision == null || !decision.allowed()) return denied();
        } catch (RuntimeException ignored) {
            return denied();
        }

        return transactions.inTransaction(() -> {
            try {
                Optional<BenhNhan> patient = patients.getByIdInScope(
                        command.patientId(), context.facilityId(), context.departmentId());
                if (patient == null || patient.isEmpty() || patient.get().isDeleted()
                        || patient.get().getId() == null || patient.get().getId() != command.patientId()) return denied();
                TienSuBenh history;
                boolean create = command.historyId() == null;
                if (create) {
                    history = new TienSuBenh();
                    history.setMaBenhNhan(command.patientId());
                    history.setCreatedBy(context.employeeId());
                    history.setOwnershipScope(context.facilityId(), context.departmentId());
                } else {
                    Optional<TienSuBenh> existing = histories.getActiveByIdPatientAndScope(
                            command.historyId(), command.patientId(), context.facilityId(), context.departmentId());
                    if (existing == null || existing.isEmpty()) return denied();
                    history = existing.get();
                    if (history.getId() == null || history.getId() != command.historyId()
                            || history.getMaBenhNhan() != command.patientId() || history.isDeleted()
                            || !history.hasExactOwnershipScope(context.facilityId(), context.departmentId())
                            || history.getVersion() == null || !history.getVersion().equals(command.expectedVersion())) {
                        return denied();
                    }
                    history.setUpdatedBy(context.employeeId());
                }
                history.setLoaiTienSu(command.category());
                history.setMoTa(command.narrative().trim());
                if (create) histories.add(history);
                else if (!histories.update(history, command.expectedVersion())) {
                    return KetQua.failure("PATIENT_HISTORY_CONFLICT", "Tiền sử đã thay đổi hoặc không còn khả dụng.");
                }
                audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(
                        actor.employeeId(), actor.roleId(), actor.roleCode(), actor.facilityId(), actor.departmentId(),
                        KiemToanHanhDongMa.PATIENT_UPDATED, KiemToanTaiNguyenLoai.PATIENT, command.patientId(),
                        KiemToanKetQua.SUCCESS, null, Instant.now(), actor.workstationId(), actor.correlationId()));
                return KetQua.success(new BenhNhanTienSuBienNhan(command.patientId(), history.getId(),
                        history.getVersion() == null ? 0L : history.getVersion()));
            } catch (RuntimeException exception) {
                UngDungThatBaiChanDoanLoi.logUnexpectedFailure("PATIENT_HISTORY_WRITE",
                        "PATIENT_HISTORY_FAILED", actor.correlationId().toString(), exception);
                return KetQua.failure("PATIENT_HISTORY_FAILED", FAILURE_MESSAGE);
            }
        });
    }

    /** Hàm `validatePayload`: kiểm tra hợp lệ `Payload`. */
    private static KetQua<BenhNhanTienSuBienNhan> validatePayload(SaveBenhNhanTienSuCommand command) {
        if (command == null || command.patientId() <= 0
                || (command.historyId() != null && command.historyId() <= 0)
                || (command.historyId() == null) != (command.expectedVersion() == null)
                || (command.expectedVersion() != null && command.expectedVersion() < 0)) {
            return KetQua.failure("PATIENT_HISTORY_INVALID", "Thông tin tiền sử bệnh không hợp lệ.");
        }
        if (!isApproved(command.category())) {
            return KetQua.failure("PATIENT_HISTORY_CATEGORY_INVALID", "Loại tiền sử bệnh không được hỗ trợ.");
        }
        if (command.narrative() == null || command.narrative().isBlank() || command.narrative().length() > 1000) {
            return KetQua.failure("PATIENT_HISTORY_NARRATIVE_INVALID", "Mô tả phải có nội dung và không quá 1000 ký tự.");
        }
        return null;
    }

    /** Hàm `isApproved`: là daduyet. */
    private static boolean isApproved(LoaiTienSu category) {
        return category == LoaiTienSu.BENH_NEN || category == LoaiTienSu.DI_UNG || category == LoaiTienSu.PHAU_THUAT;
    }

    /** Hàm `isDoctor`: là bacsi. */
    private static boolean isDoctor(PhanQuyenNguCanh context) {
        return context != null && context.isValid() && "DOCTOR".equals(context.roleCode());
    }

    /** Hàm `sameActor`: `same` tacnhan. */
    private static boolean sameActor(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        return actor != null && context.employeeId() == actor.employeeId() && context.roleId() == actor.roleId()
                && Objects.equals(context.roleCode(), actor.roleCode())
                && context.facilityId() == actor.facilityId() && context.departmentId() == actor.departmentId();
    }

    /** Hàm `denied`: tuchoi. */
    private static KetQua<BenhNhanTienSuBienNhan> denied() {
        return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép lưu tiền sử bệnh trong phiên hiện tại.");
    }
}
