package vn.clinicmanagement.application.reception.search;

import vn.clinicmanagement.application.reception.BenhNhanSearchMatHang;
import vn.clinicmanagement.application.reception.TiepDonReadKiemToan;

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

/** Tìm bệnh nhân trong phạm vi được cấp quyền; ghi kiểm toán và commit trước khi trả kết quả. */
public final class SearchBenhNhanHandler implements QueryHandler<SearchBenhNhanQuery, List<BenhNhanSearchMatHang>> {
    private static final String FAILURE_MESSAGE = "Không thể tìm hồ sơ lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.";
    private final IBenhNhanRepository patients;
    private final IBaoHiemRepository insuranceCards;
    private final PhanQuyenDichVu authorization;
    private final KiemToanWriteCongKetNoi audit;

    public SearchBenhNhanHandler(IBenhNhanRepository patients, IBaoHiemRepository insuranceCards,
                                 PhanQuyenDichVu authorization, KiemToanWriteCongKetNoi audit) {
        this.patients = patients;
        this.insuranceCards = insuranceCards;
        this.authorization = authorization;
        this.audit = audit;
    }

    /** Cách ghép nối cũ từ chối an toàn; để trả dữ liệu được bảo vệ phải có dịch vụ phân quyền và cổng kiểm toán. */
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

    private static String correlationId(SearchBenhNhanQuery query) {
        return query.auditActorContext() == null || query.auditActorContext().correlationId() == null
                ? null : query.auditActorContext().correlationId().toString();
    }
}
