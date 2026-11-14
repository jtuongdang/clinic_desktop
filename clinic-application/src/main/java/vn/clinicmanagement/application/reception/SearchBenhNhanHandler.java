package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenQuyetDinh;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.common.UngDungThatBaiChanDoanLoi;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.repositories.IBaoHiemRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;

import java.util.List;

/** Authorized, scoped patient search with commit-before-response audit semantics. */
public final class SearchBenhNhanHandler implements QueryHandler<SearchBenhNhanQuery, List<BenhNhanSearchMatHang>> {
    private static final String FAILURE_MESSAGE = "Không thể tìm hồ sơ lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.";
    /** Trường `patients`: bệnh nhân. */
    private final IBenhNhanRepository patients;
    private final IBaoHiemRepository insuranceCards;
    /** Trường `authorization`: phanquyen. */
    private final PhanQuyenDichVu authorization;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public SearchBenhNhanHandler(IBenhNhanRepository patients, IBaoHiemRepository insuranceCards,
                                 PhanQuyenDichVu authorization, KiemToanWriteCongKetNoi audit) {
        this.patients = patients;
        this.insuranceCards = insuranceCards;
        this.authorization = authorization;
        this.audit = audit;
    }

    /** Legacy wiring fails closed; protected results require an authorization service and audit port. */
    @Deprecated
    public SearchBenhNhanHandler(IBenhNhanRepository patients, IBaoHiemRepository insuranceCards) {
        this(patients, insuranceCards, null, null);
    }

    @Override
    public KetQua<List<BenhNhanSearchMatHang>> handle(SearchBenhNhanQuery query) {
        if (query == null || query.keyword() == null || query.keyword().isBlank()) {
            return KetQua.failure("SEARCH_KEYWORD_REQUIRED",
                    "Nhập mã bệnh nhân, CCCD, số điện thoại hoặc họ tên để tìm.");
        }
        PhanQuyenQuyetDinh decision = TiepDonReadKiemToan.evaluate(query.authorizationContext(),
                query.auditActorContext(), authorization, "PATIENT");
        if (!decision.allowed()) {
            TiepDonReadKiemToan.denied(audit, query.auditActorContext(), decision);
            return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép xem hồ sơ trong phiên hiện tại.");
        }
        try {
            String keyword = query.keyword() == null ? "" : query.keyword().trim();
            List<BenhNhanSearchMatHang> matches = patients.searchInScope(keyword,
                            query.authorizationContext().facilityId(), query.authorizationContext().departmentId(), 50)
                    .stream().limit(50)
                    .map(patient -> new BenhNhanSearchMatHang(patient,
                            insuranceCards.getLatestByPatientId(patient.getId()).orElse(null)))
                    .toList();
            TiepDonReadKiemToan.success(audit, query.auditActorContext(), KiemToanHanhDongMa.PATIENT_SEARCH);
            return KetQua.success(matches);
        } catch (RuntimeException ex) {
            UngDungThatBaiChanDoanLoi.logUnexpectedFailure("RECEPTION_SEARCH_PATIENTS", "PATIENT_SEARCH_FAILED",
                    correlationId(query), ex);
            return KetQua.failure("PATIENT_SEARCH_FAILED", FAILURE_MESSAGE);
        }
    }

    /** Hàm `correlationId`: `correlation` mã. */
    private static String correlationId(SearchBenhNhanQuery query) {
        return query.auditActorContext() == null || query.auditActorContext().correlationId() == null
                ? null : query.auditActorContext().correlationId().toString();
    }
}
