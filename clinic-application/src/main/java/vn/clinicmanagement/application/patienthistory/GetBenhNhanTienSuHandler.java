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
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.patient.TienSuBenh;
import vn.clinicmanagement.domain.repositories.ITienSuBenhRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Exact-scope doctor history read with independent commit-before-response audit. */
public final class GetBenhNhanTienSuHandler
        implements QueryHandler<GetBenhNhanTienSuQuery, List<TienSuBenh>> {
    private static final String FAILURE_MESSAGE = "Không thể tải tiền sử bệnh lúc này.";
    private final ITienSuBenhRepository histories;
    /** Trường `authorization`: phanquyen. */
    private final PhanQuyenDichVu authorization;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public GetBenhNhanTienSuHandler(
            ITienSuBenhRepository histories,
            PhanQuyenDichVu authorization,
            KiemToanWriteCongKetNoi audit) {
        this.histories = histories;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Override
    public KetQua<List<TienSuBenh>> handle(GetBenhNhanTienSuQuery query) {
        if (query == null || query.patientId() <= 0) {
            return KetQua.failure("PATIENT_ID_REQUIRED", "Mã người bệnh không hợp lệ.");
        }

        PhanQuyenNguCanh context = query.authorizationContext();
        KiemToanTacNhanNguCanh actor = query.auditActorContext();
        if (histories == null || authorization == null || audit == null
                || !isDoctor(context) || !sameActor(context, actor)) {
            return denied();
        }

        PhanQuyenQuyetDinh decision = authorization.evaluate(new PhanQuyenYeuCau(
                context, "PATIENT", HanhDongQuyen.XEM, context.facilityId(), context.departmentId()));
        if (decision == null || !decision.allowed()) return denied();

        List<TienSuBenh> result;
        try {
            List<TienSuBenh> found = histories.getHistoryByPatientAndScope(
                    query.patientId(), context.facilityId(), context.departmentId());
            if (found == null) throw new IllegalStateException("History repository returned no result list.");
            result = List.copyOf(found);
        } catch (RuntimeException exception) {
            logFailure("PATIENT_HISTORY_QUERY_FAILED", query, exception);
            return KetQua.failure("PATIENT_HISTORY_FAILED", FAILURE_MESSAGE);
        }

        try {
            audit.appendIndependently(new HeThongKiemToanNhapLieu(
                    actor.employeeId(), actor.roleId(), actor.roleCode(), actor.facilityId(), actor.departmentId(),
                    KiemToanHanhDongMa.PATIENT_VIEW, KiemToanTaiNguyenLoai.PATIENT, query.patientId(),
                    KiemToanKetQua.SUCCESS, null, Instant.now(), actor.workstationId(), actor.correlationId()));
        } catch (RuntimeException exception) {
            logFailure("PATIENT_HISTORY_AUDIT_FAILED", query, exception);
            return KetQua.failure("PATIENT_HISTORY_AUDIT_FAILED", FAILURE_MESSAGE);
        }
        return KetQua.success(result);
    }

    /** Hàm `isDoctor`: là bacsi. */
    private static boolean isDoctor(PhanQuyenNguCanh context) {
        return context != null && context.isValid() && "DOCTOR".equals(context.roleCode());
    }

    /** Hàm `sameActor`: `same` tacnhan. */
    private static boolean sameActor(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        return actor != null
                && context.employeeId() == actor.employeeId()
                && context.roleId() == actor.roleId()
                && Objects.equals(context.roleCode(), actor.roleCode())
                && context.facilityId() == actor.facilityId()
                && context.departmentId() == actor.departmentId();
    }

    /** Hàm `denied`: tuchoi. */
    private static KetQua<List<TienSuBenh>> denied() {
        return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép xem tiền sử bệnh trong phiên hiện tại.");
    }

    /** Hàm `logFailure`: nhatky thất bại. */
    private static void logFailure(String code, GetBenhNhanTienSuQuery query, RuntimeException exception) {
        KiemToanTacNhanNguCanh actor = query.auditActorContext();
        String correlationId = actor == null || actor.correlationId() == null
                ? null : actor.correlationId().toString();
        UngDungThatBaiChanDoanLoi.logUnexpectedFailure("PATIENT_HISTORY_READ", code, correlationId, exception);
    }
}
