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
import vn.clinicmanagement.domain.examination.ChanDoan;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AddChanDoanHandler implements CommandHandler<AddChanDoanCommand, AddChanDoanBienNhan> {
    private static final Set<String> DIAGNOSIS_TYPES = Set.of("SO_BO", "XAC_DINH", "KEM_THEO", "BIEN_CHUNG");

    /** Trường `visitRepository`: luotkham kho dữ liệu. */
    private final ILuotKhamRepository visitRepository;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public AddChanDoanHandler(ILuotKhamRepository visitRepository,
                               GiaoDichRanhGioi transactions,
                               KiemToanWriteCongKetNoi audit) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<AddChanDoanBienNhan> handle(AddChanDoanCommand command) {
        if (command == null || command.visitId() <= 0 || command.doctorId() <= 0
                || command.chanDoanText() == null || command.chanDoanText().isBlank()) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin chẩn đoán không hợp lệ.");
        }

        String diagnosisType = command.loaiChanDoan();
        if (diagnosisType == null || diagnosisType.isBlank()) {
            diagnosisType = "XAC_DINH";
        }
        if (!DIAGNOSIS_TYPES.contains(diagnosisType)) {
            return KetQua.failure("VALIDATION_ERROR", "Loại chẩn đoán không hợp lệ.");
        }

        PhanQuyenNguCanh context = command.authorizationContext();
        if (context == null || !context.isValid() || !"DOCTOR".equals(context.roleCode())) {
            return deny(context, command.auditActorContext());
        }

        String finalDiagnosisType = diagnosisType;
        return transactions.inTransaction(() -> {
            Optional<LuotKham> visitResult = visitRepository.getById(command.visitId());
            if (visitResult.isEmpty()) {
                return KetQua.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
            }

            LuotKham visit = visitResult.get();
            if (visit.getMaBacSi() != null && visit.getMaBacSi() != command.doctorId()) {
                return KetQua.failure("DOCTOR_MISMATCH", "Chỉ bác sĩ phụ trách mới có quyền thêm chẩn đoán.");
            }
            if (visit.getTrangThai() != TrangThaiLuotKham.DANG_KHAM) {
                return KetQua.failure("INVALID_STATE", "Chỉ có thể thêm chẩn đoán khi lượt khám đang diễn ra.");
            }
            if (visit.isEmrLocked()) {
                return KetQua.failure("EMR_LOCKED", "Hồ sơ bệnh án đã bị khóa không thể thêm chẩn đoán.");
            }

            if (command.laChinh() && visit.getChanDoans() != null) {
                for (ChanDoan diagnosis : visit.getChanDoans()) {
                    if (diagnosis.isLaChinh()) {
                        diagnosis.setLaChinh(false);
                    }
                }
            }

            ChanDoan diagnosis = new ChanDoan();
            diagnosis.setMaLuotKham(visit.getId());
            diagnosis.setMaICD(command.icdId());
            diagnosis.setChanDoanText(command.chanDoanText().trim());
            diagnosis.setLaChinh(command.laChinh());
            diagnosis.setLoaiChanDoan(finalDiagnosisType);
            diagnosis.setLuotKham(visit);
            if (visit.getChanDoans() == null) {
                visit.setChanDoans(new ArrayList<>());
            }
            visit.getChanDoans().add(diagnosis);

            visitRepository.update(visit);
            appendSuccessAudit(command.auditActorContext(), context, visit.getId());
            return KetQua.success(new AddChanDoanBienNhan(
                    visit.getId(), diagnosis.getChanDoanText(), diagnosis.isLaChinh(),
                    diagnosis.getLoaiChanDoan(), LocalDateTime.now()));
        });
    }

    private KetQua<AddChanDoanBienNhan> deny(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
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
        return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép thêm chẩn đoán trong phiên hiện tại.");
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
